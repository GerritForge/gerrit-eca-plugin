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

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.inject.AbstractModule;

public class ECAValidationModule extends AbstractModule {
  @Override
  protected void configure() {
    DynamicSet.bind(binder(), CommitValidationListener.class).to(EclipseCommitValidationListener.class);
  }
}
