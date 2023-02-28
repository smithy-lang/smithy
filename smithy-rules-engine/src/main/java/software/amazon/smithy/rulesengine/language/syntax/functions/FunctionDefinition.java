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

package software.amazon.smithy.rulesengine.language.syntax.functions;

import java.util.List;
import software.amazon.smithy.rulesengine.language.eval.type.Type;
import software.amazon.smithy.rulesengine.language.eval.value.Value;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An abstract definition of a rule-engine function.
 */
@SmithyUnstableApi
public interface FunctionDefinition {
    /**
     * The ID of this function.
     * @return The ID string
     */
    String getId();

    /**
     * The arguments to this function.
     * @return The function arguments
     */
    List<Type> getArguments();

    /**
     * The return type of this function definition.
     * @return The function return type
     */
    Type getReturnType();

    /**
     * Evaluate the arguments to a given function to compute a result.
     * @return The resulting value
     */
    Value evaluate(List<Value> arguments);

}
