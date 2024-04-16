/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.diff.testrunner.SmithyDiffTestCase;
import software.amazon.smithy.diff.testrunner.SmithyDiffTestSuite;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.Pair;

public class DiffTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("source")
    public void testRunner(String filename, Callable<SmithyDiffTestCase.Result> callable) throws Exception {
        callable.call();
    }

    public static Stream<?> source() {
        return SmithyDiffTestSuite.defaultParameterizedTestSource(DiffTest.class);
    }
}
