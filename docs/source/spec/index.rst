.. _specification:

==============
Specifications
==============

Smithy is split into several specifications.


.. _core-specifications:

-------------------
Core specifications
-------------------

.. rst-class:: hidden

.. toctree::
    :maxdepth: 1

    core
    language-specification
    selectors
    event-streams
    http
    xml


Every specification builds on top of these core specifications.

.. list-table::
    :widths: 45 55

    * - :doc:`core`
      - Defines the foundation of Smithy modeling.
    * - :doc:`language-specification`
      - Defines the syntax of ``.smithy`` models.
    * - :doc:`selectors`
      - Defines the syntax used to match shapes.
    * - :doc:`event-streams`
      - Defines streaming of structured data.
    * - :doc:`http`
      - Defines how to bind models to HTTP.
    * - :doc:`xml`
      - Defines how to bind models to XML.


-------------------------
Additional specifications
-------------------------

.. rst-class:: hidden

.. toctree::
    :maxdepth: 1

    validation
    mqtt
    http-protocol-compliance-tests

*Additional specifications* define additional functionality and
enhancements.

.. list-table::
    :widths: 45 55

    * - :doc:`validation`
      - Defines how to configure custom validation.
    * - :doc:`mqtt`
      - Defines how to bind models to MQTT.
    * - :doc:`http-protocol-compliance-tests`
      - Defines traits used to validate HTTP-based
        client and server protocol implementations.


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
