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

package software.amazon.smithy.openapi.fromsmithy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.jsonschema.JsonSchemaConstants;
import software.amazon.smithy.jsonschema.JsonSchemaConverter;
import software.amazon.smithy.jsonschema.SchemaDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.ExternalDocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.openapi.OpenApiConstants;

public class OpenApiJsonSchemaMapperTest {
    @Test
    public void convertsModels() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("test-service.json"))
                .assemble()
                .unwrap();
        SchemaDocument document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder()
                        .withMember(OpenApiConstants.OPEN_API_MODE, true)
                        .withMember(JsonSchemaConstants.DEFINITION_POINTER, OpenApiConstants.SCHEMA_COMPONENTS_POINTER)
                        .build())
                .convert(model.getShapeIndex());

        assertTrue(document.toNode().expectObjectNode().getMember("components").isPresent());
    }

    @Test
    public void stripsUnsupportedKeywords() {
        var string = StringShape.builder().id("smithy.api#String").build();
        var key = MemberShape.builder().id("smithy.example#Map$key").target("smithy.api#String").build();
        var value = MemberShape.builder().id("smithy.example#Map$value").target("smithy.api#String").build();
        var shape = MapShape.builder().id("smithy.example#Map").key(key).value(value).build();
        var index = ShapeIndex.builder().addShapes(string, shape, key, value).build();
        var document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder().withMember(OpenApiConstants.OPEN_API_MODE, true).build())
                .convert(index, shape);
        var schema = document.getRootSchema();

        assertTrue(schema.getPropertyNames().isEmpty());
    }

    @Test
    public void supportsExternalDocs() {
        String link = "https://foo.com";
        var string = StringShape.builder()
                .id("a.b#C")
                .addTrait(new ExternalDocumentationTrait(link))
                .build();
        var document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder().withMember(OpenApiConstants.OPEN_API_MODE, true).build())
                .convert(ShapeIndex.builder().addShape(string).build(), string);

        Node.assertEquals(document.getRootSchema().getExtension("externalDocs").get(), Node.from(link));
    }

    @Test
    public void supportsBoxTrait() {
        var shape = IntegerShape.builder().id("a.b#C").addTrait(new BoxTrait()).build();
        var document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder().withMember(OpenApiConstants.OPEN_API_MODE, true).build())
                .convert(ShapeIndex.builder().addShape(shape).build(), shape);

        assertThat(document.getRootSchema().getExtension("nullable").get(), equalTo(Node.from(true)));
    }

    @Test
    public void supportsDeprecatedTrait() {
        var shape = IntegerShape.builder().id("a.b#C").addTrait(DeprecatedTrait.builder().build()).build();
        var document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder().withMember(OpenApiConstants.OPEN_API_MODE, true).build())
                .convert(ShapeIndex.builder().addShape(shape).build(), shape);

        assertThat(document.getRootSchema().getExtension("deprecated").get(), equalTo(Node.from(true)));
    }

    @Test
    public void supportsInt32() {
        var shape = IntegerShape.builder().id("a.b#C").build();
        var document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder().withMember(OpenApiConstants.OPEN_API_MODE, true).build())
                .convert(ShapeIndex.builder().addShape(shape).build(), shape);

        assertThat(document.getRootSchema().getFormat().get(), equalTo("int32"));
    }

    @Test
    public void supportsInt64() {
        var shape = LongShape.builder().id("a.b#C").build();
        var document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder().withMember(OpenApiConstants.OPEN_API_MODE, true).build())
                .convert(ShapeIndex.builder().addShape(shape).build(), shape);

        assertThat(document.getRootSchema().getFormat().get(), equalTo("int64"));
    }

    @Test
    public void supportsFloatFormat() {
        var shape = FloatShape.builder().id("a.b#C").build();
        var document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder().withMember(OpenApiConstants.OPEN_API_MODE, true).build())
                .convert(ShapeIndex.builder().addShape(shape).build(), shape);

        assertThat(document.getRootSchema().getFormat().get(), equalTo("float"));
    }

    @Test
    public void supportsDoubleFormat() {
        var shape = DoubleShape.builder().id("a.b#C").build();
        var document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder().withMember(OpenApiConstants.OPEN_API_MODE, true).build())
                .convert(ShapeIndex.builder().addShape(shape).build(), shape);

        assertThat(document.getRootSchema().getFormat().get(), equalTo("double"));
    }

    @Test
    public void blobFormatDefaultsToByte() {
        var shape = BlobShape.builder().id("a.b#C").build();
        var document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder().withMember(OpenApiConstants.OPEN_API_MODE, true).build())
                .convert(ShapeIndex.builder().addShape(shape).build(), shape);

        assertThat(document.getRootSchema().getFormat().get(), equalTo("byte"));
    }

    @Test
    public void blobFormatOverriddenToBinary() {
        var shape = BlobShape.builder().id("a.b#C").build();
        var document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder()
                                .withMember(OpenApiConstants.OPEN_API_MODE, true)
                                .withMember(OpenApiConstants.OPEN_API_DEFAULT_BLOB_FORMAT, "binary")
                                .build())
                .convert(ShapeIndex.builder().addShape(shape).build(), shape);

        assertThat(document.getRootSchema().getFormat().get(), equalTo("binary"));
    }

    @Test
    public void supportsSensitiveTrait() {
        var shape = StringShape.builder().id("a.b#C").addTrait(new SensitiveTrait()).build();
        var document = JsonSchemaConverter.create()
                .config(Node.objectNodeBuilder().withMember(OpenApiConstants.OPEN_API_MODE, true).build())
                .convert(ShapeIndex.builder().addShape(shape).build(), shape);

        assertThat(document.getRootSchema().getFormat().get(), equalTo("password"));
    }
}
