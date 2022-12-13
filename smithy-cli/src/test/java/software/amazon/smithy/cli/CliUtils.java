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

    public static Result runSmithy(String... args) {
        return run(SmithyCli.create().createCli(), args);
    }

    public static Result run(Cli cli, String... args) {
        CapturedPrinter stdout = new CapturedPrinter();
        CapturedPrinter stderr = new CapturedPrinter();
        cli.stdout(stdout);
        cli.stderr(stderr);
        int result;

        try {
            result = cli.run(args);
        } catch (CliError e) {
            // ignore the error since everything we need was captured via stderr.
            result = e.code;
        }

        return new Result(result, stdout.result.toString(), stderr.result.toString());
    }

    private static class CapturedPrinter implements CliPrinter {
        private final StringBuilder result = new StringBuilder();

        @Override
        public synchronized void println(String text) {
            result.append(text).append(System.lineSeparator());
        }
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
