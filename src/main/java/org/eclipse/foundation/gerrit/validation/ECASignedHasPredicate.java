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

import com.google.gerrit.entities.SubmitRecord;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.project.SubmitRequirementEvaluationException;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.SubmitRequirementPredicate;
import com.google.gerrit.server.rules.SubmitRule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;

/**
 * A predicate that checks if a signed Eclipse Contributor Agreement. This predicate wraps the
 * existing {@link ECASignedSubmitRequirement} (that implements the {@link SubmitRule}) to perform
 * the logic.
 */
@Singleton
class ECASignedHasPredicate extends SubmitRequirementPredicate {

  private final ECASignedSubmitRequirement ecaSignedSubmitRequirement;

  @Inject
  ECASignedHasPredicate(
      @PluginName String pluginName, ECASignedSubmitRequirement ownersSubmitRequirement) {
    super("has", ECASignedHasOperand.OPERAND + "_" + pluginName);
    this.ecaSignedSubmitRequirement = ownersSubmitRequirement;
  }

  @Override
  public boolean match(ChangeData cd) {
    Optional<SubmitRecord> submitRecord = ecaSignedSubmitRequirement.evaluate(cd);
    return submitRecord
        .map(
            sr -> {
              if (sr.status == SubmitRecord.Status.RULE_ERROR) {
                throw new SubmitRequirementEvaluationException(sr.errorMessage);
              }
              return sr.status == SubmitRecord.Status.OK;
            })
        .orElse(true);
  }

  /**
   * Assuming that it is similarly expensive to calculate this as the 'code-owners' plugin hence
   * giving the same value.
   */
  @Override
  public int getCost() {
    return 10;
  }
}
