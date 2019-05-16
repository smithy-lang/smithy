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

package software.amazon.smithy.model;

/**
 * An exception that can be traced back to a {@link SourceLocation}.
 */
public class SourceException extends RuntimeException implements FromSourceLocation {
    private final transient SourceLocation sourceLocation;

    public SourceException(String message, FromSourceLocation sourceLocation, Throwable cause) {
        super(createMessage(message, sourceLocation.getSourceLocation()), cause);
        this.sourceLocation = sourceLocation.getSourceLocation();
    }

    public SourceException(String message, FromSourceLocation sourceLocation) {
        super(createMessage(message, sourceLocation.getSourceLocation()));
        this.sourceLocation = sourceLocation.getSourceLocation();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    private static String createMessage(String message, SourceLocation sourceLocation) {
        if (sourceLocation == SourceLocation.NONE) {
            return message;
        } else {
            String asString = sourceLocation.toString();
            return message.contains(asString) ? message : message + " (" + asString + ")";
        }
    }

    /**
     * Retrieves the message for this exception without the appended source
     * location.
     *
     * @return The trimmed message.
     */
    public String getMessageWithoutLocation() {
        return getMessage().replace(" (" + sourceLocation + ")", "");
    }
}
