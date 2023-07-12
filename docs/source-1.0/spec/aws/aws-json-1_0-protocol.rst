.. _aws-json-1_0:

=====================
AWS JSON 1.0 protocol
=====================

This specification defines the ``aws.protocols#awsJson1_0`` protocol.


.. smithy-trait:: aws.protocols#awsJson1_0
.. _aws.protocols#awsJson1_0-trait:

----------------------------------
``aws.protocols#awsJson1_0`` trait
----------------------------------

Summary
    Adds support for an HTTP protocol that sends "POST" requests and
    responses with JSON documents.
Trait selector
    ``service``
Value type
    Structure
See
    `Protocol tests <https://github.com/smithy-lang/smithy/tree/__smithy_version__/smithy-aws-protocol-tests/model/awsJson1_0>`_

``aws.protocols#awsJson1_0`` is a structure that supports the following
members:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - http
      - ``[string]``
      - The priority ordered list of supported HTTP protocol versions.
    * - eventStreamHttp
      - ``[string]``
      - The priority ordered list of supported HTTP protocol versions
        that are required when using :ref:`event streams <event-streams>`
        with the service. If not set, this value defaults to the value
        of the ``http`` member. Any entry in ``eventStreamHttp`` MUST
        also appear in ``http``.

Each entry in ``http`` and ``eventStreamHttp`` SHOULD be a valid
`Application-Layer Protocol Negotiation (ALPN) Protocol ID`_ (for example,
``http/1.1``, ``h2``, etc). Clients SHOULD pick the first protocol in the
list they understand when connecting to a service. A client SHOULD assume
that a service supports ``http/1.1`` when no ``http`` or ``eventStreamHttp``
values are provided.

The following example defines a service that uses ``aws.protocols#awsJson1_0``.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.protocols#awsJson1_0

        @awsJson1_0
        service MyService {
            version: "2020-02-05"
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyService": {
                    "type": "service",
                    "version": "2020-02-05",
                    "traits": {
                        "aws.protocols#awsJson1_0": {}
                    }
                }
            }
        }

The following example defines a service that requires the use of
``h2`` when using event streams.

.. code-block:: smithy

    namespace smithy.example

    use aws.protocols#awsJson1_0

    @awsJson1_0(
        http: ["h2", "http/1.1"],
        eventStreamHttp: ["h2"]
    )
    service MyService {
        version: "2020-02-05"
    }

The following example defines a service that requires the use of
``h2`` or ``http/1.1`` when using event streams, where ``h2`` is
preferred over ``http/1.1``.

.. code-block:: smithy

    namespace smithy.example

    use aws.protocols#awsJson1_0

    @awsJson1_1(
        http: ["h2", "http/1.1"],
        eventStreamHttp: ["h2", "http/1.1"]
    )
    service MyService {
        version: "2020-02-05"
    }

The following example defines a service that requires the use of
``h2`` for all requests, including event streams.

.. code-block:: smithy

    namespace smithy.example

    use aws.protocols#awsJson1_0

    @awsJson1_0(http: ["h2"])
    service MyService {
        version: "2020-02-05"
    }

.. |quoted shape name| replace:: ``awsJson1_0``
.. |protocol content type| replace:: ``application/x-amz-json-1.0``
.. |protocol error type contents| replace:: :ref:`shape-id`
.. |protocol test link| replace:: https://github.com/smithy-lang/smithy/tree/main/smithy-aws-protocol-tests/model/awsJson1_0
.. include:: aws-json.rst.template
.. include:: error-rename-simple.rst.template

.. _`Application-Layer Protocol Negotiation (ALPN) Protocol ID`: https://www.iana.org/assignments/tls-extensiontype-values/tls-extensiontype-values.xhtml#alpn-protocol-ids
