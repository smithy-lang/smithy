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

import java.util.Objects;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class ValidationError {
    private final ValidationErrorType validationErrorType;
    private final String error;
    private final SourceLocation sourceLocation;

    public ValidationError(ValidationErrorType validationErrorType, String error, SourceLocation sourceLocation) {
        this.validationErrorType = validationErrorType;
        this.error = error;
        this.sourceLocation = sourceLocation;
    }

    public ValidationErrorType validationErrorType() {
        return validationErrorType;
    }

    public String error() {
        return error;
    }

    public SourceLocation sourceLocation() {
        return sourceLocation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(validationErrorType, error, sourceLocation);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        ValidationError that = (ValidationError) obj;
        return Objects.equals(this.validationErrorType, that.validationErrorType)
               && Objects.equals(this.error, that.error)
               && Objects.equals(this.sourceLocation, that.sourceLocation);
    }

    @Override
    public String toString() {
        return this.validationErrorType + ", " + this.error + System.lineSeparator() + this.sourceLocation;
    }

}
