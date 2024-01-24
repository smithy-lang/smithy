/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.aws.smoketests.model;

import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Concrete vendor params to apply to AWS services by default.
 */
public final class AwsVendorParams extends BaseAwsVendorParams {
    public static final ShapeId ID = ShapeId.from("aws.test#AwsVendorParams");

    AwsVendorParams(ObjectNode node) {
        super(node);
    }
}
