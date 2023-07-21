--------------------------------------
Mapping Smithy Shapes to Your Language
--------------------------------------

One of the first design documents to write is how shapes in Smithy
will map to types in your target environment. The way shapes map to a
target environment can also vary depending on if you are generating a
client or server.


Interoperability
================

When determining how shapes are represented in a target environment, adhering
to the Smithy specification is a hard requirement and the first of Smithy's
tenets. This implies two practical considerations to keep in mind:

1. Generators cannot create a client that will break backward
   compatibility if a service team makes a backward compatible change
   according to the Smithy specification. Any deviation from this should
   require opt-in from the end-user and emit warnings.
2. Code generators should not enforce their own restrictions on top of
   the restrictions defined in the Smithy model. For example, if a
   particular identifier is a reserved word in the target programming
   language, the code generator should automatically modify the
   identifier to deconflict it with the reserved word.


Smithy shapes
=============

This section identifies the non-exhaustive shapes and traits that code
generators need to account for (additional details on each type of shape
and trait can be found in the Smithy specification).

.. note::

    When mapping Smithy shapes to a target environment, you may
    decide that the abstractions provided in the standard library of the
    target environment aren't ergonomic enough or don't map well to Smithy.
    In these cases, you can provide your own abstractions. For example, the
    AWS SDK for Java created an `SdkBytes`_ class to make it easier to
    provide the contents of a blob to the SDK.


When to Generate unique named types
-----------------------------------

The following shapes *should not* generate uniquely named types based on the
name provided in a model:

* blobs
* booleans
* strings
* numbers
* documents
* lists
* maps

When possible, the above types should use the target environment's equivalent
language built-in type (for example, a Smithy string would become a
Java ``String```). Creating Smithy-specific types where an idiomatic language
built-in type is available hurts the developer experience of the generator
and should be avoided. Furthermore, changing the names of these types in the
Smithy model should not impact generated code.

The following shapes *are* expected to translate into named generated types
or methods in the target environment:

* services
* operations
* structures
* unions
* structure and union member names
* enums
* intEnums
* enum and intEnum member names


Blob
----

Blobs represent opaque binary data. There are three kinds of blobs:

1. Blobs that are expected to fit into memory. These shapes are not
   marked with the :ref:`streaming-trait`. Such blobs should be
   represented as a kind of byte array that is stored in memory
   (for example, ``byte[]`` in Java, or a string in PHP).
2. Unbounded blobs that are not expected to fit into memory. These
   blobs are marked with the ``streaming`` trait and should be
   represented using some kind of streaming abstraction that
   can work with potentially infinite streams of data.
3. Bounded blobs that are not expected to fit into memory. These
   blobs are marked with both the ``streaming`` trait and the
   :ref:`requireslength-trait`, which implies that the stream has
   some method for "telling" callers its length.


Boolean
-------

Boolean shapes in Smithy represent true or false values. These should
always map to a language's standard Boolean type.


Document
--------

The document type represents untyped data. Document types by default are
JSON-like values that can be set to a string, number, boolean, list, map,
or null. Document types are generally used for truly untyped data that
users are expected to dynamically inspect at runtime.

.. note::

    Document types are limited to the JSON data model for now; however,
    future support for other type systems *could* be added to document
    types (for example, a CBOR document).


String
------

Strings should be represented using string types from the target
environment's standard library when possible.

- Avoid creating custom string type for Smithy code generators unless
  it's absolutely necessary.
- Never generate specific types for normal strings — the name of a
  normal string shape is irrelevant and should not appear in generated
  code.
- Code used to represent strings must be able to losslessly round-trip
  UTF-8 data. Don't create a custom string type if your programming
  language represents strings as bytes (like in PHP) or uses UTF-16
  (like Java). However, if the string type in a given language can only
  contain, say, ASCII characters, then you should use some kind of byte
  array to represent strings.

Strings in Smithy can be marked with the :ref:`enum-trait`; however,
Smithy code generators should transform models prior to code generation
to convert these kinds of strings to proper ``enum`` shapes.


Enums
-----

Smithy IDL V2 introduced a proper :ref:`enum` shape that obsoletes the
:ref:`enum-trait`. Enums define a set of allowed string values that can be
provided for the shape.

.. important::

      Because client implementations often lag behind service, clients
      *must not* fail to deserialize and serialize unknown enum values.
      For example, implementations could use a kind of discriminated union
      with a catch-all unknown value placeholder, provide additional
      accessor methods to retrieve the raw string value of an enum,
      or some other technique to carry unknown values.

Consider the following Smithy model:

.. code-block:: smithy

    enum Suit {
        DIAMOND
        CLUB
        HEART
        SPADE
    }

It could be generated as the following enum in Rust:

.. code-block:: rust

    #[non_exhaustive]
    enum Suit {
       DIAMOND,
       CLUB,
       HEART,
       SPADE,

       #[non_exhaustive]
       Unknown(String)
    }

Notice that unknown enum variants are captured in ``Unknown``, along
with the unknown value. This allows the enum type to store newly added
values that the client doesn't yet know about. Also note that the enum
is ``non_exhaustive``, because new enum values can be added in the
future, and we want consumers of the generated code to account for this.

Smithy also supports :ref:`intEnum`. It's just like an ``enum`` but is an
enum of integer values. ``intEnum`` shapes must also support sending and
receiving unknown integer values to account for newly added enum
members. For example:

.. code-block:: smithy

   intEnum FaceCard {
       JACK = 1
       QUEEN = 2
       KING = 3
       ACE = 4
       JOKER = 5
   }


Timestamp
---------

A :ref:`timestamp` shape represents an instant in time with no UTC
offset or timezone. For example, to represent a timestamp in Java,
you would use `java.time.Instant`_ and not `java.time.OffsetDateTime`_
because a timestamp has no UTC offset.

The serialization format of a timestamp is an implementation detail
determined by a :ref:`protocol <protocoldefinition-trait>` and must not
have any effect on the types exposed by tooling to represent a timestamp
value. If a timestamp in one service is serialized as a string and in
another service as an integer, the type exposed by the code generator to
represent these timestamps must be exactly the same type. Put another
way: changing the protocol and serialization format of a timestamp should
not break previously generated code.


Numbers: byte, short, integer, long, float, double, bigInteger, bigDecimal
--------------------------------------------------------------------------

Smithy supports various numeric types. If a target environment does not
support smaller types like byte, short, or float, then these types
should be rolled into the next largest supported numeric type (e.g.,
byte → integer, short → integer, float → double).

If a target environment does not support :ref:`bigInteger` (an arbitrary
precision integer) or :ref:`bigDecimal` (an arbitrary precision decimal),
then a library dependency should be used if and only if one of these types are
encountered or the runtime library of the generator should provide an
implementation.

.. note::

    If a library is needed to provide support for larger numeric types,
    then the library should only be required conditionally if the type is
    used in the service closure. This can be handled automatically using
    Smithy symbol and symbol dependency abstractions, or by crawling the
    shapes in a service closure to detect specific types.


List
----

The *list* type represents an ordered homogeneous collection of values.
A list type should be code generated using the list or array type
provided in the standard library of the target environment.

.. rubric:: Value presence

*  List values are always present (non-nullable) unless the list is marked
   with the ``@sparse`` trait.


Ignore set shapes from Smithy 1.0
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The ``set`` type was deprecated in Smithy 1.0 and removed in Smithy
2.0. Smithy model implementations should automatically add the
:ref:`uniqueItems-trait` to set shapes, and code generators should
treat set shapes exactly like list shapes marked with ``uniqueItems``.

.. note::

   When using the Smithy Java reference implementation, the
   ``uniqueItems`` trait is automatically added to set shapes, and the
   class used to represent set shapes, SetShape, extends from ListShape,
   allowing you to ignore the difference between list and set shapes
   altogether.


Map
---

The :ref:`map` type represents a map data structure that maps
``string`` keys to homogeneous values. Maps are not required to
maintain insertion order. Implementations should use the idiomatic
map data structure of the target environment when possible.

.. rubric:: Key and value presence

* Map keys are always present and never nullable.
* Map values are always present (non-nullable) unless the map is
  marked with the :ref:`sparse-trait`.


Structure
---------

The *structure* type represents a fixed set of named, heterogeneous
values. Structures are always code generated and use the name provided
in the model. Structures are generally code generated into things like
POJOs, POCOs, etc. Smithy IDL 2.0 supports adding a :ref:`default-trait` to
structure members. Some target environments allow types to be created using a
kind of literal syntax that does not perform any custom initialization.
In these cases, it may be necessary to use a constructor method in order
to set members to their default zero values if needed.

Structure and union members are ordered based on the order they are
defined in the model. When adding new members, they should be added to
the end of the structure. While this allows code generators like C++ to
maintain ABI compatibility, it requires extreme levels of rigor to
enforce that every change will be ABI compatible.


Error structures
~~~~~~~~~~~~~~~~

Structures marked with the ``@error`` trait should be code generated as
a kind of error type or exception type in the target environment. A good
design goal for errors generated from Smithy models is to allow generic
abstractions to work across generated Smithy clients. For example,
developers should be able to create a middleware that can be used with
any Smithy generated client to check if an error is a client error,
server error, retryable, or throttling error. The :ref:`retryable-trait`
is used to describe if an error can be retried, and the ``throttling``
property of this trait describes if the error is due to throttling. This
information should be exposed by the generated type in some way.

Errors could have a kind of hierarchy resembling the following (note
that other error conditions like networking errors need to be accounted
for as well):

- Service specific error: a top-level error type generated specifically
  for every error the service can return. This error is used when an
  unmodeled exception is encountered.
- Client Error: Error used when an ``@error`` trait is set to "client".
- Server Error: Error used when an ``@error`` trait is set to "server".


Union
-----

The union type represents a `tagged union data
structure <https://en.wikipedia.org/wiki/Tagged_union>`__ that can take
on several different, but fixed, types. Unions function similarly to
structures except that only one member can be used at any one time.
Unions are always code generated and use the name provided in the model.
Code generators should provide some kind of abstraction to make union
types easier to use. For example, if a target environment supports sum
types or discriminated unions, use them. Sealed classes with specific
subtypes for each variant of the union are also good options.

- The member that is set in a union cannot be optional.
- There must be exactly one member of the union set to a non-null
  value.
- Clients must account for unknown union values by storing the name of
  the unknown variant.


Unit types in unions
~~~~~~~~~~~~~~~~~~~~

Union members may target Smithy's built-in unit type, ``smithy.api#Unit``,
meaning the member has no meaningful value. If a member targets the unit type,
implementations should generate code that omits the value for that variant or
sets the value to a specific type (e.g., ``Void`` in Java). You can detect
if a member targets the Unit type using the following:

.. code-block:: java

   import software.amazon.smithy.model.traits.UnitTypeTrait;

   for (MemberShape member : unitShape.members()) {
       if (member.getTarget().equals(UnitTypeTrait.UNIT)) {
           // Generate special code to handle unit types.
       } else {
           // The member is a normal shape.
       }
   }


Service
-------

A *service* is the entry point of an API that aggregates resources and
operations together. The service shape will tell you which protocols a
service supports, which auth schemes it supports, the operations of the
service, and the resources contained in the service.


Computing a service closure
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The closure of shapes connected to a service are the shapes that will be
code generated. You can compute this closure using a
`Walker <https://github.com/smithy-lang/smithy/blob/main/smithy-model/src/main/java/software/amazon/smithy/model/neighbor/Walker.java>`__:

.. code-block:: java

   Walker walker = new Walker(someModel);
   Set<Shape> closure = walker.walkShapes(someService);

You can get the entire set of operations contained in a service using a
``TopDownIndex``:

.. code-block:: java

   TopDownIndex index = TopDownIndex.of(model);
   Set<OperationShape> operations = index.getContainedOperations(someService);

.. tip::

    :ref:`directedcodegen` automatically handles this for you.


Service renames
~~~~~~~~~~~~~~~

Services might need to "rename" shapes in order to disambiguate shapes
that share the same name. This is done so that namespaces in the Smithy
model do no need to have a 1:1 namespace mapping in generated code. When
determining the name of a shape for use in codegen, never rely on the
shape ID directly, but rather first check if the shape was renamed
within the closure of a service. This can be done by passing a
``ServiceShape`` into ``ShapeId#getName``:

.. code-block:: java

   // Good!
   String goodCodegenName = someShapeId.getName(someServiceShape);

   // Bad!
   String badCodegenName = someShapeId.getName();


Operation
---------

The *operation* type represents the input, output, and possible errors
of an API operation.


Generating unique input and output shapes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Client code generators must generate distinct types for all operation
input and output shape structures. Members of an input structure should
all be treated as optional regardless of if the member is marked with
the ``@default`` trait or ``@required`` trait. This allows service teams
to evolve their API without breaking previously generated clients.

A more recent feature of Smithy allows marking structures as specific to
the input or output of an operation using ``@input`` and ``@output``
traits. You can transform the model being code generated and create
synthetic input and output shapes when necessary using the
`createDedicatedInputAndOutput <https://github.com/smithy-lang/smithy/blob/main/smithy-model/src/main/java/software/amazon/smithy/model/transform/ModelTransformer.java#L546>`_
model transformer. The following example creates a new Model that has
dedicated input and output shapes for every operation, each marked with
the ``@input`` or ``@output`` trait, and each uses a consistent name
that ends with ``Input`` or ``Output``.

.. code-block:: java

   ModelTransformer transformer = ModelTransformer.create();
   Model transformed = transformer.createDedicatedInputAndOutput(
       "model", "Input", "Output"
   );

.. tip::

    :ref:`directedcodegen` automatically handles this for you.


Resource
--------

A *resource* is an entity with an identity that has a set of operations.

Resources add hierarchy to a model. You will need to traverse from a
service to every operation and resource in order to crawl the entire
service. This process can be simplified using the
`TopDownIndex <https://github.com/smithy-lang/smithy/blob/main/smithy-model/src/main/java/software/amazon/smithy/model/knowledge/TopDownIndex.java>`__.
Iterating only over the operations attached to a service will not
provide every operation in the closure of the service.

.. note::

    Exposing resource abstractions through code, if attempted, should
    be done in addition to a more traditional, flattened, service
    interface with every operation contained in the service.


Other shape topics
==================

Shapes can be recursive
-----------------------

Smithy :ref:`shapes support recursion <recursive-shape-definitions>`.
Some languages like Rust require the size of types to be known at
compile time. Recursive types in these languages need some kind of heap
allocation to ensure they have a size known at compile time. In order to
identify which member in a recursive loop needs to be heap allocated,
implementations will need to utilize a topological sort. Smithy's
:ref:`directedcodegen` abstraction will automatically generate code based
on a topological sort, though generators that need more control over how to handle recursion will need to manually
use a `TopologicalIndex <https://github.com/smithy-lang/smithy/blob/main/smithy-codegen-core/src/main/java/software/amazon/smithy/codegen/core/TopologicalIndex.java>`__.

.. seealso::

    `Rust design doc <https://github.com/awslabs/smithy-rs/blob/main/design/src/smithy/recursive_shapes.md>`__
    for how they handled recursive shapes.


.. _codegen-mixins-are-an-implementation-detail:

Mixins are an implementation detail of the model
------------------------------------------------

Mixins are considered an implementation detail of a model and should not
impact code generation. Code generators should transform the model prior
to code generation to remove mixins.

.. seealso::

   :ref:`Flattening mixins <codegen-flattening-mixins>`


Member optionality
------------------

Smithy has different rules around when a member is always present or
optional. The rules around nullability are defined in the Smithy
specification. However, all of this complexity is accounted for
automatically using the
`NullableIndex <https://github.com/smithy-lang/smithy/blob/main/smithy-model/src/main/java/software/amazon/smithy/model/knowledge/NullableIndex.java>`_.

.. code-block:: java

   NullableIndex index = NullableIndex.of(model);
   if (index.isNullable(someMember)) {
       // optional
   } else {
       // always present
   }


FAQ
===

Should constraint traits impact generated types?
------------------------------------------------

In general, no. Baking anything about these traits into generated types
makes these traits impossible to change in the future without breaking
previously generated code.

- Length and pattern traits should have no impact on generated types.
  Shapes with a length or pattern trait should be represented as a
  standard string type.
- Range traits must have no impact on generated types. Code generators
  must not rely on range traits to determine which numeric type is best
  for representing a Smithy shape. Instead, code generators must rely
  on the Smithy type used for the numeric shape (integer, long, etc).
- The ``@enum`` trait was replaced by the ``enum`` shape in Smithy IDL
  2.0. Both the ``@enum`` trait and ``enum`` shape can influence code
  generation, though they must be considered a specialization of
  strings. This allows servers to add new enum values over time without
  breaking previously generated clients.
- The ``@required`` trait is no longer considered a constraint trait in
  Smithy IDL 2.0. It is now expected to influence code generated types.


Should clients enforce constraint traits?
-----------------------------------------

No. A client should defer the validation of constraint traits to the
service.


Why don't we validate constraint traits on the client?
------------------------------------------------------

Validating constraint traits on the client makes it extremely hard to
change constraint traits, even in what appears to be a backward
compatible change. Changes to constraint traits need to be backward
compatible by making the constraint more relaxed. However, things fall
apart when multiple actors are creating resources using different
versions of the service.

Years ago, many AWS SDKs validated constraint traits client-side and
refused to send non-compliant input. During that period, Amazon EC2
updated instance IDs to support a longer string, and existing SDKs began
to break when this change was deployed. That's because the use of other
tools like the AWS Management Console or even managed services created
instances that SDKs could no longer interact with. If a client
encountered an instance created in the console and then tried to make a
subsequent call using the instance ID, the client would refuse to send
the request. This resulted in a large amount of customer pain and took
months of effort to correct. If the client ignored constraint traits and
allowed the service to enforce them, the change would have been
transparent to previously generated clients.


Do generators need to worry about mixins?
-----------------------------------------

No. See :ref:`codegen-mixins-are-an-implementation-detail`.


Is there an easier way to account for errors of operations inheriting service errors?
-------------------------------------------------------------------------------------

Yes. You can flatten error hierarchies before generating code. This is
also something that :ref:`directedcodegen` can handle for you.

.. seealso::

    :ref:`Copying service errors to operation errors <codegen-copying-errors-to-service>`


.. _SdkBytes: https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/main/java/software/amazon/awssdk/core/SdkBytes.java
.. _java.time.Instant: https://docs.oracle.com/javase/8/docs/api/java/time/Instant.html
.. _java.time.OffsetDateTime: https://docs.oracle.com/javase/8/docs/api/java/time/OffsetDateTime.html
