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

import java.util.Locale;
import java.util.Set;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.utils.SetUtils;

/**
 * Binds a member to an HTTP header.
 */
public final class HttpHeaderTrait extends StringTrait {
    private static final String TRAIT = "smithy.api#httpHeader";
    private static final Set<String> BLACKLIST = SetUtils.of(
            "authorization",
            "connection",
            "content-length",
            "expect",
            "host",
            "max-forwards",
            "proxy-authenticate",
            "server",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "user-agent",
            "www-authenticate",
            "x-forwarded-for");

    public HttpHeaderTrait(String value, SourceLocation sourceLocation) {
        super(TRAIT, value, sourceLocation);

        if (getValue().isEmpty()) {
            throw new SourceException("httpHeader field name binding must not be empty", getSourceLocation());
        }

        if (BLACKLIST.contains(getValue().toLowerCase(Locale.US))) {
            throw new SourceException("httpHeader cannot be set to `" + getValue() + "`", getSourceLocation());
        }
    }

    public HttpHeaderTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<HttpHeaderTrait> {
        public Provider() {
            super(TRAIT, HttpHeaderTrait::new);
        }
    }
}
