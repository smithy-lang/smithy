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

package software.amazon.smithy.model.selector;

import java.util.List;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;

/**
 * Traverses into the neighbors of shapes with an optional list of
 * neighbor rel filters.
 */
final class ForwardNeighborSelector extends AbstractNeighborSelector {

    ForwardNeighborSelector(List<String> relTypes) {
        super(relTypes);
    }

    @Override
    NeighborProvider getNeighborProvider(Context context, boolean includeTraits) {
        return includeTraits
               ? context.neighborIndex.getProviderWithTraitRelationships()
               : context.neighborIndex.getProvider();
    }

    @Override
    Response emitMatchingRel(Context context, Relationship rel, Receiver next) {
        return next.apply(context, rel.getNeighborShape().get());
    }
}
