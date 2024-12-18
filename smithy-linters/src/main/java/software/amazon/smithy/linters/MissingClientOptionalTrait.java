/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.linters;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;

/**
 * Validates that the clientOptional trait is applied based on the rules
 * defined in the validator.
 */
public final class MissingClientOptionalTrait extends AbstractValidator {

    /**
     * MissingClientOptionalTrait configuration settings.
     */
    public static final class Config {
        private boolean onRequiredStructureOrUnion;
        private boolean onRequiredOrDefault;

        /**
         * Whether clientOptional is required for members marked as required that
         * target structures or unions (shapes with no zero value, meaning it's
         * impossible to later remove the required trait and replace it with the
         * default trait).
         *
         * @return Returns true if required.
         */
        public boolean onRequiredStructureOrUnion() {
            return onRequiredStructureOrUnion;
        }

        public void onRequiredStructureOrUnion(boolean onRequiredStructuresOrUnion) {
            this.onRequiredStructureOrUnion = onRequiredStructuresOrUnion;
        }

        /**
         * Whether clientOptional is required for all members marked as required or default.
         *
         * @return Returns true if required.
         */
        public boolean onRequiredOrDefault() {
            return onRequiredOrDefault;
        }

        public void onRequiredOrDefault(boolean onDefault) {
            this.onRequiredOrDefault = onDefault;
        }
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(MissingClientOptionalTrait.class, configuration -> {
                NodeMapper mapper = new NodeMapper();
                Config config = mapper.deserialize(configuration, MissingClientOptionalTrait.Config.class);
                return new MissingClientOptionalTrait(config);
            });
        }
    }

    private final Config config;

    public MissingClientOptionalTrait(Config config) {
        this.config = config;
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (MemberShape member : model.getMemberShapes()) {
            if (member.hasTrait(ClientOptionalTrait.class)) {
                continue;
            }
            if (member.hasTrait(DefaultTrait.class) && config.onRequiredOrDefault) {
                events.add(danger(member, "@default members must also be marked with the @clientOptional trait"));
            }
            if (member.hasTrait(RequiredTrait.class)) {
                if (config.onRequiredOrDefault) {
                    events.add(danger(member, "@required members must also be marked with the @clientOptional trait"));
                } else if (config.onRequiredStructureOrUnion && isTargetingStructureOrUnion(model, member)) {
                    events.add(danger(member,
                            "@required members that target a structure or union must be marked with "
                                    + "the @clientOptional trait. Not using the @clientOptional trait here "
                                    + "is risky because there is no backward compatible way to replace the "
                                    + "@required trait with the @default trait if the member ever needs to "
                                    + "be made optional."));
                }
            }
        }
        return events;
    }

    private boolean isTargetingStructureOrUnion(Model model, MemberShape member) {
        Shape target = model.expectShape(member.getTarget());
        return target.isStructureShape() || target.isUnionShape();
    }
}
