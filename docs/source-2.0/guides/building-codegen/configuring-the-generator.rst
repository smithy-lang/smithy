-------------------------
Configuring the Generator
-------------------------

This document provides guidance on how to configure a code generator.


Introduction
============

Smithy code generators are configured using plugins defined in
smithy-build.json files. For example:

.. code-block:: json
    :emphasize-lines: 4-9

    {
        "version": "1.0",
        "plugins": {
            "foo-client-codegen": {
                "service": "smithy.example#Weather",
                "package": "com.example.weather",
                "edition": "2023"
            }
        }
    }


How to name codegen plugins
===========================

Smithy code generation plugins should use a naming pattern of
``<language>-<type>-codegen``, where:

* ``<language>`` is the name of the programming language
* ``<type>`` is one of "client", "server", or "types"
* ``codegen`` is the kind of plugin (in this case, a code generator)

Examples:

* ``foo-client-codegen``: generate a hypothetical Foo language client
* ``foo-server-codegen``: generate a hypothetical Foo language server


Recommended properties
======================

Every codegen plugin should support the following properties, and may
choose to introduce any other properties as needed.


``service``
-----------

The ``service`` property defines the service shape ID to generate (for
example, ``"smithy.example#Weather"``). Smithy models can contain
multiple services. Providing a ``service`` shape ID tells code
generators which service to generate.

Generators may choose to make ``service`` optional. If optional, the
generator will attempt to find every service in the model. If only a
single service is found in the model, it is used for code generation. If
multiple services are found, the generator should fail and require an
explicit service shape ID.


``protocol`` (client and type codegen only)
-------------------------------------------

Defines the optional Smithy protocol to use for the generated client.
This value is provided as a shape ID that refers to the protocol trait
(for example, ``"aws.protocols#restJson1"``). This protocol shape ID
must be present in the model and applied as a trait to the resolved
``service`` shape to generate. If no protocol is provided, the generator
should find all the protocol traits attached to the resolved ``service``
and choose which protocol to use for code generation. It is up to the
generator to prioritize and choose protocols.

.. note::

    The ``protocol`` setting is typically only used by clients because
    a service is expected to support every protocol of the service, while
    a client can choose to connect over a single protocol.


``edition``
-----------

The ``edition`` property configures the code generator to use the best
practices defined as of a specific date (for example, ``2023``).
Editions should automatically enable and disable other feature-gates in
a generator. For example, if the TypeScript code generator decided that
there needs to be a new way to generate unions, then they could continue
to support the existing union behavior, add a feature gate to generate
the new union code, and eventually add a new edition that enables this
feature by default.

Editions in Smithy code generators are basically the same thing as
`editions in
Rust <https://doc.rust-lang.org/edition-guide/editions/index.html>`__.
They configure the Smithy code generator to take on new default behavior
as use cases evolve, features are added to the target language, or we
learn from customer feedback that we didn't get an abstraction right.

It is highly recommended that you make ``edition`` **required** to force
end users to opt-in to an edition rather than use a default edition.
Avoiding a default in this case makes it much more likely that new and
improved code generation features will be used by new users rather than
them naively sticking with an outdated edition simply because it's the
default.


``relativeDate`` (client and type codegen only)
-----------------------------------------------

Causes code generation to omit shapes that were deprecated prior to the
given ISO 8601 date (``YYYY-MM-DD``).

While other relativization transforms can be added in the future,
setting ``relativeDate`` causes shapes marked with the :ref:`deprecated-trait`
that have a "since" version that lexicographically comes before the provided
value to be omitted from the generated code. If the shape uses a ``since``
value that does not follow the ``YYYY-MM-DD`` format, then the shape is
included regardless of the deprecated trait.

For example, consider the following model:

.. code-block:: smithy

    service Foo {
        operations: [PutA, CreateA]
    }

    @deprecated(since: "2019-06-11")
    operation PutA {
        input:= {}
        output:= {}
    }

    operation CreateA {
        input:= {}
        output:= {}
    }

If ``relativeDate`` is set to ``2023-04-15``, then the ``PutA``
operation, its inputs, and outputs are omitted from codegen because the
``since`` value of the trait comes before the provided date.


``relativeVersion`` (client and type codegen only)
--------------------------------------------------

This setting provides the same behavior as ``relativeDate``, but uses
`Semantic Versioning <https://semver.org/>`__ rather than a date-based
versioning strategy. The provided string value is parsed into a SemVer
representation and compared against the ``since`` property of shapes
marked as ``@deprecated``. If the ``@deprecated`` trait uses a ``since``
value that is not a valid SemVer string, then the shape is included.

For example, consider the following model:

.. code-block:: smithy

    service Foo {
        operations: [PutA, CreateA]
    }

    @deprecated(since: "2.4")
    operation PutA {
        input:= {}
        output:= {}
    }

    operation CreateA {
        input:= {}
        output:= {}
    }

If ``relativeVersion`` is set to ``3.0``, then the ``PutA`` operation is
omitted from codegen because the ``since`` value of the trait is an
earlier version than the provided version.

.. note::

    ``relativeVersion`` and ``relativeDate`` can be used in tandem.


Converting JSON configuration to Java
=====================================

Configuration settings are parsed into generic "node" objects that
Smithy-Build plugins can then deserialize into strongly typed `Java
records <https://docs.oracle.com/en/java/javase/14/language/records.html>`__
or POJOs. For example:

.. code-block:: java

    public final class FooCodegenSettings {
        private ShapeId service;
        private String packageName;
        private String edition;

        public ShapeId getService() {
            return service;
        }

        public void setService(ShapeId service) {
            this.service = service;
        }

        public String getPackage() {
            return packageName;
        }

        public void setPackage(String packageName) {
            this.packageName = packageName;
        }

        public void getEdition(String edition) {
            this.edition = edition;
        }

        public String setEdition() {
            return edition;
        }
    }

You can use :ref:`directedcodegen` to
easily wire up the POJO to your generator. Wiring up the configuration
provided to the plugin to the generator can be done in
``SmithyBuildPlugin#execute`` using ``CodegenDirector#settings``.

.. code-block:: java
    :emphasize-lines: 12

    public final class FooCodegenPlugin implements SmithyBuildPlugin {
        @Override
        public String getName() {
            return "foo-client-codegen";
        }

        @Override
        public void execute(PluginContext context) {
            CodegenDirector<FooWriter, FooIntegration, FooContext, FooCodegenSettings>
                    runner = new CodegenDirector<>();
            runner.directedCodegen(new DirectedFooCodegen());
            runner.settings(FooCodegenSettings.class, context.getSettings());
            // ...
            runner.run();
        }
    }

.. seealso::

    * :ref:`codegen-creating-smithy-build-plugin`
    * :ref:`running-directedcodegen`
