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

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.ParserUtils;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.SmithyIdlModelSerializer;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.SimpleParser;
import software.amazon.smithy.utils.StringUtils;

final class Upgrader {
    private static final Pattern VERSION = Pattern.compile("(?m)^\\s*\\$\\s*version:\\s*\"1\\.0\"\\s*$");
    private static final EnumSet<ShapeType> HAD_DEFAULT_VALUE_IN_1_0 = EnumSet.of(
            ShapeType.BYTE,
            ShapeType.SHORT,
            ShapeType.INTEGER,
            ShapeType.LONG,
            ShapeType.FLOAT,
            ShapeType.DOUBLE,
            ShapeType.BOOLEAN);

    private Upgrader() {
    }

    static String upgradeFile(Model completeModel, Path filePath) {
        ShapeUpgradeVisitor visitor = new ShapeUpgradeVisitor(completeModel, IoUtils.readUtf8File(filePath));

        completeModel.shapes()
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
    private static class SourceLocationSorter implements Comparator<FromSourceLocation>, Serializable {
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

    private static class TraitSorter implements Comparator<Trait>, Serializable {
        @Override
        public int compare(Trait o1, Trait o2) {
            return new SourceLocationSorter().compare(o1.getSourceLocation(), o2.getSourceLocation());
        }
    }

    private static class ShapeUpgradeVisitor extends ShapeVisitor.Default<Void> {
        private final Model completeModel;
        private ModelWriter writer;

        ShapeUpgradeVisitor(Model completeModel, String modelString) {
            this.completeModel = completeModel;
            this.writer = new ModelWriter(modelString);
        }

        String getModelString() {
            return writer.flush();
        }

        @Override
        protected Void getDefault(Shape shape) {
            if (shape.hasTrait(BoxTrait.class)) {
                writer.eraseTrait(shape, shape.expectTrait(BoxTrait.class));
            }
            // handle members in reverse definition order
            shape.members().stream()
                    .sorted(new SourceLocationSorter().reversed())
                    .forEach(this::handleMemberShape);
            return null;
        }

        private void handleMemberShape(MemberShape shape) {
            replacePrimitiveTarget(shape);

            if (hasSyntheticDefault(shape)) {
                SourceLocation memberLocation = shape.getSourceLocation();
                String padding = "";
                if (memberLocation.getColumn() > 1) {
                    padding = StringUtils.repeat(' ', memberLocation.getColumn() - 1);
                }
                writer.insertLine(shape.getSourceLocation().getLine(), padding + "@default");
            }

            if (shape.hasTrait(BoxTrait.class)) {
                writer.eraseTrait(shape, shape.expectTrait(BoxTrait.class));
            }
        }

        private void replacePrimitiveTarget(MemberShape member) {
            Shape target = completeModel.expectShape(member.getTarget());
            if (!Prelude.isPreludeShape(target) || !HAD_DEFAULT_VALUE_IN_1_0.contains(target.getType())) {
                return;
            }

            IdlAwareSimpleParser parser = new IdlAwareSimpleParser(writer.flush());
            parser.rewind(member.getSourceLocation());

            parser.consumeUntilNoLongerMatches(character -> character != ':');
            parser.skip();
            parser.ws();

            // Capture the start of the target identifier
            int start = parser.position();
            parser.consumeUntilNoLongerMatches(ParserUtils::isValidIdentifierCharacter);

            // Replace the target with the proper target. Note that we don't
            // need to do any sort of mapping because smithy already upgraded
            // the target, so we can just use the name of the target it added.
            writer.replace(start, parser.position(), target.getId().getName());
        }

        private boolean hasSyntheticDefault(MemberShape shape) {
            Shape target = completeModel.expectShape(shape.getTarget());
            if (!(HAD_DEFAULT_VALUE_IN_1_0.contains(target.getType()) && shape.hasTrait(DefaultTrait.class))) {
                return false;
            }
            // When Smithy injects the default trait, it sets the source
            // location equal to the shape's source location. This is
            // impossible in any other scenario, so we can use this info
            // to know whether it was injected or not.
            return shape.getSourceLocation().equals(shape.expectTrait(DefaultTrait.class).getSourceLocation());
        }

        @Override
        public Void memberShape(MemberShape shape) {
            // members are handled from their containers so that they can
            // be properly sorted.
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

            writer.insertLine(shape.getSourceLocation().getLine() + 1, serializeEnum(shape));
            writer.eraseLine(shape.getSourceLocation().getLine());
            writer.eraseTrait(shape, enumTrait);

            return null;
        }

        private String serializeEnum(StringShape shape) {
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

    private static class IdlAwareSimpleParser extends SimpleParser {
        public IdlAwareSimpleParser(String expression) {
            super(expression);
        }

        public void rewind(SourceLocation location) {
            rewind(0, 1, 1);
            while (!eof()) {
                if (line() == location.getLine() && column() == location.getColumn()) {
                    break;
                }
                skip();
            }
            if (eof()) {
                throw syntax("Expected a source location, but was EOF");
            }
        }

        @Override
        public void ws() {
            while (!eof()) {
                switch (peek()) {
                    case '/':
                        // If we see a comment, advance to the next line.
                        if (peek(1) == '/') {
                            consumeRemainingCharactersOnLine();
                            break;
                        } else {
                            return;
                        }
                    case ' ':
                    case '\t':
                    case '\r':
                    case '\n':
                    case ',':
                        skip();
                        break;
                    default:
                        return;
                }
            }
        }
    }

    private static class ModelWriter {
        private String contents;

        public ModelWriter(String contents) {
            this.contents = contents;
        }

        public String flush() {
            if (!contents.endsWith("\n")) {
                contents = contents + "\n";
            }
            return contents;
        }

        private void insertLine(int lineNumber, String line) {
            List<String> lines = new ArrayList<>(Arrays.asList(contents.split("\n")));
            lines.add(lineNumber - 1, line);
            contents = String.join("\n", lines);
        }

        private void eraseLine(int lineNumber) {
            List<String> lines = new ArrayList<>(Arrays.asList(contents.split("\n")));
            lines.remove(lineNumber - 1);
            contents = String.join("\n", lines);
        }

        private void eraseTrait(Shape shape, Trait trait) {
            SourceLocation to = findLocationAfterTrait(shape, trait.getClass());
            erase(trait.getSourceLocation(), to);
        }

        private SourceLocation findLocationAfterTrait(Shape shape, Class<? extends Trait> target) {
            boolean haveSeenTarget = false;
            List<Trait> traits = new ArrayList<>(shape.getIntroducedTraits().values());
            traits.sort(new TraitSorter());
            for (Trait trait : traits) {
                if (target.isInstance(trait)) {
                    haveSeenTarget = true;
                } else if (haveSeenTarget && !trait.getSourceLocation().equals(SourceLocation.NONE)) {
                    return trait.getSourceLocation();
                }
            }
            return shape.getSourceLocation();
        }

        private void erase(SourceLocation from, SourceLocation to) {
            IdlAwareSimpleParser parser = new IdlAwareSimpleParser(contents);
            parser.rewind(from);
            int fromPosition = parser.position();
            parser.rewind(to);
            int toPosition = parser.position();
            contents = contents.substring(0, fromPosition) + contents.substring(toPosition);
        }

        private void replace(int from, int to, String with) {
            contents = contents.substring(0, from) + with + contents.substring(to);
        }
    }
}
