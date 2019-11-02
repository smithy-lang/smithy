package software.amazon.smithy.codegen.freemarker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.MapUtils;

public class FreeMarkerEngineTest {
    @Test
    public void rendersTemplatesWithObjectWrapper() {
        FreeMarkerEngine engine = FreeMarkerEngine.builder()
                .classLoader(getClass())
                .build();
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "Michael");
        properties.put("empty", Optional.empty());
        properties.put("present", Optional.of("Hello"));
        properties.put("object", Node.objectNode().withMember("foo", Node.from("bar")));
        properties.put("array", Node.arrayNode().withValue(Node.from("element")));
        properties.put("number", Node.from(10));
        properties.put("boolean", Node.from(true));
        properties.put("stream", Stream.of("a", "b", "c"));
        String result = engine.render("objectWrapper.ftl", properties);

        assertThat(result, containsString("Hello, Michael\n:Hello\nbar\nelement\n10\ntrue\na,b,c"));
    }

    @Test
    public void rendersTemplatesWithUtils() {
        FreeMarkerEngine engine = FreeMarkerEngine.builder()
                .classLoader(getClass().getClassLoader())
                .putDefaultProperty("snake", "snake_man")
                .build();
        String result = engine.render("software/amazon/smithy/codegen/freemarker/utils.ftl", MapUtils.of());

        assertThat(result, containsString("snakeMan - SnakeMan"));
    }
}
