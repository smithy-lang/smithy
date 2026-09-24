/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.shapes.MemberShape;

/**
 * The result of {@link ResourceMemberInference#inferByName} matching a structure's members to a
 * resource's identifiers or properties by name.
 */
final class MemberInferenceResult {
    // Matched identifier or property name to the structure member that provides it.
    final Map<String, MemberShape> matched = new LinkedHashMap<>();
    // Members that matched no identifier or property name and are not @notProperty.
    final List<MemberShape> unmatched = new ArrayList<>();
}
