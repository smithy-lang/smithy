/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

public class ResourceLifecycleResolverTest {

    private static final String MODEL = "$version: \"2\"\n"
            + "namespace com.test\n"
            + "structure In { spec: Spec, items: ItemList, name: String, groups: GroupList, matrix: Matrix, byId: ItemMap }\n"
            + "structure Spec { size: Integer }\n"
            + "map ItemMap { key: String, value: Item }\n"
            + "list ItemList { member: Item }\n"
            + "structure Item { id: String, tag: String }\n"
            + "list GroupList { member: Group }\n"
            + "structure Group { items: ItemList }\n"
            + "list Matrix { member: ItemList }\n"
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
    public void walksFieldChain() {
        Model model = model();
        ResourceLifecycleResolver.PathResult r =
                ResourceLifecycleResolver.walk(model, struct(model, "In"), JmespathExpression.parse("spec.size"));
        assertThat(r.error, is(nullValue()));
        assertThat(r.root, is(false));
        assertThat(r.arrayDepth, is(0));
        assertThat(r.leaf.getId(), is(ShapeId.from("smithy.api#Integer")));
    }

    @Test
    public void walksProjectionToLeaf() {
        Model model = model();
        ResourceLifecycleResolver.PathResult r =
                ResourceLifecycleResolver.walk(model, struct(model, "In"), JmespathExpression.parse("items[*].id"));
        assertThat(r.error, is(nullValue()));
        assertThat(r.arrayDepth, is(1));
        assertThat(r.leaf.getId(), is(ShapeId.from("smithy.api#String")));
    }

    @Test
    public void walksProjectionToElementStructure() {
        Model model = model();
        ResourceLifecycleResolver.PathResult r =
                ResourceLifecycleResolver.walk(model, struct(model, "In"), JmespathExpression.parse("items[*]"));
        assertThat(r.error, is(nullValue()));
        assertThat(r.arrayDepth, is(1));
        assertThat(r.leaf.getId(), is(ShapeId.from("com.test#Item")));
    }

    @Test
    public void walksCurrentNodeAsRoot() {
        Model model = model();
        ResourceLifecycleResolver.PathResult r =
                ResourceLifecycleResolver.walk(model, struct(model, "In"), JmespathExpression.parse("@"));
        assertThat(r.error, is(nullValue()));
        assertThat(r.root, is(true));
        assertThat(r.leaf.getId(), is(ShapeId.from("com.test#In")));
    }

    @Test
    public void errorsOnMissingMember() {
        Model model = model();
        ResourceLifecycleResolver.PathResult r =
                ResourceLifecycleResolver.walk(model, struct(model, "In"), JmespathExpression.parse("nope"));
        assertThat(r.error, is(notNullValue()));
    }

    @Test
    public void errorsOnFieldAccessOfArray() {
        Model model = model();
        ResourceLifecycleResolver.PathResult r =
                ResourceLifecycleResolver.walk(model, struct(model, "In"), JmespathExpression.parse("items.id"));
        assertThat(r.error, is(notNullValue()));
    }

    @Test
    public void projectionSignatureIsTheIteratedList() {
        Model model = model();
        ResourceLifecycleResolver.PathResult r =
                ResourceLifecycleResolver.walk(model, struct(model, "In"), JmespathExpression.parse("items[*].id"));
        assertThat(r.error, is(nullValue()));
        assertThat(r.arrays, is(List.of(ShapeId.from("com.test#ItemList"))));
    }

    @Test
    public void sameListSharesSignature() {
        Model model = model();
        ResourceLifecycleResolver.PathResult id = ResourceLifecycleResolver.walk(model,
                struct(model, "In"),
                JmespathExpression.parse("items[*].id"));
        ResourceLifecycleResolver.PathResult tag = ResourceLifecycleResolver.walk(model,
                struct(model, "In"),
                JmespathExpression.parse("items[*].tag"));
        assertThat(id.arrays, is(tag.arrays));
    }

    @Test
    public void nestedProjectionsAccumulateSignatureInOrder() {
        Model model = model();
        ResourceLifecycleResolver.PathResult r = ResourceLifecycleResolver.walk(model,
                struct(model, "In"),
                JmespathExpression.parse("groups[*].items[*].id"));
        assertThat(r.error, is(nullValue()));
        assertThat(r.arrays,
                is(List.of(ShapeId.from("com.test#GroupList"), ShapeId.from("com.test#ItemList"))));
        assertThat(r.leaf.getId(), is(ShapeId.from("smithy.api#String")));
    }

    @Test
    public void flattenCollapsesNestedListsToOuterIdentity() {
        Model model = model();
        // matrix is a list of ItemList; matrix[] flattens to a single sequence of Item.
        ResourceLifecycleResolver.PathResult r = ResourceLifecycleResolver.walk(model,
                struct(model, "In"),
                JmespathExpression.parse("matrix[]"));
        assertThat(r.error, is(nullValue()));
        assertThat(r.arrays, is(List.of(ShapeId.from("com.test#Matrix"))));
        assertThat(r.leaf.getId(), is(ShapeId.from("com.test#Item")));
    }

    @Test
    public void scalarFieldHasEmptySignature() {
        Model model = model();
        ResourceLifecycleResolver.PathResult r =
                ResourceLifecycleResolver.walk(model, struct(model, "In"), JmespathExpression.parse("name"));
        assertThat(r.error, is(nullValue()));
        assertThat(r.arrays.isEmpty(), is(true));
    }

    @Test
    public void fieldAccessOnMapResolvesToValueType() {
        Model model = model();
        // byId is a map<String, Item>; a field access is a key lookup that resolves to the value
        // type Item regardless of the key name (JMESPath treats `byId.anything` as a key access).
        ResourceLifecycleResolver.PathResult r =
                ResourceLifecycleResolver.walk(model, struct(model, "In"), JmespathExpression.parse("byId.anyKey"));
        assertThat(r.error, is(nullValue()));
        assertThat(r.leaf.getId(), is(ShapeId.from("com.test#Item")));
        assertThat(r.arrays.isEmpty(), is(true));
    }

    @Test
    public void infersPropertiesByNameWithOverridesAndExclusions() {
        Model model = model();
        ResourceShape widget = model.expectShape(ShapeId.from("com.test#Widget"), ResourceShape.class);
        ResourceLifecycleResolver.InferenceResult r =
                ResourceLifecycleResolver.inferByName(widget,
                        struct(model, "Elem"),
                        ResourceLifecycleResolver.BindingKind.PROPERTY);
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
        ResourceLifecycleResolver.InferenceResult r =
                ResourceLifecycleResolver.inferByName(widget,
                        struct(model, "Elem"),
                        ResourceLifecycleResolver.BindingKind.IDENTIFIER);
        assertThat(r.matched, hasKey("widgetId"));
        assertThat(r.matched.get("widgetId").getMemberName(), is("widgetId"));
        // size and colour are not identifiers; extra is excluded.
        assertThat(unmatchedNames(r), containsInAnyOrder("size", "colour"));
    }

    private static List<String> unmatchedNames(ResourceLifecycleResolver.InferenceResult r) {
        return r.unmatched.stream().map(MemberShape::getMemberName).collect(Collectors.toList());
    }
}
