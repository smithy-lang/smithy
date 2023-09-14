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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
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
        assertEquals("This is a test." + System.lineSeparator(),
                     IoUtils.readUtf8File(Paths.get(getClass().getResource("test.txt").toURI()).toString()));
    }

    @Test
    public void readsFromPath() throws URISyntaxException {
        assertEquals("This is a test." + System.lineSeparator(),
                IoUtils.readUtf8File(Paths.get(getClass().getResource("test.txt").toURI())));
    }

    @Test
    public void readsFromClass() {
        assertEquals("This is a test." + System.lineSeparator(),
                IoUtils.readUtf8Resource(getClass(), "test.txt"));
    }

    @Test
    public void readsFromClassLoader() {
        assertEquals("This is a test." + System.lineSeparator(), IoUtils.readUtf8Resource(
                getClass().getClassLoader(), "software/amazon/smithy/utils/test.txt"));
    }

    @Test
    public void throwsWhenProcessFails() {
        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> {
            IoUtils.runCommand("thisCommandDoesNotExist" + new Random().nextInt(1000));
        });

        assertThat(e.getMessage(), containsString("failed with exit code"));
    }

    @Test
    public void doesNotThrowWhenGivenOutput() {
        StringBuilder sb = new StringBuilder();
        String name = "thisCommandDoesNotExist" + new Random().nextInt(1000);
        int code = IoUtils.runCommand(name, Paths.get(System.getProperty("user.dir")), sb);

        assertThat(code, not(0));
        assertThat(sb.toString(), not(emptyString()));
    }

    @Test
    public void deletesDirectories() throws IOException {
        Path path = Files.createTempDirectory("delete_empty_dir");
        Files.write(path.resolve("foo"), "hello".getBytes(StandardCharsets.UTF_8));
        Path nested = path.resolve("a").resolve("b");
        Files.createDirectories(nested);
        Files.write(nested.resolve("baz"), "hello".getBytes(StandardCharsets.UTF_8));

        assertThat(Files.exists(path), is(true));
        assertThat(Files.exists(nested), is(true));

        IoUtils.rmdir(path);

        assertThat(Files.exists(path), is(false));
        assertThat(Files.exists(nested), is(false));
    }

    @Test
    public void rmDirIgnoresIfNotExists() throws IOException {
        Path path = Files.createTempDirectory("delete_empty_dir");
        Files.delete(path);

        assertThat(IoUtils.rmdir(path), is(false));
    }

    @Test
    public void rmDirFailsWhenNotDir() throws IOException {
        Path path = Files.createTempFile("foo", ".baz");

        Assertions.assertThrows(IllegalArgumentException.class, () -> IoUtils.rmdir(path));

        Files.delete(path);
    }
}
