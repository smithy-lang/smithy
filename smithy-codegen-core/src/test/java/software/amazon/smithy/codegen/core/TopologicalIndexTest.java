/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
import software.amazon.smithy.model.selector.PathFinder;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.FunctionalUtils;

public class TopologicalIndexTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(TopologicalIndexTest.class.getResource("topological-sort.smithy"))
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void sortsTopologically() {
        TopologicalIndex index = TopologicalIndex.of(model);

        List<String> ordered = new ArrayList<>();
        for (Shape shape : index.getOrderedShapes()) {
            ordered.add(shape.getId().toString());
        }

        List<String> recursive = new ArrayList<>();
        for (Shape shape : index.getRecursiveShapes()) {
            recursive.add(shape.getId().toString());
        }

        assertThat(ordered,
                contains(
                        "smithy.example#MyString",
                        "smithy.example#BamList$member",
                        "smithy.example#BamList",
                        "smithy.example#Bar$bam",
                        "smithy.api#Integer",
                        "smithy.example#Bar$baz",
                        "smithy.example#Bar",
                        "smithy.example#Foo$bar",
                        "smithy.example#Foo$foo",
                        "smithy.example#Foo"));

        assertThat(recursive,
                contains(
                        "smithy.example#Recursive",
                        "smithy.example#Recursive$b",
                        "smithy.example#RecursiveList$member",
                        "smithy.example#RecursiveList",
                        "smithy.example#Recursive$a"));
    }

    @Test
    public void checksIfShapeByIdIsRecursive() {
        TopologicalIndex index = TopologicalIndex.of(model);

        assertThat(index.isRecursive(ShapeId.from("smithy.example#Recursive$b")), is(true));
        assertThat(index.isRecursive(ShapeId.from("smithy.example#MyString")), is(false));
    }

    @Test
    public void checksIfShapeIsRecursive() {
        TopologicalIndex index = TopologicalIndex.of(model);

        assertThat(index.isRecursive(model.expectShape(ShapeId.from("smithy.example#MyString"))), is(false));
        assertThat(index.isRecursive(model.expectShape(ShapeId.from("smithy.example#Recursive$b"))), is(true));
    }

    @Test
    public void getsRecursiveClosureById() {
        TopologicalIndex index = TopologicalIndex.of(model);

        assertThat(index.getRecursiveClosure(ShapeId.from("smithy.example#MyString")), empty());
        assertThat(index.getRecursiveClosure(ShapeId.from("smithy.example#Recursive$b")), not(empty()));
    }

    @Test
    public void getsRecursiveClosureByShape() {
        TopologicalIndex index = TopologicalIndex.of(model);

        assertThat(index.getRecursiveClosure(model.expectShape(ShapeId.from("smithy.example#MyString"))),
                empty());
        assertThat(index.getRecursiveClosure(model.expectShape(ShapeId.from("smithy.example#Recursive$b"))),
                not(empty()));
    }

    @Test
    public void handlesMoreRecursion() {
        Model recursive = Model.assembler()
                .addImport(getClass().getResource("topological-recursion.smithy"))
                .assemble()
                .unwrap();
        TopologicalIndex index = TopologicalIndex.of(recursive);

        // The topological index must capture all shapes in the index not in the prelude.
        Set<Shape> nonPrelude = recursive.shapes()
                .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                .collect(Collectors.toSet());
        Set<Shape> topologicalShapes = new HashSet<>(index.getOrderedShapes());
        topologicalShapes.addAll(index.getRecursiveShapes());
        assertThat(topologicalShapes, equalTo(nonPrelude));

        // The ordered shape IDs must be in the this order.
        List<String> orderedIds = index.getOrderedShapes()
                .stream()
                .map(Shape::getId)
                .map(ShapeId::toString)
                .collect(Collectors.toList());
        assertThat(orderedIds,
                contains(
                        "smithy.example#MyString",
                        "smithy.example#NonRecursive$foo",
                        "smithy.example#NonRecursive",
                        "smithy.example#NonRecursiveList$member",
                        "smithy.example#NonRecursiveList",
                        "smithy.example#User$notRecursive",
                        "smithy.example#UsersMap$key"));

        List<String> recursiveIds = index.getRecursiveShapes()
                .stream()
                .map(Shape::getId)
                .map(ShapeId::toString)
                .collect(Collectors.toList());
        assertThat(recursiveIds,
                contains(
                        "smithy.example#User",
                        "smithy.example#User$recursiveUser",
                        "smithy.example#UsersList$member",
                        "smithy.example#UsersMap$value",
                        "smithy.example#GetFooInput$foo",
                        "smithy.example#UsersList",
                        "smithy.example#UsersMap",
                        "smithy.example#GetFooInput",
                        "smithy.example#User$recursiveList",
                        "smithy.example#User$recursiveMap",
                        "smithy.example#GetFoo",
                        "smithy.example#Example"));

        for (String recursiveId : recursiveIds) {
            ShapeId id = ShapeId.from(recursiveId);
            // Must be considered recursive.
            assertThat(index.isRecursive(id), is(true));
            // Must not be in the ordered set.
            assertThat(index.getOrderedShapes().contains(recursive.expectShape(id)), is(false));
            // Must not have an empty recursion closure.
            assertThat(index.getRecursiveClosure(id), not(empty()));
        }
    }

    @Test
    public void handlesDiamondChainsQuickly() {
        Model diamonds = Model.assembler()
                .addUnparsedModel("diamonds.smithy", diamondIdl(40, false))
                .assemble()
                .unwrap();

        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            TopologicalIndex index = new TopologicalIndex(diamonds);
            Set<Shape> nonPrelude = nonPreludeShapes(diamonds);

            assertThat(new HashSet<>(index.getOrderedShapes()).containsAll(nonPrelude), is(true));
            assertThat(index.getRecursiveShapes(), empty());
        });
    }

    @Test
    public void handlesRecursiveDiamondChainsQuickly() {
        Model diamonds = Model.assembler()
                .addUnparsedModel("diamonds.smithy", diamondIdl(40, true))
                .assemble()
                .unwrap();

        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            TopologicalIndex index = new TopologicalIndex(diamonds);

            assertThat(index.getOrderedShapes(), empty());
            assertThat(new HashSet<>(index.getRecursiveShapes()), equalTo(nonPreludeShapes(diamonds)));
            assertThat(index.isTruncated(), is(true));
            assertThat(index.isTruncated(ShapeId.from("diamond#S0")), is(true));
            // S40 and L only reach their own cycle, so their single path is kept.
            assertThat(index.isTruncated(ShapeId.from("diamond#S40")), is(false));
            for (Shape shape : nonPreludeShapes(diamonds)) {
                Set<PathFinder.Path> closure = index.getRecursiveClosure(shape.getId());
                assertThat(closure, not(empty()));
                for (PathFinder.Path path : closure) {
                    assertThat(path.getStartShape(), equalTo(shape));
                    Set<ShapeId> seen = new HashSet<>();
                    for (Relationship rel : path) {
                        seen.add(rel.getShape().getId());
                    }
                    // The last edge points back to a shape already on the path.
                    assertThat(seen.contains(path.get(path.size() - 1).getNeighborShapeId()), is(true));
                }
            }
        });
    }

    @Test
    public void handlesDeepChainsWithoutStackOverflow() {
        int length = 20000;
        Model chain = Model.assembler()
                .addUnparsedModel("chain.smithy", linearChainIdl(length, false))
                .assemble()
                .unwrap();

        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            TopologicalIndex index = new TopologicalIndex(chain);

            assertThat(index.getRecursiveShapes(), empty());
            List<String> ordered = index.getOrderedShapes()
                    .stream()
                    .limit(3)
                    .map(shape -> shape.getId().toString())
                    .collect(Collectors.toList());
            assertThat(ordered, contains("chain#L20000", "chain#L19999$m", "chain#L19999"));
        });

        Model cycle = Model.assembler()
                .addUnparsedModel("chain.smithy", linearChainIdl(length, true))
                .assemble()
                .unwrap();

        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            TopologicalIndex index = new TopologicalIndex(cycle);

            assertThat(index.getOrderedShapes(), empty());
            assertThat(new HashSet<>(index.getRecursiveShapes()), equalTo(nonPreludeShapes(cycle)));
            assertThat(index.getRecursiveClosure(ShapeId.from("chain#L20000")), not(empty()));
        });
    }

    @Test
    public void truncatesClosuresToBudgetPrefix() {
        Model recursive = Model.assembler()
                .addImport(getClass().getResource("topological-recursion.smithy"))
                .assemble()
                .unwrap();
        TopologicalIndex full = new TopologicalIndex(recursive);
        TopologicalIndex small = new TopologicalIndex(recursive, 5);

        assertThat(new ArrayList<>(small.getOrderedShapes()), equalTo(new ArrayList<>(full.getOrderedShapes())));
        assertThat(new HashSet<>(small.getRecursiveShapes()), equalTo(new HashSet<>(full.getRecursiveShapes())));

        boolean shorter = false;
        for (Shape shape : full.getRecursiveShapes()) {
            List<PathFinder.Path> fullClosure = new ArrayList<>(full.getRecursiveClosure(shape));
            List<PathFinder.Path> smallClosure = new ArrayList<>(small.getRecursiveClosure(shape));
            assertThat(smallClosure, not(empty()));
            assertThat(smallClosure.size() <= fullClosure.size(), is(true));
            assertThat(smallClosure, equalTo(fullClosure.subList(0, smallClosure.size())));
            shorter |= smallClosure.size() < fullClosure.size();
            assertThat(small.isRecursive(shape), is(true));
            assertThat(small.isTruncated(shape), is(smallClosure.size() < fullClosure.size()));
            assertThat(full.isTruncated(shape), is(false));
        }
        assertThat(shorter, is(true));
        assertThat(small.isTruncated(), is(true));
        assertThat(full.isTruncated(), is(false));
    }

    @Test
    public void keepsOneClosurePathPerShapeWhenBudgetIsSpent() {
        Model recursive = Model.assembler()
                .addImport(getClass().getResource("topological-recursion.smithy"))
                .assemble()
                .unwrap();
        TopologicalIndex full = new TopologicalIndex(recursive);
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        Logger logger = Logger.getLogger(TopologicalIndex.class.getName());
        logger.addHandler(handler);
        TopologicalIndex index;
        try {
            index = new TopologicalIndex(recursive, 1);
        } finally {
            logger.removeHandler(handler);
        }

        List<Shape> expected = new ArrayList<>();
        List<String> truncated = new ArrayList<>();
        for (Shape shape : full.getRecursiveShapes()) {
            Set<PathFinder.Path> closure = index.getRecursiveClosure(shape);
            assertThat(closure.size(), equalTo(1));
            assertThat(index.isTruncated(shape), is(full.getRecursiveClosure(shape).size() > 1));
            if (index.isTruncated(shape)) {
                truncated.add(shape.getId().toString());
            }
            // The kept path is the first one of the full closure.
            assertThat(closure.iterator().next(), equalTo(full.getRecursiveClosure(shape).iterator().next()));
            expected.add(shape);
        }

        expected.sort(Comparator.<Shape>comparingInt(s -> index.getRecursiveClosure(s).iterator().next().size())
                .thenComparing(Comparator.naturalOrder()));
        assertThat(new ArrayList<>(index.getRecursiveShapes()), equalTo(expected));

        assertThat(index.isTruncated(), is(true));
        assertThat(truncated, not(empty()));
        assertThat(records.size(), equalTo(1));
        String message = records.get(0).getMessage();
        assertThat(message, containsString(truncated.size() + " shapes have truncated closures: "));
        Collections.sort(truncated);
        for (String id : truncated.subList(0, Math.min(10, truncated.size()))) {
            assertThat(message, containsString(id));
        }
    }

    @Test
    public void matchesReferenceImplementationOnRandomModels() {
        Random random = new Random(1);
        for (int t = 0; t < 300; t++) {
            String idl = randomIdl(random);
            Model model = Model.assembler()
                    .addUnparsedModel("random.smithy", idl)
                    .disableValidation()
                    .assemble()
                    .unwrap();
            ReferenceIndex expected = new ReferenceIndex(model);
            TopologicalIndex actual = new TopologicalIndex(model);
            assertThat(idl, actual.isTruncated(), is(false));

            assertThat(idl, new ArrayList<>(actual.getOrderedShapes()), equalTo(expected.shapes));
            assertThat(idl,
                    new ArrayList<>(actual.getRecursiveShapes()),
                    equalTo(new ArrayList<>(expected.recursiveShapes.keySet())));
            for (Map.Entry<Shape, List<PathFinder.Path>> entry : expected.recursiveShapes.entrySet()) {
                assertThat(idl,
                        new ArrayList<>(actual.getRecursiveClosure(entry.getKey())),
                        equalTo(entry.getValue()));
            }
        }
    }

    private static String randomIdl(Random random) {
        int n = 2 + random.nextInt(9);
        StringBuilder sb = new StringBuilder("$version: \"2.0\"\nnamespace r\n");
        for (int i = 0; i < n; i++) {
            int kind = random.nextInt(4);
            if (kind == 0) {
                sb.append("list X").append(i).append(" { member: X").append(random.nextInt(n)).append(" }\n");
            } else if (kind == 1) {
                sb.append("map X")
                        .append(i)
                        .append(" { key: String, value: X")
                        .append(random.nextInt(n))
                        .append(" }\n");
            } else {
                sb.append(kind == 2 ? "structure" : "union").append(" X").append(i).append(" {\n");
                int members = random.nextInt(4);
                for (int j = 0; j < members; j++) {
                    sb.append(" m")
                            .append(j)
                            .append(": ")
                            .append(random.nextInt(5) == 0 ? "String" : "X" + random.nextInt(n))
                            .append("\n");
                }
                sb.append("}\n");
            }
        }
        return sb.toString();
    }

    // A copy of the original path-enumerating algorithm, used as an oracle on small models.
    private static final class ReferenceIndex {
        private final List<Shape> shapes = new ArrayList<>();
        private final Map<Shape, List<PathFinder.Path>> recursiveShapes = new LinkedHashMap<>();

        ReferenceIndex(Model model) {
            Set<Shape> roots = new TreeSet<>(nonPreludeShapes(model));
            Map<Integer, Map<Shape, List<PathFinder.Path>>> frequencyMap = new TreeMap<>();
            NeighborProvider provider = NeighborProviderIndex.of(model).getProvider();
            for (Shape shape : roots) {
                Set<PathFinder.Path> paths = explore(shape, Collections.emptyList(), Collections.emptySet(), provider);
                if (!paths.isEmpty()) {
                    int edges = 0;
                    for (PathFinder.Path path : paths) {
                        edges += path.size();
                    }
                    frequencyMap.computeIfAbsent(edges, e -> new LinkedHashMap<>())
                            .put(shape, new ArrayList<>(paths));
                }
            }
            for (Map<Shape, List<PathFinder.Path>> entry : frequencyMap.values()) {
                recursiveShapes.putAll(entry);
            }
        }

        private Set<PathFinder.Path> explore(
                Shape shape,
                List<Relationship> path,
                Set<Shape> visited,
                NeighborProvider provider
        ) {
            if (visited.contains(shape)) {
                return Collections.singleton(new PathFinder.Path(path));
            }
            Set<Shape> newVisited = new LinkedHashSet<>(visited);
            newVisited.add(shape);
            Map<Shape, Relationship> shapeRelationshipMap = new TreeMap<>();
            for (Relationship rel : provider.getNeighbors(shape)) {
                if (rel.getRelationshipType().getDirection() == RelationshipDirection.DIRECTED) {
                    if (!rel.getNeighborShapeId().equals(shape.getId()) && rel.getNeighborShape().isPresent()) {
                        shapeRelationshipMap.put(rel.getNeighborShape().get(), rel);
                    }
                }
            }
            Set<PathFinder.Path> recursivePaths = new LinkedHashSet<>();
            for (Map.Entry<Shape, Relationship> entry : shapeRelationshipMap.entrySet()) {
                List<Relationship> newPath = new ArrayList<>(path);
                newPath.add(entry.getValue());
                recursivePaths.addAll(explore(entry.getKey(), newPath, newVisited, provider));
            }
            if (recursivePaths.isEmpty() && !shapes.contains(shape)) {
                shapes.add(shape);
            }
            return recursivePaths;
        }
    }

    private static Set<Shape> nonPreludeShapes(Model model) {
        return model.shapes()
                .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                .collect(Collectors.toSet());
    }

    // Chained diamonds: S(i) -> A(i), B(i) -> S(i + 1). Optionally the last shape points back through a list.
    private static String diamondIdl(int n, boolean recursiveEnd) {
        StringBuilder sb = new StringBuilder("$version: \"2.0\"\nnamespace diamond\n\n");
        for (int i = 0; i < n; i++) {
            sb.append("structure S")
                    .append(i)
                    .append(" {\n    a: A")
                    .append(i)
                    .append("\n    b: B")
                    .append(i)
                    .append("\n}\n");
            sb.append("structure A").append(i).append(" {\n    m: S").append(i + 1).append("\n}\n");
            sb.append("structure B").append(i).append(" {\n    m: S").append(i + 1).append("\n}\n");
        }
        if (recursiveEnd) {
            sb.append("structure S").append(n).append(" {\n    l: L\n}\n");
            sb.append("list L {\n    member: S").append(n).append("\n}\n");
        } else {
            sb.append("structure S").append(n).append(" {}\n");
        }
        return sb.toString();
    }

    // A linear chain L0 -> L1 -> ... -> L(n). Optionally the last shape points back through a list.
    private static String linearChainIdl(int n, boolean recursiveEnd) {
        StringBuilder sb = new StringBuilder("$version: \"2.0\"\nnamespace chain\n\n");
        for (int i = 0; i < n; i++) {
            sb.append("structure L").append(i).append(" {\n    m: L").append(i + 1).append("\n}\n");
        }
        if (recursiveEnd) {
            sb.append("structure L").append(n).append(" {\n    l: LL\n}\n");
            sb.append("list LL {\n    member: L").append(n).append("\n}\n");
        } else {
            sb.append("structure L").append(n).append(" {}\n");
        }
        return sb.toString();
    }
}
