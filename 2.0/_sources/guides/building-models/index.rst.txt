.. _building-models:

======================
Building Smithy Models
======================

Building a Smithy model is the process of validating a model and
creating build artifacts like other models, JARs,
OpenAPI specifications, code, etc. Smithy models are built using the
:doc:`Smithy Gradle plugin <gradle-plugin>`. The Gradle plugin is a
wrapper around a :doc:`smithy-build <build-config>` and ``smithy-build.json``
files, which is where Smithy code generation is configured and most
build logic is implemented.

.. toctree::
    :maxdepth: 1
    :caption: Build system guides

    gradle-plugin
    build-config
