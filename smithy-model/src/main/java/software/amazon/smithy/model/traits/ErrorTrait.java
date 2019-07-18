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

import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Indicates that a structure can be used as an error.
 */
public final class ErrorTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#error");

    public ErrorTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);

        if (!isClientError() && !isServerError()) {
            throw new SourceException(String.format(
                    "error trait must be set to client or server, found `%s`", getValue()), sourceLocation);
        }
    }

    public ErrorTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<ErrorTrait> {
        public Provider() {
            super(ID, ErrorTrait::new);
        }
    }

    /**
     * Returns the recommended default HTTP status code of the error.
     *
     * @return Returns the default HTTP status code.
     */
    public int getDefaultHttpStatusCode() {
        return isClientError() ? 400 : 500;
    }

    /**
     * @return Returns true if is a client error.
     */
    public boolean isClientError() {
        return getValue().equals("client");
    }

    /**
     * @return Returns true if it is a server error.
     */
    public boolean isServerError() {
        return getValue().equals("server");
    }
}
