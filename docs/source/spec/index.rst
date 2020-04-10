.. _specification:

==============
Specifications
==============

Smithy is split into several specifications.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


.. _smithy-specification:

--------------------
Smithy specification
--------------------

.. toctree::
    :numbered:
    :maxdepth: 3

    Introduction <core/index>
    core/lexical-structure
    core/shapes
    core/prelude-model
    core/traits
    core/constraint-traits
    core/documentation-traits
    core/type-refinement-traits
    core/protocol-traits
    core/auth-traits
    core/behavior-traits
    core/resource-traits
    core/stream-traits
    core/http-traits
    core/xml-traits
    core/endpoint-traits
    core/selectors
    core/model-metadata
    core/merging-models
    core/json-ast


.. _additional-specifications:

-------------------------
Additional specifications
-------------------------

.. rst-class:: hidden

.. toctree::
    :maxdepth: 1

    validation
    http-protocol-compliance-tests
    mqtt

*Additional specifications* define additional functionality and
enhancements.

.. list-table::
    :widths: 45 55

    * - :doc:`validation`
      - Defines how to configure validation.
    * - :doc:`http-protocol-compliance-tests`
      - Defines traits used to validate HTTP-based
        client and server protocol implementations.
    * - :doc:`mqtt`
      - Defines how to bind models to MQTT.

.. _aws-specifications:

------------------
AWS specifications
------------------

.. rst-class:: hidden

.. toctree::
    :maxdepth: 1

    aws/index

Smithy AWS specifications define protocols, authentication schemes,
and traits used to model AWS services.

* :doc:`aws/index`
