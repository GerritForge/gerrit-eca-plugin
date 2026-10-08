/**
 * ***************************************************************************** Copyright (C) 2026
 * Eclipse Foundation
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************
 */
package org.eclipse.foundation.gerrit.validation;

import static com.google.common.truth.Truth.assertThat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import org.eclipse.foundation.gerrit.validation.CommitStatus.CommitStatusMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Pins the ECA JSON field names and that the records round-trip through Gson. */
@RunWith(JUnit4.class)
public class EcaJsonTest {
  private final Gson gson = new Gson();

  private static ValidationRequest sampleRequest() {
    return new ValidationRequest(
        "example/repo",
        List.of(
            new Commit(
                "abc123",
                "subject line",
                "full body",
                List.of("parent1", "parent2"),
                new GitUser("Jane Doe", "jane@example.com"),
                new GitUser("John Roe", "john@example.com"),
                /* head= */ true)),
        "gerrit",
        /* strictMode= */ true);
  }

  @Test
  public void validationRequest_serializesWithEcaFieldNames() {
    JsonObject json = JsonParser.parseString(gson.toJson(sampleRequest())).getAsJsonObject();

    assertThat(json.keySet()).containsExactly("repoUrl", "commits", "provider", "strictMode");
    assertThat(json.get("repoUrl").getAsString()).isEqualTo("example/repo");
    assertThat(json.get("provider").getAsString()).isEqualTo("gerrit");
    assertThat(json.get("strictMode").getAsBoolean()).isTrue();

    JsonObject commit = json.getAsJsonArray("commits").get(0).getAsJsonObject();
    assertThat(commit.keySet())
        .containsExactly("hash", "subject", "body", "parents", "author", "committer", "head");
    // the ECA contract spells the email field "mail", not "email".
    assertThat(commit.getAsJsonObject("author").keySet()).containsExactly("name", "mail");
    assertThat(commit.getAsJsonObject("author").get("mail").getAsString())
        .isEqualTo("jane@example.com");
  }

  @Test
  public void validationRequest_roundTrips() {
    ValidationRequest request = sampleRequest();

    ValidationRequest back = gson.fromJson(gson.toJson(request), ValidationRequest.class);

    assertThat(back).isEqualTo(request);
  }

  @Test
  public void validationResponse_deserializesEcaPayload() {
    String payload =
        "{"
            + "\"passed\":false,"
            + "\"errorCount\":1,"
            + "\"time\":\"2025-01-01T00:00:00Z\","
            + "\"trackedProject\":true,"
            + "\"commits\":{"
            + "  \"abc123\":{"
            + "    \"messages\":[{\"code\":0,\"message\":\"ok\"}],"
            + "    \"warnings\":[],"
            + "    \"errors\":[{\"code\":-1,\"message\":\"no ECA on file\"}]"
            + "  }"
            + "}"
            + "}";

    ValidationResponse response = gson.fromJson(payload, ValidationResponse.class);

    assertThat(response.passed()).isFalse();
    assertThat(response.errorCount()).isEqualTo(1);
    assertThat(response.trackedProject()).isTrue();
    assertThat(response.commits().keySet()).containsExactly("abc123");

    CommitStatus status = response.commits().get("abc123");
    assertThat(status.messages()).hasSize(1);
    assertThat(status.messages().get(0).message()).isEqualTo("ok");

    CommitStatusMessage error = status.errors().get(0);
    assertThat(error.code()).isEqualTo(-1);
    assertThat(error.message()).isEqualTo("no ECA on file");
  }

  @Test
  public void commitStatus_omittedLists_normalizeToEmpty() {
    CommitStatus status =
        gson.fromJson("{\"errors\":[{\"code\":-1,\"message\":\"x\"}]}", CommitStatus.class);

    // omitted arrays normalize to empty, not null.
    assertThat(status.messages()).isEmpty();
    assertThat(status.warnings()).isEmpty();
    assertThat(status.errors()).hasSize(1);
  }

  @Test
  public void validationResponse_offShapeBody_hasNullCommits() {
    // off-shape bodies deserialize to null commits, which the fail-closed guard rejects.
    assertThat(gson.fromJson("{}", ValidationResponse.class).commits()).isNull();
    assertThat(
            gson.fromJson("{\"error\":\"upstream unavailable\"}", ValidationResponse.class)
                .commits())
        .isNull();
  }
}
