package org.eclipse.foundation.gerrit.validation;

import com.google.inject.AbstractModule;

public class ECAValidationModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new ECASignedHasOperand.ECASignedHasOperandModule());
    install(new ECASignedSubmitRequirement.ECASignedSubmitRequirementModule());
  }
}
