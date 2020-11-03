package software.amazon.smithy.jmespath;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * This test loads invalid and valid files, to ensure that they
 * are either able to be parsed or not able to be parsed.
 */
public class RunnerTest {
    @Test
    public void validTests() {
        for (String line : readFile(getClass().getResourceAsStream("valid"))) {
            try {
                JmespathExpression expression = JmespathExpression.parse(line);
                for (ExpressionProblem problem : expression.lint().getProblems()) {
                    if (problem.severity == ExpressionProblem.Severity.ERROR) {
                        Assertions.fail("Did not expect an ERROR for line: " + line + "\n" + problem);
                    }
                }
            } catch (JmespathException e) {
                Assertions.fail("Error loading line:\n" + line + "\n" + e.getMessage(), e);
            }
        }
    }

    @Test
    public void invalidTests() {
        for (String line : readFile(getClass().getResourceAsStream("invalid"))) {
            try {
                JmespathExpression.parse(line);
                Assertions.fail("Expected line to fail: " + line);
            } catch (JmespathException e) {
                // pass
            }
        }
    }

    private List<String> readFile(InputStream stream) {
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .lines()
                .map(line -> {
                    if (line.endsWith(",")) {
                        return line.substring(0, line.length() - 1);
                    } else {
                        return line;
                    }
                })
                .map(line -> Lexer.tokenize(line).next().value.expectStringValue())
                .collect(Collectors.toList());
    }
}
