/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.expressions.literal;

import java.util.List;
import java.util.Map;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;

/**
 * Literal visitor interface.
 *
 * @param <T> the return type of the visitor.
 */
public interface LiteralVisitor<T> {
    /**
     * Visits a boolean literal.
     *
     * @param b the boolean literal value.
     * @return the value from the visitor.
     */
    T visitBoolean(boolean b);

    /**
     * Visits a string literal.
     *
     * @param value the template value.
     * @return the value from the visitor.
     */
    T visitString(Template value);

    /**
     * Visits a record literal.
     *
     * @param members the map of keys to literal values.
     * @return the value from the visitor.
     */
    T visitRecord(Map<Identifier, Literal> members);

    /**
     * Visits a tuple literal.
     *
     * @param members the list of literal values.
     * @return the value from the visitor.
     */
    T visitTuple(List<Literal> members);

    /**
     * Visits a integer literal.
     *
     * @param value the value literal value.
     * @return the value from the visitor.
     */
    T visitInteger(int value);
}
