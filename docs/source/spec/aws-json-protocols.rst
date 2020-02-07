.. _aws-json-protocols:

==================
AWS JSON protocols
==================

This specification defines the ``aws.protocols#awsJson1_0`` and
``aws.protocols#awsJson1_1`` protocols.

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
            "smithy": "0.5.0",
            "shapes": {
                "smithy.example#MyService": {
                    "type": "service",
                    "version": "2020-02-05",
                    "traits": {
                        "aws.protocols#awsJson1_0": true
                    }
                }
            }
        }

*TODO: Add specifications, protocol examples, etc.*


.. _aws.protocols#awsJson1_1-trait:

----------------------------------
``aws.protocols#awsJson1_1`` trait
----------------------------------

Summary
    Adds support for an HTTP protocol that sends POST requests and
    responses with JSON documents.
Trait selector
    ``service``
Value type
    Annotation trait.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.protocols#awsJson1_1

        @awsJson1_1
        service MyService {
            version: "2020-02-05"
        }

    .. code-tab:: json

        {
            "smithy": "0.5.0",
            "shapes": {
                "smithy.example#MyService": {
                    "type": "service",
                    "version": "2020-02-05",
                    "traits": {
                        "aws.protocols#awsJson1_1": true
                    }
                }
            }
        }

*TODO: Add specifications, protocol examples, etc.*
