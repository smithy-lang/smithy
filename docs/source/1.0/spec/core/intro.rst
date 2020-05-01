============
Introduction
============

Smithy is an interface definition language and set of tools used to
build clients, servers, and other kinds of artifacts through
model transformations. Smithy models define a service as a collection
of resources, operations, and shapes.


----------------------------
Status of this specification
----------------------------

This specification is at version |release|.


---------------------
Requirements notation
---------------------

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
"SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this
document are to be interpreted as described in [:rfc:`2119`].

This specification makes use of the Augmented Backus-Naur Form (ABNF)
[:rfc:`5234`] notation, including the *core rules* defined in Appendix B
of that document.


--------------
Example models
--------------

Unless declared otherwise, example Smithy models given in this specification
are written using the :ref:`Smithy Interface Definition Language (IDL) <lexical-structure>`
syntax, similar to:

.. tabs::

    .. code-tab:: smithy

        structure MyStructure {
            @required
            foo: String,

            @deprecated
            baz: Integer,
        }

    .. code-tab:: json

        {
            "smithy": "1.0.0",
            "shapes": {
                "smithy.example#MyStructure": {
                    "type": "structure",
                    "members": {
                        "foo": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        },
                        "baz": {
                            "target": "smithy.api#Integer",
                            "traits": {
                                "smithy.api#deprecated": {}
                            }
                        }
                    }
                }
            }
        }

Complementary JSON examples are provided alongside Smithy IDL examples
where appropriate.


--------
Feedback
--------

Readers are invited to report technical errors and ambiguities in this
specification to the Smithy GitHub repository at https://github.com/awslabs/smithy.
This specification is open source, so contributions are welcome.
