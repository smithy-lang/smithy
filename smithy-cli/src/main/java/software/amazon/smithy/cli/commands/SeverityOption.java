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

import java.util.function.Consumer;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.model.validation.Severity;

/**
 * Add the --severity option.
 */
final class SeverityOption implements ArgumentReceiver {

    static final String SEVERITY = "--severity";

    private Severity severity;

    @Override
    public void registerHelp(HelpPrinter printer) {
        printer.param(SEVERITY, null, "SEVERITY", "Set the minimum reported validation severity (one of NOTE, "
                                                  + "WARNING [default setting], DANGER, ERROR).");
    }

    @Override
    public Consumer<String> testParameter(String name) {
        if (SEVERITY.equals(name)) {
            return value -> {
                severity(Severity.fromString(value).orElseThrow(() -> {
                    return new CliError("Invalid severity level: " + value);
                }));
            };
        }
        return null;
    }

    /**
     * Set the severity.
     *
     * @param severity Severity to set.
     */
    void severity(Severity severity) {
        this.severity = severity;
    }

    /**
     * Get the severity level, taking into account standard options that affect the default.
     *
     * @param options Standard options to query if no severity is explicitly set.
     * @return Returns the resolved severity option.
     */
    Severity severity(StandardOptions options) {
        if (severity != null) {
            return severity;
        } else if (options.quiet()) {
            return Severity.DANGER;
        } else {
            return Severity.WARNING;
        }
    }
}
