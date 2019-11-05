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

package software.amazon.smithy.jsonschema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;

public class RefStrategyTest {
    @Test
    public void defaultImplUsesDefaultPointer() {
        RefStrategy ref = RefStrategy.createDefaultStrategy();
        String pointer = ref.toPointer(ShapeId.from("smithy.example#Foo"), Node.objectNode());

        assertThat(pointer, equalTo("#/definitions/SmithyExampleFoo"));
    }

    @Test
    public void defaultImplUsesCustomPointerAndAppendsSlashWhenNecessary() {
        RefStrategy ref = RefStrategy.createDefaultStrategy();
        ObjectNode config = Node.objectNodeBuilder()
                .withMember(JsonSchemaConstants.DEFINITION_POINTER, Node.from("#/components/schemas"))
                .build();
        String pointer = ref.toPointer(ShapeId.from("smithy.example#Foo"), config);

        assertThat(pointer, equalTo("#/components/schemas/SmithyExampleFoo"));
    }

    @Test
    public void defaultImplUsesCustomPointerAndOmitsSlashWhenNecessary() {
        RefStrategy ref = RefStrategy.createDefaultStrategy();
        ObjectNode config = Node.objectNodeBuilder()
                .withMember(JsonSchemaConstants.DEFINITION_POINTER, Node.from("#/components/schemas"))
                .build();
        String pointer = ref.toPointer(ShapeId.from("smithy.example#Foo"), config);

        assertThat(pointer, equalTo("#/components/schemas/SmithyExampleFoo"));
    }

    @Test
    public void defaultImplStripsNamespacesWhenRequested() {
        RefStrategy ref = RefStrategy.createDefaultStrategy();
        ObjectNode config = Node.objectNodeBuilder()
                .withMember(JsonSchemaConstants.SMITHY_STRIP_NAMESPACES, true)
                .build();
        String pointer = ref.toPointer(ShapeId.from("smithy.example#Foo"), config);

        assertThat(pointer, equalTo("#/definitions/Foo"));
    }

    @Test
    public void defaultImplAddsRefWhenMember() {
        RefStrategy ref = RefStrategy.createDefaultStrategy();
        String pointer = ref.toPointer(ShapeId.from("smithy.example#Foo$bar"), Node.objectNode());

        assertThat(pointer, equalTo("#/definitions/SmithyExampleFooBarMember"));
    }

    @Test
    public void canDeconflictNames() {
        ObjectNode config = Node.objectNode();

        StringShape stringShape = StringShape.builder().id("com.foo#String").build();
        MemberShape pageScriptsListMember = MemberShape.builder()
                .id("com.foo#PageScripts$member")
                .target(stringShape)
                .build();
        ListShape pageScripts = ListShape.builder()
                .id("com.foo#PageScripts")
                .member(pageScriptsListMember)
                .build();
        MemberShape pageScriptsMember = MemberShape.builder()
                .id("com.foo#Page$scripts")
                .target(stringShape)
                .build();
        StructureShape page = StructureShape.builder()
                .id("com.foo#Page")
                .addMember(pageScriptsMember)
                .build();

        ShapeIndex index = ShapeIndex.builder()
                .addShapes(page, pageScriptsMember, pageScripts, pageScriptsListMember, stringShape)
                .build();

        RefStrategy strategy = RefStrategy.createDefaultDeconflictingStrategy(index, config);
        assertThat(strategy.toPointer(pageScriptsMember.getId(), config),
                   equalTo("#/definitions/ComFooPageScriptsMember"));
        assertThat(strategy.toPointer(pageScriptsListMember.getId(), config),
                   equalTo("#/definitions/ComFooPageScriptsMember2"));
    }

    @Test
    public void deconflictingStrategyPassesThroughToDelegate() {
        ObjectNode config = Node.objectNode();
        ShapeIndex index = ShapeIndex.builder().build();
        RefStrategy strategy = RefStrategy.createDefaultDeconflictingStrategy(index, config);

        assertThat(strategy.toPointer(ShapeId.from("com.foo#Nope"), config), equalTo("#/definitions/ComFooNope"));
    }
}
