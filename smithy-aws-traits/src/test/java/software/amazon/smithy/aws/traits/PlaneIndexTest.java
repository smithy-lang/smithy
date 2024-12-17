/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
