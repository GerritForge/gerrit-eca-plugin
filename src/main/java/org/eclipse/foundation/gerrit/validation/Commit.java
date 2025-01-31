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
 * Represents a Git commit with basic data and metadata about the revision.
 *
 * @author Martin Lowe
 */
@AutoValue
public abstract class Commit {
  public abstract String hash();

  public abstract String subject();

  public abstract String body();

  public abstract List<String> parents();

  public abstract GitUser author();

  public abstract GitUser committer();

  public abstract boolean head();

  public static JsonAdapter<Commit> jsonAdapter(Moshi moshi) {
    return new AutoValue_Commit.MoshiJsonAdapter(moshi);
  }

  static Builder builder() {
    return new AutoValue_Commit.Builder();
  }

  @Override
  public String toString() {
    StringBuilder builder2 = new StringBuilder();
    builder2.append("Commit [hash()=");
    builder2.append(hash());
    builder2.append(", subject()=");
    builder2.append(subject());
    builder2.append(", body()=");
    builder2.append(body());
    builder2.append(", parents()=");
    builder2.append(parents());
    builder2.append(", author()=");
    builder2.append(author());
    builder2.append(", committer()=");
    builder2.append(committer());
    builder2.append(", head()=");
    builder2.append(head());
    builder2.append("]");
    return builder2.toString();
  }

  @AutoValue.Builder
  abstract static class Builder {
    public abstract Builder hash(String hash);

    public abstract Builder subject(String subject);

    public abstract Builder body(String body);

    public abstract Builder parents(List<String> parents);

    public abstract Builder author(GitUser author);

    public abstract Builder committer(GitUser committer);

    public abstract Builder head(boolean head);

    abstract Commit build();
  }
}
