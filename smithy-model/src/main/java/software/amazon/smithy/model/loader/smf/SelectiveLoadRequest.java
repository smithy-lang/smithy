/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader.smf;

import java.util.Set;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Describes what to selectively load from an SMF file.
 *
 * <p>For the dynamic client use case: load a service shape (with only its
 * common errors expanded) and specific operations (with their full transitive
 * closure).
 */
@SmithyUnstableApi
public final class SelectiveLoadRequest {

    private final ShapeId service;
    private final Set<ShapeId> operations;
    private final boolean verifyCrc;
    private final ClassLoader classLoader;

    private SelectiveLoadRequest(Builder builder) {
        this.service = builder.service;
        this.operations = builder.operations.copy();
        this.verifyCrc = builder.verifyCrc;
        this.classLoader = builder.classLoader;
    }

    public ShapeId getService() {
        return service;
    }

    public Set<ShapeId> getOperations() {
        return operations;
    }

    public boolean getVerifyCrc() {
        return verifyCrc;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ShapeId service;
        private final BuilderRef<Set<ShapeId>> operations = BuilderRef.forOrderedSet();
        private boolean verifyCrc = true;
        private ClassLoader classLoader;

        public Builder service(ShapeId service) {
            this.service = service;
            return this;
        }

        public Builder addOperation(ShapeId operation) {
            this.operations.get().add(operation);
            return this;
        }

        public Builder operations(Set<ShapeId> operations) {
            this.operations.clear();
            this.operations.get().addAll(operations);
            return this;
        }

        public Builder verifyCrc(boolean verifyCrc) {
            this.verifyCrc = verifyCrc;
            return this;
        }

        public Builder classLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public SelectiveLoadRequest build() {
            if (service == null) {
                throw new IllegalStateException("service is required");
            }
            return new SelectiveLoadRequest(this);
        }
    }
}
