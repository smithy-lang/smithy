/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.language.error;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.rulesengine.language.util.SourceLocationHelpers;

/**
 * Exception thrown when the ruleset is invalid.
 */
public class InvalidRulesException extends RuntimeException implements FromSourceLocation {
    private final transient SourceLocation sourceLocation;

    public InvalidRulesException(String message, FromSourceLocation location) {
        super(createMessage(message, location.getSourceLocation()));
        sourceLocation = location.getSourceLocation();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    private static String createMessage(String message, SourceLocation sourceLocation) {
        if (sourceLocation == SourceLocation.NONE) {
            return message;
        } else {
            String prettyLocation = SourceLocationHelpers.stackTraceForm(sourceLocation);
            return message.contains(prettyLocation) ? message : message + " (" + prettyLocation + ")";
        }
    }
}
