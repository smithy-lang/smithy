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

package software.amazon.smithy.cli.commands;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.cli.CliUtils;
import software.amazon.smithy.model.validation.Severity;

public class ValidateCommandTest {
    @Test
    public void hasLongHelpCommand() {
        CliUtils.Result result = CliUtils.runSmithy("validate", "--help");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Validate"));
    }

    @Test
    public void hasShortHelpCommand() {
        CliUtils.Result result = CliUtils.runSmithy("validate", "-h");

        assertThat(result.code(), equalTo(0));
        assertThat(result.stdout(), containsString("Validate"));
    }

    @Test
    public void usesModelDiscoveryWithCustomValidClasspath() throws URISyntaxException {
        String dir = Paths.get(getClass().getResource("valid.jar").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("validate", "--debug", "--discover-classpath", dir);

        assertThat(result.code(), equalTo(0));
    }

    @Test
    public void usesModelDiscoveryWithCustomInvalidClasspath() throws Exception {
        String dir = Paths.get(getClass().getResource("invalid.jar").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("validate", "--debug", "--discover-classpath", dir);

        assertThat(result.code(), not(equalTo(0)));
        assertThat(result.stderr(), containsString("ERROR: 1"));
    }

    @Test
    public void failsOnUnknownTrait() throws Exception {
        String model = Paths.get(getClass().getResource("unknown-trait.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("validate", model);

        assertThat(result.code(), not(equalTo(0)));
        assertThat(result.stderr(), containsString("ERROR: 1"));
    }

    @Test
    public void allowsUnknownTrait() throws URISyntaxException {
        String model = Paths.get(getClass().getResource("unknown-trait.smithy").toURI()).toString();
        CliUtils.Result result = CliUtils.runSmithy("validate", "--allow-unknown-traits", model);

        assertThat(result.code(), equalTo(0));
    }

    @Test
    public void canSetSeverityToSuppressed() throws Exception {
        CliUtils.Result cliResult = runValidationEventsTest(Severity.SUPPRESSED);
        String result = cliResult.stdout();

        assertThat(result, containsString("EmitSuppressed"));
        assertThat(result, containsString("EmitNotes"));
        assertThat(result, containsString("EmitWarnings"));
        assertThat(result, containsString("EmitDangers"));
        assertThat(result, containsString("HttpLabelTrait"));
    }

    @Test
    public void canSetSeverityToNote() throws Exception {
        CliUtils.Result cliResult = runValidationEventsTest(Severity.NOTE);
        String result = cliResult.stdout();

        assertThat(result, not(containsString("─ EmitSuppressed")));
        assertThat(result, containsString("─ EmitNotes"));
        assertThat(result, containsString("─ EmitWarnings"));
        assertThat(result, containsString("─ EmitDangers"));
        assertThat(result, containsString("─ HttpLabelTrait"));
    }

    @Test
    public void canSetSeverityToWarning() throws Exception {
        CliUtils.Result cliResult = runValidationEventsTest(Severity.WARNING);
        String result = cliResult.stdout();

        assertThat(result, not(containsString("EmitSuppressed")));
        assertThat(result, not(containsString("EmitNotes")));
        assertThat(result, containsString("EmitWarnings"));
        assertThat(result, containsString("EmitDangers"));
        assertThat(result, containsString("HttpLabelTrait"));
    }

    @Test
    public void canSetSeverityToDanger() throws Exception {
        CliUtils.Result cliResult = runValidationEventsTest(Severity.DANGER);
        String result = cliResult.stdout();

        assertThat(result, not(containsString("EmitSuppressed")));
        assertThat(result, not(containsString("EmitNotes")));
        assertThat(result, not(containsString("EmitWarnings")));
        assertThat(result, containsString("EmitDangers"));
        assertThat(result, containsString("HttpLabelTrait"));
    }

    @Test
    public void canSetSeverityToError() throws Exception {
        CliUtils.Result cliResult = runValidationEventsTest(Severity.ERROR);
        String result = cliResult.stdout();

        assertThat(result, not(containsString("EmitSuppressed")));
        assertThat(result, not(containsString("EmitNotes")));
        assertThat(result, not(containsString("EmitWarnings")));
        assertThat(result, not(containsString("EmitDangers")));
        assertThat(result, containsString("HttpLabelTrait"));
    }

    private CliUtils.Result runValidationEventsTest(Severity severity) throws Exception {
        Path validationEventsModel = Paths.get(getClass().getResource("validation-events.smithy").toURI());
        return CliUtils.runSmithy("validate", "--debug",
                                  "--severity", severity.toString(), validationEventsModel.toString());
    }

    @Test
    public void validatesSeverity() {
        CliUtils.Result result = CliUtils.runSmithy("validate", "--severity", "FOO");

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("Invalid severity"));
    }

    @Test
    public void showAndHideAreExclusive() {
        CliUtils.Result result = CliUtils.runSmithy("validate", "--show-validators", "X", "--hide-validators", "Y");

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("--show-validators and --hide-validators are mutually exclusive"));
    }

    @Test
    public void hideAndShowAreExclusive() {
        CliUtils.Result result = CliUtils.runSmithy("validate", "--hide-validators", "Y", "--show-validators", "X");

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("--show-validators and --hide-validators are mutually exclusive"));
    }

    @Test
    public void validatesEventIdsAreNotEmpty() {
        CliUtils.Result result = CliUtils.runSmithy("validate", "--show-validators", "Foo,,Bar");

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("Invalid validation event ID"));
    }

    @Test
    public void validatesEventIdsAreNotBlank() {
        CliUtils.Result result = CliUtils.runSmithy("validate", "--show-validators", "Foo, ,Bar");

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("Invalid validation event ID"));
    }

    @Test
    public void validatesEventIdsAreNotLeadingEmpty() {
        CliUtils.Result result = CliUtils.runSmithy("validate", "--show-validators", ",Bar");

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("Invalid validation event ID"));
    }

    @Test
    public void canShowEventsById() throws Exception {
        Path validationEventsModel = Paths.get(getClass().getResource("validation-events.smithy").toURI());
        CliUtils.Result result = CliUtils.runSmithy("validate", "--debug", "--show-validators", "EmitWarnings",
                                                    validationEventsModel.toString());

        assertThat(result.code(), not(0));
        assertThat(result.stdout(), containsString("EmitWarnings"));
        assertThat(result.stdout(), not(containsString("EmitDangers")));
        assertThat(result.stdout(), not(containsString("HttpLabelTrait")));
    }

    @Test
    public void canHideEventsById() throws Exception {
        Path validationEventsModel = Paths.get(getClass().getResource("validation-events.smithy").toURI());
        CliUtils.Result result = CliUtils.runSmithy("validate", "--debug", "--hide-validators", "EmitDangers",
                                                    validationEventsModel.toString());

        assertThat(result.code(), not(0));
        assertThat(result.stdout(), containsString("EmitWarnings"));
        assertThat(result.stdout(), not(containsString("EmitDangers")));
        assertThat(result.stdout(), containsString("HttpLabelTrait"));
    }

    @Test
    public void canOutputCsv() throws Exception {
        Path validationEventsModel = Paths.get(getClass().getResource("validation-events.smithy").toURI());
        CliUtils.Result result = CliUtils.runSmithy("validate", "--format", "csv",
                                                    validationEventsModel.toString());

        assertThat(result.code(), not(0));
        assertThat(result.stdout(), containsString("suppressionReason"));
        assertThat(result.stdout(), containsString("EmitWarnings"));
        assertThat(result.stdout(), containsString("EmitDangers"));
        assertThat(result.stdout(), containsString("HttpLabelTrait"));
        assertThat(result.stdout(), not(containsString("FAILURE"))); // stderr
    }

    @Test
    public void canOutputText() throws Exception {
        Path validationEventsModel = Paths.get(getClass().getResource("validation-events.smithy").toURI());
        CliUtils.Result result = CliUtils.runSmithy("validate", "--format", "text",
                                                    validationEventsModel.toString());

        assertThat(result.code(), not(0));
        assertThat(result.stdout(), not(containsString("suppressionReason")));
        assertThat(result.stdout(), containsString("EmitWarnings"));
        assertThat(result.stdout(), containsString("EmitDangers"));
        assertThat(result.stdout(), containsString("HttpLabelTrait"));
        assertThat(result.stdout(), not(containsString("FAILURE"))); // stderr
    }

    @Test
    public void outputFormatMustBeValid() {
        CliUtils.Result result = CliUtils.runSmithy("validate", "--format", "HELLO");

        assertThat(result.code(), not(0));
        assertThat(result.stderr(), containsString("Unexpected --format: `HELLO`"));
    }
}
