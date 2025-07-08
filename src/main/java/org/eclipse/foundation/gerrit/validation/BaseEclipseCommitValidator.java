/**
 * ***************************************************************************** Copyright (c) 2025
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
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonEncodingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import retrofit2.Response;

abstract class BaseEclipseCommitValidator {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final int DEFAULT_API_TIMEOUT_SECS = 20;

  final String pluginName;
  private final JsonAdapter<ValidationResponse> responseAdapter;
  final PluginConfigFactory pluginCfgFactory;
  private final RetrofitFactory retrofitFactory;

  public BaseEclipseCommitValidator(
      PluginConfigFactory pluginCfgFactory, @PluginName String pluginName) {
    this.pluginCfgFactory = pluginCfgFactory;
    this.pluginName = pluginName;
    this.retrofitFactory = new RetrofitFactory();
    Optional<JsonAdapter<ValidationResponse>> adapter =
        retrofitFactory.adapter(ValidationResponse.class);
    if (adapter.isEmpty()) {
      throw new IllegalStateException("Cannot process validation responses, not continuing");
    }
    this.responseAdapter = adapter.get();
  }

  /**
   * Validate a single commit (this listener will be invoked for each commit in a push operation).
   */
  public ValidationResponse validate(
      Project.NameKey project,
      PersonIdent authorIdent,
      PersonIdent committerIdent,
      RevCommit commit)
      throws CommitValidationException {

    // create the request container
    ValidationRequest.Builder req = ValidationRequest.builder();
    req.repoUrl(project.toString());
    req.provider("gerrit");
    req.strictMode(true);
    req.commits(Collections.singletonList(getRequestCommit(commit, authorIdent, committerIdent)));

    // send the request and await the response from the API
    ValidationRequest requestActual = req.build();
    logger.atFine().log("Request object: %s", requestActual);

    try {
      int apiTimeout =
          pluginCfgFactory
              .getFromProjectConfigWithInheritance(project, pluginName)
              .getInt("apiTimeout", DEFAULT_API_TIMEOUT_SECS);

      APIService apiService =
          retrofitFactory.newService(APIService.BASE_URL, apiTimeout, APIService.class);

      CompletableFuture<Response<ValidationResponse>> futureResponse =
          apiService.validate(requestActual);
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
      return response;
    } catch (IOException | ExecutionException e) {
      logger.atSevere().withCause(e).log("%s", e.getMessage());
      throw new CommitValidationException(
          "An error happened while checking commit: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      logger.atSevere().withCause(e).log("%s", e.getMessage());
      Thread.currentThread().interrupt();
      throw new CommitValidationException(
          "Verification of commit has been interrupted: " + e.getMessage(), e);
    } catch (NoSuchProjectException e) {
      throw new CommitValidationException("Cannot find project " + project, e);
    }
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
}
