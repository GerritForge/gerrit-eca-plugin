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
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

@Singleton
public class ECASignedSubmitRequirement extends BaseEclipseCommitValidator {
  private final GitRepositoryManager repoManager;
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Inject
  public ECASignedSubmitRequirement(GitRepositoryManager repoManager) {
    this.repoManager = repoManager;
  }

  public boolean evaluate(ChangeData cd) {
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
      return true;
    }

    try (Repository repo = repoManager.openRepository(project);
        RevWalk rw = new RevWalk(repo)) {
      RevCommit commit = rw.lookupCommit(cd.currentPatchSet().commitId());
      rw.parseBody(commit);

      ValidationResponse response =
          validate(change.getProject(), cd.getAuthor(), cd.getCommitter(), commit);

      if (response.errorCount() > 0 && response.trackedProject()) {
        return false;
      }
    } catch (IOException | CommitValidationException e) {
      logger.atSevere().withCause(e).log(
          "Could not evaluate ECA Signed Submit Requirement for Project '%s': change #%d.",
          project, changeId);
      return false;
    }

    return true;
  }
}
