package software.amazon.smithy.jsonschema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;

public class DefaultRefStrategyTest {

    private PropertyNamingStrategy propertyNamingStrategy = PropertyNamingStrategy.createDefaultStrategy();
    private ObjectNode config = Node.objectNode();

    @Test
    public void usesDefaultPointer() {
        RefStrategy ref = RefStrategy.createDefaultStrategy(
                Model.builder().build(), Node.objectNode(), propertyNamingStrategy);
        String pointer = ref.toPointer(ShapeId.from("smithy.example#Foo"));

        assertThat(pointer, equalTo("#/definitions/Foo"));
    }

    @Test
    public void usesCustomPointerAndAppendsSlashWhenNecessary() {
        RefStrategy ref = RefStrategy.createDefaultStrategy(Model.builder().build(), Node.objectNodeBuilder()
                .withMember(JsonSchemaConstants.DEFINITION_POINTER, Node.from("#/components/schemas"))
                .build(), propertyNamingStrategy);
        String pointer = ref.toPointer(ShapeId.from("smithy.example#Foo"));

        assertThat(pointer, equalTo("#/components/schemas/Foo"));
    }

    @Test
    public void usesCustomPointerAndOmitsSlashWhenNecessary() {
        RefStrategy ref = RefStrategy.createDefaultStrategy(Model.builder().build(), Node.objectNodeBuilder()
                .withMember(JsonSchemaConstants.DEFINITION_POINTER, Node.from("#/components/schemas"))
                .build(), propertyNamingStrategy);
        String pointer = ref.toPointer(ShapeId.from("smithy.example#Foo"));

        assertThat(pointer, equalTo("#/components/schemas/Foo"));
    }

    @Test
    public void includesNamespacesWhenRequested() {
        RefStrategy ref = RefStrategy.createDefaultStrategy(Model.builder().build(), Node.objectNodeBuilder()
                .withMember(JsonSchemaConstants.KEEP_NAMESPACES, true)
                .build(), propertyNamingStrategy);
        String pointer = ref.toPointer(ShapeId.from("smithy.example#Foo"));

        assertThat(pointer, equalTo("#/definitions/SmithyExampleFoo"));
    }

    @Test
    public void stripsNonAlphanumericCharactersWhenRequested() {
        RefStrategy ref = RefStrategy.createDefaultStrategy(Model.builder().build(), Node.objectNodeBuilder()
                .withMember(JsonSchemaConstants.ALPHANUMERIC_ONLY_REFS, true)
                .build(), propertyNamingStrategy);
        String pointer = ref.toPointer(ShapeId.from("smithy.example#Foo_Bar"));

        assertThat(pointer, equalTo("#/definitions/FooBar"));
    }

    @Test
    public void addsListAndSetMembers() {
        StringShape string = StringShape.builder().id("foo.bar#String").build();
        MemberShape member = MemberShape.builder()
                .id("foo.bar#Scripts$member")
                .target("foo.bar#String")
                .build();
        ListShape list = ListShape.builder()
                .id("foo.bar#Scripts")
                .member(member)
                .build();
        Model model = Model.builder().addShapes(string, list, member).build();
        RefStrategy ref = RefStrategy.createDefaultStrategy(model, config, propertyNamingStrategy);
        String pointer = ref.toPointer(member.getId());

        assertThat(pointer, equalTo("#/definitions/Scripts/items"));
    }

    @Test
    public void addsMapMembers() {
        StringShape string = StringShape.builder().id("foo.bar#String").build();
        MemberShape key = MemberShape.builder()
                .id("foo.bar#Scripts$key")
                .target("foo.bar#String")
                .build();
        MemberShape value = MemberShape.builder()
                .id("foo.bar#Scripts$value")
                .target("foo.bar#String")
                .build();
        MapShape map = MapShape.builder()
                .id("foo.bar#Scripts")
                .key(key)
                .value(value)
                .build();
        Model model = Model.builder().addShapes(string, map, key, value).build();
        RefStrategy ref = RefStrategy.createDefaultStrategy(model, config, propertyNamingStrategy);

        assertThat(ref.toPointer(key.getId()), equalTo("#/definitions/Scripts/propertyNames"));
        assertThat(ref.toPointer(value.getId()), equalTo("#/definitions/Scripts/additionalProperties"));
    }

    @Test
    public void addsStructureMembers() {
        StringShape string = StringShape.builder().id("foo.bar#String").build();
        MemberShape member = MemberShape.builder()
                .id("foo.bar#Scripts$pages")
                .target("foo.bar#String")
                .build();
        StructureShape struct = StructureShape.builder()
                .id("foo.bar#Scripts")
                .addMember(member)
                .build();
        Model model = Model.builder().addShapes(string, struct, member).build();
        RefStrategy ref = RefStrategy.createDefaultStrategy(model, config, propertyNamingStrategy);

        assertThat(ref.toPointer(struct.getId()), equalTo("#/definitions/Scripts"));
        assertThat(ref.toPointer(member.getId()), equalTo("#/definitions/Scripts/properties/pages"));
    }

    @Test
    public void usesRefForStructureMembers() {
        StructureShape baz = StructureShape.builder()
                .id("foo.bar#Baz")
                .addMember("bam", ShapeId.from("foo.bar#Bam"))
                .build();
        StructureShape bam = StructureShape.builder()
                .id("foo.bar#Bam")
                .build();
        Model model = Model.builder().addShapes(baz, bam).build();
        RefStrategy ref = RefStrategy.createDefaultStrategy(model, config, propertyNamingStrategy);

        assertThat(ref.toPointer(baz.getMember("bam").get().getId()), equalTo("#/definitions/Bam"));
    }
}
