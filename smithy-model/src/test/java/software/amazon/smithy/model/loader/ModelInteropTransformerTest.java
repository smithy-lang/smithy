package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.ShapeMatcher;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.Pair;

public class ModelInteropTransformerTest {
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

        Shape shape = result.getResult().get().expectShape(ShapeId.from("smithy.example#Foo$boxedMember"));
        assertThat(shape.hasTrait(BoxTrait.class), is(true));
        assertThat(shape.hasTrait(DefaultTrait.class), is(true));
        assertThat(shape.expectTrait(DefaultTrait.class).toNode().isNullNode(), is(true));

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
            try (Stream<Path> paths = Files.walk(Paths.get(ModelInteropTransformerTest.class.getResource(directory).toURI()))) {
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
                .description("member to have a default trait")
                .addAssertion(member -> member.hasTrait(DefaultTrait.class),
                              member -> "no @default trait")
                .build();
    }

    private static Matcher<ShapeId> v2ShapeUsesBoxTrait(ValidatedResult<Model> result) {
        return ShapeMatcher.builderFor(MemberShape.class, result)
                .description("v2 shape uses box trait")
                .addEventAssertion(Validator.MODEL_ERROR, Severity.ERROR,
                                   "@box is not supported in Smithy IDL 2.0")
                .build();
    }

    // This test ensures that the upgrader properly upgrades 1.0 models to 2.0 models, back patches 2.0 models with
    // synthetic box traits so they work with 1.0 implementations, and that serializing and deserializing an upgraded
    // model carries the same nullability results in both 1.0 and 2.0 implementations (it can't be lossy).
    @Test
    public void ensuresConsistentNullabilityAcrossVersions() {
        Pattern splitPattern = Pattern.compile("\r\n|\r|\n");
        List<Pair<String, String>> cases = ListUtils.of(
            Pair.of("1-to-2", "nullableBooleanBoxedTarget"),
            Pair.of("1-to-2", "nullableBooleanBoxedNonPreludeTarget"),
            Pair.of("1-to-2", "nullableBooleanInV1BoxedTargetRequired"),
            Pair.of("1-to-2", "nonNullableBooleanUnboxedTarget"),
            Pair.of("1-to-2", "nullableBooleanBoxedMember"),
            Pair.of("1-to-2", "nonNullableBooleanUnboxedCustomTarget"),
            Pair.of("1-to-2", "nullableIntegerBoxedTarget"),
            Pair.of("1-to-2", "nullableIntegerBoxedNonPreludeTarget"),
            Pair.of("1-to-2", "nullableIntegerInV1BoxedTargetRequired"),
            Pair.of("1-to-2", "nonNullableIntegerUnboxedTarget"),
            Pair.of("1-to-2", "nullableIntegerBoxedMember"),
            Pair.of("1-to-2", "nonNullableIntegerUnboxedCustomTarget"),

            Pair.of("2-to-1", "booleanDefaultZeroValueToNonNullable"),
            Pair.of("2-to-1", "booleanDefaultNonZeroValueToNullable"),
            Pair.of("2-to-1", "booleanRequiredToNullable"),
            Pair.of("2-to-1", "booleanDefaultWithAddedTraitToNullable"),
            Pair.of("2-to-1", "booleanDefaultWithClientOptionalTraitToNullable"),
            Pair.of("2-to-1", "intEnumSetToZeroValueToNonNullable"),
            Pair.of("2-to-1", "booleanDefaultZeroValueToNonNullablePrelude")
        );

        cases.forEach(pair -> {
            String suite = pair.left;
            String name = pair.right;

            // Load the contents of the model as a string and parse out the expected nullability JSON
            // from the comment on the first line.
            String contents = IoUtils.readUtf8Resource(getClass(), "upgrade-box/" + suite + "/" + name + ".smithy");
            String[] lines = splitPattern.split(contents, 2);
            String expectedJsonValue = lines[0].replace("// ", "").trim();
            ObjectNode expectedNode = Node.parse(expectedJsonValue).expectObjectNode();
            Map<String, Boolean> nullability = new HashMap<>();
            expectedNode.getStringMap().forEach((k, v) -> nullability.put(k, v.expectBooleanNode().getValue()));

            Model original = Model.assembler()
                    .addUnparsedModel(name + ".smithy", contents)
                    .assemble()
                    .unwrap();

            upgradeAssertions(name, original, nullability, 0);

            // Round trip the model twice just to be certain nothing is getting lost between
            // serializations.
            Model updatedModel = original;
            for (int tripCount = 1; tripCount <= 2; tripCount++) {
                Node serialized = ModelSerializer.builder().build().serialize(updatedModel);
                updatedModel = Model.assembler()
                        .addDocumentNode(serialized)
                        .assemble()
                        .unwrap();
                upgradeAssertions(name, updatedModel, nullability, tripCount);
            }
        });
    }

    private void upgradeAssertions(String name, Model model, Map<String, Boolean> expected, int roundTrip) {
        NullableIndex index = NullableIndex.of(model);
        ShapeId shape = ShapeId.from("smithy.example#Foo$" + name);
        MemberShape member = model.expectShape(shape, MemberShape.class);

        Map<String, Boolean> result = new HashMap<>();
        boolean isBoxed = member.getMemberTrait(model, BoxTrait.class).isPresent();
        result.put("v1-box", isBoxed);
        result.put("v1-client-zero-value",
                   index.isMemberNullable(member, NullableIndex.CheckMode.CLIENT_ZERO_VALUE_V1));
        result.put("v2", index.isMemberNullable(model.expectShape(shape, MemberShape.class)));

        String reason = "Expected " + name + " to have nullability of " + expected + " but found "
                        + result + " (round trip #" + roundTrip + ')';

        assertThat(reason, expected, equalTo(result));

        // Ensure that the deprecated index check using isNullable matches v1-client-zero-value results.
        boolean isDeprecatedIndexWorking = index.isNullable(member);
        if (!isDeprecatedIndexWorking == result.get("v1-client-zero-value")) {
            String reasonBox = "Expected deprecated index checks to be " + result.get("v1") + " for " + name
                               + "; traits: " + member.getAllTraits() + "; round trip " + roundTrip;
            Assertions.fail(reasonBox);
        }
    }

    // Loading a Smithy 1.0 model and serializing it is lossy if we don't add some way
    // to indicate that the box trait was present on a root level shape. This test ensures
    // that box information can be round-tripped when converting a 1.0 model to a 2.0 model.
    @Test
    public void boxTraitOnRootShapeIsNotLossyWhenRoundTripped() {
        Model model = Model.assembler()
                .addUnparsedModel("foo.smithy", "$version: \"1.0\"\n"
                                                + "namespace smithy.example\n"
                                                + "@box\n"
                                                + "integer MyInteger\n"
                                                + "\n"
                                                + "integer PrimitiveInteger\n"
                                                + "\n"
                                                + "structure Foo {\n"
                                                + "    @box\n"
                                                + "    baz: MyInteger\n"
                                                + "    bam: PrimitiveInteger\n"
                                                +"}\n")
                .assemble()
                .unwrap();

        Node roundTripNode = ModelSerializer.builder().build().serialize(model);
        Model roundTripModel = Model.assembler().addDocumentNode(roundTripNode).assemble().unwrap();

        // Ensure it works when first loaded, and it works when round-tripped through the serializer and assembler.
        boxTraitRootAssertionsV1(model, 0);
        boxTraitRootAssertionsV1(roundTripModel, 1);
    }

    private void boxTraitRootAssertionsV1(Model model, int roundTrip) {
        ShapeId myInteger = ShapeId.from("smithy.example#MyInteger");
        ShapeId foo = ShapeId.from("smithy.example#Foo");
        ShapeId fooBaz = ShapeId.from("smithy.example#Foo$baz");

        ShapeId primitiveInteger = ShapeId.from("smithy.example#PrimitiveInteger");
        ShapeId fooBam = ShapeId.from("smithy.example#Foo$bam");

        assertThat(model.expectShape(myInteger).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(foo).hasTrait(BoxTrait.class), is(false));
        assertThat(model.expectShape(fooBaz).hasTrait(BoxTrait.class), is(true));

        Node serialized = ModelSerializer.builder().build().serialize(model);
        String raw = Node.prettyPrintJson(serialized);
        Model model2 = Model.assembler()
                .addUnparsedModel("foo.json", raw)
                .assemble()
                .unwrap();

        assertThat(model2.expectShape(myInteger).hasTrait(BoxTrait.class), is(true));
        assertThat(model2.expectShape(myInteger).hasTrait(DefaultTrait.class), is(false));
        assertThat(model2.expectShape(foo).hasTrait(BoxTrait.class), is(false));
        assertThat(model2.expectShape(fooBaz).hasTrait(DefaultTrait.class), is(true));
        assertThat(model2.expectShape(fooBaz).expectTrait(DefaultTrait.class).toNode(), equalTo(Node.nullNode()));

        // This member gets a synthetic box trait because the default value is set to null.
        assertThat(model2.expectShape(fooBaz).hasTrait(BoxTrait.class), is(true));

        // Primitive integer gets a synthetic default trait and no box trait.
        assertThat(model2.expectShape(primitiveInteger).hasTrait(DefaultTrait.class), is(true));
        assertThat(model2.expectShape(primitiveInteger).hasTrait(BoxTrait.class), is(false));

        // The member referring to PrimitiveInteger gets a synthetic default trait and no box trait.
        assertThat(model2.expectShape(fooBam).hasTrait(DefaultTrait.class), is(true));
        assertThat(model2.expectShape(fooBam).hasTrait(BoxTrait.class), is(false));
    }

    @Test
    public void boxTraitOnlyAddedToRootWhenNotSetToZeroValueDefault() {
        Model model = Model.assembler()
                .addUnparsedModel("foo.smithy", "$version: \"2.0\"\n"
                                                + "namespace smithy.example\n"
                                                + "\n"
                                                + "@default(\"\")\n"
                                                + "string DefaultString\n"
                                                + "\n"
                                                + "integer BoxedInteger\n"
                                                + "\n"
                                                + "@default(1)\n"
                                                + "integer BoxedIntegerWithDefault\n"
                                                + "\n"
                                                + "@default(0)\n"
                                                + "integer PrimitiveInteger\n"
                                                + "\n"
                                                + "intEnum BoxedIntEnum {\n"
                                                + "    ONE = 1\n"
                                                + "}\n"
                                                + "\n"
                                                + "@default(1)\n"
                                                + "intEnum BoxedIntEnumWithDefault {\n"
                                                + "    ONE = 1\n"
                                                + "}\n"
                                                + "\n"
                                                + "@default(0)\n"
                                                + "intEnum PrimitiveIntEnum {\n"
                                                + "    ZERO = 0\n"
                                                + "}\n"
                                                + "\n"
                                                + "structure Foo {\n"
                                                + "    DefaultString: DefaultString = \"\"\n"
                                                + "    BoxedInteger: BoxedInteger\n"
                                                + "    PrimitiveInteger: PrimitiveInteger = 0\n"
                                                + "    BoxedIntegerWithDefault: BoxedIntegerWithDefault = 1\n"
                                                + "    BoxedIntEnum: BoxedIntEnum\n"
                                                + "    BoxedIntEnumWithDefault: BoxedIntEnumWithDefault = 1\n"
                                                + "    PrimitiveIntEnum: PrimitiveIntEnum = 0\n"
                                                +"}\n")
                .assemble()
                .unwrap();

        Node roundTripNode = ModelSerializer.builder().build().serialize(model);
        Model roundTripModel = Model.assembler().addDocumentNode(roundTripNode).assemble().unwrap();

        // Ensure it works when first loaded, and it works when round-tripped through the serializer and assembler.
        boxTraitRootAssertionsV2(model, 0);
        boxTraitRootAssertionsV2(roundTripModel, 1);
    }

    private void boxTraitRootAssertionsV2(Model model, int roundTrip) {
        ShapeId defaultString = ShapeId.from("smithy.example#DefaultString");
        ShapeId fooDefaultString = ShapeId.from("smithy.example#Foo$DefaultString");

        ShapeId boxedInteger = ShapeId.from("smithy.example#BoxedInteger");
        ShapeId fooBoxedInteger = ShapeId.from("smithy.example#Foo$BoxedInteger");

        ShapeId boxedIntegerWithDefault = ShapeId.from("smithy.example#BoxedIntegerWithDefault");
        ShapeId fooBoxedIntegerWithDefault = ShapeId.from("smithy.example#Foo$BoxedIntegerWithDefault");

        ShapeId primitiveInteger = ShapeId.from("smithy.example#PrimitiveInteger");
        ShapeId fooPrimitiveInteger = ShapeId.from("smithy.example#Foo$PrimitiveInteger");

        ShapeId boxedIntEnum = ShapeId.from("smithy.example#BoxedIntEnum");
        ShapeId fooBoxedIntEnum = ShapeId.from("smithy.example#Foo$BoxedIntEnum");

        ShapeId boxedIntEnumWithDefault = ShapeId.from("smithy.example#BoxedIntEnumWithDefault");
        ShapeId fooBoxedIntEnumWithDefault = ShapeId.from("smithy.example#Foo$BoxedIntEnumWithDefault");

        ShapeId primitiveIntEnum = ShapeId.from("smithy.example#PrimitiveIntEnum");
        ShapeId fooPrimitiveIntEnum = ShapeId.from("smithy.example#Foo$PrimitiveIntEnum");

        // Do not box strings for v1 compatibility.
        assertThat(model.expectShape(defaultString).hasTrait(BoxTrait.class), is(false));
        assertThat(model.expectShape(fooDefaultString).hasTrait(BoxTrait.class), is(false));
        assertThat(model.expectShape(fooDefaultString).hasTrait(DefaultTrait.class), is(true));
        assertThat(model.expectShape(defaultString).hasTrait(DefaultTrait.class), is(true));

        // Add box to BoxedInteger because it has no default trait.
        assertThat(model.expectShape(boxedInteger).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(fooBoxedInteger).hasTrait(BoxTrait.class), is(false)); // no need to box member too
        assertThat(model.expectShape(boxedInteger).hasTrait(DefaultTrait.class), is(false));
        assertThat(model.expectShape(fooBoxedInteger).hasTrait(DefaultTrait.class), is(false));

        // Add box to BoxedIntegerWithDefault because it has a default that isn't the v1 zero value.
        assertThat(model.expectShape(boxedIntegerWithDefault).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(fooBoxedIntegerWithDefault).hasTrait(BoxTrait.class), is(false)); // no need to box the member too
        assertThat(model.expectShape(boxedIntegerWithDefault).hasTrait(DefaultTrait.class), is(true));
        assertThat(model.expectShape(fooBoxedIntegerWithDefault).hasTrait(DefaultTrait.class), is(true));

        // No box trait on PrimitiveInteger because it has a zero value default.
        assertThat(model.expectShape(primitiveInteger).hasTrait(BoxTrait.class), is(false));
        assertThat(model.expectShape(fooPrimitiveInteger).hasTrait(BoxTrait.class), is(false));
        assertThat(model.expectShape(primitiveInteger).hasTrait(DefaultTrait.class), is(true));
        assertThat(model.expectShape(fooPrimitiveInteger).hasTrait(DefaultTrait.class), is(true));

        // Add box to BoxedIntEnum because it has no default trait.
        assertThat(model.expectShape(boxedIntEnum).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(fooBoxedIntEnum).hasTrait(BoxTrait.class), is(false)); // no need to box the member too
        assertThat(model.expectShape(boxedIntEnum).hasTrait(DefaultTrait.class), is(false));
        assertThat(model.expectShape(fooBoxedIntEnum).hasTrait(DefaultTrait.class), is(false));

        // Add box to BoxedIntEnumWithDefault because it has a default that isn't the v1 zero value.
        assertThat(model.expectShape(boxedIntEnumWithDefault).hasTrait(BoxTrait.class), is(true));
        assertThat(model.expectShape(fooBoxedIntEnumWithDefault).hasTrait(BoxTrait.class), is(false)); // no need to box the member too
        assertThat(model.expectShape(boxedIntEnumWithDefault).hasTrait(DefaultTrait.class), is(true));
        assertThat(model.expectShape(fooBoxedIntEnumWithDefault).hasTrait(DefaultTrait.class), is(true));

        // No box trait on PrimitiveIntEnum because it has a zero value default.
        assertThat(model.expectShape(primitiveIntEnum).hasTrait(BoxTrait.class), is(false));
        assertThat(model.expectShape(fooPrimitiveIntEnum).hasTrait(BoxTrait.class), is(false));
        assertThat(model.expectShape(primitiveIntEnum).hasTrait(DefaultTrait.class), is(true));
        assertThat(model.expectShape(fooPrimitiveIntEnum).hasTrait(DefaultTrait.class), is(true));
    }

    @Test
    public void doesNotFailWhenUpgradingWhenZeroValueIncompatibleWithRangeTrait() {
        ValidatedResult<Model> result = Model.assembler()
                .addImport(getClass().getResource("range-upgrade-1.0.smithy"))
                .assemble();

        assertThat(result.isBroken(), is(false));

        Model model1 = result.unwrap();
        ShapeId shapeId1 = ShapeId.from("smithy.example#MyLong1");
        Shape shape1 = model1.expectShape(shapeId1);

        assertThat(shape1.hasTrait(RangeTrait.class), is(true));
        // Make sure the range trait wasn't modified.
        assertThat(shape1.expectTrait(RangeTrait.class).getMin().get().toString(), equalTo("1"));
        assertThat(result.getValidationEvents().stream()
                           .anyMatch(event -> event.getMessage().contains("must be greater than or equal to 1")),
                   is(true));
    }
}
