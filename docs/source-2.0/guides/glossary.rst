--------
Glossary
--------

.. index:: foo

.. glossary::

    AbstractCodeWriter
        A Java class in the :term:`Smithy reference implementation` used to
        generate code for a :term:`target environment`. Find the
        `source code on GitHub <https://github.com/smithy-lang/smithy/blob/main/smithy-utils/src/main/java/software/amazon/smithy/utils/AbstractCodeWriter.java>`__.

    codegen
    Code generation
    Code generator
        Code generators output source code that represents the shapes defined
        in a :term:`Smithy model`, and the code they generate use both the
        standard library and the :term:`runtime libraries` for the
        :term:`target environment`.

    codegen-core
        A `set of Java libraries <https://github.com/smithy-lang/smithy/tree/main/smithy-codegen-core>`__
        built on top of the :term:`Smithy reference implementation` that are
        used to implement Smithy code generators. codegen-core contains
        libraries for writing code, managing dependencies, managing imports,
        converting Smithy shapes to :term:`symbols` of a :term:`target environment`,
        :term:`reserved words` handling, and more.

    Gradle
        `Gradle <https://gradle.org/>`__ is a build tool for Java, Kotlin, and
        other languages. Gradle is typically the build system used to develop
        Smithy code generators. The Smithy team
        `maintains a Gradle plugin <https://github.com/awslabs/smithy-gradle-plugin>`__ for
        running :term:`Smithy-Build` via Gradle.

    Integrations
        Integrations are code generator plugins. Integrations are defined by
        each :term:`code generator`. They can be used to preprocess the model,
        modify the :term:`SymbolProvider` used during code generation, add
        dependencies for the :term:`target environment`, generate additional
        files, register protocol code generators, add configuration options
        to clients, and more.

        .. seealso:: :doc:`building-codegen/making-codegen-pluggable`

    Java Service Provider Interface
    SPI
        A feature for loading and discovering implemenations of a Java interface.
        SPI is used throughout the :term:`Smithy reference implementation` as
        a plugin system. See the `Java documentation <https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html>`__
        for more information.

    Knowledge index
        Abstractions provided in the :term:`Smithy reference implementation`
        that extract information from the metamodel in a more accessible way.
        For example, the `HttpBindingIndex`_ makes it easier to codegen HTTP
        bindings, and the `NullableIndex`_ hides the details of determining
        if a member is optional or always present.

    Projection
        A specific view of a Smithy model that has added, removed, or
        transformed model components.

        .. seealso:: :doc:`building-models/build-config`

    Reserved words
        Identifiers and words that cannot be used in a
        :term:`target environment`. Reserved words can be contextual or global
        to the target language (for example, a word might only be reserved when
        used as a structure property but not when used as the name of a
        shape). Code generators are expected to automatically translate
        reserved words into an identifier that is safe to use in the
        target environment.

    Runtime libraries
        The libraries used at runtime in a :term:`target environment`.
        For example, HTTP clients, type implementations like
        big decimal, etc.

    Semantic model
        The Smithy semantic model is an in-memory representation of the
        :term:`shapes`, :term:`traits`, and metadata defined in the Smithy
        model. In the :term:`Smithy reference implementation`, the
        semantic model is contained in the `Model class`_.

    Serde
        Shortened version of serialization and deserialization.

    Service closure
        The shapes connected to a service. These shapes are code generated.

    Shapes
        Shapes are named declarations of Smithy types that make up the
        :term:`semantic model`.

    Smithy-Build
        A model transformation framework built on top of the
        :term:`Smithy reference implementation`. Code generators are
        implemented as :ref:`smithy-build <smithy-build>` plugins.

    smithy-build.json
        The file used to configure :term:`Smithy-Build`.
        :term:`Code generators <code generator>` are configured and executed
        by adding plugins to smithy-build.json files in various projections.

        .. seealso:: :doc:`building-models/build-config`

    Smithy model
        Smithy models define services, operations, resources, and shapes.
        Smithy models are made up of one or more files to form the
        semantic model. Model files can use a JSON or IDL representation.

    Smithy reference implementation
        The Java implementation of Smithy that is used to load, validate,
        transform, and extract information from Smithy models.

    Smithy type
        The types of shapes that can be defined in a Smithy model (for example,
        string, integer, structure, etc.).

    Symbol
    Symbols
        The qualified name of a type in a target programming language. Symbols
        are used to map Smithy shapes to types in a :term:`target environment`,
        refer to language types, and refer to libraries that might be needed by
        the generated code. A symbol contains an optional namespace, optional
        namespace delimiter, name, a declaration file stating where the
        symbol is declared, a definition file stating where a symbol is
        defined, and a bag of properties associated with the symbol. Symbols
        can also contain *SymbolDependencies* that are used to automatically
        manage imports in a CodeWriter and to generate dependency closures for
        the target environment.

    SymbolProvider
        A SymbolProvider is used to generate Symbols for Smithy shapes and
        members. SymbolProviders can be decorated to provided additional
        functionality like automatically renaming reserved words.

    Target environment
        The intended programming language and specific environment of a code
        generator. For example, TypeScript running in the browser is a target
        environment.

    Traits
        Traits are model components that can be attached to :ref:`shapes <shapes>`
        to describe additional information about the shape; shapes provide
        the structure and layout of an API, while traits provide refinement
        and style. Code generators use traits to influence generated code.


.. _HttpBindingIndex: https://github.com/smithy-lang/smithy/blob/main/smithy-model/src/main/java/software/amazon/smithy/model/knowledge/HttpBindingIndex.java
.. _NullableIndex: https://github.com/smithy-lang/smithy/blob/main/smithy-model/src/main/java/software/amazon/smithy/model/knowledge/NullableIndex.java
.. _Model class: https://github.com/smithy-lang/smithy/blob/main/smithy-model/src/main/java/software/amazon/smithy/model/Model.java
