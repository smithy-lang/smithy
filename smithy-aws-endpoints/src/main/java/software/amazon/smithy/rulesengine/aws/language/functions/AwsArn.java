/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.language.functions;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An AWS ARN.
 */
@SmithyUnstableApi
public final class AwsArn implements ToSmithyBuilder<AwsArn> {
    private final String partition;
    private final String service;
    private final String region;
    private final String accountId;
    private final List<String> resource;

    private AwsArn(Builder builder) {
        this.partition = SmithyBuilder.requiredState("partition", builder.partition);
        this.service = SmithyBuilder.requiredState("service", builder.service);
        this.region = SmithyBuilder.requiredState("region", builder.region);
        this.accountId = SmithyBuilder.requiredState("accountId", builder.accountId);
        this.resource = builder.resource.copy();
    }

    /**
     * Parses and returns the ARN components if the provided value is a valid AWS ARN.
     *
     * @param arn the value to parse.
     * @return the optional ARN.
     */
    public static Optional<AwsArn> parse(String arn) {
        String[] base = arn.split(":", 6);
        if (base.length != 6) {
            return Optional.empty();
        }
        // First section must be "arn".
        if (!base[0].equals("arn")) {
            return Optional.empty();
        }
        // Sections for partition, service, and resource type must not be empty.
        if (base[1].isEmpty() || base[2].isEmpty() || base[5].isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(builder()
                .partition(base[1])
                .service(base[2])
                .region(base[3])
                .accountId(base[4])
                .resource(Arrays.asList(base[5].split("[:/]", -1)))
                .build());
    }

    /**
     * Builder to create an {@link AwsArn} instance.
     *
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the ARN's partition.
     *
     * @return returns the ARN's partition.
     */
    public String getPartition() {
        return partition;
    }

    /**
     * Gets the ARN's service.
     *
     * @return returns the ARN's service.
     */

    public String getService() {
        return service;
    }

    /**
     * Gets the ARN's region.
     *
     * @return returns the ARN's region.
     */

    public String getRegion() {
        return region;
    }

    /**
     * Gets the ARN's accountId.
     *
     * @return returns the ARN's accountId.
     */

    public String getAccountId() {
        return accountId;
    }

    /**
     * Gets the ARN's resource components.
     *
     * @return returns the ARN's resource components.
     */

    public List<String> getResource() {
        return resource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(partition, service, region, accountId, resource);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AwsArn awsArn = (AwsArn) o;
        return partition.equals(awsArn.partition) && service.equals(awsArn.service) && region.equals(awsArn.region)
               && accountId.equals(awsArn.accountId) && resource.equals(awsArn.resource);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        resource.forEach(builder::append);

        return "Arn[partition=" + partition + ", "
               + "service=" + service + ", "
               + "region=" + region + ", "
               + "accountId=" + accountId + ", "
               + "resource=" + builder + "]";
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .partition(partition)
                .service(service)
                .region(region)
                .accountId(accountId)
                .resource(resource);
    }

    /**
     * A builder used to create an {@link AwsArn} class.
     */
    public static final class Builder implements SmithyBuilder<AwsArn> {
        private final BuilderRef<List<String>> resource = BuilderRef.forList();
        private String partition;
        private String service;
        private String region;
        private String accountId;

        private Builder() {}

        public Builder partition(String partition) {
            this.partition = partition;
            return this;
        }

        public Builder service(String service) {
            this.service = service;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder resource(List<String> resource) {
            this.resource.clear();
            this.resource.get().addAll(resource);
            return this;
        }

        @Override
        public AwsArn build() {
            return new AwsArn(this);
        }
    }
}
