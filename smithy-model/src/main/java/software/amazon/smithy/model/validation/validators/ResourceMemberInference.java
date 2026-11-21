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
 * and which do not. A member bound to the other kind (an identifier when inferring
 * properties, or a property when inferring identifiers) is reported separately
 * rather than as unmatched, because a structure commonly carries both. Matching is
 * by name only; the caller checks that each matched or other-kind member targets
 * the shape of the identifier or property it is bound to.
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
     * @return The matched and unmatched members, and the members bound to the other kind.
     */
    static MemberInferenceResult inferByName(ResourceShape resource, StructureShape element, BindingKind kind) {
        BindingKind otherKind = otherKind(kind);
        MemberInferenceResult result = new MemberInferenceResult();
        for (MemberShape member : element.members()) {
            if (member.hasTrait(NotPropertyTrait.ID)) {
                continue;
            }
            String boundName = boundName(member, kind);
            String otherName = boundName(member, otherKind);
            if (isBound(resource, boundName, kind)) {
                result.matched.put(boundName, member);
            } else if (isBound(resource, otherName, otherKind)) {
                result.otherKind.put(otherName, member);
            } else {
                result.unmatched.add(member);
            }
        }
        return result;
    }

    /**
     * Gets the kind that is not the given one.
     *
     * @param kind The kind being inferred.
     * @return The other kind.
     */
    static BindingKind otherKind(BindingKind kind) {
        return kind == BindingKind.IDENTIFIER ? BindingKind.PROPERTY : BindingKind.IDENTIFIER;
    }

    private static boolean isBound(ResourceShape resource, String name, BindingKind kind) {
        return kind == BindingKind.IDENTIFIER
                ? resource.getIdentifiers().containsKey(name)
                : resource.getProperties().containsKey(name);
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
