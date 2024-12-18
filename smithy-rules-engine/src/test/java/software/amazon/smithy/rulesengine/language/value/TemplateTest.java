/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.value;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.GetAttr;

public class TemplateTest {
    @Test
    public void validateTemplate() {
        checkTemplateParts("asdf", "asdf");
        checkTemplateParts("a{B}c", "a", "{B}", "c");
        checkTemplateParts("a{{b}}c", "a{b}c");
        checkTemplateParts("https://{Bucket#arn-region}.{Region}.amazonaws.com",
                "https://",
                "{Bucket#arn-region}",
                ".",
                "{Region}",
                ".amazonaws.com");
        checkTemplateParts("https://{Partition#meta.dnsSuffix}", "https://", "{Partition#meta.dnsSuffix}");
        checkTemplateParts("https://{ {\"ref\": \"Foo\"} }.com", "https://", "{ {\"ref\": \"Foo\"} }", ".com");
        checkTemplateParts("{a}b", "{a}", "b");
    }

    private void checkTemplateParts(String templateInput, String... parts) {
        Template template = Template.fromString(templateInput);
        List<String> templateParts = new ArrayList<>();
        for (Template.Part part : template.getParts()) {
            templateParts.add(part.toString());
        }
        assertEquals(Arrays.asList(parts), templateParts);
    }

    @Test
    public void validateShortformParsing() {
        assertEquals(Expression.parseShortform("a", SourceLocation.none()),
                Expression.getReference(Identifier.of("a"), SourceLocation.none()));
        assertEquals(Expression.parseShortform("a#b", SourceLocation.none()),
                GetAttr.ofExpressions(
                        Expression.getReference(Identifier.of("a"), SourceLocation.none()),
                        "b"));
        assertEquals(Expression.parseShortform("a#b.c", SourceLocation.none()),
                GetAttr.ofExpressions(
                        Expression.getReference(Identifier.of("a"), SourceLocation.none()),
                        "b.c"));
    }

    @Test
    public void invalidTemplates() {
        Expression.parseShortform("a#", SourceLocation.none());
    }
}
