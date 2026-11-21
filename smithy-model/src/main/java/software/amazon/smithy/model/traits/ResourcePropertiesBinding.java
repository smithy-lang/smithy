/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A {@link ResourceBinding} that may also declare where to find the resource's
 * properties.
 *
 * <p>Implemented by {@link ResourceLifecycleBinding} for the create, read, and
 * update traits, and by {@link ResourcePutBinding} for the put trait.
 */
@SmithyUnstableApi
public interface ResourcePropertiesBinding extends ResourceBinding {

    /**
     * @return Gets the explicit map of resource property name to a locator for its value.
     */
    Map<String, ResourceMemberBinding> getProperties();

    /**
     * @return Gets the structural JMESPath from whose members properties are inferred by name.
     */
    Optional<String> getPropertiesFrom();
}
