/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.contracts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isA;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.jmespath.ast.ComparatorExpression;
import software.amazon.smithy.jmespath.ast.FunctionExpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class ContractsTraitTest {
    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("test.smithy"))
                .assemble()
                .unwrap();

        // @contracts applies to operations, where expressions reference the
        // {input, output, error, before, after} call.
        Shape shape = result.expectShape(ShapeId.from("smithy.example#FetchLogs"));
        ContractsTrait trait = shape.expectTrait(ContractsTrait.class);
        assertThat(trait.getContracts().size(), equalTo(1));
        Condition contract = trait.getContracts().get("StartBeforeEnd");
        assertThat(contract.getExpression(), isA(ComparatorExpression.class));

        // A deeper contract reads resource state from the before snapshot through a
        // projected reference handle.
        shape = result.expectShape(ShapeId.from("smithy.example#PublishDocument"));
        trait = shape.expectTrait(ContractsTrait.class);
        assertThat(trait.getContracts().size(), equalTo(1));
        contract = trait.getContracts().get("DocumentApprovedOnSuccess");
        assertThat(contract.getExpression(), isA(FunctionExpression.class));
    }
}
