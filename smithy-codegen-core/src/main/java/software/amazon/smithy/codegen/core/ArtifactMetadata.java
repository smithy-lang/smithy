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

import software.amazon.smithy.model.node.FromNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;

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
 */
public class ArtifactMetadata implements ToNode, FromNode, ValidateRequirements {
    public static final String ID_TEXT = "id";
    public static final String VERSION_TEXT = "version";
    public static final String TYPE_TEXT = "type";
    public static final String TYPE_VERSION_TEXT = "typeVersion";
    public static final String HOMEPAGE_TEXT = "homepage";
    public static final String TIMESTAMP_TEXT = "timestamp";
    private String id;
    private String version;
    private String timestamp;
    private String type;
    private String typeVersion; //optional
    private String homepage; //optional
    private NodeMapper nodeMapper = new NodeMapper();

    public ArtifactMetadata() {
    }

    private ArtifactMetadata(String id, String version, String timestamp, String type, String typeVersion,
                             String homepage) {
        this.id = id;
        this.version = version;
        this.timestamp = timestamp;
        this.type = type;
        this.typeVersion = typeVersion;
        this.homepage = homepage;
    }

    /**
     * Converts the metadata contained in ArtifactMetadata's variables into an ObjectNode.
     *
     * @return an ObjectNode with that contains StringNodes representing the trace file
     * metadata
     */
    @Override
    public ObjectNode toNode() {
        //error handling
        validateRequiredFields();

        Map<String, String> toSerialize = new HashMap<>();

        toSerialize.put(ID_TEXT, id);
        toSerialize.put(VERSION_TEXT, version);
        toSerialize.put(TYPE_TEXT, type);
        toSerialize.put(TIMESTAMP_TEXT, timestamp);
        if (typeVersion != null) {
            toSerialize.put(TYPE_VERSION_TEXT, typeVersion);
        }
        if (homepage != null) {
            toSerialize.put(HOMEPAGE_TEXT, homepage);
        }

        return nodeMapper.serialize(toSerialize).expectObjectNode();
    }

    /**
     * Instantiates ArtifactMetadata instance variables using an ObjectNode that contains the artifact section of the
     * trace file.
     *
     * @param jsonNode an ObjectNode that contains all children of the artifact tag in the trace file
     */
    @Override
    public void fromNode(Node jsonNode) {
        //cast to objectNode
        ObjectNode node = jsonNode.expectObjectNode();

        //error handling during deserialization
        nodeMapper.setWhenMissingSetter(NodeMapper.WhenMissing.FAIL);

        id = nodeMapper.deserialize(node.expectStringMember(ID_TEXT), String.class);
        version = nodeMapper.deserialize(node.expectStringMember(VERSION_TEXT), String.class);
        timestamp = nodeMapper.deserialize(node.expectStringMember(TIMESTAMP_TEXT), String.class);
        type = nodeMapper.deserialize(node.expectStringMember(TYPE_TEXT), String.class);

        if (node.containsMember(TYPE_VERSION_TEXT)) {
            typeVersion = nodeMapper.deserialize(node.expectStringMember(TYPE_VERSION_TEXT), String.class);
        }
        if (node.containsMember(HOMEPAGE_TEXT)) {
            homepage = nodeMapper.deserialize(node.expectStringMember(HOMEPAGE_TEXT), String.class);
        }

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
     * Gets this ArtifactMetadata's id.
     *
     * @return ArtifactMetadata's id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets this ArtifactMetadata's id.
     *
     * @param id ArtifactMetadata's id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets this ArtifactMetadata's version.
     *
     * @return ArtifactMetadata's version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets this ArtifactMetadata's version.
     *
     * @param version ArtifactMetadata's version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Gets this ArtifactMetadata's timestamp.
     *
     * @return ArtifactMetadata's timestamp
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Sets this ArtifactMetadata's timestamp.
     *
     * @param timestamp ArtifactMetadata's timestamp
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets this ArtifactMetadata's type.
     *
     * @return ArtifactMetadata's type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets this ArtifactMetadata's type.
     *
     * @param type the ArtifactMetadata's type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets this ArtifactMetadata's TypeVersion in an Optional container.
     *
     * @return Optional container with type version or empty container if
     * TypeVersion has not been set
     */
    public Optional<String> getTypeVersion() {
        return Optional.ofNullable(typeVersion);
    }

    /**
     * Sets this ArtifactMetadata's TypeVersion.
     *
     * @param typeVersion the ArtifactMetadata's TypeVersion
     */
    public void setTypeVersion(String typeVersion) {
        this.typeVersion = typeVersion;
    }

    /**
     * Gets this ArtifactMetadata's Homepage in an Optional container.
     *
     * @return Optional container with homepage or empty container if
     * homepage has not been set
     */
    public Optional<String> getHomepage() {
        return Optional.ofNullable(homepage);
    }

    /**
     * Sets this ArtifactMetadata's Homepage.
     *
     * @param homepage ArtifactMetadata's Homepage
     */
    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public static class ArtifactMetadataBuilder {

        private String id;
        private String version;
        private String timestamp;
        private String type;
        private String typeVersion;
        private String homepage;

        /**
         * Constructor for builder with all required fields.
         *
         * @param id        ArtifactMetadata's id.
         * @param version   ArtifactMetadata's version.
         * @param timestamp ArtifactMetadata's timestamp.
         * @param type      ArtifactMetadata's type.
         */
        public ArtifactMetadataBuilder(String id, String version, String timestamp, String type) {
            this.id = id;
            this.version = version;
            this.timestamp = timestamp;
            this.type = type;
        }

        /**
         * Constructor for builder with all required fields except timestamp which can
         * later be set with the setTimestampAsNow method.
         *
         * @param id      ArtifactMetadata's id.
         * @param version ArtifactMetadata's version..
         * @param type    ArtifactMetadata's type.
         */
        public ArtifactMetadataBuilder(String id, String version, String type) {
            this.id = id;
            this.version = version;
            this.type = type;
        }

        /**
         * Sets the timestamp as the current time in RFC 3339 format.
         *
         * @return This builder.
         */
        public ArtifactMetadataBuilder setTimestampAsNow() {
            //set timestamp based on current time
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            this.timestamp = dateFormat.format(new Date());
            return this;
        }

        /**
         * Sets this builder's typeVersion.
         *
         * @param typeVersion typeVersion of ArtifactMetadata.
         * @return This builder.
         */
        public ArtifactMetadataBuilder setTypeVersion(String typeVersion) {
            this.typeVersion = typeVersion;
            return this;
        }

        /**
         * Sets this builder's homepage.
         *
         * @param homepage homepage of ArtifactMetadata.
         * @return This builder.
         */
        public ArtifactMetadataBuilder setHomepage(String homepage) {
            this.homepage = homepage;
            return this;
        }

        /**
         * @return The ArtifactMetadata object corresponding to this builder.
         */
        public ArtifactMetadata build() {
            return new ArtifactMetadata(id, version, timestamp, type, typeVersion, homepage);
        }
    }
}
