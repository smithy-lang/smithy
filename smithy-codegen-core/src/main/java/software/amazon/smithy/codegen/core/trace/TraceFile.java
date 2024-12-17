/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.Prelude;
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

/**
 * Class that represents the contents of a Smithy trace file.
 * TraceFile's require a smithyTrace file version number, {@link TraceMetadata}, and
 * {@link Map} from {@link ShapeId} to a List of {@link ShapeLink} objects. TraceFile's
 * optionally have a {@link ArtifactDefinitions} object. TraceFile handles parsing, serialization
 * and deserialization of a Smithy trace file.
 */
public final class TraceFile implements ToNode, ToSmithyBuilder<TraceFile> {
    public static final String SMITHY_TRACE_TEXT = "smithyTrace";
    public static final String METADATA_TEXT = "metadata";
    public static final String DEFINITIONS_TEXT = "definitions";
    public static final String SHAPES_TEXT = "shapes";
    public static final String SMITHY_TRACE_VERSION = "1.0";

    private String smithyTrace;
    private TraceMetadata metadata;
    private ArtifactDefinitions artifactDefinitions; //Optional
    private Map<ShapeId, List<ShapeLink>> shapes;

    private TraceFile(Builder builder) {
        smithyTrace = SmithyBuilder.requiredState(SMITHY_TRACE_TEXT, builder.smithyTrace);
        metadata = SmithyBuilder.requiredState(METADATA_TEXT, builder.metadata);
        if (builder.shapes.isEmpty()) {
            throw new IllegalStateException("TraceFile's shapes field must not be empty to build it.");
        }
        shapes = MapUtils.copyOf(builder.shapes);
        artifactDefinitions = builder.artifactDefinitions;

        //validate we received a TraceFile where ShapeLink types and tags match definitions
        validateTypesAndTags();
    }

    /**
     * Converts ObjectNode into TraceFile.
     *
     * @param value an ObjectNode that represents the entire trace file.
     * @return TraceFile produced from an ObjectNode.
     */
    public static TraceFile fromNode(Node value) {
        ObjectNode node = value.expectObjectNode();
        Builder builder = builder()
                .smithyTrace(node.expectStringMember(SMITHY_TRACE_TEXT).getValue())
                .metadata(TraceMetadata.fromNode(node.expectObjectMember(METADATA_TEXT)));

        //parse shapes
        Map<StringNode, Node> shapeMap = node.expectObjectMember(SHAPES_TEXT).getMembers();
        for (Map.Entry<StringNode, Node> entry : shapeMap.entrySet()) {
            for (Node linkNode : (entry.getValue().expectArrayNode()).getElements()) {
                builder.addShapeLink(entry.getKey().getValue(), ShapeLink.fromNode(linkNode));
            }
        }

        //parse definitions
        if (node.containsMember(DEFINITIONS_TEXT)) {
            builder.definitions(ArtifactDefinitions.fromNode(node.expectObjectMember(DEFINITIONS_TEXT)));
        }

        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
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
            String shapeId = entry.getKey().toString();
            ArrayNode shapeListNode = entry.getValue() //get list of ShapeLinks
                    .stream()
                    .map(ShapeLink::toNode) //convert each ShapeLink to an ObjectNode
                    .collect(ArrayNode.collect()); //collect each ObjectNode in an ArrayNode
            shapesBuilder.withMember(shapeId, shapeListNode);
        }

        //returning ObjectNode for TraceFile
        return ObjectNode.objectNodeBuilder()
                .withMember(SMITHY_TRACE_TEXT, smithyTrace)
                .withMember(METADATA_TEXT, metadata)
                .withOptionalMember(DEFINITIONS_TEXT, getArtifactDefinitions())
                .withMember(SHAPES_TEXT, shapesBuilder.build())
                .build();
    }

    /**
     * Throws an error if any ShapeLink object contains a tag or type that is not in artifactDefinition's.
     * This method should be called after creating a TraceFile object to verify that all the types and tags
     * in shapes have been defined in artifactDefinition's. This TraceFile's ArtifactDefinitions object
     * MUST be defined prior to calling this method.
     *
     * @throws ExpectationNotMetException if a type or tag in shapes is not in artifactDefinitions.
     */
    private void validateTypesAndTags() {
        //only validate if the optional ArtifactDefinitions is non-null
        if (artifactDefinitions != null) {
            //for each entry in the shapes map
            for (Map.Entry<ShapeId, List<ShapeLink>> entry : shapes.entrySet()) {
                //for each ShapeLink in entry's List<ShapeLink>
                for (ShapeLink link : entry.getValue()) {
                    //checking if link's type is in artifactDefinitions
                    if (!artifactDefinitions.getTypes().containsKey(link.getType())) {
                        throw new ExpectationNotMetException(entry.getKey().toString()
                                + " contains types that aren't in definitions.", SourceLocation.none());
                    }

                    //checking if link's tags are all in artifactDefinitions
                    List<String> tags = link.getTags();
                    for (String tag : tags) {
                        if (!artifactDefinitions.getTags().containsKey(tag)) {
                            throw new ExpectationNotMetException(entry.getKey().toString() + " " + tag
                                    + " is a tag that isn't in definitions.", SourceLocation.none());
                        }
                    }
                }
            }
        }
    }

    /**
     * Parses model and determines whether the trace file object meets the specs of the model by checking if
     * the trace file contains all the ShapeIds in the model and the model contains all the ShapeIDs in the trace file.
     *
     * @param model the Smithy model  to validate the trace file against.
     * @throws ExpectationNotMetException if model contains a ShapeID not in TraceFile or TraceFile contains a ShapeID
     *                                    not in model.
     */
    public void validateModel(Model model) {
        Set<ShapeId> fileShapes = new HashSet<>(shapes.keySet());
        Set<ShapeId> fileShapesCopy = new HashSet<>(fileShapes);

        Set<ShapeId> modelShapes = model.toSet()
                .stream()
                .filter(shape -> !Prelude.isPreludeShape(shape)) //ignore shapes in smithy.api namespace
                .map(Shape::getId) //get ShapeId for each shape
                .collect(Collectors.toSet()); //collect into a set of ShapeIds

        //get shapes in TraceFile that aren't in model;
        fileShapes.removeAll(modelShapes);
        //get shapes in model that aren't in TraceFile;
        modelShapes.removeAll(fileShapesCopy);

        //if there are shapes in TraceFile not in model or vice versa
        if (fileShapes.size() > 0 || modelShapes.size() > 0) {
            //building the error message
            StringBuilder errorMessageBuilder = new StringBuilder().append("Model validation failed.");

            if (fileShapes.size() > 0) {
                errorMessageBuilder.append(" The following shapes are in the TraceFile, but missing from the model: ");
                fileShapes.stream().forEach(id -> {
                    errorMessageBuilder.append(id.toString()).append(", ");
                });
                errorMessageBuilder.append(". ");
            }

            if (modelShapes.size() > 0) {
                errorMessageBuilder.append("The following shapes are in the model, but missing from the TraceFile: ");
                modelShapes.stream().forEach(id -> {
                    errorMessageBuilder.append(id.toString()).append(", ");
                });
                errorMessageBuilder.append(". ");
            }

            throw new ExpectationNotMetException(errorMessageBuilder.toString(), SourceLocation.none());
        }
    }

    /**
     * Gets this TraceFile's smithyTrace.
     * The smithyTrace {@link String} contains the Smithy trace file version number.
     *
     * @return a String representing trace file ID.
     */
    public String getSmithyTrace() {
        return smithyTrace;
    }

    /**
     * Gets this TraceFile's TraceMetadata.
     *
     * @return a TraceMetadata object.
     */
    public TraceMetadata getMetadata() {
        return metadata;
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
     * The shapes {@link Map} provides a mapping of absolute Smithy shape IDs to a list
     * of shape link objects. A single Smithy shape can be responsible for generating
     * multiple components in the target artifact.
     *
     * @return a Map from ShapeIDs to a list of ShapeLink's that represents the contents of the
     * shapes tag in the trace file.
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
                .metadata(metadata)
                .smithyTrace(smithyTrace)
                .definitions(artifactDefinitions)
                .shapes(shapes);
    }

    /**
     * Builder for constructing TraceFile's from scratch.
     */
    public static final class Builder implements SmithyBuilder<TraceFile> {

        private final Map<ShapeId, List<ShapeLink>> shapes = new HashMap<>();
        private String smithyTrace = SMITHY_TRACE_VERSION;
        private ArtifactDefinitions artifactDefinitions;
        private TraceMetadata metadata;

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
         * @param metadata Trace file TraceMetadata.
         * @return This builder.
         */
        public Builder metadata(TraceMetadata metadata) {
            this.metadata = metadata;
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
            List<ShapeLink> list = this.shapes.getOrDefault(id, new ArrayList<>());
            list.add(link);
            this.shapes.put(id, list);
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
        public Builder addShapeLinks(ShapeId id, List<ShapeLink> linkList) {
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
        public Builder addShapeLinks(String idString, List<ShapeLink> linkList) {
            return addShapeLinks(ShapeId.from(idString), linkList);
        }

        /**
         * @param shapes shapes map for TraceFile.
         * @return This builder.
         */
        public Builder shapes(Map<ShapeId, List<ShapeLink>> shapes) {
            this.shapes.clear();
            this.shapes.putAll(shapes);
            return this;
        }

    }

}
