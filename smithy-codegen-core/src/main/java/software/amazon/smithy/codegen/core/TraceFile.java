package software.amazon.smithy.codegen.core;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;

import java.io.*;
import java.net.URI;
import java.util.*;

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
 *
 */
public class TraceFile {
    private String smithyTrace;
    private ArtifactMetadata artifactMetadata;
    private Definitions definitions; //Optional
    private Map<ShapeId, List<ShapeLink>> shapes;

    public final String smithyTraceText = "smithyTrace";
    public final String artifactText = "artifact";
    public final String definitionsText = "definitions";
    public final String shapesText = "shapes";

    private final SourceLocation sl = new SourceLocation("");


    /**
     * Parses and validates the trace file passed in as filename
     * and instantiates smithyTrace and defintions, and fills
     * artifactMetadata and shapes.
     *
     * @param filename  the absolute or relative path of tracefile
     * @throws FileNotFoundException if filename is not found for reading
     * @throws software.amazon.smithy.model.node.ExpectationNotMetException
     * if smithyTrace, artifactMetadata, ShapeLink are not found or not structure correctly
     */
    public void parseTraceFile(URI filename) throws FileNotFoundException {
        InputStream stream = new FileInputStream(new File(filename));
        ObjectNode node = (ObjectNode) Node.parse(stream);

        //instantiate on parsing
        artifactMetadata = new ArtifactMetadata();
        shapes = new HashMap<>();

        //throws error if there is no smithyTraceText
        smithyTrace = node.expectStringMember(smithyTraceText).getValue();
        //throws error if there's no artifactText
        artifactMetadata.fromJsonNode(node.expectObjectMember(artifactText));
        //throws error if no shape or shape incorrectly formatted

        parseShapeObject(node);
        parseDefinitionsObject(node);
    }

    /**
     * Helper method for parseTraceFile.
     * Parses the Definition object from the trace file node if the
     * trace file contains a definitions object.
     *
     * @param node  an ObjectNode that contains the entire trace file
     * @see ObjectNode
     * @see Definitions
     */
    private void parseDefinitionsObject(ObjectNode node) {
        if(node.containsMember(definitionsText)){
            definitions = new Definitions();
            definitions.fromJsonNode(node.expectObjectMember(definitionsText));
        }
    }

    /**
     * Helper method for parseTraceFile.
     * Parses the shapes map from the trace file node. Iterates over all shapeIds in the node.
     * For each shapeId, iterates over all ShapeLinks and constructs a list of ShapeLink objects,
     * then puts adds the shapeId and its corresponding list to the shapes map.
     *
     * @param node an ObjectNode that contains the entire trace file
     * @see ObjectNode
     * @see ShapeId
     * @see ShapeLink
     */
    private void parseShapeObject(ObjectNode node) {
        //throws error if doesn't find shapeText or if shape map is empty
        Map<StringNode, Node> shapeMap = node.expectObjectMember(shapesText).getMembers();
        if(shapeMap.isEmpty()) throw new TraceFileParsingException(this.getClass().getSimpleName(), shapesText);

        for(StringNode key: shapeMap.keySet()){
            ShapeId shapeId = ShapeId.from(key.getValue());
            List<ShapeLink> linkList = new ArrayList<>();

            ArrayNode arrayNode = shapeMap.get(key).expectArrayNode("ShapeID -> List<ShapeLink> level of JSON is " +
                    "incorrectly formatted in trace file - see example");

            for (Node value : arrayNode) {
                ShapeLink shapeLink = new ShapeLink();
                ObjectNode next = value.expectObjectNode("List<ShapeLink> level of JSON is incorrectly formatted" +
                        "in trace file - see example");
                shapeLink.fromJsonNode(next);
                linkList.add(shapeLink);
            }

            shapes.put(shapeId, linkList);
        }
    }

    /**
     * Writes the TraceFile instance variables to a single JSON ObjectNode
     * to construct a trace file.
     *
     * @param fileName absolute or relative path to write the trace file
     * @throws IOException if there is an error writing to fileName
     */
    public void writeTraceFile(String fileName) throws IOException {
        Map<StringNode, Node> nodesMap = new HashMap<>();

        //error checking
        if(smithyTrace == null) throw new TraceFileWritingException(this.getClass().getSimpleName(), smithyTraceText);
        if(artifactMetadata == null) throw new TraceFileWritingException(this.getClass().getSimpleName(), artifactText);
        if(shapes == null) throw new TraceFileWritingException(this.getClass().getSimpleName(), shapesText);

        nodesMap.put(new StringNode(smithyTraceText, sl), new StringNode(smithyTrace, sl));
        nodesMap.put(new StringNode(artifactText, sl), artifactMetadata.toJsonNode());
        if(definitions!=null)  nodesMap.put(new StringNode(definitionsText, sl), definitions.toJsonNode());
        shapeObjectToJson(nodesMap);

        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));

        writer.write(Node.prettyPrintJson(new ObjectNode(nodesMap, sl), "  "));
        writer.close();
    }

    /**
     * Helper method for writeTraceFile that converts the shapes Map to an ObjectNode
     * by iterating over the contents of shapes and calling ShapeLink's toJsonNode method.
     *
     * @param nodesMap a map of the top level of the trace file
     */
    private void shapeObjectToJson(Map<StringNode, Node> nodesMap) {
        Map<StringNode, Node> shapeMap = new HashMap<>();
        if(shapes.keySet().isEmpty()) throw new TraceFileWritingException(this.getClass().getSimpleName(), shapesText);

        for(ShapeId key: shapes.keySet()){
            List<Node> shapeLinkList = new ArrayList<>();
            if(shapes.get(key).isEmpty()) throw new TraceFileWritingException(this.getClass().getSimpleName(), key.toString());
            for(ShapeLink link: shapes.get(key)){
                shapeLinkList.add(link.toJsonNode());
            }
            shapeMap.put(new StringNode(key.toString(), sl), new ArrayNode(shapeLinkList, sl));
        }
        nodesMap.put(new StringNode(shapesText, sl), new ObjectNode(shapeMap, sl));
    }

    /**
     * Gets this TraceFile's smithyTrace.
     *
     * @return a String representing trace file ID, or null if ID has not been set
     */
    public String getSmithyTrace(){
        return smithyTrace;
    }

    /**
     * Gets this TraceFile's ArtifactMetadata.
     *
     * @return an ArtifactMetadata object, or null if ArtifactMetadata has not been set.
     */
    public ArtifactMetadata getArtifactMetadata(){
        return artifactMetadata;
    }

    /**
     * Gets this TraceFile's Definitions
     *
     * @return an Optional Definitions container that contains this TraceFile's Definition
     * or isEmpty if Definition's has not been set.
     */
    public Optional<Definitions> getDefinitions(){
        return Optional.ofNullable(definitions);
    }

    /**
     * Gets this TraceFile's Shapes map.
     *
     * @return a Map from ShapeIDs to a list of ShapeLink's that represents the contents of the
     * shapes tag in the trace file, or null if shapes has not been set.
     */
    public Map<ShapeId, List<ShapeLink>> getShapes(){
        return shapes;
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
     * Sets this TraceFile's Artifact Metadata.
     *
     * @param artifactMetadata ArtifactMetadata object for TraceFile.
     */
    public void setArtifactMetadata(ArtifactMetadata artifactMetadata) {
        this.artifactMetadata = artifactMetadata;
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
     * Sets this TraceFile's shapes map.
     *
     * @param shapes a map from ShapeIds to a list of corresponding ShapeLink
     *               objects
     */
    public void setShapes(Map<ShapeId, List<ShapeLink>> shapes) {
        this.shapes = shapes;
    }
}
