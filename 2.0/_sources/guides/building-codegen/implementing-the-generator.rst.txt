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

        default void generateOperation(GenerateOperationDirective<C, S> directive) {}

        void generateStructure(GenerateStructureDirective<C, S> directive);

        void generateError(GenerateErrorDirective<C, S> directive);

        void generateUnion(GenerateUnionDirective<C, S> directive);

        default void generateList(GenerateListDirective<C, S> directive) {}

        default void generateMap(GenerateMapDirective<C, S> directive) {}

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

        // Assuming service() returns a configured service shape ID.
        runner.service(settings.service());

        // Codegen can also be driven by a shape closure, either on its own
        // or alongside a service to enable combined mode.
        // runner.shapeClosure(settings.closure());

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
class should have a ``ShapeId`` for the service to generate and/or a
closure ID for the closure to generate.

.. code-block:: java

    public final class MylangSettings {
        private ShapeId service;
        private String closureId;

        public void service(ShapeId service) {
            this.service = service;
        }

        public ShapeId service() {
            return service;
        }

        public void closure(String closureId) {
            this.closureId = closureId;
        }

        public String closure() {
            return closureId;
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

Generating for a single service
===============================

Code generators will typically generate code for interacting with a single,
specific service. To do this, simply call ``runner.service(...)`` to set the
service that code generation will be directed from.

When generating code from a service shape, all shapes in that
:ref:`service's closure <service-closure>` will be in scope for generation.
Other services and shapes may be present in the model, but generation directives
for shapes outside the closure will not be invoked.

Generating for a shape closure
==============================

Code generators can additionally be driven by a
:ref:`shape closure <shape-closures>`. Like a
:ref:`service's closure <service-closure>`, a shape closure defines the set of
shapes that are in scope for generation. Code generators can use a shape closure
to drive code generation by calling ``runner.shapeClosure(...)`` with the ID of
the closure to generate.

Shape closures are distinct from service closures in a number of ways, but the
most important things to note for a code generator are:

* There is no guarantee that there is exactly one service. A shape closure may
  have any number of service shapes.

  Not all generators may be able to represent multiple service shapes in their
  outputs. Generators that support output without service shapes can enable
  :ref:`type codegen <directed-type-codegen>` to generate code for just the data
  shapes in the closure.
* Since multiple services are supported, ``performDefaultCodegenTransforms``
  does not apply service-defined errors to operations.
* The ``service`` property of a ``Directive`` refers to the top-level service
  configured to drive code generation. This will not be present in shape
  closure driven generation.

  As a result, the ``shape`` property of ``GenerateServiceDirective`` must be
  used when generating services for closures. For the sake of simplicity, it is
  recommended to use the ``shape`` property even in service-based generation
  where both properties refer to the same shape.
* There is no guarantee that shape names in the closure are case-insensitively
  unique across namespaces. Service closures enforce this behavior, but it is
  optional in shape closures. Generators that cannot represent such conflicts
  can call ``runner.requireCaseInsensitiveNames()`` to fail code generation when
  the shapes being generated have case-insensitively conflicting names (taking
  closure-defined renames into account).

.. _combined-codegen:

Combining a service and a shape closure
========================================

A service and a shape closure can be set together, enabling "combined mode".
The shape closure is the set of shapes that gets generated, and the service is
designated the primary service. The service must be a member of the closure,
otherwise code generation should fail.

To enable it, set both on the director:

.. code-block:: java

    runner.service(settings.service());
    runner.shapeClosure(settings.closure());

This is useful when the generated set must include standalone shapes that are
not reachable from the service while still generating the service itself. In
combined mode the directive's ``getService()`` returns the service and
``getShapeClosureId()`` returns the closure ID.

.. _directed-type-codegen:

Generating only data shapes
============================

Calling ``runner.generateDataShapesOnly()`` enables "type codegen", where only
the shapes that carry data are generated. Service, resource, and operation
shapes are never generated, even when they are present in the generated closure.
Operation input and output shapes as well as error shapes are still generated.

This mode composes with either source.

.. important::

    Code generators are expected to not attempt to generate any service or
    client scaffolding when this mode is enabled.


.. _directive-service-access:

Accessing the service from a directive
======================================

Because code generation can be driven by a service or a shape closure, the
``service`` of a ``Directive`` is no longer guaranteed to be present. The
directive provides accessors that make this explicit:

* ``getService()`` returns an ``Optional<ServiceShape>`` and is the safe way to
  access the service. It is empty when generation is driven by a shape closure
  with no primary service.
* ``expectService()`` returns the ``ServiceShape``, throwing a
  ``CodegenException`` when there is none. Use this only where a service is
  guaranteed, such as in service-driven generation.
* ``getShapeClosureId()`` returns the ``Optional<String>`` ID of the shape
  closure driving generation, if any.
* ``getRenames()`` returns the ``Map<ShapeId, String>`` of renames that apply to
  the shapes being generated. These mirror the generated set: the closure's
  renames in closure or combined mode, otherwise the primary service's renames.
  Prefer this over reading renames from a service so naming works in every mode.

.. note::

    The older ``service()`` accessor is deprecated. It now throws a
    ``CodegenException`` rather than returning ``null`` when generation is driven
    by a shape closure, so existing generators keep their behavior in
    service-driven generation but must migrate to ``getService()`` or
    ``expectService()`` to support shape closures.


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
