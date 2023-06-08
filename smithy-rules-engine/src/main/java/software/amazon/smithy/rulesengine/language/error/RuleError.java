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

package software.amazon.smithy.rulesengine.language.error;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An exception that can be thrown when rule-set is invalid. Used for providing meaningful contextual
 * information when an invalid rule-set is encountered.
 */
@SmithyUnstableApi
public final class RuleError extends RuntimeException {
    private final List<Pair<String, SourceLocation>> contexts = new ArrayList<>();
    private final SourceException root;

    public RuleError(SourceException root) {
        super(root);
        this.root = root;
    }

    public static <T> T context(String message, Runnable f) {
        return RuleError.context(message, SourceLocation.none(), () -> {
            f.run();
            return null;
        });
    }

    public static <T> T context(String message, Evaluator<T> f) throws RuleError {
        return RuleError.context(message, SourceLocation.none(), f);
    }

    public static void context(String message, FromSourceLocation sourceLocation, Runnable f) {
        context(message, sourceLocation, () -> {
            f.run();
            return null;
        });
    }

    public static <T> T context(String message, FromSourceLocation sourceLocation, Evaluator<T> f) throws RuleError {
        try {
            return f.call();
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

    public RuleError withContext(String context, SourceLocation loc) {
        this.contexts.add(Pair.of(context, loc));
        return this;
    }

    @Override
    public String toString() {
        StringBuilder message = new StringBuilder();
        SourceLocation lastLoc = SourceLocation.none();
        for (int i = contexts.size() - 1; i >= 0; i--) {
            Pair<String, SourceLocation> context = contexts.get(i);
            message.append(context.left);
            message.append("\n");
            if (context.right != SourceLocation.NONE && context.right != lastLoc) {
                message.append("  at ")
                        .append(context.right.getSourceLocation().getFilename())
                        .append(":")
                        .append(context.right.getSourceLocation().getLine())
                        .append("\n");
                lastLoc = context.right;
            }
        }

        message.append(root.getMessageWithoutLocation());
        if (root.getSourceLocation() != SourceLocation.none() && root.getSourceLocation() != lastLoc) {
            message.append("\n").append("  at ")
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
