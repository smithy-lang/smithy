package software.amazon.smithy.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.loader.IdlTokenizer;

public class TreeTypeTest {

    @Test
    public void idl() {
        String idl = "$version: \"2.0\"\n\nnamespace com.foo\n";
        CapturingTokenizer tokenizer = new CapturingTokenizer(IdlTokenizer.create(idl));
        TreeType.IDL.parse(tokenizer);
        assertTreeIsValid(tokenizer.getRoot());
        rootAndChildTypesEqual(tokenizer.getRoot(),
                TreeType.IDL,
                TreeType.CONTROL_SECTION,
                TreeType.METADATA_SECTION,
                TreeType.SHAPE_SECTION);
    }

    @Test
    public void controlSection() {
        String controlSection = "$version: \"2.0\"\n$foo: [\"bar\"]\n";
        TokenTree tree = getTree(TreeType.CONTROL_SECTION, controlSection);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.CONTROL_SECTION,
                TreeType.CONTROL_STATEMENT,
                TreeType.CONTROL_STATEMENT);
    }

    @Test
    public void controlStatement() {
        String controlStatement = "$version: \"2.0\"\n";
        TokenTree tree = getTree(TreeType.CONTROL_STATEMENT, controlStatement);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.CONTROL_STATEMENT,
                TreeType.TOKEN,
                TreeType.NODE_OBJECT_KEY,
                TreeType.TOKEN,
                TreeType.SP,
                TreeType.NODE_VALUE,
                TreeType.BR);
    }

    @Test
    public void identifierNodeObjectKey() {
        String identifier = "version";
        TokenTree tree = getTree(TreeType.NODE_OBJECT_KEY, identifier);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.NODE_OBJECT_KEY,
                TreeType.IDENTIFIER);
    }

    @Test
    public void stringNodeObjectKey() {
        String string = "\"foo bar\"";
        TokenTree tree = getTree(TreeType.NODE_OBJECT_KEY, string);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.NODE_OBJECT_KEY,
                TreeType.QUOTED_TEXT);
    }

    @Test
    public void metadataSection() {
        String metadataSection = "metadata foo = bar\nmetadata bar=baz\n";
        TokenTree tree = getTree(TreeType.METADATA_SECTION, metadataSection);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.METADATA_SECTION,
                TreeType.METADATA_STATEMENT,
                TreeType.METADATA_STATEMENT);
    }

    @Test
    public void metadataStatement() {
        String statement = "metadata foo = bar // Foo\n\n\t";
        TokenTree tree = getTree(TreeType.METADATA_STATEMENT, statement);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.METADATA_STATEMENT,
                TreeType.TOKEN,
                TreeType.SP,
                TreeType.NODE_OBJECT_KEY,
                TreeType.SP,
                TreeType.TOKEN,
                TreeType.SP,
                TreeType.NODE_VALUE,
                TreeType.BR);
    }

    @Test
    public void shapeSection() {
        String shapeSection = "namespace com.foo\nuse com.bar#Baz\nstructure Foo {}";
        TokenTree tree = getTree(TreeType.SHAPE_SECTION, shapeSection);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.SHAPE_SECTION,
                TreeType.NAMESPACE_STATEMENT,
                TreeType.USE_SECTION,
                TreeType.SHAPE_STATEMENTS);
    }

    @Test
    public void namespaceStatement() {
        String namespaceStatement = "namespace \t com.foo// Foo\n";
        TokenTree tree = getTree(TreeType.NAMESPACE_STATEMENT, namespaceStatement);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.NAMESPACE_STATEMENT,
                TreeType.TOKEN,
                TreeType.SP,
                TreeType.NAMESPACE,
                TreeType.BR);
    }

    @Test
    public void namespace() {
        String namespace = "foo.bar.baz.qux";
        TokenTree tree = getTree(TreeType.NAMESPACE, namespace);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.NAMESPACE,
                TreeType.IDENTIFIER,
                TreeType.TOKEN,
                TreeType.IDENTIFIER,
                TreeType.TOKEN,
                TreeType.IDENTIFIER,
                TreeType.TOKEN,
                TreeType.IDENTIFIER);
    }

    @Test
    public void identifier() {
        String identifier1 = "foo";
        TokenTree tree1 = getTree(TreeType.IDENTIFIER, identifier1);
        assertTreeIsValid(tree1);
        rootAndChildTypesEqual(tree1,
                TreeType.IDENTIFIER,
                TreeType.TOKEN);

        String identifier2 = "_foo";
        TokenTree tree2 = getTree(TreeType.IDENTIFIER, identifier2);
        assertTreeIsValid(tree2);
        rootAndChildTypesEqual(tree2,
                TreeType.IDENTIFIER,
                TreeType.TOKEN);

        String identifier3 = "_1foo";
        TokenTree tree3 = getTree(TreeType.IDENTIFIER, identifier3);
        assertTreeIsValid(tree3);
        rootAndChildTypesEqual(tree3,
                TreeType.IDENTIFIER,
                TreeType.TOKEN);

        String identifier4 = "1foo";
        TokenTree tree4 = getTree(TreeType.IDENTIFIER, identifier4);
        rootAndChildTypesEqual(tree4,
                TreeType.IDENTIFIER,
                TreeType.ERROR);
    }

    @Test
    public void useSection() {
        String useSection = "use com.foo#Foo\nuse com.bar#Bar\n";
        TokenTree tree = getTree(TreeType.USE_SECTION, useSection);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.USE_SECTION,
                TreeType.USE_STATEMENT,
                TreeType.USE_STATEMENT);
    }

    @Test
    public void useStatement() {
        String useStatement = "use \tcom.foo#Bar\t\n";
        TokenTree tree = getTree(TreeType.USE_STATEMENT, useStatement);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.USE_STATEMENT,
                TreeType.TOKEN,
                TreeType.SP,
                TreeType.ABSOLUTE_ROOT_SHAPE_ID,
                TreeType.BR);
    }

    @Test
    public void shapeId() {
        String id = "foo";
        TokenTree idTree = getTree(TreeType.SHAPE_ID, id);
        assertTreeIsValid(idTree);
        rootAndChildTypesEqual(idTree,
                TreeType.SHAPE_ID,
                TreeType.ROOT_SHAPE_ID);

        String withMember = "foo$bar";
        TokenTree withMemberTree = getTree(TreeType.SHAPE_ID, withMember);
        assertTreeIsValid(withMemberTree);
        rootAndChildTypesEqual(withMemberTree,
                TreeType.SHAPE_ID,
                TreeType.ROOT_SHAPE_ID,
                TreeType.SHAPE_ID_MEMBER);
    }

    @Test
    public void rootShapeId() {
        String absolute = "com.foo.bar#Baz";
        TokenTree absoluteTree = getTree(TreeType.ROOT_SHAPE_ID, absolute);
        assertTreeIsValid(absoluteTree);
        rootAndChildTypesEqual(absoluteTree,
                TreeType.ROOT_SHAPE_ID,
                TreeType.ABSOLUTE_ROOT_SHAPE_ID);

        String id = "foo";
        TokenTree idTree = getTree(TreeType.ROOT_SHAPE_ID, id);
        assertTreeIsValid(idTree);
        rootAndChildTypesEqual(idTree,
                TreeType.ROOT_SHAPE_ID,
                TreeType.IDENTIFIER);
    }

    @Test
    public void absoluteRootShapeId() {
        String absolute = "com.foo.bar#Baz";
        TokenTree tree = getTree(TreeType.ABSOLUTE_ROOT_SHAPE_ID, absolute);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.ABSOLUTE_ROOT_SHAPE_ID,
                TreeType.NAMESPACE,
                TreeType.TOKEN,
                TreeType.IDENTIFIER);
    }

    @Test
    public void shapeIdMember() {
        String shapeIdMember = "$foo";
        TokenTree tree = getTree(TreeType.SHAPE_ID_MEMBER, shapeIdMember);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.SHAPE_ID_MEMBER,
                TreeType.TOKEN,
                TreeType.IDENTIFIER);
    }

    @Test
    public void shapeStatements() {
        String statements = "structure Foo {\n\tfoo: Bar\n}\n\napply foo @bar\n";
        TokenTree tree = getTree(TreeType.SHAPE_STATEMENTS, statements);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.SHAPE_STATEMENTS,
                TreeType.SHAPE_OR_APPLY_STATEMENT,
                TreeType.BR,
                TreeType.SHAPE_OR_APPLY_STATEMENT,
                TreeType.BR);
    }

    @Test
    public void shapeOrApplyStatement() {
        String shape = "string Foo";
        TokenTree shapeTree = getTree(TreeType.SHAPE_OR_APPLY_STATEMENT, shape);
        assertTreeIsValid(shapeTree);
        rootAndChildTypesEqual(shapeTree,
                TreeType.SHAPE_OR_APPLY_STATEMENT,
                TreeType.SHAPE_STATEMENT);

        String apply = "apply foo @bar";
        TokenTree applyTree = getTree(TreeType.SHAPE_OR_APPLY_STATEMENT, apply);
        assertTreeIsValid(applyTree);
        rootAndChildTypesEqual(applyTree,
                TreeType.SHAPE_OR_APPLY_STATEMENT,
                TreeType.APPLY_STATEMENT);
    }

    @Test
    public void shapeStatement() {
        String statement = "@foo\nstring Foo";
        TokenTree tree = getTree(TreeType.SHAPE_STATEMENT, statement);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.SHAPE_STATEMENT,
                TreeType.TRAIT_STATEMENTS,
                TreeType.SHAPE);
    }

    @Test
    public void shape() {
        String shape = "structure Foo {}\n";
        TokenTree tree = getTree(TreeType.SHAPE, shape);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.SHAPE,
                TreeType.AGGREGATE_SHAPE);
    }

    @Test
    public void simpleShape() {
        String shape = "string \tFoo";
        TokenTree shapeTree = getTree(TreeType.SIMPLE_SHAPE, shape);
        assertTreeIsValid(shapeTree);
        rootAndChildTypesEqual(shapeTree,
                TreeType.SIMPLE_SHAPE,
                TreeType.SIMPLE_TYPE_NAME,
                TreeType.SP,
                TreeType.IDENTIFIER);

        String withMixins = "string Foo with [Bar]";
        TokenTree withMixinsTree = getTree(TreeType.SIMPLE_SHAPE, withMixins);
        assertTreeIsValid(withMixinsTree);
        rootAndChildTypesEqual(withMixinsTree,
                TreeType.SIMPLE_SHAPE,
                TreeType.SIMPLE_TYPE_NAME,
                TreeType.SP,
                TreeType.IDENTIFIER,
                TreeType.SP,
                TreeType.MIXINS);
    }

    @Test
    public void simpleTypeName() {
        String simpleTypeName = "string";
        TokenTree tree = getTree(TreeType.SIMPLE_TYPE_NAME, simpleTypeName);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.SIMPLE_TYPE_NAME,
                TreeType.TOKEN);
    }

    @Test
    public void enumShape() {
        String enumShape = "enum Foo\n\t{foo = 1\n}";
        TokenTree tree = getTree(TreeType.ENUM_SHAPE, enumShape);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.ENUM_SHAPE,
                TreeType.ENUM_TYPE_NAME,
                TreeType.SP,
                TreeType.IDENTIFIER,
                TreeType.WS,
                TreeType.ENUM_SHAPE_MEMBERS);

        String withMixins = "enum Foo with [Bar] \n{foo}";
        TokenTree withMixinsTree = getTree(TreeType.ENUM_SHAPE, withMixins);
        assertTreeIsValid(withMixinsTree);
        rootAndChildTypesEqual(withMixinsTree,
                TreeType.ENUM_SHAPE,
                TreeType.ENUM_TYPE_NAME,
                TreeType.SP,
                TreeType.IDENTIFIER,
                TreeType.SP,
                TreeType.MIXINS,
                TreeType.WS,
                TreeType.ENUM_SHAPE_MEMBERS);
    }

    @Test
    public void enumTypeName() {
        String enumTypeName = "intEnum";
        TokenTree tree = getTree(TreeType.ENUM_TYPE_NAME, enumTypeName);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.ENUM_TYPE_NAME,
                TreeType.TOKEN);
    }

    @Test
    public void enumShapeMembers() {
        String members = "{\nfoo bar baz\n}";
        TokenTree tree = getTree(TreeType.ENUM_SHAPE_MEMBERS, members);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.ENUM_SHAPE_MEMBERS,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.ENUM_SHAPE_MEMBER,
                TreeType.WS,
                TreeType.ENUM_SHAPE_MEMBER,
                TreeType.WS,
                TreeType.ENUM_SHAPE_MEMBER,
                TreeType.WS,
                TreeType.TOKEN);
    }

    @Test
    public void enumShapeMember() {
        String member = "FOO";
        TokenTree tree = getTree(TreeType.ENUM_SHAPE_MEMBER, member);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.ENUM_SHAPE_MEMBER,
                TreeType.TRAIT_STATEMENTS,
                TreeType.IDENTIFIER);

        String withValue = "FOO = BAR\n";
        TokenTree withValueTree = getTree(TreeType.ENUM_SHAPE_MEMBER, withValue);
        assertTreeIsValid(withValueTree);
        rootAndChildTypesEqual(withValueTree,
                TreeType.ENUM_SHAPE_MEMBER,
                TreeType.TRAIT_STATEMENTS,
                TreeType.IDENTIFIER,
                TreeType.VALUE_ASSIGNMENT);

        String noWs = "FOO=BAR\n";
        TokenTree noWsTree = getTree(TreeType.ENUM_SHAPE_MEMBER, noWs);
        assertTreeIsValid(noWsTree);
        rootAndChildTypesEqual(noWsTree,
                TreeType.ENUM_SHAPE_MEMBER,
                TreeType.TRAIT_STATEMENTS,
                TreeType.IDENTIFIER,
                TreeType.VALUE_ASSIGNMENT);

        String withTraits = "@foo\n@bar\nFOO";
        TokenTree withTraitsTree = getTree(TreeType.ENUM_SHAPE_MEMBER, withTraits);
        assertTreeIsValid(withTraitsTree);
        rootAndChildTypesEqual(withTraitsTree,
                TreeType.ENUM_SHAPE_MEMBER,
                TreeType.TRAIT_STATEMENTS,
                TreeType.IDENTIFIER);
    }

    @Test
    public void aggregateShape() {
        String shape = "structure Foo {}";
        TokenTree tree = getTree(TreeType.AGGREGATE_SHAPE, shape);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.AGGREGATE_SHAPE,
                TreeType.AGGREGATE_TYPE_NAME,
                TreeType.SP,
                TreeType.IDENTIFIER,
                TreeType.SP,
                TreeType.SHAPE_MEMBERS);
    }

    @Test
    public void aggregateTypeName() {
        String typeName = "structure";
        TokenTree tree = getTree(TreeType.AGGREGATE_TYPE_NAME, typeName);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.AGGREGATE_TYPE_NAME,
                TreeType.TOKEN);
    }

    @Test
    public void aggregateShapeForResource() {
        String shape = "structure Foo for Bar {}";
        TokenTree tree = getTree(TreeType.AGGREGATE_SHAPE, shape);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.AGGREGATE_SHAPE,
                TreeType.AGGREGATE_TYPE_NAME,
                TreeType.SP,
                TreeType.IDENTIFIER,
                TreeType.SP,
                TreeType.FOR_RESOURCE,
                TreeType.SP,
                TreeType.SHAPE_MEMBERS);
    }

    @Test
    public void aggregateShapeMixins() {
        String shape = "structure Foo with [Bar, Baz] {}";
        TokenTree tree = getTree(TreeType.AGGREGATE_SHAPE, shape);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.AGGREGATE_SHAPE,
                TreeType.AGGREGATE_TYPE_NAME,
                TreeType.SP,
                TreeType.IDENTIFIER,
                TreeType.SP,
                TreeType.MIXINS,
                TreeType.WS,
                TreeType.SHAPE_MEMBERS);
    }

    @Test
    public void aggregateShapeForResourceAndMixins() {
        String shape = "structure Foo for Bar with [Baz] {}";
        TokenTree tree = getTree(TreeType.AGGREGATE_SHAPE, shape);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.AGGREGATE_SHAPE,
                TreeType.AGGREGATE_TYPE_NAME,
                TreeType.SP,
                TreeType.IDENTIFIER,
                TreeType.SP,
                TreeType.FOR_RESOURCE,
                TreeType.SP,
                TreeType.MIXINS,
                TreeType.WS,
                TreeType.SHAPE_MEMBERS);
    }

    @Test
    public void forResource() {
        String forResource = "for Foo";
        TokenTree tree = getTree(TreeType.FOR_RESOURCE, forResource);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.FOR_RESOURCE,
                TreeType.TOKEN,
                TreeType.SP,
                TreeType.SHAPE_ID);
    }

    @Test
    public void mixins() {
        String mixins = "with [Foo, Bar]";
        TokenTree tree = getTree(TreeType.MIXINS, mixins);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.MIXINS,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.TOKEN,
                TreeType.SHAPE_ID,
                TreeType.WS,
                TreeType.SHAPE_ID,
                TreeType.TOKEN);
    }

    @Test
    public void shapeMembers() {
        String shapeMembers = "{\n\n\tfoo: Bar\n\tbar: Baz\n}";
        TokenTree tree = getTree(TreeType.SHAPE_MEMBERS, shapeMembers);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.SHAPE_MEMBERS,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.SHAPE_MEMBER,
                TreeType.WS,
                TreeType.SHAPE_MEMBER,
                TreeType.WS,
                TreeType.TOKEN);
    }

    @Test
    public void shapeMember() {
        String shapeMember = "@foo\n/// Foo\n\tfoo: Bar";
        TokenTree tree = getTree(TreeType.SHAPE_MEMBER, shapeMember);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.SHAPE_MEMBER,
                TreeType.TRAIT_STATEMENTS,
                TreeType.EXPLICIT_SHAPE_MEMBER);
    }

    @Test
    public void traitStatements() {
        String traitStatements = "@foo\t// Foo\n\t/// Foo\n@bar\n";
        TokenTree tree = getTree(TreeType.TRAIT_STATEMENTS, traitStatements);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.TRAIT_STATEMENTS,
                TreeType.TRAIT,
                TreeType.WS,
                TreeType.TRAIT,
                TreeType.WS);
    }

    @Test
    public void trait() {
        String trait = "@com.foo#Bar";
        TokenTree tree = getTree(TreeType.TRAIT, trait);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.TRAIT,
                TreeType.TOKEN,
                TreeType.SHAPE_ID);
    }

    @Test
    public void traitWithEmptyBody() {
        String trait = "@abc()";
        TokenTree tree = getTree(TreeType.TRAIT, trait);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                               TreeType.TRAIT,
                               TreeType.TOKEN,
                               TreeType.SHAPE_ID,
                               TreeType.TRAIT_BODY);
    }

    @Test
    public void traitWithNonEmptyBody() {
        String trait = "@abc(hi)";
        TokenTree tree = getTree(TreeType.TRAIT, trait);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                               TreeType.TRAIT,
                               TreeType.TOKEN,
                               TreeType.SHAPE_ID,
                               TreeType.TRAIT_BODY);
    }

    @Test
    public void traitBody() {
        String traitBody = "(\nfoo: Bar\n)";
        TokenTree tree = getTree(TreeType.TRAIT_BODY, traitBody);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.TRAIT_BODY,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.TRAIT_STRUCTURE,
                TreeType.TOKEN);
    }

    @Test
    public void traitBodyTraitStructure() {
        String traitBody = "foo: bar";
        TokenTree tree = getTree(TreeType.TRAIT_STRUCTURE, traitBody);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree, TreeType.TRAIT_STRUCTURE, TreeType.NODE_OBJECT_KVP);
    }

    @Test
    public void traitBodyTraitNodeString() {
        String traitBody = "(\"foo\")";
        TokenTree tree = getTree(TreeType.TRAIT_BODY, traitBody);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree, TreeType.TRAIT_BODY, TreeType.TOKEN, TreeType.TRAIT_NODE, TreeType.TOKEN);
    }

    @Test
    public void traitBodyWithWs() {
        String traitBody = "( \"foo\" )";
        TokenTree tree = getTree(TreeType.TRAIT_BODY, traitBody);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.TRAIT_BODY,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.TRAIT_NODE,
                TreeType.TOKEN);
    }

    @Test
    public void traitBodyTraitNodeStructure() {
        String traitBody = "({ foo: bar })";
        TokenTree tree = getTree(TreeType.TRAIT_BODY, traitBody);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree, TreeType.TRAIT_BODY, TreeType.TOKEN, TreeType.TRAIT_NODE, TreeType.TOKEN);
    }

    @Test
    public void traitNode() {
        String traitBody = "\"foo\"";
        TokenTree tree = getTree(TreeType.TRAIT_NODE, traitBody);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree, TreeType.TRAIT_NODE, TreeType.NODE_VALUE);
    }

    @Test
    public void traitStructure() {
        String traitStructure = "foo: bar, bar: baz\n\n// Baz\nbaz: qux\n";
        TokenTree tree = getTree(TreeType.TRAIT_STRUCTURE, traitStructure);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.TRAIT_STRUCTURE,
                TreeType.NODE_OBJECT_KVP,
                TreeType.WS,
                TreeType.NODE_OBJECT_KVP,
                TreeType.WS,
                TreeType.NODE_OBJECT_KVP,
                TreeType.WS);
    }

    @Test
    public void explicitShapeMember() {
        String explicitShapeMember = "foo \t: \t com.foo#Bar";
        TokenTree tree = getTree(TreeType.EXPLICIT_SHAPE_MEMBER, explicitShapeMember);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.EXPLICIT_SHAPE_MEMBER,
                TreeType.IDENTIFIER,
                TreeType.SP,
                TreeType.TOKEN,
                TreeType.SP,
                TreeType.SHAPE_ID);
    }

    @Test
    public void elidedShapeMember() {
        String elidedShapeMember = "$foo";
        TokenTree tree = getTree(TreeType.ELIDED_SHAPE_MEMBER, elidedShapeMember);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.ELIDED_SHAPE_MEMBER,
                TreeType.TOKEN,
                TreeType.IDENTIFIER);
    }

    @Test
    public void valueAssignment() {
        String valueAssignment = "\t =  \t foo , \n";
        TokenTree tree = getTree(TreeType.VALUE_ASSIGNMENT, valueAssignment);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.VALUE_ASSIGNMENT,
                TreeType.SP,
                TreeType.TOKEN,
                TreeType.SP,
                TreeType.NODE_VALUE,
                TreeType.SP,
                TreeType.COMMA,
                TreeType.BR);
    }

    @Test
    public void entityShape() {
        String entityShape = "service Foo {}";
        TokenTree entityShapeTree = getTree(TreeType.ENTITY_SHAPE, entityShape);
        assertTreeIsValid(entityShapeTree);
        rootAndChildTypesEqual(entityShapeTree,
                TreeType.ENTITY_SHAPE,
                TreeType.ENTITY_TYPE_NAME,
                TreeType.SP,
                TreeType.IDENTIFIER,
                TreeType.SP,
                TreeType.NODE_OBJECT);

        String noWs = "service Foo{}";
        TokenTree noWsTree = getTree(TreeType.ENTITY_SHAPE, noWs);
        assertTreeIsValid(noWsTree);
        rootAndChildTypesEqual(noWsTree,
                TreeType.ENTITY_SHAPE,
                TreeType.ENTITY_TYPE_NAME,
                TreeType.SP,
                TreeType.IDENTIFIER,
                TreeType.NODE_OBJECT);

        String withMixins = "service Foo with [Bar] {}";
        TokenTree withMixinsTree = getTree(TreeType.ENTITY_SHAPE, withMixins);
        assertTreeIsValid(withMixinsTree);
        rootAndChildTypesEqual(withMixinsTree,
                TreeType.ENTITY_SHAPE,
                TreeType.ENTITY_TYPE_NAME,
                TreeType.SP,
                TreeType.IDENTIFIER,
                TreeType.SP,
                TreeType.MIXINS,
                TreeType.WS,
                TreeType.NODE_OBJECT);
    }

    @Test
    public void entityTypeName() {
        String typeName = "service";
        TokenTree tree = getTree(TreeType.ENTITY_TYPE_NAME, typeName);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.ENTITY_TYPE_NAME,
                TreeType.TOKEN);
    }

    @Test
    public void operationShape() {
        String shape = "operation Foo {input: Foo output: Bar}";
        TokenTree shapeTree = getTree(TreeType.OPERATION_SHAPE, shape);
        assertTreeIsValid(shapeTree);
        rootAndChildTypesEqual(shapeTree,
                TreeType.OPERATION_SHAPE,
                TreeType.TOKEN,
                TreeType.SP,
                TreeType.IDENTIFIER,
                TreeType.SP,
                TreeType.OPERATION_BODY);

        String noWs = "operation Foo{input: Foo output: Bar}";
        TokenTree noWsTree = getTree(TreeType.OPERATION_SHAPE, noWs);
        assertTreeIsValid(noWsTree);
        rootAndChildTypesEqual(noWsTree,
                TreeType.OPERATION_SHAPE,
                TreeType.TOKEN,
                TreeType.SP,
                TreeType.IDENTIFIER,
                TreeType.OPERATION_BODY);

        String withMixins = "operation Foo with [Bar] {input: Foo output: Bar}";
        TokenTree withMixinsTree = getTree(TreeType.OPERATION_SHAPE, withMixins);
        assertTreeIsValid(withMixinsTree);
        rootAndChildTypesEqual(withMixinsTree,
                TreeType.OPERATION_SHAPE,
                TreeType.TOKEN,
                TreeType.SP,
                TreeType.IDENTIFIER,
                TreeType.SP,
                TreeType.MIXINS,
                TreeType.WS,
                TreeType.OPERATION_BODY);
    }

    @Test
    public void operationBody() {
        String operationBody = "{ input: foo\noutput: bar }";
        TokenTree operationBodyTree = getTree(TreeType.OPERATION_BODY, operationBody);
        assertTreeIsValid(operationBodyTree);
        rootAndChildTypesEqual(operationBodyTree,
                TreeType.OPERATION_BODY,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.OPERATION_PROPERTY,
                TreeType.WS,
                TreeType.OPERATION_PROPERTY,
                TreeType.WS,
                TreeType.TOKEN);

        String noWs = "{input:foo}";
        TokenTree noWsTree = getTree(TreeType.OPERATION_BODY, noWs);
        assertTreeIsValid(noWsTree);
        rootAndChildTypesEqual(noWsTree,
                TreeType.OPERATION_BODY,
                TreeType.TOKEN,
                TreeType.OPERATION_PROPERTY,
                TreeType.TOKEN);

        String onlyWs = "{\n// Foo\n \n}";
        TokenTree onlyWsTree = getTree(TreeType.OPERATION_BODY, onlyWs);
        assertTreeIsValid(onlyWsTree);
        rootAndChildTypesEqual(onlyWsTree,
                TreeType.OPERATION_BODY,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.TOKEN);
    }

    @Test
    public void operationProperty() {
        String input = "input: foo";
        TokenTree inputTree = getTree(TreeType.OPERATION_PROPERTY, input);
        assertTreeIsValid(inputTree);
        rootAndChildTypesEqual(inputTree,
                TreeType.OPERATION_PROPERTY,
                TreeType.OPERATION_INPUT);

        String output = "output: foo";
        TokenTree outputTree = getTree(TreeType.OPERATION_PROPERTY, output);
        assertTreeIsValid(outputTree);
        rootAndChildTypesEqual(outputTree,
                TreeType.OPERATION_PROPERTY,
                TreeType.OPERATION_OUTPUT);

        String errors = "errors: []";
        TokenTree errorsTree = getTree(TreeType.OPERATION_PROPERTY, errors);
        assertTreeIsValid(errorsTree);
        rootAndChildTypesEqual(errorsTree,
                TreeType.OPERATION_PROPERTY,
                TreeType.OPERATION_ERRORS);
    }

    @Test
    public void operationInput() {
        String withWs = "input\n//Foo\n : foo";
        TokenTree withWsTree = getTree(TreeType.OPERATION_INPUT, withWs);
        assertTreeIsValid(withWsTree);
        rootAndChildTypesEqual(withWsTree,
                TreeType.OPERATION_INPUT,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.SHAPE_ID);

        String noWs = "input:foo";
        TokenTree noWsTree = getTree(TreeType.OPERATION_INPUT, noWs);
        assertTreeIsValid(noWsTree);
        rootAndChildTypesEqual(noWsTree,
                TreeType.OPERATION_INPUT,
                TreeType.TOKEN,
                TreeType.TOKEN,
                TreeType.SHAPE_ID);

        String shapeId = "input: foo.bar#Baz";
        TokenTree shapeIdTree = getTree(TreeType.OPERATION_INPUT, shapeId);
        assertTreeIsValid(shapeIdTree);
        rootAndChildTypesEqual(shapeIdTree,
                TreeType.OPERATION_INPUT,
                TreeType.TOKEN,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.SHAPE_ID);

        String inline = "input := {}";
        TokenTree inlineTree = getTree(TreeType.OPERATION_INPUT, inline);
        assertTreeIsValid(inlineTree);
        rootAndChildTypesEqual(inlineTree,
                TreeType.OPERATION_INPUT,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.INLINE_AGGREGATE_SHAPE);
    }

    @Test
    public void operationOutput() {
        String withWs = "output\n//Foo\n : foo";
        TokenTree withWsTree = getTree(TreeType.OPERATION_OUTPUT, withWs);
        assertTreeIsValid(withWsTree);
        rootAndChildTypesEqual(withWsTree,
                TreeType.OPERATION_OUTPUT,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.SHAPE_ID);

        String noWs = "output:foo";
        TokenTree noWsTree = getTree(TreeType.OPERATION_OUTPUT, noWs);
        assertTreeIsValid(noWsTree);
        rootAndChildTypesEqual(noWsTree,
                TreeType.OPERATION_OUTPUT,
                TreeType.TOKEN,
                TreeType.TOKEN,
                TreeType.SHAPE_ID);

        String shapeId = "output: foo.bar#Baz";
        TokenTree shapeIdTree = getTree(TreeType.OPERATION_OUTPUT, shapeId);
        assertTreeIsValid(shapeIdTree);
        rootAndChildTypesEqual(shapeIdTree,
                TreeType.OPERATION_OUTPUT,
                TreeType.TOKEN,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.SHAPE_ID);

        String inline = "output := {}";
        TokenTree inlineTree = getTree(TreeType.OPERATION_OUTPUT, inline);
        assertTreeIsValid(inlineTree);
        rootAndChildTypesEqual(inlineTree,
                TreeType.OPERATION_OUTPUT,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.INLINE_AGGREGATE_SHAPE);
    }

    @Test
    public void inlineAggregateShape() {
        String basic = ":= { foo: bar }";
        TokenTree basicTree = getTree(TreeType.INLINE_AGGREGATE_SHAPE, basic);
        assertTreeIsValid(basicTree);
        rootAndChildTypesEqual(basicTree,
                TreeType.INLINE_AGGREGATE_SHAPE,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.TRAIT_STATEMENTS,
                TreeType.SHAPE_MEMBERS);

        String withTraits = ":= @foo\n\n@bar\n\n{ foo: bar }";
        TokenTree withTraitsTree = getTree(TreeType.INLINE_AGGREGATE_SHAPE, withTraits);
        assertTreeIsValid(withTraitsTree);
        rootAndChildTypesEqual(withTraitsTree,
                TreeType.INLINE_AGGREGATE_SHAPE,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.TRAIT_STATEMENTS,
                TreeType.SHAPE_MEMBERS);
    }

    @Test
    public void operationErrors() {
        String empty = "errors: []";
        TokenTree emptyTree = getTree(TreeType.OPERATION_ERRORS, empty);
        assertTreeIsValid(emptyTree);
        rootAndChildTypesEqual(emptyTree,
                TreeType.OPERATION_ERRORS,
                TreeType.TOKEN,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.TOKEN,
                TreeType.TOKEN);

        String noWs = "errors:[foo]";
        TokenTree noWsTree = getTree(TreeType.OPERATION_ERRORS, noWs);
        assertTreeIsValid(noWsTree);
        rootAndChildTypesEqual(noWsTree,
                TreeType.OPERATION_ERRORS,
                TreeType.TOKEN,
                TreeType.TOKEN,
                TreeType.TOKEN,
                TreeType.SHAPE_ID,
                TreeType.TOKEN);

        String withWs = "errors\n//Foo\n: \n[\nfoo // Foo\nbar ]";
        TokenTree withWsTree = getTree(TreeType.OPERATION_ERRORS, withWs);
        assertTreeIsValid(withWsTree);
        rootAndChildTypesEqual(withWsTree,
                TreeType.OPERATION_ERRORS,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.SHAPE_ID,
                TreeType.WS,
                TreeType.SHAPE_ID,
                TreeType.WS,
                TreeType.TOKEN);
    }

    @Test
    public void applyStatement() {
        String applyStatement = "apply foo @bar";
        TokenTree tree = getTree(TreeType.APPLY_STATEMENT, applyStatement);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.APPLY_STATEMENT,
                TreeType.APPLY_STATEMENT_SINGULAR);
    }

    @Test
    public void applyStatementSingular() {
        String singular = "apply foo\n\t@bar";
        TokenTree tree = getTree(TreeType.APPLY_STATEMENT_SINGULAR, singular);
        assertTreeIsValid(tree);
        rootAndChildTypesEqual(tree,
                TreeType.APPLY_STATEMENT_SINGULAR,
                TreeType.TOKEN,
                TreeType.SP,
                TreeType.SHAPE_ID,
                TreeType.WS,
                TreeType.TRAIT);
    }

    @Test
    public void applyStatementBlock() {
        String block = "apply foo\n\t{@foo\n@bar\n}";
        TokenTree blockTree = getTree(TreeType.APPLY_STATEMENT_BLOCK, block);
        assertTreeIsValid(blockTree);
        rootAndChildTypesEqual(blockTree,
                TreeType.APPLY_STATEMENT_BLOCK,
                TreeType.TOKEN,
                TreeType.SP,
                TreeType.SHAPE_ID,
                TreeType.WS,
                TreeType.TOKEN,
                TreeType.TRAIT_STATEMENTS,
                TreeType.TOKEN);

        String withLeadingWsInBlock = "apply foo\n{// Bar\n@foo\n}";
        TokenTree withLeadingWsInBlockTree = getTree(TreeType.APPLY_STATEMENT_BLOCK, withLeadingWsInBlock);
        assertTreeIsValid(withLeadingWsInBlockTree);
        rootAndChildTypesEqual(withLeadingWsInBlockTree,
                TreeType.APPLY_STATEMENT_BLOCK,
                TreeType.TOKEN,
                TreeType.SP,
                TreeType.SHAPE_ID,
                TreeType.WS,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.TRAIT_STATEMENTS,
                TreeType.TOKEN);

        String minimalWs = "apply foo {@bar}";
        TokenTree minimalWsTree = getTree(TreeType.APPLY_STATEMENT_BLOCK, minimalWs);
        assertTreeIsValid(minimalWsTree);
        rootAndChildTypesEqual(minimalWsTree,
                TreeType.APPLY_STATEMENT_BLOCK,
                TreeType.TOKEN,
                TreeType.SP,
                TreeType.SHAPE_ID,
                TreeType.WS,
                TreeType.TOKEN,
                TreeType.TRAIT_STATEMENTS,
                TreeType.TOKEN);
    }

    @Test
    public void nodeValue() {
        String array = "[]";
        TokenTree arrayTree = getTree(TreeType.NODE_VALUE, array);
        assertTreeIsValid(arrayTree);
        rootAndChildTypesEqual(arrayTree,
                TreeType.NODE_VALUE,
                TreeType.NODE_ARRAY);

        String object = "{}";
        TokenTree objectTree = getTree(TreeType.NODE_VALUE, object);
        assertTreeIsValid(objectTree);
        rootAndChildTypesEqual(objectTree,
                TreeType.NODE_VALUE,
                TreeType.NODE_OBJECT);

        String number = "1";
        TokenTree numberTree = getTree(TreeType.NODE_VALUE, number);
        assertTreeIsValid(numberTree);
        rootAndChildTypesEqual(numberTree,
                TreeType.NODE_VALUE,
                TreeType.NUMBER);

        String trueKeyword = "true";
        TokenTree trueTree = getTree(TreeType.NODE_VALUE, trueKeyword);
        assertTreeIsValid(trueTree);
        rootAndChildTypesEqual(trueTree,
                TreeType.NODE_VALUE,
                TreeType.NODE_KEYWORD);

        String falseKeyword = "false";
        TokenTree falseTree = getTree(TreeType.NODE_VALUE, falseKeyword);
        assertTreeIsValid(falseTree);
        rootAndChildTypesEqual(falseTree,
                TreeType.NODE_VALUE,
                TreeType.NODE_KEYWORD);

        String nullKeyword = "null";
        TokenTree nullTree = getTree(TreeType.NODE_VALUE, nullKeyword);
        assertTreeIsValid(nullTree);
        rootAndChildTypesEqual(nullTree,
                TreeType.NODE_VALUE,
                TreeType.NODE_KEYWORD);

        String shapeId = "foo";
        TokenTree shapeIdTree = getTree(TreeType.NODE_VALUE, shapeId);
        assertTreeIsValid(shapeIdTree);
        rootAndChildTypesEqual(shapeIdTree,
                TreeType.NODE_VALUE,
                TreeType.NODE_STRING_VALUE);

        String quotedText = "\"foo\"";
        TokenTree quotedTextTree = getTree(TreeType.NODE_VALUE, quotedText);
        assertTreeIsValid(quotedTextTree);
        rootAndChildTypesEqual(quotedTextTree,
                TreeType.NODE_VALUE,
                TreeType.NODE_STRING_VALUE);

        String textBlock = "\"\"\"\nfoo\"\"\"";
        TokenTree textBlockTree = getTree(TreeType.NODE_VALUE, textBlock);
        assertTreeIsValid(textBlockTree);
        rootAndChildTypesEqual(textBlockTree,
                TreeType.NODE_VALUE,
                TreeType.NODE_STRING_VALUE);
    }

    @Test
    public void nodeArray() {
        String empty = "[]";
        TokenTree emptyTree = getTree(TreeType.NODE_ARRAY, empty);
        assertTreeIsValid(emptyTree);
        rootAndChildTypesEqual(emptyTree,
                TreeType.NODE_ARRAY,
                TreeType.TOKEN,
                TreeType.TOKEN);

        String emptyWs = "[\n\n\t//Foo\n]";
        TokenTree emptyWsTree = getTree(TreeType.NODE_ARRAY, emptyWs);
        assertTreeIsValid(emptyWsTree);
        rootAndChildTypesEqual(emptyWsTree,
                TreeType.NODE_ARRAY,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.TOKEN);

        String withElements = "[ foo\n bar, baz // foo\n\tqux ]";
        TokenTree withElementsTree = getTree(TreeType.NODE_ARRAY, withElements);
        assertTreeIsValid(withElementsTree);
        rootAndChildTypesEqual(withElementsTree,
                TreeType.NODE_ARRAY,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.NODE_VALUE,
                TreeType.WS,
                TreeType.NODE_VALUE,
                TreeType.WS,
                TreeType.NODE_VALUE,
                TreeType.WS,
                TreeType.NODE_VALUE,
                TreeType.WS,
                TreeType.TOKEN);

        String noWs = "[foo]";
        TokenTree noWsTree = getTree(TreeType.NODE_ARRAY, noWs);
        assertTreeIsValid(noWsTree);
        rootAndChildTypesEqual(noWsTree,
                TreeType.NODE_ARRAY,
                TreeType.TOKEN,
                TreeType.NODE_VALUE,
                TreeType.TOKEN);
    }

    @Test
    public void nodeObject() {
        String empty = "{}";
        TokenTree emptyTree = getTree(TreeType.NODE_OBJECT, empty);
        assertTreeIsValid(emptyTree);
        rootAndChildTypesEqual(emptyTree,
                TreeType.NODE_OBJECT,
                TreeType.TOKEN,
                TreeType.TOKEN);

        String emptyWs = "{// Foo\n\n\t}";
        TokenTree emptyWsTree = getTree(TreeType.NODE_OBJECT, emptyWs);
        assertTreeIsValid(emptyWsTree);
        rootAndChildTypesEqual(emptyWsTree,
                TreeType.NODE_OBJECT,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.TOKEN);

        String withElements = "{ foo: bar\n// Foo\n\tbaz: qux\n}";
        TokenTree withElementsTree = getTree(TreeType.NODE_OBJECT, withElements);
        assertTreeIsValid(withElementsTree);
        rootAndChildTypesEqual(withElementsTree,
                TreeType.NODE_OBJECT,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.NODE_OBJECT_KVP,
                TreeType.WS,
                TreeType.NODE_OBJECT_KVP,
                TreeType.WS,
                TreeType.TOKEN);

        String noWs = "{foo:bar}";
        TokenTree noWsTree = getTree(TreeType.NODE_OBJECT, noWs);
        assertTreeIsValid(noWsTree);
        rootAndChildTypesEqual(noWsTree,
                TreeType.NODE_OBJECT,
                TreeType.TOKEN,
                TreeType.NODE_OBJECT_KVP,
                TreeType.TOKEN);
    }

    @Test
    public void nodeObjectKvp() {
        String kvp = "foo\n:\nbar";
        TokenTree kvpTree = getTree(TreeType.NODE_OBJECT_KVP, kvp);
        assertTreeIsValid(kvpTree);
        rootAndChildTypesEqual(kvpTree,
                TreeType.NODE_OBJECT_KVP,
                TreeType.NODE_OBJECT_KEY,
                TreeType.WS,
                TreeType.TOKEN,
                TreeType.WS,
                TreeType.NODE_VALUE);

        String noWs = "foo:bar";
        TokenTree noWsTree = getTree(TreeType.NODE_OBJECT_KVP, noWs);
        assertTreeIsValid(noWsTree);
        rootAndChildTypesEqual(noWsTree,
                TreeType.NODE_OBJECT_KVP,
                TreeType.NODE_OBJECT_KEY,
                TreeType.TOKEN,
                TreeType.NODE_VALUE);
    }

    @Test
    public void nodeObjectKey() {
        String quoted = "\"foo bar\"";
        TokenTree quotedTree = getTree(TreeType.NODE_OBJECT_KEY, quoted);
        assertTreeIsValid(quotedTree);
        rootAndChildTypesEqual(quotedTree, TreeType.NODE_OBJECT_KEY, TreeType.QUOTED_TEXT);

        String identifier = "foo";
        TokenTree idTree = getTree(TreeType.NODE_OBJECT_KEY, identifier);
        assertTreeIsValid(idTree);
        rootAndChildTypesEqual(idTree, TreeType.NODE_OBJECT_KEY, TreeType.IDENTIFIER);
    }

    @Test
    public void nodeStringValue() {
        String identifier = "foo";
        TokenTree idTree = getTree(TreeType.NODE_STRING_VALUE, identifier);
        assertTreeIsValid(idTree);
        rootAndChildTypesEqual(idTree, TreeType.NODE_STRING_VALUE, TreeType.SHAPE_ID);

        /*
         TODO: Right now grammar is ambiguous, no way to tell if its an identifier or shape id.
         String shapeId = "com.foo#Bar$baz";
         TokenTree shapeIdTree = getTree(TreeType.NODE_STRING_VALUE, shapeId);
         rootAndChildTypesEqual(shapeIdTree, TreeType.NODE_STRING_VALUE, TreeType.SHAPE_ID);
        */

        String quoted = "\"foo bar\"";
        TokenTree quotedTree = getTree(TreeType.NODE_STRING_VALUE, quoted);
        assertTreeIsValid(quotedTree);
        rootAndChildTypesEqual(quotedTree, TreeType.NODE_STRING_VALUE, TreeType.QUOTED_TEXT);

        String block = "\"\"\"\nfoo\"\"\"";
        TokenTree blockTree = getTree(TreeType.NODE_STRING_VALUE, block);
        assertTreeIsValid(blockTree);
        rootAndChildTypesEqual(blockTree, TreeType.NODE_STRING_VALUE, TreeType.TEXT_BLOCK);
    }

    @Test
    public void textBlock() {
        String empty = "\"\"\"\n\"\"\"";
        TokenTree emptyTree = getTree(TreeType.TEXT_BLOCK, empty);
        assertTreeIsValid(emptyTree);
        rootAndChildTypesEqual(emptyTree, TreeType.TEXT_BLOCK, TreeType.TOKEN);
        
        String withQuotes = "\"\"\"\n\"\"foo\"\n\"\"bar\"\"\"";
        TokenTree withQuotesTree = getTree(TreeType.TEXT_BLOCK, withQuotes);
        assertTreeIsValid(withQuotesTree);
        rootAndChildTypesEqual(withQuotesTree, TreeType.TEXT_BLOCK, TreeType.TOKEN);
    }

    @Test
    public void invalidControlSection() {
        String invalidTrailing = "$foo: bar\n$";
        TokenTree invalidTrailingTree = getTree(TreeType.CONTROL_SECTION, invalidTrailing);
        assertTreeIsInvalid(invalidTrailingTree);

        String invalidLeading = "$foo: \nbar\n$foo: bar\n";
        TokenTree invalidLeadingTree = getTree(TreeType.CONTROL_SECTION, invalidLeading);
        assertTreeIsInvalid(invalidLeadingTree);

        String multipleInvalid = "$foo: bar\n$1\n#foo: bar\n$foo = bar\n$foo: bar\n";
        TokenTree multipleInvalidTree = getTree(TreeType.CONTROL_SECTION, multipleInvalid);
        assertTreeIsInvalid(multipleInvalidTree);
    }

    @Test
    public void invalidControlStatement() {
        String missingDollar = "version: 2\n";
        TokenTree missingDollarTree = getTree(TreeType.CONTROL_STATEMENT, missingDollar);
        assertTreeIsInvalid(missingDollarTree);

        String missingColon = "$version 2\n";
        TokenTree missingColonTree = getTree(TreeType.CONTROL_STATEMENT, missingColon);
        assertTreeIsInvalid(missingColonTree);

        String notDollar = "=version: 2\n";
        TokenTree notDollarTree = getTree(TreeType.CONTROL_STATEMENT, notDollar);
        assertTreeIsInvalid(notDollarTree);

        String notColon = "$version = 2\n";
        TokenTree notColonTree = getTree(TreeType.CONTROL_STATEMENT, notColon);
        assertTreeIsInvalid(notColonTree);
    }

    @Test
    public void invalidMetadataSection() {
        String invalidTrailing = "metadata foo = bar\nmetadata = bar\n";
        TokenTree invalidTrailingTree = getTree(TreeType.METADATA_SECTION, invalidTrailing);
        assertTreeIsInvalid(invalidTrailingTree);

        String invalidLeading = "metadata foo =\nbar\nmetadata = bar\n";
        TokenTree invalidLeadingTree = getTree(TreeType.METADATA_SECTION, invalidLeading);
        assertTreeIsInvalid(invalidLeadingTree);

        String multipleInvalid = "metadata foo = bar\nmetadata = \nfoo\nmetadata bar: baz\nmetadata baz = qux\n";
        TokenTree multipleInvalidTree = getTree(TreeType.METADATA_SECTION, multipleInvalid);
        assertTreeIsInvalid(multipleInvalidTree);
    }

    @Test
    public void invalidMetadataStatement() {
        String missingKeyword = "foo = bar\n";
        TokenTree missingKeywordTree = getTree(TreeType.METADATA_STATEMENT, missingKeyword);
        assertTreeIsInvalid(missingKeywordTree);

        String missingEquals = "metadata foo bar\n";
        TokenTree missingEqualsTree = getTree(TreeType.METADATA_STATEMENT, missingEquals);
        assertTreeIsInvalid(missingEqualsTree);

        String notEquals = "metadata foo: bar\n";
        TokenTree notEqualsTree = getTree(TreeType.METADATA_STATEMENT, notEquals);
        assertTreeIsInvalid(notEqualsTree);
    }

    @Test
    public void invalidNodeValue() {
        String notNodeValue = "$";
        TokenTree tree = getTree(TreeType.NODE_VALUE, notNodeValue);
        assertTreeIsInvalid(tree);
    }

    @Test
    public void invalidNodeArray() {
        String missingOpenBracket = "foo, bar]";
        TokenTree missingOpenBracketTree = getTree(TreeType.NODE_ARRAY, missingOpenBracket);
        assertTreeIsInvalid(missingOpenBracketTree);

        String missingCloseBracket = "[foo, bar";
        TokenTree missingCloseBracketTree = getTree(TreeType.NODE_ARRAY, missingCloseBracket);
        assertTreeIsInvalid(missingCloseBracketTree);

        String missingBrackets = "foo, bar";
        TokenTree missingBracketsTree = getTree(TreeType.NODE_OBJECT, missingBrackets);
        assertTreeIsInvalid(missingBracketsTree);

        String notBrackets = "{foo, bar}";
        TokenTree notBracketsTree = getTree(TreeType.NODE_OBJECT, notBrackets);
        assertTreeIsInvalid(notBracketsTree);
    }

    @Test
    public void invalidNodeObject() {
        String missingOpenBrace = "foo: bar}";
        TokenTree missingOpenBraceTree = getTree(TreeType.NODE_OBJECT, missingOpenBrace);
        assertTreeIsInvalid(missingOpenBraceTree);

        String missingCloseBrace = "{foo: bar";
        TokenTree missingCloseBraceTree = getTree(TreeType.NODE_OBJECT, missingCloseBrace);
        assertTreeIsInvalid(missingCloseBraceTree);

        String missingBraces = "foo: bar";
        TokenTree missingBracesTree = getTree(TreeType.NODE_OBJECT, missingBraces);
        assertTreeIsInvalid(missingBracesTree);

        String notBraces = "[foo: bar]";
        TokenTree notBracesTree = getTree(TreeType.NODE_ARRAY, notBraces);
        assertTreeIsInvalid(notBracesTree);
    }

    @Test
    public void invalidNodeObjectKvp() {
        String notColon = "foo = bar";
        TokenTree notColonTree = getTree(TreeType.NODE_OBJECT_KVP, notColon);
        assertTreeIsInvalid(notColonTree);

        String missingColon = "foo bar";
        TokenTree missingColonTree = getTree(TreeType.NODE_OBJECT_KVP, missingColon);
        assertTreeIsInvalid(missingColonTree);
    }

    @Test
    public void invalidNodeObjectKey() {
        String invalid = "1";
        TokenTree tree = getTree(TreeType.NODE_OBJECT_KEY, invalid);
        assertTreeIsInvalid(tree);
    }

    @Test
    public void invalidNamespaceStatement() {
        String missingKeyword = " foo.bar\n";
        TokenTree missingKeywordTree = getTree(TreeType.NAMESPACE_STATEMENT, missingKeyword);
        assertTreeIsInvalid(missingKeywordTree);
    }

    @Test
    public void invalidUseSection() {
        String invalidTrailing = "use com.foo#Bar\nuse \ncom.foo#Bar\n";
        TokenTree invalidTrailingTree = getTree(TreeType.USE_SECTION, invalidTrailing);
        assertTreeIsInvalid(invalidTrailingTree);

        String invalidLeading = "use\n com.foo#Bar\nuse com.foo#Bar\n";
        TokenTree invalidLeadingTree = getTree(TreeType.USE_SECTION, invalidLeading);
        assertTreeIsInvalid(invalidLeadingTree);

        String multipleInvalid = "use com.foo#Bar\nuse\ncom.foo#Bar\nuse #Bar\nuse com.foo#Bar\n";
        TokenTree multipleInvalidTree = getTree(TreeType.USE_SECTION, multipleInvalid);
        assertTreeIsInvalid(multipleInvalidTree);
    }

    @Test
    public void invalidUseStatement() {
        String missingKeyword = " foo.bar#Baz\n";
        TokenTree missingKeywordTree = getTree(TreeType.USE_STATEMENT, missingKeyword);
        assertTreeIsInvalid(missingKeywordTree);
    }

    @Test
    public void invalidShapeStatements() {
        String incompleteShape = "string Foo\nstructure Bar {\nfoo: Foo\n";
        TokenTree incompleteShapeTree = getTree(TreeType.SHAPE_STATEMENTS, incompleteShape);
        assertTreeIsInvalid(incompleteShapeTree);

        String firstInvalid = "string Foo {}\nstructure Bar{}\n";
        TokenTree firstInvalidTree = getTree(TreeType.SHAPE_STATEMENTS, firstInvalid);
        assertTreeIsInvalid(firstInvalidTree);

        String trailingTraits = "structure Foo {}\n@bar\n@baz(foo: bar)\n";
        TokenTree trailingTraitsTree = getTree(TreeType.SHAPE_STATEMENTS, trailingTraits);
        assertTreeIsInvalid(trailingTraitsTree);
    }

    @Test
    public void invalidShapeOrApplyStatement() {
        // SHAPE_OR_APPLY_STATEMENT checks whether it's an apply or shape statement,
        // and shape types are checked before that type is parsed, so this test
        // covers all invalid shape type name/apply cases.

        String missingTypeName = " Foo with [Bar] {}";
        TokenTree missingTypeNameTree = getTree(TreeType.SHAPE_STATEMENT, missingTypeName);
        assertTreeIsInvalid(missingTypeNameTree);

        String wrongTypeName = "unknown Foo with [Bar] {}";
        TokenTree wrongTypeNameTree = getTree(TreeType.SHAPE_STATEMENT, wrongTypeName);
        assertTreeIsInvalid(wrongTypeNameTree);
    }
    @Test
    public void invalidMixins() {
        String missingOpenBracket = "with foo, bar]";
        TokenTree missingOpenBracketTree = getTree(TreeType.MIXINS, missingOpenBracket);
        assertTreeIsInvalid(missingOpenBracketTree);

        String missingCloseBracket = "with [foo, bar";
        TokenTree missingCloseBracketTree = getTree(TreeType.MIXINS, missingCloseBracket);
        assertTreeIsInvalid(missingCloseBracketTree);

        String missingBrackets = "with foo, bar";
        TokenTree missingBracketsTree = getTree(TreeType.MIXINS, missingBrackets);
        assertTreeIsInvalid(missingBracketsTree);

        String notBrackets = "with {foo, bar}";
        TokenTree notBracketsTree = getTree(TreeType.MIXINS, notBrackets);
        assertTreeIsInvalid(notBracketsTree);
    }

    @Test
    public void invalidEnumShapeMembers() {
        String missingOpenBrace = "FOO }";
        TokenTree missingOpenBraceTree = getTree(TreeType.ENUM_SHAPE_MEMBERS, missingOpenBrace);
        assertTreeIsInvalid(missingOpenBraceTree);

        String missingCloseBrace = "{FOO";
        TokenTree missingCloseBraceTree = getTree(TreeType.ENUM_SHAPE_MEMBERS, missingCloseBrace);
        assertTreeIsInvalid(missingCloseBraceTree);

        String missingBraces = "FOO";
        TokenTree missingBracesTree = getTree(TreeType.ENUM_SHAPE_MEMBERS, missingBraces);
        assertTreeIsInvalid(missingBracesTree);

        String notBraces = "[FOO]";
        TokenTree notBracesTree = getTree(TreeType.ENUM_SHAPE_MEMBERS, notBraces);
        assertTreeIsInvalid(notBracesTree);

        String leadingInvalidMember = "{\n1\nFOO\n}";
        TokenTree leadingInvalidMemberTree = getTree(TreeType.ENUM_SHAPE_MEMBERS, leadingInvalidMember);
        assertTreeIsInvalid(leadingInvalidMemberTree);

        String trailingInvalidMember = "{\nFOO\n1\n}";
        TokenTree trailingInvalidMemberTree = getTree(TreeType.ENUM_SHAPE_MEMBERS, trailingInvalidMember);
        assertTreeIsInvalid(trailingInvalidMemberTree);

        String multipleInvalidMembers = "{\nFOO\nBAR = \n @foo(\nBAR\n = 2\nFOO\n}";
        TokenTree multipleInvalidMembersTree = getTree(TreeType.ENUM_SHAPE_MEMBERS, multipleInvalidMembers);
        assertTreeIsInvalid(multipleInvalidMembersTree);

        String trailingTraits = "{\nFOO\n@bar\n@baz\n}";
        TokenTree trailingTraitsTree = getTree(TreeType.ENUM_SHAPE_MEMBERS, trailingTraits);
        assertTreeIsInvalid(trailingTraitsTree);
    }

    @Test
    public void invalidValueAssignment() {
        String missingEquals = "1\n";
        TokenTree missingEqualsTree = getTree(TreeType.VALUE_ASSIGNMENT, missingEquals);
        assertTreeIsInvalid(missingEqualsTree);

        String notEquals = " + 1\n";
        TokenTree notEqualsTree = getTree(TreeType.VALUE_ASSIGNMENT, notEquals);
        assertTreeIsInvalid(notEqualsTree);
    }

    @Test
    public void invalidForResource() {
        String missingFor = " foo.bar#Baz";
        TokenTree missingForTree = getTree(TreeType.FOR_RESOURCE, missingFor);
        assertTreeIsInvalid(missingForTree);
    }

    @Test
    public void invalidShapeMembers() {
        String missingOpenBrace = "foo: bar }";
        TokenTree missingOpenBraceTree = getTree(TreeType.SHAPE_MEMBERS, missingOpenBrace);
        assertTreeIsInvalid(missingOpenBraceTree);

        String missingCloseBrace = "{foo: bar";
        TokenTree missingCloseBraceTree = getTree(TreeType.SHAPE_MEMBERS, missingCloseBrace);
        assertTreeIsInvalid(missingCloseBraceTree);

        String missingBraces = "foo: bar";
        TokenTree missingBracesTree = getTree(TreeType.SHAPE_MEMBERS, missingBraces);
        assertTreeIsInvalid(missingBracesTree);

        String notBraces = "[foo: bar]";
        TokenTree notBracesTree = getTree(TreeType.SHAPE_MEMBERS, notBraces);
        assertTreeIsInvalid(notBracesTree);

        String leadingInvalidMember = "{\nfoo: 1\nbar: baz\n}";
        TokenTree leadingInvalidMemberTree = getTree(TreeType.SHAPE_MEMBERS, leadingInvalidMember);
        assertTreeIsInvalid(leadingInvalidMemberTree);

        String trailingInvalidMember = "{\nfoo: bar\nbaz:\n}";
        TokenTree trailingInvalidMemberTree = getTree(TreeType.SHAPE_MEMBERS, trailingInvalidMember);
        assertTreeIsInvalid(trailingInvalidMemberTree);

        String multipleInvalidMembers = "{\nfoo: @foo({})\nfoo = com.foo#Bar\n}";
        TokenTree multipleInvalidMembersTree = getTree(TreeType.SHAPE_MEMBERS, multipleInvalidMembers);
        assertTreeIsInvalid(multipleInvalidMembersTree);

        String trailingTraits = "{\nfoo: bar\n@foo\n@bar\n}";
        TokenTree trailingTraitsTree = getTree(TreeType.SHAPE_MEMBERS, trailingTraits);
        assertTreeIsInvalid(trailingTraitsTree);
    }

    @Test
    public void invalidExplicitShapeMember() {
        String missingColon = "foo Bar";
        TokenTree missingColonTree = getTree(TreeType.EXPLICIT_SHAPE_MEMBER, missingColon);
        assertTreeIsInvalid(missingColonTree);

        String notColon = "foo = Bar";
        TokenTree notColonTree = getTree(TreeType.EXPLICIT_SHAPE_MEMBER, notColon);
        assertTreeIsInvalid(notColonTree);
    }

    @Test
    public void invalidElidedShapeMember() {
        String missingDollar = "Foo";
        TokenTree missingDollarTree = getTree(TreeType.ELIDED_SHAPE_MEMBER, missingDollar);
        assertTreeIsInvalid(missingDollarTree);

        String notDollar = "#Foo";
        TokenTree notDollarTree = getTree(TreeType.ELIDED_SHAPE_MEMBER, notDollar);
        assertTreeIsInvalid(notDollarTree);
    }

    @Test
    public void invalidOperationProperty() {
        // This production determines which kind of operation property is present,
        // so these cases cover unexpected/invalid property names.

        String missingPropertyName = " : FooInput";
        TokenTree missingPropertyNameTree = getTree(TreeType.OPERATION_PROPERTY, missingPropertyName);
        assertTreeIsInvalid(missingPropertyNameTree);

        String wrongPropertyName = "unknown: FooInput";
        TokenTree wrongPropertyNameTree = getTree(TreeType.OPERATION_PROPERTY, wrongPropertyName);
        assertTreeIsInvalid(wrongPropertyNameTree);
    }

    @Test
    public void invalidOperationInput() {
        String missingColon = "input Foo";
        TokenTree missingColonTree = getTree(TreeType.OPERATION_INPUT, missingColon);
        assertTreeIsInvalid(missingColonTree);

        String notColon = "input = Foo";
        TokenTree notColonTree = getTree(TreeType.OPERATION_INPUT, notColon);
        assertTreeIsInvalid(notColonTree);

        String missingValue = "input: ";
        TokenTree missingValueTree = getTree(TreeType.OPERATION_INPUT, missingValue);
        assertTreeIsInvalid(missingValueTree);
    }

    @Test
    public void invalidOperationOutput() {
        String missingColon = "output Foo";
        TokenTree missingColonTree = getTree(TreeType.OPERATION_OUTPUT, missingColon);
        assertTreeIsInvalid(missingColonTree);

        String notColon = "output = Foo";
        TokenTree notColonTree = getTree(TreeType.OPERATION_OUTPUT, notColon);
        assertTreeIsInvalid(notColonTree);

        String missingValue = "output: ";
        TokenTree missingValueTree = getTree(TreeType.OPERATION_OUTPUT, missingValue);
        assertTreeIsInvalid(missingValueTree);
    }

    @Test
    public void invalidOperationErrors() {
        String missingColon = "errors Foo";
        TokenTree missingColonTree = getTree(TreeType.OPERATION_ERRORS, missingColon);
        assertTreeIsInvalid(missingColonTree);


        String notColon = "errors = Foo";
        TokenTree notColonTree = getTree(TreeType.OPERATION_ERRORS, notColon);
        assertTreeIsInvalid(notColonTree);

        String missingValue = "errors: ";
        TokenTree missingValueTree = getTree(TreeType.OPERATION_ERRORS, missingValue);
        assertTreeIsInvalid(missingValueTree);

        String missingOpenBracket = "errors: Foo, Bar]";
        TokenTree missingOpenBracketTree = getTree(TreeType.OPERATION_ERRORS, missingOpenBracket);
        assertTreeIsInvalid(missingOpenBracketTree);

        String missingCloseBracket = "errors: [Foo, Bar";
        TokenTree missingCloseBracketTree = getTree(TreeType.OPERATION_ERRORS, missingCloseBracket);
        assertTreeIsInvalid(missingCloseBracketTree);

        String missingBrackets = "errors: Foo, Bar";
        TokenTree missingBracketsTree = getTree(TreeType.OPERATION_ERRORS, missingBrackets);
        assertTreeIsInvalid(missingBracketsTree);

        String notBrackets = "errors: {Foo, Bar}";
        TokenTree notBracketsTree = getTree(TreeType.OPERATION_ERRORS, notBrackets);
        assertTreeIsInvalid(notBracketsTree);
    }

    @Test
    public void invalidInlineAggregateShape() {
        String missingWalrus = " @foo\n for Bar with [Baz] {}";
        TokenTree missingWalrusTree = getTree(TreeType.INLINE_AGGREGATE_SHAPE, missingWalrus);
        assertTreeIsInvalid(missingWalrusTree);

        String notWalrus = "= @foo\n for bar with [Baz] {}";
        TokenTree notWalrusTree = getTree(TreeType.INLINE_AGGREGATE_SHAPE, notWalrus);
        assertTreeIsInvalid(notWalrusTree);
    }

    @Test
    public void invalidTraitStatements() {
        String incomplete = "@foo({bar: baz}\n";
        TokenTree incompleteTree = getTree(TreeType.TRAIT_STATEMENTS, incomplete);
        assertTreeIsInvalid(incompleteTree);

        String leadingInvalid = "@foo(:)\n@bar\n";
        TokenTree leadingInvalidTree = getTree(TreeType.TRAIT_STATEMENTS, leadingInvalid);
        assertTreeIsInvalid(leadingInvalidTree);

        String trailingInvalid = "@foo\n@bar(\n";
        TokenTree trailingInvalidTree = getTree(TreeType.TRAIT_STATEMENTS, trailingInvalid);
        assertTreeIsInvalid(trailingInvalidTree);

        String multipleInvalid = "@foo\n@\nbaz\n@foo({\n@bar\n";
        TokenTree multipleInvalidTree = getTree(TreeType.TRAIT_STATEMENTS, multipleInvalid);
        assertTreeIsInvalid(multipleInvalidTree);
    }

    @Test
    public void invalidTrait() {
        String missingAt = "foo(bar: baz)";
        TokenTree missingAtTree = getTree(TreeType.TRAIT, missingAt);
        assertTreeIsInvalid(missingAtTree);
    }

    @Test
    public void invalidTraitBody() {
        String missingOpenParen = "foo: bar)";
        TokenTree missingOpenParenTree = getTree(TreeType.TRAIT_BODY, missingOpenParen);
        assertTreeIsInvalid(missingOpenParenTree);

        String missingCloseParen = "(foo: bar";
        TokenTree missingCloseParenTree = getTree(TreeType.TRAIT_BODY, missingCloseParen);
        assertTreeIsInvalid(missingCloseParenTree);

        String missingParens = "foo: bar";
        TokenTree missingParensTree = getTree(TreeType.TRAIT_BODY, missingParens);
        assertTreeIsInvalid(missingParensTree);

        String notParens = "{foo: bar}";
        TokenTree notParensTree = getTree(TreeType.TRAIT_BODY, notParens);
        assertTreeIsInvalid(notParensTree);
    }

    @Test
    public void invalidApplyStatementBlock() {
        String missingOpenBrace = "apply foo @bar }";
        TokenTree missingOpenBraceTree = getTree(TreeType.APPLY_STATEMENT_BLOCK, missingOpenBrace);
        assertTreeIsInvalid(missingOpenBraceTree);

        String missingCloseBrace = "apply foo { @bar";
        TokenTree missingCloseBraceTree = getTree(TreeType.APPLY_STATEMENT_BLOCK, missingCloseBrace);
        assertTreeIsInvalid(missingCloseBraceTree);

        String missingBraces = "apply foo @bar";
        TokenTree missingBracesTree = getTree(TreeType.APPLY_STATEMENT_BLOCK, missingBraces);
        assertTreeIsInvalid(missingBracesTree);

        String notBraces = "apply foo [@bar]";
        TokenTree notBracesTree = getTree(TreeType.APPLY_STATEMENT_BLOCK, notBraces);
        assertTreeIsInvalid(notBracesTree);

        String invalidTraits = "apply foo {\n@bar(\n@ baz\n}";
        TokenTree invalidTraitsTree = getTree(TreeType.APPLY_STATEMENT_BLOCK, invalidTraits);
        assertTreeIsInvalid(invalidTraitsTree);
    }

    @Test
    public void invalidAbsoluteRootShapeId() {
        String notPound = "com.foo$Bar";
        TokenTree notPoundTree = getTree(TreeType.ABSOLUTE_ROOT_SHAPE_ID, notPound);
        assertTreeIsInvalid(notPoundTree);

        String trailingPound = "com.foo#";
        TokenTree trailingPoundTree = getTree(TreeType.ABSOLUTE_ROOT_SHAPE_ID, trailingPound);
        assertTreeIsInvalid(trailingPoundTree);

        String multiPound = "com.foo##Bar";
        TokenTree multiPoundTree = getTree(TreeType.ABSOLUTE_ROOT_SHAPE_ID, multiPound);
        assertTreeIsInvalid(multiPoundTree);
    }

    @Test
    public void invalidShapeIdMember() {
        String missingIdentifier = "$";
        TokenTree missingIdentifierTree = getTree(TreeType.SHAPE_ID_MEMBER, missingIdentifier);
        assertTreeIsInvalid(missingIdentifierTree);

        String notDollar = "#Foo";
        TokenTree notDollarTree = getTree(TreeType.SHAPE_ID_MEMBER, notDollar);
        assertTreeIsInvalid(notDollarTree);
    }

    @Test
    public void invalidNamespace() {
        String trailingDot = "com.foo.";
        TokenTree trailingDotTree = getTree(TreeType.NAMESPACE, trailingDot);
        assertTreeIsInvalid(trailingDotTree);

        String multiDot = "com.foo..bar";
        TokenTree multiDotTree = getTree(TreeType.NAMESPACE, multiDot);
        assertTreeIsInvalid(multiDotTree);
    }

    @Test
    public void invalidIdentifier() {
        String leadingNumber = "1foo";
        TokenTree leadingNumberTree = getTree(TreeType.IDENTIFIER, leadingNumber);
        assertTreeIsInvalid(leadingNumberTree);

        String leadingSymbol = "@foo";
        TokenTree leadingSymbolTree = getTree(TreeType.IDENTIFIER, leadingSymbol);
        assertTreeIsInvalid(leadingSymbolTree);
    }

    @Test
    public void invalidBr() {
        // Need the "foo" at the end because EOF is a valid BR.
        String missingNewline = "\tfoo";
        TokenTree missingNewlineTree = getTree(TreeType.BR, missingNewline);
        assertTreeIsInvalid(missingNewlineTree);
    }

    @Test
    public void invalidComment() {
        String invalidSlashes = "/ / Foo";
        TokenTree invalidSlashesTree = getTree(TreeType.COMMENT, invalidSlashes);
        assertTreeIsInvalid(invalidSlashesTree);
    }

    private static void rootAndChildTypesEqual(TokenTree actualTree, TreeType expectedRoot, TreeType... expectedChildren) {
        assertEquals(expectedRoot, actualTree.getType());
        String actual = actualTree.getChildren().stream().map(t -> t.getType().toString()).collect(Collectors.joining(","));
        String expected = Arrays.stream(expectedChildren).map(Object::toString).collect(Collectors.joining(","));
        assertEquals(expected, actual);
    }

    private static void assertTreeIsValid(TokenTree tree) {
        if (tree.getType() == TreeType.ERROR) {
            Assertions.fail(() -> "Expected tree to be valid, but found error: " + tree);
        } else {
            for (TokenTree child : tree.getChildren()) {
                assertTreeIsValid(child);
            }
        }
    }

    private static void assertTreeIsInvalid(TokenTree tree) {
        TreeCursor cursor = tree.zipper();
        if (cursor.findChildrenByType(TreeType.ERROR).isEmpty()) {
            Assertions.fail(() -> "Expected tree to be invalid, but found no errors.\nFull tree:\n" + tree);
        }
    }

    private static TokenTree getTree(TreeType type, String forText) {
        IdlTokenizer tokenizer = IdlTokenizer.create(forText);
        return TokenTree.of(tokenizer, type);
    }
}
