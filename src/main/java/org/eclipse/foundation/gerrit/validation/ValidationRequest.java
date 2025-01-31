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

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import java.util.List;

/**
 * Represents a request to validate a list of commits.
 *
 * @author Martin Lowe
 */
@AutoValue
public abstract class ValidationRequest {
  public abstract String repoUrl();

  public abstract List<Commit> commits();

  public abstract String provider();

  public abstract boolean strictMode();

  public static JsonAdapter<ValidationRequest> jsonAdapter(Moshi moshi) {
    return new AutoValue_ValidationRequest.MoshiJsonAdapter(moshi);
  }

  @Override
  public String toString() {
    StringBuilder builder2 = new StringBuilder();
    builder2.append("ValidationRequest [repoUrl()=");
    builder2.append(repoUrl());
    builder2.append(", commits()=");
    builder2.append(commits());
    builder2.append(", provider()=");
    builder2.append(provider());
    builder2.append(", strictMode()=");
    builder2.append(strictMode());
    builder2.append("]");
    return builder2.toString();
  }

  static Builder builder() {
    return new AutoValue_ValidationRequest.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {
    public abstract Builder repoUrl(String repoUrl);

    public abstract Builder commits(List<Commit> commits);

    public abstract Builder provider(String provider);

    public abstract Builder strictMode(boolean strictMode);

    abstract ValidationRequest build();
  }
}
