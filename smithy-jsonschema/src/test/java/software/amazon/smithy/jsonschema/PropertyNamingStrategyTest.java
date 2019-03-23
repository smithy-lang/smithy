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
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.JsonNameTrait;

public class PropertyNamingStrategyTest {
    @Test
    public void defaultStrategyUsesJsonNameTraitIfConfigured() {
        var strategy = PropertyNamingStrategy.createDefaultStrategy();
        var member = MemberShape.builder()
                .id("smithy.example#Structure$foo")
                .target("a.b#C")
                .addTrait(new JsonNameTrait("FOO"))
                .build();
        var struct = StructureShape.builder().id("smithy.example#Structure").addMember(member).build();
        var config = Node.objectNodeBuilder().withMember(JsonSchemaConstants.SMITHY_USE_JSON_NAME, true).build();
        var memberName = strategy.toPropertyName(struct, member, config);

        assertThat(memberName, equalTo("FOO"));
    }

    @Test
    public void defaultStrategyIgnoresJsonNameTraitIfNotConfigured() {
        var strategy = PropertyNamingStrategy.createDefaultStrategy();
        var member = MemberShape.builder()
                .id("smithy.example#Structure$foo")
                .target("a.b#C")
                .addTrait(new JsonNameTrait("FOO"))
                .build();
        var struct = StructureShape.builder().id("smithy.example#Structure").addMember(member).build();
        var config = Node.objectNode();
        var memberName = strategy.toPropertyName(struct, member, config);

        assertThat(memberName, equalTo("foo"));
    }

    @Test
    public void defaultStrategyUsesMemberName() {
        var strategy = PropertyNamingStrategy.createDefaultStrategy();
        var member = MemberShape.builder().id("smithy.example#Structure$foo").target("a.b#C").build();
        var struct = StructureShape.builder().id("smithy.example#Structure").addMember(member).build();
        var config = Node.objectNode();
        var memberName = strategy.toPropertyName(struct, member, config);

        assertThat(memberName, equalTo("foo"));
    }

    @Test
    public void memberNameStrategyUsesMemberName() {
        var strategy = PropertyNamingStrategy.createMemberNameStrategy();
        var member = MemberShape.builder()
                .id("smithy.example#Structure$foo")
                .target("a.b#C")
                .addTrait(new JsonNameTrait("FOO"))
                .build();
        var struct = StructureShape.builder().id("smithy.example#Structure").addMember(member).build();
        var config = Node.objectNode();
        var memberName = strategy.toPropertyName(struct, member, config);

        assertThat(memberName, equalTo("foo"));
    }
}
