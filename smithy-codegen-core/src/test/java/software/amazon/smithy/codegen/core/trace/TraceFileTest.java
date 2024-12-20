/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.trace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.MockManifest;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;

class TraceFileTest {
    /**
     * Correct TraceFile use tests
     * Test creating, parsing, validating and writing a TraceFile from scratch.
     * Test parse and write functionality combined on a correct trace file
     * Test parse and write functionality with nodes with single children instead of arrays of children
     * Test parse and write functionality combined on a correct trace file without the optional definitions section
     */

    @Test
    void assertWriteTraceFileFromScratchWorks() throws IOException, URISyntaxException {
        /**
         * Building TraceMetadata - this builder uses setTimestampAsNow, but you can also use a different
         * builder constructor to specify a custom timestamp.
         * The required fields are id, version, type, timestamp.
         */

        String id = "software.amazon.awssdk.services:snowball:2.10.79";
        String version = "2.10.79";
        String type = "Java";
        String typeVersion = "1.8";
        String homepage = "https://github.com/aws/aws-sdk-java-v2/";
        TraceMetadata traceMetadata = TraceMetadata.builder()
                .id(id)
                .version(version)
                .type(type)
                .setTimestampAsNow()
                .homepage(homepage)
                .typeVersion(typeVersion)
                .build();

        /**
         * Building Definitions - this example uses addTag and addType to add individual key value pairs.
         * There's another builder constructor that allows you to add the entire tags/types Map<String,String> without
         * having to add them individually.
         */
        ArtifactDefinitions artifactDefinitions = ArtifactDefinitions.builder()
                .addTag("service", "Service client")
                .addTag("request", "AWS SDK request type")
                .addTag("requestBuilder", "AWS SDK request builder")
                .addType("TYPE", "Class, interface (including annotation type), or enum declaration")
                .build();

        /**
         * Building TraceFile - build the trace file by passing the different objects to the builder
         * You can either construct the shapes map yourself, and add it to TraceFile, or you can add each shape
         * individually as shown below.
         * SmithyTrace is a constant in my code set to 1.0 for this version, so you don't have to worry about
         * setting it.
         */
        TraceFile.Builder traceFileBuilder = TraceFile.builder()
                .metadata(traceMetadata)
                .definitions(artifactDefinitions);

        //adding one ShapeLink to TraceFile
        type = "TYPE";
        id = "software.amazon.awssdk.services.snowball.SnowballClient";
        traceFileBuilder.addShapeLink("com.amazonaws.snowball#Snowball",
                ShapeLink.builder()
                        .type(type)
                        .id(id)
                        .addTag("service")
                        .build());

        //adding multiple ShapeLinks for the same ShapeId; can also add a List<ShapeLink>
        type = "TYPE";
        id = "software.amazon.awssdk.services.snowball.model.ListClusterJobsRequest$Builder";
        traceFileBuilder.addShapeLink("com.amazonaws.snowball#ListClustersRequest",
                ShapeLink.builder()
                        .type(type)
                        .id(id)
                        .addTag("requestBuilder")
                        .build());
        type = "TYPE";
        id = "software.amazon.awssdk.services.snowball.model.ListClusterJobsRequest";
        traceFileBuilder.addShapeLink("com.amazonaws.snowball#ListClustersRequest",
                ShapeLink.builder()
                        .type(type)
                        .id(id)
                        .addTag("request")
                        .build());

        //finally, build the TraceFile
        TraceFile traceFile = traceFileBuilder.build();

        /**
         * After creating a TraceFile, you may want to validate it. You can validate whether all your TraceFile
         * matches your model by checking if all the ShapeIds in a model file are in the TraceFile and all the
         * ShapeIds in a model file are in the TraceFile. This line is commented out here because there is no
         * Smithy model for this TraceFile, so it cannot be validated against a model. See validateModel tests
         * below for complete use cases.
         */
        //traceFile.validateModel(assembleModelTestHelper(("Your model resource path here");

        //Then write the TraceFile, just specify path/filename string.
        String filename = "trace-file-output.json";
        MockManifest manifest = writeTraceFileTestHelper(traceFile, filename);

        /**
         * Then parse the trace file, specify the files URI for parsing
         * Parsing fills all required fields and checks that they're filled
         */

        TraceFile traceFile2 = parseTraceFileFromManifest(manifest, filename);

        //few assorted checks
        assertThat(traceFile2.getMetadata().getId(), equalTo("software.amazon.awssdk.services:snowball:2.10.79"));
        assertThat(traceFile2.getArtifactDefinitions().get().getTags().keySet(),
                containsInAnyOrder("service",
                        "request",
                        "requestBuilder"));
        assertThat(traceFile2.getShapes().get(ShapeId.from("com.amazonaws.snowball#Snowball")).get(0).getType(),
                equalTo("TYPE"));
        assertThat(traceFile2.getSmithyTrace(), equalTo("1.0"));
    }

    @Test
    void assertsParseTraceFileWorksWithCorrectTraceFile() throws URISyntaxException, FileNotFoundException {
        TraceFile traceFile = parseTraceFileFromFile(getClass().getResource("trace-file.json").toURI());

        assertThat(traceFile.getMetadata().getId(), equalTo("software.amazon.awssdk.services:snowball:2.10.79"));
        assertThat(traceFile.getArtifactDefinitions().get().getTags().keySet(),
                containsInAnyOrder("service",
                        "request",
                        "response",
                        "requestBuilder",
                        "responseBuilder"));
        assertThat(traceFile.getShapes().get(ShapeId.from("com.amazonaws.snowball#Snowball")).get(0).getType(),
                equalTo("TYPE"));
        assertThat(traceFile.getSmithyTrace(), equalTo("1.0"));
    }

    @Test
    void assertsWriteTraceFileWorksWithCorrectTraceFile() throws URISyntaxException, IOException {
        TraceFile traceFile = parseTraceFileFromFile(getClass().getResource("trace-file.json").toURI());
        MockManifest manifest = writeTraceFileTestHelper(traceFile, "trace-file-output.json");
        TraceFile traceFile2 = parseTraceFileFromManifest(manifest, "trace-file-output.json");

        assertThat(traceFile2.getMetadata().getId(), equalTo("software.amazon.awssdk.services:snowball:2.10.79"));
        assertThat(traceFile2.getArtifactDefinitions().get().getTags().keySet(),
                containsInAnyOrder("service",
                        "request",
                        "response",
                        "requestBuilder",
                        "responseBuilder"));
        assertThat(traceFile2.getShapes().get(ShapeId.from("com.amazonaws.snowball#Snowball")).get(0).getType(),
                equalTo("TYPE"));
        assertThat(traceFile2.getSmithyTrace(), equalTo("1.0"));
    }

    @Test
    void assertParseWriteWorksWithoutDefinitions() throws IOException, URISyntaxException {
        TraceFile traceFile = parseTraceFileFromFile(getClass().getResource("trace-file.json").toURI());

        //set definitions to null before writing and parsing again
        traceFile = traceFile.toBuilder().definitions(null).build();

        MockManifest manifest = writeTraceFileTestHelper(traceFile, "trace-file-output.json");
        TraceFile traceFile2 = parseTraceFileFromManifest(manifest, "trace-file-output.json");

        assertThat(traceFile.getMetadata().getId(), equalTo("software.amazon.awssdk.services:snowball:2.10.79"));
        assertThat(traceFile.getShapes().get(ShapeId.from("com.amazonaws.snowball#Snowball")).get(0).getType(),
                equalTo("TYPE"));
        assertThat(traceFile.getSmithyTrace(), equalTo("1.0"));
    }

    /**
     * Validate TraceFile use tests
     * validateTypesOrTags throws with invalid type, and throws with invalid tags
     * validateModel works with valid input, throws with invalid model/trace file pair
     */

    @Test
    void buildThrowsOnInvalidType() throws ExpectationNotMetException {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            TraceFile traceFile = parseTraceFileFromFile(getClass().getResource("trace-file.json").toURI());
            traceFile.toBuilder()
                    .addShapeLink("com.amazonaws.snowball#Snowball",
                            ShapeLink
                                    .builder()
                                    .id("id")
                                    .type("fake_type")
                                    .build())
                    .build();
        });
    }

    @Test
    void buildThrowsOnInvalidTag() throws ExpectationNotMetException {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            TraceFile traceFile = parseTraceFileFromFile(getClass().getResource("trace-file.json").toURI());
            traceFile.toBuilder()
                    .addShapeLink("com.amazonaws.snowball#Snowball",
                            ShapeLink
                                    .builder()
                                    .id("id")
                                    .type("TYPE")
                                    .addTag("fake_tag")
                                    .build())
                    .build();
        });
    }

    @Test
    void validateModelDoesNotThrowOnValidTraceFileModelPair()
            throws ExpectationNotMetException, URISyntaxException, FileNotFoundException {
        Assertions.assertDoesNotThrow(() -> {
            TraceFile traceFile =
                    parseTraceFileFromFile(getClass().getResource("trace-for-simple-service-validation.json").toURI());
            traceFile.validateModel(assembleModelTestHelper("simple-service.smithy"));
        });
    }

    @Test
    void validateModelThrowsOnTraceFileWithoutAllModelShapeIDs()
            throws ExpectationNotMetException, URISyntaxException, FileNotFoundException {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            TraceFile traceFile =
                    parseTraceFileFromFile(getClass().getResource("trace-for-model-validation.json").toURI());
            traceFile.validateModel(assembleModelTestHelper("service-with-shapeids.smithy"));
        });
    }

    @Test
    void validateModelThrowsOnModelWithoutAllTraceFileShapeIds()
            throws ExpectationNotMetException, URISyntaxException, FileNotFoundException {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            TraceFile traceFile =
                    parseTraceFileFromFile(getClass().getResource("trace-for-simple-service-validation.json").toURI());
            Model model = assembleModelTestHelper("simple-service.smithy");
            //add a shape to our model that's not in our trace file
            model = model.toBuilder().addShape(StringShape.builder().id("ns.foo#bar").build()).build();
            traceFile.validateModel(model);
        });
    }

    /**
     * Incorrect build tests:
     * Test null values for all required variables of TraceFile
     */

    @Test
    void buildThrowsWithNoSmithyTrace() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            TraceFile traceFile = parseTraceFileFromFile(getClass().getResource("trace-file.json").toURI());
            //set smithyTrace to null before writing and parsing again
            traceFile.toBuilder().smithyTrace(null).build();
        });
    }

    @Test
    void buildThrowsWithNoTraceMetadata() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            TraceFile traceFile = parseTraceFileFromFile(getClass().getResource("trace-file.json").toURI());
            //set to null before writing and parsing again
            traceFile.toBuilder().metadata(null).build();
        });
    }

    @Test
    void buildThrowsWithNoShapes() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            TraceFile traceFile = parseTraceFileFromFile(getClass().getResource("trace-file.json").toURI());
            //set to null before writing and parsing again
            traceFile.toBuilder().shapes(null).build();
        });
    }

    TraceFile parseTraceFileFromFile(URI filename) throws FileNotFoundException {
        InputStream stream = new FileInputStream(new File(filename));
        return TraceFile.fromNode(Node.parse(stream).expectObjectNode());
    }

    TraceFile parseTraceFileFromManifest(MockManifest manifest, String filename) {
        return TraceFile.fromNode(Node.parse(manifest.expectFileString(filename)));
    }

    MockManifest writeTraceFileTestHelper(TraceFile traceFile, String fileName) throws IOException {
        MockManifest manifest = new MockManifest();
        manifest.writeFile(fileName, Node.prettyPrintJson(traceFile.toNode()));
        return manifest;
    }

    Model assembleModelTestHelper(String modelResourceName) {
        return Model.assembler()
                .addImport(getClass().getResource(modelResourceName))
                .assemble()
                .unwrap();
    }

}
