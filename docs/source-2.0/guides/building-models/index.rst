.. _building-models:

======================
Building Smithy Models
======================

These guides describe how build artifacts are generated from Smithy
models, including JARs, OpenAPI specifications, generated code, etc.


Build system overview
=====================

Smithy models are built using the :doc:`Smithy Gradle plugin <gradle-plugin>`.
The Gradle plugin is a wrapper around a :doc:`smithy-build <build-config>` and
``smithy-build.json`` files, which is where Smithy code generation is
configured and most build logic is implemented. This separation allows
developers to more easily create build plugins for Smithy without needing
deep knowledge of Gradle, and allows build plugins to work with any
build tool. For example, Smithy is integrated with Amazon's internal
build system using only a lightweight wrapper around smithy-build.


Build system guides
===================

.. rst-class:: large-toctree

.. toctree::
    :maxdepth: 2

    gradle-plugin
    build-config
