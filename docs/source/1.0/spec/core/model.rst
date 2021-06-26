.. _smithy-model:

================
The Smithy model
================

The *Smithy model* describes the Smithy semantic model and the files used to
create it. Smithy models are used to describe services and data structures.

.. contents:: Table of contents
    :depth: 1
    :local:
    :backlinks: none


.. _smithy-overview:

---------------
Smithy overview
---------------

Smithy is a framework that consists of a semantic model, file formats used to
define a model, and a build process used to validate models and facilitate
model transformations.

.. text-figure::
    :caption: **Figure 1.1**: Smithy framework concepts
    :name: figure-1.1

                    ┌────────────────┐ part of          ┌────────────────┐
                    │                │╲                ╱│                │
                    │ Semantic Model │─○──────────────○─│   Model File   │
                    │                │╱                ╲│                │
                    └────────────────┘                  └────────────────┘
                                               split into       ╲│╱
                                                                 ○
                                                                 │
                    ┌────────────────┐                           ┼
                    │JSON AST (.json)│────────┐         ┌────────────────┐
                    └────────────────┘        │         │                │
                                              ├────────▷│ Representation │
                    ┌────────────────┐        │         │                │
                    │ IDL (.smithy)  │────────┘         └────────────────┘
                    └────────────────┘

Semantic model
    The in-memory model used by tools. The :ref:`semantic model <semantic-model>`
    may be serialized into one or more model file representations.

.. _model-files:

Model File
    A file on the file system, in a particular representation. The model files
    that make up a semantic model MAY be split across multiple files to
    improve readability or modularity, and those files are not required to
    use the same representation. Model files do not explicitly include other
    model files; this responsibility is left to tooling to ensure that all
    necessary model files are merged together to form a valid semantic model.

    One or more model files can be :ref:`assembled (or merged) <merging-models>`
    together to form a semantic model.
Representation
    A particular model file format such as the Smithy IDL or JSON AST.
    Representations are loaded into the semantic model by mapping the
    representation to concepts in the semantic model.

    * The :ref:`Smithy IDL <idl>` is a human-readable format that aims to
      streamline authoring and reading models.
    * The :ref:`JSON AST <json-ast>` aims to provide a more machine-readable
      format to easily share models across language implementations and better
      integrate with JSON-based ecosystems.


.. _semantic-model:

------------------
The semantic model
------------------

Smithy's *semantic model* is an in-memory model used by tools. It is
independent of any particular serialized representation. The semantic
model contains :ref:`metadata <metadata>` and a graph of
:ref:`shapes <shapes>` connected by :ref:`shape IDs <shape-id>`.

.. text-figure::
    :caption: **Figure 1.2**: The semantic model
    :name: figure-1.2

                                          ┌───────────────┐
                                          │Semantic Model │╲
                                          ├───────────────┤─○────────┐
                                          │metadata?      │╱         │
                                          │               │          │
                                          │               │          │
                                          └───────────────┘          │
                                                  ┼     ┼ prelude    │
                                                  │     ○────────────┘
                                                  ○
                                           shapes╱│╲
        ┌───────────────┐                 ┌───────────────┐
        │ Applied Trait │╲          shape │  «abstract»   │
        ├───────────────┤─○──────────────┼│     Shape     │            ┌───────────────┐
        │               │╱                ├───────────────┤            │    ShapeID    │
        │               │                 │               │            ├───────────────┤
        │               │╲     applied-to │               │         id │namespace      │
        │               │─○──────────────┼│               │┼──────────┼│shape_name     │
        │               │╱traits          │               │            │member_name?   │
        └───────────────┘                 └───────────────┘            └───────────────┘

Shape
    Shapes are named data definitions that describe the structure of an API.
    Shapes are referenced and connected by :ref:`shape IDs <shape-id>`.
    Relationships between shapes are formed by :ref:`members <member>` that
    target other shapes, properties of shapes like the ``input`` and
    ``output`` properties of an :ref:`operation <operation>`, and
    :ref:`applied traits <applying-traits>` that attach a trait to a shape.
Shape ID
    A :ref:`shape ID <shape-id>` is used to identify shapes defined in a
    model. For example, ``smithy.example#MyShape``,
    ``smithy.example#Foo$bar``, and ``Baz`` are all different kinds of shape
    IDs.
Trait
    :ref:`Traits <traits>` are specialized shapes that form the basis of
    Smithy's meta-model. Traits are applied to shapes to associate metadata
    to a shape. They are typically used by tools to influence validation,
    serialization, and code generation.
Applied trait
    An applied trait is an instance of a trait applied to a shape, configured
    using a :ref:`node value <node-value>`.
Model metadata
    :ref:`Metadata <metadata>` is a schema-less extensibility mechanism used
    to associate metadata to an entire model.
Prelude
    The :ref:`prelude <prelude>` defines various simple shapes and every
    trait defined in the core specification. All Smithy models automatically
    include the prelude.


.. _shapes:

------
Shapes
------

Smithy models are made up of shapes. Shapes come in three kinds: simple,
aggregate, and service. A simple shape defines atomic or primitive values
such as ``integer`` and ``string``. Aggregate shapes have members such as
a list of strings or an ``Address`` structure. Service shapes have specific
semantics, unlike the very generic simple and aggregate shapes, as they
represent either a service, a resource managed by a service, or operations
on services and resources.

Shapes are visualized using the following diagram:

.. text-figure::
    :caption: **Figure 1.4**: Smithy shapes
    :name: figure-1.4

                                      ┌─────────────┐
                             members ╱│ «abstract»  │
                            ┌───────○─│    Shape    │
                            │        ╲│             │
                            │         └─────────────┘
                            │                △
                  ┌─────────│────────────────┼────────────────────┐
                  │         │                │                    │
          ┌───────────────┐ │         ┌─────────────┐      ┌─────────────┐
          │  «abstract»   │ │container│ «abstract»  │      │ «abstract»  │
          │    Simple     │ └────────┼│  Aggregate  │      │   Service   │
          └───────────────┘           └─────────────┘      └─────────────┘
                  △                    △                    △
    ┌──────────┐  │  ┌──────────┐      │    ┌────────────┐  │    ┌─────────────────────────┐
    │blob      │──┼──│boolean   │      ├────│    List    │  │    │         Service         │
    └──────────┘  │  └──────────┘      │    ├────────────┤  │    ├─────────────────────────┤
    ┌──────────┐  │  ┌──────────┐      │    │member      │  │    │version                  │
    │document  │──┼──│string    │      │    └────────────┘  ├────│operations: [Operation]? │
    └──────────┘  │  └──────────┘      │    ┌────────────┐  │    │resources: [Resource]?   │
    ┌──────────┐  │                    ├────│    Set     │  │    └─────────────────────────┘
    │timestamp │──┤                    │    ├────────────┤  │    ┌─────────────────────────┐
    └──────────┘  │                    │    │member      │  │    │        Operation        │
                  │                    │    └────────────┘  │    ├─────────────────────────┤
          ┌───────────────┐            │    ┌────────────┐  │    │input: Structure?        │
          │  «abstract»   │            ├────│    Map     │  ├────│output: Structure?       │
          │    Number     │            │    ├────────────┤  │    │errors: [Structure]?     │
          └───────────────┘            │    │key         │  │    └─────────────────────────┘
                  △                    │    │value       │  │    ┌─────────────────────────┐
    ┌──────────┐  │  ┌──────────┐      │    └────────────┘  │    │        Resource         │
    │byte      │──┼──│short     │      │    ┌────────────┐  │    ├─────────────────────────┤
    └──────────┘  │  └──────────┘      ├────│ Structure  │  │    │identifiers?             │
    ┌──────────┐  │  ┌──────────┐      │    └────────────┘  │    │create: Operation?       │
    │integer   │──┼──│long      │      │    ┌────────────┐  │    │put: Operation?          │
    └──────────┘  │  └──────────┘      └────│   Union    │  │    │read: Operation?         │
    ┌──────────┐  │  ┌──────────┐           └────────────┘  └────│update: Operation?       │
    │float     │──┼──│double    │                                │delete: Operation?       │
    └──────────┘  │  └──────────┘                                │list: : Operation?       │
    ┌──────────┐  │  ┌──────────┐                                │operations: [Operation]? │
    │bigInteger│──┴──│bigDecimal│                                │collectionOperations:    │
    └──────────┘     └──────────┘                                │    [Operation]?         │
                                                                 │resources: [Resource]?   │
                                                                 └─────────────────────────┘


.. _shape-id:

Shape ID
========

All shapes have an assigned shape ID. A :dfn:`shape ID` is used to refer to
shapes in the model. Shape IDs adhere to the following syntax:

.. code-block:: none

    com.foo.baz#ShapeName$memberName
    \_________/ \_______/ \________/
         |          |          |
     Namespace  Shape name  Member name

Namespace
    A namespace is a mechanism for logically grouping shapes in a way
    that makes them reusable alongside other models without naming
    conflicts. A semantic model MAY contain shapes defined across multiple
    namespaces. The IDL representation supports zero or one namespace per
    model file, while the JSON AST representation supports zero or more
    namespaces per model file.
Absolute shape ID
    An :dfn:`absolute shape ID` starts with a :token:`namespace` name,
    followed by "``#``", followed by a *relative shape ID*. All shape
    IDs in the semantic model MUST be absolute.
    For example, ``smithy.example#Foo`` and ``smithy.example#Foo$bar``
    are absolute shape IDs.
Relative shape ID
    A :dfn:`relative shape ID` contains a :token:`shape name <identifier>`
    and an optional :token:`member name <identifier>`. The shape name and
    member name are separated by the "``$``" symbol if a member name is
    present. For example, ``Foo`` and ``Foo$bar`` are relative shape IDs.
Root shape ID
    A :dfn:`root shape ID` is a shape ID that does not contain a member.
    For example, ``smithy.example#Foo`` and ``Foo`` are root shape IDs.

.. rubric:: Shape ID ABNF

Shape IDs are formally defined by the following ABNF:

.. productionlist:: smithy
    shape_id               :`root_shape_id` [`shape_id_member`]
    root_shape_id          :`absolute_root_shape_id` / `identifier`
    absolute_root_shape_id :`namespace` "#" `identifier`
    namespace              :`identifier` *("." `identifier`)
    identifier             :identifier_start *identifier_chars
    identifier_start       :*"_" ALPHA
    identifier_chars       :ALPHA / DIGIT / "_"
    shape_id_member        :"$" `identifier`

.. rubric:: Best practices for defining shape names

1. **Use a strict form of PascalCase for shape names.**
   Consumers of a Smithy model MAY choose to inflect shape names, structure
   member names, and other facets of a Smithy model in order to expose a more
   idiomatic experience to particular programming languages. In order to make
   this easier for consumers of a model, model authors SHOULD utilize a
   strict form of PascalCase in which only the first letter of acronyms,
   abbreviations, and initialisms are capitalized.

   ===========   ===============
   Recommended   Not recommended
   ===========   ===============
   UserId        UserID
   ResourceArn   ResourceARN
   IoChannel     IOChannel
   HtmlEntity    HTMLEntity
   HtmlEntity    HTML_Entity
   ===========   ===============

2. **Limit the number of namespaces used to model a single domain.**
   Ideally only a single namespace is used to model a single logical domain.
   Limiting the number of namespaces used to define a logical grouping of
   shapes limits the potential for ambiguity if the shapes are used by the
   same service or need to be referenced within the same model.


.. _shape-id-conflicts:

Shape ID conflicts
==================

While shape ID references within the semantic model are case-sensitive, no
two shapes in the semantic model can have the same case-insensitive shape ID.
This restriction makes it easier to use Smithy models for code generation in
programming languages that do not support case-sensitive identifiers or that
perform some kind of normalization on generated identifiers (for example,
a Python code generator might convert all member names to lower snake case).
To illustrate, ``com.Foo#baz`` and ``com.foo#BAZ`` are not allowed in the
same semantic model. This restriction also extends to member names:
``com.foo#Baz$bar`` and ``com.foo#Baz$BAR`` are in conflict.

.. seealso::

    :ref:`merging-models` for information on how conflicting shape
    definitions for the same shape ID are handled when assembling the
    semantic model from multiple model files.


.. _simple-types:

-------------
Simple shapes
-------------

*Simple types* are types that do not contain nested types or shape references.

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
        serialization of a timestamp is an implementation detail that is
        determined by a :ref:`protocol <protocolDefinition-trait>` and
        MUST NOT have any effect on the types exposed by tooling to
        represent a timestamp value.
    * - document
      - Represents protocol-agnostic open content that functions as a kind of
        "any" type. Document types are represented by a JSON-like data model
        and can contain UTF-8 strings, arbitrary precision numbers, booleans,
        nulls, a list of these values, and a map of UTF-8 strings to these
        values. Open content is useful for modeling unstructured data that has
        no schema, data that can't be modeled using rigid types, or data that
        has a schema that evolves outside of the purview of a model. The
        serialization format of a document is an implementation detail of a
        protocol and MUST NOT have any effect on the types exposed by tooling
        to represent a document value.

Simple shapes are defined in the IDL using a :ref:`simple_shape_statement <idl-simple>`.

.. note::

    The :ref:`prelude model <prelude>` contains pre-defined shapes for every
    simple type.

.. rubric:: Simple shape examples

The following example defines a shape for each simple type in the
``smithy.example`` namespace.

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

.. note::

    When defining shapes in the IDL, a namespace MUST first be declared.


.. _aggregate-types:

----------------
Aggregate shapes
----------------

Aggregate types define shapes that are composed of other shapes. Aggregate shapes
reference other shapes using :ref:`members <member>`.

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Type
      - Description
    * - :ref:`member`
      - Defined in aggregate shapes to reference other shapes
    * - :ref:`list`
      - Ordered collection of homogeneous values
    * - :ref:`set`
      - Collection of unique homogeneous values
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

:dfn:`Members` are defined in aggregate shapes to reference other shapes using
a :ref:`shape ID <shape-id>`. The shape referenced by a member is called its
"target". A member MUST NOT target a :ref:`trait <trait-shapes>`, ``operation``,
``resource``, ``service``, or ``member``.


.. _list:

List
====

The :dfn:`list` type represents an ordered homogeneous collection of values.
A list shape requires a single member named ``member``. Lists are defined
in the IDL using a :ref:`list_statement <idl-list>`.
The following example defines a list with a string member from the
:ref:`prelude <prelude>`:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        list MyList {
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyList": {
                    "type": "list",
                    "member": {
                        "target": "smithy.api#String"
                    }
                }
            }
        }

.. rubric:: List member nullability

Lists are considered *dense* by default, meaning they MAY NOT contain ``null``
values. A list MAY be made *sparse* by applying the :ref:`sparse-trait`.
The :ref:`box-trait` is not used to determine if a list is dense or sparse;
a list with no ``@sparse`` trait is always considered dense. The following
example defines a sparse list:

.. tabs::

    .. code-tab:: smithy

        @sparse
        list SparseList {
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#SparseList": {
                    "type": "list",
                    "member": {
                        "target": "smithy.api#String"
                    },
                    "traits": {
                        "smithy.api#sparse": {}
                    }
                }
            }
        }

If a client encounters a ``null`` value when deserializing a dense list
returned from a service, the client MUST discard the ``null`` value. If a
service receives a ``null`` value for a dense list from a client, it SHOULD
reject the request.

.. rubric:: List member shape ID

The shape ID of the member of a list is the list shape ID followed by
``$member``. For example, the shape ID of the list member in the above
example is ``smithy.example#MyList$member``.


.. _set:

Set
===

The :dfn:`set` type represents a collection of unique homogeneous
values. A set shape requires a single member named ``member``.
Sets are defined in the IDL using a :ref:`set_statement <idl-set>`.
The following example defines a set of strings:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        set StringSet {
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#StringSet": {
                    "type": "set",
                    "member": {
                        "target": "smithy.api#String"
                    }
                }
            }
        }

.. rubric:: Sets MUST NOT contain ``null`` values

The values contained in a set are not permitted to be ``null``. ``null`` set
values do not provide much, if any, utility, and set implementations across
programming languages often do not support ``null`` values.

If a client encounters a ``null`` value when deserializing a set returned
from a service, the client MUST discard the ``null`` value. If a service
receives a ``null`` value for a set from a client, it SHOULD reject the
request.

.. rubric:: Set member shape ID

The shape ID of the member of a set is the set shape ID followed by
``$member``. For example, the shape ID of the set member in the above
example is ``smithy.example#StringSet$member``.

.. rubric:: Language support for sets

Not all programming languages support set data structures. Such languages
SHOULD represent sets as a custom set data structure that can interpret value
hash codes and equality, or alternatively, store the values of a set data
structure in a list and rely on validation to ensure uniqueness.

.. rubric:: Set member ordering

Sets MUST be insertion ordered. Not all programming languages that support
sets support ordered sets, requiring them may be overly burdensome for users,
or conflict with language idioms. Such languages SHOULD store the values
of sets in a list and rely on validation to ensure uniqueness.


.. _map:

Map
===

The :dfn:`map` type represents a map data structure that maps ``string``
keys to homogeneous values. A map requires a member named ``key``
that MUST target a ``string`` shape and a member named ``value``.
Maps are defined in the IDL using a :ref:`map_statement <idl-map>`.
The following example defines a map of strings to integers:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        map IntegerMap {
            key: String
            value: Integer
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#IntegerMap": {
                    "type": "map",
                    "key": {
                        "target": "smithy.api#String"
                    },
                    "value": {
                        "target": "smithy.api#String"
                    }
                }
            }
        }

.. rubric:: Map keys MUST NOT be ``null``

Map keys are not permitted to be ``null``. Not all protocol serialization
formats have a way to define ``null`` map keys, and map implementations
across programming languages often do not allow ``null`` keys in maps.

.. rubric:: Map value member nullability

Maps values are considered *dense* by default, meaning they MAY NOT contain
``null`` values. A map MAY be made *sparse* by applying the
:ref:`sparse-trait`. The :ref:`box-trait` is not used to determine if a map
is dense or sparse; a map with no ``@sparse`` trait is always considered
dense. The following example defines a sparse map:

.. tabs::

    .. code-tab:: smithy

        @sparse
        map SparseMap {
            key: String
            value: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#SparseMap": {
                    "type": "map",
                    "key": {
                        "target": "smithy.api#String"
                    },
                    "value": {
                        "target": "smithy.api#String"
                    },
                    "traits": {
                        "smithy.api#sparse": {}
                    }
                }
            }
        }

If a client encounters a ``null`` map value when deserializing a dense map
returned from a service, the client MUST discard the ``null`` entry. If a
service receives a ``null`` map value for a dense map from a client, it
SHOULD reject the request.

.. rubric:: Map member shape IDs

The shape ID of the ``key`` member of a map is the map shape ID followed by
``$key``, and the shape ID of the ``value`` member is the map shape ID
followed by ``$value``. For example, the shape ID of the ``key`` member in
the above map is ``smithy.example#IntegerMap$key``, and the ``value``
member is ``smithy.example#IntegerMap$value``.


.. _structure:

Structure
=========

The :dfn:`structure` type represents a fixed set of named, unordered,
heterogeneous values. A structure shape contains a set of named members, and
each member name maps to exactly one :ref:`member <member>` definition.
Structures are defined in the IDL using a
:ref:`structure_statement <idl-structure>`.

The following example defines a structure with two members, one of which
is marked with the :ref:`required-trait` by suffixing the shape ID with
``!``.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        structure MyStructure {
            foo: String
            baz: Integer!
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
                            "target": "smithy.api#Integer",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                }
            }
        }

.. seealso::

    :ref:`idl-applying-traits` for a description of how to apply traits.

.. rubric:: Adding new members

New members added to existing structures SHOULD be added to the end of the
structure. This ensures that programming languages that require a specific
data structure layout or alignment for code generated from Smithy models are
able to maintain backward compatibility.

.. rubric:: Structure member shape IDs

The shape ID of a member of a structure is the structure shape ID, followed
by ``$``, followed by the member name. For example, the shape ID of the ``foo``
member in the above example is ``smithy.example#MyStructure$foo``.


.. _default-values:

Default structure member values
-------------------------------

The values provided for structure members are either always present and set to
a default value when necessary or *boxed*, meaning a value is optionally present
with no default value. Members are considered boxed if the member is marked with
the :ref:`box-trait` or the shape targeted by the member is marked with the box
trait. Members that target strings, timestamps, and aggregate shapes are always
considered boxed and have no default values.

- The default value of a ``byte``, ``short``, ``integer``, ``long``,
  ``float``, and ``double`` shape that is not boxed is zero.
- The default value of a ``boolean`` shape that is not boxed is ``false``.
- All other shapes are always considered boxed and have no default value.


.. _union:

Union
=====

The union type represents a `tagged union data structure`_ that can take
on several different, but fixed, types. Unions function similarly to
structures except that only one member can be used at any one time. A union
shape MUST contain one or more named :ref:`members <member>`. Unions are
defined in the IDL using a :ref:`union_statement <idl-union>`.

The following example defines a union shape with several members:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        union MyUnion {
            i32: Integer

            stringA: String

            @sensitive
            stringB: String
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

.. rubric:: Union member nullability

Exactly one member of a union MUST be set to a non-null value. In protocol
serialization formats that support ``null`` values (for example, JSON), if a
``null`` value is provided for a union member, it is discarded as if it was
not provided.

.. rubric:: Adding new members

New members added to existing unions SHOULD be added to the end of the
union. This ensures that programming languages that require a specific
data structure layout or alignment for code generated from Smithy models are
able to maintain backward compatibility.

.. rubric:: Union member shape IDs

The shape ID of a member of a union is the union shape ID, followed
by ``$``, followed by the member name. For example, the shape ID of the ``i32``
member in the above example is ``smithy.example#MyUnion$i32``.


Recursive shape definitions
===========================

Smithy allows for recursive shape definitions with the following constraint:
the member of a list, set, or map cannot directly or transitively target its
containing shape unless one or more members in the path from the container
back to itself targets a structure or union shape. This ensures that shapes
that are typically impossible to define in various programming languages are
not defined in Smithy models (for example, you can't define a recursive list
in Java ``List<List<List....``).

The following recursive shape definition is **valid**:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

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

The following recursive shape definition is **invalid**:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

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


.. _service-types:

--------------
Service shapes
--------------

*Service types* have specific semantics and define services, resources,
and operations.

.. list-table::
    :header-rows: 1
    :widths: 10 90

    * - Type
      - Description
    * - :ref:`service <service>`
      - Entry point of an API that aggregates resources and operations together
    * - :ref:`operation <operation>`
      - Represents the input, output, and errors of an API operation
    * - :ref:`resource <resource>`
      - Entity with an identity that has a set of operations

..  _service:

Service
=======

A :dfn:`service` is the entry point of an API that aggregates resources and
operations together. The :ref:`resources <resource>` and
:ref:`operations <operation>` of an API are bound within the closure of a
service. A service is defined in the IDL using a
:ref:`service_statement <idl-service>`.

The service shape supports the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - version
      - ``string``
      - Defines the optional version of the service. The version can be provided in
        any format (e.g., ``2017-02-11``, ``2.0``, etc).
    * - :ref:`operations <service-operations>`
      - [``string``]
      - Binds a set of ``operation`` shapes to the service. Each
        element in the given list MUST be a valid :ref:`shape ID <shape-id>`
        that targets an :ref:`operation <operation>` shape.
    * - :ref:`resources <service-resources>`
      - [``string``]
      - Binds a set of ``resource`` shapes to the service. Each element in
        the given list MUST be a valid :ref:`shape ID <shape-id>` that targets
        a :ref:`resource <resource>` shape.
    * - errors
      - [``string``]
      - Defines a list of common errors that every operation bound within the
        closure of the service can return. Each provided shape ID MUST target
        a :ref:`structure <structure>` shape that is marked with the
        :ref:`error-trait`.
    * - rename
      - map of :ref:`shape ID <shape-id>` to ``string``
      - Disambiguates shape name conflicts in the
        :ref:`service closure <service-closure>`. Map keys are shape IDs
        contained in the service, and map values are the disambiguated shape
        names to use in the context of the service. Each given shape ID MUST
        reference a shape contained in the closure of the service. Each given
        map value MUST match the :token:`identifier` production used for
        shape IDs. Renaming a shape *does not* give the shape a new shape ID.

        * No renamed shape name can case-insensitively match any other renamed
          shape name or the name of a non-renamed shape contained in the
          service.
        * Member shapes MAY NOT be renamed.
        * Resource, operation, and shapes marked with the :ref:`error-trait`
          MAY NOT be renamed. Renaming shapes is intended for incidental naming
          conflicts, not for renaming the fundamental concepts of a service.
        * Shapes from other namespaces marked as :ref:`private <private-trait>`
          MAY be renamed.
        * A rename MUST use a name that is case-sensitively different from the
          original shape ID name.

The following example defines a service with no operations or resources.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

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

The following example defines a service shape that defines a set of errors
that are common to every operation in the service:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        service MyService {
            version: "2017-02-11",
            errors: [SomeError]
        }

        @error("client")
        structure SomeError {}

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyService": {
                    "type": "service",
                    "version": "2017-02-11",
                    "errors": [
                        {
                            "target": "smithy.example#SomeError"
                        }
                    ]
                },
                "smithy.example#SomeError": {
                    "type": "structure",
                    "traits": {
                        "smithy.api#error": "client"
                    }
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

        namespace smithy.example

        service MyService {
            version: "2017-02-11"
            operations: [GetServerTime]
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

        namespace smithy.example

        service MyService {
            version: "2017-02-11"
            resources: [MyResource]
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
    their namespace, and conflicts MUST be disambiguated using the
    ``rename`` property of a service.

By requiring unique names within a service, each service forms a
`ubiquitous language`_, making it easier for developers to understand the
model and artifacts generated from the model, like code. For example, when
using Java code generated from a Smithy model, a developer should not need
to discern between ``BadRequestException`` classes across multiple packages
that can be thrown by an operation. Uniqueness is required
case-insensitively because many model transformations (like code generation)
change the casing and inflection of shape names to make artifacts more
idiomatic.

.. rubric:: Shape types allowed to conflict in a closure

:ref:`Simple types <simple-types>` and :ref:`lists <list>` or
:ref:`sets <set>` of compatible simple types are allowed to conflict because
a conflict for these type would rarely have an impact on generated artifacts.
These kinds of conflicts are only allowed if both conflicting shapes are the
same type and have the exact same traits. In the case of a list or set, a
conflict is only allowed if the members of the conflicting shapes target
compatible shapes.

.. rubric:: Disambiguating shapes with ``rename``

The ``rename`` property of a service is used to disambiguate conflicting
shape names found in the closure of a service. The ``rename`` property is
essentially a `context map`_ used to ensure that the service still presents
a ubiquitous language despite bringing together shapes from multiple
namespaces.

.. note::

    Renames SHOULD be used sparingly. Renaming shapes is something typically
    only needed when aggregating models from multiple independent teams into
    a single service.

The following example defines a service that contains two shapes named
"Widget" in its closure. The ``rename`` property is used to disambiguate
the conflicting shapes.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        service MyService {
            version: "2017-02-11"
            operations: [GetSomething]
            rename: {
                "foo.example#Widget": "FooWidget"
            }
        }

        operation GetSomething {
            output: GetSomethingOutput
        }

        structure GetSomethingOutput {
            widget1: Widget
            fooWidget: foo.example#Widget
        }

        structure Widget {}

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyService": {
                    "type": "service",
                    "version": "2017-02-11",
                    "operations": [
                        {
                            "target": "smithy.example#GetSomething"
                        }
                    ],
                    "rename": {
                        "foo.example#Widget": "FooWidget"
                    }
                },
                "smithy.example#GetSomething": {
                    "type": "operation",
                    "output": {
                        "target": "smithy.example#GetSomethingOutput"
                    }
                },
                "smithy.example#GetSomethingOutput": {
                    "type": "structure",
                    "members": {
                        "widget1": {
                            "target": "smithy.example#Widget"
                        },
                        "fooWidget": {
                            "target": "foo.example#Widget"
                        }
                    }
                },
                "smithy.example#Widget": {
                    "type": "structure"
                }
            }
        }

.. rubric:: Resources and operations can be bound once

An operation or resource MUST NOT be bound to multiple shapes within the
closure of a service. This constraint allows services to discern between
operations and resources using only their shape name rather than a
fully-qualified path from the service to the shape.


..  _operation:

Operation
=========

The :dfn:`operation` type represents the input, output, and possible errors of
an API operation. Operation shapes are bound to :ref:`resource <resource>`
shapes and :ref:`service <service>` shapes. An operation is defined in the IDL
using an :ref:`operation_statement <idl-operation>`.

An operation supports the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - input
      - ``string``
      - The optional input ``structure`` of the operation. The value MUST be
        a valid :ref:`shape ID <shape-id>` that targets a
        :ref:`structure <structure>` shape. The targeted shape MUST NOT be
        marked with the :ref:`error-trait`.
    * - output
      - ``string``
      - The optional output ``structure`` of the operation. The value MUST
        be a valid :ref:`shape ID <shape-id>` that targets a
        :ref:`structure <structure>` shape. The targeted shape MUST NOT
        be marked with the :ref:`error-trait`.
    * - errors
      - [``string``]
      - Defines the error ``structure``\s that an operation can return using
        a set of shape IDs that MUST target :ref:`structure <structure>`
        shapes that are marked with the :ref:`error-trait`.

The following example defines an operation shape that accepts an input
structure named ``Input``, returns an output structure named ``Output``, and
can potentially return the ``NotFound`` or ``BadRequest``
:ref:`error structures <error-trait>`.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        operation MyOperation {
            input: Input
            output: Output
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


..  _resource:

Resource
========

Smithy defines a :dfn:`resource` as an entity with an identity that has a
set of operations. A resource shape is defined in the IDL using a
:ref:`resource_statement <idl-resource>`.

A resource supports the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - :ref:`identifiers <resource-identifiers>`
      - ``object``
      - Defines a map of identifier string names to :ref:`shape-id`\s used to
        identify the resource. Each shape ID MUST target a ``string`` shape.
    * - :ref:`create <create-lifecycle>`
      - ``string``
      - Defines the lifecycle operation used to create a resource using one
        or more identifiers created by the service. The value MUST be a
        valid :ref:`shape-id` that targets an ``operation`` shape.
    * - :ref:`put <put-lifecycle>`
      - ``string``
      - Defines an idempotent lifecycle operation used to create a resource
        using identifiers provided by the client. The value MUST be a
        valid :ref:`shape-id` that targets an ``operation`` shape.
    * - :ref:`read <read-lifecycle>`
      - ``string``
      - Defines the lifecycle operation used to retrieve the resource. The
        value MUST be a valid :ref:`shape-id` that targets an
        ``operation`` shape.
    * - :ref:`update <update-lifecycle>`
      - ``string``
      - Defines the lifecycle operation used to update the resource. The
        value MUST be a valid :ref:`shape-id` that targets an
        ``operation`` shape.
    * - :ref:`delete <delete-lifecycle>`
      - ``string``
      - Defines the lifecycle operation used to delete the resource. The
        value MUST be a valid :ref:`shape-id` that targets an ``operation``
        shape.
    * - :ref:`list <list-lifecycle>`
      - ``string``
      - Defines the lifecycle operation used to list resources of this type.
        The value MUST be a valid :ref:`shape-id` that targets an
        ``operation`` shape.
    * - operations
      - [``string``]
      - Binds a list of non-lifecycle instance operations to the resource.
        Each value in the list MUST be a valid :ref:`shape-id` that targets
        an ``operation`` shape.
    * - collectionOperations
      - [``string``]
      - Binds a list of non-lifecycle collection operations to the resource.
        Each value in the list MUST be a valid :ref:`shape-id` that targets
        an ``operation`` shape.
    * - resources
      - [``string``]
      - Binds a list of resources to this resource as a child resource,
        forming a containment relationship. Each value in the list MUST be a
        valid :ref:`shape-id` that targets a ``resource``. The resources
        MUST NOT have a cyclical containment hierarchy, and a resource
        can not be bound more than once in the entire closure of a
        resource or service.


.. _resource-identifiers:

Resource Identifiers
====================

:dfn:`Identifiers` are used to refer to a specific resource within a service.
The identifiers property of a resource is a map of identifier names to
:ref:`shape IDs <shape-id>` that MUST target string shapes.

For example, the following model defines a ``Forecast`` resource with a
single identifier named ``forecastId`` that targets the ``ForecastId`` shape:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        resource Forecast {
            identifiers: { forecastId: ForecastId }
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
            }
            resources: [ResourceB]
        }

        resource ResourceB {
            identifiers: {
                a: String
                b: String
            }
            resources: [ResourceC]
        }

        resource ResourceC {
            identifiers: {
                a: String
                b: String
                c: String
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
                a: String
                b: String
            }
            resources: [Invalid1, Invalid2]
        }

        resource Invalid1 {
            // Invalid: missing "a".
            identifiers: {
                b: String
            }
        }

        resource Invalid2 {
            identifiers: {
                a: String
                // Invalid: does not target the same shape.
                b: SomeOtherString
            }
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

.. rubric:: Identifier binding validation

- Child resources MUST provide identifier bindings for all of its parent's
  identifiers.
- Identifier bindings are only formed on input structure members that are
  marked as :ref:`required <required-trait>`.
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
            identifiers: { forecastId: ForecastId }
            read: GetForecast
        }

        @readonly
        operation GetForecast {
            input: GetForecastInput
            output: GetForecastOutput
        }

        structure GetForecastInput {
            forecastId: ForecastId!
        }

        structure GetForecastOutput {
            weather: WeatherData!
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
            identifiers: { forecastId: ForecastId }
            collectionOperations: [BatchPutForecasts]
        }

        operation BatchPutForecasts {
            input: BatchPutForecastsInput
            output: BatchPutForecastsOutput
        }

        structure BatchPutForecastsInput {
            forecasts: BatchPutForecastList!
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
        resources: [HistoricalForecast]
    }

    resource HistoricalForecast {
        identifiers: {
            forecastId: ForecastId
            historicalId: HistoricalForecastId
        }
        read: GetHistoricalForecast
        list: ListHistoricalForecasts
    }

    @readonly
    operation GetHistoricalForecast {
        input: GetHistoricalForecastInput
        output: GetHistoricalForecastOutput
    }

    structure GetHistoricalForecastInput {
        @resourceIdentifier("forecastId")
        customForecastIdName: ForecastId!

        @resourceIdentifier("historicalId")
        customHistoricalIdName: String!
    }

the :ref:`resourceIdentifier-trait` on ``GetHistoricalForecastInput$customForecastIdName``
maps it to the "forecastId" identifier is provided by the
"customForecastIdName" member, and the :ref:`resourceIdentifier-trait`
on ``GetHistoricalForecastInput$customHistoricalIdName`` maps that member
to the "historicalId" identifier.


.. _lifecycle-operations:

Resource lifecycle operations
=============================

:dfn:`Lifecycle operations` are used to transition the state of a resource
using well-defined semantics. Lifecycle operations are defined by providing a
shape ID to the ``put``, ``create``, ``read``, ``update``, ``delete``, and
``list`` properties of a resource. Each shape ID MUST target an
:ref:`operation <operation>` that is compatible with the semantics of the
lifecycle.

The following example defines a resource with each lifecycle method:

.. code-block:: smithy

    namespace smithy.example

    resource Forecast {
        identifiers: { forecastId: ForecastId }
        put: PutForecast
        create: CreateForecast
        read: GetForecast
        update: UpdateForecast
        delete: DeleteForecast
        list: ListForecasts
    }


.. _put-lifecycle:

Put lifecycle
-------------

The ``put`` lifecycle operation is used to create a resource using identifiers
provided by the client.

- Put operations MUST NOT be marked with the :ref:`readonly-trait`.
- Put operations MUST be marked with the :ref:`idempotent-trait`.
- Put operations MUST form valid :ref:`instance operations <instance-operations>`.

The following example defines the ``PutForecast`` operation.

.. code-block:: smithy

    @idempotent
    operation PutForecast {
        input: PutForecastInput
        output: PutForecastOutput
    }

    structure PutForecastInput {
        // The client provides the resource identifier.
        forecastId: ForecastId!

        chanceOfRain: Float
    }

.. rubric:: Put semantics

The semantics of a ``put`` lifecycle operation are similar to the semantics
of a HTTP PUT method as described in :rfc:`section 4.3.4 of [RFC7231] <7231#section-4.3.4>`:

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

- Create operations MUST NOT be marked with the :ref:`readonly-trait`.
- Create operations MUST form valid :ref:`collection operations <collection-operations>`.
- The ``create`` operation MAY be marked with the :ref:`idempotent-trait`.

The following example defines the ``CreateForecast`` operation.

.. code-block:: smithy

    operation CreateForecast {
        input: CreateForecastInput
        output: CreateForecastOutput
    }

    operation CreateForecast {
        input: CreateForecastInput
        output: CreateForecastOutput
    }

    structure CreateForecastInput {
        // No identifier is provided by the client, so the service is
        // responsible for providing the identifier of the resource.
        chanceOfRain: Float
    }


.. _read-lifecycle:

Read lifecycle
--------------

The ``read`` operation is the canonical operation used to retrieve the current
representation of a resource.

- Read operations MUST be valid :ref:`instance operations <instance-operations>`.
- Read operations MUST be marked with the :ref:`readonly-trait`.

For example:

.. code-block:: smithy

    @readonly
    operation GetForecast {
        input: GetForecastInput
        output: GetForecastOutput
        errors: [ResourceNotFound]
    }

    structure GetForecastInput {
        forecastId: ForecastId!
    }


.. _update-lifecycle:

Update lifecycle
----------------

The ``update`` operation is the canonical operation used to update a
resource.

- Update operations MUST be valid :ref:`instance operations <instance-operations>`.
- Update operations MUST NOT be marked with the :ref:`readonly-trait`.

For example:

.. code-block:: smithy

    operation UpdateForecast {
        input: UpdateForecastInput
        output: UpdateForecastOutput
        errors: [ResourceNotFound]
    }

    structure UpdateForecastInput {
        forecastId: ForecastId!

        chanceOfRain: Float
    }


.. _delete-lifecycle:

Delete lifecycle
----------------

The ``delete`` operation is canonical operation used to delete a resource.

- Delete operations MUST be valid :ref:`instance operations <instance-operations>`.
- Delete operations MUST NOT be marked with the :ref:`readonly-trait`.
- Delete operations MUST be marked with the :ref:`idempotent-trait`.

For example:

.. code-block:: smithy

    @idempotent
    operation DeleteForecast {
        input: DeleteForecastInput
        output: DeleteForecastOutput
        errors: [ResourceNotFound]
    }

    structure DeleteForecastInput {
        forecastId: ForecastId!
    }


.. _list-lifecycle:

List lifecycle
--------------

The ``list`` operation is the canonical operation used to list a
collection of resources.

- List operations MUST form valid :ref:`collection operations <collection-operations>`.
- List operations MUST be marked with the :ref:`readonly-trait`.
- The output of a list operation SHOULD contain references to the resource
  being listed.
- List operations SHOULD be :ref:`paginated <paginated-trait>`.

For example:

.. code-block:: smithy

    @readonly @paginated
    operation ListForecasts {
        input: ListForecastsInput
        output: ListForecastsOutput
    }

    structure ListForecastsInput {
        maxResults: Integer
        nextToken: String
    }

    structure ListForecastsOutput {
        nextToken: String
        forecasts: ForecastList!
    }

    list ForecastList {
        member: ForecastId
    }


.. _traits:

------
Traits
------

*Traits* are model components that can be attached to :ref:`shapes <shapes>`
to describe additional information about the shape; shapes provide the
structure and layout of an API, while traits provide refinement and style.


.. _applying-traits:

Applying traits to shapes
=========================

An instance of a trait applied to a shape is called an *applied trait*. Only
a single instance of a trait can be applied to a shape. The way in which a
trait is applied to a shape depends on the model file representation.

Traits are applied to shapes in the IDL using :token:`trait_statements` that
immediately precede a shape. The following example applies the
:ref:`sensitive-trait` and :ref:`documentation-trait` to ``MyString``:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @sensitive
        @documentation("Contains a string")
        string MyString

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyString": {
                    "type": "string",
                    "traits": {
                        "smithy.api#documentation": "Contains a string",
                        "smithy.api#sensitive": {}
                    }
                }
            }
        }

* Refer to the :ref:`IDL specification <idl-applying-traits>` for a
  description of how traits are applied in the IDL.
* Refer to the :ref:`JSON AST specification <json-ast>` for a
  description of how traits are applied in the JSON AST.

.. rubric:: Scope of member traits

Traits that target :ref:`members <member>` apply only in the context of
the member shape and do not affect the shape targeted by the member. Traits
applied to a member supersede traits applied to the shape targeted by the
member and do not inherently conflict.


.. _apply-statements:

Applying traits externally
--------------------------

Both the IDL and JSON AST model representations allow traits to be applied
to shapes outside of a shape's definition. This is done using an
:token:`apply <apply_statement>` statement in the IDL, or the
:ref:`apply <ast-apply>` type in the JSON AST. For example, this can be
useful to allow different teams within the same organization to independently
own different facets of a model; a service team could own the model that
defines the shapes and traits of the API, and a documentation team could
own a model that applies documentation traits to the shapes.

The following example applies the :ref:`documentation-trait` and
:ref:`length-trait` to the ``smithy.example#MyString`` shape:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        apply MyString @documentation("This is my string!")
        apply MyString @length(min: 1, max: 10)

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyString": {
                    "type": "apply",
                    "traits": {
                        "smithy.api#documentation": "This is my string!",
                        "smithy.api#length": {
                            "min": 1,
                            "max": 10
                        }
                    }
                }
            }
        }

.. note::

    In the semantic model, applying traits outside of a shape definition is
    treated exactly the same as applying the trait inside of a shape
    definition.


.. _trait-conflict-resolution:

Trait conflict resolution
=========================

Trait conflict resolution is used when the same trait is applied multiple
times to a shape. Duplicate traits applied to shapes are allowed in the
following cases:

1. If the trait is a ``list`` or ``set`` shape, then the conflicting trait
   values are concatenated into a single trait value.
2. If both values are exactly equal, then the conflict is ignored.

All other instances of trait collisions are prohibited.

The following model definition is **valid** because the ``length`` trait is
duplicated on the ``MyList`` shape with the same values:

.. code-block:: smithy

    namespace smithy.example

    @length(min: 0, max: 10)
    list MyList {
        member: String
    }

    apply MyList @length(min: 0, max: 10)

The following model definition is **valid** because the ``tags`` trait
is a list. The resulting value assigned to the ``tags`` trait on the
``Hello`` shape is a list that contains "a", "b", and "c".

.. code-block:: smithy

    namespace smithy.example

    @tags(["a", "b"])
    string Hello

    apply Hello @tags(["c"])

The following model definition is **invalid** because the ``length`` trait is
duplicated on the ``MyList`` shape with different values:

.. code-block:: smithy

    namespace smithy.example

    @length(min: 0, max: 10)
    list MyList {
        member: String
    }

    apply MyList @length(min: 10, max: 20)


.. _trait-node-values:

Trait node values
=================

The value provided for a trait MUST be compatible with the ``shape`` of the
trait. The following table defines each shape type that is available to
target from traits and how their values are defined in
:token:`node <node_value>` values.

.. list-table::
    :header-rows: 1
    :widths: 20 20 60

    * - Smithy type
      - Node type
      - Description
    * - blob
      - string
      - A ``string`` value that is base64 encoded.
    * - boolean
      - boolean
      - Can be set to ``true`` or ``false``.
    * - byte
      - number
      - The value MUST fall within the range of -128 to 127
    * - short
      - number
      - The value MUST fall within the range of -32,768 to 32,767
    * - integer
      - number
      - The value MUST fall within the range of -2^31 to (2^31)-1.
    * - long
      - number
      - The value MUST fall within the range of -2^63 to (2^63)-1.
    * - float
      - string | number
      - The value MUST be either a normal JSON number or one of the following
        string values: ``"NaN"``, ``"Infinity"``, ``"-Infinity"``.
    * - double
      - string | number
      - The value MUST be either a normal JSON number or one of the following
        string values: ``"NaN"``, ``"Infinity"``, ``"-Infinity"``.
    * - bigDecimal
      - string | number
      - bigDecimal values can be serialized as strings to avoid rounding
        issues when parsing a Smithy model in various languages.
    * - bigInteger
      - string | number
      - bigInteger values can be serialized as strings to avoid truncation
        issues when parsing a Smithy model in various languages.
    * - string
      - string
      - The provided value SHOULD be compatible with the ``mediaType`` of the
        string shape if present; however, this is not validated by Smithy.
    * - timestamp
      - number | string
      - If a number is provided, it represents Unix epoch seconds with optional
        millisecond precision. If a string is provided, it MUST be a valid
        :rfc:`3339` string with no UTC offset and optional fractional
        precision (for example, ``1985-04-12T23:20:50.52Z``).
    * - list and set
      - array
      - Each value in the array MUST be compatible with the targeted member.
    * - map
      - object
      - Each key MUST be compatible with the ``key`` member of the map, and
        each value MUST be compatible with the ``value`` member of the map.
    * - structure
      - object
      - All members marked as required MUST be provided in a corresponding
        key-value pair. Each key MUST correspond to a single member name of
        the structure. Each value MUST be compatible with the member that
        corresponds to the member name.
    * - union
      - object
      - The object MUST contain a single single key-value pair. The key MUST be
        one of the member names of the union shape, and the value MUST be
        compatible with the corresponding shape.

.. rubric:: Constraint traits

Trait values MUST be compatible with any constraint traits found related to the
shape being validated.


.. _trait-shapes:

.. _defining-traits:

Defining traits
===============

Traits are defined inside of a namespace by applying ``smithy.api#trait`` to
a shape. This trait can only be applied to simple types, ``list``, ``map``,
``set``, ``structure``, and ``union`` shapes.

The following example defines a trait with a :ref:`shape ID <shape-id>` of
``smithy.example#myTraitName`` and applies it to ``smithy.example#MyString``:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @trait(selector: "*")
        structure myTraitName {}

        @myTraitName
        string MyString

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#myTraitName": {
                    "type": "structure",
                    "traits": {
                        "smithy.api#trait": {
                            "selector": "*"
                        }
                    }
                },
                "smithy.example#MyString": {
                    "type": "string",
                    "traits": {
                        "smithy.api#myTraitName": {}
                    }
                }
            }
        }

.. rubric:: Trait properties

``smithy.api#trait`` is a structure that supports the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - selector
      - ``string``
      - A valid :ref:`selector <selectors>` that defines where the trait
        can be applied. For example, a ``selector`` set to ``:test(list, map)``
        means that the trait can be applied to a :ref:`list <list>` or
        :ref:`map <map>` shape. This value defaults to ``*`` if not set,
        meaning the trait can be applied to any shape.
    * - conflicts
      - [``string``]
      - Defines the shape IDs of traits that MUST NOT be applied to the same
        shape as the trait being defined. This allows traits to be defined as
        mutually exclusive. Provided shape IDs MAY target unknown traits
        that are not defined in the model.
    * - structurallyExclusive
      - ``string``
      - One of "member" or "target". When set to "member", only a single
        member of a structure can be marked with the trait. When set to
        "target", only a single member of a structure can target a shape
        marked with this trait.

The following example defines two custom traits: ``beta`` and
``structuredTrait``:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        /// A trait that can be applied to a member.
        @trait(selector: "structure > member")
        structure beta {}

        /// A trait that has members.
        @trait(selector: "string", conflicts: [beta])
        structure structuredTrait {
            lorem: StringShape!
            ipsum: StringShape!
            dolor: StringShape
        }

        // Apply the "beta" trait to the "foo" member.
        structure MyShape {
            @beta
            foo: StringShape!
        }

        // Apply the structuredTrait to the string.
        @structuredTrait(
            lorem: "This is a custom trait!"
            ipsum: "lorem and ipsum are both required values.")
        string StringShape

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#beta": {
                    "type": "apply",
                    "traits": {
                        "smithy.api#type": "structure",
                        "smithy.api#trait": {
                            "selector": "structure > member"
                        },
                        "smithy.api#documentation": "A trait that can be applied to a member."
                    }
                },
                "smithy.example#structuredTrait": {
                    "type": "apply",
                    "traits": {
                        "smithy.api#type": "structure",
                        "smithy.api#trait": {
                            "selector": "string",
                            "conflicts": [
                                "smithy.example#beta"
                            ]
                        },
                        "smithy.api#members": {
                            "lorem": {
                                "target": "StringShape",
                                "required": true
                            },
                            "dolor": {
                                "target": "StringShape"
                            }
                        },
                        "smithy.api#documentation": "A trait that has members."
                    }
                },
                "smithy.example#MyShape": {
                    "type": "apply",
                    "traits": {
                        "smithy.api#type": "structure",
                        "smithy.api#members": {
                            "beta": {
                                "target": "StringShape",
                                "required": true,
                                "beta": true
                            }
                        }
                    }
                },
                "smithy.example#StringShape": {
                    "type": "apply",
                    "traits": {
                        "smithy.api#type": "string",
                        "smithy.api#structuredTrait": {
                            "lorem": "This is a custom trait!",
                            "ipsum": "lorem and ipsum are both required values."
                        }
                    }
                }
            }
        }

.. rubric:: Prelude traits

When using the IDL, built-in traits defined in the Smithy
:ref:`prelude <prelude>` namespace, ``smithy.api``, are automatically
available in every Smithy model and namespace through relative shape IDs.

.. rubric:: References to traits

The only valid reference to a trait is through applying a trait to a
shape. Members and references within a model MUST NOT target shapes.

.. rubric:: Naming traits

By convention, trait shape names SHOULD use a lowercase name so that they
visually stand out from normal shapes.


.. _annotation-trait:

Annotation traits
-----------------

A structure trait with no members is called an *annotation trait*. It's hard
to predict what information a trait needs to capture when modeling a domain;
a trait might start out as a simple annotation, but later might benefit
from additional information. By defining an annotation trait rather than a
boolean trait, the trait can safely add optional members over time as needed.

The following example defines an annotation trait named ``foo``:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @trait
        structure foo {}

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#foo": {
                    "type": "structure",
                    "traits": {
                        "smithy.api#trait": {}
                    }
                }
            }
        }

A member can be safely added to an annotation trait if the member is not
marked as :ref:`required <required-trait>`. The applications of the ``foo``
trait in the previous example and the following example are all valid even
after adding a member to the ``foo`` trait:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @trait
        structure foo {
            baz: String
        }

        @foo(baz: "bar")
        string MyString4

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#foo": {
                    "type": "structure",
                    "members": {
                        "baz": {
                            "target": "smithy.api#String"
                        }
                    },
                    "traits": {
                        "smithy.api#trait": {}
                    }
                },
                "smithy.example#MyString4": {
                    "type": "string",
                    "traits": {
                        "smithy.api#foo": {
                            "baz": "bar"
                        }
                    }
                }
            }
        }


.. _metadata:

--------------
Model metadata
--------------

Metadata is a schema-less extensibility mechanism used to associate
metadata to an entire model. For example, metadata is used to define
:ref:`validators <validator-definition>` and model-wide
:ref:`suppressions <suppression-definition>`. Metadata is defined
using an ``object`` :ref:`node value <node-value>`.


.. _merging-metadata:

Merging metadata
================

When a conflict occurs between top-level metadata key-value pairs,
metadata is merged using the following logic:

1. If a metadata key is only present in one model, then the entry is valid
   and added to the merged model.
2. If both models contain the same key and both values are arrays, then
   the entry is valid; the values of both arrays are concatenated into a
   single array and added to the merged model.
3. If both models contain the same key and both values are exactly equal,
   then the conflict is ignored and the value is added to the merged model.
4. If both models contain the same key, the values do not both map to
   arrays, and the values are not equal, then the key is invalid and there
   is a metadata conflict error.

Given the following two Smithy models:

.. code-block:: smithy
    :caption: model-a.smithy

    metadata "foo" = ["baz", "bar"]
    metadata "qux" = "test"
    metadata "validConflict" = "hi!"

.. code-block:: smithy
    :caption: model-b.smithy

    metadata "foo" = ["lorem", "ipsum"]
    metadata "lorem" = "ipsum"
    metadata "validConflict" = "hi!"

Merging ``model-a.smithy`` and ``model-b.smithy`` produces the following
model:

.. code-block:: smithy

    metadata "foo" = ["baz", "bar", "lorem", "ipsum"]
    metadata "qux" = "test"
    metadata "lorem" = "ipsum"
    metadata "validConflict" = "hi!"


.. _node-value:

-----------
Node values
-----------

Node values are JSON-like values used in the following places in the
semantic model:

* **metadata**: Metadata is defined as a node value object.
* **applied trait**: The value of a trait applied to a shape is defined
  using a node value.

.. text-figure::
    :caption: **Figure 1.3**: Node value types
    :name: figure-1.3

    ┌─────────────────┐                     ┌─────────────┐
    │ Semantic Model  │                     │Applied Trait│
    └─────────────────┘                     └─────────────┘
      │                                            │
      │                                            │
      │                                            ┼ nodeValue
      │                                     ┌─────────────┐
      │                                     │ «abstract»  │
      │                                     │    Value    │
      │metadata                             └─────────────┘
      │                                            △
      ○      ┌───────────────────┬─────────────────┼───────────────┬───────────────┐
      ┼      │                   │                 │               │               │
    ┌─────────────────┐ ┌─────────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
    │     Object      │ │      Array      │ │   Number    │ │   Boolean   │ │   String    │
    ├─────────────────┤ ├─────────────────┤ └─────────────┘ └─────────────┘ └─────────────┘
    │members:         │ │members: [Value] │
    │  [String, Value]│ └─────────────────┘
    └─────────────────┘

The following example defines metadata using a node value:

.. tabs::

    .. code-tab:: smithy

        metadata foo = "hello"

    .. code-tab:: json

        {
            "smithy": "1.0",
            "metadata": {
                "foo": "hello"
            }
        }

The following example defines a trait using a node value:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @length(min: 1, max: 10)
        string MyString

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#MyString": {
                    "type": "string",
                    "traits": {
                        "smithy.api#length": {
                            "min": 1,
                            "max": 10
                        }
                    }
                }
            }
        }


Node value types
================

Node values have the same data model as JSON; they consist of the following
kinds of values:

.. list-table::
    :header-rows: 1
    :widths: 30 70

    * - Type
      - Description
    * - null
      - The lack of a value
    * - string
      - A UTF-8 string
    * - number
      - A double precision floating point number
    * - boolean
      - A Boolean, true or false value
    * - array
      - A list of heterogeneous node values
    * - object
      - A map of string keys to heterogeneous node values

.. rubric:: Shape IDs, text blocks, et al.

There is no specific node value type for shape IDs, text blocks, or
other higher-level features of the IDL; these values are stored and
treated in the semantic model as simply opaque strings, and their
validation happens before the creation of the model.


.. _merging-models:

-------------------
Merging model files
-------------------

Implementations MUST take the following steps when merging two or more
:ref:`model files <model-files>` to form a
:ref:`semantic model <semantic-model>`:

#. Merge the metadata objects of all model files using the steps defined in
   :ref:`merging-metadata`.
#. Shapes defined in a single model file are added to the semantic model as-is.
#. Shapes with the same shape ID defined in multiple model files are
   reconciled using the following rules:

   #. All conflicting shapes MUST have the same shape type.
   #. Conflicting :ref:`aggregate shapes <aggregate-types>` MUST contain the
      same members that target the same shapes.
   #. Conflicting :ref:`service shapes <service-types>` MUST contain the same
      properties and target the same shapes.
#. Conflicting traits defined in shape definitions or through
   :ref:`apply statements <apply-statements>` are reconciled using
   :ref:`trait conflict resolution <trait-conflict-resolution>`.

.. note::

    *The following guidance is non-normative.* Because the Smithy IDL allows
    forward references to shapes that have not yet been defined or shapes
    that are defined in another model file, implementations likely need to
    defer :ref:`resolving relative shape IDs <relative-shape-id>` to
    absolute shape IDs until *all* model files are loaded.


.. _tagged union data structure: https://en.wikipedia.org/wiki/Tagged_union
.. _ubiquitous language: https://martinfowler.com/bliki/UbiquitousLanguage.html
.. _context map: https://martinfowler.com/bliki/BoundedContext.html
