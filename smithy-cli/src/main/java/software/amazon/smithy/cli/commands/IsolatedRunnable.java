/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.cli.commands;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Consumer;
import software.amazon.smithy.cli.CliError;

final class IsolatedRunnable implements Runnable {

    private final ClassLoader classLoader;
    private final Consumer<ClassLoader> consumer;

    IsolatedRunnable(Collection<Path> artifacts, ClassLoader parent, Consumer<ClassLoader> consumer) {
        this(createClassLoaderFromPaths(artifacts, parent), consumer);
    }

    private IsolatedRunnable(ClassLoader classLoader, Consumer<ClassLoader> consumer) {
        this.classLoader = classLoader;
        this.consumer = consumer;
    }

    private static ClassLoader createClassLoaderFromPaths(Collection<Path> artifacts, ClassLoader parent) {
        return new URLClassLoader(createUrlsFromPaths(artifacts), parent);
    }

    private static URL[] createUrlsFromPaths(Collection<Path> paths) {
        URL[] urls = new URL[paths.size()];
        int i = 0;
        for (Path artifact : paths) {
            try {
                urls[i++] = artifact.toUri().toURL();
            } catch (MalformedURLException e) {
                throw new CliError("Error creating class loader: " + artifact);
            }
        }

        return urls;
    }

    @Override
    public void run() {
        try {
            Thread thread = new Thread(() -> consumer.accept(classLoader));
            thread.setContextClassLoader(classLoader);
            ExceptionHandler handler = new ExceptionHandler();
            thread.setUncaughtExceptionHandler(handler);
            thread.start();
            thread.join();
            if (handler.e != null) {
                throw new CliError(handler.e.getMessage(), 1, handler.e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CliError(e.getMessage(), 1, e);
        }
    }

    private static final class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        volatile Throwable e;

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            this.e = e;
        }
    }
}
