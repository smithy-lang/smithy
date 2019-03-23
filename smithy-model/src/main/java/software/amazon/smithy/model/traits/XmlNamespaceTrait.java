/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.traits;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.ToSmithyBuilder;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
* Defines the XML Namespace prefix and URI.
*/
public final class XmlNamespaceTrait extends AbstractTrait implements ToSmithyBuilder<XmlNamespaceTrait> {
    private static final String TRAIT = "smithy.api#xmlNamespace";
    private static final List<String> XML_NAMESPACE_PROPERTIES = List.of("uri");

    private final String uri;

    private XmlNamespaceTrait(Builder builder) {
        super(TRAIT, builder.getSourceLocation());
        uri = SmithyBuilder.requiredState("uri", builder.uri);
    }

    public String getUri() {
        return uri;
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(Map.of(), getSourceLocation())
                .withMember("uri", Node.from(uri));
    }

    /**
    * @return Returns a builder used to create an XmlNamespace trait.
    */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).uri(uri);
    }

    /**
    * Builder used to create an XmlNamespace trait.
    */
    public static final class Builder extends AbstractTraitBuilder<XmlNamespaceTrait, Builder> {
        private String uri;

        private Builder() {}

        public Builder uri(String uri) {
            this.uri = Objects.requireNonNull(uri);
            return this;
        }

        @Override
        public XmlNamespaceTrait build() {
            return new XmlNamespaceTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public String getTraitName() {
            return TRAIT;
        }

        @Override
        public XmlNamespaceTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            ObjectNode node = value.expectObjectNode();
            node.warnIfAdditionalProperties(XML_NAMESPACE_PROPERTIES);
            builder.uri(node.expectMember("uri").expectStringNode().getValue());
            return builder.build();
        }
    }
}
