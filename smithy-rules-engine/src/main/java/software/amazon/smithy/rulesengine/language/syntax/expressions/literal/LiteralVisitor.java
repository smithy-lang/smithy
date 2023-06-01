/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions.literal;

import java.util.List;
import java.util.Map;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;

public interface LiteralVisitor<T> {
    T visitBoolean(boolean b);

    T visitString(Template value);

    T visitRecord(Map<Identifier, Literal> members);

    T visitTuple(List<Literal> members);

    T visitInteger(int value);
}
