/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;

public class ErrorTraitTest {
    @Test
    public void loadsTrait() {
        ErrorTrait trait = new ErrorTrait("client", SourceLocation.NONE);
        assertThat(trait.toNode(), equalTo(Node.from("client")));
        assertThat(trait.getValue(), equalTo("client"));
    }

    @Test
    public void determinesIfClientOrServerError() {
        ErrorTrait client = new ErrorTrait("client", SourceLocation.NONE);
        ErrorTrait server = new ErrorTrait("server", SourceLocation.NONE);

        assertTrue(client.isClientError());
        assertFalse(client.isServerError());
        assertTrue(server.isServerError());
        assertFalse(server.isClientError());
    }

    @Test
    public void returnsDefaultStatusCodes() {
        ErrorTrait a = new ErrorTrait("client", SourceLocation.NONE);
        ErrorTrait b = new ErrorTrait("server", SourceLocation.NONE);

        assertEquals(400, a.getDefaultHttpStatusCode());
        assertEquals(500, b.getDefaultHttpStatusCode());
    }
}
