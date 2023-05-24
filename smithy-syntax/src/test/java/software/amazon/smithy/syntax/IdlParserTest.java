package software.amazon.smithy.syntax;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.loader.IdlToken;
import software.amazon.smithy.model.loader.IdlTokenizer;

public class IdlParserTest {
    @Test
    public void createsTreeFromParser() {
        String model = "$version: \"2.0\"\n\n"
                       + "$foo: b.ar\n"
                       + "/// Foo\n"
                       + "metadata foo = \"bar\"\n"
                       + "metadata foo\n"
                       + "namespace hello.there\n\n"
                       + "use smithy.example#Abc\n"
                       + "use smithy.other#Bar\n\n"
                       + "/// Foo\n"
                       + "string Foo\n"
                       + "integer Bar\n"
                       + "@com.foo#foo\n"
                       + "document Document\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);

        TreeCursor match = tree.zipper().findAt(3, 4);
        assertThat(match.getTree().getTokens().get(0).getLexeme().toString(), equalTo("foo"));
        assertThat(match.getTree().getTokens().get(0).getIdlToken(), is(IdlToken.IDENTIFIER));
    }

    @Test
    public void parsesTraits() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "/// Foo\n"
                       + "@foo({a: true})\n"
                       + "@length(min: 1, max: 10)\n"
                       + "string Foo\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void parsesMixins() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "string Foo with [Bar, Baz]\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void parsesLists() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "@foo\n"
                       + "list Foo with [Bar, Baz] {\n"
                       + "    member: String\n"
                       + "}\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void parsesUnions() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "union Foo with [Bar, Baz] {\n"
                       + "    a: String\n"
                       + "    bar: smithy.api#Number,\n"
                       + "    /// Hello, S\n"
                       + "    bam: S\n"
                       + "    @deprecated\n"
                       + "    boo: S }\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void parsesStructures() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "structure Foo with [Bar, Baz] {\n"
                       + "    a: String\n"
                       + "    bar: smithy.api#Number,\n"
                       + "    /// Hello, S\n"
                       + "    bam: S\n"
                       + "    @deprecated\n"
                       + "    boo: S }\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void parsesStructuresWithDefaults() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "structure Foo with [Bar, Baz] {\n"
                       + "    a: String = \"abc\"\n"
                       + "    bar: smithy.api#Number,\n"
                       + "    bam: S = []\n"
                       + "}\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void parsesStructuresWithForResource() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "structure Foo for Resource with [Bar, Baz] {}\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void parsesSingularApplyStatement() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "apply Foo @sensitive\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void parsesBlockApplyStatement() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "apply Foo {\n"
                       + "  @sensitive\n"
                       + "  @bam\n"
                       + "}\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void parsesServiceAndResource() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "service Foo {\n"
                       + "  version: \"1.0\",\n"
                       + "  resources: [Baz]\n"
                       + "}\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void parsesExplicitMaps() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "map Foo {\n"
                       + "  key: String\n"
                       + "  value: String\n"
                       + "}\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void parsesElidedMaps() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "map Foo with[FooPrime]{\n"
                       + "  $key,\n"
                       + "  $value,\n"
                       + "}\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void detectsMapOrderIssue() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "map Foo with[FooPrime]{\n"
                       + "  $value,\n"
                       + "  $key,\n"
                       + "}\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void detectsDuplicateMapMemberIssue() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "map Foo with[FooPrime]{\n"
                       + "  $key,\n"
                       + "  $key,\n"
                       + "}\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void parsesEnum() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "enum Foo {\n"
                       + "  hello = \"HELLO\",\n"
                       + "  there\n"
                       + "}\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void parsesIntEnum() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "intEnum Foo {\n"
                       + "  hello = 1\n\n"
                       + "  @enumValue(2)\n"
                       + "  there"
                       + "}\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void parsesOperationWithShapeIds() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "operation Foo {\n"
                       + "  input: FooInput,\n"
                       + "  output: FooOutput\n"
                       + "  errors: [A, B]"
                       + "}\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }

    @Test
    public void parsesOperationWithInlineStructs() {
        String model = "$version: \"2.0\"\n\n"
                       + "namespace hello.there\n\n"
                       + "operation Foo {\n"
                       + "  input := {}\n"
                       + "  output := for FooResource {\n"
                       + "    bar: String\n"
                       + "  }"
                       + "}\n";
        IdlTokenizer tokenizer = IdlTokenizer.create("example.smithy", model);
        TokenTree tree = TokenTree.parse(tokenizer);

        System.out.println(tree);
    }
}
