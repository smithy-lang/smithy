/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.BottomUpIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.traits.RetryableTrait;

/** Derives machine-readable operation safety signals for dry-run plans. */
final class CallSafety {
    private CallSafety() {}

    static Document describe(OperationShape operation, Model model, ShapeId serviceId) {
        String lifecycle = lifecycleRole(operation, model, serviceId);
        boolean readonlyTrait = operation.hasTrait(ReadonlyTrait.class);
        boolean idempotentTrait = operation.hasTrait(IdempotentTrait.class);
        boolean hasIdempotencyToken = inputHasIdempotencyToken(operation, model);
        boolean retryable = operation.hasTrait(RetryableTrait.class);
        String name = operation.getId().getName().toLowerCase(Locale.ROOT);

        boolean readonly;
        String basis;
        if (lifecycle != null) {
            readonly = lifecycle.equals("read") || lifecycle.equals("list");
            basis = "resource";
        } else if (readonlyTrait) {
            readonly = true;
            basis = "trait";
        } else {
            readonly = nameLooksReadonly(name);
            basis = "heuristic";
        }

        boolean destructive = "delete".equals(lifecycle)
                || (!readonly && (name.startsWith("delete") || name.startsWith("remove")
                        || name.startsWith("terminate")
                        || name.startsWith("purge")
                        || name.startsWith("destroy")));

        Boolean idempotent;
        if (idempotentTrait || hasIdempotencyToken || readonly) {
            idempotent = true;
        } else if (lifecycle != null) {
            idempotent = !lifecycle.equals("create");
        } else {
            idempotent = null;
        }

        Map<String, Document> safety = new LinkedHashMap<>();
        safety.put("readonly", Document.of(readonly));
        safety.put("mutating", Document.of(!readonly));
        safety.put("destructive", Document.of(destructive));
        safety.put("idempotent", idempotent == null ? Document.ofObject(null) : Document.of(idempotent));
        safety.put("idempotencyToken", Document.of(hasIdempotencyToken));
        safety.put("retryable", Document.of(retryable));
        if (lifecycle != null) {
            safety.put("lifecycle", Document.of(lifecycle));
        }
        safety.put("basis", Document.of(basis));
        return Document.of(safety);
    }

    private static boolean nameLooksReadonly(String name) {
        return name.startsWith("describe") || name.startsWith("list")
                || name.startsWith("get")
                || name.startsWith("batchget")
                || name.startsWith("query")
                || name.startsWith("scan")
                || name.startsWith("search")
                || name.startsWith("head")
                || name.startsWith("lookup");
    }

    private static String lifecycleRole(OperationShape operation, Model model, ShapeId serviceId) {
        var binding = BottomUpIndex.of(model).getResourceBinding(serviceId, operation);
        if (binding.isEmpty()) {
            return null;
        }
        var resource = binding.get();
        ShapeId id = operation.getId();
        if (resource.getRead().filter(id::equals).isPresent())
            return "read";
        if (resource.getList().filter(id::equals).isPresent())
            return "list";
        if (resource.getCreate().filter(id::equals).isPresent())
            return "create";
        if (resource.getPut().filter(id::equals).isPresent())
            return "put";
        if (resource.getUpdate().filter(id::equals).isPresent())
            return "update";
        if (resource.getDelete().filter(id::equals).isPresent())
            return "delete";
        return null;
    }

    private static boolean inputHasIdempotencyToken(OperationShape operation, Model model) {
        Shape input = model.getShape(operation.getInputShape()).orElse(null);
        if (input == null) {
            return false;
        }
        return input.getAllMembers().values().stream().anyMatch(m -> m.hasTrait(IdempotencyTokenTrait.class));
    }
}
