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

package software.amazon.smithy.cli;

public final class CliUtils {

    // Run Smithy with colors disabled by default.
    public static Result runSmithy(String... args) {
        try {
            EnvironmentVariable.NO_COLOR.set("true");
            return run(SmithyCli.create().createCli(), args);
        } finally {
            EnvironmentVariable.NO_COLOR.clear();
        }
    }

    public static Result runSmithyWithAutoColors(String... args) {
        return run(SmithyCli.create().createCli(), args);
    }

    private static Result run(Cli cli, String... args) {
        CliPrinter stdout = new BufferPrinter();
        CliPrinter stderr = new BufferPrinter();
        cli.stdout(stdout);
        cli.stderr(stderr);
        int result;

        try {
            result = cli.run(args);
        } catch (CliError e) {
            // ignore the error since everything we need was captured via stderr.
            result = e.code;
        }

        return new Result(result, stdout.toString(), stderr.toString());
    }

    public static final class Result {
        private final int code;
        private final String stdout;
        private final String stderr;

        public Result(int code, String stdout, String stderr) {
            this.code = code;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public int code() {
            return code;
        }

        public String stdout() {
            return stdout;
        }

        public String stderr() {
            return stderr;
        }
    }
}
