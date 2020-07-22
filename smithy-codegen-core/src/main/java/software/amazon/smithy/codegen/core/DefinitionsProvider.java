/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.codegen.core;

import software.amazon.smithy.model.shapes.Shape;

public class DefinitionsProvider extends SymbolProviderDecorator {
    protected ArtifactDefinitions definitions;
    private boolean isArtifactDefinitionsFilled = false;

    /**
     * Constructor for {@link SymbolProviderDecorator}.
     *
     * @param provider The {@link SymbolProvider} to be decorated.
     */
    public DefinitionsProvider(SymbolProvider provider, ArtifactDefinitions definitions) {
        super(provider);
        this.definitions = definitions;
    }

    @Override
    public Symbol toSymbol(Shape shape) {
        Symbol symbol = provider.toSymbol(shape);
        if (!isArtifactDefinitionsFilled) {
            symbol.toBuilder().putProperty(TraceFile.DEFINITIONS_TEXT,definitions);
            isArtifactDefinitionsFilled = true;
        }
        return symbol;
    }
}
