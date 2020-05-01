.. _aws-json-1_0:

=====================
AWS JSON 1.0 protocol
=====================

This specification defines the ``aws.protocols#awsJson1_0`` protocol.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


.. _aws.protocols#awsJson1_0-trait:

----------------------------------
``aws.protocols#awsJson1_0`` trait
----------------------------------

Summary
    Adds support for an HTTP protocol that sends POST requests and
    responses with JSON documents.
Trait selector
    ``service``
Value type
    Annotation trait.
See
    `Protocol tests <https://github.com/awslabs/smithy/tree/meta-protocol-and-auth/smithy-aws-protocol-tests/model>`_

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
            "smithy": "1.0.0",
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

*TODO: Add specifications, protocol examples, etc.*
