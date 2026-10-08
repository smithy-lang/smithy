/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.smithy.cli.CliUtils;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.synthetic.SyntheticShapeTrait;

class InlineCollectionMigrationEligibilityTest {
    private static final String HEADER = "$version: \"2.1\"\nnamespace smithy.example\n\n";

    @TempDir
    Path directory;

    @ParameterizedTest
    @ValueSource(strings = {"--infer-inline-collections", "--force-inline-collections"})
    void checksExistingMemberTraitsWithoutBlockingEligibleReferences(String option) throws IOException {
        String contents = HEADER + """
                @trait(selector: "member :test(> list :not([trait|synthetic]))")
                structure namedOnly {}
                @trait(selector: "member :test(> list)")
                structure listOnly {}
                list Strings { member: String }
                structure Input {
                    @namedOnly
                    named: Strings
                    @listOnly
                    inline: Strings
                    @unknown
                    unknown: Strings
                    plain: Strings
                }
                """;
        Path path = write("model.smithy", contents);

        migrate("migrate", option, "--allow-unknown-traits", path.toString());

        assertThat(Files.readString(path),
                equalTo(contents
                        .replace("inline: Strings", "inline: [String]")
                        .replace("unknown: Strings", "unknown: [String]")
                        .replace("plain: Strings", "plain: [String]")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"--infer-inline-collections", "--force-inline-collections"})
    void preservesPrivateElementVisibilityAcrossNamespaces(String option) throws IOException {
        Path definitions = write("definitions.smithy", """
                $version: "2.1"
                namespace smithy.a
                @private
                string Secret
                list Secrets { member: Secret }
                list Outer { member: Secrets }
                """);
        Path references = write("references.smithy", HEADER + """
                use smithy.a#Secrets
                use smithy.a#Outer
                structure Input {
                    secrets: Secrets
                    outer: Outer
                }
                """);

        migrate("migrate", option, definitions.toString(), references.toString());

        assertThat(Files.readString(references), equalTo(HEADER + """
                use smithy.a#Secrets
                use smithy.a#Outer
                structure Input {
                    secrets: Secrets
                    outer: [Secrets]
                }
                """));
        Model.assembler().addImport(definitions).addImport(references).assemble().unwrap();
    }

    @Test
    void keepsPrivateCollectionsNamedEvenWhenForced() throws IOException {
        String contents = HEADER + """
                @private
                @length(min: 1)
                list Strings { member: String }
                structure Input { names: Strings }
                """;
        Path path = write("model.smithy", contents);

        migrate("migrate", "--force-inline-collections", path.toString());

        assertThat(Files.readString(path), equalTo(contents));
    }

    @ParameterizedTest
    @CsvSource({
            "--infer-inline-collections, names: Strings",
            "--force-inline-collections, names: Strings",
            "--infer-inline-collections, @property(name: \"names\") renamed: Strings",
            "--force-inline-collections, @property(name: \"names\") renamed: Strings"
    })
    void preservesResourcePropertiesAndRenamedBindings(String option, String boundMember) throws IOException {
        String contents = HEADER + """
                list Strings { member: String }
                resource Thing {
                    identifiers: { id: String }
                    properties: { names: Strings }
                    read: Read
                }
                @readonly
                operation Read {
                    input := { @required id: String }
                    output := {
                        %s
                        @notProperty
                        extra: Strings
                    }
                }
                structure Input { names: Strings }
                """.formatted(boundMember);
        Path path = write("model.smithy", contents);

        migrate("migrate", option, path.toString());

        assertThat(Files.readString(path),
                equalTo(contents
                        .replace("extra: Strings", "extra: [String]")
                        .replace("structure Input { names: Strings }", "structure Input { names: [String] }")));
        Model.assembler().addImport(path).assemble().unwrap();
    }

    @ParameterizedTest
    @ValueSource(strings = {"--infer-inline-collections", "--force-inline-collections"})
    void preservesNestedResourcePropertiesAndSharedMixinTargets(String option) throws IOException {
        String contents = HEADER + """
                list Strings { member: String }
                @mixin
                structure Common {
                    names: Strings
                }
                structure Properties with [Common] {}
                resource Thing {
                    identifiers: { id: String }
                    properties: { names: Strings }
                    read: Read
                }
                @readonly
                operation Read {
                    input := { @required id: String }
                    output := {
                        @nestedProperties
                        payload: Properties
                    }
                }
                structure Input { names: Strings }
                """;
        Path path = write("model.smithy", contents);

        migrate("migrate", option, path.toString());

        assertThat(Files.readString(path),
                equalTo(contents
                        .replace("structure Input { names: Strings }", "structure Input { names: [String] }")));
        Model.assembler().addImport(path).assemble().unwrap();
    }

    @Test
    void protectsResourceBindingsDeclaredWithTargetElision() throws IOException {
        String contents = HEADER + """
                list Strings { member: String }
                @mixin
                structure Common { names: Strings }
                structure ReadOutput with [Common] { @property(name: "names") $names }
                resource Thing {
                    identifiers: { id: String }
                    properties: { names: Strings }
                    read: Read
                }
                @readonly
                operation Read {
                    input := { @required id: String }
                    output: ReadOutput
                }
                structure Input { names: Strings }
                """;
        Path path = write("model.smithy", contents);

        migrate("migrate", "--infer-inline-collections", path.toString());

        assertThat(Files.readString(path),
                equalTo(contents
                        .replace("structure Input { names: Strings }", "structure Input { names: [String] }")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"--infer-inline-collections", "--force-inline-collections"})
    void checksTraitsAppliedToInheritedMembers(String option) throws IOException {
        String contents = HEADER + """
                @trait(selector: "member :test(> list :not([trait|synthetic]))")
                structure namedOnly {}
                list Strings { member: String }
                @mixin
                structure Common { names: Strings }
                structure Derived with [Common] {}
                apply Derived$names @namedOnly
                structure Input { names: Strings }
                """;
        Path path = write("model.smithy", contents);

        migrate("migrate", option, path.toString());

        assertThat(Files.readString(path),
                equalTo(contents
                        .replace("structure Input { names: Strings }", "structure Input { names: [String] }")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "@unknownThing\nstructure Unrelated with [Mixed] {}",
            "structure Unrelated with [Mixed] { @unknownThing $other }",
            "structure Unrelated with [Mixed] { @unknownThing $names }"
    })
    void toleratesUnknownTraitsReplayedByMixinLoading(String unrelated) throws IOException {
        Path path = write("model.smithy", HEADER + """
                @length(min: 1)
                list Strings { member: String }
                @mixin
                structure Mixed {
                    other: String
                    names: Strings
                }
                %s
                structure Input { names: Strings }
                """.formatted(unrelated));

        migrate("migrate", "--allow-unknown-traits", "--force-inline-collections", path.toString());

        Model model = Model.assembler()
                .putProperty(software.amazon.smithy.model.loader.ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                .addImport(path)
                .assemble()
                .unwrap();
        assertThat(model.expectShape(member(model, "Input$names").getTarget()).hasTrait(SyntheticShapeTrait.ID),
                equalTo(true));
    }

    @Test
    void evaluatesSelectorsWithoutTheInternalProbeReferrer() throws IOException {
        Path path = write("model.smithy", HEADER + """
                @trait(selector: ":is(list, member :not(:test(> list < member[id|member=value])))")
                structure noValueReferrer {}
                @noValueReferrer
                list Strings { member: String }
                structure Input {
                    names: Strings
                }
                """);

        migrate("migrate", "--force-inline-collections", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(model.expectShape(member(model, "Input$names").getTarget()).hasTrait(SyntheticShapeTrait.ID),
                equalTo(true));
    }

    @Test
    void writesCanonicalTraitSyntaxIncludingDocumentationAndAnnotations() throws IOException {
        String collection = """
                /// Collection documentation
                @length(min: 1)
                @unstable
                list Strings { member: String }
                """;
        Path path = write("model.smithy", HEADER + collection + """
                structure Input {
                    names: Strings
                }
                """);

        migrate("migrate", "--force-inline-collections", path.toString());

        assertThat(Files.readString(path), equalTo(HEADER + collection + """
                structure Input {
                    /// Collection documentation
                    @length(
                        min: 1
                    )
                    @unstable
                    names: [String]
                }
                """));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n"})
    void copiesTraitsWithTheMembersLineEndingsAndPreservesIndexes(String newline) throws IOException {
        Path idx = write("idx.smithy", """
                $version: "2.1"
                namespace smithy.protocols
                @trait(selector: ":is(structure, union) > member")
                integer idx
                """);
        String header = HEADER.replace("\n", "\r\n");
        String collection = "@length(min: 1)\nlist Strings { member: String }\n".replace("\n", newline);
        String input = """
                structure Input {
                    1. names: Strings // Keep this comment
                }
                """.replace("\n", newline);
        Path path = write("model.smithy", header + collection + input);

        migrate("migrate", "--force-inline-collections", path.toString(), idx.toString());

        assertThat(Files.readString(path), equalTo(header + collection + """
                structure Input {
                    @length(
                        min: 1
                    )
                    1. names: [String] // Keep this comment
                }
                """.replace("\n", newline)));
    }

    @Test
    void usesTheDeclaringMixinMemberWhenSourceLocationsAreShared() throws IOException {
        Path path = write("model.smithy", HEADER + """
                @length(min: 1)
                list Strings { member: String }
                @mixin
                structure Common {
                    names: Strings
                }
                structure Input with [Common] {}
                apply Input$names @length(min: 2)
                """);

        migrate("migrate", "--force-inline-collections", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(member(model, "Common$names").expectTrait(LengthTrait.class).getMin().get(), equalTo(1L));
        assertThat(member(model, "Input$names").expectTrait(LengthTrait.class).getMin().get(), equalTo(2L));
        assertThat(model.expectShape(member(model, "Input$names").getTarget()).hasTrait(SyntheticShapeTrait.ID),
                equalTo(true));
    }

    @Test
    void retainsExistingNoPositionalArgumentScopeIncludingConfiguredImports() throws IOException {
        Path imported = write("imported.smithy", HEADER + """
                list Strings { member: String }
                structure Imported { names: Strings }
                """);
        Path source = write("source.smithy", HEADER + "structure Input { names: Strings }\n");
        Path config = write("smithy-build.json",
                Node.prettyPrintJson(Node.objectNodeBuilder()
                        .withMember("version", "1.0")
                        .withMember("sources", ArrayNode.fromStrings(source.toString()))
                        .withMember("imports", ArrayNode.fromStrings(imported.toString()))
                        .build()));

        migrate("migrate", "--config", config.toString(), "--infer-inline-collections");

        assertThat(Files.readString(imported), equalTo(HEADER + """
                list Strings { member: String }
                structure Imported { names: [String] }
                """));
        assertThat(Files.readString(source), equalTo(HEADER + "structure Input { names: [String] }\n"));
    }

    @Test
    void skipsNewTraitConflictsAtOneSiteWithoutBlockingOtherReferences() throws IOException {
        Path path = write("model.smithy", HEADER + """
                @trait(selector: ":is(list, member)", conflicts: [local])
                structure copied {}
                @trait(selector: "member")
                structure local {}
                @copied
                list Strings { member: String }
                structure Input {
                    @local
                    named: Strings
                    inline: Strings
                }
                """);

        migrate("migrate", "--force-inline-collections", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(member(model, "Input$named").getTarget(), equalTo(ShapeId.from("smithy.example#Strings")));
        assertThat(model.expectShape(member(model, "Input$inline").getTarget()).hasTrait(SyntheticShapeTrait.ID),
                equalTo(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"--infer-inline-collections", "--force-inline-collections"})
    void checksTraitsOnAncestorsOfEditedNamedCollections(String option) throws IOException {
        String contents = HEADER + """
                @trait(selector: "member :test(> list > member > map :not([trait|synthetic]))")
                structure innerMapNamed {}
                map Tags { key: String, value: String }
                list TagsList { member: Tags }
                structure Input { @innerMapNamed rows: TagsList }
                structure Control { tags: Tags }
                """;
        Path path = write("model.smithy", contents);

        migrate("migrate", option, path.toString());

        assertThat(Files.readString(path), equalTo(contents.replace("tags: Tags", "tags: {String: String}")));
        Model.assembler().addImport(path).assemble().unwrap();
    }

    @ParameterizedTest
    @ValueSource(strings = {"--infer-inline-collections", "--force-inline-collections"})
    void checksEnclosingMemberTraitsWhenEditingExistingInlineCollections(String option) throws IOException {
        String contents = HEADER + """
                @trait(selector: "member :test(> list > member > map :not([trait|synthetic]))")
                structure innerMapNamed {}
                map Tags { key: String, value: String }
                structure Input { @innerMapNamed rows: [Tags] }
                structure Control { tags: Tags }
                """;
        Path path = write("model.smithy", contents);

        migrate("migrate", option, path.toString());

        assertThat(Files.readString(path), equalTo(contents.replace("tags: Tags", "tags: {String: String}")));
        Model.assembler().addImport(path).assemble().unwrap();
    }

    @ParameterizedTest
    @ValueSource(strings = {"--infer-inline-collections", "--force-inline-collections"})
    void migratesResourceOperationMembersThatAreNotDeclaredProperties(String option) throws IOException {
        String contents = HEADER + """
                list Strings { member: String }
                resource Thing {
                    identifiers: { id: String }
                    read: Read
                }
                @readonly
                operation Read {
                    input := { @required id: String }
                    output := { names: Strings }
                }
                structure Control { names: Strings }
                """;
        Path path = write("model.smithy", contents);

        migrate("migrate", option, path.toString());

        assertThat(Files.readString(path), equalTo(contents.replace("names: Strings", "names: [String]")));
        Model.assembler().addImport(path).assemble().unwrap();
    }

    @ParameterizedTest
    @ValueSource(strings = {"--infer-inline-collections", "--force-inline-collections"})
    void honorsSuppressedLoaderDangersDuringBothMigrationPhases(String option) throws IOException {
        String contents = """
                $version: "2.0"
                metadata suppressions = [{id: "SyntacticShapeIdTarget", namespace: "*"}]
                namespace smithy.example
                @trait
                string ref
                @ref(NotAShape)
                string Tagged
                """ + (option.equals("--force-inline-collections") ? "@length(min: 1)\n" : "") + """
                list Strings { member: String }
                structure Input { names: Strings }
                """;
        Path path = write("model.smithy", contents);

        migrate("migrate", option, path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(model.expectShape(member(model, "Input$names").getTarget()).hasTrait(SyntheticShapeTrait.ID),
                equalTo(true));
        assertThat(Files.readString(path), containsString("$version: \"2.1\""));
        if (option.equals("--force-inline-collections")) {
            assertThat(member(model, "Input$names").expectTrait(LengthTrait.class).getMin().get(), equalTo(1L));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"--infer-inline-collections", "--force-inline-collections"})
    void keepsExplicitMixinRedefinitionsCompatibleWithUntargetedImports(String option) throws IOException {
        String importedContents = HEADER + """
                list Strings { member: String }
                @mixin
                structure Common { names: Strings }
                """;
        Path imported = write("imported.smithy", importedContents);
        String contents = HEADER + """
                structure Input with [Common] { names: Strings }
                structure Control { names: Strings }
                """;
        Path source = write("source.smithy", contents);
        Path config = write("smithy-build.json",
                Node.prettyPrintJson(Node.objectNodeBuilder()
                        .withMember("version", "1.0")
                        .withMember("imports", ArrayNode.fromStrings(imported.toString()))
                        .build()));

        migrate("migrate", "--config", config.toString(), option, source.toString());

        assertThat(Files.readString(imported), equalTo(importedContents));
        assertThat(Files.readString(source),
                equalTo(contents.replace("structure Control { names: Strings }",
                        "structure Control { names: [String] }")));
        Model.assembler().addImport(imported).addImport(source).assemble().unwrap();
    }

    @Test
    void upgradesUnqualifiedVersionTwoAndMigratesCompatibleMixinTargets() throws IOException {
        String importedContents = HEADER.replace("\"2.1\"", "\"2\"") + """
                list Strings { member: String }
                @mixin
                structure Common { names: Strings }
                """;
        Path imported = write("imported.smithy", importedContents);
        String contents = HEADER + """
                structure Input with [Common] { names: Strings }
                structure Control { names: Strings }
                """;
        Path source = write("source.smithy", contents);

        migrate("migrate", "--infer-inline-collections", imported.toString(), source.toString());

        assertThat(Files.readString(imported),
                equalTo(importedContents.replace("\"2\"", "\"2.1\"").replace("names: Strings", "names: [String]")));
        assertThat(Files.readString(source), equalTo(contents.replace("names: Strings", "names: [String]")));
        Model.assembler().addImport(imported).addImport(source).assemble().unwrap();
    }

    @Test
    void keepsCompatibleMixinEditsTogetherWhenAnUnrelatedEditIsRejected() throws IOException {
        Path original = write("a-original.smithy", HEADER + """
                list Strings { member: String }
                @mixin
                structure Common { names: Strings }
                """);
        Path redefinition = write("z-redefinition.smithy", HEADER + """
                @trait(selector: "member :test(> list :not([trait|synthetic]))")
                structure namedOnly {}
                structure Input with [Common] { names: Strings }
                structure Guarded { @namedOnly values: Strings }
                structure Control { names: Strings }
                """);

        migrate("migrate", "--infer-inline-collections", original.toString(), redefinition.toString());

        Model model = Model.assembler().addImport(original).addImport(redefinition).assemble().unwrap();
        for (String id : new String[] {"Common$names", "Input$names", "Control$names"}) {
            assertThat(model.expectShape(member(model, id).getTarget()).hasTrait(SyntheticShapeTrait.ID),
                    equalTo(true));
        }
        assertThat(member(model, "Guarded$values").getTarget(), equalTo(ShapeId.from("smithy.example#Strings")));
    }

    @Test
    void checksStructurallyExclusiveTraitsAgainstPreviouslyAcceptedEdits() throws IOException {
        Path path = write("model.smithy", HEADER.replace("\"2.1\"", "\"2.0\"") + """
                @trait(selector: ":is(list, member)", structurallyExclusive: "member")
                structure onlyOne {}
                @onlyOne
                list Strings { member: String }
                structure Input { a: Strings, b: Strings }
                """);

        migrate("migrate", "--force-inline-collections", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(model.expectShape(member(model, "Input$a").getTarget()).hasTrait(SyntheticShapeTrait.ID),
                equalTo(true));
        assertThat(member(model, "Input$a").hasTrait("smithy.example#onlyOne"), equalTo(true));
        assertThat(member(model, "Input$b").getTarget(), equalTo(ShapeId.from("smithy.example#Strings")));
        assertThat(Files.readString(path), containsString("$version: \"2.1\""));
    }

    @Test
    void preservesWholeTraitMemberPrecedenceWithoutMergingProperties() throws IOException {
        Path path = write("model.smithy", HEADER + """
                @length(min: 1, max: 10)
                list Strings { member: String }
                structure Input { @length(min: 3) names: Strings }
                """);
        Model before = Model.assembler().addImport(path).assemble().unwrap();
        LengthTrait effectiveBefore = member(before, "Input$names").getMemberTrait(before, LengthTrait.class).get();
        assertThat(effectiveBefore.getMax().isPresent(), equalTo(false));

        migrate("migrate", "--force-inline-collections", path.toString());

        Model after = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(member(after, "Input$names").getMemberTrait(after, LengthTrait.class).get().toNode(),
                equalTo(effectiveBefore.toNode()));
        assertThat(after.expectShape(member(after, "Input$names").getTarget()).hasTrait(SyntheticShapeTrait.ID),
                equalTo(true));
    }

    @Test
    void keepsDeclarationScopedSuppressionsOnNamedCollections() throws IOException {
        String contents = HEADER + """
                @suppress(["Length"])
                list Strings { member: String }
                map Tags { key: String, value: String }
                structure Input { names: Strings, tags: Tags }
                """;
        Path path = write("model.smithy", contents);

        migrate("migrate", "--force-inline-collections", path.toString());

        assertThat(Files.readString(path), equalTo(contents.replace("tags: Tags", "tags: {String: String}")));
    }

    @Test
    void preservesCrLfWhenCopyingTraitsOntoTheFinalLineWithoutATrailingNewline() throws IOException {
        String contents = (HEADER + """
                /// Collection documentation
                @length(min: 1)
                list Strings { member: String }
                structure Input { names: Strings }
                """).stripTrailing().replace("\n", "\r\n");
        Path path = write("model.smithy", contents);

        migrate("migrate", "--force-inline-collections", path.toString());

        String migrated = Files.readString(path);
        assertThat(migrated.replace("\r\n", ""), not(containsString("\n")));
        assertThat(migrated, containsString("/// Collection documentation\r\n"));
        assertThat(migrated.endsWith("\n"), equalTo(false));
        Model.assembler().addImport(path).assemble().unwrap();
    }

    @Test
    void preservesEmptyObjectDocumentTraitValuesWhenCopyingTraits() throws IOException {
        Path path = write("model.smithy", HEADER + """
                @trait(selector: ":is(list, member)")
                document myDoc
                @myDoc({})
                list Strings { member: String }
                structure Input { names: Strings }
                """);

        migrate("migrate", "--force-inline-collections", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(member(model, "Input$names").findTrait("smithy.example#myDoc").get().toNode().isObjectNode(),
                equalTo(true));
        assertThat(model.expectShape(member(model, "Input$names").getTarget()).hasTrait(SyntheticShapeTrait.ID),
                equalTo(true));
    }

    @Test
    void explainsRefusedEditsAtFineLoggingLevel() throws IOException {
        Path path = write("model.smithy", HEADER + """
                @trait(selector: "member :test(> list :not([trait|synthetic]))")
                structure namedOnly {}
                list Strings { member: String }
                structure Input { @namedOnly names: Strings }
                """);

        CliUtils.Result result = CliUtils.runSmithy("migrate",
                "--infer-inline-collections",
                "--logging",
                "FINE",
                path.toString());

        assertThat(result.stderr(), result.code(), equalTo(0));
        assertThat(result.stderr(), containsString("Keeping collection reference"));
        assertThat(result.stderr(), containsString("smithy.example#Input$names"));
        assertThat(result.stderr(), containsString("TraitTarget"));
    }

    private Path write(String name, String contents) throws IOException {
        Path path = directory.resolve(name);
        Files.writeString(path, contents);
        return path;
    }

    private void migrate(String... arguments) {
        CliUtils.Result result = CliUtils.runSmithy(arguments);
        assertThat(result.stderr(), result.code(), equalTo(0));
    }

    private MemberShape member(Model model, String id) {
        return model.expectShape(ShapeId.from("smithy.example#" + id), MemberShape.class);
    }
}
