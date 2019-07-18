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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Restricts string values to a specified regular expression.
 */
public final class PatternTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("smithy.api#pattern");

    private final Pattern pattern;

    public PatternTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
        this.pattern = compilePattern(value, sourceLocation);
    }

    public PatternTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    public static final class Provider extends StringTrait.Provider<PatternTrait> {
        public Provider() {
            super(ID, PatternTrait::new);
        }
    }

    /**
     * Gets the regex pattern.
     *
     * @return returns compiled regular expression.
     */
    public Pattern getPattern() {
        return pattern;
    }

    private static Pattern compilePattern(String value, FromSourceLocation sourceLocation) {
        try {
            return Pattern.compile(value);
        } catch (PatternSyntaxException e) {
            throw new SourceException(
                    "Invalid pattern trait regular expression: `" + value + "`. " + e.getMessage(),
                    sourceLocation);
        }
    }
}
