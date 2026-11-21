/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
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
            + "  @idempotencyToken clientToken: String\n"
            + "  other: String\n"
            + "}\n"
            + "operation RenamedDummy { output: RenamedElem }\n"
            + "structure RenamedElem {\n"
            + "  @required @resourceIdentifier(\"widgetId\") id: String\n"
            + "  size: Integer\n"
            + "}\n"
            + "operation ExplicitDummy { output: ExplicitElem }\n"
            + "structure ExplicitElem {\n"
            + "  @required @resourceIdentifier(\"widgetId\") size: String\n"
            + "  @property(name: \"color\") widgetId: String\n"
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
                ResourceMemberInference.inferByName(model,
                        widget,
                        struct(model, "Elem"),
                        ResourceMemberInference.BindingKind.PROPERTY);
        assertThat(r.matched, hasKey("size"));
        assertThat(r.matched, hasKey("color")); // colour -> color via @property
        assertThat(r.matched.get("color").getMemberName(), is("colour"));
        // extra is @notProperty and clientToken is an @idempotencyToken (excluded); widgetId is an
        // identifier (other kind); other is neither a property nor an identifier (unmatched).
        assertThat(r.otherKind.keySet(), containsInAnyOrder("widgetId"));
        assertThat(unmatchedNames(r), containsInAnyOrder("other"));
    }

    @Test
    public void infersIdentifiersByName() {
        Model model = model();
        ResourceShape widget = model.expectShape(ShapeId.from("com.test#Widget"), ResourceShape.class);
        MemberInferenceResult r =
                ResourceMemberInference.inferByName(model,
                        widget,
                        struct(model, "Elem"),
                        ResourceMemberInference.BindingKind.IDENTIFIER);
        assertThat(r.matched, hasKey("widgetId"));
        assertThat(r.matched.get("widgetId").getMemberName(), is("widgetId"));
        // size and colour are properties (other kind); extra and clientToken are excluded; other is
        // neither a property nor an identifier (unmatched).
        assertThat(r.otherKind.keySet(), containsInAnyOrder("size", "color"));
        assertThat(unmatchedNames(r), containsInAnyOrder("other"));
    }

    @Test
    public void skipsIdentifierRenamedWithResourceIdentifierWhenInferringProperties() {
        Model model = model();
        ResourceShape widget = model.expectShape(ShapeId.from("com.test#Widget"), ResourceShape.class);
        MemberInferenceResult r =
                ResourceMemberInference.inferByName(model,
                        widget,
                        struct(model, "RenamedElem"),
                        ResourceMemberInference.BindingKind.PROPERTY);
        assertThat(r.matched, hasKey("size"));
        // id is bound to the widgetId identifier through @resourceIdentifier (other kind).
        assertThat(r.otherKind.get("widgetId").getMemberName(), is("id"));
        assertThat(r.unmatched, empty());
    }

    @Test
    public void explicitTraitsDecideTheKindOverMemberNames() {
        Model model = model();
        ResourceShape widget = model.expectShape(ShapeId.from("com.test#Widget"), ResourceShape.class);
        StructureShape explicit = struct(model, "ExplicitElem");

        // size is named like a property but is the widgetId identifier through @resourceIdentifier,
        // and widgetId is named like the identifier but is the color property through @property.
        MemberInferenceResult properties =
                ResourceMemberInference.inferByName(model,
                        widget,
                        explicit,
                        ResourceMemberInference.BindingKind.PROPERTY);
        assertThat(properties.matched.get("color").getMemberName(), is("widgetId"));
        assertThat(properties.otherKind.get("widgetId").getMemberName(), is("size"));

        MemberInferenceResult identifiers =
                ResourceMemberInference.inferByName(model,
                        widget,
                        explicit,
                        ResourceMemberInference.BindingKind.IDENTIFIER);
        assertThat(identifiers.matched.get("widgetId").getMemberName(), is("size"));
        assertThat(identifiers.otherKind.get("color").getMemberName(), is("widgetId"));
    }

    private static List<String> unmatchedNames(MemberInferenceResult r) {
        return r.unmatched.stream().map(MemberShape::getMemberName).collect(Collectors.toList());
    }
}
