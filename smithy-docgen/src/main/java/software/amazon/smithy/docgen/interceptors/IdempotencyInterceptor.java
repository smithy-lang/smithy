/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen.interceptors;

import java.util.Optional;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.docgen.sections.ShapeDetailsSection;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.docgen.writers.DocWriter.NoticeType;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Provides information about idempotency depending on a number of traits.
 */
@SmithyInternalApi
public final class IdempotencyInterceptor implements CodeInterceptor<ShapeDetailsSection, DocWriter> {
    private static final Pair<String, String> IDEMPOTENT_REF = Pair.of(
            "idempotent",
            "https://datatracker.ietf.org/doc/html/rfc7231.html#section-4.2.2");
    private static final Pair<String, String> UUID_REF = Pair.of(
            "UUID",
            "https://tools.ietf.org/html/rfc4122.html");

    @Override
    public Class<ShapeDetailsSection> sectionType() {
        return ShapeDetailsSection.class;
    }

    @Override
    public boolean isIntercepted(ShapeDetailsSection section) {
        var shape = section.shape();
        var model = section.context().model();
        var operationIndex = OperationIndex.of(model);

        if (shape.hasTrait(IdempotencyTokenTrait.class)
                && operationIndex.isInputStructure(shape.asMemberShape().get().getContainer())) {
            return true;
        }

        var target = shape.isMemberShape()
                ? model.expectShape(shape.asMemberShape().get().getTarget())
                : shape;

        if (!target.isOperationShape()) {
            return false;
        }

        return shape.getMemberTrait(model, IdempotentTrait.class).isPresent()
                || shape.getMemberTrait(model, ReadonlyTrait.class).isPresent()
                || getIdempotencyToken(model, target.asOperationShape().get()).isPresent();
    }

    private Optional<MemberShape> getIdempotencyToken(Model model, OperationShape operation) {
        var input = model.expectShape(operation.getInputShape());
        for (var member : input.members()) {
            if (member.hasTrait(IdempotencyTokenTrait.class)) {
                return Optional.of(member);
            }
        }
        return Optional.empty();
    }

    @Override
    public void write(DocWriter writer, String previousText, ShapeDetailsSection section) {
        if (section.shape().isMemberShape()) {
            writer.openAdmonition(NoticeType.NOTE);
            writer.write("""
                    This value will be used by the service to ensure the request is $R. \
                    Clients SHOULD automatically populate this (typically with a $R) if \
                    it was not explicitly set.

                    """, IDEMPOTENT_REF, UUID_REF);
            writer.closeAdmonition();
            writer.writeWithNoFormatting(previousText);
            return;
        }

        var operation = section.shape().asOperationShape().get();
        var idempotencyToken = getIdempotencyToken(section.context().model(), operation)
                .map(member -> section.context().symbolProvider().toSymbol(member))
                .map(symbol -> SymbolReference.builder()
                        .alias(String.format("idempotency token (%s)", symbol.getName()))
                        .symbol(symbol)
                        .build());
        writer.putContext("token", idempotencyToken);
        writer.openAdmonition(NoticeType.NOTE);
        writer.write("""
                This operation is $R${?token} when the ${token:R} is set${/token}.

                """, IDEMPOTENT_REF);
        writer.closeAdmonition();
        writer.writeWithNoFormatting(previousText);
    }
}
