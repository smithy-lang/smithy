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

package software.amazon.smithy.model.loader;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;

/**
 * Thrown when the syntax of the IDL is invalid.
 */
public class ModelSyntaxException extends SourceException {
    public ModelSyntaxException(String message, int line, int column) {
        this(message, SourceLocation.NONE.getFilename(), line, column);
    }

    public ModelSyntaxException(String message, String filename, int line, int column) {
        this(message, new SourceLocation(filename, line, column));
    }

    public ModelSyntaxException(String message, FromSourceLocation sourceLocation) {
        super(message, sourceLocation);
    }
}
