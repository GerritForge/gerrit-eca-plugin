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

import java.util.Map;

/**
 * Represents an internal response for a call to this API.
 *
 * @author Martin Lowe
 */
public record ValidationResponse(
    boolean passed,
    int errorCount,
    String time,
    Map<String, CommitStatus> commits,
    boolean trackedProject) {}
