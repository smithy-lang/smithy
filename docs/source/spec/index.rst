.. _specification:

==============
Specifications
==============

Smithy is split into several specifications. The core specification
defines the foundation of Smithy modeling. Additional specifications
define optional features that can be used in Smithy models.


.. _core-specification:

------------------
Core specification
------------------

.. toctree::
    :numbered:

    core/index
    core/lexical-structure
    core/shapes
    core/shape-id
    core/prelude-model
    core/traits
    core/constraint-traits
    core/documentation-traits
    core/type-refinement-traits
    core/protocol-traits
    core/auth-traits
    core/behavior-traits
    core/resource-traits
    core/event-stream-traits
    core/http-traits
    core/xml-traits
    core/endpoint-traits
    core/selectors
    core/json-ast
    core/model-metadata
    core/merging-models


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


------------------
AWS specifications
------------------

.. rst-class:: hidden

.. toctree::
    :maxdepth: 1

    aws-core
    aws-auth
    aws-iam
    amazon-apigateway
    aws-restjson1-protocol
    aws-json-protocols
    aws-restxml-protocol
    aws-query-protocol

AWS-specific specifications are defined below.

.. list-table::
    :widths: 45 55

    * - :doc:`aws-core`
      - Defines core traits used to integrate Smithy models with AWS.
    * - :doc:`aws-auth`
      - Defines AWS authentication schemes.
    * - :doc:`aws-iam`
      - Defines AWS IAM traits.
    * - :doc:`amazon-apigateway`
      - Defines Amazon API Gateway integrations.
    * - :doc:`aws-restjson1-protocol`
      - Defines the AWS restJson1 protocol.
    * - :doc:`aws-json-protocols`
      - Defines the AWS JSON 1.0 and 1.1 protocols.
    * - :doc:`aws-restxml-protocol`
      - Defines the AWS restXml protocol.
    * - :doc:`aws-query-protocol`
      - Defines the AWS query protocol.
