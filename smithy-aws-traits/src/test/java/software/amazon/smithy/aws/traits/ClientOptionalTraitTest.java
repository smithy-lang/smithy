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
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

public class ClientOptionalTraitTest {

    private Model getModel() {
        return Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("client-optional-trait.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void loadsTrait() {
        Model model = getModel();
        StructureShape struct = model.expectShape(ShapeId.from("smithy.example#Foo"), StructureShape.class);



        assertThat(struct.getMember("baz").get().hasTrait(ClientOptionalTrait.class), is(true));
        assertThat(struct.getMember("bar").get().hasTrait(ClientOptionalTrait.class), is(true));
        assertThat(struct.getMember("bam").get().hasTrait(ClientOptionalTrait.class), is(true));
    }
}
