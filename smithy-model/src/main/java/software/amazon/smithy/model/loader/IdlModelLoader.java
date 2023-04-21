/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static java.lang.String.format;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.ListUtils;

final class IdlModelLoader {

    private static final String PUT_KEY = "put";
    private static final String CREATE_KEY = "create";
    private static final String READ_KEY = "read";
    private static final String UPDATE_KEY = "update";
    private static final String DELETE_KEY = "delete";
    private static final String LIST_KEY = "list";
    private static final String RESOURCES_KEY = "resources";
    private static final String OPERATIONS_KEY = "operations";
    private static final String PROPERTIES_KEY = "properties";
    private static final String RENAME_KEY = "rename";
    private static final String COLLECTION_OPERATIONS_KEY = "collectionOperations";
    private static final String IDENTIFIERS_KEY = "identifiers";
    private static final String VERSION_KEY = "version";
    private static final String TYPE_KEY = "type";
    private static final String ERRORS_KEY = "errors";

    static final Collection<String> RESOURCE_PROPERTY_NAMES = ListUtils.of(
            TYPE_KEY, CREATE_KEY, READ_KEY, UPDATE_KEY, DELETE_KEY, LIST_KEY,
            IDENTIFIERS_KEY, RESOURCES_KEY, OPERATIONS_KEY, PUT_KEY, PROPERTIES_KEY, COLLECTION_OPERATIONS_KEY);
    static final List<String> SERVICE_PROPERTY_NAMES = ListUtils.of(
            TYPE_KEY, VERSION_KEY, OPERATIONS_KEY, RESOURCES_KEY, RENAME_KEY, ERRORS_KEY);
    private static final Set<String> SHAPE_TYPES = new HashSet<>();

    static {
        for (ShapeType type : ShapeType.values()) {
            if (type != ShapeType.MEMBER) {
                SHAPE_TYPES.add(type.toString());
            }
        }
    }

    private final String filename;
    private final IdlTokenizer tokenizer;
    private final Map<String, ShapeId> useShapes = new HashMap<>();
    private final IdlReferenceResolver resolver;
    private Consumer<LoadOperation> operations;
    private Version modelVersion = Version.VERSION_1_0;
    private String namespace;
    private boolean emittedVersion = false;

    private String operationInputSuffix = "Input";
    private String operationOutputSuffix = "Output";

    IdlModelLoader(String filename, CharSequence model, Function<CharSequence, String> stringTable) {
        this.filename = filename;
        this.tokenizer = IdlTokenizer.builder()
                .filename(filename)
                .model(model)
                .stringTable(stringTable)
                .validationEventListener(this::emit)
                .build();
        this.resolver = this::addForwardReference;
    }

    void parse(Consumer<LoadOperation> operationConsumer) {
        operations = operationConsumer;
        tokenizer.skipWsAndDocs();
        parseControlSection();

        // Emit a version from the current location if the assumed 1.0 is used.
        if (!emittedVersion) {
            addOperation(new LoadOperation.ModelVersion(modelVersion, tokenizer.getCurrentTokenLocation()));
        }

        tokenizer.skipWsAndDocs();
        parseMetadataSection();
        parseShapeSection();
    }

    void emit(ValidationEvent event) {
        addOperation(new LoadOperation.Event(event));
    }

    void addOperation(LoadOperation operation) {
        operations.accept(operation);
    }

    public ModelSyntaxException syntax(String message) {
        return syntax(null, message);
    }

    ModelSyntaxException syntax(ShapeId shapeId, String message) {
        return ModelSyntaxException.builder()
                .message(format("Syntax error at line %d, column %d: %s",
                                tokenizer.getCurrentTokenLine(), tokenizer.getCurrentTokenColumn(), message))
                .sourceLocation(tokenizer.getCurrentTokenLocation())
                .shapeId(shapeId)
                .build();
    }

    void addForwardReference(String id, BiFunction<ShapeId, ShapeType, ValidationEvent> receiver) {
        int memberPosition = id.indexOf('$');

        // Check for members by removing the member and checking for the root shape.
        if (memberPosition > 0 && memberPosition < id.length() - 1) {
            addForwardReference(
                id.substring(0, memberPosition),
                (resolved, type) -> receiver.apply(resolved.withMember(id.substring(memberPosition + 1)), type)
            );
        } else {
            String resolved = useShapes.containsKey(id) ? useShapes.get(id).toString() : id;
            addOperation(new LoadOperation.ForwardReference(namespace, resolved, receiver));
        }
    }

    void addForwardReference(String id, Consumer<ShapeId> consumer) {
        addForwardReference(id, (resolved, found) -> {
            consumer.accept(resolved);
            return null;
        });
    }

    String expectNamespace() {
        if (namespace == null) {
            throw new IllegalStateException("No namespace was set before trying to resolve a forward reference");
        }
        return namespace;
    }

    /**
     * Adds a trait to a shape after resolving all shape IDs.
     *
     * <p>Resolving the trait against a trait definition is deferred until
     * the entire model is loaded. A namespace is required to have been set
     * if the trait name is not absolute.
     *
     * @param target Shape to add the trait to.
     * @param traitName Trait name to add.
     * @param traitValue Trait value as a Node object.
     * @param isAnnotation Set to true to indicate that the value for the trait was omitted.
     */
    private void onDeferredTrait(ShapeId target, String traitName, Node traitValue, boolean isAnnotation) {
        addForwardReference(traitName, (traitId, type) -> {
            Node coerced = coerceTraitValue(traitValue, isAnnotation, type);
            addOperation(new LoadOperation.ApplyTrait(
                modelVersion,
                traitValue.getSourceLocation(),
                expectNamespace(),
                target,
                traitId,
                coerced
            ));
            return null;
        });
    }

    private Node coerceTraitValue(Node value, boolean isAnnotation, ShapeType targetType) {
        if (isAnnotation && value.isNullNode()) {
            if (targetType == null || targetType == ShapeType.STRUCTURE || targetType == ShapeType.MAP) {
                // The targetType == null condition helps mitigate a confusing
                // failure mode where a trait isn't defined in the model, but a
                // TraitService is found as a service provider for the trait.
                // If the TraitService creates an annotation trait, then using null
                // instead of object results in a failure about passing null for an
                // annotation trait, and that's confusing because the actual error
                // message should be about the missing trait definition. Because the
                // vast majority of annotation traits are modeled as objects, this
                // makes the assumption that the value is an object (which addresses
                // the above failure case).
                return new ObjectNode(Collections.emptyMap(), value.getSourceLocation());
            } else if (targetType == ShapeType.LIST || targetType == ShapeType.SET) {
                return new ArrayNode(Collections.emptyList(), value.getSourceLocation());
            }
        }

        return value;
    }

    private void parseControlSection() {
        Set<CharSequence> definedKeys = new HashSet<>();

        while (tokenizer.getCurrentToken() == IdlToken.DOLLAR) {
            try {
                tokenizer.next();
                tokenizer.expect(IdlToken.IDENTIFIER, IdlToken.STRING);
                String key = tokenizer.internString(tokenizer.getCurrentTokenStringSlice());

                tokenizer.next();
                tokenizer.skipSpaces();
                tokenizer.expect(IdlToken.COLON);
                tokenizer.next();
                tokenizer.skipSpaces();

                if (!definedKeys.add(key)) {
                    throw syntax(format("Duplicate control statement `%s`", key));
                }

                Node value = IdlNodeParser.expectAndSkipNode(tokenizer, resolver);

                switch (key) {
                    case "version":
                        onVersion(value);
                        break;
                    case "operationInputSuffix":
                        operationInputSuffix = value.expectStringNode().getValue();
                        break;
                    case "operationOutputSuffix":
                        operationOutputSuffix = value.expectStringNode().getValue();
                        break;
                    default:
                        emit(ValidationEvent.builder()
                                     .id(Validator.MODEL_ERROR)
                                     .sourceLocation(value)
                                     .severity(Severity.WARNING)
                                     .message(format("Unknown control statement `%s` with value `%s",
                                                     key, Node.printJson(value)))
                                     .build());
                        break;
                }

                tokenizer.expectAndSkipBr();
            } catch (ModelSyntaxException e) {
                errorRecovery(e);
            }
        }
    }

    private void onVersion(Node value) {
        if (!value.isStringNode()) {
            value.expectStringNode(() -> "The $version control statement must have a string value, but found "
                                         + Node.printJson(value));
        }

        String parsedVersion = value.expectStringNode().getValue();
        Version resolvedVersion = Version.fromString(parsedVersion);

        if (resolvedVersion == null) {
            throw syntax("Unsupported Smithy version number: " + parsedVersion);
        }

        emittedVersion = true;
        modelVersion = resolvedVersion;
        addOperation(new LoadOperation.ModelVersion(modelVersion, value.getSourceLocation()));
    }

    private void parseMetadataSection() {
        while (tokenizer.doesCurrentIdentifierStartWith('m')) {
            try {
                tokenizer.expectCurrentLexeme("metadata");
                tokenizer.next(); // skip "metadata"
                tokenizer.expectAndSkipSpaces();
                tokenizer.expect(IdlToken.IDENTIFIER, IdlToken.STRING);
                String key = tokenizer.internString(tokenizer.getCurrentTokenStringSlice());
                tokenizer.next();
                tokenizer.skipSpaces();
                tokenizer.expect(IdlToken.EQUAL);
                tokenizer.next();
                tokenizer.skipSpaces();
                Node value = IdlNodeParser.expectAndSkipNode(tokenizer, resolver);
                operations.accept(new LoadOperation.PutMetadata(modelVersion, key, value));
                tokenizer.expectAndSkipBr();
            } catch (ModelSyntaxException e) {
                errorRecovery(e);
            }
        }
    }

    private void parseShapeSection() {
        if (tokenizer.doesCurrentIdentifierStartWith('n')) {
            tokenizer.expectCurrentLexeme("namespace");
            tokenizer.next(); // skip "namespace"
            tokenizer.expectAndSkipSpaces();

            // Parse the namespace.
            namespace = tokenizer.internString(IdlShapeIdParser.expectAndSkipShapeIdNamespace(tokenizer));
            tokenizer.expectAndSkipBr();

            // An unfortunate side effect of allowing insignificant documentation comments:
            // Keep track of a potential documentation comment location because we need to skip doc comments to parse
            // a potential `use statement`. If a `use` statement is present, captured documentation comments are
            // correctly cleared from the lexer. If not found, the captured comments are applied to the first shape.
            SourceLocation possibleDocCommentLocation = tokenizer.getCurrentTokenLocation();
            tokenizer.skipWsAndDocs();
            parseUseSection();
            parseFirstShapeStatement(possibleDocCommentLocation);
            parseSubsequentShapeStatements();
        } else if (tokenizer.hasNext()) {
            throw syntax("Expected a namespace definition but found "
                         + tokenizer.getCurrentToken().getDebug(tokenizer.getCurrentTokenLexeme()));
        }
    }

    private void parseUseSection() {
        while (tokenizer.getCurrentToken() == IdlToken.IDENTIFIER) {
            // Don't over-parse here for unions.
            String keyword = tokenizer.internString(tokenizer.getCurrentTokenLexeme());
            if (!keyword.equals("use")) {
                break;
            }

            tokenizer.next(); // skip "use"
            tokenizer.expectAndSkipSpaces();

            SourceLocation idLocation = tokenizer.getCurrentTokenLocation();
            String idString = tokenizer.internString(IdlShapeIdParser.expectAndSkipAbsoluteShapeId(tokenizer));
            ShapeId id = ShapeId.from(idString);

            if (id.hasMember()) {
                throw new ModelSyntaxException("Use statements cannot use members", idLocation);
            }

            if (useShapes.containsKey(id.getName())) {
                ShapeId previous = useShapes.get(id.getName());
                String message = String.format("Cannot use name `%s` because it conflicts with `%s`", id, previous);
                throw new ModelSyntaxException(message, idLocation);
            } else {
                useShapes.put(id.getName(), id);
            }

            // Validate use statements when the model is fully loaded.
            addForwardReference(idString, (resolved, type) -> {
                if (type != null) {
                    return null;
                } else {
                    return ValidationEvent.builder()
                            .id(Validator.MODEL_ERROR)
                            .severity(Severity.WARNING)
                            .sourceLocation(idLocation)
                            .message("Use statement refers to undefined shape: " + id)
                            .build();
                }
            });

            tokenizer.expectAndSkipBr();
            tokenizer.skipWsAndDocs();
        }
    }

    private void parseApplyStatement(List<IdlTraitParser.Result> traits) {
        // Detect if doc comments were found before the apply statement, and if so warn.
        // If traits were attached to an apply statement then fail.
        SourceLocation foundDocComments = null;

        for (IdlTraitParser.Result trait : traits) {
            if (trait.getTraitType() != IdlTraitParser.TraitType.DOC_COMMENT) {
                throw syntax("Traits applied to apply statement");
            } else {
                foundDocComments = trait.getValue().getSourceLocation();
            }
        }

        if (foundDocComments != null) {
            LoaderUtils.emitBadDocComment(foundDocComments, null);
        }

        tokenizer.expectCurrentLexeme("apply");
        tokenizer.next();
        tokenizer.expectAndSkipWhitespace();
        String target = tokenizer.internString(IdlShapeIdParser.expectAndSkipShapeId(tokenizer));
        tokenizer.expectAndSkipWhitespace();
        List<IdlTraitParser.Result> traitsToApply;

        if (IdlToken.AT == tokenizer.expect(IdlToken.AT, IdlToken.LBRACE)) {
            // Parse a single trait.
            traitsToApply = Collections.singletonList(IdlTraitParser.expectAndSkipTrait(tokenizer, resolver));
        } else {
            // Parse a trait block.
            tokenizer.next(); // skip opening LBRACE.
            tokenizer.skipWsAndDocs();
            traitsToApply = IdlTraitParser.expectAndSkipTraits(tokenizer, resolver);
            tokenizer.skipWsAndDocs();
            tokenizer.expect(IdlToken.RBRACE);
            tokenizer.next();
        }

        // First, resolve the targeted shape.
        addForwardReference(target, id -> {
            for (IdlTraitParser.Result trait : traitsToApply) {
                String traitNameString = tokenizer.internString(trait.getTraitName());
                onDeferredTrait(id, traitNameString, trait.getValue(),
                                trait.getTraitType() == IdlTraitParser.TraitType.ANNOTATION);
            }
        });

        tokenizer.expectAndSkipBr();
    }

    private void parseFirstShapeStatement(SourceLocation possibleDocCommentLocation) {
        if (tokenizer.getCurrentToken() != IdlToken.EOF) {
            try {
                tokenizer.skipWsAndDocs();
                String docLines = tokenizer.removePendingDocCommentLines();
                List<IdlTraitParser.Result> traits = IdlTraitParser.parseDocsAndTraitsBeforeShape(tokenizer, resolver);
                if (docLines != null) {
                    traits.add(new IdlTraitParser.Result(DocumentationTrait.ID.toString(),
                                                         new StringNode(docLines, possibleDocCommentLocation),
                                                         IdlTraitParser.TraitType.DOC_COMMENT));
                }
                if (parseShapeDefinition(traits, docLines != null)) {
                    parseShapeOrApply(traits);
                }
            } catch (ModelSyntaxException e) {
                errorRecovery(e);
            }
        }
    }

    private void parseSubsequentShapeStatements() {
        while (tokenizer.hasNext()) {
            try {
                boolean hasDocComment = tokenizer.getCurrentToken() == IdlToken.DOC_COMMENT;
                List<IdlTraitParser.Result> traits = IdlTraitParser.parseDocsAndTraitsBeforeShape(tokenizer, resolver);
                if (parseShapeDefinition(traits, hasDocComment)) {
                    parseShapeOrApply(traits);
                }
            } catch (ModelSyntaxException e) {
                errorRecovery(e);
            }
        }
    }

    private void errorRecovery(ModelSyntaxException e) {
        if (!tokenizer.hasNext()) {
            throw e;
        }

        // Here we do rudimentary error recovery to attempt to make sense of the remaining model.
        // We do this by scanning tokens until we find the next "$", identifier, docs, or trait at the start of a line.
        // The model is still invalid and will fail to validate, but things like IDEs should still be able to do
        // things like goto definition.
        emit(ValidationEvent.fromSourceException(e));

        do {
            // Always skip the current token since it was the one that failed.
            IdlToken token = tokenizer.next();
            if (tokenizer.getCurrentTokenColumn() == 1 && isErrorRecoveryToken(token)) {
                break;
            }
        } while (tokenizer.hasNext());
    }

    // These tokens are good signals that the next shape is starting.
    private boolean isErrorRecoveryToken(IdlToken token) {
        switch (token) {
            case IDENTIFIER:
            case DOC_COMMENT:
            case AT:
            case DOLLAR:
                return true;
            default:
                return false;
        }
    }

    private boolean parseShapeDefinition(List<IdlTraitParser.Result> traits, boolean hasDocComment) {
        if (tokenizer.getCurrentToken() != IdlToken.EOF) {
            // Continue to parse if not at the end of the file.
            return true;
        } else if (hasDocComment) {
            // When hasDocComment is true and the number of traits is 1, then the only trait is a documentation trait
            // created from parsing "///". In this case, warn that a dangling documentation comment was detected
            // but don't fail.
            if (traits.size() == 1) {
                emit(LoaderUtils.emitBadDocComment(tokenizer.getCurrentTokenLocation(),
                                                   traits.get(0).getValue().expectStringNode().getValue()));
                return false;
            } else {
                // If more than 1 trait is present when hasDocComment is true, then other traits were defined, and
                // we do want to fail.
                return true;
            }
        } else {
            // Fail because there are traits, it's not just a documentation comment, and there's no more IDL to parse.
            return !traits.isEmpty();
        }
    }

    private void parseShapeOrApply(List<IdlTraitParser.Result> traits) {
        SourceLocation location = tokenizer.getCurrentTokenLocation();
        tokenizer.expect(IdlToken.IDENTIFIER);
        String shapeType = tokenizer.internString(tokenizer.getCurrentTokenLexeme());

        if (shapeType.equals("apply")) {
            parseApplyStatement(traits);
            return;
        }

        ShapeType type = ShapeType.fromString(shapeType)
                .orElseThrow(() -> syntax("Unknown shape type: " + shapeType));
        tokenizer.next();
        tokenizer.expectAndSkipSpaces();
        ShapeId id = parseShapeName();

        switch (type) {
            case STRING:
            case BLOB:
            case BOOLEAN:
            case DOCUMENT:
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BIG_DECIMAL:
            case BIG_INTEGER:
            case TIMESTAMP:
                parseSimpleShape(id, location, type.createBuilderForType());
                break;
            case LIST:
                parseCollection(id, location, ListShape.builder());
                break;
            case SET:
                parseCollection(id, location, SetShape.builder());
                break;
            case MAP:
                parseMapStatement(id, location);
                break;
            case ENUM:
                parseEnumShape(id, location, EnumShape.builder());
                break;
            case INT_ENUM:
                parseEnumShape(id, location, IntEnumShape.builder());
                break;
            case STRUCTURE:
                parseStructuredShape(id, location, StructureShape.builder(), MemberParsing.PARSING_STRUCTURE_MEMBER);
                break;
            case UNION:
                parseStructuredShape(id, location, UnionShape.builder(), MemberParsing.PARSING_MEMBER);
                break;
            case SERVICE:
                parseServiceStatement(id, location);
                break;
            case RESOURCE:
                parseResourceStatement(id, location);
                break;
            case OPERATION:
                parseOperationStatement(id, location);
                break;
            default:
                throw syntax("Shape type unknown: " + shapeType);
        }

        addTraits(id, traits);
        tokenizer.expectAndSkipBr();
    }

    private void addTraits(ShapeId id, List<IdlTraitParser.Result> traits) {
        for (IdlTraitParser.Result result : traits) {
            String traitName = tokenizer.internString(result.getTraitName());
            onDeferredTrait(id, traitName, result.getValue(),
                            result.getTraitType() == IdlTraitParser.TraitType.ANNOTATION);
        }
    }

    private ShapeId parseShapeName() {
        int line = tokenizer.getCurrentTokenLine();
        int column = tokenizer.getCurrentTokenColumn();
        tokenizer.expect(IdlToken.IDENTIFIER);
        String name = tokenizer.internString(tokenizer.getCurrentTokenStringSlice());
        ShapeId id = ShapeId.fromRelative(expectNamespace(), name);
        if (useShapes.containsKey(name)) {
            ShapeId previous = useShapes.get(name);
            String message = String.format("Shape name `%s` conflicts with imported shape `%s`", name, previous);
            throw new ModelSyntaxException(message, filename, line, column);
        }
        tokenizer.next();
        return id;
    }

    private void parseSimpleShape(ShapeId id, SourceLocation location, AbstractShapeBuilder<?, ?> builder) {
        LoadOperation.DefineShape operation = createShape(builder.source(location).id(id));
        parseMixins(operation);
        addOperation(operation);
    }

    LoadOperation.DefineShape createShape(AbstractShapeBuilder<?, ?> builder) {
        return new LoadOperation.DefineShape(modelVersion, builder);
    }

    private void parseMixins(LoadOperation.DefineShape operation) {
        tokenizer.skipSpaces();

        if (!tokenizer.doesCurrentIdentifierStartWith('w')) {
            return;
        }

        tokenizer.expect(IdlToken.IDENTIFIER);
        tokenizer.expectCurrentLexeme("with");

        if (!modelVersion.supportsMixins()) {
            throw syntax(operation.toShapeId(), "Mixins can only be used with Smithy version 2 or later. "
                                                + "Attempted to use mixins with version `" + modelVersion + "`.");
        }

        tokenizer.next();
        tokenizer.skipWsAndDocs();
        tokenizer.expect(IdlToken.LBRACKET);
        tokenizer.next();
        tokenizer.skipWsAndDocs();

        do {
            String target = tokenizer.internString(IdlShapeIdParser.expectAndSkipShapeId(tokenizer));
            addForwardReference(target, resolved -> {
                operation.addDependency(resolved);
                operation.addModifier(new ApplyMixin(resolved));
            });
            tokenizer.skipWsAndDocs();;
        } while (tokenizer.getCurrentToken() != IdlToken.RBRACKET);

        tokenizer.expect(IdlToken.RBRACKET);
        tokenizer.next();
    }

    // See parseMap for information on why members are parsed before the list/set is registered with the ModelFile.
    private void parseCollection(ShapeId id, SourceLocation location, CollectionShape.Builder<?, ?> builder) {
        LoadOperation.DefineShape operation = createShape(builder.id(id).source(location));
        parseMixins(operation);
        tokenizer.skipWsAndDocs();
        tokenizer.expect(IdlToken.LBRACE);
        tokenizer.next();
        tokenizer.skipWs();
        parsePossiblyElidedMember(operation, "member");
        tokenizer.skipWsAndDocs();
        tokenizer.expect(IdlToken.RBRACE);
        tokenizer.next();
        operations.accept(operation);
    }

    // Parsed list, set, and map members.
    private void parsePossiblyElidedMember(LoadOperation.DefineShape operation, String memberName) {
        boolean isElided = false;
        List<IdlTraitParser.Result> memberTraits = IdlTraitParser.parseDocsAndTraitsBeforeShape(tokenizer, resolver);
        SourceLocation location = tokenizer.getCurrentTokenLocation();

        if (tokenizer.getCurrentToken() == IdlToken.DOLLAR) {
            isElided = true;
            if (!modelVersion.supportsTargetElision()) {
                throw syntax(operation.toShapeId().withMember(memberName),
                             "Members can only elide targets in IDL version 2 or later");
            }
            tokenizer.next();
            tokenizer.expect(IdlToken.IDENTIFIER);
        } else {
            if (!tokenizer.doesCurrentIdentifierStartWith(memberName.charAt(0))) {
                if (!memberTraits.isEmpty()) {
                    throw syntax("Expected member definition to follow traits");
                }
                return;
            }
        }

        MemberShape.Builder memberBuilder = MemberShape.builder()
                .id(operation.toShapeId().withMember(memberName))
                .source(location);

        tokenizer.expectCurrentLexeme(memberName);
        tokenizer.next();

        if (!isElided) {
            tokenizer.skipWsAndDocs();
            tokenizer.expect(IdlToken.COLON);
            tokenizer.next();
            tokenizer.skipWsAndDocs();
            String id = tokenizer.internString(IdlShapeIdParser.expectAndSkipShapeId(tokenizer));
            addForwardReference(id, memberBuilder::target);
        }

        operation.addMember(memberBuilder);
        addTraits(memberBuilder.getId(), memberTraits);
    }

    private void parseEnumShape(ShapeId id, SourceLocation location, AbstractShapeBuilder<?, ?> builder) {
        LoadOperation.DefineShape operation = createShape(builder.id(id).source(location));
        parseMixins(operation);
        tokenizer.skipWsAndDocs();
        tokenizer.expect(IdlToken.LBRACE);
        tokenizer.next();
        tokenizer.skipWs();

        while (tokenizer.getCurrentToken() != IdlToken.EOF && tokenizer.getCurrentToken() != IdlToken.RBRACE) {
            List<IdlTraitParser.Result> memberTraits = IdlTraitParser
                    .parseDocsAndTraitsBeforeShape(tokenizer, resolver);
            SourceLocation memberLocation = tokenizer.getCurrentTokenLocation();
            tokenizer.expect(IdlToken.IDENTIFIER);
            String memberName = tokenizer.internString(tokenizer.getCurrentTokenLexeme());

            MemberShape.Builder memberBuilder = MemberShape.builder()
                    .id(id.withMember(memberName))
                    .source(memberLocation)
                    .target(UnitTypeTrait.UNIT);
            operation.addMember(memberBuilder);
            addTraits(memberBuilder.getId(), memberTraits);

            // Check for optional value assignment.
            tokenizer.next();
            tokenizer.skipSpaces();

            if (tokenizer.getCurrentToken() == IdlToken.EQUAL) {
                tokenizer.next();
                tokenizer.skipSpaces();
                Node value = IdlNodeParser.expectAndSkipNode(tokenizer, resolver);
                memberBuilder.addTrait(new EnumValueTrait.Provider().createTrait(memberBuilder.getId(), value));
                tokenizer.expectAndSkipBr();
            } else {
                tokenizer.skipWs();
            }
        }

        tokenizer.expect(IdlToken.RBRACE);
        tokenizer.next();
        operations.accept(operation);
    }

    private void parseMapStatement(ShapeId id, SourceLocation location) {
        LoadOperation.DefineShape operation = createShape(MapShape.builder().id(id).source(location));
        parseMixins(operation);
        tokenizer.skipWsAndDocs();
        tokenizer.expect(IdlToken.LBRACE);
        tokenizer.next();
        tokenizer.skipWs();
        parsePossiblyElidedMember(operation, "key");
        tokenizer.skipWs();
        parsePossiblyElidedMember(operation, "value");
        tokenizer.skipWsAndDocs();
        tokenizer.expect(IdlToken.RBRACE);
        tokenizer.next();
        operations.accept(operation);
    }

    private void parseStructuredShape(
            ShapeId id,
            SourceLocation location,
            AbstractShapeBuilder<?, ?> builder,
            MemberParsing memberParsing
    ) {
        LoadOperation.DefineShape operation = createShape(builder.id(id).source(location));

        // If it's a structure, parse the optional "from" statement to enable
        // eliding member targets for resource identifiers.
        if (builder.getShapeType() == ShapeType.STRUCTURE) {
            parseForResource(operation);
        }

        // Parse optional "with" statements to add mixins, but only if it's supported by the version.
        parseMixins(operation);
        parseMembers(operation, memberParsing);
        operations.accept(operation);
    }

    private enum MemberParsing {
        PARSING_STRUCTURE_MEMBER {
            @Override
            boolean supportsAssignment() {
                return true;
            }

            @Override
            Trait createAssignmentTrait(ShapeId id, Node value) {
                return new DefaultTrait(value);
            }
        },
        PARSING_MEMBER {
            @Override
            boolean supportsAssignment() {
                return false;
            }

            @Override
            Trait createAssignmentTrait(ShapeId id, Node value) {
                throw new UnsupportedOperationException();
            }
        };

        abstract boolean supportsAssignment();

        abstract Trait createAssignmentTrait(ShapeId id, Node value);
    }

    private void parseMembers(LoadOperation.DefineShape op, MemberParsing memberParsing) {
        Set<String> definedMembers = new HashSet<>();

        tokenizer.skipWsAndDocs();
        tokenizer.expect(IdlToken.LBRACE);
        tokenizer.next();
        tokenizer.skipWs();

        while (tokenizer.hasNext() && tokenizer.getCurrentToken() != IdlToken.RBRACE) {
            parseMember(op, definedMembers, memberParsing);
            tokenizer.skipWs();
        }

        tokenizer.expect(IdlToken.RBRACE);
        tokenizer.next();
    }

    private void parseMember(LoadOperation.DefineShape operation, Set<String> defined, MemberParsing memberParsing) {
        ShapeId parent = operation.toShapeId();

        // Parse optional member traits.
        List<IdlTraitParser.Result> memberTraits = IdlTraitParser.parseDocsAndTraitsBeforeShape(tokenizer, resolver);
        SourceLocation memberLocation = tokenizer.getCurrentTokenLocation();

        // Handle the case of a dangling documentation comment followed by "}".
        if (tokenizer.getCurrentToken() == IdlToken.RBRACE) {
            if (memberTraits.size() == 1
                    && memberTraits.get(0).getTraitType() == IdlTraitParser.TraitType.DOC_COMMENT) {
                return;
            }
        }

        boolean isTargetElided = tokenizer.getCurrentToken() == IdlToken.DOLLAR;
        if (isTargetElided) {
            tokenizer.expect(IdlToken.DOLLAR);
            tokenizer.next();
        }

        tokenizer.expect(IdlToken.IDENTIFIER);
        String memberName = tokenizer.internString(tokenizer.getCurrentTokenLexeme());

        if (defined.contains(memberName)) {
            // This is a duplicate member name.
            throw syntax(parent, "Duplicate member of `" + parent + "`: '" + memberName + '\'');
        }

        defined.add(memberName);

        ShapeId memberId = parent.withMember(memberName);

        if (isTargetElided && !modelVersion.supportsTargetElision()) {
            throw syntax(memberId, "Members can only elide targets in IDL version 2 or later");
        }

        MemberShape.Builder memberBuilder = MemberShape.builder().id(memberId).source(memberLocation);

        tokenizer.next(); // skip past the identifier.

        // Members whose targets are elided will have those targets resolved later e.g. by SetResourceBasedTargets
        if (!isTargetElided) {
            tokenizer.skipSpaces();
            tokenizer.expect(IdlToken.COLON);
            tokenizer.next();
            tokenizer.skipSpaces();
            String target = tokenizer.internString(IdlShapeIdParser.expectAndSkipShapeId(tokenizer));
            addForwardReference(target, memberBuilder::target);
        }

        // Skip spaces to check if there is default trait sugar.
        tokenizer.skipSpaces();

        if (memberParsing.supportsAssignment() && tokenizer.getCurrentToken() == IdlToken.EQUAL) {
            if (!modelVersion.isDefaultSupported()) {
                throw syntax("@default assignment is only supported in IDL version 2 or later");
            }
            tokenizer.expect(IdlToken.EQUAL);
            tokenizer.next();
            tokenizer.skipSpaces();
            Node node = IdlNodeParser.expectAndSkipNode(tokenizer, resolver);
            memberBuilder.addTrait(memberParsing.createAssignmentTrait(memberId, node));
            tokenizer.expectAndSkipBr();
        }

        // Only add the member once fully parsed.
        operation.addMember(memberBuilder);
        addTraits(memberBuilder.getId(), memberTraits);
    }

    private void parseForResource(LoadOperation.DefineShape operation) {
        tokenizer.skipSpaces();

        if (!tokenizer.doesCurrentIdentifierStartWith('f')) {
            return;
        }

        tokenizer.expectCurrentLexeme("for");

        if (!modelVersion.supportsTargetElision()) {
            throw syntax(operation.toShapeId(), "Structures can only be bound to resources with Smithy version 2 or "
                                                + "later. Attempted to bind a structure to a resource with version `"
                                                + modelVersion + "`.");
        }

        tokenizer.next();
        tokenizer.expectAndSkipSpaces();

        String forTarget = tokenizer.internString(IdlShapeIdParser.expectAndSkipShapeId(tokenizer));
        addForwardReference(forTarget, shapeId -> {
            operation.addDependency(shapeId);
            operation.addModifier(new ApplyResourceBasedTargets(shapeId));
        });
    }

    private void parseServiceStatement(ShapeId id, SourceLocation location) {
        ServiceShape.Builder builder = new ServiceShape.Builder().id(id).source(location);
        LoadOperation.DefineShape operation = createShape(builder);
        parseMixins(operation);
        tokenizer.skipWsAndDocs();
        tokenizer.expect(IdlToken.LBRACE);
        ObjectNode shapeNode = IdlNodeParser.expectAndSkipNode(tokenizer, resolver).expectObjectNode();
        LoaderUtils.checkForAdditionalProperties(shapeNode, id, SERVICE_PROPERTY_NAMES).ifPresent(this::emit);
        shapeNode.getStringMember(VERSION_KEY).map(StringNode::getValue).ifPresent(builder::version);
        optionalIdList(shapeNode, OPERATIONS_KEY, builder::addOperation);
        optionalIdList(shapeNode, RESOURCES_KEY, builder::addResource);
        optionalIdList(shapeNode, ERRORS_KEY, builder::addError);
        AstModelLoader.loadServiceRenameIntoBuilder(builder, shapeNode);
        operations.accept(operation);
    }

    private void optionalId(ObjectNode node, String name, Consumer<ShapeId> consumer) {
        if (node.getMember(name).isPresent()) {
            addForwardReference(node.expectStringMember(name).getValue(), consumer);
        }
    }

    private void optionalIdList(ObjectNode node, String name, Consumer<ShapeId> consumer) {
        if (node.getMember(name).isPresent()) {
            ArrayNode value = node.expectArrayMember(name);
            for (StringNode element : value.getElementsAs(StringNode.class)) {
                addForwardReference(element.getValue(), consumer);
            }
        }
    }

    private void parseResourceStatement(ShapeId id, SourceLocation location) {
        ResourceShape.Builder builder = ResourceShape.builder().id(id).source(location);
        LoadOperation.DefineShape operation = createShape(builder);

        parseMixins(operation);
        tokenizer.skipWsAndDocs();
        tokenizer.expect(IdlToken.LBRACE);
        ObjectNode shapeNode = IdlNodeParser.expectAndSkipNode(tokenizer, resolver).expectObjectNode();

        LoaderUtils.checkForAdditionalProperties(shapeNode, id, RESOURCE_PROPERTY_NAMES).ifPresent(this::emit);
        optionalId(shapeNode, PUT_KEY, builder::put);
        optionalId(shapeNode, CREATE_KEY, builder::create);
        optionalId(shapeNode, READ_KEY, builder::read);
        optionalId(shapeNode, UPDATE_KEY, builder::update);
        optionalId(shapeNode, DELETE_KEY, builder::delete);
        optionalId(shapeNode, LIST_KEY, builder::list);
        optionalIdList(shapeNode, OPERATIONS_KEY, builder::addOperation);
        optionalIdList(shapeNode, RESOURCES_KEY, builder::addResource);
        optionalIdList(shapeNode, COLLECTION_OPERATIONS_KEY, builder::addCollectionOperation);

        // Load identifiers and resolve forward references.
        shapeNode.getObjectMember(IDENTIFIERS_KEY).ifPresent(ids -> {
            for (Map.Entry<StringNode, Node> entry : ids.getMembers().entrySet()) {
                String name = entry.getKey().getValue();
                StringNode target = entry.getValue().expectStringNode();
                addForwardReference(target.getValue(), targetId -> builder.addIdentifier(name, targetId));
            }
        });
        // Load properties and resolve forward references.
        shapeNode.getObjectMember(PROPERTIES_KEY).ifPresent(properties -> {
            if (!modelVersion.supportsResourceProperties()) {
                throw syntax(id, "Resource properties can only be used with Smithy version 2 or later. "
                                 + "Attempted to use resource properties with version `" + modelVersion + "`.");
            }
            for (Map.Entry<StringNode, Node> entry : properties.getMembers().entrySet()) {
                String name = entry.getKey().getValue();
                StringNode target = entry.getValue().expectStringNode();
                addForwardReference(target.getValue(), targetId -> builder.addProperty(name, targetId));
            }
        });
        operations.accept(operation);
    }

    private void parseOperationStatement(ShapeId id, SourceLocation location) {
        OperationShape.Builder builder = OperationShape.builder().id(id).source(location);
        LoadOperation.DefineShape operation = createShape(builder);
        parseMixins(operation);
        tokenizer.skipWsAndDocs();
        tokenizer.expect(IdlToken.LBRACE);
        tokenizer.next();
        tokenizer.skipWsAndDocs();

        Set<String> defined = new HashSet<>();
        while (tokenizer.hasNext() && tokenizer.getCurrentToken() != IdlToken.RBRACE) {
            tokenizer.expect(IdlToken.IDENTIFIER);
            String key = tokenizer.internString(tokenizer.getCurrentTokenLexeme());
            if (!defined.add(key)) {
                throw syntax(id, String.format("Duplicate operation %s property for `%s`", key, id));
            }

            tokenizer.next();
            tokenizer.skipWsAndDocs();

            switch (key) {
                case "input":
                    IdlToken nextInput = tokenizer.expect(IdlToken.COLON, IdlToken.WALRUS);
                    tokenizer.next();
                    IdlTraitParser.Result inputTrait = new IdlTraitParser.Result(
                            InputTrait.ID.toString(), Node.objectNode(), IdlTraitParser.TraitType.ANNOTATION);
                    parseInlineableOperationMember(id, nextInput, operationInputSuffix, builder::input, inputTrait);
                    break;
                case "output":
                    IdlToken nextOutput = tokenizer.expect(IdlToken.COLON, IdlToken.WALRUS);
                    tokenizer.next();
                    IdlTraitParser.Result outputTrait = new IdlTraitParser.Result(
                            OutputTrait.ID.toString(), Node.objectNode(), IdlTraitParser.TraitType.ANNOTATION);
                    parseInlineableOperationMember(id, nextOutput, operationOutputSuffix, builder::output, outputTrait);
                    break;
                case "errors":
                    tokenizer.expect(IdlToken.COLON);
                    tokenizer.next();
                    parseIdList(builder::addError);
                    break;
                default:
                    throw syntax(id, String.format("Unknown property %s for %s", key, id));
            }
            tokenizer.expectAndSkipWhitespace();
            tokenizer.skipWsAndDocs();
        }

        tokenizer.expect(IdlToken.RBRACE);
        tokenizer.next();
        operations.accept(operation);
    }

    private void parseInlineableOperationMember(
            ShapeId id,
            IdlToken token,
            String suffix,
            Consumer<ShapeId> consumer,
            IdlTraitParser.Result defaultTrait
    ) {
        if (token == IdlToken.WALRUS) {
            if (!modelVersion.supportsInlineOperationIO()) {
                throw syntax(id, "Inlined operation inputs and outputs can only be used with Smithy version 2 or "
                                 + "later. Attempted to use inlined IO with version `" + modelVersion + "`.");
            }
            // Remove any pending, invalid docs that may have come before the inline shape.
            tokenizer.removePendingDocCommentLines();
            tokenizer.next();
            // don't skip docs here in case there are docs on the inlined structure.
            tokenizer.skipWs();
            consumer.accept(parseInlineStructure(id.getName() + suffix, defaultTrait));
        } else {
            tokenizer.skipWsAndDocs();
            String target = tokenizer.internString(IdlShapeIdParser.expectAndSkipShapeId(tokenizer));
            addForwardReference(target, consumer);
        }
    }

    private ShapeId parseInlineStructure(String name, IdlTraitParser.Result defaultTrait) {
        List<IdlTraitParser.Result> traits = IdlTraitParser.parseDocsAndTraitsBeforeShape(tokenizer, resolver);
        if (defaultTrait != null) {
            traits.add(defaultTrait);
        }
        ShapeId id = ShapeId.fromRelative(expectNamespace(), name);
        SourceLocation location = tokenizer.getCurrentTokenLocation();
        StructureShape.Builder builder = StructureShape.builder().id(id).source(location);
        LoadOperation.DefineShape operation = createShape(builder);
        parseMixins(operation);
        parseForResource(operation);
        parseMembers(operation, MemberParsing.PARSING_STRUCTURE_MEMBER);
        addTraits(id, traits);
        operations.accept(operation);
        return id;
    }

    private void parseIdList(Consumer<ShapeId> consumer) {
        tokenizer.increaseNestingLevel();
        tokenizer.skipWsAndDocs();
        tokenizer.expect(IdlToken.LBRACKET);
        tokenizer.next();
        tokenizer.skipWsAndDocs();

        while (tokenizer.hasNext() && tokenizer.getCurrentToken() != IdlToken.RBRACKET) {
            tokenizer.expect(IdlToken.IDENTIFIER);
            String target = tokenizer.internString(IdlShapeIdParser.expectAndSkipShapeId(tokenizer));
            addForwardReference(target, consumer);
            tokenizer.skipWsAndDocs();
        }

        tokenizer.expect(IdlToken.RBRACKET);
        tokenizer.next();
        tokenizer.decreaseNestingLevel();
    }
}
