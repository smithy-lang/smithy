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
   the behavior of querying these types is undefined and might produce
   an error.
.. [#ftimestamp] ``timestamp`` values are represented in JMESPath expressions
   as epoch seconds with optional decimal precision. This allows for
   timestamp values to be used with relative comparators like ``<`` and ``>``.
.. [#fstructure] Structure members are referred to by member name and not
   the data sent over the wire. For example, the :ref:`jsonname-trait` is not
   respected in JMESPath expressions that select structure members.
.. [#funion] ``union`` values are represented exactly like structures except
   only a single member is set to a non-null value.

.. _jmespath-contract-extensions:

------------------------------
Contract expression extensions
------------------------------

JMESPath_ on its own can only query a value. The :ref:`contract traits
<contract-traits>` also need to say things about an operation *call* and about
the state of resources before and after it, so Smithy extends the data model
and the function set available to contract expressions. These extensions are
defined by Smithy and are not part of the JMESPath specification. They are
available only in the expressions of contract traits, such as the
:ref:`contracts trait <contracts-trait>`. Other uses of JMESPath, such as
:ref:`waiters <waiters>`, cannot use them.

The pieces fit together as follows:

1. A *call* exposes ``input``, ``output``, ``error``, ``before`` and ``after``.
2. A named :ref:`references-trait` entry on a structure projects a resource
   *handle*, such as ``input.document``.
3. The ``resource`` function looks that handle up in the ``before`` or
   ``after`` snapshot and returns the resource's state.

Contract expressions are currently checked against :ref:`examples-trait`
values that provide ``before`` and ``after`` snapshots. See the :ref:`contracts
trait <contracts-trait>` for a complete example.

.. _jmespath-operation-call:

Operation calls
~~~~~~~~~~~~~~~

An operation has no value instance; it is a procedure. What a
:ref:`contract <contracts-trait>` constrains is a single *call*, exposed to
JMESPath as an object with the following members:

- ``input``: an instance of the operation's input shape.
- ``output``: an instance of the output shape on a successful call; absent on failure.
- ``error``: an object ``{shapeId, content}`` on a failed call; absent on success.
  ``output`` and ``error`` are mutually exclusive.
- ``before`` and ``after``: :ref:`world snapshots <world-snapshot>` immediately
  before and after the call. These are model-only (see `Model-only members`_).

Expressions reference these members, for example ``input.start < input.end``.

Model-only members
~~~~~~~~~~~~~~~~~~

The ``input``, ``output``, and ``error`` members are *observable*: they are the
data a client actually sends and receives. The ``before`` and ``after``
snapshots are *model-only*: they exist in the model to describe resource state
for reasoning, and are never present on the wire. Features that are evaluated
at runtime, such as waiters, may reference only the observable members.
Model-only members are available only to build-time analysis such as the
:ref:`conditions trait <conditions-trait>`.

Reference handles
~~~~~~~~~~~~~~~~~

Named :ref:`references-trait` entries are projected into the data model as
resource *handles* reachable as ``<member-root>.<name>`` (for example
``input.key``). The entry is declared either on a structure or directly on a
string shape used as one of its members. Each handle is an object
``{service, resource, ids}`` synthesized from the reference's bindings:
``resource`` is the shape ID of the referenced resource, and ``ids`` maps each
resource identifier name to its value. For a reference on a structure, ``ids``
comes from the reference's own ``ids`` mapping. When ``ids`` is
:ref:`omitted <implicit-ids>`, each identifier's value is the value of the
member of the same name. For a reference on a string shape, which has exactly one
identifier and no ``ids`` mapping, that identifier's value is the member's own
value. The handle mirrors the coordinates that locate the resource in a
:ref:`world snapshot <world-snapshot>`: the service shape, then the resource
shape, then the identifiers.

``service`` is present only when the reference declares one. When a handle has a ``service``, ``resource`` resolves against that service. When
it has none, ``resource`` requires the world snapshot to hold exactly one service
and resolves against it (see `Functions`_ below), so nothing has to guess a
"current service". In either case the chosen service holds at most one instance,
and resolving against one that holds more is an error. Addressing a specific
service or instance in a world that holds several (for cross-service references
such as ARNs) is future work.

A handle is model-only: it is a pointer synthesized from the observable
identifier members rather than data on the wire. The identifiers inside it are
observable, but the handle itself is not.


.. _jmespath-functions:

Functions
~~~~~~~~~

In addition to the `built-in JMESPath functions`_, Smithy defines the following
model-only functions for use in contract expressions.

``resource``
^^^^^^^^^^^^

::

    resource(world, handle) -> object

Looks up a single resource instance in a ``world`` snapshot (a ``before`` or
``after`` state) and returns it, or ``null`` when no instance matches. ``world``
is a :ref:`world snapshot <world-snapshot>`. ``handle`` is an object
``{service, resource, ids}`` where ``resource`` is the resource shape ID, ``ids``
maps each resource identifier name to its value, and ``service`` is an optional
service shape ID; a named :ref:`references-trait` projects such a handle
automatically (see `Reference handles`_).

When the handle has a ``service``, ``resource`` resolves against that named
service. When it has none, the ``world`` must hold exactly one service: resolving
a world that holds no service returns ``null``, and resolving one that holds more
than one service is an error. The chosen service holds at most one instance: the
function returns ``null`` if it has no instance, and it is an error if it has more
than one. Within that instance's ``resources`` member, the instance of the
``resource`` shape whose ``identifiers`` match the handle's ``ids`` is returned,
or ``null`` when none matches. The returned resource instance is an object with
``identifiers`` and ``properties`` members, so a property is read through
``properties`` (for example
``resource(before, input.document).properties.status``).

``requires``
^^^^^^^^^^^^

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
