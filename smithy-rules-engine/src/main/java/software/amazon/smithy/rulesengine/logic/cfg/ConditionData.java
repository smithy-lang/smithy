/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.logic.ConditionInfo;

/**
 * Extracts and indexes condition data from a CFG.
 */
public final class ConditionData {
    private final Condition[] conditions;
    private final Map<Condition, Integer> conditionToIndex;
    private final Map<Condition, ConditionInfo> conditionInfos;

    private ConditionData(Condition[] conditions, Map<Condition, Integer> index, Map<Condition, ConditionInfo> infos) {
        this.conditions = conditions;
        this.conditionToIndex = index;
        this.conditionInfos = infos;
    }

    /**
     * Extracts and indexes all conditions from a CFG.
     *
     * @param cfg the control flow graph to process
     * @return ConditionData containing indexed conditions
     */
    public static ConditionData from(Cfg cfg) {
        List<Condition> conditionList = new ArrayList<>();
        Map<Condition, Integer> indexMap = new LinkedHashMap<>();
        Map<Condition, ConditionInfo> infoMap = new HashMap<>();

        for (CfgNode node : cfg) {
            if (node instanceof ConditionNode) {
                ConditionNode condNode = (ConditionNode) node;
                ConditionInfo info = condNode.getCondition();
                Condition condition = info.getCondition();

                if (!indexMap.containsKey(condition)) {
                    indexMap.put(condition, conditionList.size());
                    conditionList.add(condition);
                    infoMap.put(condition, info);
                }
            }
        }

        return new ConditionData(conditionList.toArray(new Condition[0]), indexMap, infoMap);
    }

    public Condition[] getConditions() {
        return conditions;
    }

    public Map<Condition, ConditionInfo> getConditionInfos() {
        return conditionInfos;
    }
}
