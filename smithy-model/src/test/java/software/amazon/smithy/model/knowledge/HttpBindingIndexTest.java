/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
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
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpResponseCodeTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

public class HttpBindingIndexTest {

    private static Model model;

    @BeforeAll
    private static void readModel() {
        model = Model.assembler()
                .addImport(HttpBindingIndex.class.getResource("http-index.json"))
                .assemble()
                .unwrap();
    }

    @Test
    public void providesResponseCode() {
        HttpBindingIndex index = HttpBindingIndex.of(model);

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
        HttpBindingIndex index = HttpBindingIndex.of(model);

        assertThat(index.getResponseCode(ShapeId.from("ns.foo#Error")), is(400));
    }

    @Test
    public void determinesErrorCodeFromErrorTrait() {
        StructureShape structure = StructureShape.builder()
                .id("ns.foo#Error")
                .addTrait(new ErrorTrait("client", SourceLocation.NONE))
                .build();
        Model model = Model.assembler().addShape(structure).assemble().unwrap();
        HttpBindingIndex index = HttpBindingIndex.of(model);

        assertThat(index.getResponseCode(ShapeId.from("ns.foo#Error")), is(400));
    }

    @Test
    public void returnsEmptyBindingsWhenNoInputOrOutput() {
        HttpBindingIndex index = HttpBindingIndex.of(model);

        assertThat(index.getRequestBindings(ShapeId.from("ns.foo#ServiceOperationNoInputOutput")).entrySet(), empty());
        assertThat(index.getResponseBindings(ShapeId.from("ns.foo#ServiceOperationNoInputOutput")).entrySet(), empty());
    }

    @Test
    public void returnsResponseMemberBindingsWithDefaults() {
        HttpBindingIndex index = HttpBindingIndex.of(model);
        ShapeId id = ShapeId.from("ns.foo#ServiceOperationExplicitMembers");
        Map<String, HttpBinding> responseBindings = index.getResponseBindings(id);

        assertThat(responseBindings.size(), is(5));
        assertThat(responseBindings.get("foo"),
                equalTo(new HttpBinding(
                        expectMember(model, "ns.foo#ServiceOperationExplicitMembersOutput$foo"),
                        HttpBinding.Location.HEADER,
                        "X-Foo",
                        new HttpHeaderTrait("X-Foo", SourceLocation.NONE))));
        assertThat(responseBindings.get("qux"),
                equalTo(new HttpBinding(
                        expectMember(model, "ns.foo#ServiceOperationExplicitMembersOutput$qux"),
                        HttpBinding.Location.PREFIX_HEADERS,
                        "X-Prefix-",
                        new HttpPrefixHeadersTrait("X-Prefix-", SourceLocation.NONE))));
        assertThat(responseBindings.get("baz"),
                equalTo(new HttpBinding(
                        expectMember(model, "ns.foo#ServiceOperationExplicitMembersOutput$baz"),
                        HttpBinding.Location.DOCUMENT,
                        "baz",
                        null)));
        assertThat(responseBindings.get("bar"),
                equalTo(new HttpBinding(
                        expectMember(model, "ns.foo#ServiceOperationExplicitMembersOutput$bar"),
                        HttpBinding.Location.DOCUMENT,
                        "bar",
                        null)));
        assertThat(responseBindings.get("bam"),
                equalTo(new HttpBinding(
                        expectMember(model, "ns.foo#ServiceOperationExplicitMembersOutput$bam"),
                        HttpBinding.Location.DOCUMENT,
                        "bam",
                        null)));
    }

    @Test
    public void returnsResponseMemberBindingsWithExplicitBody() {
        HttpBindingIndex index = HttpBindingIndex.of(model);
        ShapeId id = ShapeId.from("ns.foo#ServiceOperationExplicitBody");
        Map<String, HttpBinding> responseBindings = index.getResponseBindings(id);

        assertThat(responseBindings.entrySet(), hasSize(3));
        assertThat(responseBindings.get("foo"),
                equalTo(new HttpBinding(
                        expectMember(model, "ns.foo#ServiceOperationExplicitBodyOutput$foo"),
                        HttpBinding.Location.HEADER,
                        "X-Foo",
                        new HttpHeaderTrait("X-Foo", SourceLocation.NONE))));
        assertThat(responseBindings.get("qux"),
                equalTo(new HttpBinding(
                        expectMember(model, "ns.foo#ServiceOperationExplicitBodyOutput$qux"),
                        HttpBinding.Location.PREFIX_HEADERS,
                        "X-Prefix-",
                        new HttpPrefixHeadersTrait("X-Prefix-", SourceLocation.NONE))));
        assertThat(responseBindings.get("baz"),
                equalTo(new HttpBinding(
                        expectMember(model, "ns.foo#ServiceOperationExplicitBodyOutput$baz"),
                        HttpBinding.Location.PAYLOAD,
                        "baz",
                        new HttpPayloadTrait())));
    }

    @Test
    public void returnsResponseCodeBinding() {
        HttpBindingIndex index = HttpBindingIndex.of(model);
        ShapeId id = ShapeId.from("ns.foo#OperationWithBoundResponseCode");
        Map<String, HttpBinding> responseBindings = index.getResponseBindings(id);

        assertThat(responseBindings.size(), is(1));
        assertThat(responseBindings.get("Status"),
                equalTo(new HttpBinding(
                        expectMember(model, "ns.foo#StructureWithBoundResponseCode$Status"),
                        HttpBinding.Location.RESPONSE_CODE,
                        "Status",
                        new HttpResponseCodeTrait())));
    }

    @Test
    public void returnsErrorResponseCode() {
        HttpBindingIndex index = HttpBindingIndex.of(model);
        ShapeId id = ShapeId.from("ns.foo#ErrorExplicitStatus");

        assertThat(index.getResponseCode(id), is(403));
    }

    @Test
    public void findsLabelBindings() {
        HttpBindingIndex index = HttpBindingIndex.of(model);
        ShapeId id = ShapeId.from("ns.foo#WithLabels");
        Map<String, HttpBinding> bindings = index.getRequestBindings(id);

        assertThat(bindings.entrySet(), hasSize(1));
        assertThat(bindings.get("baz"),
                equalTo(new HttpBinding(
                        expectMember(model, "ns.foo#WithLabelsInput$baz"),
                        HttpBinding.Location.LABEL,
                        "baz",
                        null)));
    }

    @Test
    public void findsQueryBindings() {
        HttpBindingIndex index = HttpBindingIndex.of(model);
        ShapeId id = ShapeId.from("ns.foo#ServiceOperationExplicitMembers");
        Map<String, HttpBinding> bindings = index.getRequestBindings(id);

        assertThat(bindings.get("baz"),
                equalTo(new HttpBinding(
                        expectMember(model, "ns.foo#ServiceOperationExplicitMembersInput$baz"),
                        HttpBinding.Location.QUERY,
                        "baz",
                        new HttpQueryTrait("baz"))));
        assertThat(bindings.get("bar"),
                equalTo(new HttpBinding(
                        expectMember(model, "ns.foo#ServiceOperationExplicitMembersInput$bar"),
                        HttpBinding.Location.QUERY,
                        "bar",
                        new HttpQueryTrait("bar"))));
    }

    @Test
    public void findsQueryParamsBindings() {
        HttpBindingIndex index = HttpBindingIndex.of(model);
        ShapeId id = ShapeId.from("ns.foo#ServiceOperationExplicitMembers");
        Map<String, HttpBinding> bindings = index.getRequestBindings(id);

        assertThat(bindings.get("corge"),
                equalTo(new HttpBinding(
                        expectMember(model, "ns.foo#ServiceOperationExplicitMembersInput$corge"),
                        HttpBinding.Location.QUERY_PARAMS,
                        "corge",
                        new HttpQueryParamsTrait())));
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
                        .addTrait(new HttpPayloadTrait())
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
        HttpBindingIndex index = HttpBindingIndex.of(model);
        Map<String, HttpBinding> requestBindings = index.getRequestBindings(operation.getId());

        assertThat(requestBindings.get("bar").getLocation(), is(HttpBinding.Location.PAYLOAD));
        assertThat(requestBindings.get("baz").getLocation(), is(HttpBinding.Location.UNBOUND));
    }

    @Test
    public void checksForHttpRequestAndResponseBindings() {
        Shape shape = MemberShape.builder()
                .target("smithy.api#Timestamp")
                .id("smithy.example#Baz$bar")
                .addTrait(new HttpLabelTrait())
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
    public void checksForHttpResponseCodeBindings() {
        Shape shape = MemberShape.builder()
                .target("smithy.api#Integer")
                .id("smithy.example#Baz$bar")
                .addTrait(new HttpResponseCodeTrait())
                .build();

        assertThat(HttpBindingIndex.hasHttpRequestBindings(shape), is(false));
        assertThat(HttpBindingIndex.hasHttpResponseBindings(shape), is(true));
    }

    @Test
    public void checksForRequestQueryBindings() {
        Shape queryShape = MemberShape.builder()
                .target("smithy.api#Timestamp")
                .id("smithy.example#Baz$bar")
                .addTrait(new HttpQueryTrait("foo"))
                .build();

        assertThat(HttpBindingIndex.hasHttpRequestBindings(queryShape), is(true));
        assertThat(HttpBindingIndex.hasHttpResponseBindings(queryShape), is(false));
    }

    @Test
    public void checksForRequestQueryParamsBindings() {
        Shape queryParamsShape = MemberShape.builder()
                .target("smithy.api#Timestamp")
                .id("smithy.example#Baz$bar")
                .addTrait(new HttpQueryParamsTrait())
                .build();

        assertThat(HttpBindingIndex.hasHttpRequestBindings(queryParamsShape), is(true));
        assertThat(HttpBindingIndex.hasHttpResponseBindings(queryParamsShape), is(false));
    }

    @Test
    public void resolvesStructureBodyContentType() {
        HttpBindingIndex index = HttpBindingIndex.of(model);
        ShapeId operation = ShapeId.from("ns.foo#ServiceOperationWithStructurePayload");
        Optional<String> contentType = index.determineResponseContentType(operation, "application/json");

        assertThat(contentType, equalTo(Optional.of("application/json")));
    }

    @Test
    public void resolvesStringBodyContentType() {
        HttpBindingIndex index = HttpBindingIndex.of(model);
        ShapeId operation = ShapeId.from("ns.foo#ServiceOperationExplicitMembers");
        Optional<String> contentType = index.determineRequestContentType(operation, "application/json");

        assertThat(contentType, equalTo(Optional.of("text/plain")));
    }

    @Test
    public void resolvesBlobBodyContentType() {
        HttpBindingIndex index = HttpBindingIndex.of(model);
        ShapeId operation = ShapeId.from("ns.foo#ServiceOperationWithBlobPayload");
        Optional<String> contentType = index.determineResponseContentType(operation, "application/json");

        assertThat(contentType, equalTo(Optional.of("application/octet-stream")));
    }

    @Test
    public void resolvesMediaType() {
        HttpBindingIndex index = HttpBindingIndex.of(model);
        ShapeId operation = ShapeId.from("ns.foo#ServiceOperationWithMediaType");
        Optional<String> contentType = index.determineResponseContentType(operation, "application/json");

        assertThat(contentType, equalTo(Optional.of("application/xml")));
    }

    @Test
    public void resolvesResponseEventStreamMediaType() {
        HttpBindingIndex index = HttpBindingIndex.of(model);
        ShapeId operation = ShapeId.from("ns.foo#ServiceOperationWithEventStream");
        String expected = "application/vnd.amazon.eventstream";
        Optional<String> contentType = index.determineResponseContentType(operation, "ignore/me", expected);

        assertThat(contentType, equalTo(Optional.of(expected)));
    }

    @Test
    public void resolvesDocumentMediaType() {
        HttpBindingIndex index = HttpBindingIndex.of(model);
        ShapeId operation = ShapeId.from("ns.foo#ServiceOperationExplicitMembers");
        Optional<String> contentType = index.determineResponseContentType(operation, "application/json");

        assertThat(contentType, equalTo(Optional.of("application/json")));
    }

    private static MemberShape expectMember(Model model, String id) {
        ShapeId shapeId = ShapeId.from(id);
        return model.expectShape(shapeId).asMemberShape().get();
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
        HttpBindingIndex index = HttpBindingIndex.of(model);
        TimestampFormatTrait.Format format = index.determineTimestampFormat(
                member,
                HttpBinding.Location.HEADER,
                TimestampFormatTrait.Format.DATE_TIME);

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
        HttpBindingIndex index = HttpBindingIndex.of(model);

        assertThat(index.determineTimestampFormat(
                member,
                HttpBinding.Location.HEADER,
                TimestampFormatTrait.Format.EPOCH_SECONDS),
                equalTo(TimestampFormatTrait.Format.HTTP_DATE));
    }

    @Test
    public void prefixHeadersLocationUsesHttpDateTimestampFormat() {
        MemberShape key = MemberShape.builder()
                .id("foo.bar#Baz$key")
                .target("smithy.api#String")
                .build();
        MemberShape value = MemberShape.builder()
                .id("foo.bar#Baz$value")
                .target("smithy.api#Timestamp")
                .build();
        Model model = Model.assembler()
                .addShape(value)
                .addShape(MapShape.builder().addMember(key).addMember(value).id("foo.bar#Baz").build())
                .assemble()
                .unwrap();
        HttpBindingIndex index = HttpBindingIndex.of(model);

        assertThat(index.determineTimestampFormat(
                value,
                HttpBinding.Location.PREFIX_HEADERS,
                TimestampFormatTrait.Format.EPOCH_SECONDS),
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
        HttpBindingIndex index = HttpBindingIndex.of(model);

        assertThat(index.determineTimestampFormat(
                member,
                HttpBinding.Location.QUERY,
                TimestampFormatTrait.Format.EPOCH_SECONDS),
                equalTo(TimestampFormatTrait.Format.DATE_TIME));
        assertThat(index.determineTimestampFormat(
                member,
                HttpBinding.Location.LABEL,
                TimestampFormatTrait.Format.EPOCH_SECONDS),
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
        HttpBindingIndex index = HttpBindingIndex.of(model);

        assertThat(index.determineTimestampFormat(
                member,
                HttpBinding.Location.DOCUMENT,
                TimestampFormatTrait.Format.EPOCH_SECONDS),
                equalTo(TimestampFormatTrait.Format.EPOCH_SECONDS));
    }
}
