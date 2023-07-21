------------------------
Making Codegen Pluggable
------------------------

This document describes various code generation and runtime concepts
that can be used to make Smithy code generators open to extension.


Why make codegen extensible?
============================

Smithy code generators need to be extensible so that optional features
can be contributed to augment generated code. For example, Smithy code
generators can generate generic clients that know how to send requests
to an endpoint, but AWS SDK code generators resolve endpoints based on
other configuration settings like regions. Smithy code generators should
have no built-in concept of "region", and instead they should rely on
codegen *integrations* that can augment generated code based on the
presence of traits and configuration found in smithy-build.json files.


Integrations
============

*Integrations* are the primary abstraction used to customize Smithy code
generators. Integrations are found on the classpath using
Java Service Provider Interfaces (:term:`SPI`) and are used to customize
Smithy code generators.


What can integrations customize?
--------------------------------

Various aspects of a Smithy code generator can be customized. For example:

- Generate custom files like licenses, readmes, etc.
- Preprocess the model (e.g., validate that the model uses only features
  supported by the generator, remove unsupported features, add codegen
  specific traits, etc)
- Add parameters used to configure a client (e.g., constructor
  arguments, builder parameters, etc)
- Inject interceptors into the client automatically (based on traits or
  opt-in flags)
- Inject custom client or server HTTP request headers
- Contribute available protocol implementations that a generator can
  choose from when generating clients or servers
- Contribute authentication scheme implementations that a generator can
  choose from when generating clients or servers
- Intercept and modify sections of generated code (this feature is part
  of :term:`AbstractCodeWriter`)
- Add dependencies either unconditionally or based on the presence of
  shapes and traits in the model
- Modify the :term:`SymbolProvider` used to convert shapes into code
  (e.g., add custom reserved words, change how names are generated, etc.)
- Add custom retry-strategies


Only customize through opt-in
-----------------------------

Simply finding an integration on the classpath should not enable the
integration. Integrations should only be enabled through opt-in signals.
Traits found in the model and feature configuration in smithy-build.json
are used to enable customizations performed by integrations.


Creating a ``SmithyIntegrations``
=================================

Smithy codegen provides a pre-built integration interface,
`SmithyIntegration <https://github.com/smithy-lang/smithy/blob/main/smithy-codegen-core/src/main/java/software/amazon/smithy/codegen/core/SmithyIntegration.java>`__,
that *should* be used by every Smithy code generator. Using this
standardized interface ensures all code generators follow the same basic
framework and makes it easier to contribute features that span multiple
code generators.

``SmithyIntegration`` requires a few generic type parameters:

.. code-block:: java

    SmithyIntegration<S,
                      W extends SymbolWriter<W, ?>,
                      C extends CodegenContext<S, W, ?>>

-  ``S``: The settings object used to configure the code generator. This
   object should be a basic POJO or `Java
   record <https://docs.oracle.com/en/java/javase/14/language/records.html>`__
   that captures the same properties used by smithy-build.json files to
   configure the generator. For example, this object might contain the
   service shape ID being generated, a specific protocol shape ID to
   generate, the code generation mode (client or server), etc.
-  ``W``: The specific subclass of ``SymbolWriter`` that is used by the
   code generator. For example, if generating Python code, you should
   create a ``PythonWriter`` and supply that as the type parameter.
-  ``C``: The ``CodegenContext`` object used by the generator. This type
   depends on codegen settings. It provides integration methods access
   to the model being generated, the settings object, the
   ``SymbolProvider`` used to convert shapes to ``Symbol``\ s, and a
   ``FileManifest`` that's used to write files to disk. Each
   implementation is expected to create a specific subtype of
   ``CodegenContext``.

Example of a custom ``SmithyIntegration`` for Python:

.. code-block:: java

    interface PythonIntegration extends
            SmithyIntegration<PythonSettings, PythonWriter, PythonContext> {}

Example codegen settings type:

.. code-block:: java

    record PythonSettings(ShapeId service, ShapeId protocol);
    // A builder pattern could be applied later if the number of arguments grows.

Example of a custom ``CodegenContext``:

.. code-block:: java

    record PythonContext(
        Model model,
        PythonSettings settings,
        SymbolProvider symbolProvider,
        FileManifest fileManifest,
        WriterDelegator<PythonWriter> writerDelegator,
        List<PythonIntegration> integrations
    ) implements CodegenContext<PythonSettings, PythonWriter, PythonIntegration> {}

This integration is then implemented to implement customizations:

.. code-block:: java

    public final class AddCodeLicense implements PythonIntegration {
        // implement overrides, detailed below
    }


Identifying integrations
------------------------

Integrations are identified using the ``SmithyIntegration#name()``
method. This method will return the canonical class name of the
integration by default, but it can be overridden to provide a different
name. Note that naming conflicts between integrations are not allowed.


How integrations are ordered
----------------------------

Integrations are ordered using a kind of priority ordered dependency
graph. Integrations can specify that they should be applied before other
integrations by name and/or after other integrations by name. The
following example states that the integration needs to run before
``"Foo"`` but after ``"CodeLicenseHeader"``:

.. code-block:: java

    @Override
    public List<String> runBefore() {
        return List.of("Foo");
    }

    @Override List<String> runAfter() {
        return List.of("CodeLicenseHeader");
    }

In rare cases, you might need more granular control over the order of
an integration. A priority can be provided to influence when the integration
is applied relative to other integrations when their dependencies are
resolved. The higher the priority, the earlier an integration is applied.

.. code-block:: java

    @Override
    public byte priority() {
        return 64;
    }

.. tip::

    :ref:`directedcodegen` automatically handles finding integrations on
    the classpath and topologically ordering them.


Preprocessing models
--------------------

A common requirement of code generators is to preprocess the model. For
example, a generator that doesn't support :ref:`event streams <event-streams>`
might want to filter out event stream operations and emit warnings.
A generator could also choose to apply synthetic traits (traits that are not
persisted when the model is serialized) to shapes in the model as part of
their code generation strategy.

The model can be preprocessed by implementing the
``SmithyIntegration#preprocessModel`` method and returning an updated
model.

.. code-block:: java

    @Override
    Model preprocessModel(Model model, PythonSettings settings) {
        // Perform some transformation and return the updated model.
        return model;
    }

.. seealso:: :ref:`codegen-transforming-the-model`


Changing how shapes are named or how files are generated
--------------------------------------------------------

Another requirement when generating code might be to change the strategy
used for naming shapes, the file location of shapes, or just adding
metadata to each :term:`Symbol` created by a :term:`SymbolProvider`. This can
be achieved by implementing ``SmithyIntegration#decorateSymbolProvider``:

.. code-block:: java

    @Override
    public SymbolProvider decorateSymbolProvider(Model model, PythonSetting settings, SymbolProvider symbolProvider) {
        // Decorate the symbol provider and add a "foo" property to every symbol.
        return shape -> symbolProvider.toSymbol(shape)
                .toBuilder()
                .putProperty("foo", "hello!")
                .build());
    }


.. _codegen-intercepting:

Intercepting and updating sections of code
------------------------------------------

Code generators can designate sections of code that can be modified
by integrations. This feature allows integrations to do things like add
text to every code file (for example a license header), apply
annotations to generated types, change the type signature of a class,
change how classes are created, etc. Implementations of ``CodeInterceptors``
registered with ``SmithyIntegration``\ s must be added to each code
writer created during code generation.

Let's say you wanted to emit a customizable section in generated code
where headers for the code could be modified to add a custom license
header or disclaimer that the code is generated. This can be achieved by
first creating an implementation of ``CodeSection``. We'll call it
``CodeHeader``:

.. code-block:: java

    // This event does not need any properties.
    record CodeHeader() implements CodeSection;

When generating code, inject the section at the start of each file:

.. code-block:: java

    mywriter.injectSection(new CodeHeader());

``injectSection`` is used because this section of code is empty by default.
If the section should have content by default, then use ``pushSection`` and
``popSection``:

.. code-block:: java

    mywriter.pushSection(new CodeHeader());
    mywriter.write("// This is generated code");
    mywriter.popSection();

The call to ``injectSection`` implicitly calls ``popSection``. When
``popSection`` is called, the code that was written during that section is
sent to each matching interceptor so that they can prepend to the contents,
append to the contents, or completely rewrite the contents.

``CodeInterceptor``\ s can be registered to append to this section by
returning interceptors from ``SmithyIntegration#interceptors``:

.. code-block:: java

    @Override
    public List<? extends CodeInterceptor<? extends CodeSection, PythonWriter>>
            interceptors(C codegenContext) {
        return List.of(new CodeHeaderInterceptor());
    }

Interceptors should be created as a dedicated class. The following interceptor
appends to the existing content in the section:

.. code-block:: java

    final class CodeHeaderInterceptor extends CodeInterceptor.Appender<CodeHeader, PythonWriter> {
        @Override
        public Class<CodeHeader> sectionType() {
            return CodeHeader.class;
        }

        @Override
        public void append(PythonWriter writer, CodeHeader section) {
            writer.write("""
                /*
                 * Copyright 2023 example.com, Inc. or its affiliates. All Rights Reserved.
                 */
                """);
        }
    }


Generating other custom content
-------------------------------

Integrations might need to write additional files like a README, license
files, or generate additional code. Integrations can override the
``SmithyIntegration#customize`` method to perform anything they need to
do. This method is provided the ``CodegenContext`` type that is used
with the integration, allowing the ``customize`` method access to the
model, settings object, symbol provider, ``WriterDelegator``, and
``FileManifest`` used to save and read files.

The following example writes a custom README.md file:

.. code-block:: java

    @Override
    public void customize(PythonContext context) {
        context.writerDelegator().useFileWriter("README.md", writer -> {
            writer.write("""
                # $L service client
                Client SDK library ...""",
                context.settings().service()
            );
        });
    }


Registering ``SmithyIntegration``\ s
====================================

Implementations of Integrations are registered with Java :term:`SPI` by
adding a specific ``META-INF`` file and found on the classpath. For example,
if the integration class is defined as
``software.amazon.smithy.python.client.PythonIntegration``, then when using
:term:`Gradle`, the fully qualified class name of each implementation of the
integration needs to placed in a file named
``src/main/resources/META-INF/services/software.amazon.smithy.python.client.PythonIntegration``.

For example:

.. code-block:: python

    # in src/main/resources/META-INF/services/software.amazon.smithy.python.client.PythonIntegration
    software.foobaz.AddCodeLicense


Using ``SmithyIntegration``\ s in generators
============================================

:ref:`directedcodegen` automatically handles finding integrations on the
classpath, topologically ordering them, and applying each integration method at
the appropriate point of code generation.
