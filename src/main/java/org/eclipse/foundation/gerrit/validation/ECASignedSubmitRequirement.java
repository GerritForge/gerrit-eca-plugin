// Copyright (C) 2025 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.eclipse.foundation.gerrit.validation;

import static java.util.Objects.requireNonNull;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.LegacySubmitRequirement;
import com.google.gerrit.entities.SubmitRecord;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.rules.SubmitRule;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class ECASignedSubmitRequirement implements SubmitRule {
  public static class ECASignedSubmitRequirementModule extends AbstractModule {
    @Override
    public void configure() {
      bind(SubmitRule.class)
          .annotatedWith(Exports.named("ECASignedSubmitRequirement"))
          .to(ECASignedSubmitRequirement.class);
    }
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final LegacySubmitRequirement SUBMIT_REQUIREMENT =
      LegacySubmitRequirement.builder().setFallbackText("ECASigned").setType("ECASigned").build();

  private final ProjectCache projectCache;

  @Inject
  ECASignedSubmitRequirement(ProjectCache projectCache) {
    this.projectCache = projectCache;
  }

  @Override
  public Optional<SubmitRecord> evaluate(ChangeData cd) {
    requireNonNull(cd, "changeData");
    Change change = cd.change();
    logger.atWarning().log(
        "Evaluate ECA signed for change %s in project %s",
        change.getId().get(), change.getProject().get());

    //    Project.NameKey project = cd.project();
    //    int changeId = cd.getId().get();
    //    if (change.isClosed()) {
    //      logger.atFine().log(
    //          "Project '%s': change #%d is closed therefore OWNERS submit requirements are
    // skipped.",
    //          project, changeId);
    //      return Optional.empty();
    //    }

    return Optional.of(ok());
  }

  private static SubmitRecord notReady(String ownersLabel, String missingApprovals) {
    SubmitRecord submitRecord = new SubmitRecord();
    submitRecord.status = SubmitRecord.Status.NOT_READY;
    submitRecord.errorMessage = missingApprovals;
    submitRecord.requirements = List.of(SUBMIT_REQUIREMENT);
    SubmitRecord.Label label = new SubmitRecord.Label();
    label.label = String.format("%s from owners", ownersLabel);
    label.status = SubmitRecord.Label.Status.NEED;
    submitRecord.labels = List.of(label);
    return submitRecord;
  }

  private static SubmitRecord ok() {
    SubmitRecord submitRecord = new SubmitRecord();
    submitRecord.status = SubmitRecord.Status.OK;
    submitRecord.requirements = List.of(SUBMIT_REQUIREMENT);
    return submitRecord;
  }

  private static SubmitRecord ruleError(String err) {
    SubmitRecord submitRecord = new SubmitRecord();
    submitRecord.status = SubmitRecord.Status.RULE_ERROR;
    submitRecord.errorMessage = err;
    return submitRecord;
  }
}
