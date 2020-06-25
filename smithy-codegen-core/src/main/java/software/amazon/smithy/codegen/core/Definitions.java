package software.amazon.smithy.codegen.core;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that defines the acceptable values that can be used in ShapeLink objects.
 * <p>
 *     The types {@link Map} defines the set of types that can be used in a {@link ShapeLink} object.
 *     Each key is the name of the type, and each value is a description of the type.
 *     For programming languages, these types represent language-specific components.
 *     For example, in Java, types should map to the possible values of {@link java.lang.annotation.ElementType}.
 * </p>
 * <p>
 *     The tags {@link Map} defines the set of tags that can be used in a {@link ShapeLink} object.
 *     Each key is the name of the tag, and each value is the description of the tag.
 *     Tags are used to provide semantics to links. Tools that evaluate trace models
 *     use these tags to inform their analysis. For example, a tag for an AWS SDK code
 *     generator could be "requestBuilder" to indicate that a class is used as a builder for a request.
 * </p>
 */
public class Definitions {
    private Map<String, String> tags;
    private Map<String, String> types;

    public final String typeText = "types";
    public final String tagsText = "tags";

    private final SourceLocation sl = new SourceLocation("");

    /**
     * Default constructor for definitions, instantiates empty HashMaps for tags and types
     */
    public Definitions(){
        tags = new HashMap<>();
        types = new HashMap<>();
    }

    /**
     * Gets this Definition's Tags Map
     * @return this Definition's Tags Map
     */
    public Map<String, String> getTags() {
        return tags;
    }

    /**
     * Sets this Definition's Tags Map
     * @param tags tags map for definitions
     */
    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    /**
     * Gets this Definition's Types Map
     * @return this Definition's Type's Map
     */
    public Map<String, String> getTypes() {
        return types;
    }

    /**
     * Sets this Definition's Types Map
     * @param types types Map for Definitions
     */
    public void setTypes(Map<String, String> types) {
        this.types = types;
    }

    /**
     * Converts an ObjectNode that represents the definitions section of the
     * trace file into the types maps and tags map instance variable
     *
     * @param node ObjectNode that contains the JSON data inside the definitions tag of
     *             the trace file
     * @throws TraceFileParsingException if definition's type or tag nodes are missing or incorrectly formatted
     * @throws software.amazon.smithy.model.node.ExpectationNotMetException if the values associated with definition's
     * type or tag nodes are missing or incorrectly formatted
     */
    public void fromJsonNode(ObjectNode node){
        Map<StringNode, Node> typesNode = node.getObjectMember(typeText)
                .orElseThrow(()-> new TraceFileParsingException(this.getClass().getSimpleName(), typeText))
                .getMembers();

        //throw an error if tags node is empty or if type nodes is empty
        if(typesNode.isEmpty()) throw new TraceFileParsingException(this.getClass().getSimpleName(), typeText);

        for(StringNode keyNode: typesNode.keySet()){
            StringNode valueNode = typesNode.get(keyNode).expectStringNode("Types -> List of Types - level of Json" +
                    "in trace file is incorrect, types must be Strings -- see example for correct formatting");
            types.put(keyNode.getValue(), valueNode.getValue());
        }

        Map<StringNode, Node> tagsNode = node.getObjectMember(tagsText)
                .orElseThrow(()-> new TraceFileParsingException(this.getClass().getSimpleName(), typeText))
                .getMembers();

        //throw an error if tags node is empty or if type nodes is empty
        if(tagsNode.isEmpty()) throw new TraceFileParsingException(this.getClass().getSimpleName(), tagsText);

        for(StringNode keyNode: tagsNode.keySet()){
            StringNode valueNode = tagsNode.get(keyNode).expectStringNode("Tags -> List of Tags - level of Json" +
                    "in trace file is incorrect, Tags must be Strings -- see example for correct formatting");
            tags.put(keyNode.getValue(), valueNode.getValue());
        }
    }

    /**
     * Parses a definitions file and converts it into a definitions object. This is useful
     * in the scenario when the user is provided the definitions specification and must create
     * the trace file from that definitions specification.
     * @param filename a definitions file URI - see example
     * @throws FileNotFoundException if the definitions file path is not found
     * @return ObjectNode corresponding to parsed URI
     */
    public ObjectNode fromDefinitionsFile(URI filename) throws FileNotFoundException {
        InputStream stream = new FileInputStream(new File(filename));
        return Node.parse(stream)
                .asObjectNode()
                .orElseThrow(()-> new TraceFileParsingException(this.getClass().getSimpleName(), typeText + " or " + tagsText));
    }

    /**
     * Converts the types and tags Maps into a single ObjectNode.
     *
     * @return an ObjectNode that contains two ObjectNode children; one contains the tag map structure the
     * other contains the type map structure.
     * @throws TraceFileWritingException if types or tags is not defined when the method is called
     */
    public ObjectNode toJsonNode(){
        //error handling
        if(types==null || types.isEmpty()) throw new TraceFileWritingException(this.getClass().getSimpleName(), typeText);
        if(tags==null || tags.isEmpty()) throw new TraceFileWritingException(this.getClass().getSimpleName(), tagsText);

        ObjectNode typesObjectNode = toJsonNodeHelper(types);
        ObjectNode tagsObjectNode = toJsonNodeHelper(tags);

        Map<StringNode, Node> typesTagsMap = new HashMap<>();
        typesTagsMap.put(new StringNode(typeText, sl), typesObjectNode);
        typesTagsMap.put(new StringNode(tagsText, sl), tagsObjectNode);

        return new ObjectNode(typesTagsMap, sl);
    }

    /**
     * Helper method for toJsonNode that converts either the tags or types Maps into a single
     * ObjectNode.
     *
     * @param myMap either the tags or types Map to be converted.
     * @return an ObjectNode that contains the ObjectNode representation of the inputted map.
     */
    private ObjectNode toJsonNodeHelper(Map<String, String> myMap) {
        Map<StringNode, Node> nodesObject= new HashMap<>();
        for(String mapKey: myMap.keySet()) {
            nodesObject.put(new StringNode(mapKey, sl), new StringNode(myMap.get(mapKey), sl));
        }
        return new ObjectNode(nodesObject, sl);
    }
}
