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
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

abstract class BaseEclipseCommitValidator {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final int DEFAULT_API_TIMEOUT_SECS = 20;
  private static final URI ECA_VALIDATION_URI = URI.create("https://api.eclipse.org/git/eca");

  final String pluginName;
  final PluginConfigFactory pluginCfgFactory;
  private final Gson gson = new Gson();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  BaseEclipseCommitValidator(PluginConfigFactory pluginCfgFactory, @PluginName String pluginName) {
    this.pluginCfgFactory = pluginCfgFactory;
    this.pluginName = pluginName;
  }

  /**
   * Validate a single commit (this listener will be invoked for each commit in a push operation).
   *
   * <p>Fails closed: network errors, timeouts and malformed responses raise {@link
   * CommitValidationException}.
   */
  ValidationResponse validate(
      Project.NameKey project,
      PersonIdent authorIdent,
      PersonIdent committerIdent,
      RevCommit commit)
      throws CommitValidationException {
    ValidationRequest request =
        new ValidationRequest(
            project.toString(),
            List.of(getRequestCommit(commit, authorIdent, committerIdent)),
            "gerrit",
            /* strictMode= */ true);
    logger.atFine().log("Request object: %s", request);

    int apiTimeout;
    try {
      apiTimeout =
          pluginCfgFactory
              .getFromProjectConfigWithInheritance(project, pluginName)
              .getInt("apiTimeout", DEFAULT_API_TIMEOUT_SECS);
    } catch (NoSuchProjectException e) {
      throw new CommitValidationException(
          "No such project",
          new CommitValidationMessage("Cannot find project " + project, true),
          e);
    }

    HttpRequest httpRequest =
        HttpRequest.newBuilder(ECA_VALIDATION_URI)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request), StandardCharsets.UTF_8))
            .build();

    // overall deadline (connect, headers, body); <= 0 uses the default.
    int deadlineSecs = apiTimeout > 0 ? apiTimeout : DEFAULT_API_TIMEOUT_SECS;
    CompletableFuture<HttpResponse<String>> future =
        httpClient.sendAsync(
            httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    HttpResponse<String> httpResponse;
    try {
      httpResponse = future.get(deadlineSecs, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      future.cancel(true);
      logger.atSevere().withCause(e).log("ECA validation timed out after %ss", deadlineSecs);
      throw new CommitValidationException(
          "Verification of the commit timed out",
          new CommitValidationMessage("ECA validation timed out after " + deadlineSecs + "s", true),
          e);
    } catch (ExecutionException e) {
      logger.atSevere().withCause(e).log("%s", e.getMessage());
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      throw new CommitValidationException(
          "An error happened while checking commit",
          new CommitValidationMessage(cause.getMessage(), true),
          e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.atSevere().withCause(e).log("%s", e.getMessage());
      throw new CommitValidationException(
          "Verification of commit has been interrupted",
          new CommitValidationMessage(e.getMessage(), true),
          e);
    }

    ValidationResponse response;
    try {
      response = gson.fromJson(httpResponse.body(), ValidationResponse.class);
    } catch (JsonSyntaxException e) {
      logger.atSevere().withCause(e).log("%s", e.getMessage());
      throw new CommitValidationException(
          "An error happened while retrieving validation response, please contact the"
              + " administrator if this error persists",
          e);
    }
    // an off-shape or error body deserializes to null commits; reject it rather than pass.
    if (response == null || response.commits() == null) {
      throw new CommitValidationException(
          "The ECA service returned an incomplete or malformed response, please contact the"
              + " administrator if this error persists");
    }
    logger.atFine().log("Response object: %s", response);
    return response;
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
    RevCommit[] parents = src.getParents();
    List<String> parentHashes = new ArrayList<>(parents.length);
    for (RevCommit parent : parents) {
      parentHashes.add(parent.name());
    }
    return new Commit(
        src.name(),
        src.getShortMessage(),
        src.getFullMessage(),
        parentHashes,
        new GitUser(author.getName(), author.getEmailAddress()),
        new GitUser(committer.getName(), committer.getEmailAddress()),
        /* head= */ true);
  }
}
