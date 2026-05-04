/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class TlsConfigTest {
    @Test
    public void defaultsToEmpty() {
        TlsConfig config = TlsConfig.builder().build();

        assertFalse(config.getInsecureSkipVerification().isPresent());
    }

    @Test
    public void roundTripsViaBuilder() {
        TlsConfig config = TlsConfig.builder()
                .insecureSkipVerification(true)
                .build();

        assertThat(config.toBuilder().build(), equalTo(config));
    }

}
