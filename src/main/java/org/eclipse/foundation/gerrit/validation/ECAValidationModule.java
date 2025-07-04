package org.eclipse.foundation.gerrit.validation;

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.inject.AbstractModule;

public class ECAValidationModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new ECASignedHasOperand.ECASignedHasOperandModule());
    install(new ECASignedSubmitRequirement.ECASignedSubmitRequirementModule());
    DynamicSet.bind(binder(), CommitValidationListener.class).to(EclipseCommitValidationListener.class);
  }
}
