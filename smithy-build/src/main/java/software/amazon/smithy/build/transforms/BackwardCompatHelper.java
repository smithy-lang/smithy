/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.ListUtils;

/**
 * Helper class used to allow older versions of smithy-build.json files to
 * automatically rewrite a list of strings in an object to a member named
 * "__args" that contains the list of strings.
 *
 * <p>For example, the following deprecated JSON:
 *
 * <pre>{@code
 * {
 *     "version": "1.0",
 *     "projections": {
 *         "projection-name": {
 *             "transforms": [
 *                 {
 *                     "name": "transform-name",
 *                     "args": [
 *                         "argument1",
 *                         "argument2"
 *                     ]
 *                 }
 *             }
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>Is rewritten to the following JSON using {@code ConfigLoader}:
 *
 * <pre>{@code
 * {
 *     "version": "1.0",
 *     "projections": {
 *         "projection-name": {
 *             "transforms": [
 *                 {
 *                     "name": "transform-name",
 *                     "args": {
 *                         "__args": [
 *                             "argument1",
 *                             "argument2"
 *                         ]
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>And this, in turn, uses the result of {@link #getBackwardCompatibleNameMapping()}
 * to rewrite the JSON to the preferred format:
 *
 * <pre>{@code
 * {
 *     "version": "1.0",
 *     "projections": {
 *         "projection-name": {
 *             "transforms": [
 *                 {
 *                     "name": "transform-name",
 *                     "args": {
 *                         "<result of getBackwardCompatibleNameMapping()>": [
 *                             "argument1",
 *                             "argument2"
 *                         ]
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 * }</pre>
 *
 * @param <T> Type of configuration object to deserialize into.
 */
abstract class BackwardCompatHelper<T> extends ConfigurableProjectionTransformer<T> {

    private static final Logger LOGGER = Logger.getLogger(BackwardCompatHelper.class.getName());
    private static final String ARGS = "__args";

    /**
     * Gets the name that "__args" is to be rewritten to.
     *
     * @return Returns the name to rewrite.
     */
    abstract String getBackwardCompatibleNameMapping();

    @Override
    public Model transform(TransformContext context) {
        return super.transform(updateContextIfNecessary(context));
    }

    @Override
    public List<String> getAdditionalProjections(TransformContext context) {
        return getAdditionalProjectionsFunction()
                // short-circuit updating the context if additional projections are not supported
                .map(unused -> super.getAdditionalProjections(updateContextIfNecessary(context)))
                .orElseGet(ListUtils::of);

    }

    private TransformContext updateContextIfNecessary(TransformContext context) {
        ObjectNode original = context.getSettings();

        if (!original.getMember(ARGS).isPresent()) {
            return context;
        }

        LOGGER.warning(() -> String.format(
                "Deprecated projection transform arguments detected for `%s`; change this list of strings "
                        + "to an object with a property named `%s`",
                getName(),
                getBackwardCompatibleNameMapping()));

        ObjectNode updated = original.toBuilder()
                .withMember(getBackwardCompatibleNameMapping(), original.getMember(ARGS).get())
                .withoutMember(ARGS)
                .build();

        return context.toBuilder().settings(updated).build();
    }
}
