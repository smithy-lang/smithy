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

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.FromNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Class that represents the contents of a Smithy trace file.
 * TraceFile's require a smithyTrace file version number, {@link ArtifactMetadata}, and
 * {@link Map} from {@link ShapeId} to a List of {@link ShapeLink} objects. TraceFile's
 * optionally have a {@link Definitions} object.
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
public class TraceFile implements ToNode, FromNode, ValidateRequirements {
    public static final String SMITHY_TRACE_TEXT = "smithyTrace";
    public static final String ARTIFACT_TEXT = "artifact";
    public static final String DEFINITIONS_TEXT = "definitions";
    public static final String SHAPES_TEXT = "shapes";

    public static final String SMITHY_TRACE_VERSION = "1.0";

    private String smithyTrace;
    private ArtifactMetadata artifactMetadata;
    private Definitions definitions; //Optional
    private Map<ShapeId, List<ShapeLink>> shapes;
    private NodeMapper nodeMapper = new NodeMapper();
    private SourceLocation sl = new SourceLocation("");

    /**
     * Default constructor for TraceFile for use when parsing.
     */
    public TraceFile() {
    }

    private TraceFile(String smithyTrace, Definitions definitions, ArtifactMetadata artifactMetadata,
                      Map<ShapeId, List<ShapeLink>> shapes) {

        this.smithyTrace = smithyTrace;
        this.definitions = definitions;
        this.artifactMetadata = artifactMetadata;
        this.shapes = shapes;
    }

    /**
     * Parses and validates the trace file passed in as filename
     * and instantiates smithyTrace and defintions, and fills
     * artifactMetadata and shapes.
     *
     * @param filename the absolute or relative path of tracefile
     * @throws FileNotFoundException if filename is not found for reading
     */
    public void parseTraceFile(URI filename) throws FileNotFoundException {
        InputStream stream = new FileInputStream(new File(filename));
        ObjectNode node = (ObjectNode) Node.parse(stream);
        fromNode(node);
    }

    /**
     * Converts ObjectNode into TraceFile.
     *
     * @param jsonNode an ObjectNode that represents the entire trace file.
     */
    @Override
    public void fromNode(Node jsonNode) {
        //throw error if trace file top level is incorrectly formatted
        ObjectNode node = jsonNode.expectObjectNode();

        nodeMapper.setWhenMissingSetter(NodeMapper.WhenMissing.FAIL);

        //parse trace
        smithyTrace = nodeMapper.deserialize(node.expectStringMember(SMITHY_TRACE_TEXT), String.class);

        //parse metadata
        artifactMetadata = nodeMapper.deserialize(node.expectObjectMember(ARTIFACT_TEXT), ArtifactMetadata.class);

        //parse shapes
        shapes = new HashMap<>();
        Map<StringNode, Node> shapeMap = node.expectObjectMember(SHAPES_TEXT).getMembers();
        for (Map.Entry<StringNode, Node> entry : shapeMap.entrySet()) {
            ShapeId shapeId = ShapeId.from(entry.getKey().getValue());
            List<ShapeLink> list =
                    nodeMapper.deserializeCollection(entry.getValue(), ArrayList.class, ShapeLink.class);
            shapes.put(shapeId, list);
        }

        //parse definitions
        if (node.containsMember(DEFINITIONS_TEXT)) {
            definitions = nodeMapper.deserialize(node.expectObjectMember(DEFINITIONS_TEXT), Definitions.class);
            definitions.validateRequiredFields();
        }

        //error checking
        validateRequiredFields();
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
        //error checking
        validateRequiredFields();

        Map<String, Object> toSerialize = new HashMap<>();
        toSerialize.put(SMITHY_TRACE_TEXT, smithyTrace);
        toSerialize.put(ARTIFACT_TEXT, artifactMetadata);
        if (definitions != null) {
            toSerialize.put(DEFINITIONS_TEXT, definitions);
        }
        toSerialize.put(SHAPES_TEXT, shapes);

        return nodeMapper.serialize(toSerialize).expectObjectNode();
    }

    /**
     * Finds invalid types and tags and either removes them or throws an error depending on
     * whether toThrow is true or false.
     *
     * @throws ExpectationNotMetException if a type or tag in shapes is not in definitions.
     */
    public void validateTypesAndTags() {
        Objects.requireNonNull(shapes);
        Objects.requireNonNull(definitions);
        for (Map.Entry<ShapeId, List<ShapeLink>> entry : shapes.entrySet()) {
            Iterator<ShapeLink> i = entry.getValue().iterator();
            while (i.hasNext()) {
                ShapeLink link = i.next();
                if (!definitions.getTypes().containsKey(link.getType())) {
                    throw new ExpectationNotMetException(entry.getKey().toString()
                            + " contains types that aren't in definitions.", sl);
                } else {
                    Optional<List<String>> tags = link.getTags();
                    if (tags.isPresent()) {
                        Iterator<String> iter = tags.get().iterator();
                        while (iter.hasNext()) {
                            String next = iter.next();
                            if (!definitions.getTags().containsKey(next)) {
                                throw new ExpectationNotMetException(entry.getKey().toString() + " " + next
                                        + " is a tag that isn't in definitions.", sl);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Parses model and determines whether the trace file object meets the specs of the model by checking if
     * the trace file contains all the shape Ids in the model and the model contains all the ShapeIDs in the trace file.
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

        //error check - shapes must be non-null to use this method
        Objects.requireNonNull(shapes);

        //model contains all the shapeIds in shapes.keySet()
        for (ShapeId id : shapes.keySet()) {
            model.expectShape(id);
        }

        //shapes.keySet() contains all the shapeIds in model
        for (Shape shape : model.toSet()) {
            ShapeId id = shape.getId();
            if (!shapes.containsKey(id)) {
                throw new ExpectationNotMetException("shapes does not contain" + id.toString() + " but model does", sl);
            }
        }
    }

    /**
     * Checks if all of the objects required fields are not null.
     *
     * @throws NullPointerException if any of the required fields are null
     */
    @Override
    public void validateRequiredFields() {
        Objects.requireNonNull(smithyTrace);
        Objects.requireNonNull(artifactMetadata);
        Objects.requireNonNull(shapes);
        artifactMetadata.validateRequiredFields();
        for (Map.Entry<ShapeId, List<ShapeLink>> entry : shapes.entrySet()) {
            for (ShapeLink link : entry.getValue()) {
                link.validateRequiredFields();
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
     * Sets this TraceFile's smithyTrace version String.
     *
     * @param smithyTrace String containing smithy trace version number.
     */
    public void setSmithyTrace(String smithyTrace) {
        this.smithyTrace = smithyTrace;
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
     * Sets this TraceFile's Artifact Metadata.
     *
     * @param artifactMetadata ArtifactMetadata object for TraceFile.
     */
    public void setArtifactMetadata(ArtifactMetadata artifactMetadata) {
        this.artifactMetadata = artifactMetadata;
    }

    /**
     * Gets this TraceFile's Definitions.
     *
     * @return an Optional Definitions container that contains this TraceFile's Definition
     * or isEmpty if Definition's has not been set.
     */
    public Optional<Definitions> getDefinitions() {
        return Optional.ofNullable(definitions);
    }

    /**
     * Sets this TraceFile's Definitions.
     *
     * @param definitions Definitions object for TraceFile.
     */
    public void setDefinitions(Definitions definitions) {
        this.definitions = definitions;
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
     * Sets this TraceFile's shapes map.
     *
     * @param shapes a map from ShapeIds to a list of corresponding ShapeLink
     *               objects
     */
    public void setShapes(Map<ShapeId, List<ShapeLink>> shapes) {
        this.shapes = shapes;
    }

    /**
     * Builder for constructing TraceFile's from scratch.
     */
    public static class TraceFileBuilder {

        private String smithyTrace;
        private Definitions definitions;
        private ArtifactMetadata artifactMetadata;
        private Map<ShapeId, List<ShapeLink>> shapes;

        /**
         * Constructor for builder with all required parameters.
         * SmithyTrace is automatically set.
         *
         * @param artifactMetadata ArtifactMetadata for TraceFile
         * @param shapes           Map of ShapeId to Lists of ShapeLinks
         */
        public TraceFileBuilder(ArtifactMetadata artifactMetadata, Map<ShapeId, List<ShapeLink>> shapes) {
            this.smithyTrace = SMITHY_TRACE_VERSION;
            this.artifactMetadata = artifactMetadata;
            this.shapes = shapes;
        }

        /**
         * Constructor for builder that sets SmithTrace and
         * instantiates the shapes map.
         */
        public TraceFileBuilder() {
            this.shapes = new HashMap<>();
            this.smithyTrace = SMITHY_TRACE_VERSION;
        }

        /**
         * @param smithyTrace Trace file version number.
         * @return This builder.
         */
        public TraceFileBuilder setSmithyTrace(String smithyTrace) {
            this.smithyTrace = smithyTrace;
            return this;
        }

        /**
         * @param definitions Trace file definitions.
         * @return This builder.
         */
        public TraceFileBuilder setDefinitions(Definitions definitions) {
            this.definitions = definitions;
            return this;
        }

        /**
         * @param artifactMetadata Trace file ArtifactMetadata.
         * @return This builder.
         */
        public TraceFileBuilder setArtifactMetadata(ArtifactMetadata artifactMetadata) {
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
        public TraceFileBuilder addShapeLink(ShapeId id, ShapeLink link) {
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
        public TraceFileBuilder addShapeLink(String idString, ShapeLink link) {
            return addShapeLink(ShapeId.from(idString), link);
        }

        /**
         * Adds a list of ShapeLinks to this ShapeId in the TraceFile's shapes map.
         *
         * @param id       ShapeId.
         * @param linkList List of ShapeLinks corresponding to a ShapeId.
         * @return This builder.
         */
        public TraceFileBuilder addShapeLinkList(ShapeId id, List<ShapeLink> linkList) {
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
        public TraceFileBuilder addShapeLinkList(String idString, List<ShapeLink> linkList) {
            return addShapeLinkList(ShapeId.from(idString), linkList);
        }

        /**
         * @param shapes shapes map for TraceFile.
         * @return This builder.
         */
        public TraceFileBuilder setShapes(Map<ShapeId, List<ShapeLink>> shapes) {
            this.shapes = shapes;
            return this;
        }

        /**
         * @return The TraceFile.
         */
        public TraceFile build() {
            return new TraceFile(smithyTrace, definitions, artifactMetadata, shapes);
        }
    }
}
