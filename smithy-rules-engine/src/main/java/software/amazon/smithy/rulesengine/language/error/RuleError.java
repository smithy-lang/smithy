/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.error;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An exception that can be thrown when a rule-set is invalid.
 *
 * <p>Used for providing meaningful contextual information when
 * an invalid rule-set is encountered.
 */
@SmithyUnstableApi
public final class RuleError extends RuntimeException {
    private final List<Pair<String, SourceLocation>> contexts = new ArrayList<>();
    private final SourceException root;

    /**
     * Constructs a new RuleError from the source exception.
     * @param root the exception this rule error is based on.
     */
    public RuleError(SourceException root) {
        super(root);
        this.root = root;
    }

    /**
     * Evaluates the runnable and creates a RuleError with the provided context if there's an error.
     *
     * @param message a message representing the context for this runnable statement's evaluation.
     * @param runnable a runnable to evaluate a statement in the current context.
     */
    public static void context(String message, Runnable runnable) {
        context(message, SourceLocation.none(), runnable);
    }

    /**
     * Evaluates the runnable and creates a RuleError with the provided context if there's an error.
     *
     * @param <T> the type of the value returned by the runnable.
     * @param message a message representing the context for this runnable statement's evaluation.
     * @param runnable a runnable to evaluate a statement in the current context.
     * @return the value returned by the runnable.
     * @throws RuleError when the rule being evaluated in the context fails.
     */
    public static <T> T context(String message, Evaluator<T> runnable) throws RuleError {
        return context(message, SourceLocation.none(), runnable);
    }

    /**
     * Evaluates the runnable and creates a RuleError with the provided context if there's an error.
     *
     * @param message a message representing the context for this runnable statement's evaluation.
     * @param sourceLocation the source location for this runnable statement's evaluation.
     * @param runnable a runnable to evaluate a statement in the current context.
     */
    public static void context(String message, FromSourceLocation sourceLocation, Runnable runnable) {
        context(message, sourceLocation, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Evaluates the runnable and creates a RuleError with the provided context if there's an error.
     *
     * @param <T> the type of the value returned by the runnable.
     * @param message a message representing the context for this runnable statement's evaluation.
     * @param sourceLocation the source location for this runnable statement's evaluation.
     * @param runnable a runnable to evaluate a statement in the current context.
     * @return the value returned by the runnable.
     * @throws RuleError when the rule being evaluated in the context fails.
     */
    public static <T> T context(
            String message,
            FromSourceLocation sourceLocation,
            Evaluator<T> runnable
    ) throws RuleError {
        try {
            return runnable.call();
        } catch (SourceException ex) {
            throw new RuleError(ex).withContext(message, sourceLocation.getSourceLocation());
        } catch (RuleError ex) {
            throw ex.withContext(message, sourceLocation.getSourceLocation());
        } catch (Exception | Error ex) {
            if (ex.getMessage() == null) {
                throw new RuntimeException(ex);
            }
            throw new RuleError(new SourceException(ex.getMessage(), sourceLocation.getSourceLocation(), ex))
                    .withContext(message, sourceLocation.getSourceLocation());
        }
    }

    /**
     * Sets a piece of context on this error.
     *
     * @param context the context to add to the error.
     * @param location the source location of the context being added.
     * @return returns this error with the added context.
     */
    public RuleError withContext(String context, SourceLocation location) {
        this.contexts.add(Pair.of(context, location));
        return this;
    }

    @Override
    public String toString() {
        StringBuilder message = new StringBuilder();
        SourceLocation lastLoc = SourceLocation.none();
        for (int i = contexts.size() - 1; i >= 0; i--) {
            Pair<String, SourceLocation> context = contexts.get(i);
            message.append(context.left);
            message.append(System.lineSeparator());
            if (context.right != SourceLocation.NONE && context.right != lastLoc) {
                message.append("  at ")
                        .append(context.right.getSourceLocation().getFilename())
                        .append(":")
                        .append(context.right.getSourceLocation().getLine())
                        .append(System.lineSeparator());
                lastLoc = context.right;
            }
        }

        message.append(root.getMessageWithoutLocation());
        if (root.getSourceLocation() != SourceLocation.none() && root.getSourceLocation() != lastLoc) {
            message.append(System.lineSeparator()).append("  at ")
                    .append(root.getSourceLocation().getFilename())
                    .append(":").append(root.getSourceLocation().getLine());
        }
        return message.toString();
    }

    @FunctionalInterface
    public interface Evaluator<T> {
        T call() throws InnerParseError;
    }
}
