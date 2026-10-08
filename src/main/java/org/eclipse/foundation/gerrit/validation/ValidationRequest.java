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
 * Represents a request to validate a list of commits.
 *
 * @author Martin Lowe
 */
public record ValidationRequest(
    String repoUrl, List<Commit> commits, String provider, boolean strictMode) {}
