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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.foundation.gerrit.validation.CommitStatus.CommitStatusMessage;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

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
@Singleton
public class EclipseCommitValidationListener extends BaseEclipseCommitValidator
    implements CommitValidationListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String ECA_DOCUMENTATION = "Please see http://wiki.eclipse.org/ECA";

  private final ProjectCache projectCache;

  @Inject
  public EclipseCommitValidationListener(
      @PluginName String pluginName,
      ProjectCache projectCache,
      PluginConfigFactory pluginCfgFactory) {
    super(pluginCfgFactory, pluginName);
    this.projectCache = projectCache;
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

    // Check whether the validation is enabled for this project
    if (!isEnabledForProject(project)) {
      logger.atFine().log(
          "Plugin %s is not enabled for project %s: Skip validation", pluginName, project.get());
      return messages;
    }

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
    ValidationResponse response;
    try {
      response = validate(project, authorIdent, committerIdent, commit);
      for (CommitStatus c : response.commits().values()) {
        messages.addAll(
            c.messages().stream()
                .map(
                    message ->
                        new CommitValidationMessage(
                            message.message(), message.code() < 0 && response.trackedProject()))
                .collect(Collectors.toList()));
        addEmptyLine(messages);
        if (response.errorCount() > 0 && response.trackedProject()) {

          errors.addAll(
              c.errors().stream().map(CommitStatusMessage::message).collect(Collectors.toList()));
          errors.add("An Eclipse Contributor Agreement is required.");
        }
      }
    } catch (CommitValidationException cve) {
      errors.add(cve.getMessage());
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
   * Check whether ECA plugin is enabled for this project.
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
