.. _aws-query-protocol:

==================
AWS query protocol
==================

This specification defines the ``aws.protocols#awsQuery`` protocol.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


.. _aws.protocols#awsQuery-trait:

--------------------------------
``aws.protocols#awsQuery`` trait
--------------------------------

Summary
    Adds support for an HTTP protocol that sends requests in the query
    string and responses in XML documents.
Trait selector
    ``service [trait|xmlNamespace]``

    *Service shapes with the xmlNamespace trait*
Value type
    Annotation trait.
See
    `Protocol tests <https://github.com/awslabs/smithy/tree/__smithy_version__/smithy-aws-protocol-tests/model/awsQuery>`_

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.protocols#awsQuery

        @awsQuery
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
                        "aws.protocols#awsQuery": {}
                    }
                }
            }
        }

.. important::

    This protocol does not support document types.

.. important::

    This protocol only permits the :ref:`httpPayload-trait` to be applied to
    members that target structures, documents, strings, blobs, or unions.

*TODO: Add specifications, protocol examples, etc.*
