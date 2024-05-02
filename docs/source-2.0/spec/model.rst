.. _smithy-model:

================
The Smithy model
================

The *Smithy model* describes the Smithy semantic model and the files used to
create it. Smithy models are used to describe services and data structures.


.. _smithy-overview:

---------------
Smithy overview
---------------

Smithy is a framework that consists of a semantic model, file formats used to
define a model, and a build process used to validate models and facilitate
model transformations.

.. code-block:: none
    :caption: **Figure 1.1**: Smithy framework concepts
    :name: figure-1.1
    :class: no-copybutton

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

    * :ref:`Smithy IDL <idl>`: a human-readable format that aims to
      streamline authoring and reading models.
    * :ref:`JSON AST <json-ast>`: a machine-readable JSON-based format.


.. _semantic-model:

------------------
The semantic model
------------------

Smithy's *semantic model* is an in-memory model used by tools. It is
independent of any particular serialized representation. The semantic
model contains metadata and a graph of shapes connected by shape IDs.

.. code-block:: none
    :caption: **Figure 1.2**: The semantic model
    :name: figure-1.2
    :class: no-copybutton

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
    :ref:`Shapes <shapes>` are named data definitions that describe the
    structure of an API. Shapes are referenced and connected by shape IDs.
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


.. _metadata:

--------------
Model metadata
--------------

Metadata is a schema-less extensibility mechanism used to associate
metadata to an entire model. For example, metadata is used to define
:ref:`validators <validator-definition>` and model-wide
:ref:`suppressions <suppression-definition>`. Metadata is defined
using a :ref:`node value <node-value>`. The following example configures
a model validator:

.. code-block:: smithy

    $version: "2"
    metadata validators = [
        {
            name: "EmitEachSelector"
            id: "OperationInputName"
            message: "This shape is referenced as input but the name does not end with 'Input'"
            configuration: {
                selector: "operation -[input]-> :not([id|name$=Input i])"
            }
        }
    ]


.. _merging-metadata:

Metadata conflicts
==================

When a conflict occurs between top-level metadata key-value pairs,
the following conflict resolution logic is used:

1. If both values are arrays, the values of both arrays are concatenated
   into a single array.
2. Otherwise, if both values are exactly equal, the conflict is ignored.
3. Otherwise, the conflict is invalid.

Given the following two Smithy models:

.. code-block:: smithy
    :caption: model-a.smithy

    $version: "2"
    metadata "foo" = ["baz", "bar"]
    metadata "qux" = "test"
    metadata "validConflict" = "hi!"

.. code-block:: smithy
    :caption: model-b.smithy

    $version: "2"
    metadata "foo" = ["lorem", "ipsum"]
    metadata "lorem" = "ipsum"
    metadata "validConflict" = "hi!"

Merging ``model-a.smithy`` and ``model-b.smithy`` produces the following
model:

.. code-block:: smithy

    $version: "2"
    metadata "foo" = ["baz", "bar", "lorem", "ipsum"]
    metadata "qux" = "test"
    metadata "lorem" = "ipsum"
    metadata "validConflict" = "hi!"


.. _node-value:

-----------
Node values
-----------

Node values are JSON-like values used to define metadata and the value of
an applied trait.

.. code-block:: none
    :caption: **Figure 1.3**: Node value types
    :name: figure-1.3
    :class: no-copybutton

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

The following example defines :ref:`metadata <metadata>` using a node value:

.. code-block:: smithy

    metadata foo = "hello"

The following example defines a :ref:`trait <traits>` using a node value:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @length(min: 1, max: 10)
    string MyString

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


.. _merging-models:

-------------------
Merging model files
-------------------

Multiple :ref:`model files <model-files>` can be used to create a
:ref:`semantic model <semantic-model>`. Implementations MUST
take the following steps when merging two or more model files:

1. Merge the metadata objects of all model files. If top-level metadata
   key-value pairs conflict, :ref:`merge the metadata <merging-metadata>`
   if possible or fail.
2. Shapes defined in a single model file are added to the semantic model as-is.
3. Shapes with the same shape ID defined in multiple model files are
   reconciled using the following rules:

   #. All conflicting shapes MUST have the same shape type.
   #. Conflicting :ref:`aggregate shape types <aggregate-types>` MUST contain
      the same members that target the same shapes.
   #. Conflicting :ref:`service shape types <service-types>` MUST contain the
      same properties and target the same shapes.
   #. The traits from each shape are treated as if they are defined using
      an :ref:`apply statement <apply-statements>`: non-conflicting traits are
      added to the merged shape, and conflicting traits are resolved through
      step (4).
4. Conflicting traits defined in shape definitions or through apply statements
   are reconciled using :ref:`trait conflict resolution <trait-conflict-resolution>`.


.. _shapes:

------
Shapes
------

Smithy models are made up of shapes. Shapes are named definitions of types.

Shapes are visualized using the following diagram:

.. code-block:: none
    :caption: **Figure 1.4**: Smithy shapes
    :name: figure-1.4
    :class: no-copybutton

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
    └──────────┘  │  └──────────┘      │                    │    │resources: [Resource]?   │
    ┌──────────┐  │       △            │    ┌────────────┐  │    └─────────────────────────┘
    │timestamp │──┤       │            ├────│    Map     │  │    ┌─────────────────────────┐
    └──────────┘  │  ┌──────────┐      │    ├────────────┤  │    │        Operation        │
                  │  │enum      │      │    │key         │  │    ├─────────────────────────┤
                  │  └──────────┘      │    │value       │  │    │input: Structure         │
          ┌───────────────┐            │    └────────────┘  ├────│output: Structure        │
          │  «abstract»   │            │                    |    │errors: [Structure]?     │
          │    Number     │            │    ┌────────────┐  │    └─────────────────────────┘
          └───────────────┘            ├────│ Structure  │  │    ┌─────────────────────────┐
                  △                    │    └────────────┘  │    │        Resource         │
    ┌──────────┐  │  ┌──────────┐      │    ┌────────────┐  │    ├─────────────────────────┤
    │float     │──┼──│double    │      └────│   Union    │  │    │identifiers?             │
    └──────────┘  │  └──────────┘           └────────────┘  │    │create: Operation?       │
    ┌──────────┐  │  ┌──────────┐                           │    │put: Operation?          │
    │bigInteger│──┼──│bigDecimal│                           │    │read: Operation?         │
    └──────────┘  │  └──────────┘                           └────│update: Operation?       │
    ┌──────────┐  │  ┌──────────┐                                │delete: Operation?       │
    │byte      │──┼──│short     │                                │list: : Operation?       │
    └──────────┘  │  └──────────┘                                │operations: [Operation]? │
    ┌──────────┐  │  ┌──────────┐                                │collectionOperations:    │
    │integer   │──┴──│long      │                                │    [Operation]?         │
    └──────────┘     └──────────┘                                │resources: [Resource]?   │
         △                                                       └─────────────────────────┘
         │
    ┌──────────┐
    │intEnum   │
    └──────────┘


Shape types
===========

Shape types are grouped into three categories:

:ref:`Simple types <simple-types>`
    Simple types are types that do not contain nested types or shape references.

    .. list-table::
        :header-rows: 1
        :widths: 10 90

        * - Type
          - Description
        * - :ref:`blob`
          - Uninterpreted binary data
        * - :ref:`boolean`
          - Boolean value type
        * - :ref:`string`
          - UTF-8 encoded string
        * - :ref:`enum`
          - A string with a fixed set of values.
        * - :ref:`byte`
          - 8-bit signed integer ranging from -128 to 127 (inclusive)
        * - :ref:`short`
          - 16-bit signed integer ranging from -32,768 to 32,767 (inclusive)
        * - :ref:`integer`
          - 32-bit signed integer ranging from -2^31 to (2^31)-1 (inclusive)
        * - :ref:`intEnum`
          - An integer with a fixed set of values.
        * - :ref:`long`
          - 64-bit signed integer ranging from -2^63 to (2^63)-1 (inclusive)
        * - :ref:`float`
          - Single precision IEEE-754 floating point number
        * - :ref:`double`
          - Double precision IEEE-754 floating point number
        * - :ref:`bigInteger`
          - Arbitrarily large signed integer
        * - :ref:`bigDecimal`
          - Arbitrary precision signed decimal number
        * - :ref:`timestamp`
          - An instant in time with no UTC offset or timezone.
        * - :ref:`document`
          - Open content that functions as a kind of "any" type.
:ref:`Aggregate types <aggregate-types>`
    Aggregate types contain configurable member references to others shapes.

    .. list-table::
        :header-rows: 1
        :widths: 10 90

        * - Type
          - Description
        * - :ref:`list <list>`
          - Ordered collection of homogeneous values
        * - :ref:`map <map>`
          - Map data structure that maps string keys to homogeneous values
        * - :ref:`structure <structure>`
          - Fixed set of named heterogeneous members
        * - :ref:`union <union>`
          - Tagged union data structure that can take on one of several
            different, but fixed, types
:ref:`Service types <service-types>`
    Types that define the organization and operations of a service.

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


.. _member:

Member shapes
=============

:dfn:`Members` are defined in shapes to reference other shapes using
a :ref:`shape ID <shape-id>`. Members are found in :ref:`enum <enum>`,
:ref:`intEnum <intEnum>`, :ref:`list <list>`, :ref:`map <map>`,
:ref:`structure <structure>`, and :ref:`union <union>` shapes. The shape
referenced by a member is called its "target". A member MUST NOT target a
:ref:`trait <trait-shapes>`, :ref:`operation <operation>`,
:ref:`resource <resource>`, :ref:`service <service>`, or ``member``.

The following example defines a :ref:`list <list>`
that contains a member shape. Further examples can be found in the
documentation for :ref:`shape types <shapes>`.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    list UserNameList {
        member: UserName
    }


.. _shape-id:

Shape ID
========

A :dfn:`shape ID` is used to refer to shapes in the model. All shapes have an
assigned shape ID.

The following example defines a shape in the ``smithy.example`` namespace named
``MyString``, giving the shape a shape ID of ``smithy.example#MyString``:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    string MyString

Shape IDs have the following syntax:

.. code-block:: none
    :class: no-copybutton

    smithy.example.foo#ExampleShapeName$memberName
    └─────────┬──────┘ └───────┬──────┘ └────┬───┘
         (Namespace)     (Shape name)  (Member name)
                       └──────────────┬────────────┘
                             (Relative shape ID)
    └──────────────────────┬───────────────────────┘
                  (Absolute shape ID)


Absolute shape ID
    An :dfn:`absolute shape ID` starts with a :token:`namespace <smithy:Namespace>`,
    followed by "``#``", followed by a *relative shape ID*. For example,
    ``smithy.example#Foo`` and ``smithy.example#Foo$bar`` are absolute shape IDs.
Relative shape ID
    A :dfn:`relative shape ID` contains a :token:`shape name <smithy:Identifier>`
    and an optional :token:`member name <smithy:Identifier>`. The shape name and
    member name are separated by the "``$``" symbol if a member name is
    present. For example, ``Foo`` and ``Foo$bar`` are relative shape IDs.
Namespace
    A namespace is a mechanism for logically grouping shapes in a way
    that makes them reusable alongside other models without naming
    conflicts. A semantic model MAY contain shapes defined across multiple
    namespaces. The IDL representation supports zero or one namespace per
    model file, while the JSON AST representation supports zero or more
    namespaces per model file.

    Models SHOULD use a single namespace to model a single logical domain.
    Limiting the number of namespaces used to define a logical grouping of
    shapes limits the potential for ambiguity if the shapes are used by the
    same service or need to be referenced within the same model.
Shape name
    The name of the shape within a namespace.

    Consumers of a Smithy model MAY choose to inflect shape names, structure
    member names, and other facets of a Smithy model in order to expose a more
    idiomatic experience to particular programming languages. In order to make
    this easier for consumers of a model, model authors SHOULD utilize a strict
    form of PascalCase in which only the first letter of acronyms,
    abbreviations, and initialisms are capitalized. For example, prefer
    ``UserId`` over ``UserID``, and ``Arn`` over ``ARN``.
Root shape ID
    A :dfn:`root shape ID` is a shape ID that does not contain a member.
    For example, ``smithy.example#Foo`` and ``Foo`` are root shape IDs.


Shape ID ABNF
-------------

Shape IDs are formally defined by the following ABNF:

.. productionlist:: smithy
    ShapeId              :`RootShapeId` [`ShapeIdMember`]
    RootShapeId          :`AbsoluteRootShapeId` / `Identifier`
    AbsoluteRootShapeId  :`Namespace` "#" `Identifier`
    Namespace            :`Identifier` *("." `Identifier`)
    Identifier           :`IdentifierStart` *`IdentifierChars`
    IdentifierStart      :(1*"_" (ALPHA / DIGIT)) / ALPHA
    IdentifierChars      :ALPHA / DIGIT / "_"
    ShapeIdMember        :"$" `Identifier`


.. _shape-id-conflicts:

Shape ID conflicts
------------------

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


.. _non-aws-traits:
.. _traits:

------
Traits
------

*Traits* are model components that can be attached to :ref:`shapes <shapes>`
to describe additional information about the shape; shapes provide the
structure and layout of an API, while traits provide refinement and style.


.. _applying-traits:

Applying traits
===============

An instance of a trait applied to a shape is called an *applied trait*. Only
a single instance of a trait can be applied to a shape. The way in which a
trait is applied to a shape depends on the model file representation.

Traits are applied to shapes in the IDL using :token:`smithy:TraitStatements` that
immediately precede a shape. The following example applies the
:ref:`length-trait` and :ref:`documentation-trait` to ``MyString``:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @length(min: 1, max: 100)
    @documentation("Contains a string")
    string MyString

* Refer to the :ref:`IDL specification <idl-applying-traits>` for a
  description of how traits are applied in the IDL.
* Refer to the :ref:`JSON AST specification <json-ast>` for a
  description of how traits are applied in the JSON AST.

Scope of member traits
----------------------

Traits that target :ref:`members <member>` apply only in the context of
the member shape and do not affect the shape targeted by the member. Traits
applied to a member supersede traits applied to the shape targeted by the
member and do not inherently conflict.

In the following example, the :ref:`range-trait` applied to ``numberOfItems``
takes precedence over the trait applied to ``PositiveInteger``.

.. code-block:: smithy

    structure ShoppingCart {
        // This trait supersedes the PositiveInteger trait.
        @range(min: 7, max:12)
        numberOfItems: PositiveInteger
    }

    @range(min: 1)
    integer PositiveInteger


.. _apply-statements:

Applying traits externally
--------------------------

Both the IDL and JSON AST model representations allow traits to be applied
to shapes outside of a shape's definition. This is done using an
:token:`apply <smithy:ApplyStatement>` statement in the IDL, or the
:ref:`apply <ast-apply>` type in the JSON AST. For example, this can be
useful to allow different teams within the same organization to independently
own different facets of a model; a service team could own the model that
defines the shapes and traits of the API, and a documentation team could
own a model that applies documentation traits to the shapes.

The following example applies the :ref:`documentation-trait` and
:ref:`length-trait` to the ``smithy.example#MyString`` shape:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    apply MyString @documentation("This is my string!")
    apply MyString @length(min: 1, max: 10)

.. note::

    In the semantic model, applying traits outside of a shape definition is
    treated exactly the same as applying the trait inside of a shape
    definition.


.. _trait-conflict-resolution:

Trait conflict resolution
-------------------------

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

    $version: "2"
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

    $version: "2"
    namespace smithy.example

    @tags(["a", "b"])
    string Hello

    apply Hello @tags(["c"])

The following model definition is **invalid** because the ``length`` trait is
duplicated on the ``MyList`` shape with different values:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @length(min: 0, max: 10)
    list MyList {
        member: String
    }

    apply MyList @length(min: 10, max: 20)


.. _trait-node-values:

Trait node values
-----------------

The value provided for a trait MUST be compatible with the ``shape`` of the
trait. The following table defines each shape type that is available to
target from traits and how their values are defined in
:token:`node <smithy:NodeValue>` values.

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
        :rfc:`3339` string with optional fractional precision but no UTC offset 
        (for example, ``1985-04-12T23:20:50.52Z``).
    * - list
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
      - The object MUST contain a single key-value pair. The key MUST be
        one of the member names of the union shape, and the value MUST be
        compatible with the corresponding shape.

.. important::

    Trait values MUST be compatible with the :ref:`required-trait` and any
    associated :doc:`constraint traits <constraint-traits>`.


.. _trait-shapes:
.. _defining-traits:

Defining traits
===============

Traits are defined by applying :ref:`smithy.api#trait <trait-trait>` to a shape.
This trait can only be applied to simple types and aggregate types.
By convention, trait shape names SHOULD use a lowercase name so that they
visually stand out from normal shapes.

The following example defines a trait with a :ref:`shape ID <shape-id>` of
``smithy.example#myTraitName`` and applies it to ``smithy.example#MyString``:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @trait(selector: "*")
    structure myTraitName {}

    @myTraitName
    string MyString

The following example defines two custom traits: ``beta`` and
``structuredTrait``:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    /// A trait that can be applied to a member.
    @trait(selector: "structure > member")
    structure beta {}

    /// A trait that has members.
    @trait(selector: "string", conflicts: [beta])
    structure structuredTrait {
        @required
        lorem: StringShape

        @required
        ipsum: StringShape

        dolor: StringShape
    }

    // Apply the "beta" trait to the "foo" member.
    structure MyShape {
        @required
        @beta
        foo: StringShape
    }

    // Apply the structuredTrait to the string.
    @structuredTrait(
        lorem: "This is a custom trait!"
        ipsum: "lorem and ipsum are both required values.")
    string StringShape

Prelude traits
--------------

When using the IDL, built-in traits defined in the Smithy
:ref:`prelude <prelude>` namespace, ``smithy.api``, are automatically
available in every Smithy model and namespace through relative shape IDs.

References to traits
--------------------

The only valid reference to a trait is through applying a trait to a
shape. Members and references within a model MUST NOT target shapes.


.. smithy-trait:: smithy.api#trait

.. _trait-trait:

``trait`` trait
---------------

Summary
    Marks a shape as a :ref:`trait <traits>`.
Trait selector
    ``:is(simpleType, list, map, set, structure, union)``

    This trait can only be applied to simple types, ``list``, ``map``, ``set``,
    ``structure``, and ``union`` shapes.
Value type
    ``structure``

Trait properties
^^^^^^^^^^^^^^^^

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
    * - breakingChanges
      - [:ref:`BreakingChangeRule <trait-breaking-change-rules>`]
      - Defines the backward compatibility rules of the trait.


.. _annotation-trait:

Annotation traits
-----------------

A structure trait with no members is called an *annotation trait*. It's hard
to predict what information a trait needs to capture when modeling a domain;
a trait might start out as a simple annotation, but later might benefit
from additional information. By defining an annotation trait rather than a
boolean trait, the trait can safely add optional members over time as needed.

The following example defines an annotation trait named ``foo``:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @trait
    structure foo {}

A member can be safely added to an annotation trait if the member is not
marked as :ref:`required <required-trait>`. The applications of the ``foo``
trait in the previous example and the following example are all valid even
after adding a member to the ``foo`` trait:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @trait
    structure foo {
        baz: String
    }

    @foo(baz: "bar")
    string MyString4


.. _trait-breaking-change-rules:

Breaking change rules
---------------------

Backward compatibility rules of a trait can be defined in the ``breakingChanges``
member of a trait definition. This member is a list of diff rules. Smithy
tooling that performs semantic diff analysis between two versions of the same
model can use these rules to detect breaking or risky changes.

.. note::

    Not every kind of breaking change can be described using the
    ``breakingChanges`` property. Such backward compatibility rules SHOULD
    instead be described through documentation and ideally enforced through
    custom diff tooling.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - change
      - ``string``
      - **Required**. The type of change. This value can be set to one of the
        following:

        - ``add``: The trait or value at the given path was added.
        - ``remove``: The trait or value at the given path was removed.
        - ``update``: The trait or value at the given path was changed.
        - ``any``: The trait or value at the given path was added, removed,
          or changed.
        - ``presence``: The trait or value at the given path was either
          added or removed.
    * - path
      - ``string``
      - A JSON pointer as described in :rfc:`6901` that points to the values
        to compare from the original model to the updated model. If omitted
        or if an empty string is provided (``""``), the entire trait is used
        as the value for comparison. The provided pointer MUST correctly
        correspond to shapes in the model.
    * - severity
      - ``string``
      - Defines the severity of the change. This value can be set to:

        - ``ERROR``: The change is backward incompatible. This is the default
          assumed severity.
        - ``DANGER``: The change is very likely backward incompatible.
        - ``WARNING``: The change might be backward incompatible.
        - ``NOTE``: The change is likely ok, but should be noted during
          things like code reviews.
    * - message
      - ``string``
      - Provides an optional plain text message that provides information about
        why the detected change could be problematic.

It is a backward incompatible change to add the following trait to an
existing shape:

.. code-block:: smithy

    @trait(breakingChanges: [{change: "add"}])
    structure cannotAdd {}

.. note::

    The above trait definition is equivalent to the following:

    .. code-block:: smithy

        @trait(
            breakingChanges: [
                {
                    change: "add",
                    path: "",
                    severity: "ERROR"
                }
            ]
        )
        structure cannotAdd {}

It is a backward incompatible change to add or remove the following trait from
an existing shape:

.. code-block:: smithy

    @trait(breakingChanges: [{change: "presence"}])
    structure cannotToAddOrRemove {}

It is very likely backward incompatible to change the "foo" member of the
following trait or to remove the "baz" member:

.. code-block:: smithy

    @trait(
        breakingChanges: [
            {
                change: "update",
                path: "/foo",
                severity: "DANGER"
            },
            {
                change: "remove",
                path: "/baz",
                severity: "DANGER"
            }
        ]
    )
    structure fooBaz {
        foo: String,
        baz: String
    }

So for example, if the following shape:

.. code-block:: smithy

    @fooBaz(foo: "a", baz: "b")
    string Example

Is changed to:

.. code-block:: smithy

    @fooBaz(foo: "b")
    string Example

Then the change to the ``foo`` member from "a" to "b" is backward
incompatible, as is the removal of the ``baz`` member.

Referring to list members
^^^^^^^^^^^^^^^^^^^^^^^^^

The JSON pointer can path into the members of a list using a ``member``
segment.

In the following example, it is a breaking change to change values of lists
or sets in instances of the ``names`` trait:

.. code-block:: smithy

    @trait(
        breakingChanges: [
            {
                change: "update",
                path: "/names/member"
            }
        ]
    )
    structure names {
        names: NameList
    }

    @private
    list NameList {
        member: String
    }

So for example, if the following shape:

.. code-block:: smithy

    @names(names: ["Han", "Luke"])
    string Example

Is changed to:

.. code-block:: smithy

    @names(names: ["Han", "Chewy"])
    string Example

Then the change to the second value of the ``names`` member is
backward incompatible because it changed from ``Luke`` to ``Chewy``.

Referring to map members
^^^^^^^^^^^^^^^^^^^^^^^^

Members of a map shape can be referenced in a JSON pointer using
``key`` and ``value``.

The following example defines a trait where it is backward incompatible
to remove a key value pair from a map:

.. code-block:: smithy

    @trait(
        breakingChanges: [
            {
                change: "remove",
                path: "/key"
            }
        ]
    )
    map jobs {
        key: String,
        value: String
    }

So for example, if the following shape:

.. code-block:: smithy

    @jobs(Han: "Smuggler", Luke: "Jedi")
    string Example

Is changed to:

.. code-block:: smithy

    @jobs(Luke: "Jedi")
    string Example

Then the removal of the "Han" entry of the map is flagged as backward
incompatible.

The following example detects when values of a map change.

.. code-block:: smithy

    @trait(
        breakingChanges: [
            {
                change: "update",
                path: "/value"
            }
        ]
    )
    map jobs {
        key: String,
        value: String
    }

So for example, if the following shape:

.. code-block:: smithy

    @jobs(Han: "Smuggler", Luke: "Jedi")
    string Example

Is changed to:

.. code-block:: smithy

    @jobs(Han: "Smuggler", Luke: "Ghost")
    string Example

Then the change to Luke's mapping from "Jedi" to "Ghost" is
backward incompatible.

.. note::

    * Using the "update" ``change`` type with a map key has no effect.
    * Using any ``change`` type other than "update" with map values has no
      effect.


..  _prelude:

-------
Prelude
-------

All Smithy models automatically include a *prelude*. The prelude defines
various simple shapes and every trait defined in the core specification.
When using the :ref:`IDL <idl>`, shapes defined in the prelude that are not
marked with the :ref:`private-trait` can be referenced from within any
namespace using a relative shape ID.

.. literalinclude:: ../../../smithy-model/src/main/resources/software/amazon/smithy/model/loader/prelude.smithy
    :language: smithy
    :caption: Smithy Prelude
    :name: prelude-shapes

.. note::

    Private shapes defined in the prelude are subject to change at any time.


.. _unit-type:

Unit type
=========

Smithy provides a singular `unit type`_ named ``smithy.api#Unit``. The unit
type in Smithy is similar to ``Void`` and ``None`` in other languages. It is
used when the input or output of an :ref:`operation <operation>` has no
meaningful value or if a :ref:`union <union>` member has no meaningful value.
``smithy.api#Unit`` MUST NOT be referenced in any other context.

The ``smithy.api#Unit`` shape is defined in Smithy's :ref:`prelude <prelude>`
as a structure shape marked with the ``smithy.api#unitType`` trait to
differentiate it from other structures. It is the only such structure in the
model that can be marked with the ``smithy.api#unitType`` trait.

.. seealso:: :ref:`union`, :ref:`operation`


.. _Option type: https://doc.rust-lang.org/std/option/enum.Option.html
