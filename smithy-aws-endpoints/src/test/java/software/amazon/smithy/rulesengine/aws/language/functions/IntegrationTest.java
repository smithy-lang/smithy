package software.amazon.smithy.rulesengine.aws.language.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.Pair;

public class IntegrationTest {
    public static List<Pair<Node, String>> invalidRules() throws Exception {
        try (Stream<Path> paths = Files.list(
                Paths.get(IntegrationTest.class.getResource("invalid-rules/").toURI()))
        ) {
            return paths.map(path -> {
                try {
                    String pathContents = IoUtils.toUtf8String(new FileInputStream(path.toFile()));
                    Node content = Node.parseJsonWithComments(pathContents,
                            path.subpath(path.getNameCount() - 2, path.getNameCount())
                                    .toString().replace("\\", "/"));

                    List<String> commentLines = new ArrayList<>();
                    for (String line : pathContents.split(System.lineSeparator())) {
                        if (line.startsWith("//")) {
                            commentLines.add(line.substring(3));
                        }
                    }

                    return Pair.of(content, String.join(System.lineSeparator(), commentLines));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
        }
    }

    @ParameterizedTest
    @MethodSource("invalidRules")
    public void checkInvalidRules(Pair<Node, String> validationTestCase) {
        RuleError error = assertThrows(RuleError.class, () -> EndpointRuleSet.fromNode(validationTestCase.getLeft()));
        assertEquals(validationTestCase.getRight(), error.toString());
    }
}
