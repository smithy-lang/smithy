.. _aggregate-types:

---------------
Aggregate types
---------------

Aggregate types contain configurable member references to others shapes.


.. _list:

List
====

The :dfn:`list` type represents an ordered homogeneous collection of values.
A list shape requires a single member named ``member``. Lists are defined
in the IDL using a :ref:`list_statement <idl-list>`.
The following example defines a list with a string member from the
:ref:`prelude <prelude>`:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    list MyList {
        member: String
    }


List member optionality
-----------------------

Lists are considered *dense* by default, meaning they cannot contain ``null``
values. A list MAY be made *sparse* by applying the :ref:`sparse-trait`. The
following example defines a sparse list:

.. code-block:: smithy

    @sparse
    list SparseList {
        member: String
    }


List member shape ID
--------------------

The shape ID of the member of a list is the list shape ID followed by
``$member``. For example, the shape ID of the list member in the above
example is ``smithy.example#MyList$member``.


.. _map:

Map
===

The :dfn:`map` type represents a map data structure that maps ``string``
keys to homogeneous values. A map requires a member named ``key``
that MUST target a ``string`` shape and a member named ``value``.
Maps are defined in the IDL using a :ref:`map_statement <idl-map>`.
The following example defines a map of strings to integers:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    map IntegerMap {
        key: String
        value: Integer
    }


Map member optionality
----------------------

Map keys are never optional
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Map keys are not permitted to be ``null``. Not all protocol serialization
formats have a way to define ``null`` map keys, and map implementations
across programming languages often do not allow ``null`` keys in maps.

Map values are always present by default
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Maps values are considered *dense* by default, meaning they cannot contain
``null`` values. A map MAY be made *sparse* by applying the
:ref:`sparse-trait`. The following example defines a sparse map:

.. code-block:: smithy

    @sparse
    map SparseMap {
        key: String
        value: String
    }


Map member shape IDs
--------------------

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

The following example defines a structure with three members, one of which
is marked with the :ref:`required-trait`, and one that is marked with the
:ref:`default-trait` using IDL syntactic sugar.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    structure MyStructure {
        foo: String

        @required
        baz: Integer

        greeting: String = "Hello"
    }

.. seealso::

    * :ref:`idl-applying-traits` for a description of how to apply traits.
    * :doc:`mixins` to reduce structure duplication
    * :ref:`idl-target-elision` to define members that inherit target from
      resources or mixins.

Adding new structure members
----------------------------

Members MAY be added to structures. New members MUST NOT be marked with the
:ref:`required-trait`. New members SHOULD be added to the end of the
structure. This ensures that programming languages that require a specific
data structure layout or alignment for code generated from Smithy models are
able to maintain backward compatibility.


Structure member shape IDs
--------------------------

The shape ID of a member of a structure is the structure shape ID, followed
by ``$``, followed by the member name. For example, the shape ID of the ``foo``
member in the above example is ``smithy.example#MyStructure$foo``.


.. _structure-optionality:

Structure member optionality
----------------------------

Whether a structure member is optional is determined by evaluating the
:ref:`required-trait`, :ref:`default-trait`, :ref:`clientOptional-trait`,
:ref:`input-trait`, and :ref:`addedDefault-trait`. Authoritative model
consumers like servers MAY choose to determine optionality using more
restrictive rules by ignoring the ``@input`` and ``@clientOptional`` traits.

.. list-table::
    :header-rows: 1
    :widths: 25 25 50

    * - Trait
      - Authoritative
      - Non-Authoritative
    * - :ref:`@clientOptional <clientOptional-trait>`
      - Ignored
      - Optional regardless of the ``@required`` or ``@default`` trait
    * - :ref:`@input <input-trait>`
      - Ignored
      - All members are optional regardless of the ``@required`` or ``@default`` trait
    * - :ref:`@required <required-trait>`
      - Present
      - Present unless also ``@clientOptional`` or part of an ``@input`` structure
    * - :ref:`@default <default-trait>`
      - Present
      - Present unless also ``@clientOptional`` or part of an ``@input`` structure
    * - (Other members)
      - Optional
      - Optional


Required members
~~~~~~~~~~~~~~~~

The :ref:`required-trait` indicates that a value MUST always be present for a
member in order to create a valid structure. Code generators SHOULD generate
accessors for these members that always return a value.

.. code-block:: smithy

    structure TimeSpan {
        // years must always be present to make a TimeSpan
        @required
        years: Integer
    }


Client error correction
^^^^^^^^^^^^^^^^^^^^^^^

If a mis-configured server fails to serialize a value for a required member,
to avoid downtime, clients MAY attempt to fill in an appropriate default value
for the member:

* boolean: false
* numbers: 0
* timestamp: 0 seconds since the Unix epoch
* string: ""
* blob: empty bytes
* document: null
* list: []
* map: {}
* enum, intEnum, union: The unknown variant. These types SHOULD define an
  unknown variant to account for receiving unknown members.
* union: The unknown variant. Code generators for unions SHOULD define an
  unknown variant to account for newly added members.
* structure: {} if possible, otherwise a deserialization error.


Default values
~~~~~~~~~~~~~~

The :ref:`default-trait` gives a structure member a default value. The
following example uses syntactic sugar in the Smithy IDL allows to assign
a default value to the ``days`` member.

.. code-block:: smithy

    structure TimeSpan {
        @required
        years: Integer

        days: Integer = 0
    }


Evolving requirements and members
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Requirements change; what is required today might not be required tomorrow.
Smithy provides several ways to make it so that required members no longer
need to be provided without breaking previously generated code.

Migrating ``@required`` to ``@default``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If a ``required`` member no longer needs to be be required, the ``required``
trait MAY be removed and replaced with the :ref:`default-trait`. Alternatively,
a ``default`` trait MAY be added to a member marked as ``required`` to
provide a default value for the member but require that it is serialized.
Either way, the member is still considered always present to tools like code
generators, but instead of requiring the value to be provided by an end-user,
a default value is automatically provided if missing. For example, the previous
``TimeSpan`` model can be backward compatibly changed to:

.. code-block:: smithy

    structure TimeSpan {
        // @required is replaced with @default and @addedDefault
        @addedDefault
        years: Integer = 0
        days: Integer = 0
    }

The :ref:`addeddefault-trait` trait SHOULD be used any time a ``default`` trait is
added to a previously published member. Some tooling does not treat the
``required`` trait as non-nullable but does treat the ``default`` trait as
non-nullable.

Requiring members to be optional
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The :ref:`clientOptional-trait` is used to indicate that a member that is
currently required by authoritative model consumers like servers MAY become
completely optional in the future. Non-authoritative model consumers like
client code generators MUST treat the member as if it is not required and
has no default value. Authoritative model consumers MAY choose to ignore
the ``clientOptional`` trait.

For example, the following structure:

.. code-block:: smithy

    structure UserData {
        @required
        @clientOptional
        summary: String
    }

Can be backward-compatibly updated to remove the ``required`` trait:

.. code-block:: smithy

    structure UserData {
        summary: String
    }

Replacing both the ``required`` and ``clientOptional`` trait with the ``default``
trait is *not* a backward compatible change because model consumers would
transition from assuming the value is optional to assuming that it is always
present due to a default value.

Model evolution and the ``@input`` trait
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The :ref:`input-trait` specializes a structure as the input of a single
operation. Transitioning top-level members from ``required`` to optional is
allowed for such structures because it is loosening an input constraint.
Non-authoritative model consumers like clients MUST treat each member as
nullable regardless of the ``required`` or ``default`` trait. This means that
it is a backward compatible change to remove the ``required`` trait from a
member of a structure marked with the ``input`` trait, and the ``default``
trait does not need to be added in its place.

The special ":=" syntax for the operation input property automatically applies
the ``input`` trait:

.. code-block:: smithy

    operation PutTimeSpan {
        input := {
            @required
            years: String
        }
    }

Because of the ``input`` trait, the operation can be updated to remove the
``required`` trait without breaking things like previously generated clients:

.. code-block:: smithy

    operation PutTimeSpan {
        input := {
            years: String
        }
    }


.. _union:

Union
=====

The union type represents a `tagged union data structure`_ that can take
on several different, but fixed, types. Unions function similarly to
structures except that only one member can be used at any one time. Each
member in the union is a variant of the tagged union, where member names
are the tags of each variant, and the shapes targeted by members are the
values of each variant.

Unions are defined in the IDL using a :ref:`union_statement <idl-union>`.
A union shape MUST contain one or more named :ref:`members <member>`.
The following example defines a union shape with several members:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    union MyUnion {
        i32: Integer

        @length(min: 1, max: 100)
        string: String,

        time: Timestamp,
    }


Unit types in unions
--------------------

Some union members might not need any meaningful information beyond the
tag itself. For these cases, union members MAY target Smithy's built-in
:ref:`unit type <unit-type>`, ``smithy.api#Unit``.

The following example defines a union for actions a player can take in a
game.

.. code-block:: smithy

    union PlayerAction {
        /// Quit the game.
        quit: Unit,

        /// Move in a specific direction.
        move: DirectedAction,

        /// Jump in a specific direction.
        jump: DirectedAction
    }

    structure DirectedAction {
        @required
        direction: Integer
    }

The ``quit`` action has no meaningful data associated with it, while ``move``
and ``jump`` both reference ``DirectedAction``.


Union member presence
---------------------

Exactly one member of a union MUST be set. The serialization of a union is
defined by a :ref:`protocol <protocolDefinition-trait>`, but for example
purposes, if unions were to be represented in a hypothetical JSON
serialization, the following value would be valid for the ``PlayerAction``
union because a single member is present:

.. code-block:: json

    {
        "move": {
            "direction": 1
        }
    }

The following value is **invalid** because multiple members are present:

.. code-block:: json

    {
        "quit": {},
        "move": {
            "direction": 1
        }
    }

The following value is **invalid** because no members are present:

.. code-block:: json

    {}


Adding new union members
------------------------

New members added to existing unions SHOULD be added to the end of the
union. This ensures that programming languages that require a specific
data structure layout or alignment for code generated from Smithy models are
able to maintain backward compatibility.


Union member shape IDs
----------------------

The shape ID of a member of a union is the union shape ID, followed
by ``$``, followed by the member name. For example, the shape ID of the ``i32``
member in the above example is ``smithy.example#MyUnion$i32``.


.. _recursive-shape-definitions:

Recursive shape definitions
===========================

Smithy allows recursive shape definitions with the following limitations:

1. The member of a list or map cannot directly or transitively target
   its containing shape unless one or more members in the path from the
   container back to itself targets a structure or union shape. This ensures
   that shapes that are typically impossible to define in various programming
   languages are not defined in Smithy models (for example, you can't define
   a recursive list in Java ``List<List<List....``).
2. To ensure a value can be provided for a structure, recursive member
   relationship from a structure back to itself MUST NOT be made up of all
   :ref:`required <required-trait>` structure members.
3. To ensure a value can be provided for a union, recursive unions MUST
   contain at least one path through its members that is not recursive
   or steps through a list, map, or optional structure member.

The following recursive shape definition is **valid**:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    list ValidList {
        member: IntermediateStructure
    }

    structure IntermediateStructure {
        foo: ValidList
    }

The following recursive shape definition is **invalid**:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    list RecursiveList {
        member: RecursiveList
    }

The following recursive shape definition is **invalid** due to mutual
recursion and the :ref:`required-trait`.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    structure RecursiveShape1 {
        @required
        recursiveMember: RecursiveShape2
    }

    structure RecursiveShape2 {
        @required
        recursiveMember: RecursiveShape1
    }

.. _tagged union data structure: https://en.wikipedia.org/wiki/Tagged_union
.. _unit type: https://en.wikipedia.org/wiki/Unit_type
