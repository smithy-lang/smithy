/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.StringShape;

public class IncludeNamespacesTest {

    @Test
    public void removesShapesNotInNamespaces() {
        StringShape string1 = StringShape.builder().id("ns.foo#yuck").build();
        StringShape string2 = StringShape.builder().id("ns.foo#qux").build();
        StringShape string3 = StringShape.builder().id("ns.bar#yuck").build();
        StringShape string4 = StringShape.builder().id("ns.qux#yuck").build();
        Model model = Model.builder().addShapes(string1, string2, string3, string4).build();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("namespaces", Node.fromStrings("ns.foo", "ns.bar")))
                .build();
        Model result = new IncludeNamespaces().transform(context);

        assertThat(result.getShape(string1.getId()), not(Optional.empty()));
        assertThat(result.getShape(string2.getId()), not(Optional.empty()));
        assertThat(result.getShape(string3.getId()), not(Optional.empty()));
        assertThat(result.getShape(string4.getId()), is(Optional.empty()));
    }
}
