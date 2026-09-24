/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.NotPropertyTrait;
import software.amazon.smithy.model.traits.PropertyTrait;
import software.amazon.smithy.model.traits.ResourceIdentifierTrait;

/**
 * Matches the members of a structure to a resource's identifiers or properties
 * by name, honoring {@code @resourceIdentifier}, {@code @property}, and
 * {@code @notProperty}, mirroring the standard lifecycle resolution.
 *
 * <p>This is the {@code ...From} inference used by the resource lifecycle traits:
 * given the structure a {@code identifiersFrom}/{@code propertiesFrom} pointer
 * resolves to, it reports which members map to a resource identifier or property
 * and which do not.
 */
final class ResourceMemberInference {

    private ResourceMemberInference() {}

    // Whether a member set is being matched against a resource's identifiers or its properties.
    enum BindingKind {
        IDENTIFIER, PROPERTY
    }

    /**
     * Infers which members of a structure provide the resource's identifiers or properties.
     *
     * @param resource The resource being bound.
     * @param element The structure to match members against.
     * @param kind Whether to match identifier names or property names.
     * @return The matched and unmatched members.
     */
    static MemberInferenceResult inferByName(ResourceShape resource, StructureShape element, BindingKind kind) {
        MemberInferenceResult result = new MemberInferenceResult();
        for (MemberShape member : element.members()) {
            if (member.hasTrait(NotPropertyTrait.ID)) {
                continue;
            }
            String boundName = boundName(member, kind);
            boolean matched = kind == BindingKind.IDENTIFIER
                    ? resource.getIdentifiers().containsKey(boundName)
                    : resource.getProperties().containsKey(boundName);
            if (matched) {
                result.matched.put(boundName, member);
            } else {
                result.unmatched.add(member);
            }
        }
        return result;
    }

    private static String boundName(MemberShape member, BindingKind kind) {
        if (kind == BindingKind.IDENTIFIER) {
            return member.getTrait(ResourceIdentifierTrait.class)
                    .map(ResourceIdentifierTrait::getValue)
                    .orElseGet(member::getMemberName);
        }
        return member.getTrait(PropertyTrait.class)
                .flatMap(PropertyTrait::getName)
                .orElseGet(member::getMemberName);
    }
}
