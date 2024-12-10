/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.mqtt.traits;

/**
 * Thrown when an MQTT topic is malformed.
 */
public class TopicSyntaxException extends RuntimeException {
    public TopicSyntaxException(String message) {
        super(message);
    }
}
