package software.amazon.smithy.model.graph;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;

public class ModelIndexTest {
    @Test
    public void cursors() {
        ModelIndex index = ModelIndex.builder()
                .createShape(ShapeId.from("foo#Str"), ShapeType.STRING)
                        .putTrait(new SensitiveTrait())
                        .create()
                .createShape(ShapeId.from("foo#Map"), ShapeType.MAP)
                        .putMember("key", ShapeId.from("foo#Str"))
                        .putMember("value", ShapeId.from("foo#Str"))
                        .create()
                // Add traits to members.
                .putTrait(ShapeId.from("foo#Map$key"), new DocumentationTrait("Hi"))
                .createShape(ShapeId.from("foo#Mix"), ShapeType.STRUCTURE)
                        .putMember("hi", ShapeId.from("foo#Str"))
                        .putTrait(MixinTrait.builder().build())
                        .create()
                .putTrait(ShapeId.from("foo#Mix$hi"), new DocumentationTrait("hi")) // does it mixin??
                .createShape(ShapeId.from("foo#Bar"), ShapeType.STRUCTURE)
                        .putMember("bam", ShapeId.from("foo#Str"))
                        //.putMember("hi", ShapeId.from("foo#Str"))
                        .addMixin(ShapeId.from("foo#Mix"))
                        .create()
                .putTrait(ShapeId.from("foo#Bar$hi"), new SensitiveTrait()) // does it mixin??
                .createService(ShapeId.from("foo#Service"))
                        .version("2008")
                        .create()
                .createOperation(ShapeId.from("foo#OperationA"))
                        .input(ShapeId.from("foo#Bar"))
                        .output(ShapeId.from("foo#Bar"))
                        .addError(ShapeId.from("foo#E1"))
                        .putTrait(MixinTrait.builder().build())
                        .create()
                .createOperation(ShapeId.from("foo#OperationB"))
                        .addMixin(ShapeId.from("foo#OperationA"))
                        .addError(ShapeId.from("foo#E2"))
                        .create()
                .build();

        StructureCursor cursor = index.getStructure(ShapeId.from("foo#Bar"));
        System.out.println(cursor.toShapeId());
        System.out.println(cursor.getSourceLocation());
        System.out.println(cursor.getTraits());
        System.out.println(cursor.getMembers());
        System.out.println(cursor.getMembers());

        MemberCursor member = cursor.getMembers().get("bam");
        System.out.println(member.getMemberName());
        System.out.println(member.getTarget().getTraits().get(SensitiveTrait.ID));

        System.out.println(member.getReverseEdges());

        System.out.println(index.getShapesWithTrait(SensitiveTrait.ID));
        System.out.println(index.getShapesWithTrait(MixinTrait.ID));

        System.out.println("---");

        ModelIndex updated = index.toBuilder().removeTrait(ShapeId.from("foo#Baz"), SensitiveTrait.ID).build();
        System.out.println(updated.getShapesWithTrait(SensitiveTrait.ID));
        System.out.println(index.getShapesWithTrait(MixinTrait.ID));

        System.out.println("---");
        OperationCursor op = index.getOperation(ShapeId.from("foo#OperationB"));
        System.out.println(op.getInput().toShapeId());
        System.out.println(op.getOutput().toShapeId());
        System.out.println(op.getErrorIds());
        System.out.println(op.getEdges());

        System.out.println("---");
        MemberCursor m = cursor.getMembers().get("hi");
        System.out.println(m.getTraits()); // should show docs and sensitive due to merging.
        System.out.println(m.getEdges());  // Should not contain mixin edges since those aren't real.
    }
}
