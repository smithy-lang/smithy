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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class ReservedWordsTest {
    @Test
    public void composesImplementations() {
        ReservedWords a = new ReservedWordsBuilder().put("void", "_void").build();
        ReservedWords b = new ReservedWordsBuilder().put("foo", "_foo").build();
        ReservedWords composed = ReservedWords.compose(a, b);

        assertThat(composed.isReserved("void"), is(true));
        assertThat(composed.isReserved("foo"), is(true));
        assertThat(composed.isReserved("Void"), is(false));

        assertThat(composed.escape("void"), equalTo("_void"));
        assertThat(composed.escape("foo"), equalTo("_foo"));
        assertThat(composed.escape("pass"), equalTo("pass"));
    }

    @Test
    public void identityImplementation() {
        ReservedWords reservedWords = ReservedWords.identity();

        assertThat(reservedWords.isReserved("void"), is(false));
        assertThat(reservedWords.escape("void"), equalTo("void"));
    }
}
