/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
