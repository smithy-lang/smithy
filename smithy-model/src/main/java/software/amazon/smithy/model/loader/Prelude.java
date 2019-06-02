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

package software.amazon.smithy.model.loader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Represents the prelude model available to every Smithy model.
 *
 * <p>The prelude consists of public prelude shapes like
 * {@code smithy.api#String} and traits that are available to
 * all models like {@code box} and {@code required}.
 */
public final class Prelude {
    /** The Smithy prelude namespace. */
    public static final String NAMESPACE = "smithy.api";

    private static final List<AbstractShapeBuilder> PUBLIC_PRELUDE_SHAPES = ListUtils.of(
            StringShape.builder().id(NAMESPACE + "#String"),
            BlobShape.builder().id(NAMESPACE + "#Blob"),
            BigIntegerShape.builder().id(NAMESPACE + "#BigInteger"),
            BigDecimalShape.builder().id(NAMESPACE + "#BigDecimal"),
            TimestampShape.builder().id(NAMESPACE + "#Timestamp"),
            DocumentShape.builder().id(NAMESPACE + "#Document"),
            BooleanShape.builder().id(NAMESPACE + "#Boolean").addTrait(new BoxTrait()),
            BooleanShape.builder().id(NAMESPACE + "#PrimitiveBoolean"),
            ByteShape.builder().id(NAMESPACE + "#Byte").addTrait(new BoxTrait()),
            ByteShape.builder().id(NAMESPACE + "#PrimitiveByte"),
            ShortShape.builder().id(NAMESPACE + "#Short").addTrait(new BoxTrait()),
            ShortShape.builder().id(NAMESPACE + "#PrimitiveShort"),
            IntegerShape.builder().id(NAMESPACE + "#Integer").addTrait(new BoxTrait()),
            IntegerShape.builder().id(NAMESPACE + "#PrimitiveInteger"),
            LongShape.builder().id(NAMESPACE + "#Long").addTrait(new BoxTrait()),
            LongShape.builder().id(NAMESPACE + "#PrimitiveLong"),
            FloatShape.builder().id(NAMESPACE + "#Float").addTrait(new BoxTrait()),
            FloatShape.builder().id(NAMESPACE + "#PrimitiveFloat"),
            DoubleShape.builder().id(NAMESPACE + "#Double").addTrait(new BoxTrait()),
            DoubleShape.builder().id(NAMESPACE + "#PrimitiveDouble"));

    private static final Set<ShapeId> PUBLIC_PRELUDE_SHAPE_IDS;

    static {
        PUBLIC_PRELUDE_SHAPE_IDS = PUBLIC_PRELUDE_SHAPES.stream()
                .map(AbstractShapeBuilder::getId)
                .collect(SetUtils.toUnmodifiableSet());
    }

    private Prelude() {}

    /**
     * Checks if the given shape ID is defined by the prelude.
     *
     * @param id Shape ID to check.
     * @return Returns true if the shape is a prelude shape.
     */
    public static boolean isPreludeShape(ShapeId id) {
        return getPreludeModel().getShapeIndex().getShape(id).isPresent();
    }

    /**
     * Checks if the given shape is defined by the prelude.
     *
     * @param shape Shape to check.
     * @return Returns true if the shape is a prelude shape.
     */
    public static boolean isPreludeShape(Shape shape) {
        return isPreludeShape(shape.getId());
    }

    /**
     * Checks if the given shape is defined by the prelude and is not
     * marked with the {@code private} trait.
     *
     * @param id Shape to check.
     * @return Returns true if the shape is a public prelude shape.
     */
    public static boolean isPublicPreludeShape(ShapeId id) {
        return PUBLIC_PRELUDE_SHAPE_IDS.contains(id);
    }

    /**
     * Checks if the given shape is defined by the prelude and is not
     * marked with the {@code private} trait.
     *
     * @param shape Shape to check.
     * @return Returns true if the shape is a public prelude shape.
     */
    public static boolean isPublicPreludeShape(Shape shape) {
        return isPreludeShape(shape.getId());
    }

    /**
     * Checks if the given trait definition is defined by the prelude.
     *
     * @param fullyQualifiedTraitName Trait to check.
     * @return Returns true if the trait is defined by the prelude.
     */
    public static boolean isPreludeTraitDefinition(String fullyQualifiedTraitName) {
        return getPreludeModel().getTraitDefinition(fullyQualifiedTraitName).isPresent();
    }

    /**
     * Returns the resolved shape of a shape target by first checking if a
     * shape in the namespace relative to the target matches the given name,
     * and then by checking if a public prelude shape matches the given name.
     *
     * @param index Shape index to resolve against.
     * @param fromNamespace Namespace the target was defined in.
     * @param target The shape target (e.g., "foo", "smithy.api#String", etc.).
     * @return Returns the optionally resolved shape.
     * @throws ShapeIdSyntaxException if the target or namespace is invalid.
     */
    public static Optional<Shape> resolveShapeId(ShapeIndex index, String fromNamespace, String target) {
        // First check shapes in the same namespace.
        return OptionalUtils.or(index.getShape(ShapeId.fromOptionalNamespace(fromNamespace, target)),
                // Then check shapes in the prelude that are public.
                () -> index.getShape(ShapeId.fromParts(NAMESPACE, target))
                        .filter(Prelude::isPublicPreludeShape));
    }

    // Used by the ModelAssembler to load the prelude into another visitor.
    static Model getPreludeModel() {
        return PreludeHolder.PRELUDE;
    }

    // Lazy initialization holder class idiom for loading prelude traits and shapes.
    private static final class PreludeHolder {
        private static final Model PRELUDE = loadPrelude();

        private static Model loadPrelude() {
            LoaderVisitor visitor = new LoaderVisitor(ModelAssembler.LazyTraitFactoryHolder.INSTANCE);

            // Register prelude shape definitions.
            for (AbstractShapeBuilder builder : PUBLIC_PRELUDE_SHAPES) {
                visitor.onShape(builder);
            }

            // Register prelude trait definitions.
            String filename = "smithy-prelude-traits.json";

            try (InputStream inputStream = Prelude.class.getResourceAsStream(filename)) {
                String contents = IoUtils.toUtf8String(inputStream);
                new NodeModelLoader().load(filename, () -> contents, visitor);
                return visitor.onEnd().unwrap();
            } catch (IOException | UncheckedIOException e) {
                throw new ModelImportException(String.format("Unable to load prelude model `%s`: %s",
                        filename, e.getMessage()), e);
            }
        }
    }
}
