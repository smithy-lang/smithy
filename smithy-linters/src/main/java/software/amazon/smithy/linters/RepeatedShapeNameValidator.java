/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.linters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;

/**
 * Validates that structure members and union member names do not
 * repeat their shape name as prefixes of their member or tag names.
 */
public final class RepeatedShapeNameValidator extends AbstractValidator {

    public static final class Config {
        private boolean exactMatch = false;

        public boolean getExactMatch() {
            return exactMatch;
        }

        public void setExactMatch(boolean exactMatch) {
            this.exactMatch = exactMatch;
        }
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(RepeatedShapeNameValidator.class, node -> {
                Config config = new NodeMapper().deserialize(node, Config.class);
                return new RepeatedShapeNameValidator(config);
            });
        }
    }

    private final Config config;

    private RepeatedShapeNameValidator(Config config) {
        this.config = config;
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        model.shapes(StructureShape.class)
                .forEach(shape -> events.addAll(validateNames(model, shape, shape.getMemberNames())));
        model.shapes(UnionShape.class)
                .forEach(shape -> events.addAll(validateNames(model, shape, shape.getMemberNames())));
        return events;
    }

    private List<ValidationEvent> validateNames(Model model, Shape shape, Collection<String> memberNames) {
        String shapeName = shape.getId().getName();
        String lowerCaseShapeName = shapeName.toLowerCase(Locale.US);
        return memberNames.stream()
                .filter(memberName -> nameConflicts(lowerCaseShapeName, memberName))
                .map(memberName -> repeatedMemberName(model, shape, shapeName, memberName))
                .collect(Collectors.toList());
    }

    private boolean nameConflicts(String lowerCaseShapeName, String memberName) {
        String lowerCaseMemberName = memberName.toLowerCase(Locale.US);
        if (config.getExactMatch()) {
            return lowerCaseMemberName.equals(lowerCaseShapeName);
        } else {
            return lowerCaseMemberName.startsWith(lowerCaseShapeName);
        }
    }

    private ValidationEvent repeatedMemberName(Model model, Shape shape, String shapeName, String memberName) {
        Shape member = model.expectShape(shape.getId().withMember(memberName));
        if (config.getExactMatch()) {
            return warning(member,
                    String.format(
                            "The `%s` %s shape repeats its name in the member `%s`; %2$s member names should not be "
                                    + "equal to the %2$s name.",
                            shapeName,
                            shape.getType(),
                            memberName));
        } else {
            return warning(member,
                    String.format(
                            "The `%s` %s shape repeats its name in the member `%s`; %2$s member names should not be "
                                    + "prefixed with the %2$s name.",
                            shapeName,
                            shape.getType(),
                            memberName));
        }
    }
}
