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
import software.amazon.smithy.model.loader.IdlToken;
import software.amazon.smithy.model.loader.IdlTokenizer;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipDirection;
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
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.syntax.CapturedToken;
import software.amazon.smithy.syntax.TokenTree;
import software.amazon.smithy.syntax.TreeCursor;
import software.amazon.smithy.syntax.TreeType;
import software.amazon.smithy.utils.Pair;

/**
 * Replaces collection targets without reformatting files.
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

    Map<Path, String> migrate(List<Pair<Path, String>> files, ModelAssembler assembler, boolean removeOrphanedShapes) {
        Map<Path, FileMigration> migrations = new LinkedHashMap<>();
        List<Change> changes = new ArrayList<>();
        for (Pair<Path, String> file : files) {
            FileMigration migration = collect(file.left, file.right);
            migrations.put(file.left, migration);
            changes.addAll(migration.changes);
        }
        Set<Change> accepted = new LinkedHashSet<>();
        acceptValidChanges(groupMixinChanges(changes), accepted, migrations, assembler);
        Map<Path, String> result = render(migrations, accepted);
        if (removeOrphanedShapes) {
            Set<ShapeId> inlined = accepted.stream()
                    .flatMap(change -> change.collections.stream())
                    .collect(Collectors.toSet());
            return removeOrphanedShapes(result, inlined, assembler);
        }
        return result;
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
        // Full validation is required: it applies suppressions to loader DANGER
        // events and checks interactions with ancestors, resources, and mixins.
        ValidatedResult<Model> result = assemble(render(migrations, accepted), assembler);
        // Retain only diagnostics across recursive batch splits, never model copies.
        return result.getValidationEvents()
                .stream()
                .filter(event -> event.getSeverity() == Severity.ERROR || event.getSeverity() == Severity.DANGER)
                .collect(Collectors.toList());
    }

    private ValidatedResult<Model> assemble(Map<Path, String> files, ModelAssembler assembler) {
        ModelAssembler candidate = assembler.copy();
        files.forEach((path, contents) -> candidate.addUnparsedModel(path.toAbsolutePath().toString(), contents));
        return candidate.assemble();
    }

    private Map<Path, String> removeOrphanedShapes(
            Map<Path, String> files,
            Set<ShapeId> inlined,
            ModelAssembler assembler
    ) {
        if (inlined.isEmpty()) {
            return files;
        }
        Removal state = new Removal(files, assembler);
        Set<String> filenames = files.keySet()
                .stream()
                .map(path -> path.toAbsolutePath().toString())
                .collect(Collectors.toSet());
        Set<ShapeId> remaining = inlined.stream()
                .filter(id -> filenames.contains(state.model.expectShape(id).getSourceLocation().getFilename()))
                .collect(Collectors.toSet());
        while (!remaining.isEmpty()) {
            NeighborProvider forward = NeighborProvider.withIdRefRelationships(state.model,
                    NeighborProvider.withTraitRelationships(state.model, NeighborProvider.of(state.model)));
            NeighborProvider reverse = NeighborProvider.reverse(state.model, forward);
            List<ShapeId> orphaned = remaining.stream()
                    .filter(id -> isOrphaned(state.model.expectShape(id), reverse))
                    .sorted()
                    .collect(Collectors.toList());
            if (orphaned.isEmpty()) {
                break;
            }
            // A declaration can be kept by validation even without a graph reference,
            // for example when a trait selector requires that shape to exist.
            remaining.removeAll(orphaned);
            state.remove(orphaned);
            // Reassemble after each wave so synthetic shapes belonging only to a
            // removed declaration no longer keep its nested collections referenced.
        }
        return state.files;
    }

    private boolean isOrphaned(Shape shape, NeighborProvider reverse) {
        List<Shape> targets = new ArrayList<>(shape.members());
        targets.add(shape);
        for (Shape target : targets) {
            for (Relationship reference : reverse.getNeighbors(target)) {
                if (reference.getDirection() == RelationshipDirection.DIRECTED
                        && !reference.getShape().getId().withoutMember().equals(shape.getId())) {
                    return false;
                }
            }
        }
        return true;
    }

    private Set<String> loaderWarnings(ValidatedResult<Model> result) {
        return result.getValidationEvents()
                .stream()
                .filter(event -> event.getSeverity() == Severity.WARNING && event.getId().equals(Validator.MODEL_ERROR))
                .map(event -> event.getMessage())
                .collect(Collectors.toSet());
    }

    private Map<Path, String> render(Map<Path, FileMigration> migrations, Set<Change> accepted) {
        Map<Path, String> result = new LinkedHashMap<>();
        migrations.forEach((path, migration) -> result.put(path, migration.finish(accepted)));
        return result;
    }

    private FileMigration collect(Path path, String contents) {
        FileMigration migration = new FileMigration(path, contents);
        for (TreeCursor explicit : migration.root.findChildrenByType(TreeType.EXPLICIT_SHAPE_MEMBER)) {
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

    private final class Removal {
        private Map<Path, String> files;
        private Model model;
        private final Set<String> initialLoaderWarnings;
        private final ModelAssembler assembler;
        private final Map<Path, FileMigration> parsedFiles = new HashMap<>();

        private Removal(Map<Path, String> files, ModelAssembler assembler) {
            this.files = files;
            this.assembler = assembler;
            ValidatedResult<Model> initial = assemble(files, assembler);
            model = initial.unwrap();
            initialLoaderWarnings = loaderWarnings(initial);
        }

        private void remove(List<ShapeId> proposed) {
            if (!tryRemove(proposed) && proposed.size() > 1) {
                int middle = proposed.size() / 2;
                remove(proposed.subList(0, middle));
                remove(proposed.subList(middle, proposed.size()));
            }
        }

        private boolean tryRemove(List<ShapeId> proposed) {
            Map<Path, String> candidate = new LinkedHashMap<>();
            Set<ShapeId> ids = new HashSet<>(proposed);
            files.forEach((path, contents) -> candidate.put(path,
                    parsedFiles.computeIfAbsent(path, ignored -> new FileMigration(path, contents))
                            .removeDeclarations(ids)));
            ValidatedResult<Model> result = assemble(candidate, assembler);
            // Dangling use statements only produce loader warnings. Since files
            // outside the migration scope cannot be edited, retain their targets.
            // Compare messages so shifting source lines does not turn an existing
            // warning into a new one.
            if (!result.isBroken() && initialLoaderWarnings.containsAll(loaderWarnings(result))) {
                files = candidate;
                parsedFiles.clear();
                model = result.unwrap();
                proposed.forEach(id -> LOGGER.fine(() -> "Removed orphaned collection " + id));
                return true;
            }
            if (proposed.size() == 1) {
                LOGGER.fine(() -> "Keeping orphaned collection " + proposed.get(0) + ": "
                        + result.getValidationEvents());
            }
            // Release failed model copies before recursively splitting the batch.
            return false;
        }
    }

    private final class FileMigration {
        private final String contents;
        private final TreeCursor root;
        private final String namespace;
        private final Map<String, ShapeId> imports = new HashMap<>();
        private final List<Change> changes = new ArrayList<>();

        FileMigration(Path path, String contents) {
            this.contents = contents;
            root = TokenTree.of(IdlTokenizer.create(path.toAbsolutePath().toString(), contents)).zipper();
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
                String replacement = renderCollection(shape, depth, change);
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

        private String renderCollection(Shape shape, int depth, Change change) {
            if (!shape.hasTrait(SyntheticShapeTrait.ID)) {
                change.collections.add(shape.getId());
            }
            String result;
            if (shape.isListShape()) {
                result = "[" + renderNested(shape.asListShape().get().getMember().getTarget(), depth + 1, change)
                        + "]";
            } else {
                MapShape map = shape.asMapShape().get();
                result = "{" + renderNested(map.getKey().getTarget(), depth + 1, change)
                        + ": " + renderNested(map.getValue().getTarget(), depth + 1, change) + "}";
            }
            return result;
        }

        private String renderNested(ShapeId id, int depth, Change change) {
            Shape shape = model.expectShape(id);
            if (shape.hasTrait(SyntheticShapeTrait.ID)) {
                // Synthetic shapes have no named declaration to fall back to.
                return renderCollection(shape, depth, change);
            }
            // Validated models cannot recurse solely through collections, and depth is bounded.
            if (depth + minimumInlineDepth(shape) <= MAX_INLINE_DEPTH
                    && isCandidate(shape)
                    && canExposeElements(shape)
                    && shape.getAllTraits().isEmpty()) {
                return renderCollection(shape, depth, change);
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
            return applyEdits(edits);
        }

        private String removeDeclarations(Set<ShapeId> ids) {
            List<CapturedToken> tokens = root.getTree().tokens().collect(Collectors.toList());
            Map<Integer, Integer> tokenPositions = new HashMap<>();
            for (int i = 0; i < tokens.size(); i++) {
                tokenPositions.put(tokens.get(i).getPosition(), i);
            }
            List<Edit> edits = new ArrayList<>();
            for (TreeCursor statement : root.findChildrenByType(TreeType.SHAPE_STATEMENT)) {
                TreeCursor aggregate = statement.getFirstChild(TreeType.SHAPE).getFirstChild(TreeType.AGGREGATE_SHAPE);
                if (aggregate != null) {
                    String name = aggregate.getFirstChild(TreeType.IDENTIFIER).getTree().concatTokens();
                    if (ids.contains(ShapeId.fromParts(namespace, name))) {
                        edits.add(removalEdit(statement, true, end(statement.getTree()), tokens, tokenPositions));
                    }
                }
            }
            for (TreeCursor statement : root.findChildrenByType(TreeType.APPLY_STATEMENT)) {
                TreeCursor target = statement.findChildrenByType(TreeType.SHAPE_ID).get(0);
                if (ids.contains(resolve(target.getTree().concatTokens()).withoutMember())) {
                    edits.add(removalEdit(statement, true, end(statement.getTree()), tokens, tokenPositions));
                }
            }
            for (TreeCursor statement : root.findChildrenByType(TreeType.USE_STATEMENT)) {
                TreeCursor target = statement.getFirstChild(TreeType.ABSOLUTE_ROOT_SHAPE_ID);
                if (ids.contains(ShapeId.from(target.getTree().concatTokens()))) {
                    // The use statement's trailing BR owns comments documenting the
                    // following declaration. Only erase the statement's own line.
                    edits.add(removalEdit(statement, false, end(target.getTree()), tokens, tokenPositions));
                }
            }
            return applyEdits(edits);
        }

        private ShapeId resolve(String value) {
            if (value.contains("#")) {
                return ShapeId.from(value);
            }
            String[] parts = value.split("\\$", 2);
            ShapeId id = imports.getOrDefault(parts[0], ShapeId.fromParts(namespace, parts[0]));
            return parts.length == 1 ? id : id.withMember(parts[1]);
        }

        private Edit removalEdit(
                TreeCursor statement,
                boolean removeDocs,
                int end,
                List<CapturedToken> tokens,
                Map<Integer, Integer> tokenPositions
        ) {
            int start = start(statement.getTree());
            if (removeDocs) {
                int first = tokenPositions.get(start);
                int previous = first - 1;
                while (previous >= 0 && isTrivia(tokens.get(previous))) {
                    previous--;
                }
                int previousLine = previous < 0 ? 0 : tokens.get(previous).getEndLine();
                for (int i = previous + 1; i < first; i++) {
                    CapturedToken token = tokens.get(i);
                    if (token.getIdlToken() == IdlToken.DOC_COMMENT && token.getStartLine() > previousLine) {
                        start = token.getPosition();
                        break;
                    }
                }
            }
            int lineStart = contents.lastIndexOf('\n', start - 1) + 1;
            if (contents.substring(lineStart, start).chars().allMatch(c -> c == ' ' || c == '\t')) {
                start = lineStart;
            }
            int tail = end;
            while (tail < contents.length() && (contents.charAt(tail) == ' ' || contents.charAt(tail) == '\t')) {
                tail++;
            }
            if (contents.startsWith("//", tail)) {
                while (tail < contents.length() && contents.charAt(tail) != '\n' && contents.charAt(tail) != '\r') {
                    tail++;
                }
            }
            if (tail == contents.length() || contents.charAt(tail) == '\r' || contents.charAt(tail) == '\n') {
                if (tail < contents.length() && contents.charAt(tail) == '\r') {
                    tail++;
                }
                if (tail < contents.length() && contents.charAt(tail) == '\n') {
                    tail++;
                }
                end = tail;
            }
            return new Edit(start, end, "");
        }

        private String applyEdits(List<Edit> edits) {
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

    private static boolean isTrivia(CapturedToken token) {
        return token.getIdlToken().isWhitespace()
                || token.getIdlToken() == IdlToken.COMMENT
                || token.getIdlToken() == IdlToken.DOC_COMMENT;
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
        private final Set<ShapeId> collections = new HashSet<>();

        private Change(MemberShape member) {
            this.member = member;
        }
    }
}
