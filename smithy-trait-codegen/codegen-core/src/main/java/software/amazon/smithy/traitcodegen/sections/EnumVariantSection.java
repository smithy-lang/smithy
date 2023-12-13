/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.sections;

import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.utils.CodeSection;

public final class EnumVariantSection implements CodeSection {
    private final MemberShape memberShape;

    public EnumVariantSection(MemberShape memberShape) {
        this.memberShape = memberShape;
    }

    public MemberShape memberShape() {
        return memberShape;
    }
}
