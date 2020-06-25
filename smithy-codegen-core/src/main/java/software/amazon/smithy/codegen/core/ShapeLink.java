package software.amazon.smithy.codegen.core;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.*;

import java.util.*;

/**
 * Class that defines a link between the Smithy {@link software.amazon.smithy.model.shapes.Shape} and
 * the artifact that it produced.
 * <p>
 * ShapeLink objects contain the following information:
 * <ul>
 *  <li> type - The type of artifact component. This value MUST correspond to one of the types defined
 *       in the /definitions/types property of the trace file.</li>
 *  <li> id	 - The artifact-specific identifier for the artifact component. For example, in Java a valid id
 *       would be the fully-qualified name of a class, method, or field as defined in
 *       <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/doc-comment-spec.html"> Documentation Comment
 *       Specification for the Standard Doclet</a></li>
 *  <li> tags - Defines a list of tags to apply to the trace link. Each tag MUST correspond to a tag defined in
 *       the /definitions/tags property of the trace file.</li>
 *  <li> file - A URI that defines the location of the artifact component. Files MUST use the "file" URI scheme, and SHOULD be relative.</li>
 *  <li> line -	The line number in the file that contains the artifact component.</li>
 *  <li> column	- The column number in the file that contains the artifact component.</li>
 * </ul>
 *
 */
public class ShapeLink {
    private String type;
    private String id;
    private List<String> tags; //optional
    private String file; //optional
    private Integer line; //optional
    private Integer column; //optional

    public final String typeText = "type";
    public final String idText = "id";
    public final String tagsText = "tags";
    public final String fileText = "file";
    public final String lineText = "line";
    public final String columnText = "column";

    private final SourceLocation sl = new SourceLocation("");

    /**
     * Default constructor for ShapeLink
     */
    public ShapeLink(){
        tags = new ArrayList<>();
    }

    /**
     * Instantiates ShapeLink instance variables by extracting data from an ObjectNode
     *
     * @param node an ObjectNode that represents the a single ShapeLink
     */
    public void fromJsonNode(ObjectNode node) {
        type = fromJsonNodeStringHelper(typeText, node);
        id = fromJsonNodeStringHelper(idText, node);
        if(node.containsMember(tagsText)) setTagsFromJson(node);
        if(node.containsMember(fileText)) file = fromJsonNodeStringHelper(fileText, node);
        if(node.containsMember(lineText)) line = (Integer) fromJsonNodeNumberHelper(lineText, node);
        if(node.containsMember(columnText)) column = (Integer) fromJsonNodeNumberHelper(columnText, node);
    }

    /**
     * Helper method for fromJsonNode that converts from a StringNode to a String
     * @param text text corresponding to ShapeLink parameter to extract
     * @param node ObjectNode for ShapeLink section of trace file
     * @return a string representing the value of the ShapeLink data extracted from node
     * @throws TraceFileParsingException if any required tags are missing or incorrectly formatted
     */
    private String fromJsonNodeStringHelper(String text, ObjectNode node){
        return node.getStringMember(text)
                .orElseThrow(()-> new TraceFileParsingException(this.getClass().getSimpleName(), text))
                .getValue();
    }

    /**
     * Helper method for fromJsonNode that converts from a NumberNode to a String
     * @param text text corresponding to ShapeLink parameter to extract
     * @param node ObjectNode for ShapeLink section of trace file
     * @return a Number representing the value of the ShapeLink data extracted from node
     * @throws TraceFileParsingException if any required tags are missing or incorrectly formatted
     */
    private Number fromJsonNodeNumberHelper(String text, ObjectNode node){
        return node.getNumberMember(text)
                .orElseThrow(()-> new TraceFileParsingException(this.getClass().getSimpleName(), text))
                .getValue();
    }

    /**
     * Helper method for fromJsonNode that converts an ArrayNode into the list of
     * tags
     *
     * @param node and ObjectNode that represents the information for a single
     *             ShapeLink object
     */
    private void setTagsFromJson(ObjectNode node) {
        ArrayNode arrayNode = node.getArrayMember(tagsText)
                .orElseThrow(()-> new TraceFileParsingException(this.getClass().getSimpleName(), tagsText));
        for (Node value : arrayNode) {
            StringNode next = value.expectStringNode("shapes->ShapeIds->tags->list of tags level of JSON in " +
                    "trace file is formatted incorrectly, tags must be Strings - see example for correct formatting");
            tags.add(next.getValue());
        }
    }
    /**
     * Converts instance variables into an ObjectNode for writing out a ShapeLink
     *
     * @return returns an ObjectNode that contains the StringNodes with the information
     * from a ShapeLink
     * @throws TraceFileWritingException if id or type is not defined when the method is called
     */
    public ObjectNode toJsonNode(){
        Map<StringNode, Node> nodes= new HashMap<>();

        if(id==null) throw new TraceFileWritingException(this.getClass().getSimpleName(), idText);
        if(type==null) throw new TraceFileWritingException(this.getClass().getSimpleName(), typeText);

        nodes.put(new StringNode(idText, sl), new StringNode(id, sl));
        nodes.put(new StringNode(typeText, sl),new StringNode(type, sl));

        if(tags!=null && !tags.isEmpty()) nodes.put(new StringNode(tagsText, sl), getJsonFromTags());
        if(file!=null) nodes.put(new StringNode(fileText, sl), new StringNode(file, sl));
        if(line!=null) nodes.put(new StringNode(lineText, sl), new NumberNode(line, sl));
        if(column!=null) nodes.put(new StringNode(columnText, sl), new NumberNode(column, sl));

        return new ObjectNode(nodes, sl);
    }

    /**
     * Helper method for toJsonNode that converts the list of tags into an ArrayNode
     *
     * @return an ArrayNode containing the list of tags
     */
    private ArrayNode getJsonFromTags() {
        List<Node> elements = new ArrayList<>();

        for(String tag: tags){
            elements.add(new StringNode(tag, sl));
        }

        return new ArrayNode(elements, sl);
    }

    /**
     * Gets this ShapeLink's type
     * @return this ShapeLink's type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets this ShapeLink's type
     * @param type represents type of this ShapeLink
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets this ShapeLink's id
     * @return this ShapeLink's id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets this ShapeLink's Id
     * @param id represents id of this ShapeLink
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets this ShapeLink's tags in an optional container
     * @return Optional container holding this ShapeLink's list of tags
     */
    public Optional<List<String>> getTags() {
        return Optional.ofNullable(tags);
    }

    /**
     * Sets this ShapeLink's tags list
     * @param tags a list of tags to assign to this ShapeLink
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * Gets this ShapeLink's file in an optional container
     * @return Optional container holding this ShapeLink's file
     */
    public Optional<String> getFile() {
        return Optional.ofNullable(file);
    }

    /**
     * Sets this ShapeLink's file
     * @param file represents file where this ShapeLink was generated from
     */
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * Gets this ShapeLink's line number in an optional container
     * @return Optional container holding this ShapeLink's line number
     */
    public Optional<Integer> getLine() {
        return Optional.ofNullable(line);
    }

    /**
     * Sets this ShapeLink's line number
     * @param line represents link number of this ShapeLink
     */
    public void setLine(Integer line) {
        this.line = line;
    }

    /**
     * Gets this ShapeLink's column number in an optional container
     * @return Optional container holding this ShapeLink's column number
     */
    public Optional<Integer> getColumn() {
        return Optional.ofNullable(column);
    }

    /**
     * Sets this ShapeLink's column number
     * @param column represents column number of this ShapeLink
     */
    public void setColumn(Integer column) {
        this.column = column;
    }
}
