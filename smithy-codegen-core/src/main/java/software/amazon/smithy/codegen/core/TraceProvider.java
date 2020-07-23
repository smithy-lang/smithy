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

import java.util.UUID;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

public class TraceProvider extends SymbolProviderDecorator {
    private TraceFile.Builder traceFileBuilder = TraceFile.builder();

    private boolean isMetadataFilled = false;
    private boolean isDefinitionsFilled = false;

    /**
     * Constructor for {@link SymbolProviderDecorator}.
     *
     * @param provider The {@link SymbolProvider} to be decorated.
     */
    public TraceProvider(SymbolProvider provider) {
        super(provider);
    }

    /**
     * Fills ArtifactMetadata with a type and timestamp based on symbol, and a temporary UUID for version and
     * id to be replaced once the version and id for the generated code are known.
     *
     * @param symbol Symbol to extract metadata from.
     */
    public static ArtifactMetadata fillArtifactMetadata(Symbol symbol) {
        //temporarily set id and version as a UUID, this should be changed later once the version is known
        String tempIdVersion = UUID.randomUUID().toString();
        return ArtifactMetadata.builder()
                .version(tempIdVersion)
                .id(tempIdVersion)
                .type(getArtifactMetadataType(symbol))
                .setTimestampAsNow()
                .build();
    }

    /**
     * Gets the language type for ArtifactMetadata from the file extension of the source file of a symbol.
     * Language specific code generators can implement their own version of this.
     *
     * @param symbol Symbol to extract type from.
     * @return String with the type of language.
     */
    public static String getArtifactMetadataType(Symbol symbol) {
        String type = "";
        String[] split = symbol.getDefinitionFile().split("\\.");
        String extension = split[split.length - 1].toLowerCase();
        switch (extension) {
            case "ts":
                type = "TypeScript";
                break;
            case "go":
                type = "Go";
                break;
            case "py":
                type = "Python";
                break;
            case "java":
                type = "Java";
                break;
            case "c":
                type = "C";
                break;
            case "rs":
                type = "Rust";
                break;
            default:
                type = "unrecognized type";
                break;
        }
        return type;
    }

    /**
     * Extracts a {@link ShapeId} from a {@link Symbol} and creates a {@link ShapeLink} using all default mappings
     * defined in this class. The created ShapeLink has a type, id, and file.
     *
     * @param symbol The {@link Symbol} to extract information from for the {@link TraceFile}'s shapes map.
     */
    public static ShapeLink getShapeLink(Symbol symbol, Shape shape) {
        //set ShapeLink's file
        ShapeLink.Builder builder = ShapeLink.builder()
                .type(getShapeLinkType(shape))
                .id(getShapeLinkId(symbol))
                .file(getShapeLinkFile(symbol));

        return builder.build();
    }

    /**
     * Provides a default mapping from a {@link Symbol} to an Id.
     *
     * @param symbol {@link Symbol} to extract an id from.
     * @return String that contains the extracted id.
     */
    public static String getShapeLinkId(Symbol symbol) {
        return symbol.toString()
                .replace("./", "")
                .replace("/", ".");
    }

    /**
     * Provides a default mapping from a symbol to type where default type values are "FIELD" and "METHOD".
     * If the shape for the ShapeLink is an operation shape, then the ShapeLink is a method. Otherwise, it
     * is a field.
     *
     * @param shape The Shape to extract a type from.
     * @return String that contains the extracted type.
     */
    public static String getShapeLinkType(Shape shape) {
        String type = "FIELD";
        if (shape.getType().equals(ShapeType.OPERATION)) {
            type = "METHOD";
        }
        return type;
    }

    /**
     * Provides a default mapping from a symbol to a ShapeLink's file.
     *
     * @param symbol The symbol to extract a file from.
     * @return String that contains the extracted file.
     */
    public static String getShapeLinkFile(Symbol symbol) {
        return symbol.getDefinitionFile();
    }

    @Override
    public Symbol toSymbol(Shape shape) {
        Symbol symbol = provider.toSymbol(shape);

        if (!Prelude.isPreludeShape(shape.getId())) {
            //if metadata hasn't been filled yet
            if (!isMetadataFilled) {
                //if a language specific code generator has added an artifact property to our symbol
                if (symbol.getProperty(TraceFile.ARTIFACT_TEXT).isPresent()) {
                    traceFileBuilder.artifact(
                            symbol.expectProperty(TraceFile.ARTIFACT_TEXT, ArtifactMetadata.class));
                } else {
                    traceFileBuilder.artifact(fillArtifactMetadata(symbol));
                }
                isMetadataFilled = true;
            }

            //if a language specific code generator has added a definitions property to our symbol
            if (!isDefinitionsFilled && symbol.getProperty(TraceFile.DEFINITIONS_TEXT).isPresent()) {
                traceFileBuilder.definitions(
                        symbol.expectProperty(TraceFile.DEFINITIONS_TEXT, ArtifactDefinitions.class));
                isDefinitionsFilled = true;
            }

            //filling the ShapeLink for this symbol
            ShapeLink link;
            //if a language specific code generator has added a ShapeLink property to our symbol
            if (symbol.getProperty(TraceFile.SHAPES_TEXT).isPresent()) {
                link = symbol.expectProperty(TraceFile.SHAPES_TEXT, ShapeLink.class);
            } else {
                link = getShapeLink(symbol, shape);
            }
            traceFileBuilder.addShapeLink(shape.getId(), link);
        }
        return symbol;
    }

    public TraceFile getTraceFile() {
        return traceFileBuilder.build();
    }

}
