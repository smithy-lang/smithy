/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.util.function.Consumer;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.HelpPrinter;

/** Command-line options accepted by {@code smithy call}. */
final class CallOptions implements ArgumentReceiver {
    String input;
    String inputPayload;
    boolean pretty;
    boolean json;
    boolean plan;
    boolean bddTrace;
    String outputPayload;
    String awsProfile;
    String awsRegion;
    String protocol;
    String wire;
    String query;
    String continueToken;
    String url;

    @Override
    public boolean testOption(String name) {
        switch (name) {
            case "--pretty":
            case "-p":
                pretty = true;
                return true;
            case "--json":
                json = true;
                return true;
            case "--plan":
                plan = true;
                return true;
            case "--bdd-trace":
                bddTrace = true;
                return true;
            case "-w":
                wire = "headers";
                return true;
            default:
                return false;
        }
    }

    @Override
    public Consumer<String> testParameter(String name) {
        switch (name) {
            case "-i":
            case "--input":
                return value -> input = value;
            case "--input-payload":
                return value -> inputPayload = value;
            case "-o":
            case "--output-payload":
                return value -> outputPayload = value;
            case "--wire":
                return value -> wire = value;
            case "-q":
            case "--query":
                return value -> query = value;
            case "--continue":
                return value -> continueToken = value;
            case "--aws-profile":
            case "--profile":
                return value -> awsProfile = value;
            case "--aws-region":
                return value -> awsRegion = value;
            case "--protocol":
                return value -> protocol = value;
            case "--url":
                return value -> url = value;
            default:
                return null;
        }
    }

    @Override
    public void registerHelp(HelpPrinter printer) {
        printer.positional("<SERVICE>", "A registered service name (register one with `smithy register`).");
        printer.positional("<OPERATION>", "Operation to call. Omit for service-level --help.");
        printer.param("--input",
                "-i",
                "INPUT",
                "Operation input as JSON: a literal '{...}', @file.json to read a file, or - for stdin.");
        printer.param("--input-payload",
                null,
                "FILE",
                "File to stream as the operation's streaming payload member (e.g. S3 PutObject Body). "
                        + "Use - for stdin. Combine with --input for the non-payload fields.");
        printer.param("--continue",
                null,
                "TOKEN",
                "Fetch the next page using the opaque @continue token from a previous call's output. "
                        + "Carries the original input; cannot be combined with --input.");
        printer.option("--pretty", "-p", "Pretty-print JSON output.");
        printer.param("--query",
                "-q",
                "JMESPATH",
                "Filter JSON output with JMESPath (e.g. 'MetricAlarms[].AlarmName').");
        printer.option("--json", null, "Emit --help for a service/operation as JSON instead of text.");
        printer.option("--plan", null, "Build and sign the request but do not send it; print what would be sent.");
        printer.option("--bdd-trace",
                null,
                "With --plan, explain the endpoint-rule conditions and matched result.");
        printer.param("--output-payload",
                "-o",
                "FILE",
                "Write a streaming response payload (e.g. S3 GetObject Body) to this file.");
        printer.param("--wire",
                "-w",
                "MODE",
                "Include a potentially sensitive @http block. 'headers' shows messages and 'full' includes bodies. "
                        + "Authorization is redacted, but other headers and payload fields may contain secrets; "
                        + "treat it like a packet capture. Binary bodies are base64 and truncated past 4MB. "
                        + "-w is shorthand for --wire headers.");
        printer.param("--aws-profile", null, "PROFILE", "AWS profile for credentials and default region.");
        printer.param("--aws-region", null, "REGION", "AWS region (overrides the registered/profile region).");
        printer.param("--url",
                null,
                "URL",
                "Static endpoint URL, forcing the endpoint (overriding the registered URL and rules).");
        printer.param("--protocol",
                null,
                "PROTOCOL",
                "Force the protocol: " + CallProtocol.SUPPORTED + ".");
    }
}
