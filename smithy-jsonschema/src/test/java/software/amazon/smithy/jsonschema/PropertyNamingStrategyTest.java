/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jsonschema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.JsonNameTrait;

public class PropertyNamingStrategyTest {
    @Test
    public void defaultStrategyUsesJsonNameTraitIfConfigured() {
        PropertyNamingStrategy strategy = PropertyNamingStrategy.createDefaultStrategy();
        MemberShape member = MemberShape.builder()
                .id("smithy.example#Structure$foo")
                .target("a.b#C")
                .addTrait(new JsonNameTrait("FOO"))
                .build();
        StructureShape struct = StructureShape.builder().id("smithy.example#Structure").addMember(member).build();
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setUseJsonName(true);
        String memberName = strategy.toPropertyName(struct, member, config);

        assertThat(memberName, equalTo("FOO"));
    }

    @Test
    public void defaultStrategyIgnoresJsonNameTraitIfNotConfigured() {
        PropertyNamingStrategy strategy = PropertyNamingStrategy.createDefaultStrategy();
        MemberShape member = MemberShape.builder()
                .id("smithy.example#Structure$foo")
                .target("a.b#C")
                .addTrait(new JsonNameTrait("FOO"))
                .build();
        StructureShape struct = StructureShape.builder().id("smithy.example#Structure").addMember(member).build();
        JsonSchemaConfig config = new JsonSchemaConfig();
        String memberName = strategy.toPropertyName(struct, member, config);

        assertThat(memberName, equalTo("foo"));
    }

    @Test
    public void defaultStrategyUsesMemberName() {
        PropertyNamingStrategy strategy = PropertyNamingStrategy.createDefaultStrategy();
        MemberShape member = MemberShape.builder().id("smithy.example#Structure$foo").target("a.b#C").build();
        StructureShape struct = StructureShape.builder().id("smithy.example#Structure").addMember(member).build();
        JsonSchemaConfig config = new JsonSchemaConfig();
        String memberName = strategy.toPropertyName(struct, member, config);

        assertThat(memberName, equalTo("foo"));
    }

    @Test
    public void memberNameStrategyUsesMemberName() {
        PropertyNamingStrategy strategy = PropertyNamingStrategy.createMemberNameStrategy();
        MemberShape member = MemberShape.builder()
                .id("smithy.example#Structure$foo")
                .target("a.b#C")
                .addTrait(new JsonNameTrait("FOO"))
                .build();
        StructureShape struct = StructureShape.builder().id("smithy.example#Structure").addMember(member).build();
        JsonSchemaConfig config = new JsonSchemaConfig();
        String memberName = strategy.toPropertyName(struct, member, config);

        assertThat(memberName, equalTo("foo"));
    }
}
