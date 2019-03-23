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

package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static software.amazon.smithy.model.knowledge.HttpBindingIndex.Binding;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.UriPattern;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpTrait;

public class HttpBindingIndexTest {

    private static Model model = Model.assembler()
            .addImport(HttpBindingIndex.class.getResource("http-index.json"))
            .assemble()
            .unwrap();

    @Test
    public void throwsWhenShapeIsInvalid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            model.getKnowledge(HttpBindingIndex.class).getRequestBindings(ShapeId.from("ns.foo#Missing"));
        });
    }

    @Test
    public void providesResponseCode() {
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);

        assertThat(index.getResponseCode(ShapeId.from("ns.foo#ServiceOperationNoInputOutput")), is(200));
    }

    @Test
    public void determinesErrorCodeFromHttpErrorTrait() {
        StructureShape structure = StructureShape.builder()
                .id("ns.foo#Error")
                .addTrait(new ErrorTrait("client", SourceLocation.NONE))
                .addTrait(new HttpErrorTrait(400, SourceLocation.NONE))
                .build();
        Model model = Model.assembler().addShape(structure).assemble().unwrap();
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);

        assertThat(index.getResponseCode(ShapeId.from("ns.foo#Error")), is(400));
    }

    @Test
    public void determinesErrorCodeFromErrorTrait() {
        StructureShape structure = StructureShape.builder()
                .id("ns.foo#Error")
                .addTrait(new ErrorTrait("client", SourceLocation.NONE))
                .build();
        Model model = Model.assembler().addShape(structure).assemble().unwrap();
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);

        assertThat(index.getResponseCode(ShapeId.from("ns.foo#Error")), is(400));
    }

    @Test
    public void returnsEmptyBindingsWhenNoInputOrOutput() {
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);

        assertThat(index.getRequestBindings(ShapeId.from("ns.foo#ServiceOperationNoInputOutput")).entrySet(), empty());
        assertThat(index.getResponseBindings(ShapeId.from("ns.foo#ServiceOperationNoInputOutput")).entrySet(), empty());
    }

    @Test
    public void returnsResponseMemberBindingsWithDefaults() {
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);
        ShapeId id = ShapeId.from("ns.foo#ServiceOperationExplicitMembers");
        Map<String, Binding> responseBindings = index.getResponseBindings(id);

        assertThat(responseBindings.size(), is(5));
        assertThat(responseBindings.get("foo"), equalTo(new Binding(
                expectMember(model, "ns.foo#ServiceOperationExplicitMembersOutput$foo"),
                HttpBindingIndex.Location.HEADER,
                "X-Foo",
                new HttpHeaderTrait("X-Foo", SourceLocation.NONE))));
        assertThat(responseBindings.get("qux"), equalTo(new Binding(
                expectMember(model, "ns.foo#ServiceOperationExplicitMembersOutput$qux"),
                HttpBindingIndex.Location.PREFIX_HEADERS,
                "X-Prefix-",
                new HttpPrefixHeadersTrait("X-Prefix-", SourceLocation.NONE))));
        assertThat(responseBindings.get("baz"), equalTo(new Binding(
                expectMember(model, "ns.foo#ServiceOperationExplicitMembersOutput$baz"),
                HttpBindingIndex.Location.DOCUMENT, "baz", null)));
        assertThat(responseBindings.get("bar"), equalTo(new Binding(
                expectMember(model, "ns.foo#ServiceOperationExplicitMembersOutput$bar"),
                HttpBindingIndex.Location.DOCUMENT, "bar", null)));
        assertThat(responseBindings.get("bam"), equalTo(new Binding(
                expectMember(model, "ns.foo#ServiceOperationExplicitMembersOutput$bam"),
                HttpBindingIndex.Location.DOCUMENT, "bam", null)));
    }

    @Test
    public void returnsResponseMemberBindingsWithExplicitBody() {
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);
        ShapeId id = ShapeId.from("ns.foo#ServiceOperationExplicitBody");
        Map<String, Binding> responseBindings = index.getResponseBindings(id);

        assertThat(responseBindings.entrySet(), hasSize(3));
        assertThat(responseBindings.get("foo"), equalTo(new Binding(
                expectMember(model, "ns.foo#ServiceOperationExplicitBodyOutput$foo"),
                HttpBindingIndex.Location.HEADER,
                "X-Foo",
                new HttpHeaderTrait("X-Foo", SourceLocation.NONE))));
        assertThat(responseBindings.get("qux"), equalTo(new Binding(
                expectMember(model, "ns.foo#ServiceOperationExplicitBodyOutput$qux"),
                HttpBindingIndex.Location.PREFIX_HEADERS,
                "X-Prefix-",
                new HttpPrefixHeadersTrait("X-Prefix-", SourceLocation.NONE))));
        assertThat(responseBindings.get("baz"), equalTo(new Binding(
                expectMember(model, "ns.foo#ServiceOperationExplicitBodyOutput$baz"),
                HttpBindingIndex.Location.PAYLOAD,
                "baz",
                new HttpPayloadTrait(SourceLocation.NONE))));
    }

    @Test
    public void returnsErrorResponseCode() {
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);
        ShapeId id = ShapeId.from("ns.foo#ErrorExplicitStatus");

        assertThat(index.getResponseCode(id), is(403));
    }

    @Test
    public void findsLabelBindings() {
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);
        ShapeId id = ShapeId.from("ns.foo#WithLabels");
        Map<String, Binding> bindings = index.getRequestBindings(id);

        assertThat(bindings.entrySet(), hasSize(1));
        assertThat(bindings.get("baz"), equalTo(new Binding(
                expectMember(model, "ns.foo#WithLabelsInput$baz"),
                HttpBindingIndex.Location.LABEL, "baz", null)));
    }

    @Test
    public void findsUnboundMembers() {
        ServiceShape service = ServiceShape.builder()
                .id("ns.foo#Service")
                .version("1")
                .addOperation("ns.foo#Operation")
                .build();
        OperationShape operation = OperationShape.builder()
                .id("ns.foo#Operation")
                .input(ShapeId.from("ns.foo#Input"))
                .addTrait(HttpTrait.builder().uri(UriPattern.parse("/")).method("GET").build())
                .build();
        StructureShape structure = StructureShape.builder()
                .id("ns.foo#Input")
                .addMember(MemberShape.builder()
                                   .id("ns.foo#Input$bar")
                                   .target("ns.foo#String")
                                   .addTrait(new HttpPayloadTrait(SourceLocation.NONE))
                                   .build())
                .addMember(MemberShape.builder().id("ns.foo#Input$baz").target("ns.foo#String").build())
                .build();
        StringShape string = StringShape.builder().id("ns.foo#String").build();
        Model model = Model.assembler()
                .addShape(structure)
                .addShape(string)
                .addShape(operation)
                .addShape(service)
                .assemble()
                .getResult()
                .get();
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);
        Map<String, Binding> requestBindings = index.getRequestBindings(operation.getId());

        assertThat(requestBindings.get("bar").getLocation(), is(HttpBindingIndex.Location.PAYLOAD));
        assertThat(requestBindings.get("baz").getLocation(), is(HttpBindingIndex.Location.UNBOUND));
    }

    @Test
    public void checksForHttpRequestAndResponseBindings() {
        Shape shape = MemberShape.builder()
                .target("smithy.api#String")
                .id("smithy.example#Baz$bar")
                .addTrait(new HttpLabelTrait(SourceLocation.NONE))
                .build();

        assertThat(HttpBindingIndex.hasHttpRequestBindings(shape), is(true));
        assertThat(HttpBindingIndex.hasHttpResponseBindings(shape), is(false));
    }

    @Test
    public void checksForHttpResponseBindings() {
        Shape shape = MemberShape.builder()
                .target("smithy.api#String")
                .id("smithy.example#Baz$bar")
                .addTrait(new HttpHeaderTrait("hello", SourceLocation.NONE))
                .build();

        assertThat(HttpBindingIndex.hasHttpRequestBindings(shape), is(true));
        assertThat(HttpBindingIndex.hasHttpResponseBindings(shape), is(true));
    }

    private static MemberShape expectMember(Model model, String id) {
        ShapeId shapeId = ShapeId.from(id);
        return model.getShapeIndex().getShape(shapeId).get().asMemberShape().get();
    }
}
