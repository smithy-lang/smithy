.. _shapes:

======
Shapes
======

*Shapes* are instances of *types* that describe the structure of an API.

.. contents:: Table of contents
    :depth: 1
    :local:
    :backlinks: none


-------------
Shape section
-------------

The shape section of the IDL is used to define shapes and apply traits to
shapes. It comes after any :token:`control statements <control_section>` and
:token:`metadata statements <metadata_section>`.

.. productionlist:: smithy
    shape_section :[`namespace_statement` [`use_section`] [`shape_statements`]]


.. _namespaces:

Namespaces
==========

Shapes are defined inside a :dfn:`namespace`. A namespace is mechanism for
logically grouping shapes in a way that makes them reusable alongside other
models without naming conflicts.

.. _namespace-statement:

Shapes can only be defined if a namespace is declared, and only a single
namespace can appear in an IDL model file.

.. productionlist:: smithy
    namespace_statement :"namespace" `ws` `namespace` `br`

The following example defines a string shape named ``MyString`` in the
``smithy.example`` namespace:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        string MyString

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyString": {
                    "type": "string"
                }
            }
        }


.. _use-statement:

Referring to shapes
===================

The *use section* of the IDL is used to import shapes into the current
namespace so that they can be referred to using a relative shape.

.. productionlist:: smithy
    use_section   :*(`use_statement`)
    use_statement :"use" `ws` `absolute_root_shape_id` `br`

The following example imports ``smithy.example#Foo`` and
``smithy.example#Baz`` so that they can be referred to by
:ref:`relative shape IDs <relative-shape-id>`:

.. code-block:: smithy

    namespace smithy.hello

    use smithy.example#Foo
    use smithy.example#Baz

    map MyMap {
        // Resolves to smithy.example#Foo
        key: Foo,
        // Resolves to smithy.example#Baz
        value: Baz,
    }

A use statement can import :ref:`traits <traits>` too. The following example
imports the ``smithy.example#test`` and ``smithy.example#anotherTrait``
traits so that they can be applied using relative shape IDs:

.. code-block:: smithy

    namespace smithy.hello

    use smithy.example#test
    use smithy.example#anotherTrait

    @test // <-- Resolves to smithy.example#test
    string MyString

.. rubric:: Validation

#. A shape cannot be defined in a file with the same name as one of the
   shapes imported with a ``use`` statement.
#. Shapes IDs with members names cannot be imported with a use statement.


.. _relative-shape-id:

Relative shape ID resolution
----------------------------

In the Smithy IDL, relative shape IDs are resolved using the following process:

#. If a :token:`use_statement` has imported a shape with the same name,
   the shape ID resolves to the imported shape ID.
#. If a shape is defined in the same namespace as the shape with the same name,
   the namespace of the shape resolves to the *current namespace*.
#. If a shape is defined in the :ref:`prelude <prelude>` with the same name,
   the namespace resolves to ``smithy.api``.
#. If a relative shape ID does not satisfy one of the above cases, the shape
   ID is invalid, and the namespace is inherited from the *current namespace*.

The following example Smithy model contains comments above each member of
the shape named ``MyStructure`` that describes the shape the member resolves
to.

.. code-block:: smithy

    namespace smithy.example

    use foo.baz#Bar

    string MyString

    structure MyStructure {
        // Resolves to smithy.example#MyString
        // There is a shape named MyString defined in the same namespace.
        a: MyString,

        // Resolves to smithy.example#MyString
        // Absolute shape IDs do not perform namespace resolution.
        b: smithy.example#MyString,

        // Resolves to foo.baz#Bar
        // The "use foo.baz#Bar" statement imported the Bar symbol,
        // allowing the shape to be referenced using a relative shape ID.
        c: Bar,

        // Resolves to foo.baz#Bar
        // Absolute shape IDs do not perform namespace resolution.
        d: foo.baz#Bar,

        // Resolves to foo.baz#MyString
        // Absolute shape IDs do not perform namespace resolution.
        e: foo.baz#MyString,

        // Resolves to smithy.api#String
        // No shape named String was imported through a use statement
        // the smithy.example namespace does not contain a shape named
        // String, and the prelude model contains a shape named String.
        f: String,

        // Resolves to smithy.example#MyBoolean.
        // There is a shape named MyBoolean defined in the same namespace.
        // Forward references are supported both within the same file and
        // across multiple files.
        g: MyBoolean,

        // Invalid. A shape by this name has not been imported through a
        // use statement, a shape by this name does not exist in the
        // current namespace, and a shape by this name does not exist in
        // the prelude model.
        h: InvalidShape,
    }

    boolean MyBoolean


---------------
Defining shapes
---------------

Shape statements are used to define shapes.

.. productionlist:: smithy
    shape_statements             :*(`shape_statement` / `apply_statement`)
    shape_statement              :[`shape_documentation_comments` `ws`]
                                 :  `trait_statements`
                                 :  `shape_body` `br`
    shape_documentation_comments :*(`documentation_comment`)
    shape_body                   :`simple_shape_statement`
                                 :/ `list_statement`
                                 :/ `set_statement`
                                 :/ `map_statement`
                                 :/ `structure_statement`
                                 :/ `union_statement`
                                 :/ `service_statement`
                                 :/ `operation_statement`
                                 :/ `resource_statement`

.. _shape-names:

Shape names
===========

Consumers of a Smithy model MAY choose to inflect shape names, structure
member names, and other facets of a Smithy model in order to expose a more
idiomatic experience to particular programming languages. In order to make this
easier for consumers of a model, model authors SHOULD utilize a strict form of
PascalCase in which only the first letter of acronyms, abbreviations, and
initialisms are capitalized.

===========   ===============
Recommended   Not recommended
===========   ===============
UserId        UserID
ResourceArn   ResourceARN
IoChannel     IOChannel
HtmlEntity    HTMLEntity
HtmlEntity    HTML_Entity
===========   ===============


.. _simple-types:

------------
Simple types
------------

*Simple types* are types that do not contain nested types or shape references.
Shapes that are simple types are defined using the following grammar:

.. productionlist:: smithy
    simple_shape_statement :`simple_type_name` `ws` `identifier`
    simple_type_name       :"blob" / "boolean" / "document" / "string"
                           :/ "byte" / "short" / "integer" / "long"
                           :/ "float" / "double" / "bigInteger"
                           :/ "bigDecimal" / "timestamp"

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Type
      - Description
    * - blob
      - Uninterpreted binary data
    * - boolean
      - Boolean value type
    * - string
      - UTF-8 encoded string
    * - byte
      - 8-bit signed integer ranging from -128 to 127 (inclusive)
    * - short
      - 16-bit signed integer ranging from -32,768 to 32,767 (inclusive)
    * - integer
      - 32-bit signed integer ranging from -2^31 to (2^31)-1 (inclusive)
    * - long
      - 64-bit signed integer ranging from -2^63 to (2^63)-1 (inclusive)
    * - float
      - Single precision IEEE-754 floating point number
    * - double
      - Double precision IEEE-754 floating point number
    * - bigInteger
      - Arbitrarily large signed integer
    * - bigDecimal
      - Arbitrary precision signed decimal number
    * - timestamp
      - Represents an instant in time with no UTC offset or timezone. The
        serialization of a timestamp is determined by a
        :ref:`protocol <protocolDefinition-trait>`.
    * - document
      - A protocol-specific untyped value.

The following example defines a shape for each simple type in the
``smithy.example`` namespace:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        blob Blob
        boolean Boolean
        string String
        byte Byte
        short Short
        integer Integer
        long Long
        float Float
        double Double
        bigInteger BigInteger
        bigDecimal BigDecimal
        timestamp Timestamp
        document Document

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#Blob": {
                    "type": "blob"
                },
                "smithy.example#Boolean": {
                    "type": "boolean"
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
                },
                "smithy.example#Document": {
                    "type": "document"
                }
            }
        }

.. tip::

    The :ref:`prelude model <prelude>` contains shapes for every simple type.
    These shapes can be referenced using a relative shape ID
    (for example, ``String``) or using an absolute shape ID
    (for example, ``smithy.api#String``).


.. _timestamp-serialization-format:

Timestamp serialization format
==============================

By default, the serialization format of a timestamp is implicitly determined by
the :ref:`protocol <protocolDefinition-trait>` of a service; however, the serialization
format can be explicitly configured in some protocols to override the default format
using the :ref:`timestampFormat-trait`.

The following steps are taken to determine the serialization format of a
:ref:`member <member>` that targets a timestamp:

1. Use the ``timestampFormat`` trait of the member, if present.
2. Use the ``timestampFormat`` trait of the shape, if present.
3. Use the default format of the protocol.

The timestamp shape is an abstraction of time; the serialization format of a
timestamp as it is sent over the wire, whether determined by the protocol or by
the ``timestampFormat`` trait, SHOULD NOT have any effect on the types exposed
by tooling to represent a timestamp.


.. _document-type:

Document types
==============

A document type represents a protocol-specific untyped value. Documents
are useful when interacting with data that has no predefined schema,
uses a schema language that is not compatible with Smithy, or if the schema
that defines the data is specified and versioned outside of the
Smithy model.

.. note::

    * Not all protocols support document types
    * The serialization format of a document is protocol-specific.


.. _aggregate-types:

---------------
Aggregate types
---------------

Aggregate types are types that are composed of other types. Aggregate shapes
reference other shapes using :ref:`members <member>`.

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Type
      - Description
    * - :ref:`member`
      - Defined in aggregate types to reference other shapes
    * - :ref:`list`
      - homogeneous collection of values
    * - :ref:`set`
      - Unordered collection of unique homogeneous values
    * - :ref:`map`
      - Map data structure that maps string keys to homogeneous values
    * - :ref:`structure`
      - Fixed set of named heterogeneous members
    * - :ref:`union`
      - Tagged union data structure that can take on one of several
        different, but fixed, types


.. _member:

Member
======

:dfn:`Members` are defined in aggregate types to reference other shapes using
a :ref:`shape ID <shape-id>`. A member MUST NOT target an ``operation``,
``resource``, ``service``, ``member``, or :ref:`trait shapes <trait-shapes>`.

The following example defines a list shape. The member of the list is a
member shape with a shape ID of ``smithy.example#MyList$member``. The member
targets the ``MyString`` shape in the same namespace.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        list MyList {
            member: MyString
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyList": {
                    "member": {
                        "target": "smithy.example#MyString"
                    }
                }
            }
        }

All shapes that contain members use following ABNF to define members:

.. productionlist:: smithy
    shape_members           :`empty_shape_members` / `populated_shape_members`
    empty_shape_members     :"{" `ws` "}"
    populated_shape_members :"{" `ws` `shape_member_kvp`
                            :  *(`comma` `shape_member_kvp` `ws`) `trailing_comma` "}"
    shape_member_kvp        :[`shape_documentation_comments`]
                            :  `trait_statements`
                            :  `identifier` `ws` ":" `ws` `shape_id`


.. _list:

List
====

The :dfn:`list` type represents a homogeneous collection of values. A list
statement requires that a :ref:`member <member>` named ``member`` is defined
in its body. Lists are defined using the following grammar:

.. productionlist:: smithy
    list_statement :"list" `ws` `identifier` `ws` `shape_members`

The following example defines a list with a string member from the
:ref:`prelude <prelude>`:

.. tabs::

    .. code-tab:: smithy

        list MyList {
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyList": {
                    "member": {
                        "target": "smithy.api#String"
                    }
                }
            }
        }

Traits can be applied to the list shape and its member:

.. tabs::

    .. code-tab:: smithy

        @length(min: 3, max: 10)
        list MyList {
            @length(min: 1, max: 100)
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyList": {
                    "member": {
                        "target": "smithy.api#String",
                        "traits": {
                            "smithy.api#length": {
                                "min": 1,
                                "max": 100
                            }
                        }
                    },
                    "traits": {
                        "smithy.api#length": {
                            "min": 3,
                            "max": 10
                        }
                    }
                }
            }
        }


.. _set:

Set
===

The :dfn:`set` type represents an unordered collection of unique homogeneous
values. A set statement requires that a :ref:`member <member>` named
``member`` is defined in its body. Sets are defined using the following
grammar:

.. productionlist:: smithy
    set_statement :"set" `ws` `identifier` `ws` `shape_members`

The following example defines a set of strings:

.. tabs::

    .. code-tab:: smithy

        set StringSet {
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#StringSet": {
                    "member": {
                        "target": "smithy.api#String"
                    }
                }
            }
        }

Traits can be applied to the set shape and its members:

.. tabs::

    .. code-tab:: smithy

        @deprecated
        set StringSet {
            @sensitive
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#StringSet": {
                    "member": {
                        "target": "smithy.api#String"
                    },
                    "traits": {
                        "smithy.api#deprecated": {}
                    }
                }
            }
        }

.. note::

    Not all languages support set data structures. Such languages SHOULD
    represent sets as a custom set data structure that can interpret value
    hash codes and equality, or alternatively, store the values of a set
    data structure in a list and rely on validation to ensure uniqueness.


.. _map:

Map
===

The :dfn:`map` type represents a map data structure that maps string keys to
homogeneous values. A map cannot contain duplicate keys. A map statement
requires that a ``key`` and ``value`` :ref:`member <member>` are defined in
its body. The ``key`` member of a map MUST target a ``string`` shape.

Maps are defined using the following grammar:

.. productionlist:: smithy
    map_statement :"map" `ws` `identifier` `ws` `shape_members`

The following example defines a map of strings to integers:

.. tabs::

    .. code-tab:: smithy

        map IntegerMap {
            key: String,
            value: Integer
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#IntegerMap": {
                    "key": {
                        "target": "smithy.api#String"
                    },
                    "value": {
                        "target": "smithy.api#String"
                    }
                }
            }
        }

Traits can be applied to the map shape and its members:

.. tabs::

    .. code-tab:: smithy

        @length(min: 0, max: 100)
        map IntegerMap {
            @length(min: 1, max: 10)
            key: String,

            @sensitive
            value: Integer
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#IntegerMap": {
                    "key": {
                        "target": "smithy.api#String",
                        "traits": {
                            "smithy.api#length": {
                                "min": 1,
                                "max": 10
                            }
                        }
                    },
                    "value": {
                        "target": "smithy.api#String",
                        "traits": {
                            "smithy.api#sensitive": {}
                        }
                    },
                    "traits": {
                        "smithy.api#length": {
                            "min": 0,
                            "max": 100
                        }
                    }
                }
            }
        }


.. _structure:

Structure
=========

The :dfn:`structure` type represents a fixed set of named, unordered,
heterogeneous members. A member name maps to exactly one structure
:ref:`member <member>` definition. A structure is defined using the
following grammar:

.. productionlist:: smithy
    structure_statement     :"structure" `ws` `identifier` `ws` `shape_members`

The following example defines a structure with two members:

.. tabs::

    .. code-tab:: smithy

        structure MyStructure {
            foo: String,
            baz: Integer,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyStructure": {
                    "type": "structure",
                    "members": {
                        "foo": {
                            "target": "smithy.api#String"
                        },
                        "baz": {
                            "target": "smithy.api#Integer"
                        }
                    }
                }
            }
        }

Traits can be applied to structure members:

.. tabs::

    .. code-tab:: smithy

        /// This is MyStructure.
        structure MyStructure {
            /// This is documentation for `foo`.
            @required
            foo: String,

            /// This is documentation for `baz`.
            @deprecated
            baz: Integer,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyStructure": {
                    "type": "structure",
                    "members": {
                        "foo": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#documentation": "This is documentation for `foo`.",
                                "smithy.api#required": {}
                            }
                        },
                        "baz": {
                            "target": "smithy.api#Integer",
                            "traits": {
                                "smithy.api#documentation": "This is documentation for `baz`.",
                                "smithy.api#deprecated": {}
                            }
                        }
                    },
                    "traits": {
                        "smithy.api#documentation": "This is MyStructure."
                    }
                }
            }
        }

.. _union:

Union
=====

The union type represents a `tagged union data structure`_ that can take
on several different, but fixed, types. Only one type can be used at any
one time. A union is defined using the following grammar:

.. productionlist:: smithy
    union_statement :"union" `ws` `identifier` `ws` `shape_members`

The following example defines a union shape with several members:

.. tabs::

    .. code-tab:: smithy

        union MyUnion {
            i32: Integer,

            stringA: String,

            @sensitive
            stringB: String,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyUnion": {
                    "type": "union",
                    "members": {
                        "i32": {
                            "target": "smithy.api#Integer"
                        },
                        "stringA": {
                            "target": "smithy.api#String"
                        },
                        "stringB": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#sensitive": {}
                            }
                        }
                    }
                }
            }
        }


.. _default-values:

Default values
==============

The values provided for :ref:`members <member>` of
:ref:`aggregate shapes <aggregate-types>` are either always present and
set to a default value when necessary or *boxed*, meaning a value is
optionally present with no default value.

- The default value of a ``byte``, ``short``, ``integer``, ``long``,
  ``float``, and ``double`` shape that is not boxed is zero.
- The default value of a ``boolean`` shape that is not boxed is ``false``.
- All other shapes are always considered boxed and have no default value.

Members are considered boxed if and only if the member is marked with the
:ref:`box-trait` or the shape targeted by the member is marked
with the box trait. Members that target strings, timestamps, and
aggregate shapes are always considered boxed and have no default values.


Recursive shape definitions
===========================

Smithy allows for recursive shape definitions with the following constraint:
the member of a list, set, or map cannot directly or transitively target its
containing shape unless one or more members in the path from the container
back to itself targets a structure or union shape. This ensures that shapes
that are typically impossible to define in various programming languages are
not defined in Smithy models (for example, you can't define a recursive list
in Java ``List<List<List....``).

The following shape definition is invalid:

.. tabs::

    .. code-tab:: smithy

        list RecursiveList {
            member: RecursiveList
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#RecursiveList": {
                    "type": "list",
                    "member": {
                        "target": "smithy.example#RecursiveList"
                    }
                }
            }
        }

The following shape definition is valid:

.. tabs::

    .. code-tab:: smithy

        list ValidList {
            member: IntermediateStructure
        }

        structure IntermediateStructure {
            foo: ValidList
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#ValidList": {
                    "type": "list",
                    "member": {
                        "target": "smithy.example#IntermediateStructure"
                    }
                },
                "smithy.example#IntermediateStructure": {
                    "type": "structure",
                    "members": {
                        "foo": {
                            "target": "smithy.example#ValidList"
                        }
                    }
                }
            }
        }

.. _service-types:

-------------
Service types
-------------

*Service types* are types that form services, resources, and operations.

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Type
      - Description
    * - :ref:`service <service>`
      - Entry point of an API that aggregates resources and operations together
    * - :ref:`operation <operation>`
      - Represents the input, output and possible errors of an API operation
    * - :ref:`resource <resource>`
      - Entity with an identity that has a set of operations


..  _service:

Service
=======

A :dfn:`service` is the entry point of an API that aggregates resources and
operations together. The :ref:`resources <resource>` and
:ref:`operations <operation>` of an API are bound within the closure of a
service. A service shape is defined by the following grammar:

.. productionlist:: smithy
    service_statement :"service" `ws` `identifier` `ws` `node_object`

The service shape supports the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - version
      - ``string``
      - **Required**. Defines the version of the service. The version can be
        provided in any format (e.g., ``2017-02-11``, ``2.0``, etc).
    * - :ref:`operations <service-operations>`
      - [:ref:`shape-id`]
      - Binds a list of operations to the service. Each element in the list is
        a shape ID that MUST target an operation.
    * - :ref:`resources <service-resources>`
      - [:ref:`shape-id`]
      - Binds a list of resources to the service. Each element in the list is
        a shape ID that MUST target a resource.

The following example defines a service with no operations or resources.

.. tabs::

    .. code-tab:: smithy

        service MyService {
            version: "2017-02-11"
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyService": {
                    "type": "service",
                    "version": "2017-02-11"
                }
            }
        }


.. _service-operations:

Service operations
------------------

:ref:`Operation <operation>` shapes can be bound to a service by adding the
shape ID of an operation to the ``operations`` property of a service.
Operations bound directly to a service are typically RPC-style operations
that do not fit within a resource hierarchy.

.. tabs::

    .. code-tab:: smithy

        service MyService {
            version: "2017-02-11",
            operations: [GetServerTime],
        }

        @readonly
        operation GetServerTime {
            output: GetServerTimeOutput
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyService": {
                    "type": "service",
                    "version": "2017-02-11",
                    "operations": [
                        {
                            "target": "smithy.example#GetServerTime"
                        }
                    ]
                },
                "smithy.example#GetServerTime": {
                    "type": "operation",
                    "output": {
                        "target": "smithy.example#GetServerTimeOutput"
                    }
                }
            }
        }


.. _service-resources:

Service resources
-----------------

:ref:`Resource <resource>` shapes can be bound to a service by adding the
shape ID of a resource to the ``resources`` property of a service.

.. tabs::

    .. code-tab:: smithy

        service MyService {
            version: "2017-02-11",
            resources: [MyResource],
        }

        resource MyResource {}

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyService": {
                    "type": "service",
                    "version": "2017-02-11",
                    "resources": [
                        {
                            "target": "smithy.example#MyResource"
                        }
                    ]
                },
                "smithy.example#MyResource": {
                    "type": "resource"
                }
            }
        }


.. _service-closure:

Service closure
---------------

The *closure* of a service is the set of shapes connected to a service
through resources, operations, and members.

.. important::

    With some exceptions, the shapes that are referenced in the *closure*
    of a service MUST have case-insensitively unique names regardless of
    their namespace.

By requiring unique names within a service, each service forms a
`ubiquitous language`_, making it easier for developers to understand the
model and artifacts generated from the model, like code. For example, when
using Java code generated from a Smithy model, a developer should not need
to discern between ``BadRequestException`` classes across multiple packages
that can be thrown by an operation. Uniqueness is required
case-insensitively because many model transformations (like code generation)
change the casing and inflection of shape names to make artifacts more
idiomatic.

:ref:`Simple types <simple-types>` and :ref:`lists <list>` or
:ref:`sets <set>` of compatible simple types are allowed to conflict because
a conflict for these type would rarely have an impact on generated artifacts.
These kinds of conflicts are only allowed if both conflicting shapes are the
same type and have the exact same traits.

An operation or resource MUST NOT be bound to multiple shapes within the
closure of a service. This constraint allows services to discern between
operations and resources using only their shape name rather than a
fully-qualified path from the service to the shape.


..  _operation:

Operation
=========

The :dfn:`operation` type represents the input, output, and possible errors of
an API operation. Operation shapes are bound to :ref:`resource <resource>`
shapes and :ref:`service <service>` shapes. An operation shape is defined by
the following grammar:

.. productionlist:: smithy
    operation_statement :"operation" `ws` `identifier` `ws` `node_object`

An operation supports the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Type
      - Description
    * - :ref:`input <operation-input>`
      - The optional input structure of the operation.
    * - :ref:`output <operation-output>`
      - The optional output structure of the operation.
    * - :ref:`errors <operation-errors>`
      - The optional list of errors the operation can return.

The following example defines an operation shape that accepts an input
structure named ``Input``, returns an output structure named ``Output``, and
can potentially return the ``NotFound`` or ``BadRequest``
:ref:`error structures <error-trait>`.

.. tabs::

    .. code-tab:: smithy

        operation MyOperation {
            input: Input,
            output: Output,
            errors: [NotFound, BadRequest]
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyOperation": {
                    "type": "operation",
                    "input": {
                        "target": "smithy.example#Input"
                    },
                    "output": {
                        "target": "smithy.example#Output"
                    },
                    "errors": [
                        {
                            "target": "smithy.example#NotFound"
                        },
                        {
                            "target": "smithy.example#BadRequest"
                        }
                    ]
                }
            }
        }

.. _operation-input:

Operation input
---------------

The input of an operation is an optional shape ID that MUST target a
structure shape. An operation is not required to accept input.

The following example defines an operation that accepts an input structure
named ``Input``:

.. tabs::

    .. code-tab:: smithy

        operation MyOperation {
            input: Input
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyOperation": {
                    "type": "operation",
                    "input": {
                        "target": "smithy.example#Input"
                    }
                }
            }
        }


.. _operation-output:

Operation output
----------------

The output of an operation is an optional shape ID that MUST target a
structure shape. An operation is not required to return output.

The following example defines an operation that returns an output
structure named ``Output``:

.. tabs::

    .. code-tab:: smithy

        operation MyOperation {
            output: Output
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyOperation": {
                    "type": "operation",
                    "output": {
                        "target": "smithy.example#Output"
                    }
                }
            }
        }


.. _operation-errors:

Operation errors
----------------

The errors of an operation is an optional array of shape IDs that MUST
target structure shapes that are marked with the :ref:`error-trait`. Errors
defined on an operation are errors that can potentially occur when calling
an operation.

The following example defines an operation shape that accepts no input,
returns no output, and can potentially return the
``NotFound`` or ``BadRequest`` error structures.

.. tabs::

    .. code-tab:: smithy

        operation MyOperation {
            errors: [NotFound, BadRequest]
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyOperation": {
                    "type": "operation",
                    "errors": [
                        {
                            "target": "smithy.example#NotFound"
                        },
                        {
                            "target": "smithy.example#BadRequest"
                        }
                    ]
                }
            }
        }


..  _resource:

Resource
========

Smithy defines a :dfn:`resource` as an entity with an identity that has a
set of operations. A resource shape is defined by the following grammar:

.. productionlist:: smithy
    resource_statement :"resource" `ws` `identifier` `ws` `node_object`

A resource supports the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 30 60

    * - Property
      - Type
      - Description
    * - :ref:`identifiers <resource-identifiers>`
      - Map<String, :ref:`shape-id`>
      - Defines identifier names and shape IDs used to identify the resource.
    * - :ref:`create <create-lifecycle>`
      - :ref:`shape-id`
      - Defines the lifecycle operation used to create a resource using one
        or more identifiers created by the service.
    * - :ref:`put <put-lifecycle>`
      - :ref:`shape-id`
      - Defines an idempotent lifecycle operation used to create a resource
        using identifiers provided by the client.
    * - :ref:`read <read-lifecycle>`
      - :ref:`shape-id`
      - Defines the lifecycle operation used to retrieve the resource.
    * - :ref:`update <update-lifecycle>`
      - :ref:`shape-id`
      - Defines the lifecycle operation used to update the resource.
    * - :ref:`delete <delete-lifecycle>`
      - :ref:`shape-id`
      - Defines the lifecycle operation used to delete the resource.
    * - :ref:`list <list-lifecycle>`
      - :ref:`shape-id`
      - Defines the lifecycle operation used to list resources of this type.
    * - operations
      - [:ref:`shape-id`]
      - Binds a list of non-lifecycle instance operations to the resource.
    * - collectionOperations
      - [:ref:`shape-id`]
      - Binds a list of non-lifecycle collection operations to the resource.
    * - resources
      - [:ref:`shape-id`]
      - Binds a list of resources to this resource as a child resource,
        forming a containment relationship. The resources MUST NOT have a
        cyclical containment hierarchy, and a resource can not be bound more
        than once in the entire closure of a resource or service.


.. _resource-identifiers:

Identifiers
-----------

:dfn:`Identifiers` are used to refer to a specific resource within a service.
The identifiers property of a resource is a map of identifier names to
:ref:`shape IDs <shape-id>` that MUST target string shapes.

For example, the following model defines a ``Forecast`` resource with a
single identifier named ``forecastId`` that targets the ``ForecastId`` shape:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        resource Forecast {
            identifiers: {
                forecastId: ForecastId
            }
        }

        string ForecastId

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#Forecast": {
                    "type": "resource",
                    "identifiers": {
                        "forecastId": {
                            "target": "smithy.example#ForecastId"
                        }
                    }
                },
                "smithy.example#ForecastId": {
                    "type": "string"
                }
            }
        }

When a resource is bound as a child to another resource using the "resources"
property, all of the identifiers of the parent resource MUST be repeated
verbatim in the child resource, and the child resource MAY introduce any
number of additional identifiers.

:dfn:`Parent identifiers` are the identifiers of the parent of a resource.
All parent identifiers MUST be bound as identifiers in the input of every
operation bound as a child to a resource. :dfn:`Child identifiers` are the
identifiers that a child resource contains that are not present in the parent
identifiers.

For example, given the following model,

.. tabs::

    .. code-tab:: smithy

        resource ResourceA {
            identifiers: {
                a: String
            },
            resources: [ResourceB],
        }

        resource ResourceB {
            identifiers: {
                a: String,
                b: String,
            },
            resources: [ResourceC],
        }

        resource ResourceC {
            identifiers: {
                a: String,
                b: String,
                c: String,
            }
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#ResourceA": {
                    "type": "resource",
                    "resources": [
                        {
                            "target": "smithy.example#ResourceB"
                        }
                    ],
                    "identifiers": {
                        "a": {
                            "target": "smithy.api#String"
                        }
                    }
                },
                "smithy.example#ResourceB": {
                    "type": "resource",
                    "resources": [
                        {
                            "target": "smithy.example#ResourceC"
                        }
                    ],
                    "identifiers": {
                        "a": {
                            "target": "smithy.api#String"
                        },
                        "b": {
                            "target": "smithy.api#String"
                        }
                    }
                },
                "smithy.example#ResourceC": {
                    "type": "resource",
                    "identifiers": {
                        "a": {
                            "target": "smithy.api#String"
                        },
                        "b": {
                            "target": "smithy.api#String"
                        },
                        "c": {
                            "target": "smithy.api#String"
                        }
                    }
                }
            }
        }

``ResourceB`` is a valid child of ``ResourceA`` and contains a child
identifier of "b". ``ResourceC`` is a valid child of ``ResourceB`` and
contains a child identifier of "c".

However, the following defines two *invalid* child resources that do not
define an ``identifiers`` property that is compatible with their parents:

.. tabs::

    .. code-tab:: smithy

        resource ResourceA {
            identifiers: {
                a: String,
                b: String,
            },
            resources: [Invalid1, Invalid2],
        }

        resource Invalid1 {
            // Invalid: missing "a".
            identifiers: {
                b: String,
            },
        }

        resource Invalid2 {
            identifiers: {
                a: String,
                // Invalid: does not target the same shape.
                b: SomeOtherString,
            },
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#ResourceA": {
                    "type": "resource",
                    "identifiers": {
                        "a": {
                            "target": "smithy.api#String"
                        },
                        "b": {
                            "target": "smithy.api#String"
                        }
                    },
                    "resources": [
                        {
                            "target": "smithy.example#Invalid1"
                        },
                        {
                            "target": "smithy.example#Invalid2"
                        }
                    ]
                },
                "smithy.example#Invalid1": {
                    "type": "resource",
                    "identifiers": {
                        "b": {
                            "target": "smithy.api#String"
                        }
                    }
                },
                "smithy.example#Invalid2": {
                    "type": "resource",
                    "identifiers": {
                        "a": {
                            "target": "smithy.api#String"
                        },
                        "b": {
                            "target": "smithy.example#SomeOtherString"
                        }
                    }
                }
            }
        }

.. _binding-identifiers:

Binding identifiers to operations
---------------------------------

*Identifier bindings* indicate which top-level members of the input structure
of an operation provide values for the identifiers of a resource.

**Validation**

- Child resources MUST provide identifier bindings for all of its parent's
  identifiers.
- Identifier bindings are only formed on input structure members that are
  marked as :ref:`required-trait`.
- Resource operations MUST form a valid *instance operation* or
  *collection operation*.

.. _instance-operations:

:dfn:`Instance operations` are formed when all of the identifiers of a resource
are bound to the input structure of an operation or when a resource has no
identifiers. The :ref:`put <put-lifecycle>`, :ref:`read <read-lifecycle>`,
:ref:`update <update-lifecycle>`, and :ref:`delete <delete-lifecycle>`
lifecycle operations are examples of instance operations. An operation bound
to a resource using `operations` MUST form a valid instance operation.

.. _collection-operations:

:dfn:`Collection operations` are used when an operation is meant to operate on
a collection of resources rather than a specific resource. Collection
operations are formed when an operation is bound to a resource with `collectionOperations`,
or when bound to the :ref:`list <list-lifecycle>` or :ref:`create <create-lifecycle>`
lifecycle operations. A collection operation MUST omit one or more identifiers
of the resource it is bound to, but MUST bind all identifiers of any parent
resource.


.. _implicit-identifier-bindings:

Implicit identifier bindings
----------------------------

*Implicit identifier bindings* are formed when the input of an operation
contains member names that target the same shapes that are defined in the
"identifiers" property of the resource to which an operation is bound.

For example, given the following model,

.. tabs::

    .. code-tab:: smithy

        resource Forecast {
            identifiers: {
                forecastId: ForecastId,
            },
            read: GetForecast,
        }

        @readonly
        operation GetForecast {
            input: GetForecastInput,
            output: GetForecastOutput
        }

        structure GetForecastInput {
            @required
            forecastId: ForecastId,
        }

        structure GetForecastOutput {
            @required
            weather: WeatherData,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#Forecast": {
                    "type": "resource",
                    "identifiers": {
                        "forecastId": {
                            "target": "smithy.example#ForecastId"
                        }
                    },
                    "read": {
                        "target": "smithy.example#GetForecast"
                    }
                },
                "smithy.example#GetForecast": {
                    "type": "operation",
                    "input": {
                        "target": "smithy.example#GetForecastInput"
                    },
                    "output": {
                        "target": "smithy.example#GetForecastOutput"
                    },
                    "traits": {
                        "smithy.api#readonly": {}
                    }
                },
                "smithy.example#GetForecastInput": {
                    "type": "structure",
                    "members": {
                        "forecastId": {
                            "target": "smithy.example#ForecastId",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                },
                "smithy.example#GetForecastOutput": {
                    "type": "structure",
                    "members": {
                        "weather": {
                            "target": "smithy.example#WeatherData",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                }
            }
        }

``GetForecast`` forms a valid instance operation because the operation is
not marked with the ``collection`` trait and ``GetForecastInput`` provides
*implicit identifier bindings* by defining a required "forecastId" member
that targets the same shape as the "forecastId" identifier of the resource.

Implicit identifier bindings for collection operations are created in a
similar way to an instance operation, but MUST NOT contain identifier bindings
for *all* child identifiers of the resource.

Given the following model,

.. tabs::

    .. code-tab:: smithy

        resource Forecast {
            identifiers: {
                forecastId: ForecastId,
            },
            collectionOperations: [BatchPutForecasts],
        }

        operation BatchPutForecasts {
            input: BatchPutForecastsInput,
            output: BatchPutForecastsOutput
        }

        structure BatchPutForecastsInput {
            @required
            forecasts: BatchPutForecastList,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#Forecast": {
                    "type": "resource",
                    "identifiers": {
                        "forecastId": {
                            "target": "smithy.example#ForecastId"
                        }
                    },
                    "collectionOperations": [
                        {
                            "target": "smithy.example#BatchPutForecasts"
                        }
                    ]
                },
                "smithy.example#BatchPutForecasts": {
                    "type": "operation",
                    "input": {
                        "target": "smithy.example#BatchPutForecastsInput"
                    },
                    "output": {
                        "target": "smithy.example#BatchPutForecastsOutput"
                    }
                },
                "smithy.example#BatchPutForecastsInput": {
                    "type": "structure",
                    "members": {
                        "forecasts": {
                            "target": "smithy.example#BatchPutForecastList",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                }
            }
        }

``BatchPutForecasts`` forms a valid collection operation with implicit
identifier bindings because ``BatchPutForecastsInput`` does not require an
input member named "forecastId" that targets ``ForecastId``.


Explicit identifier bindings
----------------------------

*Explicit identifier bindings* are defined by applying the
:ref:`resourceIdentifier-trait` to a member of the input of for an
operation bound to a resource. Explicit bindings are necessary when the name of
the input structure member differs from the name of the resource identifier to
which the input member corresponds.

For example, given the following,

.. code-block:: smithy

    resource Forecast {
        // continued from above
        resources: [HistoricalForecast],
    }

    resource HistoricalForecast {
        identifiers: {
            forecastId: ForecastId,
            historicalId: HistoricalForecastId,
        },
        read: GetHistoricalForecast,
        list: ListHistoricalForecasts,
    }

    @readonly
    operation GetHistoricalForecast {
        input: GetHistoricalForecastInput,
        output: GetHistoricalForecastOutput
    }

    structure GetHistoricalForecastInput {
        @required
        @resourceIdentifier("forecastId")
        customForecastIdName: ForecastId,

        @required
        @resourceIdentifier("historicalId")
        customHistoricalIdName: String
    }

the :ref:`resourceIdentifier-trait` on ``GetHistoricalForecastInput$customForecastIdName``
maps it to the "forecastId" identifier is provided by the
"customForecastIdName" member, and the :ref:`resourceIdentifier-trait`
on ``GetHistoricalForecastInput$customHistoricalIdName`` maps that member
to the "historicalId" identifier.


.. _lifecycle-operations:

Lifecycle operations
--------------------

:dfn:`Lifecycle operations` are used to transition the state of a resource
using well-defined semantics. Lifecycle operations are defined by setting the
``put``, ``create``, ``read``, ``update``, ``delete``, and ``list`` properties
of a resource to target an operation shape.

The following snippet defines a resource with each lifecycle method:

.. code-block:: smithy

    resource Forecast {
        identifiers: {
            forecastId: ForecastId,
        },
        put: PutForecast,
        create: CreateForecast,
        read: GetForecast,
        update: UpdateForecast,
        delete: DeleteForecast,
        list: ListForecasts,
    }


.. _put-lifecycle:

Put lifecycle
-------------

The ``put`` lifecycle operation is used to create a resource using identifiers
provided by the client.

**Validation**

- Put operations MUST NOT be marked with :ref:`readonly-trait`.
- Put operations MUST be marked with :ref:`idempotent-trait`.
- Put operations MUST form valid :ref:`instance operations <instance-operations>`.

The following snippet defines the ``PutForecast`` operation.

.. code-block:: smithy

    operation PutForecast {
        input: PutForecastInput,
        output: PutForecastOutput
    }

    @idempotent
    structure PutForecastInput {
        // The client provides the resource identifier.
        @required
        forecastId: ForecastId,

        chanceOfRain: Float
    }

The semantics of a ``put`` lifecycle operation are similar to the semantics
of an `HTTP PUT method`_:

  The PUT method requests that the state of the target resource be
  created or replaced ...

The :ref:`noReplace-trait` can be applied to resources that define a
``put`` lifecycle operation to indicate that a resource cannot be
replaced using the ``put`` operation.


.. _create-lifecycle:

Create lifecycle
----------------

The ``create`` operation is used to create a resource using one or more
identifiers created by the service.

**Validation**

- Create operations MUST NOT be marked with :ref:`readonly-trait`.
- Create operations MUST form valid :ref:`collection operations <collection-operations>`.
- The ``create`` operation MAY be marked with :ref:`idempotent-trait`.

The following snippet defines the ``CreateForecast`` operation.

.. code-block:: smithy

    operation CreateForecast {
        input: CreateForecastInput,
        output: CreateForecastOutput
    }

    @collection
    operation CreateForecast {
        input: CreateForecastInput,
        output: CreateForecastOutput
    }

    structure CreateForecastInput {
        // No identifier is provided by the client, so the service is
        // responsible for providing the identifier of the resource.
        chanceOfRain: Float,
    }


.. _read-lifecycle:

Read lifecycle
--------------

The ``read`` operation is the canonical operation used to retrieve the current
representation of a resource.

**Validation**

- Read operations MUST be valid :ref:`instance operations <instance-operations>`.
- Read operations MUST be marked with :ref:`readonly-trait`.

For example:

.. code-block:: smithy

    @readonly
    operation GetForecast {
        input: GetForecastInput,
        output: GetForecastOutput,
        errors: [ResourceNotFound]
    }

    structure GetForecastInput {
        @required
        forecastId: ForecastId,
    }


.. _update-lifecycle:

Update lifecycle
----------------

The ``update`` operation is the canonical operation used to update a
resource.

**Validation**

- Update operations MUST be valid :ref:`instance operations <instance-operations>`.
- Update operations MUST NOT be marked with :ref:`readonly-trait`.

For example:

.. code-block:: smithy

    operation UpdateForecast {
        input: UpdateForecastInput,
        output: UpdateForecastOutput,
        errors: [ResourceNotFound]
    }

    structure UpdateForecastInput {
        @required
        forecastId: ForecastId,

        chanceOfRain: Float,
    }


.. _delete-lifecycle:

Delete lifecycle
----------------

The ``delete`` operation is canonical operation used to delete a resource.

**Validation**

- Delete operations MUST be valid :ref:`instance operations <instance-operations>`.
- Delete operations MUST NOT be marked with :ref:`readonly-trait`.
- Delete operations MUST be marked with :ref:`idempotent-trait`.

For example:

.. code-block:: smithy

    @idempotent
    operation DeleteForecast {
        input: DeleteForecastInput,
        output: DeleteForecastOutput,
        errors: [ResourceNotFound]
    }

    structure DeleteForecastInput {
        @required
        forecastId: ForecastId,
    }


.. _list-lifecycle:

List lifecycle
--------------

The ``list`` operation is the canonical operation used to list a
collection of resources.

**Validation**

- List operations MUST form valid :ref:`collection operations <collection-operations>`.
- List operations MUST be marked with :ref:`readonly-trait`.
- The output of a list operation SHOULD contain references to the resource
  being listed.
- List operations SHOULD be :ref:`paginated <paginated-trait>`.

For example:

.. code-block:: smithy

    @collection @readonly @paginated
    operation ListForecasts {
        input: ListForecastsInput,
        output: ListForecastsOutput
    }

    structure ListForecastsInput {
        maxResults: Integer,
        nextToken: String
    }

    structure ListForecastsOutput {
        nextToken: String,
        @required
        forecasts: ForecastList
    }

    list ForecastList {
        member: ForecastId
    }


.. _referencing-resources:

Referencing resources
---------------------

References between resources can be defined in a Smithy model at design-time.
Resource references allow tooling to understand the relationships between
resources and how to dereference the location of a resource.

A reference to a resource is formed when the :ref:`references-trait`
is applied to a structure or string shape. The following example creates a
reference to a ``HistoricalForecast`` resource (a resource that requires the
"forecastId" and "historicalId" identifiers):

.. code-block:: smithy

    @references([{resource: HistoricalForecast}])
    structure HistoricalReference {
        forecastId: ForecastId,
        historicalId: HistoricalForecastId
    }

Notice that in the above example, the identifiers of the resource were not
explicitly mapped to structure members. This is because the targeted structure
contains members with names that match the names of the identifiers of the
``HistoricalForecast`` resource.

Explicit mappings between identifier names and structure member names can be
defined if needed. For example:

.. code-block:: smithy

    @references([
        {
            resource: HistoricalForecast,
            ids: {
                forecastId: "customForecastId",
                historicalId: "customHistoricalId"
            }
        }
    ])
    structure AnotherHistoricalReference {
        customForecastId: String,
        customHistoricalId: String,
    }

A reference can be formed on a string shape for resources that have one
identifier. References applied to a string shape MUST omit the "ids"
property in the reference.

.. code-block:: smithy

    resource SimpleResource {
        identifiers: {
            foo: String,
        }
    }

    @references([{resource: SimpleResource}])
    string SimpleResourceReference

See the :ref:`references-trait` for more information about references.

.. _tagged union data structure: https://en.wikipedia.org/wiki/Tagged_union
.. _ubiquitous language: https://martinfowler.com/bliki/UbiquitousLanguage.html
.. _HTTP PUT method: https://tools.ietf.org/html/rfc7231#section-4.3.4
