package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class ShapeTypeTest {
    @Test
    public void createsBuilders() {
        Shape result = ShapeType.STRING.createBuilderForType().id("example#Foo").build();

        assertThat(result.getType(), Matchers.is(ShapeType.STRING));
        assertThat(result, Matchers.instanceOf(StringShape.class));
    }

    @Test
    public void hasCategory() {
        assertThat(ShapeType.STRING.getCategory(), Matchers.is(ShapeType.Category.SIMPLE));
        assertThat(ShapeType.LIST.getCategory(), Matchers.is(ShapeType.Category.AGGREGATE));
        assertThat(ShapeType.SERVICE.getCategory(), Matchers.is(ShapeType.Category.SERVICE));
    }
}
