/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.codegen.core;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Class that represents the contents of a Smithy trace file.
 * TraceFile's require a smithyTrace file version number, {@link ArtifactMetadata}, and
 * {@link Map} from {@link ShapeId} to a List of {@link ShapeLink} objects. TraceFile's
 * optionally have a {@link ArtifactDefinitions} object.
 * <p>
 * TraceFile handles parsing, serialization and deserialization of a Smithy trace file.
 * </p>
 * <p>
 * The smithyTrace {@link String} contains the Smithy trace file version number.
 * </p>
 * <p>
 * The shapes {@link Map} provides a mapping of absolute Smithy shape IDs to a list
 * of shape link objects. A single Smithy shape can be responsible for generating
 * multiple components in the target artifact.
 * </p>
 */
public final class TraceFile implements ToNode, ToSmithyBuilder<TraceFile> {
    public static final String SMITHY_TRACE_TEXT = "smithyTrace";
    public static final String ARTIFACT_TEXT = "artifact";
    public static final String DEFINITIONS_TEXT = "definitions";
    public static final String SHAPES_TEXT = "shapes";
    public static final String SMITHY_TRACE_VERSION = "1.0";

    private String smithyTrace;
    private ArtifactMetadata artifactMetadata;
    private ArtifactDefinitions artifactDefinitions; //Optional
    private Map<ShapeId, List<ShapeLink>> shapes;
    private SourceLocation sl = new SourceLocation("");

    private TraceFile(Builder builder) {
        smithyTrace = SmithyBuilder.requiredState(SMITHY_TRACE_TEXT, builder.smithyTrace);
        artifactMetadata = SmithyBuilder.requiredState(ARTIFACT_TEXT, builder.artifactMetadata);
        shapes = SmithyBuilder.requiredState(SHAPES_TEXT, MapUtils.copyOf(builder.shapes));
        artifactDefinitions = builder.artifactDefinitions;
    }

    /**
     * Parses and validates the trace file passed in as filename
     * and instantiates smithyTrace and defintions, and fills
     * artifactMetadata and shapes.
     *
     * @param filename the absolute or relative path of tracefile
     * @throws FileNotFoundException if filename is not found for reading
     */
    public static TraceFile parseTraceFile(URI filename) throws FileNotFoundException {
        InputStream stream = new FileInputStream(new File(filename));
        ObjectNode node = (ObjectNode) Node.parse(stream);
        return createFromNode(node);
    }

    /**
     * Converts ObjectNode into TraceFile.
     *
     * @param value an ObjectNode that represents the entire trace file.
     */
    public static TraceFile createFromNode(Node value) {
        ObjectNode node = value.expectObjectNode();
        Builder builder = builder()
                .smithyTrace(node.expectStringMember(SMITHY_TRACE_TEXT).getValue())
                .artifact(ArtifactMetadata.createFromNode(node.expectObjectMember(ARTIFACT_TEXT)));

        //parse shapes
        Map<StringNode, Node> shapeMap = node.expectObjectMember(SHAPES_TEXT).getMembers();
        for (Map.Entry<StringNode, Node> entry : shapeMap.entrySet()) {
            for (Node linkNode : (entry.getValue().expectArrayNode()).getElements()) {
                builder.addShapeLink(entry.getKey().getValue(), ShapeLink.createFromNode(linkNode));
            }
        }

        //parse definitions
        if (node.containsMember(DEFINITIONS_TEXT)) {
            builder.definitions(ArtifactDefinitions.createFromNode(node.expectObjectMember(DEFINITIONS_TEXT)));
        }

        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Writes the TraceFile JSON ObjectNode
     * to construct a trace file.
     *
     * @param fileName absolute or relative path to write the trace file
     * @throws IOException if there is an error writing to fileName
     */
    public void writeTraceFile(String fileName) throws IOException {
        Writer writer = new OutputStreamWriter(new FileOutputStream(new File(fileName)), StandardCharsets.UTF_8);
        PrintWriter pw = new PrintWriter(writer);
        pw.print(Node.prettyPrintJson(toNode(), "  "));
        pw.close();
    }

    /**
     * Converts TraceFile instance variables into an
     * ObjectNode.
     *
     * @return ObjectNode representation of a TraceFile.
     */
    @Override
    public ObjectNode toNode() {
        //constructing shapes ObjectNode map
        ObjectNode.Builder shapesBuilder = ObjectNode.objectNodeBuilder();
        for (Map.Entry<ShapeId, List<ShapeLink>> entry : shapes.entrySet()) {
            shapesBuilder.withMember(entry.getKey().toString(),
                    entry.getValue().stream().map(ShapeLink::toNode).collect(ArrayNode.collect()));
        }

        //returning ObjectNode for TraceFile
        return ObjectNode.objectNodeBuilder()
                .withMember(SMITHY_TRACE_TEXT, smithyTrace)
                .withMember(ARTIFACT_TEXT, artifactMetadata)
                .withOptionalMember(DEFINITIONS_TEXT, getArtifactDefinitions())
                .withMember(SHAPES_TEXT, shapesBuilder.build())
                .build();
    }

    /**
     * Finds invalid types and tags and either removes them or throws an error depending on
     * whether toThrow is true or false.
     *
     * @throws ExpectationNotMetException if a type or tag in shapes is not in definitions.
     */
    public void validateTypesAndTags() {
        //The optional ArtifactDefinitions must be non-null to call this method
        Objects.requireNonNull(artifactDefinitions);

        //for each entry in the shapes map
        for (Map.Entry<ShapeId, List<ShapeLink>> entry : shapes.entrySet()) {
            //for each ShapeLink in entry's List<ShapeLink>
            for (ShapeLink link : entry.getValue()) {
                //checking if link's type is in artifactDefinitions
                if (!artifactDefinitions.getTypes().containsKey(link.getType())) {
                    throw new ExpectationNotMetException(entry.getKey().toString()
                            + " contains types that aren't in definitions.", sl);
                }

                //checking if link's tags are all in artifactDefinitions
                Optional<List<String>> tags = link.getTags();
                if (tags.isPresent()) {
                    for (String tag : tags.get())
                        if (!artifactDefinitions.getTags().containsKey(tag)) {
                            throw new ExpectationNotMetException(entry.getKey().toString() + " " + tag
                                    + " is a tag that isn't in definitions.", sl);
                        }
                }
            }
        }
    }

    /**
     * Parses model and determines whether the trace file object meets the specs of the model by checking if
     * the trace file contains all the ShapeIds in the model and the model contains all the ShapeIDs in the trace file.
     *
     * @param modelResourceName the model name to validate the trace file against. Model should be in the
     *                          resources file.
     * @throws ExpectationNotMetException if model contains a ShapeID not in TraceFile or TraceFile contains a ShapeID
     *                                    not in model.
     */
    public void validateModel(String modelResourceName) {
        Model model = Model.assembler()
                .addImport(getClass().getResource(modelResourceName))
                .assemble()
                .unwrap();

        //model contains all the shapeIds in shapes.keySet()
        for (ShapeId id : shapes.keySet()) {
            model.expectShape(id);
        }

        //shapes.keySet() contains all the shapeIds in model, we don't care about shapes in smithy.api namespace
        for (Shape shape : model.toSet()) {
            ShapeId id = shape.getId();
            if (!id.getNamespace().equals("smithy.api") && !shapes.containsKey(id)) {
                throw new ExpectationNotMetException("shapes does not contain" + id.toString() + " but model does", sl);
            }
        }
    }

    /**
     * Gets this TraceFile's smithyTrace.
     *
     * @return a String representing trace file ID, or null if ID has not been set
     */
    public String getSmithyTrace() {
        return smithyTrace;
    }

    /**
     * Gets this TraceFile's ArtifactMetadata.
     *
     * @return an ArtifactMetadata object, or null if ArtifactMetadata has not been set.
     */
    public ArtifactMetadata getArtifactMetadata() {
        return artifactMetadata;
    }

    /**
     * Gets this TraceFile's Definitions.
     *
     * @return an Optional Definitions container that contains this TraceFile's Definition
     * or isEmpty if Definition's has not been set.
     */
    public Optional<ArtifactDefinitions> getArtifactDefinitions() {
        return Optional.ofNullable(artifactDefinitions);
    }

    /**
     * Gets this TraceFile's Shapes map.
     *
     * @return a Map from ShapeIDs to a list of ShapeLink's that represents the contents of the
     * shapes tag in the trace file, or null if shapes has not been set.
     */
    public Map<ShapeId, List<ShapeLink>> getShapes() {
        return shapes;
    }

    /**
     * Take this object and create a builder that contains all of the
     * current property values of this object.
     *
     * @return a builder for type T
     */
    @Override
    public Builder toBuilder() {
        return builder()
                .artifact(artifactMetadata)
                .smithyTrace(smithyTrace)
                .definitions(artifactDefinitions)
                .shapes(shapes);
    }

    /**
     * Builder for constructing TraceFile's from scratch.
     */
    public static final class Builder implements SmithyBuilder<TraceFile> {

        private String smithyTrace = SMITHY_TRACE_VERSION;
        private ArtifactDefinitions artifactDefinitions;
        private ArtifactMetadata artifactMetadata;
        private Map<ShapeId, List<ShapeLink>> shapes = new HashMap<>();

        /**
         * @return The TraceFile.
         */
        public TraceFile build() {
            return new TraceFile(this);
        }

        /**
         * @param smithyTrace Trace file version number.
         * @return This builder.
         */
        public Builder smithyTrace(String smithyTrace) {
            this.smithyTrace = smithyTrace;
            return this;
        }

        /**
         * @param artifactDefinitions Trace file definitions.
         * @return This builder.
         */
        public Builder definitions(ArtifactDefinitions artifactDefinitions) {
            this.artifactDefinitions = artifactDefinitions;
            return this;
        }

        /**
         * @param artifactMetadata Trace file ArtifactMetadata.
         * @return This builder.
         */
        public Builder artifact(ArtifactMetadata artifactMetadata) {
            this.artifactMetadata = artifactMetadata;
            return this;
        }

        /**
         * Adds a ShapeLink to this ShapeId in the TraceFile's shapes map.
         *
         * @param id   ShapeId
         * @param link ShapeLink corresponding to ShapeId
         * @return This builder.
         */
        public Builder addShapeLink(ShapeId id, ShapeLink link) {
            if (!this.shapes.containsKey(id)) {
                this.shapes.put(id, new ArrayList<>());
            }
            this.shapes.get(id).add(link);
            return this;
        }

        /**
         * Adds a ShapeLink to this ShapeId in the TraceFile's shapes map.
         *
         * @param idString ShapeId represented as a string.
         * @param link     ShapeLink corresponding to ShapeId
         * @return This builder.
         */
        public Builder addShapeLink(String idString, ShapeLink link) {
            return addShapeLink(ShapeId.from(idString), link);
        }

        /**
         * Adds a list of ShapeLinks to this ShapeId in the TraceFile's shapes map.
         *
         * @param id       ShapeId.
         * @param linkList List of ShapeLinks corresponding to a ShapeId.
         * @return This builder.
         */
        public Builder addShapeLinkList(ShapeId id, List<ShapeLink> linkList) {
            this.shapes.put(id, linkList);
            return this;
        }

        /**
         * Adds a list of ShapeLinks to this ShapeId in the TraceFile's shapes map.
         *
         * @param idString ShapeId as a String.
         * @param linkList List of ShapeLinks corresponding to a ShapeId.
         * @return This builder.
         */
        public Builder addShapeLinkList(String idString, List<ShapeLink> linkList) {
            return addShapeLinkList(ShapeId.from(idString), linkList);
        }

        /**
         * @param shapes shapes map for TraceFile.
         * @return This builder.
         */
        public Builder shapes(Map<ShapeId, List<ShapeLink>> shapes) {
            this.shapes = shapes;
            return this;
        }

    }

}
