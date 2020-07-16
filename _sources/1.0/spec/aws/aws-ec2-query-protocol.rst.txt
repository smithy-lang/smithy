.. _aws-ec2-query-protocol:

======================
AWS EC2 query protocol
======================

This specification defines the ``aws.protocols#ec2`` protocol.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


.. _aws.protocols#ec2Query-trait:

--------------------------------
``aws.protocols#ec2Query`` trait
--------------------------------

Summary
    Adds support for an HTTP protocol that sends requests in the query string
    OR in a ``x-form-url-encoded`` body and responses in XML documents. This
    protocol is an Amazon EC2-specific extension of the ``awsQuery`` protocol.
Trait selector
    ``service``
Value type
    Annotation trait.

.. important::

    This protocol does not support document types.

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
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyService": {
                    "type": "service",
                    "version": "2020-02-05",
                    "traits": {
                        "aws.protocols#ec2Query": {}
                    }
                }
            }
        }


.. _aws.protocols#ec2QueryName-trait:

------------------------------------
``aws.protocols#ec2QueryName`` trait
------------------------------------

Summary
    Allows a serialized query key to differ from a structure member name when
    used in the model.
Trait selector
    ``structure > member``

    *Any structure member*
Value type
    ``string``

.. important::
    The ``aws.protocols#ec2QueryName`` MUST only apply when serializing
    operation inputs using the ``aws.protocols#ec2`` protocol.

Given the following structure definition:

.. tabs::

    .. code-tab:: smithy

        structure MyStruct {
            @ec2QueryName("foo")
            bar: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyStruct": {
                    "type": "structure",
                    "members": {
                        "bar": {
                            "target": "smithy.api#String",
                            "traits": {
                                "aws.protocols#ec2QueryName": "foo"
                            }
                        }
                    }
                }
            }
        }

and the following values provided for ``MyStruct``,

::

    "bar" = "baz"

the serialization of this structure as an input on the ``aws.protocols#ec2``
protocol is:

::

    MyStruct.foo=baz


*TODO: Add specifications, protocol examples, etc.*
