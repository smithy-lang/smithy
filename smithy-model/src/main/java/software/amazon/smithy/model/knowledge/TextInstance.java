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

import java.util.Deque;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;

/**
 * Contains information about text that occurs in the Smithy IDL written by the owner,
 * inluding location metadata.
 */
public final class TextInstance {
    private final TextLocationType locationType;
    private final String text;
    private final Shape shape;
    private final Trait trait;
    private final List<String> traitPropertyPath;

    private TextInstance(
            final TextLocationType locationType,
            final String text,
            final Shape shape,
            final Trait trait,
            final Deque<String> traitPropertyPath
    ) {
        this.locationType = locationType;
        this.text = text;
        this.shape = shape;
        this.trait = trait;
        this.traitPropertyPath = traitPropertyPath != null
            ? ListUtils.copyOf(traitPropertyPath)
            : ListUtils.of();
    }

    static TextInstance createNamespaceText(String namespace) {
        Objects.requireNonNull(namespace, "'namespace' must be specified");
        return new TextInstance(TextLocationType.NAMESPACE, namespace, null, null, null);
    }

    static TextInstance createShapeInstance(Shape shape) {
        Objects.requireNonNull(shape, "'shape' must be specified");
        return new TextInstance(TextLocationType.SHAPE, shape.getId()
                    .getMember().orElseGet(() -> shape.getId().getName()),
                    shape, null, null);
    }

    static TextInstance createTraitInstance(String text, Shape shape, Trait trait, Deque<String> traitPath) {
        Objects.requireNonNull(trait, "'trait' must be specified");
        Objects.requireNonNull(shape, "'shape' must be specified");
        Objects.requireNonNull(text, "'text' must be specified");
        return new TextInstance(TextLocationType.APPLIED_TRAIT, text, shape, trait, traitPath);
    }

    /**
     * Retrieves the type of TextLocationType associated with the text.
     *
     * @return Returns the TextLocationType.
     */
    public TextLocationType getLocationType() {
        return locationType;
    }

    /**
     * Retrieves the text content of the TextInstance.
     *
     * @return Returns the model text.
     */
    public String getText() {
        return text;
    }

    /**
     * Gets the shape associated with the text.
     *
     * @return Returns the shape if the text is associated with one. Otherwise, returns null.
     */
    public Shape getShape() {
        return shape;
    }

    /**
     * Gets the trait associated with the text.
     *
     * @return Returns the trait if the text is associated with one. Otherwise, returns null.
     */
    public Trait getTrait() {
        return trait;
    }

    /**
     * Gets the ordered path components within a trait's value the text is associated with.
     *
     * @return Returns the property path if the text is associated with a trait's value.
     */
    public List<String> getTraitPropertyPath() {
        return traitPropertyPath;
    }

    /**
     * Enum type indicating what kind of location in the model associated text appeared in.
     */
    public enum TextLocationType {
        SHAPE,
        APPLIED_TRAIT,
        NAMESPACE
    }
}
