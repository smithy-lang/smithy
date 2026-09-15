/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

public class ResourceMemberInferenceTest {

    private static final String MODEL = "$version: \"2\"\n"
            + "namespace com.test\n"
            + "resource Widget {\n"
            + "  identifiers: { widgetId: String }\n"
            + "  properties: { size: Integer, color: String }\n"
            + "  create: CreateWidget\n"
            + "}\n"
            + "operation CreateWidget { input: CreateWidgetInput, output: CreateWidgetOutput }\n"
            + "@input structure CreateWidgetInput { size: Integer, color: String }\n"
            + "structure CreateWidgetOutput { @required widgetId: String }\n"
            + "operation Dummy { output: Elem }\n"
            + "structure Elem {\n"
            + "  widgetId: String\n"
            + "  size: Integer\n"
            + "  @property(name: \"color\") colour: String\n"
            + "  @notProperty extra: String\n"
            + "}\n";

    private Model model() {
        return Model.assembler().addUnparsedModel("test.smithy", MODEL).assemble().unwrap();
    }

    private StructureShape struct(Model model, String name) {
        return model.expectShape(ShapeId.from("com.test#" + name), StructureShape.class);
    }

    @Test
    public void infersPropertiesByNameWithOverridesAndExclusions() {
        Model model = model();
        ResourceShape widget = model.expectShape(ShapeId.from("com.test#Widget"), ResourceShape.class);
        MemberInferenceResult r =
                ResourceMemberInference.inferByName(widget,
                        struct(model, "Elem"),
                        ResourceMemberInference.BindingKind.PROPERTY);
        assertThat(r.matched, hasKey("size"));
        assertThat(r.matched, hasKey("color")); // colour -> color via @property
        assertThat(r.matched.get("color").getMemberName(), is("colour"));
        // extra is @notProperty (excluded); widgetId is an identifier, not a property (unmatched).
        assertThat(unmatchedNames(r), containsInAnyOrder("widgetId"));
    }

    @Test
    public void infersIdentifiersByName() {
        Model model = model();
        ResourceShape widget = model.expectShape(ShapeId.from("com.test#Widget"), ResourceShape.class);
        MemberInferenceResult r =
                ResourceMemberInference.inferByName(widget,
                        struct(model, "Elem"),
                        ResourceMemberInference.BindingKind.IDENTIFIER);
        assertThat(r.matched, hasKey("widgetId"));
        assertThat(r.matched.get("widgetId").getMemberName(), is("widgetId"));
        // size and colour are not identifiers; extra is excluded.
        assertThat(unmatchedNames(r), containsInAnyOrder("size", "colour"));
    }

    private static List<String> unmatchedNames(MemberInferenceResult r) {
        return r.unmatched.stream().map(MemberShape::getMemberName).collect(Collectors.toList());
    }
}
