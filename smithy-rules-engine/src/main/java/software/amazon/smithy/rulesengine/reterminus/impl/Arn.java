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

package software.amazon.smithy.rulesengine.reterminus.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An AWS Arn.
 */
public final class Arn {
    private final String partition;
    private final String service;
    private final String region;
    private final String accountId;
    private final List<String> resource;

    public Arn(String partition, String service, String region, String accountId, List<String> resource) {
        this.partition = partition;
        this.service = service;
        this.region = region;
        this.accountId = accountId;
        this.resource = Collections.unmodifiableList(resource);
    }

    public static Optional<Arn> parse(String arn) {
        String[] base = arn.split(":", 6);
        if (base.length != 6) {
            return Optional.empty();
        }
        if (!base[0].equals("arn")) {
            return Optional.empty();
        }
        List<String> resource = Arrays.stream(base[5].split("[:/]", -1)).collect(Collectors.toList());
        return Optional.of(new Arn(base[1], base[2], base[3], base[4], resource));
    }

    public String partition() {
        return partition;
    }

    public String service() {
        return service;
    }

    public String region() {
        return region;
    }

    public String accountId() {
        return accountId;
    }

    public List<String> resource() {
        return resource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Arn arn = (Arn) o;
        return partition.equals(arn.partition) && service.equals(arn.service) && region.equals(arn.region)
               && accountId.equals(arn.accountId) && resource.equals(arn.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partition, service, region, accountId, resource);
    }

    @Override
    public String toString() {
        StringBuilder resource = new StringBuilder();
        this.resource().forEach(resource::append);

        return "Arn["
               + "partition="
               + partition + ", "
               + "service="
               + service + ", "
               + "region="
               + region + ", "
               + "accountId="
               + accountId + ", "
               + "resource="
               + resource + ']';
    }

}
