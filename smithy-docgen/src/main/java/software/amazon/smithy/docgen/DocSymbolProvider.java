/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.AuthDefinitionTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.model.traits.StringTrait;
import software.amazon.smithy.model.traits.TitleTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Creates documentation Symbols for each shape in the model.
 *
 * <p>These symbols contain many important pieces of metadata. Particularly
 * important are:
 *
 * <ul>
 *     <li>{@code name}: The name of the symbol will be used as the title for its
 *     definition section. For services, this defaults to the value of the
 *     {@code title} trait. For other shapes, it defaults to the shape name including
 *     any renames from the attached service.
 *
 *     <li>{@code definitionFile}: The file in which the documentation for this shape
 *     should be written. By default these are all written to a single flat directory.
 *     If this is empty, the shape does not have its own definition section and cannot
 *     be linked to.
 *
 *     <li>{@link #SHAPE_PROPERTY}: A named Shape property containing the shape that
 *     the symbol represents. Decorators provided by
 *     {@link DocIntegration#decorateSymbolProvider} MUST set or preserve this
 *     property.
 *
 *     <li>{@link #OPERATION_PROPERTY}: A named OperationShape property containing the
 *     operation shape that the shape is bound to. This will only be present on
 *     structure shapes that have the {@code input} or {@code output} traits.
 *
 *     <li>{@link #LINK_ID_PROPERTY}: A named String property containing the string to
 *     use for the id for links to the shape. In HTML, this would be the {@code id} for
 *     the tag containing the shape's definition. Given a link id {@code foo}, a link
 *     to the shape's definition might look like {@code https://example.com/shapes#foo}
 *     for example. If this or {@code definitionFile} is empty, it is not possible to
 *     link to the shape.
 *
 *     <li>{@link #ENABLE_DEFAULT_FILE_EXTENSION}: A named boolean property indicating
 *     whether the symbol's definition file should have the default file extension
 *     applied. If not present or set to {@code false}, the file extension will not be
 *     applied.
 * </ul>
 *
 * <p>Decorators provided by {@link DocIntegration#decorateSymbolProvider} MUST set
 * these properties or preserve
 */
@SmithyUnstableApi
public final class DocSymbolProvider extends ShapeVisitor.Default<Symbol> implements SymbolProvider {

    /**
     * The name for a shape symbol's named property containing the shape the symbol
     * represents.
     *
     * <p>Decorators provided by {@link DocIntegration#decorateSymbolProvider} MUST
     * preserve this property.
     *
     * <p>Use {@code symbol.expectProperty(SHAPE_PROPERTY, Shape.class)} to access this
     * property.
     */
    public static final String SHAPE_PROPERTY = "shape";

    /**
     * The operation that the symbol's shape is bound to.
     *
     * <p>This property will only be present on structures that have either the
     * {@code input} or {@code output} trait.
     *
     * <p>Use {@code symbol.getProperty(OPERATION_PROPERTY, OperationShape.class)} to
     * access this property.
     */
    public static final String OPERATION_PROPERTY = "operation";

    /**
     * The name for a shape symbol's named property containing the string to use for
     * the id for links to the shape. In HTML, this would be the {@code id} for the tag
     * containing the shape's definition. Given a link id {@code foo}, a link to the
     * shape's definition might look like {@code https://example.com/shapes#foo} for
     * example.
     *
     * <p>If this or {@code definitionFile} is empty, it is not possible to link to
     * the shape.
     *
     * <p>Use {@code symbol.getProperty(LINK_ID_PROPERTY, String.class)} to access this
     * property.
     */
    public static final String LINK_ID_PROPERTY = "linkId";

    /**
     * A named boolean property indicating whether the symbol's definition file should
     * have the default file extension applied. If not present or set to {@code false},
     * the file extension will not be applied.
     *
     * <p>Use {@code symbol.getProperty(LINK_ID_PROPERTY, Boolean.class)} to access this
     * property.
     */
    public static final String ENABLE_DEFAULT_FILE_EXTENSION = "enableDefaultFileExtension";

    private static final Logger LOGGER = Logger.getLogger(DocSymbolProvider.class.getName());
    private static final String SERVICE_FILE = "index";

    private final Model model;
    private final DocSettings docSettings;
    private final ServiceShape serviceShape;
    private final Map<ShapeId, OperationShape> ioToOperation;

    /**
     * Constructor.
     *
     * @param model The model to provide symbols for.
     * @param docSettings Settings used to customize symbol creation.
     */
    public DocSymbolProvider(Model model, DocSettings docSettings) {
        this.model = model;
        this.docSettings = docSettings;
        this.serviceShape = model.expectShape(docSettings.service(), ServiceShape.class);
        this.ioToOperation = mapIoShapesToOperations(model);
    }

    private Map<ShapeId, OperationShape> mapIoShapesToOperations(Model model) {
        // Map input and output structures to their containing shapes. These will be
        // documented alongside their associated operations, so we need said operations
        // when generating symbols for them. Pre-computing this mapping is a bit faster
        // than just running a selector every time we hit an IO
        // shape.
        var operationIoMap = new HashMap<ShapeId, OperationShape>();
        var operationIndex = OperationIndex.of(model);
        for (var operation : model.getOperationShapes()) {
            operationIndex.getInputShape(operation)
                    .filter(i -> i.hasTrait(InputTrait.class))
                    .ifPresent(i -> operationIoMap.put(i.getId(), operation));
            operationIndex.getOutputShape(operation)
                    .filter(i -> i.hasTrait(OutputTrait.class))
                    .ifPresent(i -> operationIoMap.put(i.getId(), operation));
        }
        return Map.copyOf(operationIoMap);
    }

    @Override
    public Symbol toSymbol(Shape shape) {
        var symbol = shape.accept(this);
        LOGGER.fine(() -> format("Creating symbol from %s: %s", shape, symbol));
        return symbol;
    }

    @Override
    public Symbol serviceShape(ServiceShape shape) {
        return getSymbolBuilder(shape)
                .definitionFile(getDefinitionFile(SERVICE_FILE))
                .build();
    }

    @Override
    public Symbol resourceShape(ResourceShape shape) {
        return getSymbolBuilderWithFile(shape).build();
    }

    @Override
    public Symbol operationShape(OperationShape shape) {
        return getSymbolBuilderWithFile(shape).build();
    }

    @Override
    public Symbol structureShape(StructureShape shape) {
        var builder = getSymbolBuilder(shape);
        if (shape.hasTrait(TraitDefinition.class)) {
            if (shape.hasTrait(AuthDefinitionTrait.class)) {
                builder.definitionFile(getDefinitionFile(SERVICE_FILE));
            }
            return builder.build();
        }

        builder.definitionFile(getDefinitionFile(serviceShape, shape));
        if (ioToOperation.containsKey(shape.getId())) {
            // Input and output structures are documented on the operation's definition page.
            var operation = ioToOperation.get(shape.getId());
            builder.definitionFile(getDefinitionFile(serviceShape, operation));
            builder.putProperty(OPERATION_PROPERTY, operation);
        }
        return builder.build();
    }

    @Override
    public Symbol enumShape(EnumShape shape) {
        return getSymbolBuilderWithFile(shape).build();
    }

    @Override
    public Symbol intEnumShape(IntEnumShape shape) {
        return getSymbolBuilderWithFile(shape).build();
    }

    @Override
    public Symbol unionShape(UnionShape shape) {
        return getSymbolBuilderWithFile(shape).build();
    }

    @Override
    public Symbol memberShape(MemberShape shape) {
        var builder = getSymbolBuilder(shape)
                .definitionFile(getDefinitionFile(serviceShape, model.expectShape(shape.getId().withoutMember())));

        Optional<String> containerLinkId = model.expectShape(shape.getContainer())
                .accept(this)
                .getProperty(LINK_ID_PROPERTY, String.class);
        if (containerLinkId.isPresent()) {
            var linkId = containerLinkId.get() + "-" + getLinkId(getShapeName(serviceShape, shape));
            builder.putProperty(LINK_ID_PROPERTY, linkId);
        }
        return builder.build();
    }

    private Symbol.Builder getSymbolBuilder(Shape shape) {
        var name = getShapeName(serviceShape, shape);
        return Symbol.builder()
                .name(name)
                .putProperty(SHAPE_PROPERTY, shape)
                .putProperty(LINK_ID_PROPERTY, getLinkId(name))
                .putProperty(ENABLE_DEFAULT_FILE_EXTENSION, true);
    }

    private Symbol.Builder getSymbolBuilderWithFile(Shape shape) {
        return getSymbolBuilder(shape)
                .definitionFile(getDefinitionFile(serviceShape, shape));
    }

    private String getDefinitionFile(ServiceShape serviceShape, Shape shape) {
        var path = getShapeName(serviceShape, shape).replaceAll("\\s+", "");
        if (shape.isResourceShape()) {
            path = "resources/" + path;
        } else if (shape.isOperationShape()) {
            path = "operations/" + path;
        } else {
            path = "shapes/" + path;
        }
        return getDefinitionFile(path);
    }

    private String getDefinitionFile(String path) {
        return "content/" + path;
    }

    private String getShapeName(ServiceShape serviceShape, Shape shape) {
        if (shape.isServiceShape()) {
            return shape.getTrait(TitleTrait.class)
                    .map(StringTrait::getValue)
                    .orElse(shape.getId().getName());
        }
        if (shape.isMemberShape()) {
            return toMemberName(shape.asMemberShape().get());
        } else {
            return shape.getId().getName(serviceShape);
        }
    }

    private String getLinkId(String shapeName) {
        return shapeName.toLowerCase(Locale.ENGLISH).replaceAll("\\s+", "-");
    }

    // All other shapes don't get generation, so we'll do null checks where this might
    // have impact.
    @Override
    protected Symbol getDefault(Shape shape) {
        return getSymbolBuilder(shape).build();
    }

    /**
     * Adds file extensions to symbol definition files. Used with {@link DocFormat}
     * by default.
     *
     * <p>Symbols can set {@link #ENABLE_DEFAULT_FILE_EXTENSION} to {@code false} to
     * disable this on a per-symbol basis.
     */
    public static final class FileExtensionDecorator implements SymbolProvider {
        private final SymbolProvider wrapped;
        private final String extension;

        /**
         * Constructor.
         * @param wrapped The symbol provider to wrap.
         * @param extension The file extension to add. This must include any necessary periods.
         */
        public FileExtensionDecorator(SymbolProvider wrapped, String extension) {
            this.wrapped = Objects.requireNonNull(wrapped);
            this.extension = Objects.requireNonNull(extension);
        }

        @Override
        public Symbol toSymbol(Shape shape) {
            var symbol = wrapped.toSymbol(shape);
            if (!symbol.getProperty(ENABLE_DEFAULT_FILE_EXTENSION, Boolean.class).orElse(false)) {
                return symbol;
            }
            return symbol.toBuilder()
                    .definitionFile(addExtension(symbol.getDefinitionFile()))
                    .declarationFile(addExtension(symbol.getDeclarationFile()))
                    .build();
        }

        private String addExtension(String path) {
            if (!StringUtils.isBlank(path) && !path.endsWith(extension)) {
                path += extension;
            }
            return path;
        }

        @Override
        public String toMemberName(MemberShape shape) {
            return wrapped.toMemberName(shape);
        }
    }
}
