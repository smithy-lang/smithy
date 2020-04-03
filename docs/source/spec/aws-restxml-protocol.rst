.. _aws-restxml-protocol:

====================
AWS restXml protocol
====================

This specification defines the ``aws.protocols#restXml`` protocol.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


.. _aws.protocols#restXml-trait:

-------------------------------
``aws.protocols#restXml`` trait
-------------------------------

Summary
    Adds support for an HTTP-based protocol that sends XML requests and
    responses.
Trait selector
    ``service``
Value type
    Annotation trait.
See
    `Protocol tests <https://github.com/awslabs/smithy/tree/meta-protocol-and-auth/smithy-aws-protocol-tests/model>`_

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.protocols#restXml

        @restXml
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
                        "aws.protocols#restXml": true
                    }
                }
            }
        }

*TODO: Add specifications, protocol examples, etc.*
