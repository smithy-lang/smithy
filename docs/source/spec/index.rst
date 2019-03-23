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

    mqtt
    validation

*Additional specifications* define additional functionality and
enhancements.

.. list-table::
    :widths: 45 55

    * - :doc:`mqtt`
      - Defines how to bind models to MQTT.
    * - :doc:`validation`
      - Defines how to configure custom validation.


------------------
AWS specifications
------------------

.. rst-class:: hidden

.. toctree::
    :maxdepth: 1

    aws-core

AWS-specific specifications are defined below.

.. list-table::
    :widths: 45 55

    * - :doc:`aws-core`
      - Defines core traits used to integrate Smithy models with AWS.
