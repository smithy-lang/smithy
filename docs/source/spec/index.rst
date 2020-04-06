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
    core/event-stream-traits
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

    aws-core
    aws-auth
    aws-iam
    amazon-apigateway
    aws-restjson1-protocol
    aws-json-1_0-protocol
    aws-json-1_1-protocol
    aws-restxml-protocol
    aws-query-protocol
    aws-ec2-query-protocol

AWS specifications are defined below.

.. list-table::
    :widths: 45 55

    * - :doc:`aws-core`
      - Defines core traits used to integrate Smithy models with AWS.
    * - :doc:`aws-auth`
      - Defines AWS authentication schemes.
    * - :doc:`aws-iam`
      - Defines AWS IAM traits.
    * - :doc:`amazon-apigateway`
      - Defines Amazon API Gateway traits.


AWS Protocols
=============

.. list-table::
    :widths: 45 55

    * - :doc:`aws.protocols#restJson1 <aws-restjson1-protocol>`
      - Defines the AWS restJson1 protocol.
    * - :doc:`aws.protocols#awsJson1_0 <aws-json-1_0-protocol>`
      - Defines the AWS JSON 1.0 protocol.
    * - :doc:`aws.protocols#awsJson1_1 <aws-json-1_1-protocol>`
      - Defines the AWS JSON 1.1 protocol.
    * - :doc:`aws.protocols#restXml <aws-restxml-protocol>`
      - Defines the AWS restXml protocol.
    * - :doc:`aws.protocols#awsQuery <aws-query-protocol>`
      - Defines the AWS query protocol.
    * - :doc:`aws.protocols#ec2Query <aws-ec2-query-protocol>`
      - Defines the Amazon EC2 query protocol.
