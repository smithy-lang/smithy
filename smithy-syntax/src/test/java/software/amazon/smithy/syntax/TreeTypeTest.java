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
        rootAndChildTypesEqual(idTree, TreeType.NODE_STRING_VALUE, TreeType.IDENTIFIER);

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

    private static TokenTree getTree(TreeType type, String forText) {
        CapturingTokenizer tokenizer = new CapturingTokenizer(IdlTokenizer.create(forText));
        type.parse(tokenizer);
        // The root of the tree is always IDL with children appended, so the first child is the one we want.
        return tokenizer.getRoot().getChildren().get(0);
    }
}
