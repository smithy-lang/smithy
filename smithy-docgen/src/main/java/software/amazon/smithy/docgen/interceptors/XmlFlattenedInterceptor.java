/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.DocGenerationContext;
import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Adds information to the protocol section for members indicating whether they target
 * a flat list/map or wrapped list/map depending on whether they have the
 * <a href="https://smithy.io/2.0/spec/protocol-traits.html#xmlflattened-trait">
 * xmlFlattened</a> trait.
 */
@SmithyInternalApi
public class XmlFlattenedInterceptor implements CodeInterceptor<ProtocolSection, DocWriter> {
    private static final Pair<String, String> WRAPPED_LIST_REF = Pair.of(
            "wrapped",
            "https://smithy.io/2.0/spec/protocol-traits.html#wrapped-list-serialization");
    private static final Pair<String, String> FLAT_LIST_REF = Pair.of(
            "flat",
            "https://smithy.io/2.0/spec/protocol-traits.html#flattened-list-serialization");
    private static final Pair<String, String> WRAPPED_MAP_REF = Pair.of(
            "wrapped",
            "https://smithy.io/2.0/spec/protocol-traits.html#wrapped-map-serialization");
    private static final Pair<String, String> FLAT_MAP_REF = Pair.of(
            "flat",
            "https://smithy.io/2.0/spec/protocol-traits.html#flattened-map-serialization");

    @Override
    public Class<ProtocolSection> sectionType() {
        return ProtocolSection.class;
    }

    @Override
    public boolean isIntercepted(ProtocolSection section) {
        if (!section.shape().isMemberShape()) {
            return false;
        }

        var protocolShape = section.context().model().expectShape(section.protocol());
        var protocolDefinition = protocolShape.expectTrait(ProtocolDefinitionTrait.class);
        if (!protocolDefinition.getTraits().contains(XmlFlattenedTrait.ID)) {
            return false;
        }

        var target = section.context().model().expectShape(section.shape().asMemberShape().get().getTarget());
        return target.isListShape() || target.isMapShape();
    }

    @Override
    public void write(DocWriter writer, String previousText, ProtocolSection section) {
        writer.write("""
                Serialization type: $R

                $L""", getRef(section.context(), section.shape()), previousText);

    }

    private Pair<String, String> getRef(DocGenerationContext context, Shape shape) {
        var target = context.model().expectShape(shape.asMemberShape().get().getTarget());
        if (target.isMapShape()) {
            if (shape.hasTrait(XmlFlattenedTrait.class)) {
                return FLAT_MAP_REF;
            }
            return WRAPPED_MAP_REF;
        }

        if (shape.hasTrait(XmlFlattenedTrait.class)) {
            return FLAT_LIST_REF;
        }
        return WRAPPED_LIST_REF;
    }
}
