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

package software.amazon.smithy.codegen.freemarker;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.Version;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

final class SmithyObjectWrapper extends DefaultObjectWrapper {
    SmithyObjectWrapper(Version incompatibleImprovements) {
        super(incompatibleImprovements);
    }

    @Override
    protected TemplateModel handleUnknownType(Object obj) throws TemplateModelException {
        if (obj instanceof Optional) {
            Optional optional = (Optional) obj;
            return optional.isPresent() ? wrap(optional.get()) : null;
        } else if (obj instanceof StringNode) {
            return wrap(((StringNode) obj).getValue());
        } else if (obj instanceof BooleanNode) {
            return wrap(((BooleanNode) obj).getValue());
        } else if (obj instanceof NullNode) {
            return wrap(null);
        } else if (obj instanceof NumberNode) {
            return wrap(((NumberNode) obj).getValue());
        } else if (obj instanceof ArrayNode) {
            return wrap(((ArrayNode) obj).getElements());
        } else if (obj instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) obj;
            Map<String, Node> map = new HashMap<>(objectNode.size());
            objectNode.getMembers().forEach((k, v) -> map.put(k.getValue(), v));
            return wrap(map);
        } else if (obj instanceof Stream) {
            return wrap(((Stream) obj).iterator());
        }

        return super.handleUnknownType(obj);
    }
}
