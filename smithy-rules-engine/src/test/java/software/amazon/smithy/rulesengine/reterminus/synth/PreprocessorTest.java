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

package software.amazon.smithy.rulesengine.reterminus.synth;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.v1.PartitionV1;
import software.amazon.smithy.rulesengine.v1.V1ModelUtils;

//import static org.assertj.core.api.Assertions.assertThat;

public class PreprocessorTest {
    private static PartitionV1 awsPartition;

    @BeforeAll
    public static void setup() {
        List<PartitionV1> partitions = V1ModelUtils.loadPartitionsFromClasspath();
        awsPartition = partitions.stream()
                .filter(p -> p.partition().equals("aws"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not found 'aws' partition"));
    }

    @Test
    public void test_runtimeSagemaker_hasFipsSpecificVariant() {
//        Preprocessor preprocessor = new Preprocessor(awsPartition, "runtime.sagemaker");
//
//        IntermediateModel intermediateModel = preprocessor.preprocess();
//
//        EndpointModel em = intermediateModel.defaultEndpointModel();
//
//        System.out.println(intermediateModel.uniqueFipsRegions());
    }
}
