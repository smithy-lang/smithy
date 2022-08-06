package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.ShapeMatcher;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.Validator;

public class ModelUpgraderTest {
    @Test
    public void upgradesWhenAllModelsUse1_0() {
        UpgradeTestCase testCase = UpgradeTestCase.createAndValidate("upgrade/all-1.0");
        ValidatedResult<Model> result = testCase.actualModel;

        assertThat(ShapeId.from("smithy.example#Bytes$nullable"), ShapeMatcher.memberIsNullable(result));
        assertThat(ShapeId.from("smithy.example#Shorts$nullable"), ShapeMatcher.memberIsNullable(result));
        assertThat(ShapeId.from("smithy.example#Integers$nullable"), ShapeMatcher.memberIsNullable(result));

        assertThat(ShapeId.from("smithy.example#Bytes$nullable2"), ShapeMatcher.memberIsNullable(result));
        assertThat(ShapeId.from("smithy.example#Shorts$nullable2"), ShapeMatcher.memberIsNullable(result));
        assertThat(ShapeId.from("smithy.example#Integers$nullable2"), ShapeMatcher.memberIsNullable(result));

        assertThat(ShapeId.from("smithy.example#Bytes$nullable2"), targetsShape(result, "PrimitiveByte"));
        assertThat(ShapeId.from("smithy.example#Shorts$nullable2"), targetsShape(result, "PrimitiveShort"));
        assertThat(ShapeId.from("smithy.example#Integers$nullable2"), targetsShape(result, "PrimitiveInteger"));

        assertThat(ShapeId.from("smithy.example#Bytes$nonNull"), not(ShapeMatcher.memberIsNullable(result)));
        assertThat(ShapeId.from("smithy.example#Shorts$nonNull"), not(ShapeMatcher.memberIsNullable(result)));
        assertThat(ShapeId.from("smithy.example#Integers$nonNull"), addedDefaultTrait(result));

        assertThat(ShapeId.from("smithy.example#BlobPayload$payload"), addedDefaultTrait(result));
    }

    @Test
    public void upgradesWhenModelsMixingVersions() {
        UpgradeTestCase testCase = UpgradeTestCase.createAndValidate("upgrade/mixed-versions");
        ValidatedResult<Model> result = testCase.actualModel;

        // We don't rewrite or mess with Primitive* shape references. (a previous iteration of IDL 2.0
        // attempted to do shape rewrites, but it ended up causing issues with older models).
        assertThat(ShapeId.from("smithy.example#Foo$number"), targetsShape(result, "PrimitiveInteger"));
        assertThat(ShapeId.from("smithy.example#Foo$number"), addedDefaultTrait(result));
    }

    @Test
    public void emitsErrorWhenPrimitiveShapeUsedInV2() {
        UpgradeTestCase testCase = UpgradeTestCase.createAndValidate("upgrade/primitives-in-v2");
        ValidatedResult<Model> result = testCase.actualModel;

        assertThat(ShapeId.from("smithy.example#Bad$boolean"), shapeTargetsDeprecatedPrimitive(result));
        assertThat(ShapeId.from("smithy.example#Bad$byte"), shapeTargetsDeprecatedPrimitive(result));
        assertThat(ShapeId.from("smithy.example#Bad$short"), shapeTargetsDeprecatedPrimitive(result));
        assertThat(ShapeId.from("smithy.example#Bad$integer"), shapeTargetsDeprecatedPrimitive(result));
        assertThat(ShapeId.from("smithy.example#Bad$long"), shapeTargetsDeprecatedPrimitive(result));
        assertThat(ShapeId.from("smithy.example#Bad$float"), shapeTargetsDeprecatedPrimitive(result));
        assertThat(ShapeId.from("smithy.example#Bad$double"), shapeTargetsDeprecatedPrimitive(result));
    }

    @Test
    public void emitsErrorWhenBoxTraitUsedInV2() {
        UpgradeTestCase testCase = UpgradeTestCase.createAndValidate("upgrade/box-in-v2");
        ValidatedResult<Model> result = testCase.actualModel;

        assertThat(ShapeId.from("smithy.example#Bad$boolean"), v2ShapeUsesBoxTrait(result));
    }

    @Test
    public void doesNotIntroduceConflictsDuringUpgrade() {
        UpgradeTestCase testCase = UpgradeTestCase.createAndValidate("upgrade/does-not-introduce-conflict");
        ValidatedResult<Model> result = testCase.actualModel;

        assertThat(ShapeId.from("smithy.example#Foo$alreadyDefault"), targetsShape(result, "PrimitiveInteger"));
        assertThat(ShapeId.from("smithy.example#Foo$alreadyRequired"), targetsShape(result, "PrimitiveInteger"));
        assertThat(ShapeId.from("smithy.example#Foo$boxedMember"), targetsShape(result, "PrimitiveInteger"));
        assertThat(ShapeId.from("smithy.example#Foo$boxedMember"), not(addedDefaultTrait(result)));
        assertThat(ShapeId.from("smithy.example#Foo$previouslyBoxedTarget"), not(addedDefaultTrait(result)));
        assertThat(ShapeId.from("smithy.example#Foo$explicitlyBoxedTarget"), not(addedDefaultTrait(result)));
    }

    /**
     * Treats each directory under "upgrade" as a scenario where upgraded.smithy or upgraded.json is
     * the resulting upgraded 2.0 model, and every other .smithy or .json file (including sub-directories)
     * come together to form the model being loaded and converted to 2.0.
     */
    private static final class UpgradeTestCase {
        Path upgradeFile;
        final List<Path> modelFiles = new ArrayList<>();
        ValidatedResult<Model> actualModel;
        ValidatedResult<Model> expectedModel;

        private static UpgradeTestCase createAndValidate(String directory) {
            UpgradeTestCase testCase = createFromDirectory(directory);

            ModelAssembler actualAssembler = Model.assembler();
            for (Path path : testCase.modelFiles) {
                actualAssembler.addImport(path);
            }

            testCase.actualModel = actualAssembler.assemble();
            testCase.expectedModel = Model.assembler().addImport(testCase.upgradeFile).assemble();
            ModelSerializer serializer = ModelSerializer.builder().build();

            // Synthetic traits can cause the nodes to not be equal. Normalize the model by
            // serializing and then deserializing it.
            ObjectNode actualNode = serializeNormalizedModel(testCase.actualModel.getResult().get(), serializer);
            ObjectNode expectedNode = serializeNormalizedModel(testCase.expectedModel.getResult().get(), serializer);

            // Make the equality assertion with pretty diffing.
            Node.assertEquals(actualNode, expectedNode);

            return testCase;
        }

        private static ObjectNode serializeNormalizedModel(Model model, ModelSerializer serializer) {
            Model normalized = Model.assembler()
                    .addDocumentNode(serializer.serialize(model))
                    .assemble()
                    .getResult()
                    .get();
            return serializer.serialize(normalized);
        }

        private static UpgradeTestCase createFromDirectory(String directory) {
            try (Stream<Path> paths = Files.walk(Paths.get(ModelUpgraderTest.class.getResource(directory).toURI()))) {
                UpgradeTestCase testCase = new UpgradeTestCase();
                paths.filter(Files::isRegularFile).forEach(file -> {
                    if (file.endsWith("upgraded.smithy") || file.endsWith("upgraded.json")) {
                        testCase.upgradeFile = file;
                    } else if (file.toString().endsWith(".json") || file.toString().endsWith(".smithy")) {
                        testCase.modelFiles.add(file);
                    }
                });
                if (testCase.upgradeFile == null) {
                    throw new RuntimeException("No upgraded.smithy / upgraded.json file found for " + directory);
                }
                return testCase;
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Matcher<ShapeId> targetsShape(ValidatedResult<Model> result, String shapeName) {
        return ShapeMatcher.builderFor(MemberShape.class, result)
                .description("Targets " + shapeName)
                .addAssertion(member -> member.getTarget()
                                      .equals(ShapeId.fromOptionalNamespace(Prelude.NAMESPACE, shapeName)),
                              member -> "targeted " + member.getTarget())
                .build();
    }

    private static Matcher<ShapeId> addedDefaultTrait(ValidatedResult<Model> result) {
        return ShapeMatcher.builderFor(MemberShape.class, result)
                .description("member to have a default trait with warning")
                .addAssertion(member -> member.hasTrait(DefaultTrait.class),
                              member -> "no @default trait")
                .addEventAssertion(Validator.MODEL_DEPRECATION, Severity.WARNING, "@default")
                .build();
    }

    private static Matcher<ShapeId> shapeTargetsDeprecatedPrimitive(ValidatedResult<Model> result) {
        return ShapeMatcher.builderFor(MemberShape.class, result)
                .description("shape targets a deprecated primitive shape and emits a warning")
                .addAssertion(member -> member.getTarget().getName().startsWith("Primitive")
                                        && member.getTarget().getNamespace().equals(Prelude.NAMESPACE),
                              member -> "member does not target a primitive prelude shape")
                .addEventAssertion("DeprecatedShape", Severity.WARNING,
                                   "and add the @default trait to structure members that targets this shape")
                .build();
    }

    private static Matcher<ShapeId> v2ShapeUsesBoxTrait(ValidatedResult<Model> result) {
        return ShapeMatcher.builderFor(MemberShape.class, result)
                .description("v2 shape uses box trait")
                .addEventAssertion(Validator.MODEL_ERROR, Severity.ERROR,
                                   "@box is not supported in Smithy IDL 2.0")
                .build();
    }
}
