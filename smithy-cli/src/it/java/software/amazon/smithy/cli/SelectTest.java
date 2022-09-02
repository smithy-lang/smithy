package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;

public class SelectTest {
    @Test
    public void selectsShapeIds() {
        List<String> args = Arrays.asList("select", "--selector", "string [id|namespace=smithy.example]");
        IntegUtils.run("simple-config-sources", args, result -> {
            assertThat(result.getExitCode(), equalTo(0));
            assertThat(result.getOutput().trim(), equalTo("smithy.example#MyString"));
        });
    }

    @Test
    public void selectsVariables() {
        List<String> args = Arrays.asList("select", "--vars", "--selector", "list $list(*) > member > string");
        IntegUtils.run("simple-config-sources", args, result -> {
            assertThat(result.getExitCode(), equalTo(0));
            String content = result.getOutput().trim();
            // Ensure it's valid JSON
            Node.parse(content);
            assertThat(content, containsString("\"shape\": \"smithy.api#String\""));
        });
    }
}
