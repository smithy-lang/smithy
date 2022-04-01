/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SimpleParser;
import software.amazon.smithy.utils.StringUtils;

final class IdlModelParser extends SimpleParser {

    /** Only allow nesting up to 250 arrays/objects in node values. */
    private static final int MAX_NESTING_LEVEL = 250;

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
    private static final Collection<String> OPERATION_PROPERTY_NAMES = ListUtils.of("input", "output", "errors");
    private static final Set<String> SHAPE_TYPES = new HashSet<>();

    static {
        for (ShapeType type : ShapeType.values()) {
            if (type != ShapeType.MEMBER) {
                SHAPE_TYPES.add(type.toString());
            }
        }
    }

    final ForwardReferenceModelFile modelFile;
    private final String filename;
    private TraitEntry pendingDocumentationComment;

    private String operationInputSuffix = "Input";
    private String operationOutputSuffix = "Output";

    // A pending trait that also doesn't yet have a resolved trait shape ID.
    static final class TraitEntry {
        final String traitName;
        final Node value;
        final boolean isAnnotation;

        TraitEntry(String traitName, Node value, boolean isAnnotation) {
            this.traitName = traitName;
            this.value = value;
            this.isAnnotation = isAnnotation;
        }
    }

    IdlModelParser(TraitFactory traitFactory, String filename, String model) {
        super(model, MAX_NESTING_LEVEL);
        this.filename = filename;
        this.modelFile = new ForwardReferenceModelFile(filename, traitFactory);
    }

    ModelFile parse() {
        ws();
        parseControlSection();
        parseMetadataSection();
        parseShapeSection();
        return modelFile;
    }

    /**
     * Overrides whitespace parsing to handle comments.
     */
    @Override
    public void ws() {
        while (!eof()) {
            switch (peek()) {
                case '/':
                    if (peekDocComment()) {
                        parseDocComment();
                    } else {
                        parseComment();
                    }
                    break;
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                case ',':
                    skip();
                    break;
                default:
                     return;
            }
        }
    }

    @Override
    public ModelSyntaxException syntax(String message) {
        return syntax(null, message);
    }

    ModelSyntaxException syntax(ShapeId shapeId, String message) {
        return ModelSyntaxException.builder()
                .message(format("Parse error at line %d, column %d near `%s`: %s",
                                line(), column(), peekDebugMessage(), message))
                .sourceLocation(filename, line(), column())
                .shapeId(shapeId)
                .build();
    }

    private void parseControlSection() {
        Set<String> definedKeys = new HashSet<>();
        while (peek() == '$') {
            expect('$');
            ws();
            String key = IdlNodeParser.parseNodeObjectKey(this);
            ws();
            expect(':');
            ws();

            if (definedKeys.contains(key)) {
                throw syntax(format("Duplicate control statement `%s`", key));
            }
            definedKeys.add(key);

            Node value = IdlNodeParser.parseNode(this);

            if (key.equals("version")) {
                onVersion(value);
            } else if (key.equals("operationInputSuffix")) {
                operationInputSuffix = value.expectStringNode().getValue();
            } else if (key.equals("operationOutputSuffix")) {
                operationOutputSuffix = value.expectStringNode().getValue();
            } else {
                modelFile.events().add(ValidationEvent.builder()
                        .id(Validator.MODEL_ERROR)
                        .sourceLocation(value)
                        .severity(Severity.WARNING)
                        .message(format("Unknown control statement `%s` with value `%s", key, Node.printJson(value)))
                        .build());
            }

            ws();
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

        modelFile.setVersion(resolvedVersion);
    }

    private void parseMetadataSection() {
        while (peek() == 'm') {
            expect('m');
            expect('e');
            expect('t');
            expect('a');
            expect('d');
            expect('a');
            expect('t');
            expect('a');
            ws();
            String key = IdlNodeParser.parseNodeObjectKey(this);
            ws();
            expect('=');
            ws();
            modelFile.putMetadata(key, IdlNodeParser.parseNode(this));
            ws();
        }
    }

    private void parseShapeSection() {
        if (peek() == 'n') {
            expect('n');
            expect('a');
            expect('m');
            expect('e');
            expect('s');
            expect('p');
            expect('a');
            expect('c');
            expect('e');
            ws();

            // Parse the namespace.
            int start = position();
            ParserUtils.consumeNamespace(this);
            modelFile.setNamespace(sliceFrom(start));
            // Clear out any erroneous documentation comments.
            clearPendingDocs();
            ws();

            parseUseSection();
            parseShapeStatements();
        } else if (!eof()) {
            if (!ParserUtils.isIdentifierStart(peek())) {
                throw syntax("Expected a namespace definition, but found unexpected syntax");
            } else {
                throw syntax("A namespace must be defined before a use statement or shapes");
            }
        }
    }

    private void parseUseSection() {
        while (peek() == 'u' && peek(1) == 's') {
            expect('u');
            expect('s');
            expect('e');
            ws();

            int start = position();
            SourceLocation location = currentLocation();
            ParserUtils.consumeNamespace(this);
            expect('#');
            ParserUtils.consumeIdentifier(this);
            String lexeme = sliceFrom(start);
            // Clear out any erroneous documentation comments.
            clearPendingDocs();
            ws();

            modelFile.useShape(ShapeId.from(lexeme), location);
        }
    }

    private void parseShapeStatements() {
        while (!eof()) {
            ws();
            if (peek() == 'a') {
                parseApplyStatement();
            } else {
                boolean docsOnly = pendingDocumentationComment != null;
                List<TraitEntry> traits = parseDocsAndTraits();
                if (parseShapeDefinition(traits, docsOnly)) {
                    parseShape(traits);
                }
            }
        }
    }

    private void clearPendingDocs() {
        pendingDocumentationComment = null;
    }

    private boolean parseShapeDefinition(List<TraitEntry> traits, boolean docsOnly) {
        if (eof()) {
            return !traits.isEmpty() && !docsOnly;
        } else {
            return true;
        }
    }

    private List<TraitEntry> parseDocsAndTraits() {
        // Grab the pending docs, if present, and clear its state.
        TraitEntry docComment = pendingDocumentationComment;
        clearPendingDocs();

        // Parse traits, if any.
        ws();
        List<TraitEntry> traits = IdlTraitParser.parseTraits(this);
        if (docComment != null) {
            traits.add(docComment);
        }
        ws();

        return traits;
    }

    private void parseShape(List<TraitEntry> traits) {
        SourceLocation location = currentLocation();

        // Do a check here to give better parsing error messages.
        String shapeType = ParserUtils.parseIdentifier(this);
        if (!SHAPE_TYPES.contains(shapeType)) {
            switch (shapeType) {
                case "use":
                    throw syntax("A use statement must come before any shape definition");
                case "namespace":
                    throw syntax("Only a single namespace can be declared per/file");
                case "metadata":
                    throw syntax("Metadata statements must appear before a namespace statement");
                default:
                    throw syntax("Unexpected shape type: " + shapeType);
            }
        }

        ws();
        ShapeId id = parseShapeName();

        switch (shapeType) {
            case "service":
                parseServiceStatement(id, location);
                break;
            case "resource":
                parseResourceStatement(id, location);
                break;
            case "operation":
                parseOperationStatement(id, location);
                break;
            case "structure":
                parseStructuredShape(id, location, StructureShape.builder());
                break;
            case "union":
                parseStructuredShape(id, location, UnionShape.builder());
                break;
            case "list":
                parseCollection(id, location, ListShape.builder());
                break;
            case "set":
                parseCollection(id, location, SetShape.builder());
                break;
            case "map":
                parseMapStatement(id, location);
                break;
            case "boolean":
                parseSimpleShape(id, location, BooleanShape.builder());
                break;
            case "string":
                parseSimpleShape(id, location, StringShape.builder());
                break;
            case "enum":
                parseEnumShape(id, location, EnumShape.builder());
                break;
            case "blob":
                parseSimpleShape(id, location, BlobShape.builder());
                break;
            case "byte":
                parseSimpleShape(id, location, ByteShape.builder());
                break;
            case "short":
                parseSimpleShape(id, location, ShortShape.builder());
                break;
            case "integer":
                parseSimpleShape(id, location, IntegerShape.builder());
                break;
            case "intEnum":
                parseEnumShape(id, location, IntEnumShape.builder());
                break;
            case "long":
                parseSimpleShape(id, location, LongShape.builder());
                break;
            case "float":
                parseSimpleShape(id, location, FloatShape.builder());
                break;
            case "document":
                parseSimpleShape(id, location, DocumentShape.builder());
                break;
            case "double":
                parseSimpleShape(id, location, DoubleShape.builder());
                break;
            case "bigInteger":
                parseSimpleShape(id, location, BigIntegerShape.builder());
                break;
            case "bigDecimal":
                parseSimpleShape(id, location, BigDecimalShape.builder());
                break;
            case "timestamp":
                parseSimpleShape(id, location, TimestampShape.builder());
                break;
            default:
                // Unreachable.
                throw syntax(id, "Unexpected shape type: " + shapeType);
        }

        addTraits(id, traits);
        ws();
    }

    private ShapeId parseShapeName() {
        String name = ParserUtils.parseIdentifier(this);
        return ShapeId.fromRelative(modelFile.namespace(), name);
    }

    private void parseSimpleShape(ShapeId id, SourceLocation location, AbstractShapeBuilder builder) {
        modelFile.onShape(builder.source(location).id(id));
        parseMixins(id);
    }

    private void parseEnumShape(ShapeId id, SourceLocation location, AbstractShapeBuilder builder) {
        modelFile.onShape(builder.id(id).source(location));
        parseMixins(id);
        parseMembers(id, Collections.emptySet(), true);
        clearPendingDocs();
    }

    // See parseMap for information on why members are parsed before the
    // list/set is registered with the ModelFile.
    private void parseCollection(ShapeId id, SourceLocation location, CollectionShape.Builder builder) {
        builder.id(id).source(location);
        parseMixins(id);
        parseMembers(id, SetUtils.of("member"));
        modelFile.onShape(builder.id(id));
        clearPendingDocs();
    }

    private void parseMembers(ShapeId id, Set<String> requiredMembers) {
        parseMembers(id, requiredMembers, false);
    }

    private void parseMembers(ShapeId id, Set<String> requiredMembers, boolean targetsUnit) {
        Set<String> definedMembers = new HashSet<>();

        ws();
        expect('{');
        ws();

        while (!eof()) {
            if (peek() == '}') {
                break;
            }

            parseMember(id, requiredMembers, definedMembers, targetsUnit);

            // Clears out any previously captured documentation
            // comments that may have been found when parsing the member.
            clearPendingDocs();

            ws();
        }

        if (eof()) {
            expect('}');
        }

        expect('}');
    }

    private void parseMember(ShapeId parent, Set<String> allowed, Set<String> defined, boolean targetsUnit) {
        // Parse optional member traits.
        List<TraitEntry> memberTraits = parseDocsAndTraits();
        SourceLocation memberLocation = currentLocation();
        String memberName = ParserUtils.parseIdentifier(this);

        if (defined.contains(memberName)) {
            // This is a duplicate member name.
            throw syntax(parent, "Duplicate member of " + parent + ": '" + memberName + '\'');
        }

        defined.add(memberName);

        // Only enforce "allowedMembers" if it isn't empty.
        if (!allowed.isEmpty() && !allowed.contains(memberName)) {
            throw syntax(parent, "Unexpected member of " + parent + ": '" + memberName + '\'');
        }

        ShapeId memberId = parent.withMember(memberName);
        MemberShape.Builder memberBuilder = MemberShape.builder().id(memberId).source(memberLocation);
        modelFile.onShape(memberBuilder);
        String target;

        if (!targetsUnit) {
            ws();
            expect(':');

            if (peek() == '=') {
                throw syntax("Defining structures inline with the `:=` syntax may only be used when "
                        + "defining operation input and output shapes.");
            }

            ws();
            target = ParserUtils.parseShapeId(this);
        } else {
            target = UnitTypeTrait.UNIT.toString();
        }
        modelFile.addForwardReference(target, memberBuilder::target);
        addTraits(parent.withMember(memberName), memberTraits);
    }

    private void parseMapStatement(ShapeId id, SourceLocation location) {
        // Parsing members of list/set/map before registering the shape with
        // the ModelFile ensures that the shape is only registered if it
        // has all of its required members. Otherwise, the validation gives
        // a cryptic message with no context about how a "member" wasn't set
        // on a builder. This does not suffer the same error messages as
        // structures/unions because list/set/map have a fixed and required
        // set of members that must be provided.
        parseMixins(id);
        parseMembers(id, SetUtils.of("key", "value"));
        modelFile.onShape(MapShape.builder().id(id).source(location));
        clearPendingDocs();
    }

    private void parseStructuredShape(
            ShapeId id,
            SourceLocation location,
            AbstractShapeBuilder<?, ?> builder
    ) {
        // Register the structure/union with the loader before parsing members.
        // This will detect shape conflicts with other types (like an operation)
        // and still give useful error messages. Trying to parse members first
        // would otherwise result in cryptic error messages like:
        // "Member `foo.baz#Foo$Baz` cannot be added to software.amazon.smithy.model.shapes.OperationShape$Builder"
        modelFile.onShape(builder.id(id).source(location));

        // Parse optional "with" statements to add mixins, but only if it's supported by the version.
        parseMixins(id);
        parseMembers(id, Collections.emptySet());
        clearPendingDocs();
    }

    private void parseMixins(ShapeId id) {
        sp();
        if (peek() != 'w') {
            return;
        }

        expect('w');
        expect('i');
        expect('t');
        expect('h');

        if (!modelFile.getVersion().supportsMixins()) {
            throw syntax(id, "Mixins can only be used with Smithy version 2 or later. "
                    + "Attempted to use mixins with version `" + modelFile.getVersion() + "`.");
        }

        ws();
        expect('[');
        ws();

        do {
            String target = ParserUtils.parseShapeId(this);
            modelFile.addForwardReference(target, resolved -> modelFile.addPendingMixin(id, resolved));
            ws();
        } while (peek() != ']');
        expect(']');
        clearPendingDocs();
    }

    private void parseOperationStatement(ShapeId id, SourceLocation location) {
        parseMixins(id);
        OperationShape.Builder builder = OperationShape.builder().id(id).source(location);
        parseProperties(id, propertyName -> {
            switch (propertyName) {
                case "input":
                    TraitEntry inputTrait = new TraitEntry(InputTrait.ID.toString(), Node.objectNode(), true);
                    parseInlineableOperationMember(id, operationInputSuffix, builder::input, inputTrait);
                    break;
                case "output":
                    TraitEntry outputTrait = new TraitEntry(OutputTrait.ID.toString(), Node.objectNode(), true);
                    parseInlineableOperationMember(id, operationOutputSuffix, builder::output, outputTrait);
                    break;
                case "errors":
                    parseIdList(builder::addError);
                    break;
                default:
                    throw syntax(id, String.format("Unknown property %s for %s", propertyName, id));
            }
        });
        modelFile.onShape(builder);
        clearPendingDocs();
    }

    private void parseProperties(ShapeId id, Consumer<String> valueParser) {
        ws();
        expect('{');
        ws();

        Set<String> defined = new HashSet<>();
        while (!eof() && peek() != '}') {
            String key = IdlNodeParser.parseNodeObjectKey(this);
            if (defined.contains(key)) {
                throw syntax(id, String.format("Duplicate %s binding for %s", key, id));
            }
            defined.add(key);

            ws();
            expect(':');
            valueParser.accept(key);
            ws();
        }

        expect('}');
    }

    private void parseInlineableOperationMember(
            ShapeId id,
            String suffix,
            Consumer<ShapeId> consumer,
            TraitEntry defaultTrait
    ) {
        if (peek() == '=') {
            if (!modelFile.getVersion().supportsInlineOperationIO()) {
                throw syntax(id, "Inlined operation inputs and outputs can only be used with Smithy version 2 or "
                        + "later. Attempted to use inlined IO with version `" + modelFile.getVersion() + "`.");
            }
            expect('=');
            clearPendingDocs();
            ws();
            consumer.accept(parseInlineStructure(id.getName() + suffix, defaultTrait));
        } else {
            ws();
            modelFile.addForwardReference(ParserUtils.parseShapeId(this), consumer);
        }
    }

    private ShapeId parseInlineStructure(String name, TraitEntry defaultTrait) {
        List<TraitEntry> traits = parseDocsAndTraits();
        if (defaultTrait != null) {
            traits.add(defaultTrait);
        }
        ShapeId id = ShapeId.fromRelative(modelFile.namespace(), name);
        SourceLocation location = currentLocation();
        parseMixins(id);
        StructureShape.Builder builder = StructureShape.builder().id(id).source(location);

        modelFile.onShape(builder);
        parseMembers(id, Collections.emptySet());
        addTraits(id, traits);
        clearPendingDocs();
        ws();
        return id;
    }

    private void parseIdList(Consumer<ShapeId> consumer) {
        increaseNestingLevel();
        ws();
        expect('[');
        ws();

        while (!eof() && peek() != ']') {
            modelFile.addForwardReference(ParserUtils.parseShapeId(this), consumer);
            ws();
        }

        expect(']');
        decreaseNestingLevel();
    }

    private void parseServiceStatement(ShapeId id, SourceLocation location) {
        parseMixins(id);
        ws();
        ServiceShape.Builder builder = new ServiceShape.Builder().id(id).source(location);
        ObjectNode shapeNode = IdlNodeParser.parseObjectNode(this, id.toString());
        LoaderUtils.checkForAdditionalProperties(shapeNode, id, SERVICE_PROPERTY_NAMES, modelFile.events());
        shapeNode.getStringMember(VERSION_KEY).map(StringNode::getValue).ifPresent(builder::version);
        modelFile.onShape(builder);
        optionalIdList(shapeNode, OPERATIONS_KEY, builder::addOperation);
        optionalIdList(shapeNode, RESOURCES_KEY, builder::addResource);
        optionalIdList(shapeNode, ERRORS_KEY, builder::addError);
        AstModelLoader.loadServiceRenameIntoBuilder(builder, shapeNode);
        clearPendingDocs();
    }

    private void optionalId(ObjectNode node, String name, Consumer<ShapeId> consumer) {
        if (node.getMember(name).isPresent()) {
            modelFile.addForwardReference(node.expectStringMember(name).getValue(), consumer);
        }
    }

    private void optionalIdList(ObjectNode node, String name, Consumer<ShapeId> consumer) {
        if (node.getMember(name).isPresent()) {
            ArrayNode value = node.expectArrayMember(name);
            for (StringNode element : value.getElementsAs(StringNode.class)) {
                modelFile.addForwardReference(element.getValue(), consumer);
            }
        }
    }

    private void parseResourceStatement(ShapeId id, SourceLocation location) {
        parseMixins(id);
        ws();
        ResourceShape.Builder builder = ResourceShape.builder().id(id).source(location);
        modelFile.onShape(builder);
        ObjectNode shapeNode = IdlNodeParser.parseObjectNode(this, id.toString());

        LoaderUtils.checkForAdditionalProperties(shapeNode, id, RESOURCE_PROPERTY_NAMES, modelFile.events());
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
                modelFile.addForwardReference(target.getValue(), targetId -> builder.addIdentifier(name, targetId));
            }
        });
        clearPendingDocs();
    }

    // "//" *(not_newline)
    private void parseComment() {
        expect('/');
        consumeRemainingCharactersOnLine();
    }

    private void parseDocComment() {
        SourceLocation location = currentLocation();
        StringJoiner joiner = new StringJoiner("\n");
        do {
            joiner.add(parseDocCommentLine());
        } while (peekDocComment());
        pendingDocumentationComment = new TraitEntry(
                DocumentationTrait.ID.toString(), new StringNode(joiner.toString(), location), false);
    }

    private boolean peekDocComment() {
        return peek() == '/' && peek(1) == '/' && peek(2) == '/';
    }

    // documentation_comment = "///" *(not_newline)
    private String parseDocCommentLine() {
        expect('/');
        expect('/');
        expect('/');
        // Skip a leading space, if present.
        if (peek() == ' ') {
            skip();
        }
        int start = position();
        consumeRemainingCharactersOnLine();
        br();
        sp();
        return StringUtils.stripEnd(sliceFrom(start), " \t\r\n");
    }

    private void parseApplyStatement() {
        expect('a');
        expect('p');
        expect('p');
        expect('l');
        expect('y');
        ws();

        String name = ParserUtils.parseShapeId(this);
        ws();

        // Account for singular or block apply statements.
        List<TraitEntry> traitsToApply;
        if (peek() == '{') {
            expect('{');
            ws();
            traitsToApply = IdlTraitParser.parseTraits(this);
            expect('}');
        } else {
            traitsToApply = Collections.singletonList(IdlTraitParser.parseTraitValue(this));
        }

        // First, resolve the targeted shape.
        modelFile.addForwardReference(name, id -> {
            for (TraitEntry traitEntry : traitsToApply) {
                onDeferredTrait(id, traitEntry.traitName, traitEntry.value, traitEntry.isAnnotation);
            }
        });

        // Clear out any errantly captured pending docs.
        clearPendingDocs();
        ws();
    }

    private void addTraits(ShapeId id, List<TraitEntry> traits) {
        for (TraitEntry traitEntry : traits) {
            onDeferredTrait(id, traitEntry.traitName, traitEntry.value, traitEntry.isAnnotation);
        }
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
        modelFile.addForwardReference(traitName, (id, typeProvider) -> {
            modelFile.onTrait(target, id, coerceTraitValue(id, traitValue, isAnnotation, typeProvider));
        });
    }

    private Node coerceTraitValue(
            ShapeId traitId, Node value, boolean isAnnotation, Function<ShapeId, ShapeType> typeProvider) {
        if (isAnnotation && value.isNullNode()) {
            ShapeType targetType = typeProvider.apply(traitId);
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

    SourceLocation currentLocation() {
        return new SourceLocation(filename, line(), column());
    }

    NumberNode parseNumberNode() {
        return parseNumberNode(currentLocation());
    }

    NumberNode parseNumberNode(SourceLocation location) {
        String lexeme = ParserUtils.parseNumber(this);

        if (lexeme.contains("e") || lexeme.contains("E")  || lexeme.contains(".")) {
            double value = Double.parseDouble(lexeme);
            if (Double.isFinite(value)) {
                return new NumberNode(value, location);
            }
            return new NumberNode(new BigDecimal(lexeme), location);
        } else {
            try {
                return new NumberNode(Long.parseLong(lexeme), location);
            } catch (NumberFormatException e) {
                return new NumberNode(new BigInteger(lexeme), location);
            }
        }
    }

    private String peekDebugMessage() {
        StringBuilder result = new StringBuilder(expression().length());

        char c = peek();

        // Try to read an entire identifier for context (16 char max) if that's what's being peeked.
        if (c == ' ' || ParserUtils.isIdentifierStart(c) || ParserUtils.isDigit(c)) {
            if (c == ' ') {
                result.append(' ');
            }
            for (int i = c == ' ' ? 1 : 0; i < 16; i++) {
                c = peek(i);
                if (ParserUtils.isIdentifierStart(c) || ParserUtils.isDigit(c)) {
                    result.append(c);
                } else {
                    break;
                }
            }
        } else {
            // Take two characters for context.
            for (int i = 0; i < 2; i++) {
                char peek = peek(i);
                if (peek != Character.MIN_VALUE) {
                    result.append(peek);
                }
            }
        }

        return result.length() == 0 ? "[EOF]" : result.toString();
    }
}
