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

package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.traits.AuthenticationTrait;
import software.amazon.smithy.model.transform.ModelTransformer;

public class IncludeAuthenticationTest {

    @Test
    public void filtersUnsupportedAuthSchemes() {
        ServiceShape service1 = ServiceShape.builder()
                .id("ns.foo#foo")
                .version("1")
                .addTrait(AuthenticationTrait.builder()
                        .putAuthenticationScheme("foo", AuthenticationTrait.AuthScheme.builder().build())
                        .putAuthenticationScheme("baz", AuthenticationTrait.AuthScheme.builder().build()).build())
                .build();
        ServiceShape service2 = ServiceShape.builder()
                .id("ns.foo#baz")
                .version("1")
                .build();
        ServiceShape service3 = ServiceShape.builder()
                .id("ns.foo#bar")
                .version("1")
                .addTrait(AuthenticationTrait.builder()
                        .putAuthenticationScheme("foo", AuthenticationTrait.AuthScheme.builder().build()).build())
                .build();
        ShapeIndex index = ShapeIndex.builder()
                .addShapes(service1, service2, service3)
                .build();
        Model model = Model.builder()
                .shapeIndex(index)
                .build();
        Model result = new IncludeAuthentication()
                .createTransformer(Collections.singletonList("foo"))
                .apply(ModelTransformer.create(), model);

        assertThat(result.getShapeIndex().getShape(service1.getId()).get().asServiceShape().get()
                .getTrait(AuthenticationTrait.class).get()
                .getAuthenticationSchemes().keySet(), contains("foo"));
        assertThat(result.getShapeIndex().getShape(service2.getId()).get(), equalTo(service2));
        assertThat(result.getShapeIndex().getShape(service3.getId()).get(), equalTo(service3));
    }
}
