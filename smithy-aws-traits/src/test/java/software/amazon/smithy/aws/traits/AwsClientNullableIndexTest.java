/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.RequiredTrait;

public class AwsClientNullableIndexTest {
    @Test
    public void takesClientOptionalIntoAccount() {
        StringShape str = StringShape.builder().id("smithy.example#Str").build();
        StructureShape struct = StructureShape.builder()
                .id("smithy.example#Struct")
                .addMember("foo", str.getId(), b -> b.addTrait(new ClientOptionalTrait())
                        .addTrait(new DefaultTrait())
                        .build())
                .addMember("bar", str.getId(), b -> b.addTrait(new ClientOptionalTrait())
                        .addTrait(new RequiredTrait())
                        .build())
                .addMember("baz", str.getId(), b -> b.addTrait(new ClientOptionalTrait()).build())
                .addMember("bam", str.getId(), b -> b.addTrait(new RequiredTrait()).build())
                .addMember("boo", str.getId(), b -> b.addTrait(new DefaultTrait()).build())
                .build();

        Model model = Model.builder().addShapes(str, struct).build();
        AwsClientNullableIndex nullableIndex = AwsClientNullableIndex.of(model);

        assertThat(nullableIndex.isMemberOptional(struct.getMember("foo").get()), is(true));
        assertThat(nullableIndex.isMemberOptional(struct.getMember("bar").get()), is(true));
        assertThat(nullableIndex.isMemberOptional(struct.getMember("baz").get()), is(true));
        assertThat(nullableIndex.isMemberOptional(struct.getMember("bam").get()), is(false));
        assertThat(nullableIndex.isMemberOptional(struct.getMember("boo").get()), is(false));
    }

}
