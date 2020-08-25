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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class PlaneIndexTest {
    @Test
    public void computesControlPlaneVsDataPlane() {
        Model model = Model.assembler()
                .discoverModels(PlaneIndexTest.class.getClassLoader())
                .addImport(PlaneIndexTest.class.getResource("planes.json"))
                .assemble()
                .unwrap();

        ShapeId service = ShapeId.from("smithy.example#Service");
        PlaneIndex index = PlaneIndex.of(model);
        assertTrue(index.isControlPlane(service));
        assertFalse(index.isDataPlane(service));
        assertTrue(index.isPlaneDefined(service));

        assertFalse(index.isControlPlane(service, ShapeId.from("smithy.example#Operation1")));
        assertTrue(index.isDataPlane(service, ShapeId.from("smithy.example#Operation1")));
        assertTrue(index.isPlaneDefined(service, ShapeId.from("smithy.example#Operation1")));

        assertTrue(index.isControlPlane(service, ShapeId.from("smithy.example#Operation2")));
        assertTrue(index.isControlPlane(service, ShapeId.from("smithy.example#Operation3")));
        assertTrue(index.isDataPlane(service, ShapeId.from("smithy.example#Operation4")));
        assertTrue(index.isControlPlane(service, ShapeId.from("smithy.example#Operation5")));
        assertTrue(index.isControlPlane(service, ShapeId.from("smithy.example#Operation6")));
        assertTrue(index.isControlPlane(service, ShapeId.from("smithy.example#Operation7")));
        assertTrue(index.isDataPlane(service, ShapeId.from("smithy.example#Operation8")));

        assertTrue(index.isControlPlane(service, ShapeId.from("smithy.example#Resource1")));
        assertTrue(index.isDataPlane(service, ShapeId.from("smithy.example#Resource2")));

        // ID that doesn't exist.
        assertFalse(index.isDataPlane(service, ShapeId.from("smithy.example#InvalidOperationId")));

        // Service that doesn't exist.
        assertFalse(index.isControlPlane(ShapeId.from("foo.baz#Invalid")));
        assertFalse(index.isDataPlane(ShapeId.from("foo.baz#Invalid")));
        assertFalse(index.isPlaneDefined(ShapeId.from("foo.baz#Invalid")));
    }
}
