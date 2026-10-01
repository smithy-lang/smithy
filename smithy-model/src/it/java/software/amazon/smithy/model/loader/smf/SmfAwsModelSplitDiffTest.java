/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.loader.ModelDiscovery;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * Differential integration test that compares flat SMF and split hot/cold-trait SMF
 * on AWS models.
 *
 * <p>Run with:
 * {@code ./gradlew :smithy-model:integ -PawsModelsTests --tests "*SmfAwsModelSplitDiffTest*"}
 *
 * <p>Use {@code -DsmfAwsModelFilter=sts,cloudwatch} to restrict the tested models.
 * Set {@code -DsmfAwsModelFilterMode=exact} to match exact artifact names rather than substrings.
 */
@EnabledIfSystemProperty(named = "awsModelsTests", matches = "true")
@Execution(ExecutionMode.CONCURRENT)
public class SmfAwsModelSplitDiffTest {

    private static final Set<String> COLD_TRAITS = new LinkedHashSet<>(Arrays.asList(
            "smithy.api#documentation",
            "smithy.api#examples",
            "smithy.rules#endpointTests"));

    static Stream<Named<URL>> awsModels() {
        return ModelDiscovery.findModels(SmfAwsModelSplitDiffTest.class.getClassLoader())
                .stream()
                .filter(url -> url.toString().endsWith(".json"))
                .map(url -> Named.of(artifactName(url), url))
                .filter(named -> includeModel(named.getName()))
                .sorted(Comparator.comparing(Named::getName));
    }

    private static boolean includeModel(String name) {
        String filter = System.getProperty("smfAwsModelFilter", "").trim();
        if (filter.isEmpty()) {
            return true;
        }

        String mode = System.getProperty("smfAwsModelFilterMode", "contains").trim().toLowerCase();
        String lowerName = name.toLowerCase();
        for (String token : filter.split(",")) {
            String lowered = token.trim().toLowerCase();
            if (lowered.isEmpty()) {
                continue;
            }
            if ("exact".equals(mode) ? lowerName.equals(lowered) : lowerName.contains(lowered)) {
                return true;
            }
        }

        return false;
    }

    private static String artifactName(URL modelUrl) {
        String urlStr = modelUrl.toString();
        int bangIdx = urlStr.indexOf("!/");
        if (bangIdx < 0) {
            return urlStr;
        }
        String jarPath = urlStr.substring(0, bangIdx);
        String jarName = jarPath.substring(jarPath.lastIndexOf('/') + 1);
        return jarName.replaceFirst("-\\d[\\d.]*\\.jar$", "");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("awsModels")
    void splitFormatMatchesFlatFormat(URL modelUrl) {
        Model original = Model.assembler()
                .addImport(modelUrl)
                .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                .disableValidation()
                .assemble()
                .unwrap();

        byte[] flatBytes = SmfWriter.builder().build().serialize(original);
        byte[] splitBytes = SmfWriter.builder()
                .splitColdTraits(true)
                .coldTraitFilter(trait -> COLD_TRAITS.contains(trait.toShapeId().toString()))
                .build()
                .serialize(original);

        Model flat = SmfReader.read(flatBytes, false);
        Model split = SmfReader.read(splitBytes, false);
        assertModelsEquivalent(flat, split, Set.of(), "full");

        for (ServiceShape service : original.getServiceShapes()) {
            for (OperationShape operation : selectRepresentativeOperations(original, service)) {
                SelectiveLoadRequest request = SelectiveLoadRequest.builder()
                        .service(service.getId())
                        .addOperation(operation.getId())
                        .verifyCrc(false)
                        .build();

                Model flatSelective = SmfReader.readSelective(flatBytes, request);
                Model splitSelective = SmfReader.readSelective(splitBytes, request);
                assertModelsEquivalent(flatSelective,
                        splitSelective,
                        COLD_TRAITS,
                        "selective " + service.getId() + " -> " + operation.getId());
                assertRemovedTraitsAreCold(flatSelective,
                        splitSelective,
                        "selective " + service.getId() + " -> " + operation.getId());
            }
        }
    }

    private static void assertModelsEquivalent(
            Model expected,
            Model actual,
            Set<String> ignoredTraitIds,
            String context
    ) {
        List<String> failures = new ArrayList<>();

        Set<ShapeId> expectedIds = nonPreludeShapeIds(expected);
        Set<ShapeId> actualIds = nonPreludeShapeIds(actual);
        if (!expectedIds.equals(actualIds)) {
            failures.add(context + ": shape ID sets differ");
        }

        Set<ShapeId> commonIds = new LinkedHashSet<>(expectedIds);
        commonIds.retainAll(actualIds);
        for (ShapeId id : commonIds) {
            Shape expectedShape = expected.expectShape(id);
            Shape actualShape = actual.expectShape(id);

            if (expectedShape.getType() != actualShape.getType()) {
                failures.add(context + " " + id + ": type mismatch " + expectedShape.getType()
                        + " vs " + actualShape.getType());
                continue;
            }

            if (!expectedShape.getMemberNames().equals(actualShape.getMemberNames())) {
                failures.add(context + " " + id + ": member order mismatch");
            }

            Map<String, Node> expectedTraits = traitNodes(expectedShape, ignoredTraitIds);
            Map<String, Node> actualTraits = traitNodes(actualShape, ignoredTraitIds);
            if (!expectedTraits.equals(actualTraits)) {
                failures.add(context + " " + id + ": trait mismatch");
            }
        }

        if (!expected.getMetadata().equals(actual.getMetadata())) {
            failures.add(context + ": metadata mismatch");
        }

        if (!failures.isEmpty()) {
            fail("Model diff failures (" + failures.size() + "):\n  "
                    + String.join("\n  ", failures.subList(0, Math.min(failures.size(), 20)))
                    + (failures.size() > 20 ? "\n  ... and " + (failures.size() - 20) + " more" : ""));
        }
    }

    private static void assertRemovedTraitsAreCold(Model flat, Model split, String context) {
        List<String> failures = new ArrayList<>();
        for (ShapeId id : nonPreludeShapeIds(flat)) {
            Shape flatShape = flat.expectShape(id);
            Shape splitShape = split.expectShape(id);

            Set<String> flatTraitIds = flatShape.getAllTraits()
                    .keySet()
                    .stream()
                    .map(ShapeId::toString)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<String> splitTraitIds = splitShape.getAllTraits()
                    .keySet()
                    .stream()
                    .map(ShapeId::toString)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            Set<String> removed = new LinkedHashSet<>(flatTraitIds);
            removed.removeAll(splitTraitIds);
            if (!COLD_TRAITS.containsAll(removed)) {
                removed.removeAll(COLD_TRAITS);
                failures.add(context + " " + id + ": removed non-cold traits " + removed);
            }

            Set<String> added = new LinkedHashSet<>(splitTraitIds);
            added.removeAll(flatTraitIds);
            if (!added.isEmpty()) {
                failures.add(context + " " + id + ": split added unexpected traits " + added);
            }
        }

        if (!failures.isEmpty()) {
            fail("Removed-trait failures (" + failures.size() + "):\n  "
                    + String.join("\n  ", failures.subList(0, Math.min(failures.size(), 20)))
                    + (failures.size() > 20 ? "\n  ... and " + (failures.size() - 20) + " more" : ""));
        }
    }

    private static Set<ShapeId> nonPreludeShapeIds(Model model) {
        return model.toSet()
                .stream()
                .map(Shape::getId)
                .filter(id -> !Prelude.isPreludeShape(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Map<String, Node> traitNodes(Shape shape, Set<String> ignoredTraitIds) {
        Map<String, Node> result = new TreeMap<>();
        for (Map.Entry<ShapeId, Trait> entry : shape.getAllTraits().entrySet()) {
            String traitId = entry.getKey().toString();
            // Skip synthetic interop traits (e.g. smithy.api#box); SMF does not
            // preserve them and consumers use Smithy 2.0 semantics.
            if (!entry.getValue().isSynthetic() && !ignoredTraitIds.contains(traitId)) {
                result.put(traitId, entry.getValue().toNode());
            }
        }
        return result;
    }

    private static List<OperationShape> selectRepresentativeOperations(Model model, ServiceShape service) {
        List<OperationShape> operations = service.getOperations()
                .stream()
                .map(id -> model.expectShape(id, OperationShape.class))
                .sorted(Comparator.comparingInt(op -> computeSelectiveClosureSize(model, service, op)))
                .collect(Collectors.toList());

        if (operations.isEmpty()) {
            return List.of();
        }

        List<OperationShape> selected = new ArrayList<>();
        selected.add(operations.get(0));
        selected.add(operations.get(operations.size() / 2));
        selected.add(operations.get(operations.size() - 1));

        return selected.stream()
                .collect(Collectors.toMap(Shape::getId, op -> op, (left, right) -> left, java.util.LinkedHashMap::new))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    private static int computeSelectiveClosureSize(Model model, ServiceShape service, OperationShape operation) {
        Set<ShapeId> closure = new LinkedHashSet<>();
        ArrayDeque<ShapeId> queue = new ArrayDeque<>();

        closure.add(service.getId());
        for (ShapeId neighbor : topLevelNeighbors(service)) {
            Optional<Shape> maybeShape = model.getShape(neighbor);
            if (!maybeShape.isPresent()) {
                continue;
            }
            Shape shape = maybeShape.get();
            if (shape.isOperationShape() || shape.isResourceShape()) {
                continue;
            }
            closure.add(neighbor);
        }

        if (closure.add(operation.getId())) {
            queue.add(operation.getId());
        }

        while (!queue.isEmpty()) {
            ShapeId current = queue.removeFirst();
            Shape shape = model.expectShape(current);
            for (ShapeId neighbor : topLevelNeighbors(shape)) {
                if (closure.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return closure.size();
    }

    private static Set<ShapeId> topLevelNeighbors(Shape shape) {
        Set<ShapeId> neighbors = new LinkedHashSet<>();

        shape.members().forEach(member -> addNonPrelude(neighbors, member.getTarget()));

        if (shape instanceof CollectionShape) {
            addNonPrelude(neighbors, ((CollectionShape) shape).getMember().getTarget());
        } else if (shape instanceof MapShape) {
            MapShape map = (MapShape) shape;
            addNonPrelude(neighbors, map.getKey().getTarget());
            addNonPrelude(neighbors, map.getValue().getTarget());
        } else if (shape instanceof OperationShape) {
            OperationShape operation = (OperationShape) shape;
            operation.getInput().ifPresent(id -> addNonPrelude(neighbors, id));
            operation.getOutput().ifPresent(id -> addNonPrelude(neighbors, id));
            operation.getErrors().forEach(id -> addNonPrelude(neighbors, id));
        } else if (shape instanceof ResourceShape) {
            ResourceShape resource = (ResourceShape) shape;
            resource.getPut().ifPresent(id -> addNonPrelude(neighbors, id));
            resource.getCreate().ifPresent(id -> addNonPrelude(neighbors, id));
            resource.getRead().ifPresent(id -> addNonPrelude(neighbors, id));
            resource.getUpdate().ifPresent(id -> addNonPrelude(neighbors, id));
            resource.getDelete().ifPresent(id -> addNonPrelude(neighbors, id));
            resource.getList().ifPresent(id -> addNonPrelude(neighbors, id));
            resource.getOperations().forEach(id -> addNonPrelude(neighbors, id));
            resource.getCollectionOperations().forEach(id -> addNonPrelude(neighbors, id));
            resource.getResources().forEach(id -> addNonPrelude(neighbors, id));
            resource.getIdentifiers().values().forEach(id -> addNonPrelude(neighbors, id));
            resource.getProperties().values().forEach(id -> addNonPrelude(neighbors, id));
        } else if (shape instanceof ServiceShape) {
            ServiceShape service = (ServiceShape) shape;
            service.getOperations().forEach(id -> addNonPrelude(neighbors, id));
            service.getResources().forEach(id -> addNonPrelude(neighbors, id));
            service.getErrors().forEach(id -> addNonPrelude(neighbors, id));
        }

        shape.getMixins().forEach(id -> addNonPrelude(neighbors, id));
        return neighbors;
    }

    private static void addNonPrelude(Set<ShapeId> neighbors, ShapeId id) {
        if (!"smithy.api".equals(id.getNamespace())) {
            neighbors.add(id);
        }
    }
}
