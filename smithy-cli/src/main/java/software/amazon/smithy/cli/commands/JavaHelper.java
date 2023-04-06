/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.cli.commands;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.utils.StringUtils;

final class JavaHelper {

    private JavaHelper() {}

    static Path getJavaHome() {
        return Paths.get(getOrThrowIfUndefinedProperty("java.home"));
    }

    static Path getJavaBinary() {
        Path javaHome = getJavaHome();
        Path bin = javaHome.resolve("bin");
        Path windowsBinary = bin.resolve("java.exe");
        Path posixBinary = bin.resolve("java");

        if (!Files.isDirectory(bin)) {
            throw new CliError("$JAVA_HOME/bin directory not found: " + bin);
        } else if (Files.exists(windowsBinary)) {
            return windowsBinary;
        } else if (Files.exists(posixBinary)) {
            return posixBinary;
        } else {
            throw new CliError("No java binary found in " + bin);
        }
    }

    private static String getOrThrowIfUndefinedProperty(String property) {
        String result = System.getProperty(property);
        if (StringUtils.isEmpty(result)) {
            throw new CliError(result + " system property is not defined");
        }
        return result;
    }
}
