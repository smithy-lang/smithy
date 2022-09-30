/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.rulesengine.language.value;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expr.Template;
import software.amazon.smithy.rulesengine.language.syntax.fn.GetAttr;

class TemplateTest {
    @Test
    void validateTemplate() {
        checkTemplateParts("asdf", "asdf");
        checkTemplateParts("a{B}c", "a", "{B}", "c");
        checkTemplateParts("a{{b}}c", "a{b}c");
        checkTemplateParts("https://{Bucket#arn-region}.{Region}.amazonaws.com", "https://", "{Bucket#arn-region}", ".", "{Region}", ".amazonaws.com");
        checkTemplateParts("https://{Partition#meta.dnsSuffix}", "https://", "{Partition#meta.dnsSuffix}");
        checkTemplateParts("https://{ {\"ref\": \"Foo\"} }.com", "https://", "{ {\"ref\": \"Foo\"} }", ".com");
        checkTemplateParts("{a}b", "{a}", "b");
    }

    private void checkTemplateParts(String templateInput, String... parts) {
        Template template = Template.fromString(templateInput);
        assertEquals(
                Arrays.stream(parts).collect(Collectors.toList()),
                template.getParts().stream().map(Object::toString).collect(Collectors.toList()));
    }

    @Test
    void validateShortformParsing() {
        assertEquals(Expression.parseShortform("a", SourceLocation.none()), Expression.reference(Identifier.of("a"), SourceLocation.none()));
        assertEquals(Expression.parseShortform("a#b", SourceLocation.none()), GetAttr
                .builder()
                .target(Expression.reference(Identifier.of("a"), SourceLocation.none()))
                .path("b")
                .build());
        assertEquals(Expression.parseShortform("a#b.c", SourceLocation.none()), GetAttr
                .builder()
                .target(Expression.reference(Identifier.of("a"), SourceLocation.none()))
                .path("b.c")
                .build());
    }

    @Test
    void invalidTemplates() {
        Expression.parseShortform("a#", SourceLocation.none());
    }

}
