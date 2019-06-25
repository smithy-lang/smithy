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

/**
 * Throw when an error occurs with the IDL's {@code use} statements.
 */
public class UseException extends SourceException {
    public UseException(String message, FromSourceLocation sourceLocation) {
        super(message, sourceLocation.getSourceLocation());
    }
}
