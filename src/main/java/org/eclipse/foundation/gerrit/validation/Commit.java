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
 * Represents a Git commit with basic data and metadata about the revision.
 *
 * @author Martin Lowe
 */
public record Commit(
    String hash,
    String subject,
    String body,
    List<String> parents,
    GitUser author,
    GitUser committer,
    boolean head) {}
