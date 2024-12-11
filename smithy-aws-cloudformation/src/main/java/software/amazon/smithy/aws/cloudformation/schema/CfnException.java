/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema;

public class CfnException extends RuntimeException {

    public CfnException(RuntimeException e) {
        super(e);
    }

    public CfnException(String message) {
        super(message);
    }

    public CfnException(String message, Throwable previous) {
        super(message, previous);
    }
}
