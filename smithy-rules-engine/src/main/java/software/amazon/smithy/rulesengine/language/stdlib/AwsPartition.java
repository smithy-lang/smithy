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

package software.amazon.smithy.rulesengine.language.stdlib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Pattern;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.model.PartitionOutputs;
import software.amazon.smithy.rulesengine.language.model.Partitions;
import software.amazon.smithy.rulesengine.language.stdlib.partition.PartitionDataProvider;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.syntax.fn.Function;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.fn.LibraryFunction;
import software.amazon.smithy.rulesengine.language.util.LazyValue;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An AWS rule-set function for mapping a region string to a partition.
 */
@SmithyUnstableApi
public final class AwsPartition extends FunctionDefinition {
    public static final String ID = "aws.partition";

    public static final Identifier NAME = Identifier.of("name");
    public static final Identifier DNS_SUFFIX = Identifier.of("dnsSuffix");
    public static final Identifier DUAL_STACK_DNS_SUFFIX = Identifier.of("dualStackDnsSuffix");
    public static final Identifier SUPPORTS_FIPS = Identifier.of("supportsFIPS");
    public static final Identifier SUPPORTS_DUAL_STACK = Identifier.of("supportsDualStack");
    public static final Identifier INFERRED = Identifier.of("inferred");

    private final LazyValue<PartitionData> partitionData = LazyValue.<PartitionData>builder()
            .initializer(this::loadPartitionData)
            .build();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<Type> getArguments() {
        return Collections.singletonList(Type.string());
    }

    @Override
    public Type getReturnType() {
        LinkedHashMap<Identifier, Type> type = new LinkedHashMap<>();
        type.put(NAME, Type.string());
        type.put(DNS_SUFFIX, Type.string());
        type.put(DUAL_STACK_DNS_SUFFIX, Type.string());
        type.put(SUPPORTS_DUAL_STACK, Type.bool());
        type.put(SUPPORTS_FIPS, Type.bool());
        return Type.optional(new Type.Record(type));
    }

    @Override
    public Value evaluate(List<Value> arguments) {
        String regionName = arguments.get(0).expectString();

        final PartitionData data = partitionData.value();

        software.amazon.smithy.rulesengine.language.model.Partition matchedPartition;
        boolean inferred = false;

        // Known region
        matchedPartition = data.regionMap.get(regionName);
        if (matchedPartition == null) {
            // try matching on region name pattern
            for (software.amazon.smithy.rulesengine.language.model.Partition p : data.partitions) {
                Pattern regex = Pattern.compile(p.regionRegex());
                if (regex.matcher(regionName).matches()) {
                    matchedPartition = p;
                    inferred = true;
                    break;
                }
            }
        }

        if (matchedPartition == null) {
            matchedPartition = data.partitions.stream().filter(p -> p.id().equals("aws")).findFirst().get();
        }

        PartitionOutputs matchedPartitionOutputs = matchedPartition.getOutputs();
        return Value.record(MapUtils.of(
                NAME, Value.string(matchedPartition.id()),
                DNS_SUFFIX, Value.string(matchedPartitionOutputs.dnsSuffix()),
                DUAL_STACK_DNS_SUFFIX, Value.string(matchedPartitionOutputs.dualStackDnsSuffix()),
                SUPPORTS_FIPS, Value.bool(matchedPartitionOutputs.supportsFips()),
                SUPPORTS_DUAL_STACK, Value.bool(matchedPartitionOutputs.supportsDualStack()),
                INFERRED, Value.bool(inferred)));
    }

    /**
     * Constructs a function definition for resolving a string expression to a partition.
     *
     * @param expression expression to evaluate to a partition.
     * @return the function representing the partition lookup.
     */
    public static Function ofExpression(Expression expression) {
        return new LibraryFunction(new AwsPartition(), FunctionNode.ofExpressions(ID, expression));
    }

    private PartitionData loadPartitionData() {
        Iterator<PartitionDataProvider> iter = ServiceLoader.load(PartitionDataProvider.class).iterator();
        if (!iter.hasNext()) {
            throw new RuntimeException("Unable to locate partition data");
        }

        PartitionDataProvider provider = iter.next();

        Partitions partitions = provider.loadPartitions();

        PartitionData partitionData = new PartitionData();

        partitions.partitions().forEach(part -> {
            partitionData.partitions.add(part);
            part.regions().forEach((name, override) -> {
                partitionData.regionMap.put(name, part);
            });
        });

        return partitionData;
    }

    private static class PartitionData {
        private final List<software.amazon.smithy.rulesengine.language.model.Partition> partitions = new ArrayList<>();
        private final Map<String, software.amazon.smithy.rulesengine.language.model.Partition> regionMap =
                new HashMap<>();
    }
}
