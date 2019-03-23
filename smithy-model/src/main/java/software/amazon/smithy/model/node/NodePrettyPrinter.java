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

package software.amazon.smithy.model.node;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;

class NodePrettyPrinter extends DefaultPrettyPrinter {
    NodePrettyPrinter() {
        this._arrayIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
        this._objectIndenter = DefaultIndenter.SYSTEM_LINEFEED_INSTANCE;
    }

    @Override
    public NodePrettyPrinter withSeparators(Separators separators) {
        super.withSeparators(separators);
        this._objectFieldValueSeparatorWithSpaces = separators.getObjectFieldValueSeparator() + " ";
        return this;
    }
}
