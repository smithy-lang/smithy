/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.language.syntax;

import static software.amazon.smithy.rulesengine.language.RulesComponentBuilder.javaLocation;

import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class Identifier implements FromSourceLocation, ToNode {
    private final StringNode name;
    private final SourceLocation sourceLocation;

    private Identifier(StringNode name) {
        this.name = name;
        sourceLocation = name.getSourceLocation();
    }

    public static Identifier of(String name) {
        return new Identifier(new StringNode(name, javaLocation()));
    }

    public static Identifier of(StringNode name) {
        return new Identifier(name);
    }

    public StringNode getName() {
        return name;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        Identifier that = (Identifier) obj;
        return Objects.equals(this.name, that.name);
    }

    public String toString() {
        return name.getValue();
    }

    public String asString() {
        return name.getValue();
    }

    @Override
    public Node toNode() {
        return name;
    }
}
