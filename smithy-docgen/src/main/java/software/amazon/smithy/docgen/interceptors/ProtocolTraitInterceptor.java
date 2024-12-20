/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import software.amazon.smithy.docgen.sections.ProtocolSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ProtocolDefinitionTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.CodeInterceptor;

/**
 * Implements an interceptor that adds protocol trait documentation.
 *
 * @param <T> The class of the protocol trait.
 */
abstract class ProtocolTraitInterceptor<T extends Trait> implements CodeInterceptor<ProtocolSection, DocWriter> {

    /**
     * @return returns the class of the protocol trait.
     */
    protected abstract Class<T> getTraitClass();

    /**
     * @return returns the shape id of the protocol trait.
     */
    protected abstract ShapeId getTraitId();

    @Override
    public boolean isIntercepted(ProtocolSection section) {
        if (section.shape().getMemberTrait(section.context().model(), getTraitClass()).isEmpty()) {
            return false;
        }
        var protocolShape = section.context().model().expectShape(section.protocol());
        var protocolDefinition = protocolShape.expectTrait(ProtocolDefinitionTrait.class);
        return protocolDefinition.getTraits().contains(getTraitId());
    }

    @Override
    public Class<ProtocolSection> sectionType() {
        return ProtocolSection.class;
    }

    @Override
    public void write(DocWriter writer, String previousText, ProtocolSection section) {
        var trait = section.shape().getMemberTrait(section.context().model(), getTraitClass()).get();
        write(writer, previousText, section, trait);
    }

    abstract void write(DocWriter writer, String previousText, ProtocolSection section, T trait);
}
