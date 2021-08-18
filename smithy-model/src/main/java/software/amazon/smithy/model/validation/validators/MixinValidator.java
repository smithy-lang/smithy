/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Ensures that mixins do not introduce conflicting members across
 * mixin closures.
 *
 * <p>The kinds of errors detected by this validator are not actually
 * possible when loading models via the IDL or JSON, but are possible
 * when creating models manually.
 */
@SmithyInternalApi
public final class MixinValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (StructureShape shape : model.toSet(StructureShape.class)) {
            validateShape(model, shape, events);
        }
        for (UnionShape shape : model.toSet(UnionShape.class)) {
            validateShape(model, shape, events);
        }
        return events;
    }

    private void validateShape(Model model, Shape shape, List<ValidationEvent> events) {
        if (!shape.getMixins().isEmpty()) {
            validateMemberSources(model, shape, events);
        }
    }

    private void validateMemberSources(Model model, Shape shape, List<ValidationEvent> events) {
        Map<String, Set<ShapeId>> memberSources = new HashMap<>();

        // Add non-mixin members to start the map.
        for (MemberShape member : shape.members()) {
            if (member.getMixins().isEmpty()) {
                Set<ShapeId> self = new TreeSet<>();
                self.add(shape.getId());
                memberSources.put(member.getMemberName(), self);
            }
        }

        // Add each mixin member.
        for (ShapeId mixin : shape.getMixins()) {
            model.getShape(mixin).ifPresent(mixinShape -> {
                for (MemberShape member : mixinShape.members()) {
                    memberSources.computeIfAbsent(member.getMemberName(), name -> new TreeSet<>()).add(mixin);
                }
            });
        }

        // Remove entries that have no conflicts.
        memberSources.entrySet().removeIf(e -> e.getValue().size() <= 1);

        if (!memberSources.isEmpty()) {
            for (Map.Entry<String, Set<ShapeId>> entry : memberSources.entrySet()) {
                String memberName = entry.getKey();
                Set<ShapeId> conflicts = entry.getValue();
                String message = String.format(
                        "Member `%s` conflicts with members defined in the following mixins: %s",
                        memberName, conflicts);
                events.add(error(shape, message));
            }
        }
    }
}
