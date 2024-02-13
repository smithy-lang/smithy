/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

@FunctionalInterface
public interface Mapper extends BiConsumer<TraitCodegenWriter, String> {
    default Consumer<TraitCodegenWriter> with(String var) {
        return w -> accept(w, var);
    }
}
