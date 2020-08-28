package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

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

        assertThat(ordered, contains(
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

        assertThat(recursive, contains(
                "smithy.example#Recursive$b",
                "smithy.example#Recursive$a",
                "smithy.example#RecursiveList",
                "smithy.example#RecursiveList$member",
                "smithy.example#Recursive"));
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
}
