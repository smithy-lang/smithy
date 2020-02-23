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
See
    `Protocol tests <https://github.com/awslabs/smithy/tree/meta-protocol-and-auth/smithy-aws-protocol-tests/model>`_

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


.. _aws.protocols#ec2QueryName-trait:

------------------------------------
``aws.protocols#ec2QueryName`` trait
------------------------------------

Summary
    Indicates the serialized name of a structure member when that structure is
    serialized for the input of an EC2 operation using the
    ``aws.protocols#ec2Query`` protocol.
Trait selector
    ``member:of(structure)``
Value type
    ``string``

It is very important to note that the ``aws.protocols#ec2QueryName`` ONLY applies
when serializing an INPUT. For example, given the following Smithy model:

.. tabs::

    .. code-tab:: smithy

        structure MyStruct {
            @ec2QueryName("foo")
            bar: String
        }

    .. code-tab:: json

        {
            "smithy": "0.5.0",
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

The serialization of this structure as an input is:

::

    MyStruct.bar=baz

The serialization of the structure as an (XML) output is:

.. code-block:: xml

    <MyStruct>
        <foo>baz</foo>
    </MyStruct>


*TODO: Add specifications, protocol examples, etc.*
