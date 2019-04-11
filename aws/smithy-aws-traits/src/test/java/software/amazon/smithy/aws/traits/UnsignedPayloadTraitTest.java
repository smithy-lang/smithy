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

package software.amazon.smithy.aws.traits;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;

public class UnsignedPayloadTraitTest {
    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("unsigned-request-payload.json"))
                .assemble()
                .unwrap();

        assertTrue(result.getShapeIndex()
                .getShape(ShapeId.from("ns.foo#Unsigned1"))
                .flatMap(shape -> shape.getTrait(UnsignedPayloadTrait.class))
                .isPresent());

        assertTrue(result.getShapeIndex()
                .getShape(ShapeId.from("ns.foo#Unsigned2"))
                .flatMap(shape -> shape.getTrait(UnsignedPayloadTrait.class))
                .filter(trait -> trait.getValues().equals(ListUtils.of("aws.v4")))
                .isPresent());
    }
}
