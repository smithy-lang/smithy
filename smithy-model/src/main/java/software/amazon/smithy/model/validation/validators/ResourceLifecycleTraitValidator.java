/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.ast.AndExpression;
import software.amazon.smithy.jmespath.ast.ComparatorExpression;
import software.amazon.smithy.jmespath.ast.CurrentExpression;
import software.amazon.smithy.jmespath.ast.ExpressionTypeExpression;
import software.amazon.smithy.jmespath.ast.FieldExpression;
import software.amazon.smithy.jmespath.ast.FilterProjectionExpression;
import software.amazon.smithy.jmespath.ast.FlattenExpression;
import software.amazon.smithy.jmespath.ast.FunctionExpression;
import software.amazon.smithy.jmespath.ast.IndexExpression;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.ast.MultiSelectHashExpression;
import software.amazon.smithy.jmespath.ast.MultiSelectListExpression;
import software.amazon.smithy.jmespath.ast.NotExpression;
import software.amazon.smithy.jmespath.ast.ObjectProjectionExpression;
import software.amazon.smithy.jmespath.ast.OrExpression;
import software.amazon.smithy.jmespath.ast.ProjectionExpression;
import software.amazon.smithy.jmespath.ast.SliceExpression;
import software.amazon.smithy.jmespath.ast.Subexpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.AbstractResourceLifecycleTrait;
import software.amazon.smithy.model.traits.CreatesResourcesTrait;
import software.amazon.smithy.model.traits.DeletesResourcesTrait;
import software.amazon.smithy.model.traits.PutsResourcesTrait;
import software.amazon.smithy.model.traits.ReadsResourcesTrait;
import software.amazon.smithy.model.traits.ResourceLifecycleBinding;
import software.amazon.smithy.model.traits.ResourceMemberBinding;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.UpdatesResourcesTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Validates the resource lifecycle traits: {@code @createsResources}, {@code @putsResources},
 * {@code @deletesResources}, {@code @readsResources}, and {@code @updatesResources}.
 *
 * <p>Each trait is a list of bindings. A binding names a resource and, for its identifiers
 * and properties, may be unspecified, explicit (a map of name to JMESPath locator), or
 * inferred (a {@code ...From} JMESPath pointing at a structure whose members are matched by
 * name). The side identifiers and properties resolve against is fixed per trait.
 */
@SmithyUnstableApi
public final class ResourceLifecycleTraitValidator extends AbstractValidator {

    private enum Side {
        INPUT, OUTPUT
    }

    private static final class Descriptor {
        final Class<? extends AbstractResourceLifecycleTrait> traitClass;
        final ShapeId traitId;
        final String name;
        final Side identifierSide;
        final Side propertySide; // null when the trait binds no properties (delete).

        Descriptor(
                Class<? extends AbstractResourceLifecycleTrait> traitClass,
                ShapeId traitId,
                String name,
                Side identifierSide,
                Side propertySide
        ) {
            this.traitClass = traitClass;
            this.traitId = traitId;
            this.name = name;
            this.identifierSide = identifierSide;
            this.propertySide = propertySide;
        }
    }

    private static final List<Descriptor> DESCRIPTORS = ListUtils.of(
            new Descriptor(CreatesResourcesTrait.class,
                    CreatesResourcesTrait.ID,
                    "createsResources",
                    Side.OUTPUT,
                    Side.INPUT),
            new Descriptor(PutsResourcesTrait.class,
                    PutsResourcesTrait.ID,
                    "putsResources",
                    Side.INPUT,
                    Side.INPUT),
            new Descriptor(ReadsResourcesTrait.class,
                    ReadsResourcesTrait.ID,
                    "readsResources",
                    Side.INPUT,
                    Side.OUTPUT),
            new Descriptor(UpdatesResourcesTrait.class,
                    UpdatesResourcesTrait.ID,
                    "updatesResources",
                    Side.INPUT,
                    Side.INPUT),
            new Descriptor(DeletesResourcesTrait.class,
                    DeletesResourcesTrait.ID,
                    "deletesResources",
                    Side.INPUT,
                    null));

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (Descriptor descriptor : DESCRIPTORS) {
            for (OperationShape operation : model.getOperationShapesWithTrait(descriptor.traitClass)) {
                AbstractResourceLifecycleTrait trait = operation.expectTrait(descriptor.traitClass);
                for (ResourceLifecycleBinding binding : trait.getBindings()) {
                    ResourceShape resource = model.getShape(binding.getResource())
                            .flatMap(Shape::asResourceShape)
                            .orElse(null);
                    // Missing shapes and non-resource shapes are reported by the `@idRef` trait on the
                    // binding's `resource` member (a critical validator that short-circuits this one),
                    // so when we get here the reference is a resource. Skip defensively otherwise.
                    if (resource != null) {
                        Context context = new Context(model, operation, trait, descriptor, resource, events);
                        validateBinding(context, binding);
                    }
                }
            }
        }
        return events;
    }

    // Holds the values shared by every step of validating one binding, so the per-step methods take
    // a single context plus the specific data they act on rather than a long parameter list.
    private final class Context {
        final Model model;
        final OperationShape operation;
        final Trait trait;
        final Descriptor descriptor;
        final ResourceShape resource;
        final List<ValidationEvent> events;

        Context(
                Model model,
                OperationShape operation,
                Trait trait,
                Descriptor descriptor,
                ResourceShape resource,
                List<ValidationEvent> events
        ) {
            this.model = model;
            this.operation = operation;
            this.trait = trait;
            this.descriptor = descriptor;
            this.resource = resource;
            this.events = events;
        }

        String traitName() {
            return descriptor.name;
        }

        ShapeId resourceId() {
            return resource.getId();
        }

        // The input or output structure a given binding kind resolves against, or null if absent.
        StructureShape sideStructure(Side side) {
            ShapeId id = side == Side.OUTPUT ? operation.getOutputShape() : operation.getInputShape();
            return model.getShape(id).flatMap(Shape::asStructureShape).orElse(null);
        }

        ShapeId sideStructureId(Side side) {
            return side == Side.OUTPUT ? operation.getOutputShape() : operation.getInputShape();
        }

        void error(String message) {
            events.add(ResourceLifecycleTraitValidator.this.error(operation, trait, message));
        }

        void danger(String message) {
            events.add(ResourceLifecycleTraitValidator.this.danger(operation, trait, message));
        }

        void warning(String message) {
            events.add(ResourceLifecycleTraitValidator.this.warning(operation, trait, message));
        }
    }

    private void validateBinding(Context ctx, ResourceLifecycleBinding binding) {
        Descriptor descriptor = ctx.descriptor;

        // Warn if the resource is already lifecycle-bound to this operation.
        if (isLifecycleBound(ctx.resource, ctx.operation.getId(), descriptor.traitId)) {
            ctx.warning(format(
                    "Resource `%s` in `@%s` is already bound to this operation via the resource's "
                            + "lifecycle. The trait is redundant.",
                    binding.getResource(),
                    descriptor.name));
        }

        validateBindingKind(ctx,
                ResourceLifecycleResolver.BindingKind.IDENTIFIER,
                descriptor.identifierSide,
                binding.getIdentifiers(),
                binding.getIdentifiersFrom());

        if (descriptor.propertySide != null) {
            validateBindingKind(ctx,
                    ResourceLifecycleResolver.BindingKind.PROPERTY,
                    descriptor.propertySide,
                    binding.getProperties(),
                    binding.getPropertiesFrom());
        } else if (!binding.getProperties().isEmpty() || binding.getPropertiesFrom().isPresent()) {
            // The trait binds no properties (delete): a resource is deleted by its identifier
            // alone. Carrying `properties`/`propertiesFrom` is meaningless, so reject it. The
            // prelude models this member with a properties-free structure, which also flags it at
            // load time; this check makes the violation a hard error even if reached in Java.
            ctx.error(format(
                    "Binding for resource `%s` in `@%s` specifies properties, but `@%s` deletes a resource "
                            + "by its identifiers alone and has no properties.",
                    binding.getResource(),
                    descriptor.name,
                    descriptor.name));
        }
    }

    private void validateBindingKind(
            Context ctx,
            ResourceLifecycleResolver.BindingKind kind,
            Side side,
            Map<String, ResourceMemberBinding> explicit,
            Optional<String> from
    ) {
        // Unspecified: nothing to validate.
        if (explicit.isEmpty() && !from.isPresent()) {
            return;
        }

        StructureShape sideStructure = ctx.sideStructure(side);
        ShapeId sideStructureId = ctx.sideStructureId(side);
        String kindWord = kindWord(kind);
        Set<String> validNames = kind == ResourceLifecycleResolver.BindingKind.IDENTIFIER
                ? ctx.resource.getIdentifiers().keySet()
                : ctx.resource.getProperties().keySet();

        // Explicit map.
        for (Map.Entry<String, ResourceMemberBinding> entry : explicit.entrySet()) {
            String name = entry.getKey();
            String path = entry.getValue().getPath();

            if (!validNames.contains(name)) {
                ctx.error(format("%s `%s` in `@%s` does not match any %s of resource `%s`. Valid %ss: [%s]",
                        capitalize(kindWord),
                        name,
                        ctx.traitName(),
                        kindWord,
                        ctx.resourceId(),
                        kindWord,
                        ValidationUtils.tickedList(validNames)));
                continue;
            }

            JmespathExpression parsed = parseAndCheck(ctx, kindWord, name, path);
            if (parsed == null) {
                continue;
            }

            if (sideStructure != null) {
                ResourceLifecycleResolver.PathResult result =
                        ResourceLifecycleResolver.walk(ctx.model, sideStructure, parsed);
                if (result.error != null) {
                    ctx.danger(format("JMESPath expression `%s` for %s `%s` in `@%s` has problems when resolved "
                            + "against `%s`: %s",
                            path,
                            kindWord,
                            name,
                            ctx.traitName(),
                            sideStructureId,
                            result.error));
                } else {
                    checkLeafType(ctx, kind, kindWord, name, path, result.leaf);
                }
            }
        }

        if (kind == ResourceLifecycleResolver.BindingKind.IDENTIFIER) {
            validateCompositeCardinality(ctx, sideStructure, explicit);
        }

        // Inferred via `...From`.
        from.ifPresent(fromPath -> validateInferred(ctx, kind, kindWord, sideStructure, sideStructureId, fromPath));
    }

    private void validateInferred(
            Context ctx,
            ResourceLifecycleResolver.BindingKind kind,
            String kindWord,
            StructureShape sideStructure,
            ShapeId sideStructureId,
            String fromPath
    ) {
        String fromMember = kind == ResourceLifecycleResolver.BindingKind.IDENTIFIER
                ? "identifiersFrom"
                : "propertiesFrom";
        JmespathExpression parsed = parseAndCheck(ctx, fromMember, fromMember, fromPath);
        if (parsed == null || sideStructure == null) {
            return;
        }

        ResourceLifecycleResolver.PathResult result = ResourceLifecycleResolver.walk(ctx.model, sideStructure, parsed);
        if (result.error != null) {
            ctx.danger(format("`%s` `%s` in `@%s` has problems when resolved against `%s`: %s",
                    fromMember,
                    fromPath,
                    ctx.traitName(),
                    sideStructureId,
                    result.error));
            return;
        }
        if (result.root) {
            ctx.error(format("`%s` `%s` in `@%s` points at the whole %s root; it must point at a nested "
                    + "structure or a projection.",
                    fromMember,
                    fromPath,
                    ctx.traitName(),
                    sideName(sideStructureId, ctx.operation)));
            return;
        }
        if (!(result.leaf instanceof StructureShape)) {
            ctx.error(format("`%s` `%s` in `@%s` must point at a nested structure or a projection of "
                    + "structures, but resolved to `%s`.",
                    fromMember,
                    fromPath,
                    ctx.traitName(),
                    result.leaf.getId()));
            return;
        }

        StructureShape element = (StructureShape) result.leaf;
        ResourceLifecycleResolver.InferenceResult inference =
                ResourceLifecycleResolver.inferByName(ctx.resource, element, kind);

        if (inference.matched.isEmpty()) {
            ctx.error(format("`%s` `%s` in `@%s` resolves to `%s`, whose members match no %s of resource `%s`.",
                    fromMember,
                    fromPath,
                    ctx.traitName(),
                    element.getId(),
                    kindWord,
                    ctx.resourceId()));
            return;
        }

        if (!inference.unmatched.isEmpty()) {
            ctx.error(format("`%s` `%s` in `@%s` resolves to `%s`, which has members that are neither a %s nor an "
                    + "identifier of resource `%s` and are not marked `@notProperty`: [%s]",
                    fromMember,
                    fromPath,
                    ctx.traitName(),
                    element.getId(),
                    kindWord,
                    ctx.resourceId(),
                    memberNames(inference.unmatched)));
        }

        // Type agreement for matched members.
        for (Map.Entry<String, MemberShape> matched : inference.matched.entrySet()) {
            checkMemberType(ctx, kind, matched.getKey(), matched.getValue());
        }
    }

    private void checkLeafType(
            Context ctx,
            ResourceLifecycleResolver.BindingKind kind,
            String kindWord,
            String name,
            String path,
            Shape leaf
    ) {
        if (kind == ResourceLifecycleResolver.BindingKind.IDENTIFIER) {
            ShapeId declared = unwrapBaseId(ctx.model, ctx.resource.getIdentifiers().get(name));
            if (!leaf.getId().equals(declared)) {
                ctx.error(format("JMESPath expression `%s` for identifier `%s` in `@%s` resolves to `%s`, but the "
                        + "resource identifier targets `%s`.",
                        path,
                        name,
                        ctx.traitName(),
                        leaf.getId(),
                        declared));
            }
        } else {
            ShapeId declared = unwrapBaseId(ctx.model, ctx.resource.getProperties().get(name));
            if (!leaf.getId().equals(declared)) {
                ctx.error(format("JMESPath expression `%s` for property `%s` in `@%s` resolves to `%s`, but the "
                        + "resource property targets `%s`.",
                        path,
                        name,
                        ctx.traitName(),
                        leaf.getId(),
                        declared));
            }
        }
    }

    private void checkMemberType(
            Context ctx,
            ResourceLifecycleResolver.BindingKind kind,
            String name,
            MemberShape member
    ) {
        ShapeId leaf = unwrapBaseId(ctx.model, member.getTarget());
        if (kind == ResourceLifecycleResolver.BindingKind.IDENTIFIER) {
            ShapeId declared = unwrapBaseId(ctx.model, ctx.resource.getIdentifiers().get(name));
            if (!leaf.equals(declared)) {
                ctx.error(format("Inferred identifier `%s` in `@%s` (member `%s`) resolves to `%s`, but the resource "
                        + "identifier targets `%s`.",
                        name,
                        ctx.traitName(),
                        member.getMemberName(),
                        leaf,
                        declared));
            }
        } else {
            ShapeId declared = unwrapBaseId(ctx.model, ctx.resource.getProperties().get(name));
            if (!leaf.equals(declared)) {
                ctx.error(format("Inferred property `%s` in `@%s` (member `%s`) resolves to `%s`, but the resource "
                        + "property targets `%s`.",
                        name,
                        ctx.traitName(),
                        member.getMemberName(),
                        leaf,
                        declared));
            }
        }
    }

    private JmespathExpression parseAndCheck(Context ctx, String kind, String name, String path) {
        JmespathExpression parsed;
        try {
            parsed = JmespathExpression.parse(path);
        } catch (JmespathException e) {
            ctx.error(format("Invalid JMESPath expression `%s` for %s `%s` in `@%s`: %s",
                    path,
                    kind,
                    name,
                    ctx.traitName(),
                    e.getMessage()));
            return null;
        }

        List<String> unsupported = parsed.accept(new UnsupportedJmesPathVisitor());
        if (!unsupported.isEmpty()) {
            StringBuilder list = new StringBuilder();
            for (String expr : unsupported) {
                if (list.length() > 0) {
                    list.append(", ");
                }
                list.append("'").append(expr).append("'");
            }
            ctx.error(format("JMESPath expression `%s` for %s `%s` in `@%s` contains unsupported expressions: %s",
                    path,
                    kind,
                    name,
                    ctx.traitName(),
                    list));
            return null;
        }
        return parsed;
    }

    private static String kindWord(ResourceLifecycleResolver.BindingKind kind) {
        return kind == ResourceLifecycleResolver.BindingKind.IDENTIFIER ? "identifier" : "property";
    }

    private static String sideName(ShapeId sideStructureId, OperationShape operation) {
        return sideStructureId.equals(operation.getOutputShape()) ? "output" : "input";
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // Ticks member names in definition order (not sorted): the source order is meaningful and
    // easier to scan against the model than an alphabetized list.
    private static String memberNames(List<MemberShape> members) {
        StringBuilder sb = new StringBuilder();
        for (MemberShape m : members) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("`").append(m.getMemberName()).append("`");
        }
        return sb.toString();
    }

    private static ShapeId unwrapBaseId(Model model, ShapeId id) {
        Shape shape = model.expectShape(id);
        while (shape instanceof ListShape) {
            shape = model.expectShape(((ListShape) shape).getMember().getTarget());
        }
        return shape.getId();
    }

    private boolean isLifecycleBound(ResourceShape resource, ShapeId operationId, ShapeId traitId) {
        Optional<ShapeId> lifecycleOperation;
        if (traitId.equals(CreatesResourcesTrait.ID)) {
            lifecycleOperation = resource.getCreate();
        } else if (traitId.equals(PutsResourcesTrait.ID)) {
            lifecycleOperation = resource.getPut();
        } else if (traitId.equals(ReadsResourcesTrait.ID)) {
            lifecycleOperation = resource.getRead();
        } else if (traitId.equals(UpdatesResourcesTrait.ID)) {
            lifecycleOperation = resource.getUpdate();
        } else if (traitId.equals(DeletesResourcesTrait.ID)) {
            lifecycleOperation = resource.getDelete();
        } else {
            return false;
        }
        return lifecycleOperation.map(operationId::equals).orElse(false);
    }

    private void validateCompositeCardinality(
            Context ctx,
            StructureShape sideStructure,
            Map<String, ResourceMemberBinding> identifiers
    ) {
        if (sideStructure == null) {
            return;
        }

        // Resolve each identifier's path to its cardinality signature: the ordered identities of the
        // lists it iterates. Two identifiers correlate element-for-element only if their signatures
        // are equal. Scalars (empty signature) broadcast and are not compared.
        Map<String, List<ShapeId>> identifierToArrays = new LinkedHashMap<>();
        for (Map.Entry<String, ResourceMemberBinding> entry : identifiers.entrySet()) {
            JmespathExpression parsed = parseQuietly(entry.getValue().getPath());
            if (parsed == null) {
                continue;
            }
            ResourceLifecycleResolver.PathResult result =
                    ResourceLifecycleResolver.walk(ctx.model, sideStructure, parsed);
            if (result.error == null && !result.arrays.isEmpty()) {
                identifierToArrays.put(entry.getKey(), result.arrays);
            }
        }

        if (identifierToArrays.size() < 2) {
            return;
        }

        Set<List<ShapeId>> distinct = new LinkedHashSet<>(identifierToArrays.values());
        if (distinct.size() > 1) {
            StringBuilder detail = new StringBuilder();
            for (Map.Entry<String, List<ShapeId>> entry : identifierToArrays.entrySet()) {
                if (detail.length() > 0) {
                    detail.append(", ");
                }
                detail.append("`").append(entry.getKey()).append("` -> ").append(signature(entry.getValue()));
            }
            ctx.error(format(
                    "Identifiers in `@%s` for resource `%s` project through different lists, "
                            + "making cardinality ambiguous. Identifiers and the lists they iterate: %s",
                    ctx.traitName(),
                    ctx.resourceId(),
                    detail));
        }
    }

    // Renders a cardinality signature as a chain of ticked list shape names, for example `[Foo, Bar]`.
    private static String signature(List<ShapeId> arrays) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arrays.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("`").append(arrays.get(i)).append("`");
        }
        return sb.append("]").toString();
    }

    // Parses a path, returning null when it is invalid or uses unsupported expressions. Such paths
    // are reported elsewhere; here we simply skip them when computing cardinality.
    private JmespathExpression parseQuietly(String path) {
        JmespathExpression parsed;
        try {
            parsed = JmespathExpression.parse(path);
        } catch (JmespathException e) {
            return null;
        }
        return parsed.accept(new UnsupportedJmesPathVisitor()).isEmpty() ? parsed : null;
    }

    private static final class UnsupportedJmesPathVisitor implements ExpressionVisitor<List<String>> {

        @Override
        public List<String> visitComparator(ComparatorExpression expression) {
            return ListUtils.of("comparator");
        }

        @Override
        public List<String> visitCurrentNode(CurrentExpression expression) {
            return Collections.emptyList();
        }

        @Override
        public List<String> visitExpressionType(ExpressionTypeExpression expression) {
            return expression.getExpression().accept(this);
        }

        @Override
        public List<String> visitFlatten(FlattenExpression expression) {
            return expression.getExpression().accept(this);
        }

        @Override
        public List<String> visitFunction(FunctionExpression expression) {
            return ListUtils.of("`" + expression.getName() + "` function");
        }

        @Override
        public List<String> visitField(FieldExpression expression) {
            return Collections.emptyList();
        }

        @Override
        public List<String> visitIndex(IndexExpression expression) {
            return ListUtils.of("index");
        }

        @Override
        public List<String> visitLiteral(LiteralExpression expression) {
            return ListUtils.of("literal");
        }

        @Override
        public List<String> visitMultiSelectList(MultiSelectListExpression expression) {
            return ListUtils.of("multiselect list");
        }

        @Override
        public List<String> visitMultiSelectHash(MultiSelectHashExpression expression) {
            return ListUtils.of("multiselect hash");
        }

        @Override
        public List<String> visitAnd(AndExpression expression) {
            return ListUtils.of("and");
        }

        @Override
        public List<String> visitOr(OrExpression expression) {
            return ListUtils.of("or");
        }

        @Override
        public List<String> visitNot(NotExpression expression) {
            return ListUtils.of("not");
        }

        @Override
        public List<String> visitProjection(ProjectionExpression expression) {
            List<String> unsupported = new ArrayList<>();
            unsupported.addAll(expression.getLeft().accept(this));
            unsupported.addAll(expression.getRight().accept(this));
            return Collections.unmodifiableList(unsupported);
        }

        @Override
        public List<String> visitFilterProjection(FilterProjectionExpression expression) {
            return ListUtils.of("filter projection");
        }

        @Override
        public List<String> visitObjectProjection(ObjectProjectionExpression expression) {
            List<String> unsupported = new ArrayList<>();
            unsupported.addAll(expression.getLeft().accept(this));
            unsupported.addAll(expression.getRight().accept(this));
            return Collections.unmodifiableList(unsupported);
        }

        @Override
        public List<String> visitSlice(SliceExpression expression) {
            return ListUtils.of("slice");
        }

        @Override
        public List<String> visitSubexpression(Subexpression expression) {
            List<String> unsupported = new ArrayList<>();
            unsupported.addAll(expression.getLeft().accept(this));
            unsupported.addAll(expression.getRight().accept(this));
            return Collections.unmodifiableList(unsupported);
        }
    }
}
