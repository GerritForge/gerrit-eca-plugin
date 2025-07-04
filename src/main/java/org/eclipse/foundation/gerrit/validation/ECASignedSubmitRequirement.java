/**
 * ***************************************************************************** Copyright (c) 2025
 * Eclipse Foundation and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Antonio Barone (GerritForge)
 * *****************************************************************************
 */
package org.eclipse.foundation.gerrit.validation;

import static java.util.Objects.requireNonNull;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.SubmitRecord;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.rules.SubmitRule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

@Singleton
public class ECASignedSubmitRequirement extends BaseEclipseCommitValidator implements SubmitRule {
  private final GitRepositoryManager repoManager;
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Inject
  public ECASignedSubmitRequirement(GitRepositoryManager repoManager) {
    this.repoManager = repoManager;
  }

  @Override
  public Optional<SubmitRecord> evaluate(ChangeData cd) {
    requireNonNull(cd, "changeData");
    Change change = cd.change();
    logger.atWarning().log(
        "Evaluate ECA signed for change %s in project %s",
        change.getId().get(), change.getProject().get());

    Project.NameKey project = cd.project();
    int changeId = cd.getId().get();
    if (change.isClosed()) {
      logger.atFine().log(
          "Project '%s': change #%d is closed therefore ECA sign requirement is skipped.",
          project, changeId);
      return Optional.empty();
    }

    try (Repository repo = repoManager.openRepository(project);
        RevWalk rw = new RevWalk(repo)) {
      RevCommit commit = rw.lookupCommit(cd.currentPatchSet().commitId());
      rw.parseBody(commit);

      ValidationResponse response =
          validate(change.getProject(), cd.getAuthor(), cd.getCommitter(), commit);

      if (response.errorCount() > 0 && response.trackedProject()) {
        return Optional.of(notReady("An Eclipse Contributor Agreement is required."));
      }
    } catch (IOException | CommitValidationException e) {
      logger.atSevere().withCause(e).log(
          "Could not evaluate ECA Signed Submit Requirement for Project '%s': change #%d.",
          project, changeId);
      return Optional.of(notReady("Could not evaluate submit requirement"));
    }

    return Optional.of(ok());
  }

  private static SubmitRecord notReady(String errorMessage) {
    SubmitRecord submitRecord = new SubmitRecord();
    submitRecord.status = SubmitRecord.Status.NOT_READY;
    submitRecord.errorMessage = errorMessage;
    return submitRecord;
  }

  private static SubmitRecord ok() {
    SubmitRecord submitRecord = new SubmitRecord();
    submitRecord.status = SubmitRecord.Status.OK;
    return submitRecord;
  }

  private static SubmitRecord ruleError(String err) {
    SubmitRecord submitRecord = new SubmitRecord();
    submitRecord.status = SubmitRecord.Status.RULE_ERROR;
    submitRecord.errorMessage = err;
    return submitRecord;
  }
}
