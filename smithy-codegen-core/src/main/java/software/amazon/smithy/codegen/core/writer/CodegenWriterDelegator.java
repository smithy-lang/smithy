/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.writer;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolDependency;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * <p>Creates and manages {@link CodegenWriter}s for files and namespaces based
 * on {@link Symbol}s created for a {@link Shape}.
 *
 * <h2>Overview</h2>
 *
 * <p>{@code CodegenWriterDelegator} is designed to generate code in files
 * returned by the {@link Symbol#getDefinitionFile()} method of a {@link Symbol}.
 * If multiple {@code Symbol}s are created that need to be defined in the same
 * file, then the {@code CodegenWriterDelegator} ensures that the state of code
 * generator associated with the file is persisted and only written when all
 * shapes have been generated.
 *
 * <p>{@code CodegenWriter}s are lazily created each time a new filename is
 * requested. If a {@code CodegenWriter} is already associated with a filename,
 * a newline (\n) is written to the file before providing access to the
 * {@code CodegenWriter}. All of the files and CodegenWriters stored in the
 * delegator are eventually written to the provided {@link FileManifest} when
 * the {@link #flushWriters()} method is called.
 *
 * <p>This class is not thread-safe.
 *
 * <h2>Extending {@code CodegenWriterDelegator}</h2>
 *
 * <p>Language-specific code generators that utilize {@link Symbol} and
 * {@link SymbolDependency} <strong>should</strong> extend both
 * {@code CodegenWriterDelegator} and {@code CodegenWriter} to implement
 * language specific functionality. Extending these classes also makes it
 * easier to create new instances of them because they will be easier to work
 * with since generic types aren't needed in concrete implementations.
 *
 * @param <T> The type of {@link CodegenWriter} to create and manage.
 * @deprecated prefer {@link software.amazon.smithy.codegen.core.WriterDelegator}.
 * This class will be removed in a future release.
 */
@SmithyUnstableApi
@Deprecated
public class CodegenWriterDelegator<T extends CodegenWriter<T, ?>> {

    private final FileManifest fileManifest;
    private final SymbolProvider symbolProvider;
    private final Map<String, T> writers = new TreeMap<>();
    private final CodegenWriterFactory<T> codegenWriterFactory;
    private String automaticSeparator = "\n";

    /**
     * @param fileManifest Where code is written when {@link #flushWriters()} is called.
     * @param symbolProvider Maps {@link Shape} to {@link Symbol} to determine the "namespace" and file of a shape.
     * @param codegenWriterFactory Factory used to create new {@link CodegenWriter}s.
     */
    public CodegenWriterDelegator(
            FileManifest fileManifest,
            SymbolProvider symbolProvider,
            CodegenWriterFactory<T> codegenWriterFactory
    ) {
        this.fileManifest = fileManifest;
        this.symbolProvider = symbolProvider;
        this.codegenWriterFactory = codegenWriterFactory;
    }

    /**
     * Gets all of the dependencies that have been registered in writers
     * created by the {@code CodegenWriterDelegator}.
     *
     * <p>This method essentially just aggregates the results of calling
     * {@link CodegenWriter#getDependencies()} of each created writer into
     * a single array.
     *
     * <p>This method may be overridden as needed (for example, to add in
     * some list of default dependencies or to inject other generative
     * dependencies).
     *
     * @return Returns all the dependencies used in each {@code CodegenWriter}.
     */
    public List<SymbolDependency> getDependencies() {
        List<SymbolDependency> resolved = new ArrayList<>();
        writers.values().forEach(s -> resolved.addAll(s.getDependencies()));
        return resolved;
    }

    /**
     * Writes each pending {@code CodegenWriter} to the {@link FileManifest}.
     *
     * <p>The {@code toString} method is called on each writer to generate
     * the code to write to the manifest.
     *
     * <p>This method clears out the managed {@code CodegenWriter}s, meaning a
     * subsequent call to {@link #getWriters()} will return an empty map.
     *
     * <p>This method may be overridden as needed.
     */
    public void flushWriters() {
        for (Map.Entry<String, T> entry : getWriters().entrySet()) {
            fileManifest.writeFile(entry.getKey(), entry.getValue().toString());
        }

        writers.clear();
    }

    /**
     * Returns an immutable {@code Map} of created {@code CodegenWriter}s.
     *
     * <p>Each map key is the relative filename where the code will be written
     * in the {@link FileManifest}, and each map value is the associated
     * {@code CodegenWriter} of type {@code T}.
     *
     * @return Returns the immutable map of files to writers.
     */
    public final Map<String, T> getWriters() {
        return Collections.unmodifiableMap(writers);
    }

    /**
     * Gets a previously created {@code CodegenWriter} or creates a new one
     * if needed.
     *
     * <p>If a writer already exists, a newline is automatically appended to
     * the writer (either a newline or whatever value was set on
     * {@link #setAutomaticSeparator}).
     *
     * @param filename Name of the file to create.
     * @param writerConsumer Consumer that is expected to write to the {@code CodegenWriter}.
     */
    public final void useFileWriter(String filename, Consumer<T> writerConsumer) {
        useFileWriter(filename, "", writerConsumer);
    }

    /**
     * Gets a previously created writer or creates a new one if needed.
     *
     * <p>If a writer already exists, a newline is automatically appended to
     * the writer (either a newline or whatever value was set on
     * {@link #setAutomaticSeparator}).
     *
     * @param filename Name of the file to create.
     * @param namespace Namespace associated with the file (or an empty string).
     * @param writerConsumer Consumer that is expected to write to the {@code CodegenWriter}.
     */
    public final void useFileWriter(String filename, String namespace, Consumer<T> writerConsumer) {
        writerConsumer.accept(checkoutWriter(filename, namespace));
    }

    /**
     * Gets or creates a writer for a {@link Shape} by converting the {@link Shape}
     * to a {@code Symbol}.
     *
     * <p>Any dependencies (i.e., {@link SymbolDependency}) required by the
     * {@code Symbol} are automatically registered with the writer.
     *
     * <p>Any imports required to declare the {@code Symbol} in code (i.e.,
     * {@link SymbolReference.ContextOption#DECLARE}) are automatically
     * registered with the writer.
     *
     * <p>If a writer already exists, a newline is automatically appended to
     * the writer (either a newline or whatever value was set on
     * {@link #setAutomaticSeparator}).
     *
     * @param shape Shape to create the writer for.
     * @param writerConsumer Consumer that is expected to write to the {@code CodegenWriter}.
     */
    public final void useShapeWriter(Shape shape, Consumer<T> writerConsumer) {
        // Checkout/create the appropriate writer for the shape.
        Symbol symbol = symbolProvider.toSymbol(shape);
        T writer = checkoutWriter(symbol.getDefinitionFile(), symbol.getNamespace());

        // Add any needed DECLARE symbols.
        writer.addImportReferences(symbol, SymbolReference.ContextOption.DECLARE);
        symbol.getDependencies().forEach(writer::addDependency);

        writer.pushState();
        writerConsumer.accept(writer);
        writer.popState();
    }

    /**
     * Sets the automatic separator that is written to a {@code CodegenWriter}
     * each time the writer is reused.
     *
     * <p>The default line separator is a newline ("\n"), but some
     * implementations may wish to use an alternative value (e.g., "\r\n") or
     * to disable the newline separator altogether by proving an empty string.
     *
     * @param automaticSeparator The non-null line separator to use.
     */
    public final void setAutomaticSeparator(String automaticSeparator) {
        this.automaticSeparator = Objects.requireNonNull(automaticSeparator);
    }

    private T checkoutWriter(String filename, String namespace) {
        String formattedFilename = Paths.get(filename).normalize().toString();
        boolean needsNewline = writers.containsKey(formattedFilename);

        T writer = writers.computeIfAbsent(formattedFilename, file -> codegenWriterFactory.apply(file, namespace));

        // Add newlines/separators between types in the same file.
        if (needsNewline) {
            writer.writeInline(automaticSeparator);
        }

        return writer;
    }
}
