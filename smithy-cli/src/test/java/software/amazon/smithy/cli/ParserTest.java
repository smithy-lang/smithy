/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.ListUtils;

public class ParserTest {
    @Test
    public void argumentsRequireLongShortOrBoth() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Parser.Argument(null, null, Parser.Arity.ONE, "Help");
        });
    }

    @Test
    public void longNameMustStartWithHyphen() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Parser.Argument("foo", null, Parser.Arity.ONE, "Help");
        });
    }

    @Test
    public void shortNameMustStartWithHyphen() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Parser.Argument("--foo", "f", Parser.Arity.ONE, "Help");
        });
    }

    @Test
    public void getsCanonicalNames() {
        Parser.Argument arg1 = new Parser.Argument("--foo", "-f", Parser.Arity.ONE, "Help");
        Parser.Argument arg2 = new Parser.Argument(null, "-f", Parser.Arity.ONE, "Help");

        assertThat(arg1.getCanonicalName(), equalTo("--foo"));
        assertThat(arg2.getCanonicalName(), equalTo("-f"));
    }

    @Test
    public void parsesArguments() {
        Parser parser = Parser.builder()
                .option("--force", "Forces something")
                .option("--dry-run", "-d", "Do not actually do it")
                .parameter("--config", "-c", "Config file")
                .parameter("--config", "Config file")
                .repeatedParameter("--file", "File to do something with")
                .positional("<POSITIONAL>", "Positional arguments")
                .build();
        Arguments arguments = parser.parse(new String[]{
                "--force", "-d", "--config", "/foo", "--file", "1", "--file", "2", "a", "b"});

        Assertions.assertTrue(arguments.has("--force"));
        Assertions.assertTrue(arguments.has("--dry-run"));
        assertThat(arguments.parameter("--config"), equalTo("/foo"));
        assertThat(arguments.parameter("--file"), equalTo("1"));
        assertThat(arguments.repeatedParameter("--file"), equalTo(ListUtils.of("1", "2")));
        assertThat(arguments.positionalArguments(), equalTo(ListUtils.of("a", "b")));
    }

    @Test
    public void parsesArgumentsWithOffset() {
        Parser parser = Parser.builder().option("--force", "Forces something").build();
        Arguments arguments = parser.parse(new String[]{"skip", "--force"}, 1);

        Assertions.assertTrue(arguments.has("--force"));
    }

    @Test
    public void failsWhenValueProvidedForOption() {
        Assertions.assertThrows(CliError.class, () -> {
            Parser parser = Parser.builder().option("--force", "Forces something").build();
            parser.parse(new String[]{"--force", "foo"});
        });
    }

    @Test
    public void failsWhenMultipleValuesProvidedForOneArity() {
        Assertions.assertThrows(CliError.class, () -> {
            Parser parser = Parser.builder().parameter("--hi", "Forces something").build();
            parser.parse(new String[] {"--hi", "hi", "--hi", "there"});
        });
    }

    @Test
    public void failsWhenMultipleValuesProvidedForOption() {
        Assertions.assertThrows(CliError.class, () -> {
            Parser parser = Parser.builder().option("--force", "Forces something").build();
            parser.parse(new String[] {"--force", "--force"});
        });
    }

    @Test
    public void consumesPositionalAfterDoubleHyphen() {
        Parser parser = Parser.builder().parameter("--hi", "Hi").positional("<ARGS>", "Args").build();
        Arguments arguments = parser.parse(new String[]{"--hi", "hi", "--", "--hi", "there"});

        assertThat(arguments.parameter("--hi"), equalTo("hi"));
        assertThat(arguments.positionalArguments(), equalTo(ListUtils.of("--hi", "there")));
    }

    @Test
    public void failsWhenMissingArgument() {
        Assertions.assertThrows(CliError.class, () -> {
            Parser parser = Parser.builder().parameter("--file", "Forces something").build();
            parser.parse(new String[] {"--file"});
        });
    }

    @Test
    public void failsWithUnknownOption() {
        Assertions.assertThrows(CliError.class, () -> {
            Parser parser = Parser.builder().parameter("--file", "Forces something").build();
            parser.parse(new String[] {"--invalid"});
        });
    }
}
