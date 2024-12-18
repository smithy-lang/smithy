/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Creates a DANGER event when a structure or union member is added
 * anywhere other than the end of the previous definition or the
 * member order is changed.
 */
public final class ChangedMemberOrder extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        Stream<ChangedShape<?>> changes = Stream.concat(
                differences.changedShapes(StructureShape.class),
                differences.changedShapes(UnionShape.class));

        return changes
                .filter(diff -> isUnordered(diff.getOldShape().members(), diff.getNewShape().members()))
                .map(diff -> danger(diff.getNewShape(),
                        String.format(
                                "%s shape members were reordered. This can cause ABI compatibility issues in languages "
                                        + "like C and C++ where the layout and alignment of a data structure matters.",
                                diff.getOldShape().getType())))
                .collect(Collectors.toList());
    }

    private static boolean isUnordered(Collection<MemberShape> a, Collection<MemberShape> b) {
        Iterator<MemberShape> aIter = a.iterator();
        Iterator<MemberShape> bIter = b.iterator();

        while (aIter.hasNext()) {
            // If members were removed, then this check is satisfied (though there are
            // other backward incompatible changes that other evaluators will detect).
            if (!bIter.hasNext()) {
                break;
            }

            String oldMember = aIter.next().getMemberName();
            String newMember = bIter.next().getMemberName();
            if (!oldMember.equals(newMember)) {
                return true;
            }
        }

        return false;
    }
}
