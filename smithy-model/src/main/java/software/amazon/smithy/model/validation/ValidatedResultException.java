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

package software.amazon.smithy.model.validation;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Thrown when {@link ValidatedResult#validate} is called and the result contains
 * ERROR events.
 */
public class ValidatedResultException extends RuntimeException {
    private final List<ValidationEvent> events;

    public ValidatedResultException(List<ValidationEvent> events) {
        super("Result contained ERROR severity validation events: \n"
                + events.stream().map(ValidationEvent::toString).sorted().collect(Collectors.joining("\n")));
        this.events = events;
    }

    public List<ValidationEvent> getValidationEvents() {
        return events;
    }
}
