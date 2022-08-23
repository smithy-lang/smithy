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

package software.amazon.smithy.rulesengine.language.synth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import software.amazon.smithy.rulesengine.v1.DefaultsV1;
import software.amazon.smithy.rulesengine.v1.PartitionV1;
import software.amazon.smithy.rulesengine.v1.ServiceV1;
import software.amazon.smithy.rulesengine.v1.VariantV1;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class Preprocessor {
    private final PartitionV1 partition;
    private final String endpointPrefix;
    private final ServiceV1 service;

    private EndpointModel defaultEndpointModel;
    private Map<String, EndpointModel> regionalEndpointModels;

    public Preprocessor(PartitionV1 partition, String endpointPrefix) {
        this.partition = partition;
        this.endpointPrefix = endpointPrefix;
        this.service = partition.service(endpointPrefix).orElse(null);
    }

    public Optional<IntermediateModel> preprocess() {
        if (service == null) {
            return Optional.empty();
        }

        IntermediateModel.Builder builder = IntermediateModel.builder()
                .serviceId(endpointPrefix)
                .partition(partition);

        defaultEndpointModel = defaultEndpointModel();
        regionalEndpointModels = regionalEndpointModels();

        builder.defaultEndpointModel(defaultEndpointModel);

        // find region specific fips
        Set<String> uniqueFipsRegions = findRegionsWithUniqueFipsEndpoint();
        uniqueFipsRegions.forEach(builder::addUniqueFipsRegion);

        uniqueFipsRegions.forEach(r -> {
            EndpointModel regionEndpoint = endpointModelForRegion(r, defaultEndpointModel);
            builder.putRegionEndpoint(r, regionEndpoint);
        });

        // find region specific default
        Set<String> uniqueDefaultRegions = findRegionsWithUniqueDefaultEndpoint();
        uniqueDefaultRegions.forEach(builder::addUniqueDefaultRegion);

        uniqueDefaultRegions.forEach(r -> {
            EndpointModel regionEndpoint = endpointModelForRegion(r, defaultEndpointModel);
            builder.putRegionEndpoint(r, regionEndpoint);
        });

        return Optional.of(builder.build());
    }

    private Set<String> findRegionsWithUniqueDefaultEndpoint() {
        EndpointView defaultView = defaultEndpointModel.defaultView();

        Set<String> regions = new HashSet<>();
        regionalEndpointModels.forEach((region, model) -> {
            Function<String, String> resolver = (t) -> {
                if (t.equals("{region}")) {
                    return region;
                }
                return null;
            };

            EndpointView regionDefaultView = model.defaultView();

            String reifiedDefaultEndpoint = defaultView.reifyHostName(resolver);
            String reifiedRegionEndpoint = regionDefaultView.reifyHostName(resolver);

            if (!reifiedDefaultEndpoint.equals(reifiedRegionEndpoint)) {
                regions.add(region);
            }
        });

        return regions;
    }

    private Set<String> findRegionsWithUniqueFipsEndpoint() {
        EndpointView defaultFipsView = defaultEndpointModel.fipsView().get();

        Set<String> regions = new HashSet<>();
        regionalEndpointModels.forEach((region, model) -> {
            Function<String, String> resolver = (t) -> {
                if (t.equals("{region}")) {
                    return region;
                }
                return null;
            };

            EndpointView regionFipsView = model.fipsView().get();

            String reifiedDefaultEndpoint = defaultFipsView.reifyHostName(resolver);
            String reifiedRegionEndpoint = regionFipsView.reifyHostName(resolver);

            if (!reifiedDefaultEndpoint.equals(reifiedRegionEndpoint)) {
                regions.add(region);
            }
        });

        return regions;
    }

    private EndpointModel defaultEndpointModel() {
        return serviceEndpointModel().merge(partitionEndpointModel());
    }

    private EndpointModel partitionEndpointModel() {
        return endpointModel(partition.defaults())
                .toBuilder()
                .dnsSuffix(partition.dnsSuffix())
                .build();
    }

    private EndpointModel serviceEndpointModel() {
        if (service.defaults() == null) {
            return EndpointModel.builder()
                    .endpointPrefix(endpointPrefix)
                    .build();
        }
        return endpointModel(service.defaults());
    }

    private Map<String, EndpointModel> regionalEndpointModels() {
        Map<String, EndpointModel> regional = new HashMap<>();
        service.endpoints().entrySet().stream()
                .filter(e -> {
                    DefaultsV1 defaults = e.getValue();
                    return !defaults.deprecated() || (defaults.deprecated() && !defaults.variants().isEmpty());
                })
                .forEach(e -> regional.put(e.getKey(), endpointModel(e.getValue()).merge(defaultEndpointModel)));
        return regional;
    }

    private EndpointModel endpointModelForRegion(String region, EndpointModel defaultEndpointModel) {
        DefaultsV1 regionDefaults = service.endpoints().get(region);
        return endpointModel(regionDefaults).merge(defaultEndpointModel);
    }

    private EndpointModel endpointModel(DefaultsV1 defaultsV1) {
        EndpointModel.Builder builder = EndpointModel.builder();

        builder.endpointPrefix(endpointPrefix)
                .hostname(defaultsV1.hostname())
                .dnsSuffix(defaultsV1.dnsSuffix())
                .authSchemes(defaultsV1.signatureVersions());

        for (VariantV1 v : defaultsV1.variants()) {
            EndpointType type = EndpointType.fromTagList(v.tags());
            VariantModel variantModel = variantModel(v);

            switch (type) {
                case FIPS:
                    builder.fips(variantModel);
                    break;
                case DUAL_STACK:
                    builder.dualStack(variantModel);
                    break;
                case FIPS_DUAL_STACK:
                    builder.fipsDualStack(variantModel);
                    break;
                default:
                    throw new RuntimeException("Unknown endpoint type " + type);
            }
        }

        return builder.build();
    }

    private VariantModel variantModel(VariantV1 variant) {
        return VariantModel.builder()
                .dnsSuffix(variant.dnsSuffix())
                .hostname(variant.hostname())
                .authSchemes(variant.authSchemes())
                .build();
    }
}
