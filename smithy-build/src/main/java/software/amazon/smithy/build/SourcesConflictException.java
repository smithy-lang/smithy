/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

/**
 * Thrown when sources conflict in the source plugin.
 */
public class SourcesConflictException extends SmithyBuildException {
    public SourcesConflictException(String message) {
        super(message);
    }
}
