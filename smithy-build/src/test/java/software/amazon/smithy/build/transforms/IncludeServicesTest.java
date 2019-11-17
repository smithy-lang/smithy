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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.transform.ModelTransformer;

public class IncludeServicesTest {

    @Test
    public void removesTraitsNotInList() {
        ServiceShape serviceA = ServiceShape.builder().id("ns.foo#baz").version("1").build();
        ServiceShape serviceB = ServiceShape.builder().id("ns.foo#bar").version("1").build();
        StringShape string = StringShape.builder().id("ns.foo#yuck").build();
        Model model = Model.builder().addShapes(serviceA, serviceB, string).build();
        Model result = new IncludeServices()
                .createTransformer(Collections.singletonList("ns.foo#baz"))
                .apply(ModelTransformer.create(), model);

        assertThat(result.getShape(serviceA.getId()), not(Optional.empty()));
        assertThat(result.getShape(string.getId()), not(Optional.empty()));
        assertThat(result.getShape(serviceB.getId()), is(Optional.empty()));
    }
}
