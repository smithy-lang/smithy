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
import software.amazon.smithy.rulesengine.language.lang.Identifier;
import software.amazon.smithy.rulesengine.language.lang.expr.Expr;
import software.amazon.smithy.rulesengine.language.lang.expr.Template;
import software.amazon.smithy.rulesengine.language.lang.fn.GetAttr;

class TemplateTest {
    @Test
    void validateTemplate() {
        checkTemplateParts("asdf", "asdf");
        checkTemplateParts("a{B}c", "a", "{dyn B}", "c");
        checkTemplateParts("a{{b}}c", "a{b}c");
        checkTemplateParts("https://{Bucket#arn-region}.{Region}.amazonaws.com", "https://", "{dyn Bucket#arn-region}", ".", "{dyn Region}", ".amazonaws.com");
        checkTemplateParts("https://{Partition#meta.dnsSuffix}", "https://", "{dyn Partition#meta.dnsSuffix}");
        checkTemplateParts("https://{ {\"ref\": \"Foo\"} }.com", "https://", "{dyn  {\"ref\": \"Foo\"} }", ".com");
        checkTemplateParts("{a}b", "{dyn a}", "b");
    }

    private void checkTemplateParts(String templateInput, String... parts) {
        Template template = Template.fromString(templateInput);
        assertEquals(
                Arrays.stream(parts).collect(Collectors.toList()),
                template.getParts().stream().map(Object::toString).collect(Collectors.toList()));
    }

    @Test
    void validateShortformParsing() {
        assertEquals(Expr.parseShortform("a", SourceLocation.none()), Expr.ref(Identifier.of("a"), SourceLocation.none()));
        assertEquals(Expr.parseShortform("a#b", SourceLocation.none()), GetAttr
                .builder(SourceLocation.none())
                .target(Expr.ref(Identifier.of("a"), SourceLocation.none()))
                .path("b")
                .build());
        assertEquals(Expr.parseShortform("a#b.c", SourceLocation.none()), GetAttr
                .builder(SourceLocation.none())
                .target(Expr.ref(Identifier.of("a"), SourceLocation.none()))
                .path("b.c")
                .build());
    }

    @Test
    void invalidTemplates() {
        Expr.parseShortform("a#", SourceLocation.none());
    }

}
