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

package software.amazon.smithy.protocoltests.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;

import java.util.Map;
import java.util.Optional;

/**
 * Defines a test case for an HTTP request.
 */
public abstract class FailingHttpRequestTest implements ToNode  {
    protected HttpRequestTestCase inner;
    protected FailureCause failureCause;

    public static FailingHttpRequestTest fromNode(Node node) {
        HttpRequestTestCase inner = HttpRequestTestCase.fromNode(node);
        FailureCause failureCause = FailureCause.fromNode(node.expectObjectNode().expectMember("failureCause"));
        AppliesTo appliesTo = inner.getAppliesTo().orElseThrow(() -> new RuntimeException("For failing test cases, applies to must be defined"));
        if (appliesTo == AppliesTo.CLIENT) {
            return new Client(inner, failureCause);
        } else {
            return new Server(inner, failureCause);
        }
    }

    @Override
    public Node toNode() {
        return inner.toNode();
    }

    public FailureCause getFailureCause() {
        return failureCause;
    }

    static class Client extends FailingHttpRequestTest {
        private Client(HttpRequestTestCase inner, FailureCause failureCause) {
            this.inner = inner;
            this.failureCause = failureCause;
        }

        public ObjectNode getParams() {
            return this.inner.getParams();
        }
    }

    static class Server extends FailingHttpRequestTest {
        Server(HttpRequestTestCase inner, FailureCause failureCause) {
            this.inner = inner;
            this.failureCause = failureCause;
            if (!inner.getParams().isEmpty()) {
                throw new RuntimeException("Params for server test must be null");
            }

            if (getMethod() == null || getUri() == null || getHeaders() == null) {
                throw new RuntimeException("Server tests must specify a full HTTP request");
            }
        }

        public String getMethod() {
            return inner.getMethod();
        }

        public String getUri() {
            return inner.getUri();
        }

        public Optional<String> getBody() {
            return inner.getBody();
        }

        public Map<String, String> getHeaders() {
            return inner.getHeaders();
        }
    }
}
