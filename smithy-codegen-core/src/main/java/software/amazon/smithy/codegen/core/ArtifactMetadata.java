package software.amazon.smithy.codegen.core;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Class that defines information about an artifact that was created from a model for the trace file.
 *
 * <p>ArtifactMetadata contains metadata that
 * can be instantiated from a {@link software.amazon.smithy.model.node.Node} or written
 * to a {@link software.amazon.smithy.model.node.Node}. ArtifactMetadata contains the
 * following information:
 * <ul>
 *     <li>id - The identifier of the artifact. For example, Java packages should use the Maven artifact ID. </li>
 *     <li>version - The version of the artifact (for example, the AWS SDK release number).</li>
 *     <li>timestamp - The RFC 3339 date and time that the artifact was created.</li>
 *     <li>type - The type of artifact. For code generation, this is the programming language. </li>
 *     <li>typeVersion (Optional) - The artifact type version, if relevant. For example, when defining
 *     trace files for Java source code, the typeVersion would be the minimum supported JDK version.
 *     Different artifacts may have different output based on the version targets (for example the ability
 *     to use more features in a newer version of a language). </li>
 *     <li>homepage (Optional) - The homepage URL of the artifact.</li>
 * </ul>
 *
 */
public class ArtifactMetadata {
    private String id;
    private String version;
    private String timestamp;
    private String type;
    private String typeVersion; //optional
    private String homepage; //optional

    public final String idText = "id";
    public final String versionText = "version";
    public final String typeText = "type";
    public final String typeVersionText = "typeVersion";
    public final String homepageText = "homepage";
    public final String timestampText = "timestamp";

    private final SourceLocation sl = new SourceLocation("");

    /**
     * Converts the metadata contained in ArtifactMetadata's instance variables
     * into an ObjectNode
     *
     * @return an ObjectNode with that contains StringNodes representing the trace file
     * metadata
     * @throws TraceFileWritingException if id, version, type or timestamp is not defined when the method is called
     */
    public ObjectNode toJsonNode(){
        Map<StringNode, Node> nodes= new HashMap<>();

        //error handling
        if(id==null) throw new TraceFileWritingException(this.getClass().getSimpleName(), idText);
        if(version==null) throw new TraceFileWritingException(this.getClass().getSimpleName(), versionText);
        if(type==null) throw new TraceFileWritingException(this.getClass().getSimpleName(), typeText);
        if(timestamp==null) throw new TraceFileWritingException(this.getClass().getSimpleName(), timestampText);

        //using empty string for source filename
        nodes.put(new StringNode(idText, sl), new StringNode(id, sl));
        nodes.put(new StringNode(versionText, sl), new StringNode(version, sl));
        nodes.put(new StringNode(typeText, sl),new StringNode(type, sl));
        if(typeVersion!=null) nodes.put(new StringNode(typeVersionText, sl), new StringNode(typeVersion, sl));
        if(homepage!=null) nodes.put(new StringNode(homepageText, sl), new StringNode(homepage, sl));
        nodes.put(new StringNode(timestampText, sl),new StringNode(timestamp, sl));

        //create and return ObjectNode that contains the StringNodes
        return new ObjectNode(nodes, sl);
    }

    /**
     * Instantiates ArtifactMetadata instance variables using an ObjectNode that contains the
     * artifact section of the trace file
     *
     * @param jsonNode an ObjectNode that contains all children of the artifact tag in the trace file
     */
    public void fromJsonNode(ObjectNode jsonNode){
        id = fromJsonNodeHelper(idText, jsonNode);
        version = fromJsonNodeHelper(versionText, jsonNode);
        timestamp = fromJsonNodeHelper(timestampText, jsonNode);
        type = fromJsonNodeHelper(typeText, jsonNode);
        if(jsonNode.containsMember(typeVersionText))
            typeVersion = fromJsonNodeHelper(typeVersionText, jsonNode);
        if(jsonNode.containsMember(homepageText))
            homepage = fromJsonNodeHelper(homepageText, jsonNode);
    }

    /**
     * Helper method for fromJsonNode that converts from a StringNode to a String
     * @param text text corresponding to metadata parameter to extract
     * @param jsonNode ObjectNode for artifact section of trace file
     * @return a string representing the value of the metadata extracted from jsonNode
     * @throws TraceFileParsingException if any required metadata tags are missing or incorrectly formatted
     */
    private String fromJsonNodeHelper(String text, ObjectNode jsonNode){
        return jsonNode.getStringMember(text)
                .orElseThrow(()-> new TraceFileParsingException(this.getClass().getSimpleName(), text))
                .getValue();
    }


    /**
     * Gets this ArtifactMetadata's id
     * @return ArtifactMetadata's id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets this ArtifactMetadata's id
     * @param id ArtifactMetadata's id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets this ArtifactMetadata's version
     * @return ArtifactMetadata's version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets this ArtifactMetadata's version
     * @param version ArtifactMetadata's version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Gets this ArtifactMetadata's timestamp
     * @return ArtifactMetadata's timestamp
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Sets this ArtifactMetadata's timestamp
     * @param timestamp  ArtifactMetadata's timestamp
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets this ArtifactMetadata's type
     * @return ArtifactMetadata's type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets this ArtifactMetadata's type
     * @param type the ArtifactMetadata's type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets this ArtifactMetadata's TypeVersion in an Optional container
     * @return Optional container with type version or empty container if
     * TypeVersion has not been set
     */
    public Optional<String> getTypeVersion() {
        return Optional.ofNullable(typeVersion);
    }

    /**
     * Sets this ArtifactMetadata's TypeVersion
     * @param typeVersion the ArtifactMetadata's TypeVersion
     */
    public void setTypeVersion(String typeVersion) {
        this.typeVersion = typeVersion;
    }

    /**
     * Gets this ArtifactMetadata's Homepage in an Optional container
     * @return Optional container with homepage or empty container if
     * homepage has not been set
     */
    public Optional<String> getHomepage() {
        return Optional.ofNullable(homepage);
    }

    /**
     * Sets this ArtifactMetadata's Homepage
     * @param homepage ArtifactMetadata's Homepage
     */
    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }
}
