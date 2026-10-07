/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.loader.IdlTokenizer;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.SmithyIdlModelSerializer;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.traits.SuppressTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.synthetic.SyntheticShapeTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.syntax.CapturedToken;
import software.amazon.smithy.syntax.TokenTree;
import software.amazon.smithy.syntax.TreeCursor;
import software.amazon.smithy.syntax.TreeType;
import software.amazon.smithy.utils.Pair;

/**
 * Replaces collection targets without reformatting files or removing named declarations.
 *
 * <p>Proposed edits are validated together, then invalid batches are split until
 * unsafe reference sites can be left named. Source text outside the edited spans
 * is preserved. Inserting documentation into a single-line declaration requires
 * introducing new lines; the rest of that declaration is not reformatted.
 * Copied traits use the model serializer's shared IDL renderer.
 */
final class InlineCollectionMigration {
    private static final int MAX_INLINE_DEPTH = 3;
    private static final Logger LOGGER = Logger.getLogger(InlineCollectionMigration.class.getName());

    private final Model model;
    private final boolean force;
    private final Map<SourceLocation, MemberShape> members = new HashMap<>();

    InlineCollectionMigration(Model model, boolean force) {
        this.model = model;
        this.force = force;
        for (MemberShape member : model.getMemberShapes()) {
            // Inherited members can share the source location of their mixin's member.
            if (!members.containsKey(member.getSourceLocation()) || member.getMixins().isEmpty()) {
                members.put(member.getSourceLocation(), member);
            }
        }
    }

    Map<Path, String> migrate(List<Pair<Path, String>> files, ModelAssembler assembler) {
        Map<Path, FileMigration> migrations = new LinkedHashMap<>();
        List<Change> changes = new ArrayList<>();
        for (Pair<Path, String> file : files) {
            FileMigration migration = collect(file.left, file.right);
            migrations.put(file.left, migration);
            changes.addAll(migration.changes);
        }
        Set<Change> accepted = new LinkedHashSet<>();
        acceptValidChanges(groupMixinChanges(changes), accepted, migrations, assembler);
        return render(migrations, accepted);
    }

    private List<List<Change>> groupMixinChanges(List<Change> changes) {
        // Explicit mixin redefinitions must be edited together with their
        // originals, even if an unrelated invalid edit forces a batch split.
        Map<SourceLocation, List<SourceLocation>> neighbors = new HashMap<>();
        for (MemberShape member : model.getMemberShapes()) {
            for (ShapeId mixinId : member.getMixins()) {
                SourceLocation source = member.getSourceLocation();
                SourceLocation mixin = model.expectShape(mixinId, MemberShape.class).getSourceLocation();
                neighbors.computeIfAbsent(source, ignored -> new ArrayList<>()).add(mixin);
                neighbors.computeIfAbsent(mixin, ignored -> new ArrayList<>()).add(source);
            }
        }
        Map<SourceLocation, Change> bySource = new HashMap<>();
        changes.forEach(change -> bySource.put(change.member.getSourceLocation(), change));
        Set<SourceLocation> seen = new HashSet<>();
        List<List<Change>> groups = new ArrayList<>();
        for (Change change : changes) {
            if (seen.contains(change.member.getSourceLocation())) {
                continue;
            }
            List<Change> group = new ArrayList<>();
            Queue<SourceLocation> pending = new ArrayDeque<>();
            pending.add(change.member.getSourceLocation());
            while (!pending.isEmpty()) {
                SourceLocation source = pending.remove();
                if (seen.add(source)) {
                    if (bySource.containsKey(source)) {
                        group.add(bySource.get(source));
                    }
                    pending.addAll(neighbors.getOrDefault(source, Collections.emptyList()));
                }
            }
            groups.add(group);
        }
        return groups;
    }

    private void acceptValidChanges(
            List<List<Change>> proposed,
            Set<Change> accepted,
            Map<Path, FileMigration> migrations,
            ModelAssembler assembler
    ) {
        if (proposed.isEmpty()) {
            return;
        }
        List<Change> batch = proposed.stream().flatMap(List::stream).collect(Collectors.toList());
        accepted.addAll(batch);
        List<ValidationEvent> failures = validate(migrations, accepted, assembler);
        if (failures.isEmpty()) {
            return;
        }
        accepted.removeAll(batch);
        if (proposed.size() == 1) {
            for (Change change : batch) {
                LOGGER.fine(() -> "Keeping collection reference at " + change.member.getSourceLocation()
                        + " (" + change.member.getId() + "): " + failures);
            }
            return;
        }
        int middle = proposed.size() / 2;
        acceptValidChanges(proposed.subList(0, middle), accepted, migrations, assembler);
        acceptValidChanges(proposed.subList(middle, proposed.size()), accepted, migrations, assembler);
    }

    private List<ValidationEvent> validate(
            Map<Path, FileMigration> migrations,
            Set<Change> accepted,
            ModelAssembler assembler
    ) {
        ModelAssembler candidate = assembler.copy();
        render(migrations, accepted)
                .forEach((path, contents) -> candidate.addUnparsedModel(path.toAbsolutePath().toString(), contents));
        // Full validation is required: it applies suppressions to loader DANGER
        // events and checks interactions with ancestors, resources, and mixins.
        ValidatedResult<Model> result = candidate.assemble();
        // Retain only diagnostics across recursive batch splits, never model copies.
        return result.getValidationEvents()
                .stream()
                .filter(event -> event.getSeverity() == Severity.ERROR || event.getSeverity() == Severity.DANGER)
                .collect(Collectors.toList());
    }

    private Map<Path, String> render(Map<Path, FileMigration> migrations, Set<Change> accepted) {
        Map<Path, String> result = new LinkedHashMap<>();
        migrations.forEach((path, migration) -> result.put(path, migration.finish(accepted)));
        return result;
    }

    private FileMigration collect(Path path, String contents) {
        TreeCursor root = TokenTree.of(IdlTokenizer.create(path.toAbsolutePath().toString(), contents)).zipper();
        FileMigration migration = new FileMigration(root, contents);
        for (TreeCursor explicit : root.findChildrenByType(TreeType.EXPLICIT_SHAPE_MEMBER)) {
            MemberShape member = members.get(explicit.getSourceLocation());
            if (member != null) {
                Change change = new Change(member);
                migration.migrateTarget(explicit.getFirstChild(TreeType.MEMBER_TARGET),
                        member,
                        0,
                        explicit.getParent(),
                        change);
                if (!change.edits.isEmpty()) {
                    migration.changes.add(change);
                }
            }
        }
        return migration;
    }

    private boolean isCandidate(Shape shape) {
        return candidateRefusal(shape) == null;
    }

    private String candidateRefusal(Shape shape) {
        if (!(shape.isListShape() || shape.isMapShape()) || shape.hasTrait(SyntheticShapeTrait.ID)) {
            return "target is not a named list or map";
        }
        if (shape.hasTrait(PrivateTrait.ID) || shape.hasTrait(SuppressTrait.ID)) {
            return "collection carries private or suppress declaration traits";
        }
        if (shape.members().stream().anyMatch(member -> !member.getAllTraits().isEmpty())) {
            return "collection members have traits";
        }
        if (!force && !shape.getAllTraits().isEmpty()) {
            return "collection has traits; copying them requires --force-inline-collections";
        }
        if (shape.getAllTraits()
                .keySet()
                .stream()
                .anyMatch(id -> !model.getShape(id)
                        .filter(definition -> definition.hasTrait(TraitDefinition.ID))
                        .isPresent())) {
            return "collection has an unknown trait whose member applicability cannot be checked";
        }
        return null;
    }

    private final class FileMigration {
        private final String contents;
        private final String namespace;
        private final Map<String, ShapeId> imports = new HashMap<>();
        private final List<Change> changes = new ArrayList<>();

        FileMigration(TreeCursor root, String contents) {
            this.contents = contents;
            namespace = root.findChildrenByType(TreeType.NAMESPACE_STATEMENT)
                    .get(0)
                    .getFirstChild(TreeType.NAMESPACE)
                    .getTree()
                    .concatTokens();
            for (TreeCursor use : root.findChildrenByType(TreeType.USE_STATEMENT)) {
                ShapeId id = ShapeId.from(use.getFirstChild(TreeType.ABSOLUTE_ROOT_SHAPE_ID).getTree().concatTokens());
                imports.put(id.getName(), id);
            }
        }

        private void migrateTarget(
                TreeCursor target,
                MemberShape member,
                int depth,
                TreeCursor memberDefinition,
                Change change
        ) {
            Shape shape = model.expectShape(member.getTarget());
            TreeCursor shapeId = target.getFirstChild(TreeType.SHAPE_ID);
            if (shapeId != null) {
                String reason = candidateRefusal(shape);
                if (reason == null && depth + minimumInlineDepth(shape) > MAX_INLINE_DEPTH) {
                    reason = "inlining would exceed three collection levels";
                }
                if (reason == null && !canExposeElements(shape)) {
                    reason = "collection elements are private to another namespace";
                }
                if (reason != null) {
                    if ((shape.isListShape() || shape.isMapShape()) && !shape.hasTrait(SyntheticShapeTrait.ID)) {
                        String refusal = reason;
                        LOGGER.fine(() -> "Keeping collection reference at " + member.getSourceLocation()
                                + " (" + member.getId() + "): " + refusal);
                    }
                    return;
                }
                // There is no annotation position inside existing inline syntax.
                if (depth > 0 && !shape.getAllTraits().isEmpty()) {
                    LOGGER.fine(() -> "Keeping nested collection reference at " + member.getSourceLocation()
                            + ": inline elements cannot carry the collection's traits");
                    return;
                }
                String replacement = renderCollection(shape, depth);
                change.edits.add(new Edit(start(shapeId.getTree()), end(shapeId.getTree()), replacement));
                if (!shape.getAllTraits().isEmpty()) {
                    insertTraits(shape, member, memberDefinition, change);
                }
            } else if (shape.isListShape()) {
                TreeCursor inline = target.getFirstChild(TreeType.INLINE_LIST_TARGET);
                migrateTarget(inline.getFirstChild(TreeType.MEMBER_TARGET),
                        shape.asListShape().get().getMember(),
                        depth + 1,
                        memberDefinition,
                        change);
            } else if (shape.isMapShape()) {
                TreeCursor inline = target.getFirstChild(TreeType.INLINE_MAP_TARGET);
                List<TreeCursor> targets = inline.getChildrenByType(TreeType.MEMBER_TARGET);
                MapShape map = shape.asMapShape().get();
                migrateTarget(targets.get(0), map.getKey(), depth + 1, memberDefinition, change);
                migrateTarget(targets.get(1), map.getValue(), depth + 1, memberDefinition, change);
            }
        }

        private String renderCollection(Shape shape, int depth) {
            String result;
            if (shape.isListShape()) {
                result = "[" + renderNested(shape.asListShape().get().getMember().getTarget(), depth + 1)
                        + "]";
            } else {
                MapShape map = shape.asMapShape().get();
                result = "{" + renderNested(map.getKey().getTarget(), depth + 1)
                        + ": " + renderNested(map.getValue().getTarget(), depth + 1) + "}";
            }
            return result;
        }

        private String renderNested(ShapeId id, int depth) {
            Shape shape = model.expectShape(id);
            if (shape.hasTrait(SyntheticShapeTrait.ID)) {
                // Synthetic shapes have no named declaration to fall back to.
                return renderCollection(shape, depth);
            }
            // Validated models cannot recurse solely through collections, and depth is bounded.
            if (depth + minimumInlineDepth(shape) <= MAX_INLINE_DEPTH
                    && isCandidate(shape)
                    && canExposeElements(shape)
                    && shape.getAllTraits().isEmpty()) {
                return renderCollection(shape, depth);
            }
            return formatId(id);
        }

        private boolean canExposeElements(Shape collection) {
            for (MemberShape member : collection.members()) {
                Shape element = model.expectShape(member.getTarget());
                if (element.hasTrait(PrivateTrait.ID) && !element.getId().getNamespace().equals(namespace)) {
                    return false;
                }
                if (element.hasTrait(SyntheticShapeTrait.ID) && !canExposeElements(element)) {
                    return false;
                }
            }
            return true;
        }

        private void insertTraits(Shape collection, MemberShape member, TreeCursor definition, Change change) {
            List<Trait> traits = new ArrayList<>(collection.getAllTraits().values());
            traits.removeIf(trait -> member.hasTrait(trait.toShapeId()));
            if (traits.isEmpty()) {
                return;
            }
            int position = start(definition.getTree());
            String indentation = contents.substring(contents.lastIndexOf('\n', position - 1) + 1, position);
            boolean ownLine = indentation.chars().allMatch(c -> c == ' ' || c == '\t');
            String newline = lineEnding(position);
            if (!ownLine) {
                int leadingSpaces = 0;
                while (leadingSpaces < indentation.length()
                        && (indentation.charAt(leadingSpaces) == ' ' || indentation.charAt(leadingSpaces) == '\t')) {
                    leadingSpaces++;
                }
                indentation = indentation.substring(0, leadingSpaces) + "    ";
            }
            String rendered = SmithyIdlModelSerializer.builder().build().serializeTraits(model, traits, this::formatId);
            String annotations = (ownLine ? "" : newline + indentation)
                    + rendered.replace("\n", newline + indentation);
            int editStart = position;
            if (!ownLine) {
                while (editStart > 0
                        && (contents.charAt(editStart - 1) == ' ' || contents.charAt(editStart - 1) == '\t')) {
                    editStart--;
                }
            }
            change.edits.add(new Edit(editStart, position, annotations));
        }

        private String lineEnding(int position) {
            int endOfLine = contents.indexOf('\n', position);
            if (endOfLine == -1) {
                endOfLine = contents.lastIndexOf('\n', position);
            }
            return endOfLine > 0 && contents.charAt(endOfLine - 1) == '\r' ? "\r\n" : "\n";
        }

        private String formatId(ShapeId id) {
            // Qualification must honor this file's existing imports. Unlike full
            // model serialization, this migration cannot introduce use statements.
            ShapeId imported = imports.get(id.getName());
            if (id.withoutMember().equals(imported)) {
                return id.asRelativeReference();
            }
            if (imported == null) {
                if (id.getNamespace().equals(namespace)) {
                    return id.asRelativeReference();
                }
                if (Prelude.isPreludeShape(id)
                        && !model.getShape(ShapeId.fromParts(namespace, id.getName())).isPresent()) {
                    return id.asRelativeReference();
                }
            }
            return id.toString();
        }

        private String finish(Set<Change> accepted) {
            List<Edit> edits = new ArrayList<>();
            for (Change change : changes) {
                if (accepted.contains(change)) {
                    edits.addAll(change.edits);
                }
            }
            // All positions refer to the original file, so apply edits from the end.
            edits.sort(Comparator.comparingInt((Edit edit) -> edit.start).reversed());
            StringBuilder result = new StringBuilder(contents);
            for (Edit edit : edits) {
                result.replace(edit.start, edit.end, edit.replacement);
            }
            return result.toString();
        }
    }

    private int minimumInlineDepth(Shape shape) {
        int nestedDepth = 0;
        for (MemberShape member : shape.members()) {
            Shape target = model.expectShape(member.getTarget());
            if (target.hasTrait(SyntheticShapeTrait.ID)) {
                nestedDepth = Math.max(nestedDepth, minimumInlineDepth(target));
            }
        }
        return 1 + nestedDepth;
    }

    private static int start(TokenTree tree) {
        return tree.tokens().findFirst().get().getPosition();
    }

    private static int end(TokenTree tree) {
        CapturedToken last = tree.tokens().reduce((left, right) -> right).get();
        return last.getPosition() + last.getSpan();
    }

    private static final class Edit {
        private final int start;
        private final int end;
        private final String replacement;

        private Edit(int start, int end, String replacement) {
            this.start = start;
            this.end = end;
            this.replacement = replacement;
        }
    }

    private static final class Change {
        private final MemberShape member;
        private final List<Edit> edits = new ArrayList<>();

        private Change(MemberShape member) {
            this.member = member;
        }
    }
}
