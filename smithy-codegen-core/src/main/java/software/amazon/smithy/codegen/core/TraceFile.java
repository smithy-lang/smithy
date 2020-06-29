package software.amazon.smithy.codegen.core;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.*;
import software.amazon.smithy.model.shapes.Shape;
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
public class TraceFile implements ToNode, FromNode, ValidateRequirements {
    private String smithyTrace;
    private ArtifactMetadata artifactMetadata;
    private Definitions definitions; //Optional
    private Map<ShapeId, List<ShapeLink>> shapes;

    public final String smithyTraceText = "smithyTrace";
    public final String artifactText = "artifact";
    public final String definitionsText = "definitions";
    public final String shapesText = "shapes";

    private NodeMapper nodeMapper = new NodeMapper();
    private SourceLocation sl = new SourceLocation("");


    /**
     * Parses and validates the trace file passed in as filename
     * and instantiates smithyTrace and defintions, and fills
     * artifactMetadata and shapes.
     *
     * @param filename  the absolute or relative path of tracefile
     * @throws FileNotFoundException if filename is not found for reading
     * if smithyTrace, artifactMetadata, ShapeLink are not found or not structure correctly
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
    public void fromNode(Node jsonNode){
        //throw error if trace file top level is incorrectly formatted
        ObjectNode node = jsonNode.expectObjectNode();

        nodeMapper.setWhenMissingSetter(NodeMapper.WhenMissing.FAIL);

        //parse trace
        smithyTrace = nodeMapper.deserialize(node.expectStringMember(smithyTraceText),String.class);

        //parse metadata
        artifactMetadata = nodeMapper.deserialize(node.expectObjectMember(artifactText), ArtifactMetadata.class);

        //parse shapes
        shapes = new HashMap<>();
        Map<StringNode, Node> shapeMap = node.expectObjectMember(shapesText).getMembers();
        for(StringNode key: shapeMap.keySet()){
            ShapeId shapeId = ShapeId.from(key.getValue());
            List<ShapeLink> list = nodeMapper.deserializeCollection(shapeMap.get(key), ArrayList.class, ShapeLink.class);
            shapes.put(shapeId, list);
        }

        //parse definitions
        if(node.containsMember(definitionsText)) {
            definitions = nodeMapper.deserialize(node.expectObjectMember(definitionsText), Definitions.class);
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
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(Node.prettyPrintJson(toNode(), "  "));
        writer.close();
    }

    /**
     * Converts TraceFile instance variables into an
     * ObjectNode.
     *
     * @return ObjectNode representation of a TraceFile.
     */
    @Override
    public ObjectNode toNode(){
        //error checking
        validateRequiredFields();

        Map<String, Object> toSerialize = new HashMap<>();
        toSerialize.put(smithyTraceText,smithyTrace);
        toSerialize.put(artifactText, artifactMetadata);
        if(definitions!=null)  toSerialize.put(definitionsText, definitions);
        toSerialize.put(shapesText,shapes);

        return nodeMapper.serialize(toSerialize).expectObjectNode();
    }

    /**
     * Finds invalid types and tags and either removes them or throws an error depending on
     * whether toThrow is true or false.
     *
     * @throws ExpectationNotMetException if a type or tag in shapes is not in definitions.
     */
    public void validateTypesAndTags(){
        Objects.requireNonNull(shapes);
        Objects.requireNonNull(definitions);

        for(ShapeId list: shapes.keySet()){
            Iterator<ShapeLink> i = shapes.get(list).iterator();
            while(i.hasNext()){
                ShapeLink link = i.next();
                if(!definitions.getTypes().containsKey(link.getType())){
                    throw new ExpectationNotMetException(list.toString() + " contains types that aren't in definitions.", sl);
                }
                else{
                    Optional<List<String>> tags = link.getTags();
                    if(tags.isPresent()){
                        Iterator<String> iter = tags.get().iterator();
                        while(iter.hasNext()){
                            String next = iter.next();
                            if(!definitions.getTags().containsKey(next)){
                                throw new ExpectationNotMetException(list.toString() + " " + next + " is a tag that isn't in definitions.", sl);
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
     * not in model.
     */
    public void validateModel(String modelResourceName){
        Model model = Model.assembler()
                .addImport(getClass().getResource(modelResourceName))
                .assemble()
                .unwrap();

        //error check - shapes must be non-null to use this method
        Objects.requireNonNull(shapes);

        //model contains all the shapeIds in shapes.keySet()
        for(ShapeId id: shapes.keySet()){
            model.expectShape(id);
        }

        //shapes.keySet() contains all the shapeIds in model
        for(Shape shape: model.toSet()){
            ShapeId id = shape.getId();
            if(!shapes.containsKey(id))
                throw new ExpectationNotMetException("shapes does not contain" + id.toString() + " but model does", sl);
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
        for(ShapeId shapeId: shapes.keySet()){
            for(ShapeLink link: shapes.get(shapeId)){
              link.validateRequiredFields();
            }
        }
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
