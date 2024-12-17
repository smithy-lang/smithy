/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
