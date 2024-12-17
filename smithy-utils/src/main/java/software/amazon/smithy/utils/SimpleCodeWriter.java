/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

/**
 * Helper class for generating code.
 *
 * <p>This class is a general purpose implementation of {@link AbstractCodeWriter}.
 */
public class SimpleCodeWriter extends AbstractCodeWriter<SimpleCodeWriter> {
    public SimpleCodeWriter() {
        super();
        trimTrailingSpaces();
    }
}
