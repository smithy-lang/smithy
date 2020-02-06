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
    ``service``
Value type
    Annotation trait.

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
            "smithy": "0.5.0",
            "shapes": {
                "smithy.example#MyService": {
                    "type": "service",
                    "version": "2020-02-05",
                    "traits": {
                        "aws.protocols#awsQuery": true
                    }
                }
            }
        }

*TODO: Add specifications, protocol examples, etc.*


.. _aws.protocols#ec2Query-trait:

--------------------------------
``aws.protocols#ec2Query`` trait
--------------------------------

Summary
    Adds support for an HTTP protocol that sends request in the query
    string and responses in XML documents. This protocol is an
    Amazon EC2-specific extension of the ``awsQuery`` protocol.
Trait selector
    ``service``
Value type
    Annotation trait.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.protocols#ec2Query

        @ec2Query
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
                        "aws.protocols#ec2Query": true
                    }
                }
            }
        }

*TODO: Add specifications, protocol examples, etc.*
