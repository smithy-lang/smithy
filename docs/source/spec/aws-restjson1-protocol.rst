.. _aws-restjson1-protocol:

======================
AWS restJson1 protocol
======================

This specification defines the ``aws.protocols#restJson1`` protocol.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


.. _aws.protocols#restJson1-trait:

---------------------------------
``aws.protocols#restJson1`` trait
---------------------------------

Summary
    Adds support for an HTTP-based protocol that sends JSON requests and
    responses with configurable HTTP bindings.
Trait selector
    ``service``
Value type
    Annotation trait.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.protocols#restJson1

        @restJson1
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
                        "aws.protocols#restJson1": true
                    }
                }
            }
        }

*TODO: Add specifications, protocol examples, etc.*
