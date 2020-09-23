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

package software.amazon.smithy.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class IoUtilsTest {
    @Test
    public void testArrayEmptyByteArray() throws Exception {
        byte[] s = IoUtils.toByteArray(new ByteArrayInputStream(new byte[0]));
        assertEquals(0, s.length);
    }

    @Test
    public void testArrayZeroByteStream() throws Exception {
        byte[] s = IoUtils.toByteArray(new InputStream() {
            @Override
            public int read() throws IOException {
                return -1;
            }
        });
        assertEquals(0, s.length);
    }

    @Test
    public void testStringEmptyByteArray() throws Exception {
        String s = IoUtils.toUtf8String(new ByteArrayInputStream(new byte[0]));
        assertEquals("", s);
    }

    @Test
    public void testStringZeroByteStream() throws Exception {
        String s = IoUtils.toUtf8String(new InputStream() {
            @Override
            public int read() throws IOException {
                return -1;
            }
        });
        assertEquals("", s);
    }

    @Test
    public void readsFromStringPath() throws Exception {
        // Windows doesn't like the result of URL#getPath, so to test this
        // we create a Path from the URI, convert that to a string, then pass
        // it to the helper method which uses Paths.get again.
        assertEquals("This is a test.\n",
                     IoUtils.readUtf8File(Paths.get(getClass().getResource("test.txt").toURI()).toString()));
    }

    @Test
    public void readsFromPath() throws URISyntaxException {
        assertEquals("This is a test.\n", IoUtils.readUtf8File(Paths.get(getClass().getResource("test.txt").toURI())));
    }

    @Test
    public void readsFromClass() {
        assertEquals("This is a test.\n", IoUtils.readUtf8Resource(getClass(), "test.txt"));
    }

    @Test
    public void readsFromClassLoader() {
        assertEquals("This is a test.\n", IoUtils.readUtf8Resource(
                getClass().getClassLoader(), "software/amazon/smithy/utils/test.txt"));
    }
}
