package software.amazon.smithy.codegen.core;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
public class ArtifactMetadata implements ToNode, FromNode, ValidateRequirements{
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

    private NodeMapper nodeMapper = new NodeMapper();

    /**
     * Converts the metadata contained in ArtifactMetadata's instance variables
     * into an ObjectNode
     *
     * @return an ObjectNode with that contains StringNodes representing the trace file
     * metadata
     */
    @Override
    public ObjectNode toNode(){
        Map<StringNode, Node> nodes= new HashMap<>();

        //error handling
        validateRequiredFields();

        Map<String, String> toSerialize = new HashMap<>();

        toSerialize.put(idText,id);
        toSerialize.put(versionText, version);
        toSerialize.put(typeText,type);
        toSerialize.put(timestampText, timestamp);
        if(typeVersion!=null) toSerialize.put(typeVersionText, typeVersion);
        if(homepage!=null) toSerialize.put(homepageText, homepage);

        return nodeMapper.serialize(toSerialize).expectObjectNode();
    }

    /**
     * Instantiates ArtifactMetadata instance variables using an ObjectNode that contains the
     * artifact section of the trace file
     *
     * @param jsonNode an ObjectNode that contains all children of the artifact tag in the trace file
     */
    @Override
    public void fromNode(Node jsonNode){
        //cast to objectNode
        ObjectNode node = jsonNode.expectObjectNode();

        //error handling during deserialization
        nodeMapper.setWhenMissingSetter(NodeMapper.WhenMissing.FAIL);

        id = nodeMapper.deserialize(node.expectStringMember(idText), String.class);
        version = nodeMapper.deserialize(node.expectStringMember(versionText), String.class);
        timestamp = nodeMapper.deserialize(node.expectStringMember(timestampText), String.class);
        type = nodeMapper.deserialize(node.expectStringMember(typeText), String.class);

        if(node.containsMember(typeVersionText))
            typeVersion = nodeMapper.deserialize(node.expectStringMember(typeVersionText), String.class);
        if(node.containsMember(homepageText))
            homepage = nodeMapper.deserialize(node.expectStringMember(homepageText), String.class);

        //error handling
        validateRequiredFields();
    }


    /**
     * Checks if all of the ArtifactMetadata's required fields are not null.
     *
     * @throws NullPointerException if any of the required fields are null
     */
    @Override
    public void validateRequiredFields() {
        Objects.requireNonNull(id);
        Objects.requireNonNull(version);
        Objects.requireNonNull(type);
        Objects.requireNonNull(timestamp);
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
