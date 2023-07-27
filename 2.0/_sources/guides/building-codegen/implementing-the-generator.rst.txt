--------------------------
Implementing the Generator
--------------------------

This document describes how to implement a code generator using the
high-level `DirectedCodegen <https://github.com/smithy-lang/smithy/blob/main/smithy-codegen-core/src/main/java/software/amazon/smithy/codegen/core/directed/DirectedCodegen.java>`__
interface.


.. _directedcodegen:

DirectedCodegen
===============

Smithy code generators typically all follow the same patterns. In fact,
the layout of existing code generators is so similar that a kind of
"golden-path" codegen architecture was designed called
*directed codegen*. The ``DirectedCodegen`` interface and ``CodegenDirector``
class provide a kind of guided template for building a code generator.

``DirectedCodegen`` brings together all the opinionated abstractions for
implementing a Smithy code generator.

- ``Symbol`` and ``SymbolProvider`` classes used to map shapes to code
  and decouple this logic from templates. (see :doc:`decoupling-codegen-with-symbols`)
- A language-specific ``SymbolWriter`` subclass used to generate code
  using a simple template engine. (see :doc:`generating-code`)
- A ``SmithyIntegration`` subtype used to provide extension points to
  the generator. (see :doc:`making-codegen-pluggable`)
- Easy to find helper methods for getting information from the model.
  (see :doc:`using-the-semantic-model`)
- Pre-defined "directives" that tell you the kinds of shape and trait
  combinations that need to be code generated.


Implementing ``DirectedCodegen``
================================

The methods of ``DirectedCodegen`` break down the process of building up
and running a generator into specific methods.

.. code-block:: java

    public interface DirectedCodegen<C extends CodegenContext<S, ?, I>, S, I extends SmithyIntegration<S, ?, C>> {

        SymbolProvider createSymbolProvider(CreateSymbolProviderDirective<S> directive);

        C createContext(CreateContextDirective<S, I> directive);

        void generateService(GenerateServiceDirective<C, S> directive);

        default void generateResource(GenerateResourceDirective<C, S> directive) {}

        void generateStructure(GenerateStructureDirective<C, S> directive);

        void generateError(GenerateErrorDirective<C, S> directive);

        void generateUnion(GenerateUnionDirective<C, S> directive);

        void generateEnumShape(GenerateEnumDirective<C, S> directive);

        void generateIntEnumShape(GenerateIntEnumDirective<C, S> directive);

        default void customizeBeforeShapeGeneration(CustomizeDirective<C, S> directive) {}

        default void customizeBeforeIntegrations(CustomizeDirective<C, S> directive) {}

        default void customizeAfterIntegrations(CustomizeDirective<C, S> directive) {}
    }

The `source code for DirectedCodegen`_ can be found on GitHub.


DirectedCodegen prerequisites
-----------------------------

``DirectedCodegen`` has a few prerequisites before it can be implemented.

- A ``SymbolProvider`` implementation used to map Smithy shapes to
  Symbols (type ``S``). :doc:`mapping-shapes-to-languages` provides
  guidance on how shapes should map to a programming language, and
  :doc:`decoupling-codegen-with-symbols` describes how
  Symbols are used to perform the actual mapping.
- A specific implementation of ``CodegenContext`` (type ``C``). This object
  provides access to codegen settings, abstractions for writing files, and
  abstractions for creating code writers. This context object has its
  own prerequisites:

  - A settings object for the code generator. This context object
    contains the codegen settings passed to your generator through
    ``smithy-build.json`` plugins (see :doc:`configuring-the-generator`).
  - A subclass of ``SymbolWriter`` used to generate code for the
    target language.
- A ``SmithyIntegration`` implementation used to make the generator
  extensible (type ``I``).


.. _running-directedcodegen:

Running ``DirectedCodegen`` using a ``CodegenDirector``
=======================================================

A ``CodegenDirector`` is used in concert with a ``DirectedCodegen``
implementation to build up the context needed to run the generator and
call methods in the right order. ``CodegenDirector`` is typically called
in a Smithy-Build plugin using the data provided by
``software.amazon.smithy.build.PluginContext``.

.. code-block:: java

    @Override
    public void execute(PluginContext context) {
        CodegenDirector<MylangWriter,
                        MylangIntegration,
                        MylangContext,
                        MylangSettings> runner = new CodegenDirector<>();

        // Assuming MylangGenerator is an implementation of DirectedCodegen.
        runner.directedCodegen(new MylangGenerator());

        // Set the SmithyIntegration class to look for and apply using SPI.
        runner.integrationClass(TestIntegration.class);

        // Set the FileManifest and Model from the plugin.
        runner.fileManifest(context.getFileManifest());
        runner.model(context.getModel());

        // Create a MylangSettings object from the plugin settings.
        MylangSettings settings = runner.settings(MylangSettings.class,
                                                  context.getSettings());

        // Assuming service() returns the configured service shape ID.
        runner.service(settings.service());

        // Configure the director to perform some common model transforms.
        runner.performDefaultCodegenTransforms();
        runner.createDedicatedInputsAndOutputs();

        runner.run();
    }

After performing the above steps, ``CodegenDirector`` will:

1. Perform any requested model transformations
2. Automatically find implementations of your ``SmithyIntegration``
   class using Java SPI. These implementations are then used throughout
   the rest of code generation.
3. Register the ``CodeInterceptors`` from each ``SmithyIntegration``
   with your ``WriterDelegator``
4. Call each ``generate``\ \* method in a topologically sorted order
   (that is, things with no references to other shapes come before
   shapes that reference them)
5. Call ``DirectedCodegen#customizeBeforeIntegrations``
6. Run the ``customize`` method of each ``SmithyIntegration``
7. Call ``DirectedCodegen#customizeAfterIntegrations``
8. Flush any open ``SymbolWriter``\ s in your ``WriterDelegator``.


Creating a settings class
=========================

A code generator uses a settings object to configure the generator in
Smithy-Build and during directed code generation. At a minimum, this
class should have a ``ShapeId`` for the service to generate.

.. code-block:: java

    public final class MylangSettings {
        private ShapeId service;

        public void service(ShapeId service) {
            this.service = service;
        }

        public ShapeId service() {
            return service;
        }
    }

.. seealso:: :doc:`configuring-the-generator` defines recommended settings


Creating a ``CodegenContext`` class
===================================

This object provides access to codegen settings, abstractions for
writing files, and abstractions for creating code writers. You should
create a specific implementation of ``CodegenContext`` for each
generator. This can be done using a Java record, POJO, builder, etc.

.. code-block:: java

    public record MylangContext (
        Model model,
        MylangSettings settings,
        SymbolProvider symbolProvider,
        FileManifest fileManifest,
        WriterDelegator<MylangWriter> writerDelegator,
        List<MylangIntegration> integrations,
        ServiceShape service
    ) implements CodegenContext<MylangSettings, MylangWriter, MylangIntegration> {}

``DirectedCodegen#createContext`` is responsible for creating a
``CodegenContext``. Ensure that the data provided by your ``CodegenContext``
are available using the data available to ``CreateContextDirective``.


Tips for using ``DirectedCodegen``
==================================

1. Each directive object provided to the methods of a DirectedCodegen
   implementation provide all the context needed to perform that action.
2. In addition to context, directives often provide helper methods to get
   information out of the model or shape being generated.
3. If additional data is needed in a given directive, you can:

   1. Add new getters to your ``CodegenContext`` class.
   2. Add state to your ``DirectedCodegen`` class to set the context
      data you need.


.. _source code for DirectedCodegen: https://github.com/smithy-lang/smithy/blob/main/smithy-codegen-core/src/main/java/software/amazon/smithy/codegen/core/directed/DirectedCodegen.java
