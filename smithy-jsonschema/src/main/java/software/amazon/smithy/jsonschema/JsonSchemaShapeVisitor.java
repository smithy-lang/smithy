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

package software.amazon.smithy.jsonschema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.TitleTrait;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.utils.ListUtils;

final class JsonSchemaShapeVisitor extends ShapeVisitor.Default<Schema> {
    private static final String UNION_STRATEGY_ONE_OF = "oneOf";
    private static final String UNION_STRATEGY_OBJECT = "object";
    private static final String UNION_STRATEGY_STRUCTURE = "structure";

    private final Model model;
    private final ObjectNode config;
    private final RefStrategy refStrategy;
    private final PropertyNamingStrategy propertyNamingStrategy;
    private final List<JsonSchemaMapper> mappers;

    JsonSchemaShapeVisitor(
            Model model,
            ObjectNode config,
            RefStrategy refStrategy,
            PropertyNamingStrategy propertyNamingStrategy,
            List<JsonSchemaMapper> mappers
    ) {
        this.model = model;
        this.config = config;
        this.refStrategy = refStrategy;
        this.propertyNamingStrategy = propertyNamingStrategy;
        this.mappers = mappers;
    }

    @Override
    public Schema getDefault(Shape shape) {
        throw new UnsupportedOperationException("Unable to convert " + shape + " to JSON Schema");
    }

    @Override
    public Schema documentShape(DocumentShape shape) {
        // A document type becomes just `{}`
        return buildSchema(shape, createBuilder(shape, null));
    }

    @Override
    public Schema blobShape(BlobShape shape) {
        return buildSchema(shape, createBuilder(shape, "string"));
    }

    @Override
    public Schema booleanShape(BooleanShape shape) {
        return buildSchema(shape, createBuilder(shape, "boolean"));
    }

    @Override
    public Schema listShape(ListShape shape) {
        return buildSchema(shape, createCollectionType(shape));
    }

    @Override
    public Schema setShape(SetShape shape) {
        return buildSchema(shape, createCollectionType(shape).uniqueItems(true));
    }

    private Schema.Builder createCollectionType(CollectionShape shape) {
        return createBuilder(shape, "array").items(createRef(shape.getMember()));
    }

    private Schema createRef(MemberShape member) {
        if (config.getBooleanMemberOrDefault(JsonSchemaConstants.INLINE_MEMBERS)) {
            throw new UnsupportedOperationException(JsonSchemaConstants.INLINE_MEMBERS + " is not yet supported");
        }

        return Schema.builder().ref(refStrategy.toPointer(member.getId(), config)).build();
    }

    @Override
    public Schema mapShape(MapShape shape) {
        return buildSchema(shape, createBuilder(shape, "object")
                .propertyNames(createRef(shape.getKey()))
                .additionalProperties(createRef(shape.getValue())));
    }

    @Override
    public Schema byteShape(ByteShape shape) {
        return buildSchema(shape, createBuilder(shape, "number"));
    }

    @Override
    public Schema shortShape(ShortShape shape) {
        return buildSchema(shape, createBuilder(shape, "number"));
    }

    @Override
    public Schema integerShape(IntegerShape shape) {
        return buildSchema(shape, createBuilder(shape, "number"));
    }

    @Override
    public Schema longShape(LongShape shape) {
        return buildSchema(shape, createBuilder(shape, "number"));
    }

    @Override
    public Schema floatShape(FloatShape shape) {
        return buildSchema(shape, createBuilder(shape, "number"));
    }

    @Override
    public Schema doubleShape(DoubleShape shape) {
        return buildSchema(shape, createBuilder(shape, "number"));
    }

    @Override
    public Schema bigIntegerShape(BigIntegerShape shape) {
        return buildSchema(shape, createBuilder(shape, "number"));
    }

    @Override
    public Schema bigDecimalShape(BigDecimalShape shape) {
        return buildSchema(shape, createBuilder(shape, "number"));
    }

    @Override
    public Schema stringShape(StringShape shape) {
        return buildSchema(shape, createBuilder(shape, "string"));
    }

    @Override
    public Schema structureShape(StructureShape shape) {
        return structuredShape(shape, shape.getAllMembers().values());
    }

    private Schema structuredShape(Shape container, Collection<MemberShape> memberShapes) {
        Schema.Builder builder = createBuilder(container, "object");

        List<String> required = new ArrayList<>();
        for (MemberShape member : memberShapes) {
            String memberName = propertyNamingStrategy.toPropertyName(container, member, config);
            if (member.isRequired()) {
                required.add(memberName);
            }
            builder.putProperty(memberName, createRef(member));
        }

        builder.required(required);
        return buildSchema(container, builder);
    }

    @Override
    public Schema unionShape(UnionShape shape) {
        String unionStrategy = config.getStringMemberOrDefault(
                JsonSchemaConstants.SMITHY_UNION_STRATEGY, UNION_STRATEGY_ONE_OF);

        switch (unionStrategy) {
            case UNION_STRATEGY_OBJECT:
                return buildSchema(shape, createBuilder(shape, "object"));
            case UNION_STRATEGY_STRUCTURE:
                return structuredShape(shape, shape.getAllMembers().values());
            case UNION_STRATEGY_ONE_OF:
                List<Schema> schemas = new ArrayList<>();
                for (MemberShape member : shape.getAllMembers().values()) {
                    String memberName = propertyNamingStrategy.toPropertyName(shape, member, config);
                    schemas.add(Schema.builder()
                            .type("object")
                            .required(ListUtils.of(memberName))
                            .putProperty(memberName, createRef(member))
                            .build());
                }
                return buildSchema(shape, createBuilder(shape, "object").type(null).oneOf(schemas));
            default:
                throw new UnsupportedOperationException(String.format(
                        "Unknown %s strategy: %s", JsonSchemaConstants.SMITHY_UNION_STRATEGY, unionStrategy));
        }
    }

    @Override
    public Schema timestampShape(TimestampShape shape) {
        return buildSchema(shape, createBuilder(shape, "string"));
    }

    @Override
    public Schema memberShape(MemberShape memberShape) {
        Shape target = getTarget(memberShape);
        return buildSchema(memberShape, updateBuilder(memberShape, target.accept(this).toBuilder()));
    }

    private Shape getTarget(MemberShape member) {
        return model.getShape(member.getTarget())
                .orElseThrow(() -> new RuntimeException("Unable to find the shape targeted by " + member));
    }

    private Schema.Builder createBuilder(Shape shape, String defaultType) {
        return updateBuilder(shape, Schema.builder().type(defaultType));
    }

    /**
     * Updates a schema builder using shared logic across shapes.
     *
     * @param shape Shape being converted.
     * @param builder Schema being built.
     * @return Returns the updated schema builder.
     */
    private Schema.Builder updateBuilder(Shape shape, Schema.Builder builder) {
        shape.getTrait(DocumentationTrait.class)
                .map(DocumentationTrait::getValue)
                .ifPresent(builder::description);

        shape.getTrait(TitleTrait.class)
                .map(TitleTrait::getValue)
                .ifPresent(builder::title);

        shape.getTrait(MediaTypeTrait.class)
                .map(MediaTypeTrait::getValue)
                .ifPresent(builder::contentMediaType);

        shape.getTrait(PatternTrait.class)
                .map(PatternTrait::getPattern)
                .map(Pattern::pattern)
                .ifPresent(builder::pattern);

        shape.getTrait(RangeTrait.class).ifPresent(t -> {
            t.getMin().ifPresent(builder::minimum);
            t.getMax().ifPresent(builder::maximum);
        });

        shape.getTrait(LengthTrait.class).ifPresent(t -> {
            if (shape.isListShape() || shape.isSetShape()) {
                t.getMin().map(Long::intValue).ifPresent(builder::minItems);
                t.getMax().map(Long::intValue).ifPresent(builder::maxItems);
            } else if (shape.isMapShape()) {
                t.getMin().map(Long::intValue).ifPresent(builder::minProperties);
                t.getMax().map(Long::intValue).ifPresent(builder::maxProperties);
            } else {
                t.getMin().ifPresent(builder::minLength);
                t.getMax().ifPresent(builder::maxLength);
            }
        });

        if (shape.hasTrait(UniqueItemsTrait.class)) {
            builder.uniqueItems(true);
        }

        shape.getTrait(EnumTrait.class)
                .map(EnumTrait::getValues)
                .map(Map::keySet)
                .ifPresent(builder::enumValues);

        return builder;
    }

    /**
     * Builds a schema builder and applied schema mappers.
     *
     * <p>Schema builders created in each visitor method should not build
     * themselves and instead should invoke this method to finalize the
     * builder.
     *
     * @param shape Shape being converted.
     * @param builder Schema being built.
     * @return Returns the built schema.
     */
    private Schema buildSchema(Shape shape, Schema.Builder builder) {
        for (JsonSchemaMapper mapper : mappers) {
            mapper.updateSchema(shape, builder, config);
        }

        return builder.build();
    }
}
