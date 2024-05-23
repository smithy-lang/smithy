.. _service-types:

-------------
Service types
-------------

*Service types* have specific semantics and define services, resources,
and operations.


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
        map value MUST match the :token:`smithy:Identifier` production used for
        shape IDs. Renaming a shape *does not* give the shape a new shape ID.

        * No renamed shape name can case-insensitively match any other renamed
          shape name or the name of a non-renamed shape contained in the
          service.
        * Member shapes MAY NOT be renamed.
        * Resource and operation shapes MAY NOT be renamed. Renaming shapes is intended
          for incidental naming conflicts, not for renaming the fundamental concepts
          of a service.
        * Shapes from other namespaces marked as :ref:`private <private-trait>`
          MAY be renamed.
        * A rename MUST use a name that is case-sensitively different from the
          original shape ID name.

The following example defines a service with no operations or resources.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    service MyService {
        version: "2017-02-11"
    }

The following example defines a service shape that defines a set of errors
that are common to every operation in the service:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    service MyService {
        version: "2017-02-11",
        errors: [SomeError]
    }

    @error("client")
    structure SomeError {}


.. _service-operations:

Service operations
------------------

:ref:`Operation <operation>` shapes can be bound to a service by adding the
shape ID of an operation to the ``operations`` property of a service.
Operations bound directly to a service are typically RPC-style operations
that do not fit within a resource hierarchy.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    service MyService {
        version: "2017-02-11"
        operations: [GetServerTime]
    }

    @readonly
    operation GetServerTime {
        output: GetServerTimeOutput
    }


.. _service-resources:

Service resources
-----------------

:ref:`Resource <resource>` shapes can be bound to a service by adding the
shape ID of a resource to the ``resources`` property of a service.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    service MyService {
        version: "2017-02-11"
        resources: [MyResource]
    }

    resource MyResource {}


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

.. important::

    Resources and operations can only be bound once. An operation or resource
    MUST NOT be bound to multiple shapes within the closure of a service. This
    constraint allows services to discern between operations and resources
    using only their shape name rather than a fully-qualified path from the
    service to the shape.

.. note::

    Undeclared operation inputs and outputs are not a part of the service
    closure. :ref:`smithy.api#Unit <unit-type>` is the shape that is implicitly
    targeted by operation inputs and outputs when they are not explicitly
    declared. This does not, however, add ``smithy.api#Unit`` to the service's
    closure, and does not require renaming to avoid conflicts with other shapes
    named ``Unit``. Unions in the service closure with members targeting
    ``smithy.api#Unit``, however, will cause ``smithy.api#Unit`` to be a part
    of the service closure.

Shape types allowed to conflict in a closure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

:ref:`Simple types <simple-types>` and :ref:`lists <list>` of compatible simple
types are allowed to conflict because a conflict for these type would rarely
have an impact on generated artifacts. These kinds of conflicts are only
allowed if both conflicting shapes are the same type and have the exact same
traits. In the case of a list, a conflict is only allowed if the members
of the conflicting shapes target compatible shapes.

Disambiguating shapes with ``rename``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    service MyService {
        version: "2017-02-11"
        operations: [GetSomething]
        rename: {
            "foo.example#Widget": "FooWidget"
        }
    }

    operation GetSomething {
        input: GetSomethingInput
        output: GetSomethingOutput
    }

    @input
    structure GetSomethingInput {}

    @output
    structure GetSomethingOutput {
        widget1: Widget
        fooWidget: foo.example#Widget
    }

    structure Widget {}


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
      - The input of the operation defined using a :ref:`shape ID <shape-id>`
        that MUST target a structure.

        - Every operation SHOULD define a dedicated input shape marked with
          the :ref:`input-trait`. Creating a dedicated input shape ensures
          that input members can be added in the future if needed.
        - Input defaults to :ref:`smithy.api#Unit <unit-type>` if no input is
          defined, indicating that the operation has no meaningful input.
    * - output
      - ``string``
      - The output of the operation defined using a :ref:`shape ID <shape-id>`
        that MUST target a structure.

        * Every operation SHOULD define a dedicated output shape marked with
          the :ref:`output-trait`. Creating a dedicated output shape ensures
          that output members can be added in the future if needed.
        * Output defaults to :ref:`smithy.api#Unit <unit-type>` if no output
          is defined, indicating that the operation has no meaningful output.
    * - errors
      - [``string``]
      - The errors that an operation can return. Each string in the list is
        a shape ID that MUST target a :ref:`structure <structure>` shape
        marked with the :ref:`error-trait`.

The following example defines an operation that accepts an input structure
named ``MyOperationInput``, returns an output structure named
``MyOperationOutput``, and can potentially return the ``NotFound`` or
``BadRequest`` :ref:`error structures <error-trait>`.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    operation MyOperation {
        input: MyOperationInput
        output: MyOperationOutput
        errors: [NotFoundError, BadRequestError]
    }

    @input
    structure MyOperationInput {}

    @output
    structure MyOperationOutput {}

While, input and output SHOULD be explicitly defined for every operation,
omitting them is allowed. The default value for input and output is
``smithy.api#Unit``, indicating that there is no meaningful value.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    operation MySideEffectOperation {}

The following example is equivalent, but more explicit in intent:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    operation MySideEffectOperation {
        input: Unit,
        output: Unit
    }

.. warning::

    Using the ``Unit`` shape for input or output removes flexibility in how an
    operation can evolve over time because members cannot be added to the
    input or output if ever needed.


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
    * - :ref:`properties <resource-properties>`
      - ``object``
      - Defines a map of property string names to :ref:`shape-id`\s that
        enumerate the properties of the resource.
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
--------------------

:dfn:`Identifiers` are used to refer to a specific resource within a service.
The identifiers property of a resource is a map of identifier names to
:ref:`shape IDs <shape-id>` that MUST target string shapes.

For example, the following model defines a ``Forecast`` resource with a
single identifier named ``forecastId`` that targets the ``ForecastId`` shape:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    resource Forecast {
        identifiers: { forecastId: ForecastId }
    }

    string ForecastId

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

.. code-block:: smithy

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

``ResourceB`` is a valid child of ``ResourceA`` and contains a child
identifier of "b". ``ResourceC`` is a valid child of ``ResourceB`` and
contains a child identifier of "c".

However, the following defines two *invalid* child resources that do not
define an ``identifiers`` property that is compatible with their parents:

.. code-block:: smithy

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


.. _binding-identifiers:

Binding identifiers to operations
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*Identifier bindings* indicate which top-level members of the input or output
structure of an operation provide values for the identifiers of a resource.

Identifier binding validation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- Child resources MUST provide identifier bindings for all of its parent's
  identifiers.
- Identifier bindings are only formed on input or output structure members that
  are marked as :ref:`required <required-trait>`.
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
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*Implicit identifier bindings* are formed when the input of an operation
contains member names that target the same shapes that are defined in the
"identifiers" property of the resource to which an operation is bound.

For example, given the following model,

.. code-block:: smithy

    resource Forecast {
        identifiers: {
            forecastId: ForecastId
        }
        read: GetForecast
    }

    @readonly
    operation GetForecast {
        input: GetForecastInput
        output: GetForecastOutput
    }

    @input
    structure GetForecastInput for Forecast {
        @required
        $forecastId
    }

    @output
    structure GetForecastOutput {
        @required
        weather: WeatherData
    }

``GetForecast`` forms a valid instance operation because the operation is
not marked with the ``collection`` trait and ``GetForecastInput`` provides
*implicit identifier bindings* by defining a required "forecastId" member
that targets the same shape as the "forecastId" identifier of the resource.

.. seealso::

    The :ref:`target elision syntax <idl-target-elision>` for an easy way to
    define structures that reference resource identifiers and properties
    without having to repeat the target definition.

Implicit identifier bindings for collection operations are created in a
similar way to an instance operation, but MUST NOT contain identifier bindings
for *all* child identifiers of the resource.

Given the following model,

.. code-block:: smithy

    resource Forecast {
        identifiers: {
            forecastId: ForecastId
        }
        collectionOperations: [BatchPutForecasts]
    }

    operation BatchPutForecasts {
        input: BatchPutForecastsInput
        output: BatchPutForecastsOutput
    }

    @input
    structure BatchPutForecastsInput {
        @required
        forecasts: BatchPutForecastList
    }

``BatchPutForecasts`` forms a valid collection operation with implicit
identifier bindings because ``BatchPutForecastsInput`` does not require an
input member named "forecastId" that targets ``ForecastId``.


Explicit identifier bindings
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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

    @input
    structure GetHistoricalForecastInput for HistoricalForecast {
        @required
        @resourceIdentifier("forecastId")
        customForecastIdName: ForecastId

        @required
        @resourceIdentifier("historicalId")
        $customHistoricalIdName
    }

the :ref:`resourceIdentifier-trait` on ``GetHistoricalForecastInput$customForecastIdName``
maps it to the "forecastId" identifier is provided by the
"customForecastIdName" member, and the :ref:`resourceIdentifier-trait`
on ``GetHistoricalForecastInput$customHistoricalIdName`` maps that member
to the "historicalId" identifier.

If an operation input supplies both an explicit and an implicit identifier
binding, the explicit identifier binding is utilized.


.. _resource-properties:

Resource Properties
-------------------

:dfn:`Resource properties` represent the state of a resource within a service.
Properties can be referred to in the top level input and output shapes
of a resource's instance operations, including create, read, update,
delete, and put. All declared resource properties MUST appear in at
least one instance operation's input or output.

For example, the following model defines a ``Forecast`` resource with a
single property ``chanceOfRain`` read by the GetForecast operation, and the
output operation shape ``GetForecastOutput`` contains that output property.

.. code-block:: smithy

    $version: "2.0"
    namespace smithy.example

    resource Forecast {
        properties: { chanceOfRain: Float }
        read: GetForecast
    }

    @readonly
    operation GetForecast {
       output: GetForecastOutput
    }

    structure GetForecastOutput for Forecast {
        $chanceOfRain
    }

.. _binding-properties:

Binding members to properties
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*Property bindings* associate top-level members of input or output shapes
with resource properties. The match occurs through a match between member
name and property name by default.

.. code-block:: smithy

    $version: "2.0"
    namespace smithy.example

    resource Forecast {
        properties: { chanceOfRain: Float }
        read: GetForecast
    }

    @readonly
    operation GetForecast {
       output: GetForecastOutput
    }

    structure GetForecastOutput for Forecast {
        $chanceOfRain
    }

The :ref:`property-trait` is used to bind a member to
a resource property if the member name does not match the property name. This is
useful if the member name cannot be changed due backwards compatibility reasons,
but resource property modeling is being added to your Smithy model.

The following example demonstrates the ``howLikelyToRain`` member of
``GetForecastOutput`` can be bound to the ``chanceOfRain`` resource property:

.. code-block:: smithy

    resource Forecast {
        properties: { chanceOfRain: Float }
        read: GetForecast
    }

    @readonly
    operation GetForecast {
       output: GetForecastOutput
    }

    structure GetForecastOutput {
        @property(name: "chanceOfRain")
        howLikelyToRain: Float
    }

Though resource properties are usually bound to top level input and output
members, use the :ref:`nested-properties-trait` on a member to designate its
target structure shape as the root to form property bindings. No adjacent
members can form property bindings when this trait is applied.

Resource property binding validation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- All top-level input or output members must bind to a resource property unless
  marked with :ref:`notProperty-trait` or another trait with it applied, or one
  of the members is marked with :ref:`nested-properties-trait`
- Top-level members of the input and output of resource instance operations MUST
  only use properties that resolve to declared resource properties except for
  members marked with the ``@notProperty`` trait or marked with traits marked
  with the ``@notProperty`` trait.
- Defined resource properties that do not resolve to any top-level input or
  output members are invalid.
- Members that provide a value for a resource property but use a different
  target shape are invalid.
- Members marked with a :ref:`property-trait` using a name that does not map to
  a declared resource property are invalid.


.. _lifecycle-operations:

Resource lifecycle operations
-----------------------------

:dfn:`Lifecycle operations` are used to transition the state of a resource
using well-defined semantics. Lifecycle operations are defined by providing a
shape ID to the ``put``, ``create``, ``read``, ``update``, ``delete``, and
``list`` properties of a resource. Each shape ID MUST target an
:ref:`operation <operation>` that is compatible with the semantics of the
lifecycle.

The following example defines a resource with each lifecycle method:

.. code-block:: smithy

    $version: "2"
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
~~~~~~~~~~~~~

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

    @input
    structure PutForecastInput {
        // The client provides the resource identifier.
        @required
        forecastId: ForecastId

        chanceOfRain: Float
    }

Put semantics
^^^^^^^^^^^^^

The semantics of a ``put`` lifecycle operation are similar to the semantics
of a HTTP PUT method as described in :rfc:`section 4.3.4 of [RFC9110] <9110#section-9.3.4>`:

  The PUT method requests that the state of the target resource be
  created or replaced ...

The :ref:`noReplace-trait` can be applied to resources that define a
``put`` lifecycle operation to indicate that a resource cannot be
replaced using the ``put`` operation.


.. _create-lifecycle:

Create lifecycle
~~~~~~~~~~~~~~~~

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

    @input
    structure CreateForecastInput {
        // No identifier is provided by the client, so the service is
        // responsible for providing the identifier of the resource.
        chanceOfRain: Float
    }


.. _read-lifecycle:

Read lifecycle
~~~~~~~~~~~~~~

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

    @input
    structure GetForecastInput {
        @required
        forecastId: ForecastId
    }


.. _update-lifecycle:

Update lifecycle
~~~~~~~~~~~~~~~~

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

    @input
    structure UpdateForecastInput {
        @required
        forecastId: ForecastId

        chanceOfRain: Float
    }


.. _delete-lifecycle:

Delete lifecycle
~~~~~~~~~~~~~~~~

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

    @input
    structure DeleteForecastInput {
        @required
        forecastId: ForecastId
    }


.. _list-lifecycle:

List lifecycle
~~~~~~~~~~~~~~

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

    @input
    structure ListForecastsInput {
        maxResults: Integer
        nextToken: String
    }

    @output
    structure ListForecastsOutput {
        nextToken: String
        @required
        forecasts: ForecastList
    }

    list ForecastList {
        member: ForecastId
    }

.. _ubiquitous language: https://martinfowler.com/bliki/UbiquitousLanguage.html
.. _context map: https://martinfowler.com/bliki/BoundedContext.html
