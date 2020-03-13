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

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ListShape;
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
import software.amazon.smithy.model.traits.TimestampFormatTrait;

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
        Map<String, HttpBinding> responseBindings = index.getResponseBindings(id);

        assertThat(responseBindings.size(), is(5));
        assertThat(responseBindings.get("foo"), equalTo(new HttpBinding(
                expectMember(model, "ns.foo#ServiceOperationExplicitMembersOutput$foo"),
                HttpBinding.Location.HEADER,
                "X-Foo",
                new HttpHeaderTrait("X-Foo", SourceLocation.NONE))));
        assertThat(responseBindings.get("qux"), equalTo(new HttpBinding(
                expectMember(model, "ns.foo#ServiceOperationExplicitMembersOutput$qux"),
                HttpBinding.Location.PREFIX_HEADERS,
                "X-Prefix-",
                new HttpPrefixHeadersTrait("X-Prefix-", SourceLocation.NONE))));
        assertThat(responseBindings.get("baz"), equalTo(new HttpBinding(
                expectMember(model, "ns.foo#ServiceOperationExplicitMembersOutput$baz"),
                HttpBinding.Location.DOCUMENT, "baz", null)));
        assertThat(responseBindings.get("bar"), equalTo(new HttpBinding(
                expectMember(model, "ns.foo#ServiceOperationExplicitMembersOutput$bar"),
                HttpBinding.Location.DOCUMENT, "bar", null)));
        assertThat(responseBindings.get("bam"), equalTo(new HttpBinding(
                expectMember(model, "ns.foo#ServiceOperationExplicitMembersOutput$bam"),
                HttpBinding.Location.DOCUMENT, "bam", null)));
    }

    @Test
    public void returnsResponseMemberBindingsWithExplicitBody() {
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);
        ShapeId id = ShapeId.from("ns.foo#ServiceOperationExplicitBody");
        Map<String, HttpBinding> responseBindings = index.getResponseBindings(id);

        assertThat(responseBindings.entrySet(), hasSize(3));
        assertThat(responseBindings.get("foo"), equalTo(new HttpBinding(
                expectMember(model, "ns.foo#ServiceOperationExplicitBodyOutput$foo"),
                HttpBinding.Location.HEADER,
                "X-Foo",
                new HttpHeaderTrait("X-Foo", SourceLocation.NONE))));
        assertThat(responseBindings.get("qux"), equalTo(new HttpBinding(
                expectMember(model, "ns.foo#ServiceOperationExplicitBodyOutput$qux"),
                HttpBinding.Location.PREFIX_HEADERS,
                "X-Prefix-",
                new HttpPrefixHeadersTrait("X-Prefix-", SourceLocation.NONE))));
        assertThat(responseBindings.get("baz"), equalTo(new HttpBinding(
                expectMember(model, "ns.foo#ServiceOperationExplicitBodyOutput$baz"),
                HttpBinding.Location.PAYLOAD,
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
        Map<String, HttpBinding> bindings = index.getRequestBindings(id);

        assertThat(bindings.entrySet(), hasSize(1));
        assertThat(bindings.get("baz"), equalTo(new HttpBinding(
                expectMember(model, "ns.foo#WithLabelsInput$baz"),
                HttpBinding.Location.LABEL, "baz", null)));
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
        Map<String, HttpBinding> requestBindings = index.getRequestBindings(operation.getId());

        assertThat(requestBindings.get("bar").getLocation(), is(HttpBinding.Location.PAYLOAD));
        assertThat(requestBindings.get("baz").getLocation(), is(HttpBinding.Location.UNBOUND));
    }

    @Test
    public void checksForHttpRequestAndResponseBindings() {
        Shape shape = MemberShape.builder()
                .target("smithy.api#Timestamp")
                .id("smithy.example#Baz$bar")
                .addTrait(new HttpLabelTrait(SourceLocation.NONE))
                .build();

        assertThat(HttpBindingIndex.hasHttpRequestBindings(shape), is(true));
        assertThat(HttpBindingIndex.hasHttpResponseBindings(shape), is(false));
    }

    @Test
    public void checksForHttpResponseBindings() {
        Shape shape = MemberShape.builder()
                .target("smithy.api#Timestamp")
                .id("smithy.example#Baz$bar")
                .addTrait(new HttpHeaderTrait("hello", SourceLocation.NONE))
                .build();

        assertThat(HttpBindingIndex.hasHttpRequestBindings(shape), is(true));
        assertThat(HttpBindingIndex.hasHttpResponseBindings(shape), is(true));
    }

    @Test
    public void resolvesStructureBodyContentType() {
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);
        ShapeId operation = ShapeId.from("ns.foo#ServiceOperationWithStructurePayload");
        Optional<String> contentType = index.determineResponseContentType(operation, "application/json");

        assertThat(contentType, equalTo(Optional.of("application/json")));
    }

    @Test
    public void resolvesStringBodyContentType() {
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);
        ShapeId operation = ShapeId.from("ns.foo#ServiceOperationExplicitMembers");
        Optional<String> contentType = index.determineRequestContentType(operation, "application/json");

        assertThat(contentType, equalTo(Optional.of("text/plain")));
    }

    @Test
    public void resolvesBlobBodyContentType() {
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);
        ShapeId operation = ShapeId.from("ns.foo#ServiceOperationWithBlobPayload");
        Optional<String> contentType = index.determineResponseContentType(operation, "application/json");

        assertThat(contentType, equalTo(Optional.of("application/octet-stream")));
    }

    @Test
    public void resolvesMediaTypeContentType() {
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);
        ShapeId operation = ShapeId.from("ns.foo#ServiceOperationWithMediaType");
        Optional<String> contentType = index.determineResponseContentType(operation, "application/json");

        assertThat(contentType, equalTo(Optional.of("application/xml")));
    }

    private static MemberShape expectMember(Model model, String id) {
        ShapeId shapeId = ShapeId.from(id);
        return model.expectShape(shapeId).expectMemberShape();
    }

    @Test
    public void usesTimestampFormatMemberTraitToDetermineFormat() {
        MemberShape member = MemberShape.builder()
                .id("foo.bar#Baz$member")
                .target("smithy.api#Timestamp")
                .addTrait(new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS))
                .build();
        Model model = Model.assembler()
                .addShape(member)
                .addShape(ListShape.builder().member(member).id("foo.bar#Baz").build())
                .assemble()
                .unwrap();
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);
        TimestampFormatTrait.Format format = index.determineTimestampFormat(
                member, HttpBinding.Location.HEADER, TimestampFormatTrait.Format.DATE_TIME);

        assertThat(format, equalTo(TimestampFormatTrait.Format.EPOCH_SECONDS));
    }

    @Test
    public void headerLocationUsesHttpDateTimestampFormat() {
        MemberShape member = MemberShape.builder()
                .id("foo.bar#Baz$member")
                .target("smithy.api#Timestamp")
                .build();
        Model model = Model.assembler()
                .addShape(member)
                .addShape(ListShape.builder().member(member).id("foo.bar#Baz").build())
                .assemble()
                .unwrap();
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);

        assertThat(index.determineTimestampFormat(
                member, HttpBinding.Location.HEADER, TimestampFormatTrait.Format.EPOCH_SECONDS),
                   equalTo(TimestampFormatTrait.Format.HTTP_DATE));
    }

    @Test
    public void queryAndLabelLocationUsesDateTimeTimestampFormat() {
        MemberShape member = MemberShape.builder()
                .id("foo.bar#Baz$member")
                .target("smithy.api#Timestamp")
                .build();
        Model model = Model.assembler()
                .addShape(member)
                .addShape(ListShape.builder().member(member).id("foo.bar#Baz").build())
                .assemble()
                .unwrap();
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);

        assertThat(index.determineTimestampFormat(
                member, HttpBinding.Location.QUERY, TimestampFormatTrait.Format.EPOCH_SECONDS),
                   equalTo(TimestampFormatTrait.Format.DATE_TIME));
        assertThat(index.determineTimestampFormat(
                member, HttpBinding.Location.LABEL, TimestampFormatTrait.Format.EPOCH_SECONDS),
                   equalTo(TimestampFormatTrait.Format.DATE_TIME));
    }

    @Test
    public void otherLocationsUseDefaultTimestampFormat() {
        MemberShape member = MemberShape.builder()
                .id("foo.bar#Baz$member")
                .target("smithy.api#Timestamp")
                .build();
        Model model = Model.assembler()
                .addShape(member)
                .addShape(ListShape.builder().member(member).id("foo.bar#Baz").build())
                .assemble()
                .unwrap();
        HttpBindingIndex index = model.getKnowledge(HttpBindingIndex.class);

        assertThat(index.determineTimestampFormat(
                member, HttpBinding.Location.DOCUMENT, TimestampFormatTrait.Format.EPOCH_SECONDS),
                   equalTo(TimestampFormatTrait.Format.EPOCH_SECONDS));
    }
}
