.. _jmespath-data-model:

===================
JMESPath data model
===================

Several Smithy features use JMESPath_ expressions over model data, including
:ref:`waiters <waiters>`, the :ref:`operationContextParams trait
<smithy.rules#operationContextParams-trait>`, the :ref:`conditions trait
<conditions-trait>`, and the :ref:`contracts trait <contracts-trait>`. This
section defines the data
model those expressions are evaluated against: how Smithy types are exposed as
`JMESPath types`_, and how an operation call is exposed.

--------------------
Type conversions
--------------------

The data model exposed to JMESPath_ for a shape's value is converted from
Smithy types to `JMESPath types`_ using the following conversion table:

.. list-table::
    :header-rows: 1

    * - Smithy type
      - JMESPath type
    * - blob
      - string (base64 encoded)
    * - boolean
      - boolean
    * - byte
      - number
    * - short
      - number
    * - integer
      - number
    * - long
      - number [#fnumbers]_
    * - float
      - number
    * - double
      - number
    * - bigDecimal
      - number [#fnumbers]_
    * - bigInteger
      - number [#fnumbers]_
    * - string
      - string
    * - timestamp
      - number [#ftimestamp]_
    * - document
      - any type
    * - list and set
      - array
    * - map
      - object
    * - structure
      - object [#fstructure]_
    * - union
      - object [#funion]_

Footnotes
~~~~~~~~~~~~

.. [#fnumbers] ``long``, ``bigInteger``, ``bigDecimal`` are exposed as
   numbers to JMESPath. If a value for one of these types truly exceeds
   the value of a double (the native numeric type of JMESPath), then
   querying these types is a bad idea.
.. [#ftimestamp] ``timestamp`` values are represented in JMESPath expressions
   as epoch seconds with optional decimal precision. This allows for
   timestamp values to be used with relative comparators like ``<`` and ``>``.
.. [#fstructure] Structure members are referred to by member name and not
   the data sent over the wire. For example, the :ref:`jsonname-trait` is not
   respected in JMESPath expressions that select structure members.
.. [#funion] ``union`` values are represented exactly like structures except
   only a single member is set to a non-null value.

.. _jmespath-operation-call:

--------------------
Operation calls
--------------------

An operation has no value instance; it is a procedure. What a
:ref:`contract <contracts-trait>` constrains is a single *call*, exposed to
JMESPath as an object with the following members:

- ``input``: an instance of the operation's input shape.
- ``output``: an instance of the output shape on a successful call; absent on failure.
- ``error``: an object ``{shapeId, content}`` on a failed call; absent on success.
  ``output`` and ``error`` are mutually exclusive.
- ``before`` and ``after``: :ref:`world snapshots <world-snapshot>` immediately
  before and after the call. These are model-only (see below).

Expressions reference these members, for example ``input.start < input.end``.

--------------------
Model-only members
--------------------

The ``input``, ``output``, and ``error`` members are *observable*: they are the
data a client actually sends and receives. The ``before`` and ``after``
snapshots are *model-only*: they exist in the model to describe resource state
for reasoning, and are never present on the wire. Features that are evaluated
at runtime, such as waiters, may reference only the observable members.
Model-only members are available only to build-time analysis such as the
:ref:`conditions trait <conditions-trait>`.

--------------------
Reference handles
--------------------

A named :ref:`references-trait` on a structure is projected into the data model
as a resource *handle* reachable as ``<member-root>.<name>`` (for example
``input.key``). The handle is an object ``{service, resource, ids}`` (with
``service`` optional) synthesized from the reference's identifier bindings, and
is intended to be passed to functions that resolve a resource against a state
snapshot. A handle is model-only: it is a pointer synthesized from the
observable identifier members rather than data on the wire. The identifiers
inside it are observable, but the handle itself is not.


.. _jmespath-functions:

--------------------
Functions
--------------------

In addition to the `built-in JMESPath functions`_, Smithy defines the following
functions for use in expressions over this data model. Each is available
wherever the members it reads are in scope.

``resource``
~~~~~~~~~~~~

::

    resource(world, handle) -> object

Looks up a single resource instance in a ``world`` snapshot (a ``before`` or
``after`` state) and returns it, or ``null`` when no instance matches. ``world``
is a :ref:`world snapshot <world-snapshot>`. ``handle`` is an object
``{service, resource, ids}`` (``service`` optional) where ``ids`` maps each
resource identifier name to its value; a named :ref:`references-trait` projects
such a handle automatically (see `Reference handles`_).

``requires``
~~~~~~~~~~~~

::

    requires(call, predicate) -> boolean

Declares a necessary precondition on an operation ``call``. When the call
succeeds (its ``error`` is absent), ``predicate`` MUST be truthy; when the call
fails the requirement is vacuously satisfied. ``predicate`` is evaluated
eagerly, because a necessary precondition reads only pre-call state (``input``
and ``before``) that is present on both the success and failure paths.


.. _JMESPath: https://jmespath.org/
.. _JMESPath types: https://jmespath.org/specification.html#data-types
.. _built-in JMESPath functions: https://jmespath.org/specification.html#built-in-functions
