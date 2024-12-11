/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation.type;

import software.amazon.smithy.rulesengine.language.error.InnerParseError;

/**
 * The "endpoint" type, representing a valid client endpoint.
 */
public final class EndpointType extends AbstractType {
    EndpointType() {}

    @Override
    public EndpointType expectEndpointType() throws InnerParseError {
        return this;
    }

    @Override
    public String toString() {
        return "EndpointType[]";
    }
}
