/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.aws.cloudformation.traits.CfnResourceIndex.Mutability;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SetUtils;

public class CfnResourceIndexTest {
    private static final ShapeId FOO = ShapeId.from("smithy.example#FooResource");
    private static final ShapeId BAR = ShapeId.from("smithy.example#BarResource");
    private static final ShapeId BAZ = ShapeId.from("smithy.example#BazResource");

    private static Model model;
    private static CfnResourceIndex cfnResourceIndex;

    @BeforeAll
    public static void loadTestModel() {
        model = Model.assembler()
                .discoverModels(CfnResourceIndexTest.class.getClassLoader())
                .addImport(CfnResourceIndexTest.class.getResource("test-service.smithy"))
                .assemble()
                .unwrap();
        cfnResourceIndex = CfnResourceIndex.of(model);
    }

    private static class ResourceData {
        ShapeId resourceId;
        Collection<String> identifiers;
        List<Set<String>> additionalIdentifiers;
        Map<String, Collection<Mutability>> mutabilities;
        Set<String> createOnlyProperties;
        Set<String> readOnlyProperties;
        Set<String> writeOnlyProperties;
    }

    public static Collection<ResourceData> data() {
        ResourceData fooResource = new ResourceData();
        fooResource.resourceId = FOO;
        fooResource.identifiers = SetUtils.of("fooId");
        fooResource.additionalIdentifiers = ListUtils.of();
        fooResource.mutabilities = MapUtils.of(
                "fooId",
                SetUtils.of(Mutability.READ),
                "fooValidFullyMutableProperty",
                CfnResourceIndex.FULLY_MUTABLE,
                "fooValidCreateProperty",
                SetUtils.of(Mutability.CREATE),
                "fooValidCreateReadProperty",
                SetUtils.of(Mutability.CREATE, Mutability.READ),
                "fooValidReadProperty",
                SetUtils.of(Mutability.READ),
                "fooValidWriteProperty",
                SetUtils.of(Mutability.WRITE));
        fooResource.createOnlyProperties = SetUtils.of("fooValidCreateProperty", "fooValidCreateReadProperty");
        fooResource.readOnlyProperties = SetUtils.of("fooId", "fooValidReadProperty");
        fooResource.writeOnlyProperties = SetUtils.of("fooValidWriteProperty", "fooValidCreateProperty");

        ResourceData barResource = new ResourceData();
        barResource.resourceId = BAR;
        barResource.identifiers = SetUtils.of("barId");
        barResource.additionalIdentifiers = ListUtils.of(SetUtils.of("arn"));
        barResource.mutabilities = MapUtils.of(
                "barId",
                SetUtils.of(Mutability.CREATE, Mutability.READ),
                "arn",
                SetUtils.of(Mutability.READ),
                "barExplicitMutableProperty",
                CfnResourceIndex.FULLY_MUTABLE,
                "barValidAdditionalProperty",
                SetUtils.of(),
                "barImplicitReadProperty",
                SetUtils.of(Mutability.READ),
                "barImplicitFullProperty",
                CfnResourceIndex.FULLY_MUTABLE);
        barResource.createOnlyProperties = SetUtils.of("barId");
        barResource.readOnlyProperties = SetUtils.of("arn", "barImplicitReadProperty");
        barResource.writeOnlyProperties = SetUtils.of();

        ResourceData bazResource = new ResourceData();
        bazResource.resourceId = BAZ;
        bazResource.identifiers = SetUtils.of("barId", "bazId");
        bazResource.additionalIdentifiers = ListUtils.of();
        bazResource.mutabilities = MapUtils.of(
                "barId",
                SetUtils.of(Mutability.CREATE, Mutability.READ),
                "bazId",
                SetUtils.of(Mutability.READ),
                "bazExplicitMutableProperty",
                CfnResourceIndex.FULLY_MUTABLE,
                "bazImplicitFullyMutableProperty",
                CfnResourceIndex.FULLY_MUTABLE,
                "bazImplicitCreateProperty",
                SetUtils.of(Mutability.CREATE, Mutability.READ),
                "bazImplicitReadProperty",
                SetUtils.of(Mutability.READ),
                "bazImplicitWriteProperty",
                SetUtils.of(Mutability.CREATE, Mutability.WRITE));
        bazResource.createOnlyProperties = SetUtils.of("barId", "bazImplicitCreateProperty");
        bazResource.readOnlyProperties = SetUtils.of("bazId", "bazImplicitReadProperty");
        bazResource.writeOnlyProperties = SetUtils.of("bazImplicitWriteProperty");

        return ListUtils.of(fooResource, barResource, bazResource);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void detectsPrimaryIdentifiers(ResourceData data) {
        assertThat(String.format("Failure for resource %s.", data.resourceId),
                cfnResourceIndex.getResource(data.resourceId).get().getPrimaryIdentifiers(),
                containsInAnyOrder(data.identifiers.toArray()));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void detectsAdditionalIdentifiers(ResourceData data) {
        assertThat(String.format("Failure for resource %s.", data.resourceId),
                cfnResourceIndex.getResource(data.resourceId).get().getAdditionalIdentifiers(),
                containsInAnyOrder(data.additionalIdentifiers.toArray()));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void findsAllProperties(ResourceData data) {
        Map<String, CfnResourceProperty> properties = cfnResourceIndex.getResource(data.resourceId)
                .get()
                .getProperties();

        assertThat(properties.keySet(), containsInAnyOrder(data.mutabilities.keySet().toArray()));
        properties.forEach((name, definition) -> {
            assertThat(String.format("Mismatch on property %s for %s.", name, data.resourceId),
                    definition.getMutabilities(),
                    containsInAnyOrder(data.mutabilities.get(name).toArray()));
        });
    }

    @ParameterizedTest
    @MethodSource("data")
    public void findsCreateOnlyProperties(ResourceData data) {
        Set<String> properties = cfnResourceIndex.getResource(data.resourceId).get().getCreateOnlyProperties();

        assertThat(String.format("Failure for resource %s.", data.resourceId),
                properties,
                containsInAnyOrder(data.createOnlyProperties.toArray()));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void findsReadOnlyProperties(ResourceData data) {
        Set<String> properties = cfnResourceIndex.getResource(data.resourceId).get().getReadOnlyProperties();

        assertThat(String.format("Failure for resource %s.", data.resourceId),
                properties,
                containsInAnyOrder(data.readOnlyProperties.toArray()));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void findsWriteOnlyProperties(ResourceData data) {
        Set<String> properties = cfnResourceIndex.getResource(data.resourceId).get().getWriteOnlyProperties();

        assertThat(String.format("Failure for resource %s.", data.resourceId),
                properties,
                containsInAnyOrder(data.writeOnlyProperties.toArray()));
    }

    @Test
    public void setsProperIdentifierMutability() {
        Map<String, CfnResourceProperty> fooProperties = cfnResourceIndex.getResource(FOO)
                .get()
                .getProperties();
        Map<String, CfnResourceProperty> barProperties = cfnResourceIndex.getResource(BAR)
                .get()
                .getProperties();

        assertThat(fooProperties.get("fooId").getMutabilities(), containsInAnyOrder(Mutability.READ));
        assertThat(barProperties.get("barId").getMutabilities(),
                containsInAnyOrder(Mutability.CREATE, Mutability.READ));
    }

    @Test
    public void handlesAdditionalSchemaProperty() {
        Map<String, CfnResourceProperty> barProperties = cfnResourceIndex.getResource(BAR)
                .get()
                .getProperties();

        assertTrue(barProperties.containsKey("barValidAdditionalProperty"));
        assertTrue(barProperties.get("barValidAdditionalProperty").getMutabilities().isEmpty());
        assertFalse(barProperties.containsKey("barValidExcludedProperty"));
    }
}
