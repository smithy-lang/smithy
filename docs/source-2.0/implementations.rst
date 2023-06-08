===============
Implementations
===============

This document provides an overview of known Smithy implementations and
projects.

Each project provides a *Status* with the following meanings:

* **1.x** / **2.x** / etc: The major version of a stable implementation.
* **0.x**: The implementation is not yet stable and likely will change.
  These pre-release projects may lack polish and may not be easy to use.
* **WIP**: Work in progress; the implementation is not yet stable and may
  not be usable.


-----------------
Smithy meta model
-----------------

.. list-table::
    :header-rows: 1
    :widths: 25 15 10 50

    * - Project
      - Language
      - Status
      - Description
    * - :doc:`spec/index`
      - N/A
      - 2.0
      - The Smithy specification defines Smithy and its capabilities.
    * - `Reference implementation <https://github.com/awslabs/smithy>`_
      - Java
      - 1.x
      - The reference implementation of Smithy's metamodel, transformation
        tooling, and code generators is implemented in Java.
    * - `Smithy Diff <https://github.com/awslabs/smithy/tree/main/smithy-diff>`_
      - Java
      - 1.x
      - Smithy Diff is a tool used to compare two Smithy models to check
        for backward compatibility issues. Smithy Diff can be run via a
        Java library or via the Smithy CLI.
    * - `Atelier <https://github.com/johnstonskj/rust-atelier>`_
      - Rust
      - WIP
      - A Rust implementation of the Smithy IDL.


-----------
IDE Support
-----------

.. list-table::
    :header-rows: 1
    :widths: 25 15 10 50

    * - Project
      - Language
      - Status
      - Description
    * - `Visual Studio Code plugin <https://github.com/awslabs/smithy-vscode>`_
      - Java
      - WIP
      - A Visual Studio Code extension to recognize and highlight the
        Smithy IDL. It can also be used as a TextMate bundle in TextMate
        and IntelliJ using a third-party plugin.
    * - `IntelliJ plugin <https://github.com/awslabs/smithy-intellij>`_
      - Java
      - WIP
      - Smithy IntelliJ provides IDE integration for the Smithy IDL within
        IntelliJ IDEA. It utilizes smithy-language-server for its Language
        Server Protocol implementation.
    * - `Smithy LSP <https://github.com/awslabs/smithy-language-server>`_
      - Java
      - WIP
      - A Language Server Protocol implementation for the Smithy IDL.
    * - `Tree Sitter grammar for Smithy <https://github.com/indoorvivants/tree-sitter-smithy>`_
      - JavaScript, C
      - GA
      - Included in `nvim-treesitter <https://github.com/nvim-treesitter/nvim-treesitter>`_
        to provide syntax highlighting in Neovim.


-------------
Build tooling
-------------

.. list-table::
    :header-rows: 1
    :widths: 25 15 10 50

    * - Project
      - Language
      - Status
      - Description
    * - `Gradle Plugin <https://github.com/awslabs/smithy-gradle-plugin>`_
      - Java
      - 0.x
      - The Smithy Gradle plugin is currently the only supported way to
        build Smithy models. `Various example projects <https://github.com/awslabs/smithy-gradle-plugin/tree/main/examples>`_
        that show how to setup and build Smithy models can be found in the
        project's repository.
    * - `Smithy CLI <https://github.com/awslabs/smithy/tree/main/smithy-cli>`_
      - Java
      - 1.x*
      - The Smithy CLI is used to load, validate, diff, and transform
        Smithy models. The CLI is used to power Smithy's Gradle plugin,
        and used for other build tooling within Amazon. However, the CLI
        is still incubating and not yet distributed as a standalone tool.

        The CLI can be built from within the Smithy git repository using Gradle
        via:

        .. code-block:: none

            ./gradlew :smithy-cli:runtime

        And then used via:

        .. code-block:: none

            smithy-cli/build/image/smithy-cli-osx-x86_64/bin/smithy --help
            smithy-cli/build/image/smithy-cli-win64/bin/smithy --help
            smithy-cli/build/image/smithy-cli-linux-x86_64/bin/smithy --help
            smithy-cli/build/image/smithy-cli-linux-aarch_64/bin/smithy --help

    * - `SBT Plugin <https://disneystreaming.github.io/smithy4s/docs/overview/sbt>`_
      - Scala
      - 0.x
      - The Smithy SBT plugin transforms Smithy specifications into
        protocol-agnostic Scala clients and servers.

.. _client-code-generators:

----------------------
Client code generators
----------------------

The following code generators are in early development. There's no guarantee
of polish or that they work for all use cases.

.. list-table::
    :header-rows: 1
    :widths: 25 15 10 50

    * - Project
      - Generator Language
      - Status
      - Description
    * - `TypeScript <https://github.com/awslabs/smithy-typescript>`_
      - Java
      - 0.x
      - TypeScript client and server code generation for Smithy.
    * - `Go <https://github.com/awslabs/smithy-go>`_
      - Java
      - 0.x
      - Go client code generation for Smithy.
    * - `Rust <https://github.com/awslabs/smithy-rs>`_
      - Kotlin
      - 0.x
      - Rust client code generation for Smithy.
    * - `Kotlin <https://github.com/awslabs/smithy-kotlin>`_
      - Kotlin
      - 0.x
      - Kotlin client code generation for Smithy.
    * - `Swift <https://github.com/awslabs/smithy-swift>`_
      - Kotlin
      - 0.x
      - Swift client code generation for Smithy.
    * - `Dafny <https://github.com/awslabs/smithy-dafny>`_
      - Java
      - 0.x
      - Dafny client and library code generation for Smithy.
    * - `Scala code generation for Smithy <https://github.com/disneystreaming/smithy4s>`_
      - Scala
      - 0.x
      - Scala client and server code generation for Smithy.

----------------------
Server code generators
----------------------

.. list-table::
    :header-rows: 1
    :widths: 50 15 10 25

    * - Project
      - Language
      - Status
      - Additional links
    * - `Smithy Server Generator for TypeScript <https://github.com/awslabs/smithy-typescript>`_
      - Java
      - 0.x (Developer Preview)
      - :doc:`Documentation <ts-ssdk/index>`
    * - `Smithy Rust <https://github.com/awslabs/smithy-rs>`_
      - Kotlin
      - 0.x (Developer Preview)
      - `Documentation <https://github.com/awslabs/smithy-rs>`_

----------------
Model converters
----------------

.. list-table::
    :header-rows: 1
    :widths: 25 15 10 50

    * - Project
      - Language
      - Status
      - Description
    * - :ref:`smithy-to-openapi`
      - Java
      - 1.x
      - Converts Smithy models to OpenAPI. Currently the only supported protocol
        is :ref:`aws.protocols#restJson1 <aws-restjson1-protocol>`.
        Amazon API Gateway extensions can be used with Smithy's OpenAPI converter
        using the `smithy-aws-apigateway-traits <https://search.maven.org/artifact/software.amazon.smithy/smithy-aws-apigateway-traits>`_
        Maven package.
    * - `Smithy to JSON Schema <https://github.com/awslabs/smithy/tree/main/smithy-jsonschema>`_
      - Java
      - 1.x
      - Converts Smithy shapes to JSON Schema using a Java library. Because
        the use cases we've seen so far converting Smithy to JSON Schema have
        been to facilitate converting Smithy to some other format that uses
        JSON Schema or some variant of it (like OpenAPI, or CloudFormation
        resource schemas), no standalone smithy-build plugin to convert Smithy
        models to JSON Schema is currently provided.


------------
AWS specific
------------

.. list-table::
    :header-rows: 1
    :widths: 25 15 10 50

    * - Project
      - Generator Language
      - Status
      - Description
    * - :doc:`aws/index`
      - N/A
      - 1.x
      - Smithy supports various AWS plugins, traits, and specifications,
        though these are generally only useful to developers within Amazon
        and AWS.
    * - `AWS SDK for JavaScript v3 <https://github.com/aws/aws-sdk-js-v3>`_
      - Java
      - 1.x
      - The AWS SDK for JavaScript v3 is built with Smithy.
    * - `AWS SDK for Go v2 <https://github.com/aws/aws-sdk-go-v2>`_
      - Java
      - 1.x
      - The AWS SDK for Go v2 is built with Smithy.
    * - `AWS SDK for Rust <https://github.com/awslabs/aws-sdk-rust>`_
      - Kotlin
      - 0.x
      - The AWS SDK for Rust is built with Smithy. The generator
        implementation, including the AWS components, can be found in
        `smithy-rs. <https://github.com/awslabs/smithy-rs>`_
    * - :ref:`smithy-to-cloudformation`
      - Java
      - 1.x
      - Converts Smithy models to CloudFormation Resource Schemas.

.. toctree::
    :hidden:

    ts-ssdk/index
