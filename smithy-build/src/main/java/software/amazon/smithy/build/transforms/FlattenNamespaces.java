/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.build.transforms;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.build.SmithyBuildException;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.FunctionalUtils;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.Pair;

/**
 * {@code flattenNamespaces} updates a model by flattening the namespaces of
 * shapes connected to a service into a single, target namespace. When
 * configuring the transformer, a service and target namespace must be
 * specified. Optionally, tags can be specified for including any additional
 * shapes that should be flattened into the target namespace. Any shape
 * from outside the service closure that is included via the application of a
 * tag will not be included if it conflicts with a shape in the service closure.
 */
public final class FlattenNamespaces extends ConfigurableProjectionTransformer<FlattenNamespaces.Config> {

    /**
     * {@code flattenNamespaces} configuration settings.
     */
    public static final class Config {

        private String namespace;
        private ShapeId service;
        private Set<String> tags = Collections.emptySet();

        /**
         * Sets the target namespace that existing namespaces will be flattened
         * into.
         *
         * @param namespace The target namespace to use in the model.
         */
        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        /**
         * Gets the target namespace that existing namespaces will be flattened
         * into.
         *
         * @return the target namespace to be used in the model.
         */
        public String getNamespace() {
            return namespace;
        }

        /**
         * Sets the service ShapeId that will be flattened into the target
         * namespace.
         *
         * @param service The ID of the service.
         */
        public void setService(ShapeId service) {
            this.service = service;
        }

        /**
         * @return Gets the service shape ID of the service that will have
         * its shape namespaces updated.
         */
        public ShapeId getService() {
            return service;
        }

        /**
         * Sets the set of tags that are retained in the model.
         *
         * @param tags The tags to retain in the model.
         */
        public void setIncludeTagged(Set<String> tags) {
            this.tags = tags;
        }

        /**
         * Gets the set of tags that are retained in the model.
         *
         * @return Returns the tags to retain.
         */
        public Set<String> getIncludeTagged() {
            return tags;
        }
    }

    @Override
    public Class<Config> getConfigType() {
        return Config.class;
    }

    @Override
    protected Model transformWithConfig(TransformContext context, Config config) {
        if (config.getService() == null || config.getNamespace() == null) {
            throw new SmithyBuildException(
                    "'namespace' and 'service' properties must be set on flattenNamespace transformer.");
        }

        Model model = context.getModel();
        if (!model.getShape(config.getService()).isPresent()) {
            throw new SmithyBuildException("Configured service, " + config.getService()
                    + ", not found in model when performing flattenNamespaces transform.");
        }

        Map<ShapeId, ShapeId> shapesToRename = getRenamedShapes(config, model);
        model = context.getTransformer().renameShapes(model, shapesToRename);

        // Remove service renames since they've now been applied, so consumers don't fail service shape validation
        ShapeId updatedServiceId = shapesToRename.get(config.getService());
        ServiceShape updatedService = model.expectShape(updatedServiceId, ServiceShape.class)
                .toBuilder()
                .clearRename()
                .build();
        return context.getTransformer().replaceShapes(model, ListUtils.of(updatedService));
    }

    @Override
    public String getName() {
        return "flattenNamespaces";
    }

    private Map<ShapeId, ShapeId> getRenamedShapes(Config config, Model model) {
        Map<ShapeId, ShapeId> shapesToRename = getRenamedShapesConnectedToService(config, model);
        Set<ShapeId> taggedShapesToInclude = getTaggedShapesToInclude(config.getIncludeTagged(), model);

        for (ShapeId id : taggedShapesToInclude) {
            ShapeId updatedShapeId = updateNamespace(id, config.getNamespace());
            // If new shape ID already exists in map of shapes to rename, skip
            // including the additional shape to avoid a conflict.
            if (!shapesToRename.containsValue(updatedShapeId)) {
                shapesToRename.put(id, updatedShapeId);
            }
        }

        return shapesToRename;
    }

    private ShapeId updateNamespace(ShapeId shapeId, String namespace) {
        if (shapeId.getMember().isPresent()) {
            return ShapeId.fromParts(namespace, shapeId.getName(), shapeId.getMember().get());
        }
        return ShapeId.fromParts(namespace, shapeId.getName());
    }

    private Map<ShapeId, ShapeId> getRenamedShapesConnectedToService(Config config, Model model) {
        Walker shapeWalker = new Walker(NeighborProviderIndex.of(model).getProvider());
        ServiceShape service = model.expectShape(config.getService(), ServiceShape.class);
        return shapeWalker.walkShapes(service).stream()
                .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                .map(shape -> Pair.of(shape.getId(), updateNamespace(shape.getId(), config.getNamespace())))
                .map(pair -> applyServiceRenames(pair.getLeft(), pair.getRight(), service))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
    }

    private Pair<ShapeId, ShapeId> applyServiceRenames(ShapeId fromId, ShapeId toId, ServiceShape service) {
        if (service.getRename().containsKey(fromId)) {
            ShapeId newId = ShapeId.fromParts(toId.getNamespace(), service.getRename().get(fromId));
            return Pair.of(fromId, newId);
        }
        return Pair.of(fromId, toId);
    }

    private Set<ShapeId> getTaggedShapesToInclude(Set<String> tags, Model model) {
        return model.shapes()
                .filter(FunctionalUtils.not(Prelude::isPreludeShape))
                .filter(shape -> isTagged(tags, shape))
                .map(Shape::getId)
                .collect(Collectors.toSet());
    }

    private boolean isTagged(Set<String> tags, Shape shape) {
        return shape.getTags().stream().anyMatch(tags::contains);
    }
}
