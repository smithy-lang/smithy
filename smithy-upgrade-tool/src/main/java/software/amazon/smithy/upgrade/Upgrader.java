/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.upgrade;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.SmithyIdlModelSerializer;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidatedResultException;
import software.amazon.smithy.utils.IoUtils;

final class Upgrader {
    private static final Pattern VERSION = Pattern.compile("(?m)^\\s*\\$\\s*version:\\s*\"1\\.0\"\\s*$");

    private Upgrader() {
    }

    static String upgradeFile(Path astPath, Path filePath) {
        Model completeModel = Model.assembler()
                .addImport(astPath)
                .assemble()
                .unwrap();

        ValidatedResult<Model> optionalModel = Model.assembler()
                .addImport(filePath)
                .assemble();

        if (!optionalModel.getResult().isPresent()) {
            try {
                optionalModel.unwrap();
            } catch (ValidatedResultException e) {
                throw new RuntimeException(String.format("Unable to load model file: %s\n\n%s", filePath, e));
            }
            throw new RuntimeException(String.format("Unable to load model file: %s", filePath));
        }

        // We use this partial model just to be sure about the source locations
        Model partialModel = optionalModel.getResult().get();

        ShapeUpgradeVisitor visitor = new ShapeUpgradeVisitor(completeModel, IoUtils.readUtf8File(filePath));

        partialModel.shapes()
                .filter(shape -> shape.getSourceLocation().getFilename().equals(filePath.toString()))
                // Apply updates to the shapes at the bottom of the file first.
                // This lets us modify the file without invalidating the existing
                // source locations.
                .sorted(new SourceLocationSorter().reversed())
                .forEach(shape -> shape.accept(visitor));

        return updateVersion(visitor.getModelString());
    }

    private static String updateVersion(String modelString) {
        Matcher matcher = VERSION.matcher(modelString);
        if (matcher.find()) {
            return matcher.replaceFirst("\\$version: \"2.0\"\n");
        }
        return "$version: \"2.0\"\n\n" + modelString;
    }

    // Sorts shapes in the order they appear in the file.
    private static class SourceLocationSorter implements Comparator<FromSourceLocation> {
        @Override
        public int compare(FromSourceLocation s1, FromSourceLocation s2) {
            SourceLocation sourceLocation = s1.getSourceLocation();
            SourceLocation otherSourceLocation = s2.getSourceLocation();

            if (!sourceLocation.getFilename().equals(otherSourceLocation.getFilename())) {
                return sourceLocation.getFilename().compareTo(otherSourceLocation.getFilename());
            }

            int lineComparison = Integer.compare(sourceLocation.getLine(), otherSourceLocation.getLine());
            if (lineComparison != 0) {
                return lineComparison;
            }

            return Integer.compare(sourceLocation.getColumn(), otherSourceLocation.getColumn());
        }
    }

    private static class TraitSorter implements Comparator<Trait> {
        @Override
        public int compare(Trait o1, Trait o2) {
            return new SourceLocationSorter().compare(o1.getSourceLocation(), o2.getSourceLocation());
        }
    }

    private static class ShapeUpgradeVisitor extends ShapeVisitor.Default<Void> {
        private final Model completeModel;
        private String modelString;

        ShapeUpgradeVisitor(Model completeModel, String modelString) {
            this.completeModel = completeModel;
            this.modelString = modelString;
        }

        String getModelString() {
            if (!modelString.endsWith("\n")) {
                modelString = modelString + "\n";
            }
            return modelString;
        }

        @Override
        protected Void getDefault(Shape shape) {
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            if (!shape.hasTrait(EnumTrait.class)) {
                return null;
            }

            EnumTrait enumTrait = shape.expectTrait(EnumTrait.class);
            if (!enumTrait.getValues().iterator().next().getName().isPresent()) {
                return null;
            }

            insertLine(shape.getSourceLocation().getLine() + 1, writeEnum(shape));
            eraseLine(shape.getSourceLocation().getLine());
            eraseTrait(shape, enumTrait);

            return null;
        }

        private void insertLine(int lineNumber, String line) {
            List<String> lines = new ArrayList<>(Arrays.asList(modelString.split("\n")));
            lines.add(lineNumber - 1, line);
            modelString = String.join("\n", lines);
        }

        private void eraseLine(int lineNumber) {
            List<String> lines = new ArrayList<>(Arrays.asList(modelString.split("\n")));
            lines.remove(lineNumber - 1);
            modelString = String.join("\n", lines);
        }

        private void eraseTrait(Shape shape, Trait trait) {
            SourceLocation from = findRealTraitStart(trait.getSourceLocation());
            SourceLocation to = findLocationAfterTrait(shape, trait.getClass());
            erase(from, to);
        }

        private void erase(SourceLocation from, SourceLocation to) {
            int fromIndex = sourceLocationToIndex(from);
            int toIndex = sourceLocationToIndex(to);
            modelString = modelString.substring(0, fromIndex) + modelString.substring(toIndex);
        }

        // The SourceLocation that traits report is actually the location of
        // their value node, so we need to look backwards from that to find
        // where the trait definition actually begins.
        private SourceLocation findRealTraitStart(SourceLocation location) {
            // Annotation traits don't necessarily have the right column, so we
            // move back a column even before a rewind so we don't get index
            // errors.
            location = new SourceLocation(location.getFilename(), location.getLine(), location.getColumn() - 1);
            return rewindUntil(location, index -> modelString.charAt(index) == '@');
        }

        private SourceLocation rewindUntil(SourceLocation location, Function<Integer, Boolean> condition) {
            int index = sourceLocationToIndex(location);
            while (!condition.apply(index)) {
                index--;
            }
            return indexToSourceLocation(index, location.getFilename());
        }

        private int sourceLocationToIndex(SourceLocation location) {
            int line = 1;
            int column = 1;
            for (int i = 0; i < modelString.length(); i++) {
                if (modelString.charAt(i) == '\n') {
                    line++;
                    column = 1;
                    continue;
                }
                if (line == location.getLine() && column == location.getColumn()) {
                    return i;
                }
                column++;
            }
            throw new IllegalStateException(String.format("Unable to find location: %s", location));
        }

        private SourceLocation indexToSourceLocation(int index, String fileName) {
            int line = 1;
            int column = 1;
            for (int i = 0; i <= index; i++) {
                if (modelString.charAt(i) == '\n') {
                    line++;
                    column = 1;
                    continue;
                }
                if (index == i) {
                    return new SourceLocation(fileName, line, column);
                }
                column++;
            }
            throw new IllegalStateException(String.format(
                    "Unable to convert model string index %d to a source location.", index));
        }

        private SourceLocation findLocationAfterTrait(Shape shape, Class<? extends Trait> target) {
            boolean haveSeenTarget = false;
            List<Trait> traits = new ArrayList<>(shape.getIntroducedTraits().values());
            traits.sort(new TraitSorter());
            for (Trait trait : traits) {
                if (target.isInstance(trait)) {
                    haveSeenTarget = true;
                } else if (haveSeenTarget && !trait.getSourceLocation().equals(SourceLocation.NONE)) {
                    return findRealTraitStart(trait.getSourceLocation());
                }
            }
            return shape.getSourceLocation();
        }

        private String writeEnum(StringShape shape) {
            // Strip all the traits from the shape except the enum trait.
            // We're leaving the other traits where they are in the model
            // string to preserve things like comments as much as is possible.
            StringShape stripped = shape.toBuilder()
                    .clearTraits()
                    .addTrait(shape.expectTrait(EnumTrait.class))
                    .build();

            // Build a faux model that only contains the enum we want to wrte.
            Model model = Model.assembler()
                    .addShapes(stripped)
                    .assemble().unwrap();

            // Use existing conversion tools to convert it to an enum shape,
            // then serialize it using the idl serializer.
            model = ModelTransformer.create().changeStringEnumsToEnumShapes(model);
            Map<Path, String> files = SmithyIdlModelSerializer.builder().build().serialize(model);

            // There's only one shape, so there should only be one file.
            String serialized = files.values().iterator().next();

            // The serialized file will contain things we don't want, like the
            // namespace and version statements, so here we strip everything
            // we find before the enum statement.
            ArrayList<String> lines = new ArrayList<>();
            boolean foundEnum = false;
            for (String line : serialized.split("\n")) {
                if (foundEnum) {
                    lines.add(line);
                } else if (line.startsWith("enum")) {
                    lines.add(line);
                    foundEnum = true;
                }
            }

            return String.join("\n", lines);
        }
    }
}
