/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.upgrade;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.utils.IoUtils;

public class TestUpgrader {

    @ParameterizedTest(name = "{1}")
    @MethodSource("source")
    public void testUpgrade(Path initialPath, String name) {
        Path parentDir = initialPath.getParent();
        String expectedFileName = initialPath.getFileName().toString().replace(".v1.smithy", ".v2.smithy");
        Path expectedPath = parentDir.resolve(expectedFileName);

        Model model = Model.assembler().addImport(initialPath).assemble().unwrap();
        String actual = Upgrader.upgradeFile(model, initialPath);
        String expected = IoUtils.readUtf8File(expectedPath);
        assertThat(actual, equalTo(expected));
    }

    public static Stream<Arguments> source() throws Exception {
        Path start = Paths.get(TestUpgrader.class.getResource("cases").toURI());
        return Files.walk(start)
                .filter(path -> path.getFileName().toString().endsWith(".v1.smithy"))
                .map(path -> Arguments.of(path, path.getFileName().toString().replace(".v1.smithy", "")));
    }
}
