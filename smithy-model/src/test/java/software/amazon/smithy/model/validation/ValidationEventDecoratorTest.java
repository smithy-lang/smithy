/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SetUtils;

public class ValidationEventDecoratorTest {

    static final String HINT = "Consider connecting this structure to a service";
    static final String UNREFERENCED_SHAPE_EVENT_ID = "UnreferencedShape";
    static final Set<ShapeId> STRUCT_SHAPE_IDS = SetUtils.of(ShapeId.from("ns.foo#Structure"),
                                                             ShapeId.from("ns.foo#Structure2"),
                                                             ShapeId.from("ns.foo#Structure3"));

    @Test
    public void something() {
        ValidatedResult<Model> result = Model.assembler()
                                             .addImport(NodeValidationVisitorTest.class.getResource("node-validator"
                                                                                                    + ".json"))
                                             .addDecorator(new DumpHintValidationEventDecorator())
                                             .assemble();
        for (ValidationEvent event : result.getValidationEvents()) {
            ShapeId eventShapeId = event.getShapeId().orElse(null);
            if (STRUCT_SHAPE_IDS.contains(eventShapeId)) {
                assertThat(event.getHint().isPresent(), equalTo(true));
                assertThat(event.getHint().get(), equalTo(HINT));
            } else {
                assertThat(event.getHint().isPresent(), equalTo(false));
            }
        }
    }

    static class DumpHintValidationEventDecorator implements ValidationEventDecorator {
        @Override
        public ValidationEvent decorate(ValidationEvent ev) {
            if (ev.containsId(UNREFERENCED_SHAPE_EVENT_ID)) {
                // This is fragile and might fail if we change the message, but the message is all we currently have
                // to tell apart specific instances apart.
                if (ev.getMessage().contains("The structure ")) {
                    return ev.toBuilder().hint(HINT).build();
                }
            }
            return ev;
        }
    }
}
