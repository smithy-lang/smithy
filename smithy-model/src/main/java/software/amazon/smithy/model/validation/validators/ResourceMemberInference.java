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
 * and which do not. An explicit {@code @resourceIdentifier} or {@code @property}
 * decides which kind a member is bound to, as in the standard lifecycle; otherwise
 * the member name is matched against both. A member bound to the other kind (an
 * identifier when inferring properties, or a property when inferring identifiers)
 * is reported separately rather than as unmatched, because a structure commonly
 * carries both. Matching is by name only; the caller checks that each matched or
 * other-kind member targets the shape of the identifier or property it is bound to.
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
        MemberInferenceResult result = new MemberInferenceResult();
        for (MemberShape member : element.members()) {
            if (member.hasTrait(NotPropertyTrait.ID)) {
                continue;
            }
            String identifier = identifierName(resource, member);
            String property = propertyName(resource, member);
            // When a member matches both by name, the kind being inferred wins.
            String matched = kind == BindingKind.IDENTIFIER ? identifier : property;
            String other = kind == BindingKind.IDENTIFIER ? property : identifier;
            if (matched != null) {
                result.matched.put(matched, member);
            } else if (other != null) {
                result.otherKind.put(other, member);
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

    // The resource identifier a member is bound to, or null. An explicit @resourceIdentifier names
    // it; otherwise the member name is used, unless the member is explicitly a @property.
    private static String identifierName(ResourceShape resource, MemberShape member) {
        String name;
        if (member.hasTrait(ResourceIdentifierTrait.ID)) {
            name = member.expectTrait(ResourceIdentifierTrait.class).getValue();
        } else if (member.hasTrait(PropertyTrait.ID)) {
            return null;
        } else {
            name = member.getMemberName();
        }
        return resource.getIdentifiers().containsKey(name) ? name : null;
    }

    // The resource property a member is bound to, or null. An explicit @property(name) names it;
    // otherwise the member name is used, unless the member is explicitly a @resourceIdentifier.
    private static String propertyName(ResourceShape resource, MemberShape member) {
        if (member.hasTrait(ResourceIdentifierTrait.ID)) {
            return null;
        }
        String name = member.getTrait(PropertyTrait.class)
                .flatMap(PropertyTrait::getName)
                .orElseGet(member::getMemberName);
        return resource.getProperties().containsKey(name) ? name : null;
    }
}
