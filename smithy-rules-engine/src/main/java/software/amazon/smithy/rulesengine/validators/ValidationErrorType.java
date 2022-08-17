/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.validators;

public enum ValidationErrorType {
    INCONSISTENT_PARAMETER_TYPE,
    UNSUPPORTED_PARAMETER_TYPE,
    PARAMETER_MISMATCH,
    PARAMETER_TYPE_MISMATCH,
    SERVICE_ID_MISMATCH,
    REQUIRED_PARAMETER_MISSING,
    PARAMETER_IS_NOT_USED,
    PARAMETER_IS_NOT_DEFINED,
    INVALID_URI,

    INVALID_BUILTIN
}
