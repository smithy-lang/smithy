/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;

public class MavenRepositoryTest {
    @Test
    public void validatesHttpCredentialsValidColon() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            MavenRepository.builder().url("https://example.com").httpCredentials(":").build();
        });
    }

    @Test
    public void validatesHttpCredentialsHasColon() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            MavenRepository.builder().url("https://example.com").httpCredentials("foo").build();
        });
    }

    @Test
    public void validatesHttpCredentialsColonNotAtBeginning() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            MavenRepository.builder().url("https://example.com").httpCredentials(":foo").build();
        });
    }

    @Test
    public void hasUrlAndAuth() {
        MavenRepository repo1 = MavenRepository.builder()
                .url("https://example.com")
                .httpCredentials("user:pass")
                .build();
        MavenRepository repo2 = repo1.toBuilder().build();

        assertThat(repo1.getUrl(), equalTo("https://example.com"));
        assertThat(repo1.getHttpCredentials(), equalTo(Optional.of("user:pass")));
        assertThat(repo2.getUrl(), equalTo("https://example.com"));
        assertThat(repo2.getHttpCredentials(), equalTo(Optional.of("user:pass")));
        assertThat(repo1, equalTo(repo1));
        assertThat(repo1, equalTo(repo2));
    }

    @Test
    public void loadsFromNode() {
        MavenRepository repo = MavenRepository.fromNode(Node.objectNodeBuilder()
                .withMember("url", "https://example.com")
                .withMember("httpCredentials", "user:pass")
                .build());

        assertThat(repo.getUrl(), equalTo("https://example.com"));
        assertThat(repo.getHttpCredentials(), equalTo(Optional.of("user:pass")));
    }
}
