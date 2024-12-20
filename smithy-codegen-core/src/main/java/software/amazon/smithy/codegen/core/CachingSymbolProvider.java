/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Caches the results of calling {@code toSymbol} and {@code toMemberName}.
 */
final class CachingSymbolProvider implements SymbolProvider {

    private final SymbolProvider delegate;
    private final ConcurrentMap<ShapeId, Symbol> symbolCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<ShapeId, String> memberCache = new ConcurrentHashMap<>();

    CachingSymbolProvider(SymbolProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public Symbol toSymbol(Shape shape) {
        return symbolCache.computeIfAbsent(shape.toShapeId(), id -> delegate.toSymbol(shape));
    }

    @Override
    public String toMemberName(MemberShape shape) {
        return memberCache.computeIfAbsent(shape.toShapeId(), id -> delegate.toMemberName(shape));
    }
}
