/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.cli.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import software.amazon.smithy.cli.ArgumentReceiver;
import software.amazon.smithy.cli.CliError;
import software.amazon.smithy.cli.HelpPrinter;
import software.amazon.smithy.cli.StandardOptions;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Add validation specific options.
 */
final class ValidatorOptions implements ArgumentReceiver {

    static final String SEVERITY = "--severity";
    static final String SHOW_VALIDATORS = "--show-validators";
    static final String HIDE_VALIDATORS = "--hide-validators";

    private Severity severity;
    private List<String> showValidators = Collections.emptyList();
    private List<String> hideValidators = Collections.emptyList();

    @Override
    public void registerHelp(HelpPrinter printer) {
        printer.param(SEVERITY, null, "SEVERITY", "Set the minimum reported validation severity (one of NOTE, "
                                                  + "WARNING [default setting], DANGER, ERROR).");
        printer.param(SHOW_VALIDATORS, null, "VALIDATORS", "Comma-separated list of hierarchical validation event "
                                                           + "IDs to show in the output of the command, "
                                                           + "hiding the rest.");
        printer.param(HIDE_VALIDATORS, null, "VALIDATORS", "Comma-separated list of hierarchical validation event "
                                                           + "IDs to hide in the output of the command, "
                                                           + "showing the rest.");
    }

    @Override
    public Consumer<String> testParameter(String name) {
        switch (name) {
            case SEVERITY:
                return value -> {
                    severity(Severity.fromString(value).orElseThrow(() -> {
                        return new CliError("Invalid severity level: " + value);
                    }));
                };
            case SHOW_VALIDATORS:
                return value -> {
                    if (!hideValidators.isEmpty()) {
                        throw new CliError(SHOW_VALIDATORS + " and " + HIDE_VALIDATORS + " are mutually exclusive");
                    }
                    showValidators(parseIds(value));
                };
            case HIDE_VALIDATORS:
                return value -> {
                    if (!showValidators.isEmpty()) {
                        throw new CliError(SHOW_VALIDATORS + " and " + HIDE_VALIDATORS + " are mutually exclusive");
                    }
                    hideValidators(parseIds(value));
                };
            default:
                return null;
        }
    }

    private List<String> parseIds(String value) {
        List<String> result = new ArrayList<>();
        for (String id : value.split("\\s*,\\s*")) {
            id = id.trim();
            if (id.isEmpty()) {
                throw new CliError("Invalid validation event ID");
            }
            result.add(id);
        }
        return result;
    }

    /**
     * Get the set severity, which may be null.
     *
     * @return The nullable severity.
     */
    Severity severity() {
        return severity;
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
     * Set and get the severity level, taking into account standard options that affect the default.
     *
     * @param standardOptions  Standard options to query if no severity is explicitly set.
     * @return Returns the resolved severity option.
     */
    Severity detectAndGetSeverity(StandardOptions standardOptions) {
        if (severity == null) {
            if (standardOptions.quiet()) {
                severity = Severity.DANGER;
            } else {
                severity = Severity.WARNING;
            }
        }
        return severity;
    }

    /**
     * Set the list of validators to show, but hide everything else.
     *
     * @param validators Validators to show.
     */
    void showValidators(List<String> validators) {
        this.showValidators = validators;
    }

    /**
     * Get the list of validators to show.
     *
     * @return Validator event IDs.
     */
    List<String> showValidators() {
        return showValidators;
    }

    /**
     * Get the list of validators to hide.
     *
     * @return Validator event IDs.
     */
    List<String> hideValidators(String csv) {
        return hideValidators;
    }

    /**
     * Set the list of validators to hide.
     *
     * @param validators Validators to hide.
     */
    void hideValidators(List<String> validators) {
        this.hideValidators = validators;
    }

    /**
     * Check if the given validation event matches the show/hide settings.
     *
     * <p>A severity must be set before calling this method.
     *
     * @param event Event to check.
     * @return Return true if the event can be seen.
     */
    boolean isVisible(ValidationEvent event) {
        if (event.getSeverity().ordinal() < severity.ordinal()) {
            return false;
        }

        if (!showValidators.isEmpty()) {
            for (String show : showValidators) {
                if (event.containsId(show)) {
                    return true;
                }
            }
            return false;
        }

        if (!hideValidators.isEmpty()) {
            for (String hide : hideValidators) {
                if (event.containsId(hide)) {
                    return false;
                }
            }
        }

        return true;
    }
}
