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
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.synthetic.SyntheticShapeTrait;

class OrphanedShapeRemovalTest {
    private static final String HEADER = "$version: \"2.1\"\nnamespace smithy.example\n";
    private static final String COLLECTIONS = """
            list Strings { member: String }
            map Tags { key: String, value: String }
            structure Input { names: Strings, tags: Tags }
            """;

    @TempDir
    Path directory;

    @ParameterizedTest
    @CsvSource({
            "migrate, --infer-inline-collections, 1.0",
            "migrate, --infer-inline-collections, 2",
            "migrate, --infer-inline-collections, 2.0",
            "migrate, --infer-inline-collections, 2.1",
            "migrate, --force-inline-collections, 2.1",
            "upgrade-1-to-2, --infer-inline-collections, 2.1",
            "upgrade-1-to-2, --infer-inline-collections, 1.0"
    })
    void removesOnlyDeclarationsOrphanedByAcceptedMigration(String command, String option, String version)
            throws IOException {
        Path path = write("model.smithy", HEADER.replace("\"2.1\"", "\"" + version + "\"") + COLLECTIONS + """
                list AlreadyUnused { member: String }
                map AlsoUnused { key: String, value: String }
                string UnusedScalar
                structure UnusedStructure { value: String }
                """);

        migrate(command, option, "--remove-orphaned-shapes", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        for (String name : new String[] {"Strings", "Tags"}) {
            assertThat(model.getShape(id(name)).isPresent(), equalTo(false));
        }
        for (String name : new String[] {"AlreadyUnused", "AlsoUnused", "UnusedScalar", "UnusedStructure", "Input"}) {
            assertThat(model.getShape(id(name)).isPresent(), equalTo(true));
        }
        assertThat(model.expectShape(member(model, "Input$names").getTarget()).hasTrait(SyntheticShapeTrait.ID),
                equalTo(true));
        String migrated = Files.readString(path);
        migrate(command, option, "--remove-orphaned-shapes", path.toString());
        assertThat(Files.readString(path), equalTo(migrated));
    }

    @Test
    void requiresAnInlineCollectionOptionAndLeavesTheFileUntouched() throws IOException {
        String contents = HEADER.replace("\"2.1\"", "\"2.0\"") + COLLECTIONS;
        Path path = write("model.smithy", contents);

        CliUtils.Result result = CliUtils.runSmithy("migrate", "--remove-orphaned-shapes", path.toString());

        assertThat(result.code(), equalTo(1));
        assertThat(result.stderr(), containsString("requires --infer-inline-collections"));
        assertThat(Files.readString(path), equalTo(contents));
    }

    @Test
    void keepsNamedDeclarationsByDefault() throws IOException {
        Path path = write("model.smithy", HEADER + COLLECTIONS);

        migrate("migrate", "--infer-inline-collections", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(model.getShape(id("Strings")).isPresent(), equalTo(true));
        assertThat(model.getShape(id("Tags")).isPresent(), equalTo(true));
    }

    @Test
    void keepsCollectionsWhoseOtherReferencesCouldNotBeMigrated() throws IOException {
        Path path = write("model.smithy", HEADER + """
                @trait(selector: "member :test(> list :not([trait|synthetic]))")
                structure namedOnly {}
                list Strings { member: String }
                map Tags { key: String, value: String }
                structure Input {
                    @namedOnly
                    named: Strings
                    inline: Strings
                    tags: Tags
                }
                """);

        migrate("migrate", "--infer-inline-collections", "--remove-orphaned-shapes", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(model.getShape(id("Strings")).isPresent(), equalTo(true));
        assertThat(model.getShape(id("Tags")).isPresent(), equalTo(false));
        assertThat(member(model, "Input$named").getTarget(), equalTo(id("Strings")));
        assertThat(model.expectShape(member(model, "Input$inline").getTarget()).hasTrait(SyntheticShapeTrait.ID),
                equalTo(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Strings", "Strings$member"})
    void keepsCollectionsReferencedByTraitShapeIdsOrMemberIds(String target) throws IOException {
        Path path = write("model.smithy", HEADER + COLLECTIONS + """
                @trait(selector: "structure")
                structure reference {
                    @idRef
                    target: String
                }
                @reference(target: %s)
                structure Holder {}
                """.formatted(target));

        migrate("migrate", "--infer-inline-collections", "--remove-orphaned-shapes", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(model.getShape(id("Strings")).isPresent(), equalTo(true));
        assertThat(model.getShape(id("Tags")).isPresent(), equalTo(false));
    }

    @Test
    void keepsReferencesFromUntargetedImportedFiles() throws IOException {
        Path source = write("source.smithy", HEADER + COLLECTIONS);
        String importedContents = HEADER + "structure Consumer { names: Strings }\n";
        Path imported = write("imported.smithy", importedContents);
        Path config = config(imported);

        migrate("migrate",
                "--config",
                config.toString(),
                "--infer-inline-collections",
                "--remove-orphaned-shapes",
                source.toString());

        assertThat(Files.readString(imported), equalTo(importedContents));
        Model model = Model.assembler().addImport(source).addImport(imported).assemble().unwrap();
        assertThat(model.getShape(id("Strings")).isPresent(), equalTo(true));
        assertThat(model.getShape(id("Tags")).isPresent(), equalTo(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"use smithy.example#Strings", "apply smithy.example#Strings @length(min: 1)"})
    void keepsDeclarationsUsedOrAppliedInUntargetedFiles(String statement) throws IOException {
        Path source = write("source.smithy", HEADER + COLLECTIONS);
        String importedContents = "$version: \"2.1\"\nnamespace smithy.consumer\n" + statement
                + "\nstructure Consumer {}\n";
        Path imported = write("imported.smithy", importedContents);
        Path config = config(imported);

        migrate("migrate",
                "--config",
                config.toString(),
                "--force-inline-collections",
                "--remove-orphaned-shapes",
                source.toString());

        assertThat(Files.readString(imported), equalTo(importedContents));
        Model model = Model.assembler().addImport(source).addImport(imported).assemble().unwrap();
        assertThat(model.getShape(id("Strings")).isPresent(), equalTo(true));
        assertThat(model.getShape(id("Tags")).isPresent(), equalTo(false));
    }

    @Test
    void keepsCollectionsReferencedByResourceProperties() throws IOException {
        Path path = write("model.smithy", HEADER + COLLECTIONS + """
                resource Thing {
                    identifiers: { id: String }
                    properties: { names: Strings }
                    read: Read
                }
                @readonly
                operation Read {
                    input := { @required id: String }
                    output := { names: Strings }
                }
                """);

        migrate("migrate", "--infer-inline-collections", "--remove-orphaned-shapes", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(model.getShape(id("Strings")).isPresent(), equalTo(true));
        assertThat(model.getShape(id("Tags")).isPresent(), equalTo(false));
        assertThat(model.expectShape(member(model, "Input$names").getTarget()).hasTrait(SyntheticShapeTrait.ID),
                equalTo(true));
    }

    @Test
    void removesOrphansFromConfiguredSourcesWithoutPositionalPaths() throws IOException {
        Path path = write("model.smithy", HEADER + COLLECTIONS);
        Path config = write("smithy-build.json",
                Node.prettyPrintJson(Node.objectNodeBuilder()
                        .withMember("version", "1.0")
                        .withMember("sources", ArrayNode.fromStrings(path.toString()))
                        .build()));

        migrate("migrate", "--config", config.toString(), "--infer-inline-collections", "--remove-orphaned-shapes");

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(model.getShape(id("Strings")).isPresent(), equalTo(false));
        assertThat(model.getShape(id("Tags")).isPresent(), equalTo(false));
    }

    @Test
    void keepsOrphanedDeclarationsOutsideTheSelectedFiles() throws IOException {
        String importedContents = HEADER + "list Strings { member: String }\n";
        Path imported = write("imported.smithy", importedContents);
        Path source = write("source.smithy", HEADER + "structure Input { names: Strings }\n");
        Path config = config(imported);

        migrate("migrate",
                "--config",
                config.toString(),
                "--infer-inline-collections",
                "--remove-orphaned-shapes",
                source.toString());

        assertThat(Files.readString(imported), equalTo(importedContents));
        assertThat(Files.readString(source), containsString("names: [String]"));
        Model.assembler().addImport(imported).addImport(source).assemble().unwrap();
    }

    @Test
    void removesNestedOrphansAfterRemovingTheirOnlyReferringDeclaration() throws IOException {
        Path definitions = write("definitions.smithy", """
                $version: "2.1"
                namespace smithy.definitions
                use smithy.outer#Outer
                @private
                string Secret
                list Inner { member: Secret }
                structure Input { values: Outer }
                """);
        Path outer = write("outer.smithy", """
                $version: "2.1"
                namespace smithy.outer
                use smithy.definitions#Inner
                list Outer { member: Inner }
                """);

        migrate("migrate",
                "--infer-inline-collections",
                "--remove-orphaned-shapes",
                definitions.toString(),
                outer.toString());

        Model model = Model.assembler().addImport(definitions).addImport(outer).assemble().unwrap();
        assertThat(model.getShape(ShapeId.from("smithy.outer#Outer")).isPresent(), equalTo(false));
        assertThat(model.getShape(ShapeId.from("smithy.definitions#Inner")).isPresent(), equalTo(false));
        assertThat(Files.readString(definitions), containsString("values: [[Secret]]"));
        assertThat(Files.readString(definitions), not(containsString("use smithy.outer#Outer")));
        assertThat(Files.readString(outer), not(containsString("use smithy.definitions#Inner")));
    }

    @Test
    void keepsNamedTargetsAtTheInlineNestingLimit() throws IOException {
        Path path = write("model.smithy", HEADER + """
                list Inner { member: String }
                list Middle { member: [Inner] }
                list Outer { member: Middle }
                structure Input { values: Outer }
                """);

        migrate("migrate", "--infer-inline-collections", "--remove-orphaned-shapes", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(model.getShape(id("Outer")).isPresent(), equalTo(false));
        assertThat(model.getShape(id("Middle")).isPresent(), equalTo(false));
        assertThat(model.getShape(id("Inner")).isPresent(), equalTo(true));
        assertThat(Files.readString(path), containsString("values: [[[Inner]]]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n"})
    void removesMatchingUsesAndAppliesAndPreservesFollowingDocumentation(String newline) throws IOException {
        Path definitions = write("definitions.smithy", """
                $version: "2.1"
                namespace smithy.collections
                /// Collection documentation
                list Strings { member: String } // Removed with the declaration
                apply Strings @length(min: 1)
                """.replace("\n", newline));
        Path references = write("references.smithy", (HEADER + """
                use smithy.collections#Strings // Removed with the import
                /// Input documentation
                structure Input {
                    names: Strings
                }
                apply Strings {
                    @since("2026")
                }
                /// Next documentation
                structure Next {}
                """).replace("\n", newline));

        migrate("migrate",
                "--force-inline-collections",
                "--remove-orphaned-shapes",
                definitions.toString(),
                references.toString());

        Model model = Model.assembler().addImport(definitions).addImport(references).assemble().unwrap();
        assertThat(model.getShape(ShapeId.from("smithy.collections#Strings")).isPresent(), equalTo(false));
        assertThat(model.expectShape(id("Input")).expectTrait(DocumentationTrait.class).getValue(),
                equalTo("Input documentation"));
        assertThat(model.expectShape(id("Next")).expectTrait(DocumentationTrait.class).getValue(),
                equalTo("Next documentation"));
        assertThat(member(model, "Input$names").expectTrait(DocumentationTrait.class).getValue(),
                equalTo("Collection documentation"));
        assertThat(member(model, "Input$names").expectTrait(LengthTrait.class).getMin().get(), equalTo(1L));
        assertThat(Files.readString(references), not(containsString("use smithy.collections#Strings")));
        assertThat(Files.readString(references), not(containsString("apply Strings")));
        assertThat(Files.readString(definitions), not(containsString("Collection documentation")));
        if (newline.equals("\r\n")) {
            assertThat(Files.readString(references).replace("\r\n", ""), not(containsString("\n")));
        }
    }

    @Test
    void backsOffRemovalsThatWouldInvalidateSelectorsWhileRemovingOtherOrphans() throws IOException {
        Path path = write("model.smithy", HEADER + """
                @trait(selector: "member :test(:root([id|name = Strings]))")
                structure needsStrings {}
                list Numbers { member: Integer }
                list Strings { member: String }
                map Tags { key: String, value: String }
                structure Input {
                    numbers: Numbers
                    @needsStrings
                    names: Strings
                    tags: Tags
                }
                """);

        migrate("migrate", "--infer-inline-collections", "--remove-orphaned-shapes", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(model.getShape(id("Numbers")).isPresent(), equalTo(false));
        assertThat(model.getShape(id("Strings")).isPresent(), equalTo(true));
        assertThat(model.getShape(id("Tags")).isPresent(), equalTo(false));
        assertThat(model.expectShape(member(model, "Input$names").getTarget()).hasTrait(SyntheticShapeTrait.ID),
                equalTo(true));
    }

    @Test
    void removesOrphansWithSuppressedLoaderDangers() throws IOException {
        Path path = write("model.smithy", """
                $version: "2.1"
                metadata suppressions = [{id: "SyntacticShapeIdTarget", namespace: "*"}]
                namespace smithy.example
                @trait
                string ref
                @ref(NotAShape)
                string Tagged
                """ + COLLECTIONS);

        migrate("migrate", "--infer-inline-collections", "--remove-orphaned-shapes", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(model.getShape(id("Strings")).isPresent(), equalTo(false));
        assertThat(model.getShape(id("Tags")).isPresent(), equalTo(false));
        assertThat(model.getShape(id("Tagged")).isPresent(), equalTo(true));
    }

    @Test
    void existingLoaderWarningsDoNotPreventCleanup() throws IOException {
        Path path = write("model.smithy", HEADER + "use smithy.unknown#Missing\n" + COLLECTIONS);

        migrate("migrate", "--infer-inline-collections", "--remove-orphaned-shapes", path.toString());

        Model model = Model.assembler().addImport(path).assemble().unwrap();
        assertThat(model.getShape(id("Strings")).isPresent(), equalTo(false));
        assertThat(model.getShape(id("Tags")).isPresent(), equalTo(false));
        assertThat(Files.readString(path), containsString("use smithy.unknown#Missing"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\n", "\r\n"})
    void preservesOrdinaryCommentsAndUnrelatedDeclarationsAtTheEndOfTheFile(String ending) throws IOException {
        String contents = HEADER + """
                // Header comment
                list Strings { member: String } // Removed comment
                // Input comment
                structure Input { names: Strings }
                """ + "map Tags { key: String, value: String } // Final comment" + ending;
        Path path = write("model.smithy", contents);

        migrate("migrate", "--infer-inline-collections", "--remove-orphaned-shapes", path.toString());

        assertThat(Files.readString(path),
                equalTo(contents
                        .replace("list Strings { member: String } // Removed comment\n", "")
                        .replace("names: Strings", "names: [String]")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"migrate", "upgrade-1-to-2"})
    void documentsTheCleanupOption(String command) {
        CliUtils.Result result = CliUtils.runSmithy(command, "--help");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("--remove-orphaned-shapes"));
    }

    private Path config(Path imported) throws IOException {
        return write("smithy-build.json",
                Node.prettyPrintJson(Node.objectNodeBuilder()
                        .withMember("version", "1.0")
                        .withMember("imports", ArrayNode.fromStrings(imported.toString()))
                        .build()));
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

    private ShapeId id(String name) {
        return ShapeId.from("smithy.example#" + name);
    }

    private MemberShape member(Model model, String name) {
        return model.expectShape(id(name), MemberShape.class);
    }
}
