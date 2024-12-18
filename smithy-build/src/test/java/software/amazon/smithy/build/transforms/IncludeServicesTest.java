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
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.StringShape;

public class IncludeServicesTest {

    @Test
    public void removesTraitsNotInList() {
        ServiceShape serviceA = ServiceShape.builder().id("ns.foo#baz").version("1").build();
        ServiceShape serviceB = ServiceShape.builder().id("ns.foo#bar").version("1").build();
        StringShape string = StringShape.builder().id("ns.foo#yuck").build();
        Model model = Model.builder().addShapes(serviceA, serviceB, string).build();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("services", Node.fromStrings("ns.foo#baz")))
                .build();
        Model result = new IncludeServices().transform(context);

        assertThat(result.getShape(serviceA.getId()), not(Optional.empty()));
        assertThat(result.getShape(string.getId()), not(Optional.empty()));
        assertThat(result.getShape(serviceB.getId()), is(Optional.empty()));
    }
}
