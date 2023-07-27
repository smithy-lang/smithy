.. _simple-types:

------------
Simple types
------------

*Simple types* are types that do not contain nested types or shape references.


.. _blob:

blob
====

A blob is uninterpreted binary data.

.. code-block:: smithy

    blob MyBlob


.. _boolean:

boolean
=======

A boolean is a Boolean value type.

.. code-block:: smithy

    boolean MyBoolean


.. _string:

string
======

A string is a UTF-8 encoded string.

.. code-block:: smithy

    string MyString


.. _byte:

byte
====

A byte is an 8-bit signed integer ranging from -128 to 127 (inclusive).

.. code-block:: smithy

    byte MyByte


.. _short:

short
=====

A short is a 16-bit signed integer ranging from -32,768 to 32,767 (inclusive).

.. code-block:: smithy

    short MyShort


.. _integer:

integer
=======

An integer is a 32-bit signed integer ranging from -2^31 to (2^31)-1 (inclusive).

.. code-block:: smithy

    integer MyInteger


.. _long:

long
====

A long is a 64-bit signed integer ranging from -2^63 to (2^63)-1 (inclusive).

.. code-block:: smithy

    long MyLong


.. _float:

float
=====

A float is a single precision IEEE-754 floating point number.

.. code-block:: smithy

    float MyFloat


.. _double:

double
======

A double is a double precision IEEE-754 floating point number.

.. code-block:: smithy

    double MyDouble


.. _bigInteger:

bigInteger
==========

A bigInteger is an arbitrarily large signed integer.

.. code-block:: smithy

    bigInteger MyBigInteger


.. _bigDecimal:

bigDecimal
==========

A bigDecimal is an arbitrary precision signed decimal number.

.. code-block:: smithy

    bigDecimal MyBigDecimal


.. _timestamp:

timestamp
=========

A timestamp represents an instant in time in the proleptic Gregorian calendar,
independent of local times or timezones. Timestamps support an allowable date
range between midnight January 1, 0001 CE to 23:59:59.999 on
December 31, 9999 CE, with a temporal resolution of 1 millisecond. This
resolution and range ensures broad support across programming languages and
guarantees compatibility with :rfc:`3339`.

.. code-block:: smithy

    timestamp MyTimestamp


Timestamp serialization and deserialization
-------------------------------------------

The serialization format of a timestamp is an implementation detail that is
determined by a :ref:`protocol <protocolDefinition-trait>` and or
:ref:`timestampFormat-trait`. The format of a timestamp MUST NOT have any
effect on the types exposed by tooling to represent a timestamp value.

Protocols and ``timestampFormat`` traits MAY support temporal resolutions
other than 1 millisecond. For example, the ``http-date`` timestamp format
supports only seconds and forbids fractional precision. Modelers need to be
aware of these limitations when defining timestamps to avoid an unintended
loss of precision.

The use of timestamps outside the allowable range risk not interoperating
correctly across Smithy implementations; deserializers that encounter
timestamps outside the allowable range SHOULD fail to deserialize the value.


.. _document:

document
========

A document represents protocol-agnostic open content that functions as a kind
of "any" type. Document types are represented by a JSON-like data model and
can contain UTF-8 strings, arbitrary precision numbers, booleans, nulls, a
list of these values, and a map of UTF-8 strings to these values. Open content
is useful for modeling unstructured data that has no schema, data that can't
be modeled using rigid types, or data that has a schema that evolves outside
of the purview of a model. The serialization format of a document is an
implementation detail of a protocol and MUST NOT have any effect on the types
exposed by tooling to represent a document value.

.. code-block:: smithy

    document MyDocument


.. _enum:

enum
====

The enum shape is used to represent a fixed set of one or more string values.
Each value listed in the enum is a :ref:`member <member>` that implicitly
targets the :ref:`unit type <unit-type>`.

The following example defines an enum shape:

.. code-block:: smithy

    enum Suit {
        DIAMOND
        CLUB
        HEART
        SPADE
    }

The following example is exactly equivalent to the previous example, but the
enum values are made explicit:

.. code-block:: smithy

    enum Suit {
        DIAMOND = "DIAMOND"
        CLUB = "CLUB"
        HEART = "HEART"
        SPADE = "SPADE"
    }


enum values
-----------

The value of an enum member can be customized by applying the
:ref:`enumValue trait <enumValue-trait>`. If an enum member doesn't have an
explicit ``enumValue`` trait, an ``enumValue`` trait is implicitly added to
the member with the trait value set to the member's name.

The following example provides custom enum values for each member using
syntactic sugar in the Smithy IDL:

.. code-block:: smithy

    enum Suit {
        DIAMOND = "diamond"
        CLUB = "club"
        HEART = "heart"
        SPADE = "spade"
    }

The above example is exactly equivalent to the following:

.. code-block:: smithy

    enum Suit {
        @enumValue("diamond")
        DIAMOND

        @enumValue("club")
        CLUB

        @enumValue("heart")
        HEART

        @enumValue("spade")
        SPADE
    }


enum is a specialization of string
----------------------------------

Enums are considered *open*, meaning it is a backward compatible change to add
new members. Previously generated clients MUST NOT fail when they encounter an
unknown enum value. Client implementations MUST provide the capability of
sending and receiving unknown enum values.


enum validation
---------------

* Enums do not support aliasing; all values MUST be unique.
* Enum member names SHOULD NOT contain any lowercase ASCII Latin letters
  (``a-z``) and SHOULD NOT start with an ASCII underscore (``_``).
  That is, enum names SHOULD match the following regular expression:
  ``^[A-Z]+[A-Z_0-9]*$``.


.. _intEnum:

intEnum
=======

An intEnum is used to represent an enumerated set of one or more integer
values. The members of intEnum MUST be marked with the :ref:`enumValue-trait`
set to a unique integer value. Syntactic sugar in the Smithy IDL can be used
to apply the ``enumValue`` trait. Each value of an intEnum is a
:ref:`member <member>` that implicitly targets the :ref:`unit type <unit-type>`.

The following example defines an intEnum shape using Smithy IDL syntactic
sugar:

.. code-block:: smithy

    intEnum FaceCard {
        JACK = 1
        QUEEN = 2
        KING = 3
        ACE = 4
        JOKER = 5
    }

The above example is exactly equivalent to the following:

.. code-block:: smithy

    intEnum FaceCard {
        @enumValue(1)
        JACK

        @enumValue(2)
        QUEEN

        @enumValue(3)
        KING

        @enumValue(4)
        ACE

        @enumValue(5)
        JOKER
    }


intEnum is a specialization of integer
--------------------------------------

intEnums are considered *open*, meaning it is a backward compatible change to add
new members. Previously generated clients MUST NOT fail when they encounter an
unknown intEnum value. Client implementations MUST provide the capability of
sending and receiving unknown intEnum values.


intEnum validation
------------------

* intEnums do not support aliasing; all values MUST be unique.
* intEnum member names SHOULD NOT contain any lowercase ASCII Latin letters
  (``a-z``) and SHOULD NOT start with an ASCII underscore (``_``).
  That is, enum names SHOULD match the following regular expression:
  ``^[A-Z]+[A-Z_0-9]*$``.
