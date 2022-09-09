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
import software.amazon.smithy.rulesengine.language.model.Outputs;
import software.amazon.smithy.rulesengine.language.model.Partition;
import software.amazon.smithy.rulesengine.language.model.Partitions;
import software.amazon.smithy.rulesengine.language.stdlib.partition.PartitionDataProvider;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.syntax.fn.Fn;
import software.amazon.smithy.rulesengine.language.syntax.fn.FnNode;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.fn.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.util.LazyValue;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class PartitionFn extends FunctionDefinition {
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
    public String id() {
        return ID;
    }

    @Override
    public List<Type> arguments() {
        return Collections.singletonList(Type.str());
    }

    @Override
    public Type returnType() {
        LinkedHashMap<Identifier, Type> type = new LinkedHashMap<>();
        type.put(NAME, Type.str());
        type.put(DNS_SUFFIX, Type.str());
        type.put(DUAL_STACK_DNS_SUFFIX, Type.str());
        type.put(SUPPORTS_DUAL_STACK, Type.bool());
        type.put(SUPPORTS_FIPS, Type.bool());
        return Type.optional(new Type.Record(type));
    }

    @Override
    public Value eval(List<Value> arguments) {
        String regionName = arguments.get(0).expectString();

        final PartitionData data = partitionData.value();

        Partition matchedPartition;
        boolean inferred = false;

        // Known region
        matchedPartition = data.regionMap.get(regionName);
        if (matchedPartition == null) {
            // try matching on region name pattern
            for (Partition p : data.partitions) {
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

        Outputs matchedOutputs = matchedPartition.outputs();
        return Value.record(MapUtils.of(
                NAME, Value.str(matchedPartition.id()),
                DNS_SUFFIX, Value.str(matchedOutputs.dnsSuffix()),
                DUAL_STACK_DNS_SUFFIX, Value.str(matchedOutputs.dualStackDnsSuffix()),
                SUPPORTS_FIPS, Value.bool(matchedOutputs.supportsFips()),
                SUPPORTS_DUAL_STACK, Value.bool(matchedOutputs.supportsDualStack()),
                INFERRED, Value.bool(inferred)));
    }

    public static Fn ofExprs(Expr expr) {
        return new LibraryFunction(new PartitionFn(), FnNode.ofExprs(ID, expr));
    }

    public static Fn fromParam(Parameter param) {
        return PartitionFn.ofExprs(param.expr());
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
        private final List<Partition> partitions = new ArrayList<>();
        private final Map<String, Partition> regionMap = new HashMap<>();
    }
}
