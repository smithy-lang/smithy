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

package software.amazon.smithy.codegen.core;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

/**
 * This is deprecated and will be removed in 0.10.0.
 */
@FunctionalInterface
@Deprecated
public interface TemplateEngine {
    void write(String templatePath, Writer out, Map<String, Object> dataModel);

    default String render(String templatePath, Map<String, Object> dataModel) {
        StringWriter writer = new StringWriter();
        write(templatePath, writer, dataModel);
        return writer.toString();
    }

    default String render(String templatePath) {
        return render(templatePath, Collections.emptyMap());
    }
}
