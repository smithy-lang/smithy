/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.language.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.aws.language.functions.partition.Partition;
import software.amazon.smithy.rulesengine.aws.language.functions.partition.PartitionOutputs;
import software.amazon.smithy.rulesengine.aws.language.functions.partition.Partitions;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyInternalApi;
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
    public static final Identifier IMPLICIT_GLOBAL_REGION = Identifier.of("implicitGlobalRegion");
    public static final Identifier INFERRED = Identifier.of("inferred");

    private static final Definition DEFINITION = new Definition();

    // The following are mutable to allow for overriding the contents
    // of the PARTITIONS list for test use cases. They MUST NOT have
    // contents exposed directly, only through copies as is done in
    // the `evaluate` method below.
    private static final List<Partition> PARTITIONS = new ArrayList<>();
    private static final Map<String, Partition> REGION_MAP = new HashMap<>();

    static {
        PARTITIONS.addAll(Partitions.fromNode(
                Node.parse(Partitions.class.getResourceAsStream("partitions.json")))
                .getPartitions());
        initializeRegionMap();
    }

    private AwsPartition(FunctionNode functionNode) {
        super(DEFINITION, functionNode);
    }

    /**
     * Overrides the partitions provided by default.
     *
     * @param partitions A list of partitions to set.
     */
    @SmithyInternalApi
    public static void overridePartitions(Partitions partitions) {
        PARTITIONS.clear();
        PARTITIONS.addAll(partitions.getPartitions());
        initializeRegionMap();
    }

    private static void initializeRegionMap() {
        REGION_MAP.clear();
        for (Partition partition : PARTITIONS) {
            for (String region : partition.getRegions().keySet()) {
                REGION_MAP.put(region, partition);
            }
        }
    }

    /**
     * Gets the {@link FunctionDefinition} implementation.
     *
     * @return the function definition.
     */
    public static Definition getDefinition() {
        return DEFINITION;
    }

    /**
     * Creates a {@link AwsPartition} function from the given expressions.
     *
     * @param arg1 the region to retrieve partition information from.
     * @return The resulting {@link AwsPartition} function.
     */
    public static AwsPartition ofExpressions(ToExpression arg1) {
        return DEFINITION.createFunction(FunctionNode.ofExpressions(ID, arg1));
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitLibraryFunction(DEFINITION, getArguments());
    }

    /**
     * A {@link FunctionDefinition} for the {@link AwsPartition} function.
     */
    public static final class Definition implements FunctionDefinition {
        private final Type returnType;

        private Definition() {
            Map<Identifier, Type> type = new LinkedHashMap<>();
            type.put(NAME, Type.stringType());
            type.put(DNS_SUFFIX, Type.stringType());
            type.put(DUAL_STACK_DNS_SUFFIX, Type.stringType());
            type.put(SUPPORTS_DUAL_STACK, Type.booleanType());
            type.put(SUPPORTS_FIPS, Type.booleanType());
            type.put(IMPLICIT_GLOBAL_REGION, Type.stringType());
            returnType = Type.optionalType(Type.recordType(type));
        }

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
            return returnType;
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            String regionName = arguments.get(0).expectStringValue().getValue();
            Partition matchedPartition;
            boolean inferred = false;

            // Known region
            matchedPartition = REGION_MAP.get(regionName);
            if (matchedPartition == null) {
                // Try matching on region name pattern
                for (Partition partition : PARTITIONS) {
                    Pattern regex = Pattern.compile(partition.getRegionRegex());
                    if (regex.matcher(regionName).matches()) {
                        matchedPartition = partition;
                        inferred = true;
                        break;
                    }
                }
            }

            // Default to the `aws` partition.
            if (matchedPartition == null) {
                for (Partition partition : PARTITIONS) {
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
                    NAME,
                    Value.stringValue(matchedPartition.getId()),
                    DNS_SUFFIX,
                    Value.stringValue(matchedPartitionOutputs.getDnsSuffix()),
                    DUAL_STACK_DNS_SUFFIX,
                    Value.stringValue(matchedPartitionOutputs.getDualStackDnsSuffix()),
                    SUPPORTS_FIPS,
                    Value.booleanValue(matchedPartitionOutputs.supportsFips()),
                    SUPPORTS_DUAL_STACK,
                    Value.booleanValue(matchedPartitionOutputs.supportsDualStack()),
                    INFERRED,
                    Value.booleanValue(inferred),
                    IMPLICIT_GLOBAL_REGION,
                    Value.stringValue(matchedPartitionOutputs.getImplicitGlobalRegion())));
        }

        @Override
        public AwsPartition createFunction(FunctionNode functionNode) {
            return new AwsPartition(functionNode);
        }
    }
}
