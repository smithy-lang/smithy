====================
Smithy specification
====================

This is the specification of Smithy, an interface definition language and set
of tools used to build clients, servers, and other kinds of artifacts through
model transformations. This specification is at version |release|.


---------------------------------
Conventions used in this document
---------------------------------

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT",
"SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this
document are to be interpreted as described in :rfc:`2119`.

This specification makes use of the Augmented Backus-Naur Form (ABNF)
:rfc:`5234` notation, including the *core rules* defined in Appendix B
of that document.

Readers are invited to report technical errors and ambiguities in this
specification to the Smithy GitHub repository at https://github.com/smithy-lang/smithy.
This specification is open source, so contributions are welcome.

.. rubric:: Examples

Unless declared otherwise, example Smithy models given in this specification
are written using the :ref:`Smithy interface definition language (IDL) <idl>`
syntax. Complementary :ref:`JSON AST <json-ast>` examples are provided
alongside Smithy IDL examples where appropriate. For example:

.. tabs::

    .. code-tab:: smithy

        $version: "1.0"

        metadata foo = "bar"

        namespace smithy.example

        use smithy.other.namespace#MyString

        structure MyStructure {
            @required
            foo: MyString
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "metadata": {
                "foo": "bar"
            },
            "shapes": {
                "smithy.example#MyStructure": {
                    "type": "structure",
                    "members": {
                        "foo": {
                            "target": "smithy.other.namespace#MyString",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                }
            }
        }

-----------------
Table of contents
-----------------

.. rst-class:: large-toctree

.. toctree::
    :numbered:
    :maxdepth: 3

    model
    prelude-model
    constraint-traits
    documentation-traits
    type-refinement-traits
    protocol-traits
    auth-traits
    behavior-traits
    resource-traits
    stream-traits
    http-traits
    xml-traits
    endpoint-traits
    selectors
    model-validation
    idl
    json-ast
