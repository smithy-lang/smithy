/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Computes the difference between two models and any problems that might
 * occur due to those differences.
 */
public final class ModelDiff {
    private ModelDiff() {}

    /**
     * Evaluates the differences between two models.
     *
     * @param oldModel Previous version of the model.
     * @param newModel New model to compare.
     * @return Returns the computed validation events.
     */
    public static List<ValidationEvent> compare(Model oldModel, Model newModel) {
        return compare(ModelDiff.class.getClassLoader(), oldModel, newModel);
    }

    /**
     * Evaluates the differences between two models.
     *
     * @param classLoader ClassLoader used to find {@link DiffEvaluator} service providers.
     * @param oldModel Previous version of the model.
     * @param newModel New model to compare.
     * @return Returns the computed validation events.
     */
    public static List<ValidationEvent> compare(ClassLoader classLoader, Model oldModel, Model newModel) {
        List<DiffEvaluator> evaluators = new ArrayList<>();
        ServiceLoader.load(DiffEvaluator.class, classLoader).forEach(evaluators::add);
        return compare(evaluators, oldModel, newModel);
    }

    private static List<ValidationEvent> compare(List<DiffEvaluator> evaluators, Model oldModel, Model newModel) {
        Differences differences = Differences.detect(oldModel, newModel);
        return evaluators.parallelStream()
                .flatMap(evaluator -> evaluator.evaluate(differences).stream())
                .collect(Collectors.toList());
    }
}
