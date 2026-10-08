/**
 * ***************************************************************************** Copyright (C) 2020
 * Eclipse Foundation
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************
 */
package org.eclipse.foundation.gerrit.validation;

import java.util.List;

/**
 * Contains information generated about a commit that was submitted for validation to the API.
 *
 * @author Martin Lowe
 */
public record CommitStatus(
    List<CommitStatusMessage> messages,
    List<CommitStatusMessage> warnings,
    List<CommitStatusMessage> errors) {

  // omitted JSON arrays deserialize to null; normalize to empty.
  public CommitStatus {
    messages = messages == null ? List.of() : messages;
    warnings = warnings == null ? List.of() : warnings;
    errors = errors == null ? List.of() : errors;
  }

  /**
   * Represents a message with an associated error or success status code.
   *
   * @author Martin Lowe
   */
  public record CommitStatusMessage(int code, String message) {}
}
