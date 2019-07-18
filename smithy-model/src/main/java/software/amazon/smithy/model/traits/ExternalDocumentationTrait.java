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

import java.net.MalformedURLException;
import java.net.URL;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Provides a link to external documentation of a service or operation.
 */
public final class ExternalDocumentationTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#externalDocumentation");

    public ExternalDocumentationTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
        validateUrl(value, sourceLocation);
    }

    public ExternalDocumentationTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<ExternalDocumentationTrait> {
        public Provider() {
            super(ID, ExternalDocumentationTrait::new);
        }
    }

    private static String validateUrl(String url, SourceLocation location) {
        try {
            new URL(url);
            return url;
        } catch (MalformedURLException e) {
            throw new SourceException("externalDocumentation must be a valid URL. Found " + url, location);
        }
    }
}
