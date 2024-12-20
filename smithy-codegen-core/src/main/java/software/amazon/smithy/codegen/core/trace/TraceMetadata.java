/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.trace;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Class that defines information a code-generated artifact.
 */
public final class TraceMetadata implements ToNode, ToSmithyBuilder<TraceMetadata> {
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

    private TraceMetadata(Builder builder) {
        id = SmithyBuilder.requiredState(ID_TEXT, builder.id);
        version = SmithyBuilder.requiredState(VERSION_TEXT, builder.version);
        timestamp = SmithyBuilder.requiredState(TIMESTAMP_TEXT, builder.timestamp);
        type = SmithyBuilder.requiredState(TYPE_TEXT, builder.type);
        typeVersion = builder.typeVersion;
        homepage = builder.homepage;
    }

    /**
     * Instantiates TraceMetadata instance variables using an ObjectNode that contains the artifact section of the
     * trace file.
     *
     * @param value an ObjectNode that contains all children of the artifact tag in the trace file
     * @return TraceMetadata produced by deserializing the node.
     */
    public static TraceMetadata fromNode(Node value) {
        NodeMapper mapper = new NodeMapper();
        mapper.disableFromNodeForClass(TraceMetadata.class);
        return mapper.deserialize(value, TraceMetadata.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Converts the metadata contained in TraceMetadata's variables into an ObjectNode.
     *
     * @return an ObjectNode with that contains StringNodes representing the trace file
     * metadata
     */
    @Override
    public ObjectNode toNode() {
        return ObjectNode.objectNodeBuilder()
                .withMember(ID_TEXT, id)
                .withMember(VERSION_TEXT, version)
                .withMember(TYPE_TEXT, type)
                .withMember(TIMESTAMP_TEXT, timestamp)
                .withOptionalMember(TYPE_VERSION_TEXT, getTypeVersion().map(Node::from))
                .withOptionalMember(HOMEPAGE_TEXT, getHomepage().map(Node::from))
                .build();
    }

    /**
     * Gets this TraceMetadata's id.
     * The id is the identifier of the artifact. For example, Java packages should use the Maven artifact ID.
     *
     * @return TraceMetadata's id
     */
    public String getId() {
        return id;
    }

    /**
     * Gets this TraceMetadata's version (for example, the AWS SDK release number).
     *
     * @return TraceMetadata's version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets this TraceMetadata's timestamp.
     * The timestamp is the RFC 3339 date and time that the artifact was created.
     *
     * @return TraceMetadata's timestamp
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Gets this TraceMetadata's type. For code generation, this is the programming language.
     *
     * @return TraceMetadata's type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets this TraceMetadata's TypeVersion in an Optional container. For example, when defining
     * trace files for Java source code, the typeVersion would be the minimum supported JDK version.
     * Different artifacts may have different output based on the version targets (for example the ability
     * to use more features in a newer version of a language).
     *
     * @return Optional container with type version or empty container if
     * TypeVersion has not been set.
     */
    public Optional<String> getTypeVersion() {
        return Optional.ofNullable(typeVersion);
    }

    /**
     * Gets this TraceMetadata's Homepage in an Optional container.
     * The homepage is the homepage URL of the artifact.
     *
     * @return Optional container with homepage or empty container if
     * homepage has not been set
     */
    public Optional<String> getHomepage() {
        return Optional.ofNullable(homepage);
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
                .id(id)
                .version(version)
                .timestamp(timestamp)
                .type(type)
                .typeVersion(typeVersion)
                .homepage(homepage);
    }

    public static final class Builder implements SmithyBuilder<TraceMetadata> {
        private String id;
        private String version;
        private String timestamp;
        private String type;
        private String typeVersion;
        private String homepage;

        /**
         * @return The TraceMetadata object corresponding to this builder.
         */
        public TraceMetadata build() {
            return new TraceMetadata(this);
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the timestamp as the current time in RFC 3339 format.
         *
         * @return This builder.
         */
        public Builder setTimestampAsNow() {
            //set timestamp based on current time
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            this.timestamp = dateFormat.format(new Date());
            return this;
        }

        /**
         * Sets this builder's typeVersion.
         *
         * @param typeVersion typeVersion of TraceMetadata.
         * @return This builder.
         */
        public Builder typeVersion(String typeVersion) {
            this.typeVersion = typeVersion;
            return this;
        }

        /**
         * Sets this builder's homepage.
         *
         * @param homepage homepage of TraceMetadata.
         * @return This builder.
         */
        public Builder homepage(String homepage) {
            this.homepage = homepage;
            return this;
        }

    }

}
