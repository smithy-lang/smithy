.. _json-ast:

========
JSON AST
========

Smithy models written using the Smithy IDL have an isomorphic JSON
abstract syntax tree (AST) representation that can be used to more easily
integrate Smithy into languages and tools that do not have a Smithy IDL
parser.

* Smithy JSON models can be merged together with other JSON models or other
  Smithy IDL models using the rules defined in :ref:`merging-models`.
* All shape IDs in the JSON AST MUST be absolute shape IDs that contain a
  namespace. One of the main drivers of the simplicity of the JSON AST
  over the Smithy IDL is that relative and forward references never need to
  be resolved.


.. _ast-top-level-properties:

--------------------
Top level properties
--------------------

Smithy JSON models are objects that can contain the following top-level
properties:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property
      - Type
      - Description
    * - smithy
      - ``string``
      - **Required**. Defines the :ref:`version <smithy-version>` of the
        Smithy specification (e.g., "|release|"). The version can be set to
        a single number like "2" or include a point release like "2.0".
    * - metadata
      - object
      - Defines all of the :ref:`metadata <metadata>` about the model
        using a JSON object. Each key is the metadata key to set, and each
        value is the metadata value to assign to the key.
    * - shapes
      - Map<:ref:`shape ID <shape-id>`, :ref:`AST shape <ast-shapes>`>
      - A map of absolute shape IDs to shape definitions.


.. _ast-shapes:

----------
AST shapes
----------

AST :ref:`shapes <shapes>` are defined using objects that always contain
a ``type`` property to define the shape type or ``apply``.

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#MyString": {
                "type": "string"
            }
        }
    }

All entries in the ``shapes`` map can contain a ``traits`` property that
defines the traits attached to the shape. ``traits`` is a map where
each key is the absolute shape ID of a trait shape and each value is
the value to assign to the trait.

.. code-block:: json

    {
        "traits": {
            "smithy.example#documentation": "Hi!",
            "smithy.example#tags": [
                "a",
                "b"
            ]
        }
    }

The following example defines a string shape with a documentation trait.

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#MyString": {
                "type": "string",
                "traits": {
                    "smithy.api#documentation": "My documentation string"
                }
            }
        }
    }


-------------
Simple shapes
-------------

:ref:`Simple shapes <simple-types>` are defined as an object. The following
example defines a shape for each simple type:

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#Blob": {
                "type": "blob"
            },
            "smithy.example#Boolean": {
                "type": "boolean"
            },
            "smithy.example#Document": {
                "type": "document"
            },
            "smithy.example#String": {
                "type": "string"
            },
            "smithy.example#Byte": {
                "type": "byte"
            },
            "smithy.example#Short": {
                "type": "short"
            },
            "smithy.example#Integer": {
                "type": "integer"
            },
            "smithy.example#Long": {
                "type": "long"
            },
            "smithy.example#Float": {
                "type": "float"
            },
            "smithy.example#Double": {
                "type": "double"
            },
            "smithy.example#BigInteger": {
                "type": "bigInteger"
            },
            "smithy.example#BigDecimal": {
                "type": "bigDecimal"
            },
            "smithy.example#Timestamp": {
                "type": "timestamp"
            }
        }
    }


-----------
List shapes
-----------

:ref:`list` shapes have a required ``member`` property that is an
:ref:`AST member <ast-member>`.

The following example defines a list with a string member:

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#MyList": {
                "type": "list",
                "member": {
                    "target": "smithy.api#String"
                }
            }
        }
    }


.. _ast-member:

----------
AST member
----------

An *AST member definition* defines a member of a shape. It is a special
kind of :ref:`AST shape reference <ast-shape-reference>` that also
contains an optional ``traits`` property that defines traits attached to
the member. Each key in the ``traits`` property is the absolute shape ID
of the trait to apply, and each value is the value to assign to the
trait.

.. code-block:: json

    {
        "target": "smithy.example#MyShape",
        "traits": {
            "smithy.example#documentation": "Hi!"
        }
    }

The following example defines a list shape and its member.

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#MyList": {
                "type": "list",
                "member": {
                    "target": "smithy.api#String",
                    "traits": {
                        "smithy.api#documentation": "Member documentation"
                    }
                }
            }
        }
    }


.. _ast-shape-reference:

-------------------
AST shape reference
-------------------

An *AST shape reference* is an object with a single property, ``target``
that maps to an absolute shape ID.

.. code-block:: json

    {
        "target": "smithy.example#MyShape"
    }

The following example defines a shape reference inside of the ``operations``
list of a service shape.

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#Service": {
                "type": "service",
                "operations": [
                    {
                        "target": "smithy.example#Operation"
                    }
                ]
            },
            "smithy.example#Operation": {
                "type": "operation"
            }
        }
    }


---------
Map shape
---------

A :ref:`map` shape has a required ``key`` and ``value``
:ref:`AST member <ast-member>`. The shape referenced by the ``key`` member
MUST target a string shape.

The following example defines a map of strings to numbers:

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#IntegerMap": {
                "type": "map",
                "key": {
                    "target": "smithy.api#String"
                },
                "value": {
                    "target": "smithy.api#Integer"
                }
            }
        }
    }


------------------------------------------
Structure, union, enum, and intEnum shapes
------------------------------------------

:ref:`Structure <structure>`, :ref:`union <union>`, :ref:`enum <enum>`, and
:ref:`intEnum <intEnum>` shapes are defined with a ``members`` property that
contains a map of member names to :ref:`AST member <ast-member>` definitions.
Unions, enums, and intEnums all require at least one member, and a structure
shape MAY omit the ``members`` property entirely if the structure contains no
members.

Each shape's member names MUST be case-insensitively unique across the entire
set of members. Each member name MUST adhere to the :token:`smithy:Identifier`
ABNF grammar.

The following example defines a structure with one required and one optional
member:

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#MyStructure": {
                "type": "structure",
                "members": {
                    "stringMember": {
                        "target": "smithy.api#String",
                        "traits": {
                            "smithy.api#required": {}
                        }
                    },
                    "numberMember": {
                        "target": "smithy.api#Integer"
                    }
                }
            }
        }
    }

The following example defines a union:

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#MyUnion": {
                "type": "union",
                "members": {
                    "a": {
                        "target": "smithy.api#String"
                    },
                    "b": {
                        "target": "smithy.api#Integer"
                    }
                }
            }
        }
    }

The following example defines an :ref:`enum`:

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#MyEnum": {
                "type": "enum",
                "members": {
                    "FOO": {
                        "target": "smithy.api#Unit",
                        "traits": {
                            "smithy.api#enumValue": "foo"
                        }
                    }
                }
            }
        }
    }

The following example defines an :ref:`intEnum`:

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#MyIntEnum": {
                "type": "intEnum",
                "members": {
                    "FOO": {
                        "target": "smithy.api#Unit",
                        "traits": {
                            "smithy.api#enumValue": 1
                        }
                    }
                }
            }
        }
    }


.. _service-ast-shape:

-------------
Service shape
-------------

:ref:`Service <service>` shapes are defined using an object. Service
shapes defined in JSON support the same properties as the Smithy IDL.

.. list-table::
    :header-rows: 1
    :widths: 10 28 62

    * - Property
      - Type
      - Description
    * - type
      - string
      - ``service``
    * - version
      - ``string``
      - Defines the version of the service. The version can be provided in any
        format (e.g., ``2017-02-11``, ``2.0``, etc).
    * - :ref:`operations <service-operations>`
      - [:ref:`AST shape reference <ast-shape-reference>`]
      - Binds a list of operations to the service. Each reference MUST target
        an operation.
    * - :ref:`resources <service-resources>`
      - [:ref:`AST shape reference <ast-shape-reference>`]
      - Binds a list of resources to the service. Each reference MUST target
        a resource.
    * - errors
      - [:ref:`AST shape reference <ast-shape-reference>`]
      - Defines a list of common errors that every operation bound within the
        closure of the service can return. Each provided shape ID MUST target
        a :ref:`structure <structure>` shape that is marked with the
        :ref:`error-trait`.
    * - traits
      - map of :ref:`shape ID <shape-id>` to trait values
      - Traits to apply to the service
    * - rename
      - map of :ref:`shape ID <shape-id>` to ``string`` :token:`smithy:Identifier`
      - Disambiguates shape name conflicts in the
        :ref:`service closure <service-closure>`.

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#MyService": {
                "type": "service",
                "version": "2017-02-11",
                "operations": [
                    {
                        "target": "smithy.example#GetServerTime"
                    }
                ],
                "resources": [
                    {
                        "target": "smithy.example#SomeResource"
                    }
                ],
                "errors": [
                    {
                        "target": "smithy.example#SomeError"
                    }
                ],
                "traits": {
                    "smithy.api#documentation": "Documentation for the service"
                },
                "rename": {
                    "smithy.example#Widget": "SmithyWidget",
                    "foo.example#Widget": "FooWidget"
                }
            }
        }
    }


.. _resource-ast-shape:

--------------
Resource shape
--------------

:ref:`Resource <resource>` shapes are defined using an object. Resource
shapes defined in JSON support the same properties as the Smithy IDL.

.. list-table::
    :header-rows: 1
    :widths: 10 28 62

    * - Property
      - Type
      - Description
    * - type
      - string
      - ``resource``
    * - :ref:`identifiers <resource-identifiers>`
      - Map<String, :ref:`AST shape reference <ast-shape-reference>`>
      - Defines identifier names and shape IDs used to identify the resource.
    * - :ref:`properties <resource-properties>`
      - Map<String, :ref:`AST shape reference <ast-shape-reference>`>
      - Defines a map of property string names to shape IDs that
        enumerate the properties of the resource.
    * - :ref:`create <create-lifecycle>`
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines the lifecycle operation used to create a resource using one
        or more identifiers created by the service.
    * - :ref:`put <put-lifecycle>`
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines an idempotent lifecycle operation used to create a resource
        using identifiers provided by the client.
    * - :ref:`read <read-lifecycle>`
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines the lifecycle operation used to retrieve the resource.
    * - :ref:`update <update-lifecycle>`
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines the lifecycle operation used to update the resource.
    * - :ref:`delete <delete-lifecycle>`
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines the lifecycle operation used to delete the resource.
    * - :ref:`list <list-lifecycle>`
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines the lifecycle operation used to list resources of this type.
    * - operations
      - [:ref:`AST shape reference <ast-shape-reference>`]
      - Binds a list of non-lifecycle instance operations to the resource.
        Each reference MUST target an operation.
    * - collectionOperations
      - [:ref:`AST shape reference <ast-shape-reference>`]
      - Binds a list of non-lifecycle collection operations to the resource.
        Each reference MUST target an operation.
    * - resources
      - [:ref:`AST shape reference <ast-shape-reference>`]
      - Binds a list of resources to this resource as a child resource,
        forming a containment relationship. The resources MUST NOT have a
        cyclical containment hierarchy, and a resource can not be bound more
        than once in the entire closure of a resource or service.
        Each reference MUST target a resource.
    * - traits
      - Map\<:ref:`shape ID <shape-id>`, trait value>
      - Traits to apply to the resource.

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#Thing": {
                "type": "resource",
                "identifiers": {
                    "forecastId": {
                        "target": "smithy.api#String"
                    },
                },
                "create": {
                    "target": "smithy.example#CreateThing"
                },
                "read": {
                    "target": "smithy.example#GetThing"
                },
                "update": {
                    "target": "smithy.example#Updatething"
                },
                "delete": {
                    "target": "smithy.example#DeleteThing"
                },
                "list": {
                    "target": "smithy.example#ListThings"
                },
                "operations": [
                    {
                        "target": "smithy.example#SomeInstanceOperation"
                    }
                ],
                "collectionOperations": [
                    {
                        "target": "smithy.example#SomeCollectionOperation"
                    }
                ],
                "resources": [
                    {
                        "target": "smithy.example#SomeResource"
                    }
                ]
            }
        }
    }


.. _operation-ast-shape:

---------------
Operation shape
---------------

:ref:`Operation <operation>` shapes are defined using an object with the
following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 28 62

    * - Property
      - Type
      - Description
    * - type
      - string
      - ``operation``
    * - input
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines the optional input structure of the operation. The ``input``
        of an operation MUST resolve to a :ref:`structure`.
    * - output
      - :ref:`AST shape reference <ast-shape-reference>`
      - Defines the optional output structure of the operation. The ``output``
        of an operation MUST resolve to a :ref:`structure`.
    * - errors
      - [:ref:`AST shape reference <ast-shape-reference>`]
      - Defines the list of errors that MAY be encountered when invoking
        the operation. Each reference MUST resolve to a :ref:`structure`
        shape that is marked with the :ref:`error-trait` trait.
    * - traits
      - Map\<:ref:`shape ID <shape-id>`, trait value>
      - Traits to apply to the operation.

The following example defines an operation, its input, output, and errors:

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#MyOperation": {
                "type": "operation",
                "input": {
                    "target": "smithy.example#MyOperationInput"
                },
                "output": {
                    "target": "smithy.example#MyOperationOutput"
                },
                "errors": [
                    {
                        "target": "smithy.example#BadRequestError"
                    },
                    {
                        "target": "smithy.example#NotFoundError"
                    }
                ]
            },
            "smithy.example#MyOperationInput": {
                "type": "structure"
            },
            "smithy.example#MyOperationOutput": {
                "type": "structure"
            },
            "smithy.example#BadRequestError": {
                "type": "structure",
                "traits": {
                    "smithy.api#error": "client"
                }
            },
            "smithy.example#NotFoundError": {
                "type": "structure",
                "traits": {
                    "smithy.api#error": "client"
                }
            }
        }
    }


.. ast-mixins:

------
Mixins
------

All shapes in the ``shapes`` map can contain a ``mixins`` property that
defines the :ref:`mixins` that are added to the shape. ``mixins`` is an
array of :ref:`shape references <ast-shape-reference>` that target shapes
marked with the :ref:`mixin trait <mixin-trait>`.

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#BaseUser": {
                "type": "structure",
                "members": {
                    "userId": {
                        "target": "smithy.api#String"
                    }
                },
                "traits": {
                    "smithy.api#mixin": {}
                }
            },
            "smithy.example#UserDetails": {
                "type": "structure",
                "members": {
                    "username": {
                        "target": "smithy.api#String"
                    }
                },
                "mixins": [
                    {
                        "target": "smithy.example#BaseUser"
                    }
                ]
            }
        }
    }


.. _ast-apply:

--------------
AST apply type
--------------

Traits can be applied to shapes outside of their definition by setting
``type`` to ``apply``. The ``apply`` type does not actually define a shape
for the shape ID; the shape ID MUST reference a shape to which traits are
applied. The ``apply`` type allows only the ``traits`` property.

.. code-block:: json

    {
        "smithy": "2.0",
        "shapes": {
            "smithy.example#Struct": {
                "type": "structure",
                "members": {
                    "foo": {
                        "target": "smithy.api#String"
                    }
                }
            },
            "smithy.example#Struct$foo": {
                "type": "apply",
                "traits": {
                    "smithy.api#documentation": "My documentation string"
                }
            }
        }
    }
