/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.PropertyBindingIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.NotPropertyTrait;
import software.amazon.smithy.model.traits.PropertyTrait;
import software.amazon.smithy.model.traits.ResourceIdentifierTrait;

/**
 * Matches the members of a structure to a resource's identifiers or properties
 * by name, honoring {@code @resourceIdentifier}, {@code @property}, {@code @notProperty},
 * and traits marked with {@code @notProperty}.
 *
 * <p>This is the {@code ...From} inference used by the resource lifecycle traits:
 * given the structure a {@code identifiersFrom}/{@code propertiesFrom} pointer
 * resolves to, it reports which members map to a resource identifier or property
 * and which do not. An explicit {@code @resourceIdentifier} or {@code @property}
 * decides which kind a member is bound to; otherwise the member name is matched
 * against both. A member bound to the other kind (an identifier when inferring
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
     * @param model The model, used to find the traits marked with {@code @notProperty}.
     * @param resource The resource being bound.
     * @param element The structure to match members against.
     * @param kind Whether to match identifier names or property names.
     * @return The matched and unmatched members, and the members bound to the other kind.
     */
    static MemberInferenceResult inferByName(
            Model model,
            ResourceShape resource,
            StructureShape element,
            BindingKind kind
    ) {
        PropertyBindingIndex propertyIndex = PropertyBindingIndex.of(model);
        Map<String, List<MemberShape>> identifiers = new LinkedHashMap<>();
        Map<String, List<MemberShape>> properties = new LinkedHashMap<>();
        Set<MemberShape> unmatched = new HashSet<>();
        for (MemberShape member : element.members()) {
            if (member.hasTrait(NotPropertyTrait.ID)) {
                continue;
            }
            // A member with a trait marked @notProperty, such as @resourceIdentifier or
            // @idempotencyToken, is not bound to a resource property, as the notProperty trait
            // defines. A resource cannot declare an identifier and a property with the same name,
            // so at most one matches.
            boolean notProperty = propertyIndex.isNotPropertyMember(member);
            String identifier = identifierName(resource, member);
            String property = notProperty ? null : propertyName(resource, member);
            if (identifier != null) {
                identifiers.computeIfAbsent(identifier, name -> new ArrayList<>()).add(member);
            } else if (property != null) {
                properties.computeIfAbsent(property, name -> new ArrayList<>()).add(member);
            } else if (!notProperty || member.hasTrait(ResourceIdentifierTrait.ID)) {
                // A member that is neither kind is unmatched, unless a trait marked @notProperty
                // excludes it. A @resourceIdentifier that names no identifier is still unmatched.
                unmatched.add(member);
            }
        }

        MemberInferenceResult result = new MemberInferenceResult();
        Map<String, MemberShape> identifierTarget = kind == BindingKind.IDENTIFIER ? result.matched : result.otherKind;
        Map<String, MemberShape> propertyTarget = kind == BindingKind.PROPERTY ? result.matched : result.otherKind;
        for (Map.Entry<String, List<MemberShape>> entry : identifiers.entrySet()) {
            resolveIdentifier(entry.getKey(), entry.getValue(), identifierTarget, unmatched);
        }
        for (Map.Entry<String, List<MemberShape>> entry : properties.entrySet()) {
            if (entry.getValue().size() == 1) {
                propertyTarget.put(entry.getKey(), entry.getValue().get(0));
            } else {
                // As in the standard lifecycle, members resolving to the same property conflict.
                result.propertyConflicts.put(entry.getKey(), entry.getValue());
            }
        }

        // Report unmatched members in definition order.
        for (MemberShape member : element.members()) {
            if (unmatched.contains(member)) {
                result.unmatched.add(member);
            }
        }
        return result;
    }

    // Resolves the members bound to one identifier. As in the standard lifecycle, an explicit
    // @resourceIdentifier wins over a member that only matches by name; that member is then neither
    // kind. Several explicit bindings to one identifier are already an error (ResourceIdentifierBinding),
    // so none of them is used.
    private static void resolveIdentifier(
            String name,
            List<MemberShape> members,
            Map<String, MemberShape> target,
            Set<MemberShape> unmatched
    ) {
        if (members.size() == 1) {
            target.put(name, members.get(0));
            return;
        }
        List<MemberShape> explicit = new ArrayList<>();
        for (MemberShape member : members) {
            if (member.hasTrait(ResourceIdentifierTrait.ID)) {
                explicit.add(member);
            }
        }
        if (explicit.size() == 1) {
            target.put(name, explicit.get(0));
        }
        for (MemberShape member : members) {
            if (!explicit.contains(member)) {
                unmatched.add(member);
            }
        }
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
    // otherwise the member name is used. Callers skip members that cannot be properties.
    private static String propertyName(ResourceShape resource, MemberShape member) {
        String name = member.getTrait(PropertyTrait.class)
                .flatMap(PropertyTrait::getName)
                .orElseGet(member::getMemberName);
        return resource.getProperties().containsKey(name) ? name : null;
    }
}
