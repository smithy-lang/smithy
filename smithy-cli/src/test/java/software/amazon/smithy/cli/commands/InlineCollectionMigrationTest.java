/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

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
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.SinceTrait;
import software.amazon.smithy.model.traits.synthetic.SyntheticShapeTrait;

class InlineCollectionMigrationTest {
    private static final String HEADER = "$version: \"2.1\"\nnamespace smithy.example\n\n";
    private static final String COLLECTIONS = """
            list Strings {
                member: String
            }

            map Tags {
                key: String
                value: String
            }

            structure Input {
                names: Strings
                tags: Tags
            }

            union Choice {
                names: Strings
                tags: Tags
            }
            """;

    @TempDir
    Path directory;

    @ParameterizedTest
    @CsvSource({
            "migrate, 1, --infer-inline-collections",
            "migrate, 1.0, --infer-inline-collections",
            "migrate, 2, --infer-inline-collections",
            "migrate, 2.0, --infer-inline-collections",
            "migrate, 2.1, --infer-inline-collections",
            "migrate, 2.1, --force-inline-collections",
            "upgrade-1-to-2, 2.1, --infer-inline-collections",
            "upgrade-1-to-2, 2, --infer-inline-collections"
    })
    void infersCollectionsAndKeepsDeclarations(String command, String version, String option) throws IOException {
        Path path = write("model.smithy", HEADER.replace("\"2.1\"", "\"" + version + "\"") + COLLECTIONS);

        migrate(command, option, path.toString());

        String expectedHeader = version.startsWith("1") ? HEADER.replace("\"2.1\"\n", "\"2.1\"\n\n") : HEADER;
        assertThat(Files.readString(path),
                equalTo(expectedHeader + COLLECTIONS
                        .replace("names: Strings", "names: [String]")
                        .replace("tags: Tags", "tags: {String: String}")));
        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(model.expectShape(ShapeId.from("smithy.example#Strings")).hasTrait(SyntheticShapeTrait.ID),
                equalTo(false));
        assertThat(model.expectShape(ShapeId.from("smithy.example#Tags")).hasTrait(SyntheticShapeTrait.ID),
                equalTo(false));
    }

    @Test
    void migrationIsOptIn() throws IOException {
        Path path = write("model.smithy", HEADER + COLLECTIONS);

        migrate("migrate", path.toString());

        assertThat(Files.readString(path), equalTo(HEADER + COLLECTIONS));
    }

    @ParameterizedTest
    @ValueSource(strings = {"--infer-inline-collections", "--force-inline-collections"})
    void upgradesUnqualifiedVersion2BeforeInferringCollections(String option) throws IOException {
        String contents = HEADER.replace("\"2.1\"", "\"2\"") + COLLECTIONS;
        Path path = write("model.smithy", contents);

        migrate("migrate", option, path.toString());

        assertThat(Files.readString(path),
                equalTo(HEADER + COLLECTIONS
                        .replace("names: Strings", "names: [String]")
                        .replace("tags: Tags", "tags: {String: String}")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"--infer-inline-collections", "--force-inline-collections"})
    void skipsCollectionsWithMemberTraitsAndUnsupportedShapeTraits(String option) throws IOException {
        String contents = HEADER + """
                list DocumentedMembers {
                    /// Element documentation
                    member: String
                }

                map ConstrainedKeys {
                    @length(min: 1)
                    key: String
                    value: String
                }

                map ConstrainedValues {
                    key: String
                    @length(min: 1)
                    value: String
                }

                @sparse
                list SparseStrings {
                    member: String
                }

                @uniqueItems
                list UniqueStrings {
                    member: String
                }

                @sensitive
                list SensitiveStrings {
                    member: String
                }

                structure Input {
                    a: DocumentedMembers
                    b: ConstrainedKeys
                    c: ConstrainedValues
                    d: SparseStrings
                    e: UniqueStrings
                    f: SensitiveStrings
                }
                """;
        Path path = write("model.smithy", contents);

        migrate("migrate", option, path.toString());

        assertThat(Files.readString(path), equalTo(contents));
    }

    @Test
    void inferenceSkipsCollectionsWithShapeTraits() throws IOException {
        String contents = HEADER + "@length(min: 1)\n" + COLLECTIONS;
        Path path = write("model.smithy", contents);

        migrate("migrate", "--infer-inline-collections", path.toString());

        assertThat(Files.readString(path), equalTo(contents.replace("tags: Tags", "tags: {String: String}")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "2.0", "2.1"})
    void copiesTraitsToEveryReferenceAndMemberTraitsWin(String version) throws IOException {
        Path path = write("model.smithy", HEADER.replace("\"2.1\"", "\"" + version + "\"") + """
                /// Collection documentation
                @length(min: 1)
                @since("2026")
                list Strings {
                    member: String
                }

                structure Input {
                    first: Strings

                    /// Member documentation
                    @length(min: 3)
                    second: Strings
                }

                union Choice {
                    names: Strings
                }
                """);

        migrate("migrate", "--force-inline-collections", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        for (String id : new String[] {"Input$first", "Input$second", "Choice$names"}) {
            MemberShape member = member(model, id);
            assertThat(model.expectShape(member.getTarget()).hasTrait(SyntheticShapeTrait.ID), equalTo(true));
            assertThat(member.expectTrait(SinceTrait.class).getValue(), equalTo("2026"));
            assertThat(member.expectTrait(LengthTrait.class).getMin().get(),
                    equalTo(id.equals("Input$second") ? 3L : 1L));
            assertThat(member.expectTrait(DocumentationTrait.class).getValue(),
                    equalTo(id.equals("Input$second") ? "Member documentation" : "Collection documentation"));
        }
        assertThat(model.expectShape(ShapeId.from("smithy.example#Strings")).hasTrait(LengthTrait.ID), equalTo(true));

        String migrated = Files.readString(path);
        migrate("migrate", "--force-inline-collections", path.toString());
        assertThat(Files.readString(path), equalTo(migrated));
    }

    @Test
    void copiesTraitsFromApplyStatementsAndPreservesMemberOverridesFromApplyStatements() throws IOException {
        Path path = write("model.smithy", HEADER + COLLECTIONS + """

                apply Tags @length(min: 1)
                apply Input$tags @length(min: 2)
                """);

        migrate("migrate", "--force-inline-collections", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(member(model, "Input$tags").expectTrait(LengthTrait.class).getMin().get(), equalTo(2L));
        assertThat(member(model, "Choice$tags").expectTrait(LengthTrait.class).getMin().get(), equalTo(1L));
        assertThat(Files.readString(path), containsString("apply Tags @length(min: 1)"));
    }

    @Test
    void checksTraitSelectorsAtEachReferenceSite() throws IOException {
        Path path = write("model.smithy", HEADER + """
                @trait(selector: ":is(list, structure > member :test(> list))")
                structure structureOnly {}

                @structureOnly
                list Strings {
                    member: String
                }

                structure Input {
                    names: Strings
                }

                union Choice {
                    names: Strings
                }
                """);

        migrate("migrate", "--force-inline-collections", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(model.expectShape(member(model, "Input$names").getTarget()).hasTrait(SyntheticShapeTrait.ID),
                equalTo(true));
        assertThat(member(model, "Input$names").hasTrait("smithy.example#structureOnly"), equalTo(true));
        assertThat(member(model, "Choice$names").getTarget(), equalTo(ShapeId.from("smithy.example#Strings")));
    }

    @Test
    void checksSelectorsAgainstTheReplacementTarget() throws IOException {
        String contents = HEADER + """
                @trait(selector: ":is(list, member :test(> list :not([trait|synthetic])))")
                structure namedOnly {}

                @namedOnly
                list Strings {
                    member: String
                }

                structure Input {
                    names: Strings
                }
                """;
        Path path = write("model.smithy", contents);

        migrate("migrate", "--force-inline-collections", path.toString());

        assertThat(Files.readString(path), equalTo(contents));
    }

    @Test
    void recursivelyInlinesCollectionsWithinTheNestingLimit() throws IOException {
        Path path = write("model.smithy", HEADER + """
                list L1 { member: String }
                list L2 { member: L1 }
                list L3 { member: L2 }
                list L4 { member: L3 }
                structure Input { values: L4 }
                """);

        migrate("migrate", "--infer-inline-collections", path.toString());

        assertThat(Files.readString(path), equalTo(HEADER + """
                list L1 { member: String }
                list L2 { member: [String] }
                list L3 { member: [[String]] }
                list L4 { member: [[[String]]] }
                structure Input { values: [[[L1]]] }
                """));
        Model.assembler().addImport(path).assemble().unwrap();
        String migrated = Files.readString(path);
        migrate("migrate", "--infer-inline-collections", path.toString());
        assertThat(Files.readString(path), equalTo(migrated));
    }

    @Test
    void migratesReferencesInsideExistingInlineCollections() throws IOException {
        Path path = write("model.smithy", HEADER + """
                list Strings { member: String }
                structure Input {
                    nested: {String: Strings}
                }
                """);

        migrate("migrate", "--infer-inline-collections", path.toString());

        assertThat(Files.readString(path), containsString("nested: {String: [String]}"));
    }

    @Test
    void retainsNestedTraitsAndCountsExistingInlineLevels() throws IOException {
        Path path = write("model.smithy", HEADER + """
                @length(min: 1)
                list Ranged { member: String }
                list Outer { member: Ranged }
                list Deep { member: [[String]] }
                structure Input {
                    values: Outer
                    alreadyInline: [Ranged]
                    tooDeep: [Deep]
                }
                """);

        migrate("migrate", "--force-inline-collections", path.toString());

        String actual = Files.readString(path);
        assertThat(actual, equalTo(HEADER + """
                @length(min: 1)
                list Ranged { member: String }
                list Outer {
                    @length(
                        min: 1
                    )
                    member: [String] }
                list Deep { member: [[String]] }
                structure Input {
                    values: [Ranged]
                    alreadyInline: [Ranged]
                    tooDeep: [Deep]
                }
                """));
        Model.assembler().addImport(path).assemble().unwrap();
    }

    @Test
    void resolvesCrossFileTargetsAndAvoidsNameCollisions() throws IOException {
        Path collections = write("collections.smithy", """
                $version: "2.1"
                namespace smithy.collections
                @length(min: 1)
                list Strings { member: String }
                map Tags { key: String, value: String }
                """);
        Path references = write("references.smithy", HEADER + """
                use smithy.collections#Strings
                use smithy.collections#Tags
                string String
                structure Input {
                    names: Strings
                    tags: Tags
                }
                """);
        String originalCollections = Files.readString(collections);

        migrate("migrate", "--force-inline-collections", collections.toString(), references.toString());

        assertThat(Files.readString(collections), equalTo(originalCollections));
        assertThat(Files.readString(references), containsString("names: [smithy.api#String]"));
        assertThat(Files.readString(references), containsString("tags: {smithy.api#String: smithy.api#String}"));
        Model.assembler().addImport(collections).addImport(references).assemble().unwrap();
    }

    @Test
    void leavesUntargetedImportedFilesUntouched() throws IOException {
        Path imported = write("imported.smithy", """
                $version: "2.1"
                namespace smithy.collections
                list Strings { member: String }
                structure Imported { names: Strings }
                """);
        Path references = write("references.smithy", HEADER + """
                use smithy.collections#Strings
                structure Input { names: Strings }
                """);
        Path config = write("smithy-build.json",
                "{\"version\":\"1.0\",\"imports\":[\"" + imported + "\"]}");
        String originalImported = Files.readString(imported);

        migrate("migrate", "--config", config.toString(), "--infer-inline-collections", references.toString());

        assertThat(Files.readString(imported), equalTo(originalImported));
        assertThat(Files.readString(references), containsString("names: [String]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"--infer-inline-collections", "--force-inline-collections"})
    void migratesMixinMembersAndInlineOperationInputs(String option) throws IOException {
        Path path = write("model.smithy", HEADER + """
                list Strings { member: String }

                @mixin
                structure Common {
                    names: Strings
                }

                structure Input with [Common] {
                    @required
                    $names
                }

                operation Get {
                    input := {
                        names: Strings
                    }
                }
                """);

        migrate("migrate", option, path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        for (String id : new String[] {"Common$names", "Input$names", "GetInput$names"}) {
            assertThat(model.expectShape(member(model, id).getTarget()).hasTrait(SyntheticShapeTrait.ID),
                    equalTo(true));
        }
        assertThat(Files.readString(path), containsString("$names"));
    }

    @Test
    void copiesCustomTraitsAcrossNamespaces() throws IOException {
        Path definitions = write("definitions.smithy", """
                $version: "2.1"
                namespace smithy.collections
                @trait(selector: ":is(list, member :test(> list))")
                structure collectionInfo {
                    label: String
                    enabled: Boolean
                }

                @collectionInfo(label: "names", enabled: true)
                list Strings { member: String }
                """);
        Path references = write("references.smithy", HEADER + """
                use smithy.collections#Strings
                structure Input { names: Strings }
                """);

        migrate("migrate", "--force-inline-collections", definitions.toString(), references.toString());

        Model model = Model.assembler().addImport(definitions).addImport(references).assemble().unwrap();
        assertThat(member(model, "Input$names").findTrait("smithy.collections#collectionInfo")
                .get()
                .toNode()
                .expectObjectNode()
                .expectStringMember("label")
                .getValue(), equalTo("names"));
        assertThat(Files.readString(references), containsString("@smithy.collections#collectionInfo("));
    }

    @Test
    void retainsCollectionsWithUnknownTraits() throws IOException {
        String contents = HEADER + "@unknown\n" + COLLECTIONS;
        Path path = write("model.smithy", contents);

        migrate("migrate", "--allow-unknown-traits", "--force-inline-collections", path.toString());

        assertThat(Files.readString(path), equalTo(contents.replace("tags: Tags", "tags: {String: String}")));
    }

    @Test
    void skipsIneligibleMembersAndStillMigratesOtherReferences() throws IOException {
        Path valid = write("valid.smithy", HEADER + COLLECTIONS);
        Path constrained = write("constrained.smithy", HEADER + """
                @trait(selector: "member :test(> list :not([trait|synthetic]))")
                structure namedReferenceOnly {}

                structure Constrained {
                    @namedReferenceOnly
                    names: Strings
                }
                """);
        String originalValid = Files.readString(valid);
        String originalConstrained = Files.readString(constrained);
        Model.assembler().addImport(valid).addImport(constrained).assemble().unwrap();

        CliUtils.Result result = CliUtils.runSmithy("migrate",
                "--infer-inline-collections",
                valid.toString(),
                constrained.toString());

        assertThat(result.stderr(), result.code(), equalTo(0));
        assertThat(Files.readString(valid),
                equalTo(originalValid
                        .replace("names: Strings", "names: [String]")
                        .replace("tags: Tags", "tags: {String: String}")));
        assertThat(Files.readString(constrained), equalTo(originalConstrained));
    }

    @Test
    void preservesCommentsLineEndingsAndMemberIndexes() throws IOException {
        String original = HEADER + COLLECTIONS.replace("names: Strings", "1. names: Strings // Keep this comment")
                .replace("tags: Tags", "2. tags: Tags");
        Path path = write("model.smithy", original.replace("\n", "\r\n"));
        Path idx = write("idx.smithy", """
                $version: "2.1"
                namespace smithy.protocols
                @trait(selector: ":is(structure, union) > member")
                integer idx
                """);

        migrate("migrate", "--infer-inline-collections", path.toString(), idx.toString());

        assertThat(Files.readString(path),
                equalTo(original
                        .replace("names: Strings", "names: [String]")
                        .replace("tags: Tags", "tags: {String: String}")
                        .replace("\n", "\r\n")));
    }

    @Test
    void forceIncludesInferenceWhenBothFlagsAreProvided() throws IOException {
        Path path = write("model.smithy", HEADER + "@length(min: 1)\n" + COLLECTIONS);

        migrate("migrate", "--force-inline-collections", "--infer-inline-collections", path.toString());

        assertThat(Files.readString(path), containsString("names: [String]"));
        assertThat(Files.readString(path), containsString("tags: {String: String}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"migrate", "upgrade-1-to-2"})
    void documentsBothOptions(String command) {
        CliUtils.Result result = CliUtils.runSmithy(command, "--help");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("--infer-inline-collections"));
        assertThat(result.stdout(), containsString("--force-inline-collections"));
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
