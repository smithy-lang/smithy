/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.diff;

import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;

/**
 * Represents a change in metadata.
 */
public final class ChangedMetadata implements FromSourceLocation {
    private final String key;
    private final Node oldValue;
    private final Node newValue;

    ChangedMetadata(String key, Node oldValue, Node newValue) {
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Gets the key of the metadata that changed.
     *
     * @return Returns the changed key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the old metadata value for this key.
     *
     * @return Returns the value in the old model.
     */
    public Node getOldValue() {
        return oldValue;
    }

    /**
     * Gets the new metadata value for this key.
     *
     * @return Returns the value in the new model.
     */
    public Node getNewValue() {
        return newValue;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return getNewValue().getSourceLocation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ChangedMetadata)) {
            return false;
        } else {
            ChangedMetadata that = (ChangedMetadata) o;
            return getKey().equals(that.getKey())
                   && Objects.equals(getOldValue(), that.getOldValue())
                   && Objects.equals(getNewValue(), that.getNewValue());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getOldValue(), getNewValue());
    }
}
