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

package software.amazon.smithy.model.loader;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.DefaultNodeFactory;

/**
 * Loads the contents of a file if the file has a source location
 * of "N/A", isn't empty, and the first character is "{".
 */
public final class DefaultingJsonModelLoader extends NodeModelLoader {
    public DefaultingJsonModelLoader() {
        super(new DefaultNodeFactory());
    }

    @Override
    protected boolean test(String path, String contents) {
        return path.equals(SourceLocation.NONE.getFilename())
               && !contents.isEmpty()
               && contents.charAt(0) == '{';
    }
}
