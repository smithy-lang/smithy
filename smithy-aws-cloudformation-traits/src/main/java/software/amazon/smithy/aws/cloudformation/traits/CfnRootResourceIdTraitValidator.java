/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.cloudformation.traits;

import java.util.ArrayList;
import java.util.List;
import static software.amazon.smithy.model.validation.ValidationUtils.splitCamelCaseWord;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;




/**
 * Validates that members marked as having @CfnRootResourceId are marked within the listAPI
 * and not in other APIs.
 */
public final class CfnRootResourceIdTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        List<String> prefixes = new ArrayList<>();
        // accounting for if there is case issue - can change later if case is enforced
        prefixes.add("List");
        prefixes.add("list");

        for (Shape shape : model.getShapesWithTrait(CfnRootResourceIdTrait.class)) {
            CfnRootResourceIdTrait trait = shape.expectTrait(CfnRootResourceIdTrait.class);
            // get shape name so we can check if the prefix has the verb "list"
            List<String> words = splitCamelCaseWord(shape.getId().getName());
            String foundPrefix = words.get(0);
            Boolean isListOperation;

            // check if the prefix is for list operation
            if (prefixes.contains(foundPrefix)) {
                isListOperation = true;
            } else {
                isListOperation = false;
            }
            // checking trait applied to List API only and not other APIs
            // if found that trait is applied to API other than list, then add to events with error
            if (!shape.isOperationShape() && !isListOperation) {
                events.add(error(shape, trait, String.format("Member with CfnRootResourceId is not marked within a List Operation")));
            }
        }
        return events;
    }
}
