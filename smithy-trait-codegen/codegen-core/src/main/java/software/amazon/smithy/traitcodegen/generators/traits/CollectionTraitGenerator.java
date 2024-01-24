/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.traits;

import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.generators.common.BuilderGenerator;
import software.amazon.smithy.traitcodegen.generators.common.ConstructorWithBuilderGenerator;
import software.amazon.smithy.traitcodegen.generators.common.FromNodeGenerator;
import software.amazon.smithy.traitcodegen.generators.common.GetterGenerator;
import software.amazon.smithy.traitcodegen.generators.common.PropertiesGenerator;
import software.amazon.smithy.traitcodegen.generators.common.ToNodeGenerator;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Generates a Java class for collection (list/map) traits.
 */
final class CollectionTraitGenerator extends TraitGenerator {

    @Override
    protected boolean implementsToSmithyBuilder() {
        return true;
    }

    @Override
    protected void writeTraitBody(TraitCodegenWriter writer, GenerateTraitDirective directive) {
        new PropertiesGenerator(writer, directive.shape(), directive.symbolProvider()).run();
        new ConstructorWithBuilderGenerator(writer, directive.symbol(), directive.shape(),
                directive.symbolProvider()).run();
        new ToNodeGenerator(writer, directive.shape(), directive.symbolProvider(), directive.model()).run();
        new FromNodeGenerator(writer, directive.symbol(), directive.shape(), directive.symbolProvider(),
                directive.model()).run();
        new GetterGenerator(writer, directive.symbolProvider(), directive.shape(), directive.model()).run();
        new BuilderGenerator(writer, directive.symbol(), directive.symbolProvider(), directive.shape(),
                directive.model()).run();
    }
}
