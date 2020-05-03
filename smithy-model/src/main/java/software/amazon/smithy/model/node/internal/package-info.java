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

/**
 * This is an adaptation of minimal-json: https://github.com/ralfstx/minimal-json
 *
 * <p>The classes from that project are copied into this internal package
 * to allow Smithy the minimal amount of JSON parsing ability it needs to
 * read the AST model. We copied these classes in rather than use a dependency
 * to keep our external dependency count to a minimum.
 *
 * <p>A few modifications were made to the original code:
 *
 * <ul>
 *     <li>The style of the code was changed to match the rest of the project.</li>
 *     <li>There is configurable support for "//" line comments when parsing.</li>
 *     <li>Classes that aren't used were removed.</li>
 *     <li>Pretty printing was modified to use fewer classes and to accept a
 *     String rather than char[].</li>
 *     <li>All {@code Location} classes were converted to be
 *     {@link software.amazon.smithy.model.SourceLocation}</li>
 *     <li>{@link software.amazon.smithy.model.loader.ModelSyntaxException} is now
 *     thrown instead of {@code ParseException}.</li>
 *     <li>Several methods were removed from {@code JsonHandler} that weren't
 *     being used.</li>
 *     <li>A SourceLocation is now passed in all relevant JsonHandler end* methods.</li>
 * </ul>
 *
 * <p>The original copyright is as-follows:
 *
 * <pre>
 * Copyright (c) 2013, 2014 EclipseSource
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * </pre>
 */
@SmithyInternalApi
package software.amazon.smithy.model.node.internal;

import software.amazon.smithy.utils.SmithyInternalApi;
