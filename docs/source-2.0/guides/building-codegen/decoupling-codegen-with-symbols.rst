-------------------------------
Decoupling Codegen with Symbols
-------------------------------

:term:`Symbols` are used in Smithy code generators to refer to qualified types
in the target programming language. Symbols provide a layer of abstraction
between the code being generated and the logic used to determine:

* how shapes are named
* where types are declared and defined
* the runtime dependencies needed for a type
* the imports needed to define and reference a type


Quick Symbol example
====================

The following example uses the built-in "``T``" formatter of
`SymbolWriter <https://github.com/smithy-lang/smithy/blob/main/smithy-codegen-core/src/main/java/software/amazon/smithy/codegen/core/SymbolWriter.java>`_
to write symbols to the generated code and automatically add imports to
the file:

.. code-block:: java

    // Create MyWriter, an imaginary subclass of SymbolWriter.
    // Set the namespace of the writer to "example.foo", which internally
    // calls SymbolWriter#relativizeSymbols, passing in "example.foo".
    var writer = new MyWriter("example.foo");

    // Create an example Symbol that refers to a type in the same namespace
    // as the writer's current namespace. Setting the the namespace also
    // requires the namespace separator.
    var exampleSymbol = Symbol.builder()
        .name("Example")
        .namespace("example.foo", ".")
        .build();

    // "$T" in the following call to 'write' is replaced with just
    // the name of the Symbol ("Example") because the Symbol's
    // namespace matches the relative namespace of the writer.
    // If exampleSymbol was in a different namespace, the writer
    // would either write the fully-qualified name or would add
    // an import to the file for the symbol.
    writer.write("""
        if ($T.isEmpty()) {
            doSomething();
        }
        """,
        exampleSymbol);


Benefits of Symbols
===================

- Refactoring. Symbols make it easy to refactor how shapes are generated
  and the file location of where shapes are generated.
- Managing imports. Symbols can contain the list of other symbols
  needed in order to refer to the type. For example, if a list has
  generic type parameters (e.g., ``List<BigDecimal>``), then the symbol
  that refers to the list would also contain symbols referring to the
  generic type parameters. All the symbols needed in a generated code
  file can then be used to automatically create import statements as
  symbols are referenced by a CodeWriter.
- Managing dependencies. Symbols can define the dependencies needed by
  the generated code in order to reference the symbol. For example, if
  BigDecimal implementations are provided by a third-party library,
  then a dependency is needed by the generated code in order to
  reference BigDecimal in code. Symbols carry this information because
  they are implementations of a ``SymbolDependencyContainer``. After
  writing code to a CodeWriter, all the dependencies of each referenced
  symbol can be used to generate a dependency graph (e.g., Maven poms,
  Python setup.py files, etc.).
- Readability. Referencing symbols using ``$T`` makes the string
  templates passed to CodeWriter more succinct.


Referencing Symbols using SymbolReference
=========================================

A `SymbolReference`_ is used to refer to another Symbol. For example, a
SymbolReference is used when a Symbol uses generic type parameters. The
following example is a Symbol created for a list of
``example.foo.Example`` values in a Java-like language,
``collections.List<example.foo.Example>``:

.. code-block:: java

    // Create the "Example" Symbol used in the "List<Example>".
    var exampleSymbol = Symbol.builder()
            .name("Example")
            .namespace("example.foo", ".")
            .build();

    // Create a "List<Example>" that can be properly relativized
    // against the current namespace of the SymbolWriter.
    var listSymbol = Symbol.builder()
            .name("List")
            .namespace("collections", ".")
            // Automatically creates a SymbolReference from a Symbol.
            .addReference(exampleSymbol)
            .build();


Aliasing Symbols
----------------

An alias can be given to SymbolReference to deconflict the symbol with
other symbols already in the target namespace. For example, let's say
you need to reference a type that uses a fairly common name, so you
decide to alias the common name to something that is far less likely to
have conflicts.

The following example creates a Symbol for ``List<__Example>`` where
``__Example`` is an alias to ``com.foo.Example``:

.. code-block:: java

    var exampleSymbol = Symbol.builder()
        .name("Example")
        .namespace("example.foo", ".")
        .build();

    // Alias "Example" to "__Example".
    SymbolReference exampleReference = exampleSymbol.toReference("__Example");

    // Create a "List<__Example>" that can be properly relativized
    // against the current namespace of the SymbolWriter.
    var listSymbol = Symbol.builder()
        .name("List")
        .namespace("collections", ".")
        .addReference(exampleReference)
        .build();

When a SymbolReference is added to a Symbol, ``SymbolWriter`` will know
that the references of the Symbol need to be accounted for when writing
the symbol by importing any necessary dependencies with appropriate
aliases.

.. code-block:: java

    // Hypothetical example of managing imports and using references.
    var writer = new MyWriter("example.other.namespace");

    writer.write("""
        var list = new $T();
        """,
        listSymbol);

    assert(writer.toString().equals("""
        package example.other.namespace;

        import collections.List;
        import example.foo.Example as __Example;

        var list = new List<__Example>();
        """));


Symbol dependencies
===================

Symbols can be used to automatically generate dependency closures and
configuration files based on the symbols written to a ``SymbolWriter``.
This allows generated code to depend on only the closure of dependencies
they actually need. Codegen plugins can conditionally require runtime
dependencies in generated code (something needed by AWS SDKs to add
dependencies on AWS signature version 4 implementations, credential providers,
and other AWS-specific features).

The symbols used during codegen can be tracked using a ``WriterDelegator``,
and from these tracked symbols, the graph of referenced ``SymbolDependency``
can be written to whatever dependency manifest format is needed for the
target environment.

Dependencies are registered with a Symbol by creating a
`SymbolDependency <https://github.com/smithy-lang/smithy/blob/main/smithy-codegen-core/src/main/java/software/amazon/smithy/codegen/core/SymbolDependency.java>`_
and adding them to the Symbol via ``Symbol#addDependency``.

The following example creates a TypeScript Symbol for big decimal that
refers to a type defined in a package named ``big``:

.. code-block:: java

    // This dependency is needed in JavaScript.
    var bigRuntimeDependency = SymbolDependency.builder()
            .dependencyType("dependencies")
            .packageName("big")
            .version("^5.2.2")
            .build();

    // This dependency is needed by the TypeScript compiler.
    var bigTsDependency = SymbolDependency.builder()
            .dependencyType("devDependencies")
            .packageName("@types/big.js")
            .version("^4.0.5")
            .build();

    // Create a symbol used for big decimals in Smithy.
    var big = Symbol.builder()
            .name("Big")
            .namespace("big", "/")
            .addDependency(bigRuntimeDependency)
            .addDependency(bigTsDependency)
            .build();

As you can see in the above example, symbol dependencies can have a
dependency type that is used to classify when the dependency is needed
(see ``SymbolDependency#dependencyType``). It is common in TypeScript
libraries to need different dependencies for JavaScript code vs
TypeScript type definitions, so two dependencies were added to the
created ``big`` symbol: one that is a normal "dependencies" and one that
is a "``devDependencies``".


``SymbolDependency`` best practices
-----------------------------------

Creating a ``SymbolDependency`` in each place the dependency is needed
spreads them out all over a project, making it difficult to change
dependencies. Rather than create a ``SymbolDependency`` each time they
are needed in the code generator, a better practice is to create a
dedicated Java ``enum`` that contains each Symbol used in the project.
This enum can be referenced throughout a project, making it possible to
update version numbers in a single place.

For example:

.. code-block:: java

    /**
     * An enum of all of the built-in dependencies managed by this package.
     */
    public enum TypeScriptDependency implements SymbolDependencyContainer {

        // Conditionally added if a big decimal shape is found in a model.
        BIG_JS("dependencies", "big.js", "^5.2.2"),
        TYPES_BIG_JS("devDependencies", "@types/big.js", "^4.0.5");

        public static final String NORMAL_DEPENDENCY = "dependencies";
        public static final String DEV_DEPENDENCY = "devDependencies";
        public static final String PEER_DEPENDENCY = "peerDependencies";
        public static final String BUNDLED_DEPENDENCY = "bundledDependencies";
        public static final String OPTIONAL_DEPENDENCY = "optionalDependencies";

        public final SymbolDependency dependency;

        TypeScriptDependency(String type, String name, String version) {
            this.dependency = SymbolDependency.builder()
                    .dependencyType(type)
                    .packageName(name)
                    .version(version)
                    .build();
        }

        @Override
        public List<SymbolDependency> getDependencies() {
            return Collections.singletonList(dependency);
        }
    }

.. note::

    1. The ``enum`` implements `SymbolDependencyContainer`_, an abstraction
       for composing dependencies.
    2. This example is taken from
       `smithy-typescript <https://github.com/awslabs/smithy-typescript/blob/main/smithy-typescript-codegen/src/main/java/software/amazon/smithy/typescript/codegen/TypeScriptDependency.java>`__,
       which shows other possibilities like how to define unconditional
       dependencies that are needed by every client.


Tracking externally controlled dependencies
-------------------------------------------

A `DependencyTracker`_ can be used to track available dependencies using a
JSON file that can be then used to provide version numbers to an enum. This
can be useful if version numbers are maintained outside a code generator or
need to be translated from other formats or lock files. For example:

.. code-block:: java

    /**
     * An enum of all of the built-in dependencies managed by this package.
     */
    public enum TypeScriptDependency implements SymbolDependencyContainer {

        // Conditionally added if a big decimal shape is found in a model.
        BIG_JS("big.js"),
        TYPES_BIG_JS("@types/big.js");

        public final SymbolDependency dependency;

        TypeScriptDependency(String name) {
            this.dependency = VersionFile.VERSIONS.getByName(name);
        }

        @Override
        public List<SymbolDependency> getDependencies() {
            return Collections.singletonList(dependency);
        }

        private static final class VersionFile {
            private static final DependencyTracker VERSIONS = new DependencyTracker();
            static {
                String path = "sdkVersions.json";
                VERSIONS.addDependenciesFromJson(SdkVersion.class.getResource(path));
            }
        }
    }


Converting shapes to Symbols with ``SymbolProviders``
=====================================================

A ``SymbolProvider`` is used to convert Smithy shapes to Symbols. A
``SymbolProvider`` is the brains of a Smithy code generator; it tells
the code generator the types used to represent shapes in the model, the
dependencies needed by generated code, the filenames used to declare and
define types, and automatically ensures reserved words in the target
language are accounted for during codegen.

A selection of existing ``SymbolProviders`` can be found at:

1. TypeScript:
   https://github.com/awslabs/smithy-typescript/blob/main/smithy-typescript-codegen/src/main/java/software/amazon/smithy/typescript/codegen/SymbolVisitor.java
2. Python:
   https://github.com/awslabs/smithy-python/blob/develop/codegen/smithy-python-codegen/src/main/java/software/amazon/smithy/python/codegen/SymbolVisitor.java
3. Go:
   https://github.com/aws/smithy-go/blob/main/codegen/smithy-go-codegen/src/main/java/software/amazon/smithy/go/codegen/SymbolVisitor.java

The simplest way to implement a ``SymbolProvider`` is to also implement
``ShapeVisitor``. The basic setup will look something like this:

.. code-block:: java

    package software.amazon.smithy.python.codegen;

    import java.util.logging.Logger;
    import software.amazon.smithy.codegen.core.SymbolProvider;
    import software.amazon.smithy.model.Model;
    import software.amazon.smithy.model.shapes.ServiceShape;
    import software.amazon.smithy.model.shapes.Shape;
    import software.amazon.smithy.model.shapes.ShapeVisitor;

    final class SymbolVisitor implements SymbolProvider, ShapeVisitor<Symbol> {

        private static final Logger LOGGER = Logger.getLogger(SymbolVisitor.class.getName());

        private final Model model;
        private final MySettings settings;
        private final ServiceShape service;

        SymbolVisitor(Model model, MySettings settings) {
            this.model = model;
            this.settings = settings;
            this.service = model.expectShape(settings.getService(), ServiceShape.class);
        }

        @Override
        public Symbol toSymbol(Shape shape) {
            Symbol symbol = shape.accept(this);
            LOGGER.fine(() -> format("Creating symbol from %s: %s", shape, symbol));
            // TODO: Escape reserved words.
            return symbol;
        }

        @Override
        public Symbol structureShape(StructureShape shape) {
            String name = getDefaultShapeName(shape);

            // Generate errors differently than normal structures.
            if (shape.hasTrait(ErrorTrait.class)) {
                return createErrorStructure(shape);
            } else {
                return createNormalStructure(shape);
            }
        }

        private Symbol createErrorStructure(StructureShape shape) {
            throw new UnsupportedOperationException("Error type codegen not yet implemented");
        }

        private Symbol createNormalStructure(StructureShape shape) {
            return Symbol.builder()
                .name(name)
                // Change this to however the settings object configures the
                // target namespace and the namespace separator for the
                // language.
                .namespace(settings.getNamespace(), ".")
                // Change this to however filenames should work each generated
                // type. If this changes, then the files used to generate
                // code should automatically change too.
                .definitionFile("models/" + name + ".xyz")
                .build();
        }

        private String getDefaultShapeName(Shape shape) {
            // Use the service-aliased name and ensure it's capitalized.
            return StringUtils.capitalize(shape.getId().getName(service));
        }

        // TODO implement other ShapeVisitor methods.
    }


Automatically handling reserved words
-------------------------------------

Smithy code generators are expected to generate valid code for the
:term:`target environment`. Service teams defining Smithy models should not
need to know the intricacies of how Smithy models are converted to every
programming language. Instead, Smithy code generators should ensure that
reserved words in a target environment are not used by the ``SymbolProvider``.
The ``smithy-codegen-core`` library provides several abstractions for handling
reserved words. These abstractions should be used in your ``SymbolProvider``.

The primary abstraction is the `ReservedWords`_ interface. The
`ReservedWordsBuilder`_ class provides a convenient way to build an instance
of ``ReservedWords``. These ``ReservedWords`` instances should be
integrated into your ``SymbolProvider`` by passing the created names,
namespaces, and member names through the appropriate escaper. For
example:

.. code-block:: java
    :emphasize-lines: 4,8-11,18

    final class SymbolVisitor implements SymbolProvider, ShapeVisitor<Symbol> {

        // ... other properties
        private final ReservedWords escaper;

        SymbolVisitor(Model model, MySettings settings) {
            // ... other setup
            this.escaper = new ReservedWordsBuilder()
                .put("function", service.getId().getName() + "Function")
                .put("throw", service.getId().getName() + "Throw")
                .build();
        }

        // other methods...

        private String getDefaultShapeName(Shape shape) {
            String name = StringUtils.capitalize(shape.getId().getName(service));
            return escaper.escape(name);
        }
    }

While you can manually define the mapping for each reserved word, a
simpler method is to create an algorithm for automatically handling
reserved words. This can be done by creating a newline delimited file
that contains each reserved word and a ``Function<String, String>`` that
takes a reserved word and returns an escaped word. For example, given
the following file named *reservedwords.txt*:

.. code-block:: none

    function
    throw

``ReservedWordsBuilder`` can be configured to escape words using the
file and your escaping function.

.. code-block:: java

    Function<String, String> escaper = word -> {
        // Returns something like "MyServiceFunction".
        return service.getId().getName() + StringUtils.capitalize(word);
    });

    URL wordsFile = getClass().getResource("reservedwords.txt");

    ReservedWords escaper = new ReservedWordsBuilder()
        .loadWords(wordsFile, escaper)
        .build();

Reserved words handling should be as granular as possible. If a symbol
is only reserved in certain contexts, then that word should only be
treated as reserved in that context. This might require the use of
multiple instances of ``ReservedWords``.

.. code-block:: java

    var memberNameEscaper = new ReservedWordsBuilder()
        .loadWords(memberNameWordsFile, escaper)
        .build();

    var classNameEscaper = new ReservedWordsBuilder()
        .loadWords(classNameWordsFile, escaper)
        .build();

Reserved words handling is case-sensitive by default. You can use
reserved words file case insensitively using
``ReservedWordsBuilder#loadCaseInsensitiveWords``.

.. code-block:: java

    var escaper = new ReservedWordsBuilder()
        .loadCaseInsensitiveWords(wordsFile, escaper)
        .build();


Composing ``SymbolProviders``
-----------------------------

``SymbolProvider`` has a very simple interface, making it easy to
compose functionality using decorators. Decorators can be used to do
things like add caching or add more contextual data to Symbols.

The following example decorates a ``SymbolProvider`` by adding caching
of resolved Symbols:

.. code-block:: java

    var cachedProvider = SymbolProvider.caching(mySymbolProvider);

The following example creates a decorator that adds a "shape" property
to every Symbol:

.. code-block:: java

    final class MyCodegenPlugin {
        static SymbolProvider wrapSymbolProvider(SymbolProvider delegate) {
            return shape -> {
                return delegate.toSymbol(shape).toBuilder()
                        .putProperty("shape", shape)
                        .build();
            };
        }
    }

    var wrapped = MyCodegenPlugin.wrapSymbolProvider(mySymbolProvider);


Integrating Symbols into your ``SymbolWriter``
==============================================

``SymbolWriter`` provides some building blocks to help integrate Symbols
into a particular programming language, but the actual gluing together
of abstractions, generating import statements, generating dependencies,
accounting for aliasing, etc. is an exercise left to each language
implementation of Smithy.


Create an ``ImportContainer`` for your language
-----------------------------------------------

An `ImportContainer`_ is used to track the imports associated with a
specific file being generated. Each time a ``Symbol`` is written to a
`SymbolWriter`_, and each call to methods like ``SymbolWriter#addImport``,
``Symbol``\ s are sent to the ``ImportContainer`` owned by the
``SymbolWriter``. The ``ImportContainer`` should be aware of the current
namespace in use by the ``SymbolWriter``.

The following example implements a simple ``ImportContainer`` for a made
up language. If a provided ``Symbol`` is in the same namespace that the
container is tracking, the import is discarded. Otherwise, each import
is added to a map of namespaces to a map of alias → target name.

.. code-block:: java

    final class MyLangImports implements ImportContainer {
        private final Map<String, Map<String, String>> imports = new TreeMap<>();
        private final MyLangSettings settings;
        private final String namespace;

        MyLangImports(MyLangSettings settings, String namespace) {
            this.settings = settings;
            this.namespace = namespace;
        }

        @Override
        public void importSymbol(Symbol symbol, String alias) {
            var symbolNamespace = symbol.getNamespace();

            // Only import symbols in other namespaces.
            if (!symbolNamespace.equals(namespace)) {
                var namespaceImports = imports.computeIfAbsent(symbolNamespace, ns -> new TreeMap<>());
                namespaceImports.put(alias, symbol.getName());
            }
        }

        @Override
        public String toString() {
            if (imports.isEmpty()) {
                return "";
            }

            // Build up each line of import statements.
            var builder = new StringBuilder();

            for (var entry : imports.entrySet()) {
                var ns = entry.getKey();
                var alias = entry.getValue().getKey();
                var target = entry.getValue().getValue();
                builder.append("import ").append(target);

                // Use a made up aliasing syntax if the alias differs from the target.
                if (!alias.equals(target)) {
                    builder.append(" as ").append(alias)
                }

                // Import from a target namespace.
                builder.append(" from ").append(ns);
            }

            builder.append("\n");
            return builder.toString();
        }
    }

``ImportContainer`` implements ``toString`` so that ``SymbolWriter`` can
write out imports before writing out the rest of the code.


Create a ``SymbolWriter`` subclass
----------------------------------

Each language should create a subclass of `SymbolWriter`_ that
automatically manages imports, symbols, and writes documentation
strings.

The following example shows how a subclass of ``SymbolWriter`` can be
created.

.. code-block:: java

    public final class MyWriter extends SymbolWriter<MyWriter, MyImportContainer> {

        public MyWriter(String namespace) {
            super(new MyImportContainer(namespace));

            // Write Symbols relative to the current namespace.
            setRelativizeSymbols(namespace);
        }

        @Override
        public String toString() {
            // You can override how code is converted to a string. For example,
            // this allows you to add a prelude to generated code or to write the
            // necessary imports that were used in the writer.
            return getImportContainer().toString() + "\n\n" + super.toString();
        }

        public MyWriter someCustomMethod() {
            // You can implement custom methods that are specific to whatever
            // language you're implementing a generator for.
            return this;
        }
    }


Use ``WriterDelegator`` to create writers
-----------------------------------------

In order to track the dependencies used while generating code, and to
add code interceptors to each created ``SymbolWriter``, code generators
should use a `WriterDelegator`_ to create ``SymbolWriters``.

A ``WriterDelegator`` is used to create and track all the
``SymbolWriters`` used during code generation. A codegen project will
generally use a single ``WriterDelegator`` during codegen. You need to
ensure your ``WriterDelegator`` knows about the code interceptors returned
from ``SmithyIntegration``\s so that it can apply transformations as code
is written.

Let's say you need to generate code for a structure shape. You ask the
``WriterDelegator`` to give you the appropriate ``SymbolWriter``:

.. code-block:: java

    delegator.useShapeWriter(shape, writer -> {
        writer.write("Structure $L", shape.getId());
    });

``WriterDelegator`` will create the appropriate ``SymbolWriter`` that
writes to the correct file location based on the ``Symbol`` created for
the given shape. If multiple shapes use the same filename, then
``WriterDelegator`` will provide the same ``SymbolWriter`` to each call
to ``useShapeWriter``, and it will automatically inject ``\n`` prior to
vending a previously used writer (this can be customized).

Use ``useFileWriter`` to write to a file that isn't specific to a shape:

.. code-block:: java

    delegator.useFileWriter("README.md", writer -> {
        writer.write("""
            # This is my README!

            Do you like it?
            """);
    });

When codegen has completed, the generator needs to call ``flushWriters``
on the delegator to write each created ``SymbolWriter`` to the
``FileManifest`` the generator is using:

.. code-block:: java

    delegator.flushWriters();

All the symbol dependencies detected when using each ``SymbolWriter``
can be retrieved from the delegator using ``getDependencies``.

.. code-block:: java

    List<SymbolDependency> dependencies = delegator.getDependencies();

These dependencies can then be used to generate things like dependency
manifests for the created code.


FAQ
===

How do I add more information to ``Symbols``, ``SymbolReferences``, and ``SymbolDependencies``?
-----------------------------------------------------------------------------------------------

Use typed property bags to store additional information. For example:

.. code-block:: java

    Symbol foo = Symbol.builder()
        .name("Foo")
        .namespace("example.foo", ".")
        .putProperty("customData", "hello")
        .build();

    String customData = foo.getProperty("customData", String.class);

You can add properties to an existing ``Symbol``, ``SymbolReference``,
or ``SymbolDependency`` by calling ``toBuilder`` first:

.. code-block:: java

    foo = foo.toBuilder()
        .putProperty("anotherProperty", true)
        .build();


Does ``SymbolWriter`` require one namespace per file?
-----------------------------------------------------

No, but that's the easiest way to use ``SymbolWriter``. Your language's
subclass can be setup in a way that it uses multiple ``ImportContainer``
instances per/namespace in a single file. For example, an ``ImportContainer``
could be given the current namespace of a ``SymbolWriter`` each time it's
invoked, allowing the ``ImportContainer`` to perform more targeted
relativization. Then the ``ImportContainer`` would need special methods
used to convert each nested namespace's imports to a string. It's an
abstract exercise left up to the implementation.


.. _SymbolReference: https://github.com/smithy-lang/smithy/blob/main/smithy-codegen-core/src/main/java/software/amazon/smithy/codegen/core/SymbolReference.java
.. _SymbolDependencyContainer: https://github.com/smithy-lang/smithy/blob/main/smithy-codegen-core/src/main/java/software/amazon/smithy/codegen/core/SymbolDependencyContainer.java
.. _DependencyTracker: https://github.com/smithy-lang/smithy/blob/main/smithy-codegen-core/src/main/java/software/amazon/smithy/codegen/core/DependencyTracker.java
.. _ReservedWords: https://github.com/smithy-lang/smithy/blob/main/smithy-codegen-core/src/main/java/software/amazon/smithy/codegen/core/ReservedWords.java
.. _ReservedWordsBuilder: https://github.com/smithy-lang/smithy/blob/main/smithy-codegen-core/src/main/java/software/amazon/smithy/codegen/core/ReservedWordsBuilder.java
.. _ImportContainer: https://github.com/smithy-lang/smithy/blob/main/smithy-codegen-core/src/main/java/software/amazon/smithy/codegen/core/ImportContainer.java
.. _SymbolWriter: https://github.com/smithy-lang/smithy/blob/main/smithy-codegen-core/src/main/java/software/amazon/smithy/codegen/core/SymbolWriter.java
.. _WriterDelegator: https://github.com/smithy-lang/smithy/blob/main/smithy-codegen-core/src/main/java/software/amazon/smithy/codegen/core/WriterDelegator.java
