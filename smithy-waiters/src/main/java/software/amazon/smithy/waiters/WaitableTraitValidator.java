/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.waiters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;

@SmithyInternalApi
public final class WaitableTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(OperationShape.class)
                .filter(operation -> operation.hasTrait(WaitableTrait.class))
                .flatMap(operation -> validateOperation(model, operation).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateOperation(Model model, OperationShape operation) {
        List<ValidationEvent> events = new ArrayList<>();
        WaitableTrait trait = operation.expectTrait(WaitableTrait.class);

        for (Map.Entry<String, Waiter> entry : trait.getWaiters().entrySet()) {
            String waiterName = entry.getKey();
            Waiter waiter = entry.getValue();

            if (waiter.getMinDelay() > waiter.getMaxDelay()) {
                events.add(error(operation,
                        trait,
                        String.format(
                                "`%s` trait waiter named `%s` has a `minDelay` value of %d that is greater than its "
                                        + "`maxDelay` value of %d",
                                WaitableTrait.ID,
                                waiterName,
                                waiter.getMinDelay(),
                                waiter.getMaxDelay())));
            }

            boolean foundSuccess = false;
            for (int i = 0; i < waiter.getAcceptors().size(); i++) {
                Acceptor acceptor = waiter.getAcceptors().get(i);
                WaiterMatcherValidator visitor = new WaiterMatcherValidator(model, operation, waiterName, i);
                events.addAll(acceptor.getMatcher().accept(visitor));
                if (acceptor.getState() == AcceptorState.SUCCESS) {
                    foundSuccess = true;
                }
            }

            if (!foundSuccess) {
                // Emitted as unsuppressable "WaitableTrait".
                events.add(error(operation,
                        trait,
                        String.format(
                                "No success state matcher found for `%s` trait waiter named `%s`",
                                WaitableTrait.ID,
                                waiterName)));
            }
        }

        return events;
    }
}
