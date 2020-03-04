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

package software.amazon.smithy.linters;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;

/**
 * Emits a validation event for each shape that matches a selector.
 */
public final class EmitEachSelectorValidator extends AbstractValidator {

    /**
     * EmitEachSelector configuration settings.
     */
    public static final class Config {
        private Selector selector;

        /**
         * Each shape that matches the given selector will emit a validation
         * event.
         *
         * @return Selector to match on.
         */
        public Selector getSelector() {
            return selector;
        }

        public void setSelector(Selector selector) {
            this.selector = selector;
        }
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(EmitEachSelectorValidator.class, configuration -> {
                NodeMapper mapper = new NodeMapper();
                Config config = mapper.deserialize(configuration, Config.class);
                return new EmitEachSelectorValidator(config);
            });
        }
    }

    private final Config config;

    private EmitEachSelectorValidator(Config config) {
        this.config = config;
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        return config.getSelector().select(model).stream()
                .map(shape -> danger(shape, "Selector capture matched selector: " + config.getSelector()))
                .collect(Collectors.toList());
    }
}
