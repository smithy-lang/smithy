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

package software.amazon.smithy.model.knowledge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.ReferencesTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.validators.TraitValueValidator;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Index containing the full set of {@link TextInstance}s associated with a model.
 */
@SmithyUnstableApi
public final class TextIndex implements KnowledgeIndex {
    private final List<TextInstance> textInstanceList = new ArrayList<>();

    public TextIndex(Model model) {
        Set<String> visitedNamespaces = new HashSet<>();
        // Validating the prelude is a feature for internal-only Smithy development
        Node validatePreludeNode = model.getMetadata().get(TraitValueValidator.VALIDATE_PRELUDE);
        boolean validatePrelude = validatePreludeNode != null
                ? validatePreludeNode.expectBooleanNode().getValue()
                : false;

        for (final Shape shape : model.toSet()) {
            if (validatePrelude || !Prelude.isPreludeShape(shape)) {
                if (visitedNamespaces.add(shape.getId().getNamespace())) {
                    textInstanceList.add(TextInstance.createNamespaceText(shape.getId().getNamespace()));
                }
                computeShapeTextInstances(shape, textInstanceList, model);
            }
        }
    }

    public static TextIndex of(Model model) {
        return model.getKnowledge(TextIndex.class, TextIndex::new);
    }

    public Collection<TextInstance> getTextInstances() {
        return Collections.unmodifiableList(textInstanceList);
    }

    private static void computeShapeTextInstances(
            Shape shape,
            Collection<TextInstance> textInstances,
            Model model
    ) {
        textInstances.add(TextInstance.createShapeInstance(shape));

        for (Trait trait : shape.getAllTraits().values()) {
            model.getShape(trait.toShapeId()).ifPresent(traitShape -> {
                computeTextInstancesForAppliedTrait(trait.toNode(), trait, shape, textInstances,
                        new ArrayDeque<>(), model, traitShape);
            });
        }
    }

    private static void computeTextInstancesForAppliedTrait(
            Node node,
            Trait trait,
            Shape parentShape,
            Collection<TextInstance> textInstances,
            Deque<String> propertyPath,
            Model model,
            Shape currentTraitPropertyShape
    ) {
        if (trait.toShapeId().equals(ReferencesTrait.ID)) {
            //Skip ReferenceTrait because it is referring to other shape names already being checked
        } else if (node.isStringNode()) {
            textInstances.add(TextInstance.createTraitInstance(
                node.expectStringNode().getValue(), parentShape, trait, propertyPath));
        } else if (node.isObjectNode()) {
            ObjectNode objectNode = node.expectObjectNode();
            objectNode.getStringMap().entrySet().forEach(memberEntry -> {
                propertyPath.offerLast(memberEntry.getKey());
                Shape memberTypeShape = getChildMemberShapeType(memberEntry.getKey(),
                        model, currentTraitPropertyShape);
                if (memberTypeShape == null) {
                    //This means the "property" key value isn't modeled in the trait's structure/shape definition
                    //and this text instance is unique
                    propertyPath.offerLast("key");
                    textInstances.add(TextInstance.createTraitInstance(
                        memberEntry.getKey(), parentShape, trait, propertyPath));
                    propertyPath.removeLast();
                }
                computeTextInstancesForAppliedTrait(memberEntry.getValue(), trait, parentShape, textInstances,
                        propertyPath, model, memberTypeShape);
                propertyPath.removeLast();
            });
        } else if (node.isArrayNode()) {
            int index = 0;
            for (Node nodeElement : node.expectArrayNode().getElements()) {
                propertyPath.offerLast(Integer.toString(index));
                Shape memberTypeShape = getChildMemberShapeType(null,
                        model, currentTraitPropertyShape);
                computeTextInstancesForAppliedTrait(nodeElement, trait, parentShape, textInstances,
                        propertyPath, model, memberTypeShape);
                propertyPath.removeLast();
                ++index;
            }
        }
    }

    private static Shape getChildMemberShapeType(String memberKey, Model model, Shape fromShape) {
        if (fromShape != null) {
            for (MemberShape member : fromShape.members()) {
                if (member.getMemberName().equals(memberKey)) {
                    return model.getShape(member.getTarget()).get();
                }
            }
        }
        return null;
    }
}
