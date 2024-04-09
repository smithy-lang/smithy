/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.pattern.SmithyPattern;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that at most one greedy label is present in the pattern, and, if any greedy label is present that it's
 * the last label in the pattern. This validation emits DANGER events which can be suppressed if the server allows
 * any of these. Some servers do, but most don't.
 */
public class HttpUriGreedyLabelValidator extends AbstractValidator {

    private static final String MULTIPLE_GREEDY_LABELS = "MultipleGreedyLabels";
    private static final String GREEDY_LABEL_IS_NOT_LAST_LABEL = "GreedyLabelIsNotLastLabel";

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (Shape shape : model.getShapesWithTrait(HttpTrait.class)) {
            HttpTrait trait = shape.expectTrait(HttpTrait.class);
            UriPattern pattern = trait.getUri();

            List<SmithyPattern.Segment> segments = pattern.getSegments();
            // Make sure at most one greedy label exists, and that it is the
            // last label segment.
            for (int i = 0; i < segments.size(); i++) {
                SmithyPattern.Segment s = segments.get(i);
                if (s.isGreedyLabel()) {
                    for (int j = i + 1; j < segments.size(); j++) {
                        if (segments.get(j).isGreedyLabel()) {
                            events.add(danger(shape, trait,
                                              "At most one greedy label segment may exist in a pattern: " + pattern,
                                              MULTIPLE_GREEDY_LABELS));
                        }
                        if (segments.get(j).isLabel()) {
                            events.add(danger(shape, trait,
                                              "A greedy label must be the last label in its pattern: " + pattern,
                                              GREEDY_LABEL_IS_NOT_LAST_LABEL));
                        }
                    }
                }
            }
        }
        return events;
    }
}
