/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
