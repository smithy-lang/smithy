/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.neighbor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;

public class WalkerTest {

    @Test
    public void getASetOfConnectedShapes() {
        // list-of-map-of-string-to-string
        StringShape string = StringShape.builder()
                .id("ns.foo#String")
                .build();
        MemberShape key = MemberShape.builder()
                .id("ns.foo#Map$key")
                .target(string.getId())
                .build();
        MemberShape value = MemberShape.builder()
                .id("ns.foo#Map$value")
                .target(string.getId())
                .build();
        MapShape map = MapShape.builder()
                .id("ns.foo#Map")
                .key(key)
                .value(value)
                .build();
        MemberShape listMember = MemberShape.builder()
                .id("ns.foo#List$member")
                .target(map.getId())
                .build();
        ListShape list = ListShape.builder()
                .id("ns.foo#List")
                .member(listMember)
                .build();
        Walker walker = new Walker(Model.builder()
                .addShape(list)
                .addShape(listMember)
                .addShape(map)
                .addShape(key)
                .addShape(value)
                .addShape(string)
                .build());
        Set<Shape> connected = walker.walkShapes(listMember);

        assertThat(connected, containsInAnyOrder(list, listMember, map, key, value, string));
    }

    @Test
    public void supportsCycles() {
        // list-of-map-of-string-to-list-of-...
        StringShape string = StringShape.builder()
                .id("ns.foo#String")
                .build();
        MemberShape key = MemberShape.builder()
                .id("ns.foo#Map$key")
                .target(string.getId())
                .build();
        MemberShape value = MemberShape.builder()
                .id("ns.foo#Map$value")
                .target("ns.foo#List") // cycles here
                .build();
        MapShape map = MapShape.builder()
                .id("ns.foo#Map")
                .key(key)
                .value(value)
                .build();
        MemberShape listMember = MemberShape.builder()
                .id("ns.foo#List$member")
                .target(map.getId())
                .build();
        ListShape list = ListShape.builder()
                .id("ns.foo#List")
                .member(listMember)
                .build();
        Walker walker = new Walker(Model.builder()
                .addShape(list)
                .addShape(listMember)
                .addShape(map)
                .addShape(key)
                .addShape(value)
                .addShape(string)
                .build());
        Set<Shape> connected = walker.walkShapes(listMember);

        assertThat(connected, containsInAnyOrder(list, listMember, map, key, value, string));
    }

    @Test
    public void yieldsUniqueShapes() {
        OperationShape readOperation = OperationShape.builder()
                .id("smithy.example#Read")
                .build();
        ResourceShape resource = ResourceShape.builder()
                .id("smithy.example#Resource")
                .read(readOperation.getId())
                .build();
        Model model = Model.builder().addShapes(readOperation, resource).build();
        Walker walker = new Walker(model);

        List<Shape> shapes = new ArrayList<>();
        walker.iterateShapes(resource).forEachRemaining(shapes::add);

        assertThat(shapes, containsInAnyOrder(readOperation, resource));
    }
}
