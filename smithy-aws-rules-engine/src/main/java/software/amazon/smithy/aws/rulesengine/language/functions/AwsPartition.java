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

package software.amazon.smithy.aws.rulesengine.language.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Pattern;
import software.amazon.smithy.aws.rulesengine.language.functions.partition.Partition;
import software.amazon.smithy.aws.rulesengine.language.functions.partition.PartitionDataProvider;
import software.amazon.smithy.aws.rulesengine.language.functions.partition.PartitionOutputs;
import software.amazon.smithy.aws.rulesengine.language.functions.partition.Partitions;
import software.amazon.smithy.rulesengine.language.evaluation.type.RecordType;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An AWS rule-set function for mapping a region string to a partition.
 */
@SmithyUnstableApi
public final class AwsPartition extends LibraryFunction {
    public static final String ID = "aws.partition";
    public static final Identifier NAME = Identifier.of("name");
    public static final Identifier DNS_SUFFIX = Identifier.of("dnsSuffix");
    public static final Identifier DUAL_STACK_DNS_SUFFIX = Identifier.of("dualStackDnsSuffix");
    public static final Identifier SUPPORTS_FIPS = Identifier.of("supportsFIPS");
    public static final Identifier SUPPORTS_DUAL_STACK = Identifier.of("supportsDualStack");
    public static final Identifier INFERRED = Identifier.of("inferred");

    private static final Definition DEFINITION = new Definition();
    private static final PartitionData PARTITION_DATA = loadPartitionData();

    public AwsPartition(FunctionNode functionNode) {
        super(DEFINITION, functionNode);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitLibraryFunction(DEFINITION, getArguments());
    }

    private static PartitionData loadPartitionData() {
        Iterator<PartitionDataProvider> iter = ServiceLoader.load(PartitionDataProvider.class).iterator();
        if (!iter.hasNext()) {
            throw new RuntimeException("Unable to locate partition data");
        }

        PartitionDataProvider provider = iter.next();

        Partitions partitions = provider.loadPartitions();

        PartitionData partitionData = new PartitionData();

        partitions.getPartitions().forEach(part -> {
            partitionData.partitions.add(part);
            part.getRegions().forEach((name, override) -> {
                partitionData.regionMap.put(name, part);
            });
        });

        return partitionData;
    }

    private static class PartitionData {
        private final List<Partition> partitions = new ArrayList<>();
        private final Map<String, Partition> regionMap = new HashMap<>();
    }

    public static final class Definition implements FunctionDefinition {
        @Override
        public String getId() {
            return ID;
        }

        @Override
        public List<Type> getArguments() {
            return Collections.singletonList(Type.stringType());
        }

        @Override
        public Type getReturnType() {
            Map<Identifier, Type> type = new LinkedHashMap<>();
            type.put(NAME, Type.stringType());
            type.put(DNS_SUFFIX, Type.stringType());
            type.put(DUAL_STACK_DNS_SUFFIX, Type.stringType());
            type.put(SUPPORTS_DUAL_STACK, Type.booleanType());
            type.put(SUPPORTS_FIPS, Type.booleanType());
            return Type.optionalType(new RecordType(type));
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            String regionName = arguments.get(0).expectStringValue().getValue();
            Partition matchedPartition;
            boolean inferred = false;

            // Known region
            matchedPartition = PARTITION_DATA.regionMap.get(regionName);
            if (matchedPartition == null) {
                // Try matching on region name pattern
                for (Partition p : PARTITION_DATA.partitions) {
                    Pattern regex = Pattern.compile(p.getRegionRegex());
                    if (regex.matcher(regionName).matches()) {
                        matchedPartition = p;
                        inferred = true;
                        break;
                    }
                }
            }

            if (matchedPartition == null) {
                for (Partition partition : PARTITION_DATA.partitions) {
                    if (partition.getId().equals("aws")) {
                        matchedPartition = partition;
                        break;
                    }
                }
            }

            if (matchedPartition == null) {
                throw new RuntimeException("Unable to match a partition for region " + regionName);
            }

            PartitionOutputs matchedPartitionOutputs = matchedPartition.getOutputs();
            return Value.recordValue(MapUtils.of(
                    NAME, Value.stringValue(matchedPartition.getId()),
                    DNS_SUFFIX, Value.stringValue(matchedPartitionOutputs.getDnsSuffix()),
                    DUAL_STACK_DNS_SUFFIX, Value.stringValue(matchedPartitionOutputs.getDualStackDnsSuffix()),
                    SUPPORTS_FIPS, Value.booleanValue(matchedPartitionOutputs.supportsFips()),
                    SUPPORTS_DUAL_STACK, Value.booleanValue(matchedPartitionOutputs.supportsDualStack()),
                    INFERRED, Value.booleanValue(inferred)));
        }

        @Override
        public AwsPartition createFunction(FunctionNode functionNode) {
            return new AwsPartition(functionNode);
        }
    }
}
