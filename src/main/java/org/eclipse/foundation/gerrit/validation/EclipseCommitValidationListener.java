/**
 * ***************************************************************************** Copyright (c) 2013
 * Eclipse Foundation and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Wayne Beaton (Eclipse Foundation)- initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.foundation.gerrit.validation;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonEncodingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.eclipse.foundation.gerrit.validation.CommitStatus.CommitStatusMessage;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import retrofit2.Response;

/**
 * The EclipseCommitValidationListener implements CommitValidationListener to ensure that project
 * committer or contributor have a valid ECA at the time of the push.
 *
 * <p>There more is information regarding ECA requirements and workflow on the <a
 * href="http://wiki.eclipse.org/CLA/Implementation_Requirements">Eclipse Wiki</a>.
 *
 * <p>The CommitValidationListener is not defined as part of the extension API, which means that we
 * need to build this as a version-sensitive <a
 * href="http://gerrit-documentation.googlecode.com/svn/Documentation/2.6/dev-plugins.html">Gerrit
 * plugin</a>.
 */
@Listen
@Singleton
public class EclipseCommitValidationListener implements CommitValidationListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String ECA_DOCUMENTATION = "Please see http://wiki.eclipse.org/ECA";

  private final APIService apiService;
  private final JsonAdapter<ValidationResponse> responseAdapter;
  private final String pluginName;
  private final ProjectCache projectCache;
  private final PluginConfigFactory pluginCfgFactory;

  @Inject
  public EclipseCommitValidationListener(
      @PluginName String pluginName,
      ProjectCache projectCache,
      PluginConfigFactory pluginCfgFactory) {
    RetrofitFactory retrofitFactory = new RetrofitFactory();
    this.apiService = retrofitFactory.newService(APIService.BASE_URL, APIService.class);
    Optional<JsonAdapter<ValidationResponse>> adapter =
        retrofitFactory.adapter(ValidationResponse.class);
    if (adapter.isEmpty()) {
      throw new IllegalStateException("Cannot process validation responses, not continuing");
    }
    this.responseAdapter = adapter.get();
    this.pluginName = pluginName;
    this.projectCache = projectCache;
    this.pluginCfgFactory = pluginCfgFactory;
  }

  /**
   * Validate a single commit (this listener will be invoked for each commit in a push operation).
   */
  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    List<CommitValidationMessage> messages = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    Project.NameKey project = receiveEvent.project.getNameKey();
    // Check whether or not the validation is enabled for this project
    if (!isEnabledForProject(project)) {
      logger.atFine().log(
          "Plugin %s is not enabled for project %s: Skip validation", pluginName, project.get());
      return messages;
    }

    // create the request container
    ValidationRequest.Builder req = ValidationRequest.builder();
    req.repoUrl(project.toString());
    req.provider("gerrit");
    req.strictMode(true);

    // retrieve information about the current commit
    RevCommit commit = receiveEvent.commit;
    PersonIdent authorIdent = commit.getAuthorIdent();
    PersonIdent committerIdent = commit.getCommitterIdent();

    addSeparatorLine(messages);
    messages.add(
        new CommitValidationMessage(
            String.format("Reviewing commit: %1$s", commit.abbreviate(8).name()), false));
    messages.add(
        new CommitValidationMessage(
            String.format(
                "Authored by: %1$s <%2$s>", authorIdent.getName(), authorIdent.getEmailAddress()),
            false));
    addEmptyLine(messages);
    // update the commit list for the request to contain the current request
    req.commits(Arrays.asList(getRequestCommit(commit, authorIdent, committerIdent)));
    // send the request and await the response from the API
    ValidationRequest requestActual = req.build();
    logger.atFine().log("Request object: %s", requestActual);
    CompletableFuture<Response<ValidationResponse>> futureResponse =
        this.apiService.validate(requestActual);
    try {
      Response<ValidationResponse> rawResponse = futureResponse.get();
      ValidationResponse response;
      // handle error responses (okhttp doesn't assume error types)
      if (rawResponse.isSuccessful()) {
        response = rawResponse.body();
      } else {
        // auto close the response resources after fetching
        try (ResponseBody err = futureResponse.get().errorBody();
            BufferedSource src = err.source()) {
          response = this.responseAdapter.fromJson(src);
        } catch (JsonEncodingException e) {
          logger.atSevere().withCause(e).log("%s", e.getMessage());
          throw new CommitValidationException(
              "An error happened while retrieving validation response, please contact the administrator if this error persists",
              e);
        }
      }
      logger.atFine().log("Response object: %s", response);
      for (CommitStatus c : response.commits().values()) {
        messages.addAll(
            c.messages().stream()
                .map(
                    message ->
                        new CommitValidationMessage(
                            message.message(),
                            message.code() < 0 && shouldEnforceStrict(response, requestActual)))
                .collect(Collectors.toList()));
        addEmptyLine(messages);
        if (response.errorCount() > 0 && shouldEnforceStrict(response, requestActual)) {
          errors.addAll(
              c.errors().stream().map(CommitStatusMessage::message).collect(Collectors.toList()));
          errors.add("An Eclipse Contributor Agreement is required.");
        }
      }
    } catch (IOException | ExecutionException e) {
      logger.atSevere().withCause(e).log("%s", e.getMessage());
      throw new CommitValidationException("An error happened while checking commit", e);
    } catch (InterruptedException e) {
      logger.atSevere().withCause(e).log("%s", e.getMessage());
      Thread.currentThread().interrupt();
      throw new CommitValidationException("Verification of commit has been interrupted", e);
    }

    // TODO Extend exception-throwing delegation to include all possible messages.
    if (!errors.isEmpty()) {
      addDocumentationPointerMessage(messages);
      throw new CommitValidationException(errors.get(0), messages);
    }

    return addSuccessMessage(messages, "This commit passes Eclipse validation.");
  }

  private static List<CommitValidationMessage> addSuccessMessage(
      List<CommitValidationMessage> messages, String message) {
    messages.add(new CommitValidationMessage(message, false));
    return messages;
  }

  /**
   * Check whether or not ECA plugin is enabled for this project.
   *
   * @param project to check
   * @return true if ECA check is enabled for this project or any parent projects in project
   *     hierarchy, false otherwise.
   */
  private boolean isEnabledForProject(Project.NameKey project) {
    Optional<ProjectState> projectState = projectCache.get(project);
    if (!projectState.isPresent()) {
      logger.atSevere().log(
          "Failed to check if %s is enabled for project %s: Project not found",
          pluginName, project.get());
      return false;
    }
    return "true"
        .equals(
            pluginCfgFactory
                .getFromProjectConfigWithInheritance(projectState.get(), pluginName)
                .getString("enabled", "false"));
  }

  /**
   * Creates request representation of the commit, containing information about the current commit
   * and the users associated with it.
   *
   * @param src the commit associated with this request
   * @param author the author of the commit
   * @param committer the committer for this request
   * @return a Commit object to be posted to the ECA validation service.
   */
  private static Commit getRequestCommit(RevCommit src, PersonIdent author, PersonIdent committer) {
    // load commit object with information contained in the commit
    Commit.Builder c = Commit.builder();
    c.subject(src.getShortMessage());
    c.hash(src.name());
    c.body(src.getFullMessage());
    c.head(true);

    // get the parent commits, and retrieve their hashes
    RevCommit[] parents = src.getParents();
    List<String> parentHashes = new ArrayList<>(parents.length);
    for (RevCommit parent : parents) {
      parentHashes.add(parent.name());
    }
    c.parents(parentHashes);

    // convert the commit users to objects to be passed to ECA service
    GitUser.Builder authorGit = GitUser.builder();
    authorGit.mail(author.getEmailAddress());
    authorGit.name(author.getName());
    GitUser.Builder committerGit = GitUser.builder();
    committerGit.mail(committer.getEmailAddress());
    committerGit.name(committer.getName());

    c.author(authorGit.build());
    c.committer(committerGit.build());
    return c.build();
  }

  private static boolean shouldEnforceStrict(
      ValidationResponse response, ValidationRequest request) {
    return response.trackedProject() || request.strictMode();
  }

  private static void addSeparatorLine(List<CommitValidationMessage> messages) {
    messages.add(new CommitValidationMessage("----------", false));
  }

  private static void addEmptyLine(List<CommitValidationMessage> messages) {
    messages.add(new CommitValidationMessage("", false));
  }

  private static void addDocumentationPointerMessage(List<CommitValidationMessage> messages) {
    messages.add(new CommitValidationMessage(ECA_DOCUMENTATION, false));
  }
}
