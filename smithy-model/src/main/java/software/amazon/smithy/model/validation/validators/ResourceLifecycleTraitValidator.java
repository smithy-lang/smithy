/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.AbstractResourceLifecycleTrait;
import software.amazon.smithy.model.traits.ResourceBinding;
import software.amazon.smithy.model.traits.ResourceLifecycleBinding;
import software.amazon.smithy.model.traits.ResourceMemberBinding;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.model.validation.validators.LifecycleTraitDescriptor.Side;
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

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (LifecycleTraitDescriptor descriptor : LifecycleTraitDescriptor.ALL) {
            for (OperationShape operation : model.getOperationShapesWithTrait(descriptor.traitClass)) {
                AbstractResourceLifecycleTrait<?> trait = operation.expectTrait(descriptor.traitClass);
                for (ResourceBinding binding : trait.getBindings()) {
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
        final LifecycleTraitDescriptor descriptor;
        final ResourceShape resource;
        final List<ValidationEvent> events;

        Context(
                Model model,
                OperationShape operation,
                Trait trait,
                LifecycleTraitDescriptor descriptor,
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
            return model.getShape(side.of(operation)).flatMap(Shape::asStructureShape).orElse(null);
        }

        ShapeId sideStructureId(Side side) {
            return side.of(operation);
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

    private void validateBinding(Context ctx, ResourceBinding binding) {
        LifecycleTraitDescriptor descriptor = ctx.descriptor;

        // Warn if the resource is already lifecycle-bound to this operation.
        if (descriptor.isLifecycleBound(ctx.resource, ctx.operation.getId())) {
            ctx.warning(format(
                    "Resource `%s` in `@%s` is already bound to this operation via the resource's "
                            + "lifecycle. The trait is redundant.",
                    binding.getResource(),
                    descriptor.name));
        }

        validateBindingKind(ctx,
                ResourceMemberInference.BindingKind.IDENTIFIER,
                descriptor.identifierSide,
                binding.getIdentifiers(),
                binding.getIdentifiersFrom());

        // A properties side is only defined for the non-delete traits, whose bindings are always
        // ResourceLifecycleBinding. Delete uses ResourceDeletionBinding, which has no properties at
        // all, so there is nothing to validate here.
        if (descriptor.propertySide != null) {
            ResourceLifecycleBinding lifecycleBinding = (ResourceLifecycleBinding) binding;
            validateBindingKind(ctx,
                    ResourceMemberInference.BindingKind.PROPERTY,
                    descriptor.propertySide,
                    lifecycleBinding.getProperties(),
                    lifecycleBinding.getPropertiesFrom());
        }
    }

    private void validateBindingKind(
            Context ctx,
            ResourceMemberInference.BindingKind kind,
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
        Set<String> validNames = kind == ResourceMemberInference.BindingKind.IDENTIFIER
                ? ctx.resource.getIdentifiers().keySet()
                : ctx.resource.getProperties().keySet();

        // Explicit map.
        for (Map.Entry<String, ResourceMemberBinding> entry : explicit.entrySet()) {
            String name = entry.getKey();
            String path = entry.getValue().getPath();

            if (!validNames.contains(name)) {
                ctx.error(format("%s `%s` in `@%s` does not match any %s of resource `%s`. Valid %s: [%s]",
                        capitalize(kindWord),
                        name,
                        ctx.traitName(),
                        kindWord,
                        ctx.resourceId(),
                        kindPlural(kind),
                        ValidationUtils.tickedList(validNames)));
                continue;
            }

            JmespathExpression parsed = parseAndCheck(ctx, format("%s `%s`", kindWord, name), path);
            if (parsed == null) {
                continue;
            }

            if (sideStructure != null) {
                JmespathPathResult result =
                        JmespathShapeWalker.walk(ctx.model, sideStructure, parsed);
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

        if (kind == ResourceMemberInference.BindingKind.IDENTIFIER) {
            validateCompositeCardinality(ctx, sideStructure, explicit);
        }

        // Inferred via `...From`.
        from.ifPresent(fromPath -> validateInferred(ctx, kind, kindWord, sideStructure, sideStructureId, fromPath));
    }

    private void validateInferred(
            Context ctx,
            ResourceMemberInference.BindingKind kind,
            String kindWord,
            StructureShape sideStructure,
            ShapeId sideStructureId,
            String fromPath
    ) {
        String fromMember = kind == ResourceMemberInference.BindingKind.IDENTIFIER
                ? "identifiersFrom"
                : "propertiesFrom";
        JmespathExpression parsed = parseAndCheck(ctx, format("`%s`", fromMember), fromPath);
        if (parsed == null || sideStructure == null) {
            return;
        }

        JmespathPathResult result = JmespathShapeWalker.walk(ctx.model, sideStructure, parsed);
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
        MemberInferenceResult inference =
                ResourceMemberInference.inferByName(ctx.model, ctx.resource, element, kind);

        String location = format("`%s` `%s` in `@%s` resolves to `%s`",
                fromMember,
                fromPath,
                ctx.traitName(),
                element.getId());
        reportPropertyConflicts(ctx, location, inference.propertyConflicts);

        boolean conflicts = kind == ResourceMemberInference.BindingKind.PROPERTY
                && !inference.propertyConflicts.isEmpty();
        if (inference.matched.isEmpty() && !conflicts) {
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
            ctx.error(format("`%s` `%s` in `@%s` resolves to `%s`, which has members that are neither an identifier "
                    + "nor a property of resource `%s` and are not marked `@notProperty`: [%s]",
                    fromMember,
                    fromPath,
                    ctx.traitName(),
                    element.getId(),
                    ctx.resourceId(),
                    memberNames(inference.unmatched)));
        }

        // Type agreement for matched members, and for members bound to the other kind, which are
        // not inferred here but must still target that identifier's or property's shape.
        for (Map.Entry<String, MemberShape> matched : inference.matched.entrySet()) {
            checkMemberType(ctx, kind, matched.getKey(), matched.getValue());
        }
        ResourceMemberInference.BindingKind otherKind = ResourceMemberInference.otherKind(kind);
        for (Map.Entry<String, MemberShape> other : inference.otherKind.entrySet()) {
            checkMemberType(ctx, otherKind, other.getKey(), other.getValue());
        }
    }

    // Reports properties that several members of an inferred structure resolve to.
    private void reportPropertyConflicts(Context ctx, String location, Map<String, List<MemberShape>> conflicts) {
        for (Map.Entry<String, List<MemberShape>> entry : conflicts.entrySet()) {
            ctx.error(format("%s, which has several members bound to property `%s`: [%s]",
                    location,
                    entry.getKey(),
                    memberNames(entry.getValue())));
        }
    }

    private void checkLeafType(
            Context ctx,
            ResourceMemberInference.BindingKind kind,
            String kindWord,
            String name,
            String path,
            Shape leaf
    ) {
        if (kind == ResourceMemberInference.BindingKind.IDENTIFIER) {
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
            ResourceMemberInference.BindingKind kind,
            String name,
            MemberShape member
    ) {
        ShapeId leaf = unwrapBaseId(ctx.model, member.getTarget());
        if (kind == ResourceMemberInference.BindingKind.IDENTIFIER) {
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

    // Parses a path and reports it when invalid or unsupported. The subject names what the path is for,
    // such as "property `size`" or "`propertiesFrom`".
    private JmespathExpression parseAndCheck(Context ctx, String subject, String path) {
        JmespathExpression parsed;
        try {
            parsed = JmespathExpression.parse(path);
        } catch (JmespathException e) {
            ctx.error(format("Invalid JMESPath expression `%s` for %s in `@%s`: %s",
                    path,
                    subject,
                    ctx.traitName(),
                    e.getMessage()));
            return null;
        }

        List<String> unsupported = LifecycleTraitJmespath.unsupportedExpressions(parsed);
        if (!unsupported.isEmpty()) {
            StringBuilder list = new StringBuilder();
            for (String expr : unsupported) {
                if (list.length() > 0) {
                    list.append(", ");
                }
                list.append("'").append(expr).append("'");
            }
            ctx.error(format("JMESPath expression `%s` for %s in `@%s` contains unsupported expressions: %s",
                    path,
                    subject,
                    ctx.traitName(),
                    list));
            return null;
        }
        return parsed;
    }

    private static String kindWord(ResourceMemberInference.BindingKind kind) {
        return kind == ResourceMemberInference.BindingKind.IDENTIFIER ? "identifier" : "property";
    }

    private static String kindPlural(ResourceMemberInference.BindingKind kind) {
        return kind == ResourceMemberInference.BindingKind.IDENTIFIER ? "identifiers" : "properties";
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
            JmespathExpression parsed = LifecycleTraitJmespath.parseSupported(entry.getValue().getPath());
            if (parsed == null) {
                continue;
            }
            JmespathPathResult result =
                    JmespathShapeWalker.walk(ctx.model, sideStructure, parsed);
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
}
