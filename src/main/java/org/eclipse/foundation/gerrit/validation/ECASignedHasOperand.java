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

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.index.query.Predicate;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.gerrit.server.query.change.ChangeQueryBuilder.ChangeHasOperandFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/** Class contributing an "signed_eca-validation" operand to the "has" predicate. */
@Singleton
class ECASignedHasOperand implements ChangeHasOperandFactory {
  static final String OPERAND = "signed";

  static class ECASignedHasOperandModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(ChangeHasOperandFactory.class)
          .annotatedWith(Exports.named(OPERAND))
          .to(ECASignedHasOperand.class);
    }
  }

  private final ECASignedHasPredicate ECASignedHasPredicate;

  @Inject
  ECASignedHasOperand(ECASignedHasPredicate ECASignedHasPredicate) {
    this.ECASignedHasPredicate = ECASignedHasPredicate;
  }

  @Override
  public Predicate<ChangeData> create(ChangeQueryBuilder builder) throws QueryParseException {
    return ECASignedHasPredicate;
  }
}
