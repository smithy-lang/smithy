/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.tagging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;

public final class AwsTagIndexTest {
    private static final String NAMESPACE = "example.weather";
    private static final ShapeId WEATHER_SERVICE_ID = ShapeId.fromParts(NAMESPACE, "Weather");
    private static final ShapeId UNTAGGED_SERVICE_ID = ShapeId.fromParts(NAMESPACE, "UntaggedService");
    private static final ShapeId CITY_RESOURCE_ID = ShapeId.fromParts(NAMESPACE, "City");

    private static Model model;
    private static AwsTagIndex tagIndex;

    @BeforeAll
    public static void loadModel() {
        model = Model.assembler()
                .addImport(AwsTagIndex.class.getResource("aws-tag-index-test-model.smithy"))
                .discoverModels()
                .assemble()
                .unwrap();
        tagIndex = AwsTagIndex.of(model);
    }

    @Test
    public void detectsCompliantTaggingService() {
        assertTrue(tagIndex.serviceHasTagApis(WEATHER_SERVICE_ID));
        assertTrue(tagIndex.serviceHasValidTagResourceOperation(WEATHER_SERVICE_ID));
        assertTrue(tagIndex.serviceHasValidUntagResourceOperation(WEATHER_SERVICE_ID));
        assertTrue(tagIndex.serviceHasValidListTagsForResourceOperation(WEATHER_SERVICE_ID));
    }

    @Test
    public void detectsNoncompliantTaggingService() {
        assertFalse(tagIndex.serviceHasTagApis(UNTAGGED_SERVICE_ID));
        assertFalse(tagIndex.serviceHasValidTagResourceOperation(UNTAGGED_SERVICE_ID));
        assertFalse(tagIndex.serviceHasValidUntagResourceOperation(UNTAGGED_SERVICE_ID));
        assertFalse(tagIndex.serviceHasValidListTagsForResourceOperation(UNTAGGED_SERVICE_ID));
    }

    @Test
    public void detectsDefaultTagOperations() {
        Optional<ShapeId> tagOptional = tagIndex.getTagResourceOperation(WEATHER_SERVICE_ID);
        assertTrue(tagOptional.isPresent());
        assertEquals(tagOptional.get(), ShapeId.fromParts(NAMESPACE, "TagResource"));

        Optional<ShapeId> untagOptional = tagIndex.getUntagResourceOperation(WEATHER_SERVICE_ID);
        assertTrue(untagOptional.isPresent());
        assertEquals(untagOptional.get(), ShapeId.fromParts(NAMESPACE, "UntagResource"));

        Optional<ShapeId> listTagsOptional = tagIndex.getListTagsForResourceOperation(WEATHER_SERVICE_ID);
        assertTrue(listTagsOptional.isPresent());
        assertEquals(listTagsOptional.get(), ShapeId.fromParts(NAMESPACE, "ListTagsForResource"));
    }

    @Test
    public void detectsResourceCustomizedTagOperations() {
        Optional<ShapeId> tagOptional = tagIndex.getTagResourceOperation(CITY_RESOURCE_ID);
        assertTrue(tagOptional.isPresent());
        assertEquals(tagOptional.get(), ShapeId.fromParts(NAMESPACE, "TagCity"));

        Optional<ShapeId> untagOptional = tagIndex.getUntagResourceOperation(CITY_RESOURCE_ID);
        assertTrue(untagOptional.isPresent());
        assertEquals(untagOptional.get(), ShapeId.fromParts(NAMESPACE, "UntagCity"));

        Optional<ShapeId> listTagsOptional = tagIndex.getListTagsForResourceOperation(CITY_RESOURCE_ID);
        assertTrue(listTagsOptional.isPresent());
        assertEquals(listTagsOptional.get(), ShapeId.fromParts(NAMESPACE, "ListTagsForCity"));
    }

    @ParameterizedTest
    @MethodSource("resourceTagMutabilities")
    public void detectsResourceTagMutation(ShapeId shapeId, boolean isTagOnCreate, boolean isTagOnUpdate) {
        assertEquals(tagIndex.isResourceTagOnCreate(shapeId), isTagOnCreate);
        assertEquals(tagIndex.isResourceTagOnUpdate(shapeId), isTagOnUpdate);
    }

    public static Stream<Arguments> resourceTagMutabilities() {
        return Stream.of(
                Arguments.of(CITY_RESOURCE_ID, false, false),
                Arguments.of(ShapeId.fromParts(NAMESPACE, "Town"), true, true),
                Arguments.of(ShapeId.fromParts(NAMESPACE, "Farm"), true, true),
                Arguments.of(ShapeId.fromParts(NAMESPACE, "Barn"), true, false),
                Arguments.of(ShapeId.fromParts(NAMESPACE, "Silo"), true, true));
    }

    @Test
    public void resolvesTagMember() {
        assertFalse(tagIndex.getTagsMember(ShapeId.fromParts(NAMESPACE, "GetCity")).isPresent());

        Optional<MemberShape> tagMember = tagIndex.getTagsMember(ShapeId.fromParts(NAMESPACE, "CreateTown"));
        assertTrue(tagMember.isPresent());
        assertEquals(tagMember.get().toShapeId(), ShapeId.fromParts(NAMESPACE, "CreateTownInput", "tags"));
    }
}
