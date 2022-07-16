.. _aws-cloudformation-traits:

=========================
AWS CloudFormation traits
=========================

CloudFormation traits are used to describe Smithy resources and their
components so they can be converted to `CloudFormation Resource Schemas`_.

.. _aws-cloudformation-overview:

`CloudFormation Resource Schemas`_ are the standard method of `modeling a
resource provider`_ for use within CloudFormation. Smithy's modeled
:ref:`resources <resource>`, utilizing the traits below, can generate these
schemas. Automatically generating schemas from a service's API lowers the
effort needed to generate and maintain them, reduces the potential for errors
in the translation, and provides a more complete depiction of a resource in its
schema. These schemas can be utilized by the `CloudFormation Command Line
Interface`_ to build, register, and deploy `resource providers`_.


.. smithy-trait:: aws.cloudformation#cfnResource
.. _aws.cloudformation#cfnResource-trait:

----------------------------------------
``aws.cloudformation#cfnResource`` trait
----------------------------------------

Summary
    Indicates that a Smithy resource is a CloudFormation resource.
Trait selector
    ``resource``
Value type
    ``structure``

The ``aws.cloudformation#cfnResource`` trait is a structure that
supports the following members:

.. list-table::
    :header-rows: 1
    :widths:  10 20 70

    * - Property
      - Type
      - Description
    * - name
      - ``string``
      - Provides a custom CloudFormation resource name. Defaults to the
        :ref:`shape name of the shape ID <shape-id>` of the targeted
        ``resource`` when generating CloudFormation resource schemas.
    * - additionalSchemas
      - ``list<shapeId>``
      - A list of additional :ref:`shape IDs <shape-id>` of structures that
        will have their properties added to the CloudFormation resource.
        Members of these structures with the same names MUST resolve to the
        same target. See :ref:`aws-cloudformation-property-deriviation` for
        more information.

The following example defines a simple resource that is also a CloudFormation
resource:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.cloudformation#cfnResource

        @cfnResource
        resource Foo {
            identifiers: {
                fooId: String,
            },
        }


Resources that have properties that cannot be :ref:`automatically derived
<aws-cloudformation-property-deriviation>` can use the ``additionalSchemas``
trait property to include them. This is useful if interacting with a resource
requires calling non-lifecycle APIs or if some of the resource's properties
cannot be automatically converted to CloudFormation properties.

The following example provides a ``name`` value and one structure shape in the
``additionalSchemas`` list.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        use aws.cloudformation#cfnResource

        @cfnResource(
            name: "Foo",
            additionalSchemas: [AdditionalFooProperties])
        resource FooResource {
            identifiers: {
                fooId: String,
            },
        }

        structure AdditionalFooProperties {
            barProperty: String,
        }


.. _aws-cloudformation-property-deriviation:

Resource properties
===================

Smithy will automatically derive `property`__ information for resources with
the ``@aws.cloudformation#cfnResource`` trait applied.

A resource's properties include the :ref:`resource's identifiers <resource-identifiers>`
as well as the top level members of the resource's ``read`` operation output
structure, ``put`` operation input structure, ``create`` operation input
structure, ``update`` operation input structure, and any structures listed in
the ``@cfnResource`` trait's ``additionalSchemas`` property. Members
of these structures can be excluded by applying the :ref:`aws.cloudformation#cfnExcludeProperty-trait`.

.. __: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-properties

.. important::

    Any members used to derive properties that are defined in more than one of
    the above structures MUST resolve to the same target.

.. seealso::

    Refer to :ref:`property mutability <aws-cloudformation-mutability-derivation>`
    for more information on how the CloudFormation mutability of a property is
    derived.


.. smithy-trait:: aws.cloudformation#cfnExcludeProperty
.. _aws.cloudformation#cfnExcludeProperty-trait:

-----------------------------------------------
``aws.cloudformation#cfnExcludeProperty`` trait
-----------------------------------------------

Summary
    Indicates that structure member should not be included as a `property`__ in
    generated CloudFormation resource definitions.
Trait selector
    ``structure > member``

    *Any structure member*
Value type
    Annotation trait
Conflicts with
    :ref:`aws.cloudformation#cfnAdditionalIdentifier-trait`,
    :ref:`aws.cloudformation#cfnMutability-trait`

.. __: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-properties

The ``cfnExcludeProperty`` trait omits a member of a Smithy structure from the
:ref:`derived resource properties <aws-cloudformation-property-deriviation>` of
a CloudFormation resource.

The following example defines a CloudFormation resource that excludes the
``responseCode`` property:

.. code-block:: smithy

    namespace smithy.example

    use aws.cloudformation#cfnExcludeProperty
    use aws.cloudformation#cfnResource

    @cfnResource
    resource Foo {
        identifiers: {
            fooId: String,
        },
        read: GetFoo,
    }

    @readonly
    @http(method: "GET", uri: "/foos/{fooId}", code: 200)
    operation GetFoo {
        input: GetFooRequest,
        output: GetFooResponse,
    }

    @input
    structure GetFooRequest {
        @httpLabel
        @required
        fooId: String,
    }

    @output
    structure GetFooResponse {
        fooId: String,

        @httpResponseCode
        @cfnExcludeProperty
        responseCode: Integer,
    }


.. _aws-cloudformation-mutability-derivation:

-------------------
Property mutability
-------------------

Any property derived for a resource will have its mutability automatically
derived as well. CloudFormation resource properties can have the following
mutability settings:

* **Full** - Properties that can be specified when creating, updating, or
  reading a resource.
* **Create Only** - Properties that can be specified only during resource
  creation and can be returned in a ``read`` or ``list`` request.
* **Read Only** - Properties that can be returned by a ``read`` or ``list``
  request, but cannot be set by the user.
* **Write Only** - Properties that can be specified by the user, but cannot be
  returned by a ``read`` or ``list`` request.
* **Create and Write Only** - Properties that can be specified only during
  resource creation and cannot be returned in a ``read`` or ``list`` request.

Given the following model without mutability traits applied,

.. code-block:: smithy

    namespace smithy.example

    use aws.cloudformation#cfnResource

    @cfnResource
    resource Foo {
        identifiers: {
            fooId: String,
        },
        create: CreateFoo,
        read: GetFoo,
        update: UpdateFoo,
    }

    operation CreateFoo {
        input: CreateFooRequest,
        output: CreateFooResponse,
    }

    @input
    structure CreateFooRequest {
        createProperty: ComplexProperty,
        mutableProperty: ComplexProperty,
        writeProperty: ComplexProperty,
        createWriteProperty: ComplexProperty,
    }

    @output
    structure CreateFooResponse {
        fooId: String,
    }

    @readonly
    operation GetFoo {
        input: GetFooRequest,
        output: GetFooResponse,
    }

    @input
    structure GetFooRequest {
        @required
        fooId: String,
    }

    @output
    structure GetFooResponse {
        fooId: String,
        createProperty: ComplexProperty,
        mutableProperty: ComplexProperty,
        readProperty: ComplexProperty,
    }

    @idempotent
    operation UpdateFoo {
        input: UpdateFooRequest,
    }

    @input
    structure UpdateFooRequest {
        @required
        fooId: String,

        mutableProperty: ComplexProperty,
        writeProperty: ComplexProperty,
    }

    structure ComplexProperty {
        anotherProperty: String,
    }

The computed resource property mutabilities are:

.. list-table::
    :header-rows: 1
    :widths: 20 20 60

    * - Name
      - CloudFormation Mutability
      - Reasoning
    * - ``fooId``
      - Read only
      - + Returned in the ``read`` lifecycle via ``GetFooResponse``.
    * - ``createProperty``
      - Create only
      - + Specified in the ``create`` lifecycle via ``CreateFooRequest``.
        + Returned in the ``read`` lifecycle via ``GetFooResponse``.
    * - ``mutableProperty``
      - Full
      - + Specified in the ``create`` lifecycle via ``CreateFooRequest``.
        + Returned in the ``read`` lifecycle via ``GetFooResponse``.
        + Specified in the ``update`` lifecycle via ``UpdateFooRequest``.
    * - ``readProperty``
      - Read only
      - + Returned in the ``read`` lifecycle via ``GetFooResponse``.
    * - ``writeProperty``
      - Write only
      - + Specified in the ``update`` lifecycle via ``UpdateFooRequest``.
    * - ``createWriteProperty``
      - Create and write only
      - + Specified in the ``create`` lifecycle via ``CreateFooRequest``.


.. smithy-trait:: aws.cloudformation#cfnMutability
.. _aws.cloudformation#cfnMutability-trait:

------------------------------------------
``aws.cloudformation#cfnMutability`` trait
------------------------------------------

Summary
    Indicates an explicit CloudFormation mutability of the structure member
    when part of a CloudFormation resource.
Trait selector
    ``structure > member``

    *Any structure member*
Value type
    ``string`` that MUST be set to "full", "create", "create-and-read", "read",
    or "write".
Conflicts with
    :ref:`aws.cloudformation#cfnExcludeProperty-trait`

The ``cfnMutability`` trait overrides any :ref:`derived mutability setting
<aws-cloudformation-mutability-derivation>` on a member. The values of the
mutability trait have the following meanings:

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - Value
      - Description
    * - ``full``
      - Indicates that the CloudFormation property generated from this member
        can be specified by the user on ``create`` and ``update`` and can be
        returned in a ``read`` or ``list`` request.
    * - ``create``
      - Indicates that the CloudFormation property generated from this member
        can be specified only during resource creation and cannot returned in a
        ``read`` or ``list`` request. This is equivalent to having both `create
        only`_ and `write only`_ CloudFormation mutability.
    * - ``create-and-read``
      - Indicates that the CloudFormation property generated from this member
        can be specified only during resource creation and can be returned in a
        ``read`` or ``list`` request. This is equivalent to `create only`_
        CloudFormation mutability.
    * - ``read``
      - Indicates that the CloudFormation property generated from this member
        can be returned by a ``read`` or ``list`` request, but cannot be set by
        the user. This is equivalent to `read only`_ CloudFormation mutability.
    * - ``write``
      - Indicates that the CloudFormation property generated from this member
        can be specified by the user, but cannot be returned by a ``read`` or
        ``list`` request. MUST NOT be set if the member is also marked with the
        :ref:`aws.cloudformation#cfnAdditionalIdentifier-trait`. This is
        equivalent to `write only`_ CloudFormation mutability.

The following example defines a CloudFormation resource that marks the ``tags``
and ``barProperty`` properties as fully mutable:

.. code-block:: smithy

    namespace smithy.example

    use aws.cloudformation#cfnMutability
    use aws.cloudformation#cfnResource

    @cfnResource(additionalSchemas: [FooProperties])
    resource Foo {
        identifiers: {
            fooId: String,
        },
        create: CreateFoo,
    }

    operation CreateFoo {
        input: CreateFooRequest,
        output: CreateFooResponse,
    }

    @input
    structure CreateFooRequest {
        @cfnMutability("full")
        tags: TagList,
    }

    @output
    structure CreateFooResponse {
        fooId: String,
    }

    structure FooProperties {
        @cfnMutability("full")
        barProperty: String,
    }


The following example defines a CloudFormation resource that marks the
``immutableSetting`` property as create and read only:

.. code-block:: smithy

    namespace smithy.example

    use aws.cloudformation#cfnMutability
    use aws.cloudformation#cfnResource

    @cfnResource(additionalSchemas: [FooProperties])
    resource Foo {
        identifiers: {
            fooId: String,
        },
    }

    structure FooProperties {
        @cfnMutability("create-and-read")
        immutableSetting: Boolean,
    }


The following example defines a CloudFormation resource that marks the
``updatedAt`` and ``createdAt`` properties as read only:

.. code-block:: smithy

    namespace smithy.example

    use aws.cloudformation#cfnMutability
    use aws.cloudformation#cfnResource

    @cfnResource(additionalSchemas: [FooProperties])
    resource Foo {
        identifiers: {
            fooId: String,
        },
        read: GetFoo,
    }

    @readonly
    operation GetFoo {
        input: GetFooRequest,
        output: GetFooResponse,
    }

    @input
    structure GetFooRequest {
        @required
        fooId: String
    }

    @output
    structure GetFooResponse {
        @cfnMutability("read")
        updatedAt: Timestamp,
    }

    structure FooProperties {
        @cfnMutability("read")
        createdAt: Timestamp,
    }


The following example defines a CloudFormation resource that marks the
derivable ``secret`` and ``password`` properties as write only:

.. code-block:: smithy

    namespace smithy.example

    use aws.cloudformation#cfnMutability
    use aws.cloudformation#cfnResource

    @cfnResource(additionalSchemas: [FooProperties])
    resource Foo {
        identifiers: {
            fooId: String,
        },
        create: CreateFoo,
    }

    operation CreateFoo {
        input: CreateFooRequest,
        output: CreateFooResponse,
    }

    @input
    structure CreateFooRequest {
        @cfnMutability("write")
        secret: String,
    }

    @output
    structure CreateFooResponse {
        fooId: String,
    }

    structure FooProperties {
        @cfnMutability("write")
        password: String,
    }


.. smithy-trait:: aws.cloudformation#cfnName
.. _aws.cloudformation#cfnName-trait:

------------------------------------
``aws.cloudformation#cfnName`` trait
------------------------------------

Summary
    Allows a CloudFormation `resource property`__ name to differ from a
    structure member name used in the model.
Trait selector
    ``structure > member``

    *Any structure member*
Value type
    ``string``

.. __: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-properties

Given the following structure definition:

.. code-block:: smithy

    namespace smithy.example

    use aws.cloudformation#cfnName

    structure AdditionalFooProperties {
        bar: String,

        @cfnName("Tags")
        tagList: TagList,
    }

the following property names are derived from it:

::

    "bar"
    "Tags"


.. smithy-trait:: aws.cloudformation#cfnAdditionalIdentifier
.. _aws.cloudformation#cfnAdditionalIdentifier-trait:

----------------------------------------------------
``aws.cloudformation#cfnAdditionalIdentifier`` trait
----------------------------------------------------

Summary
    Indicates that the CloudFormation property generated from this member is an
    `additional identifier`__ for the resource.
Trait selector
    ``structure > :test(member > string)``

    *Any structure member that targets a string*
Value type
    Annotation trait
Validation
    The ``cfnAdditionalIdentifier`` trait MUST NOT be applied to members with
    the :ref:`aws.cloudformation#cfnMutability-trait` set to ``write`` or
    ``create``.

.. __: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-cfnAdditionalIdentifiers

Each ``cfnAdditionalIdentifier`` uniquely identifies an instance of the
CloudFormation resource it is a part of. This is useful for resources that
provide identifier aliases (for example, a resource might accept an ARN or
customer provided alias in addition to its unique ID.)

``cfnAdditionalIdentifier`` traits are ignored when applied outside of the
input to an operation bound to the ``read`` lifecycle of a resource.

The following example defines a CloudFormation resource that has the
``fooAlias`` property as an additional identifier:

.. code-block:: smithy

    namespace smithy.example

    use aws.cloudformation#cfnAdditionalIdentifier
    use aws.cloudformation#cfnResource

    @cfnResource
    resource Foo {
        identifiers: {
            fooId: String,
        },
        read: GetFoo,
    }

    @readonly
    operation GetFoo {
        input: GetFooRequest,
    }

    @input
    structure GetFooRequest {
        @required
        fooId: String,

        @cfnAdditionalIdentifier
        fooAlias: String,
    }


.. smithy-trait:: aws.cloudformation#cfnDefaultValue
.. _aws.cloudformation#cfnDefaultValue-trait:

--------------------------------------------
``aws.cloudformation#cfnDefaultValue`` trait
--------------------------------------------

Summary
    Indicates that the member annotated has a default value for the resource.
Trait selector
    ``resource > operation -[output]-> structure > member``

    *Only applicable to members of ``@output`` operations*
Value type
    Annotation trait

Given the following example, because the ``fooAlias``
member is annotated with ``cfnDefaultValue``, it can be derived
that the ``fooAlias`` member has a default value for this resource.

.. code-block:: smithy

    namespace smithy.example

    use aws.cloudformation#cfnDefaultValue
    use aws.cloudformation#cfnResource

    @cfnResource
    resource Foo {
        identifiers: {
            fooId: String,
        },
        read: GetFoo,
    }

    @readonly
    @http(method: "GET", uri: "/foos/{fooId}", code: 200)
    operation GetFoo {
        input: GetFooRequest,
        output: GetFooResponse,
    }

    @input
    structure GetFooRequest {
        @httpLabel
        @required
        fooId: String,

        fooAlias: String,
    }

    @output
    structure GetFooResponse {
        fooId: String,

        @cfnDefaultValue
        fooAlias: String,

        @httpResponseCode
        responseCode: Integer,
    }


-------------
Example model
-------------

The above traits and behaviors culminate in the ability to generate
`CloudFormation Resource Schemas`_ from a Smithy model. The following example
model utilizes all of these traits to express how a complex Smithy resource
can be annotated for CloudFormation resource generation.

Given the following model,

.. code-block:: smithy

    namespace smithy.example

    use aws.cloudformation#cfnDefaultValue
    use aws.cloudformation#cfnAdditionalIdentifier
    use aws.cloudformation#cfnExcludeProperty
    use aws.cloudformation#cfnMutability
    use aws.cloudformation#cfnResource

    @cfnResource(additionalSchemas: [FooProperties])
    resource Foo {
        identifiers: {
            fooId: String,
        },
        create: CreateFoo,
        read: GetFoo,
        update: UpdateFoo,
    }

    @http(method: "POST", uri: "/foos", code: 200)
    operation CreateFoo {
        input: CreateFooRequest,
        output: CreateFooResponse,
    }

    @input
    structure CreateFooRequest {
        @cfnMutability("full")
        tags: TagList,

        @cfnMutability("write")
        secret: String,

        fooAlias: String,

        createProperty: ComplexProperty,
        mutableProperty: ComplexProperty,
        writeProperty: ComplexProperty,
        createWriteProperty: ComplexProperty,
    }

    @output
    structure CreateFooResponse {
        fooId: String,
    }

    @readonly
    @http(method: "GET", uri: "/foos/{fooId}", code: 200)
    operation GetFoo {
        input: GetFooRequest,
        output: GetFooResponse,
    }

    @input
    structure GetFooRequest {
        @httpLabel
        @required
        fooId: String,

        @httpQuery("fooAlias")
        @cfnAdditionalIdentifier
        fooAlias: String,
    }

    @output
    structure GetFooResponse {
        fooId: String,

        @httpResponseCode
        @cfnExcludeProperty
        responseCode: Integer,

        @cfnMutability("read")
        updatedAt: Timestamp,

        @cfnDefaultValue
        fooAlias: String,
        createProperty: ComplexProperty,
        mutableProperty: ComplexProperty,
        readProperty: ComplexProperty,
    }

    @idempotent
    @http(method: "PUT", uri: "/foos/{fooId}", code: 200)
    operation UpdateFoo {
        input: UpdateFooRequest,
    }

    @input
    structure UpdateFooRequest {
        @httpLabel
        @required
        fooId: String,

        fooAlias: String,
        mutableProperty: ComplexProperty,
        writeProperty: ComplexProperty,
    }

    structure FooProperties {
        addedProperty: String,

        @cfnMutability("full")
        barProperty: String,

        @cfnName("Immutable")
        @cfnMutability("create-and-read")
        immutableSetting: Boolean,

        @cfnMutability("read")
        createdAt: Timestamp,

        @cfnMutability("write")
        password: String,
    }

    structure ComplexProperty {
        anotherProperty: String,
    }

    list TagList {
        member: String
    }

The following CloudFormation resource information is computed:

.. list-table::
    :header-rows: 1
    :widths: 20 20 60

    * - Name
      - CloudFormation Mutability
      - Reasoning
    * - ``addedProperty``
      - Full
      - + Default mutability in ``FooProperties`` via ``additionalSchemas``.
    * - ``barProperty``
      - Full
      - + ``@cfnMutability`` trait specified in ``FooProperties`` via
          ``additionalSchemas``.
    * - ``createProperty``
      - Create only
      - + Specified in the ``create`` lifecycle via ``CreateFooRequest``.
        + Returned in the ``read`` lifecycle via ``GetFooResponse``.=
    * - ``createWriteProperty``
      - Create and write only
      - + Specified in the ``create`` lifecycle via ``CreateFooRequest``.
    * - ``createdAt``
      - Read only
      - + ``@cfnMutability`` trait specified in ``FooProperties`` via
          ``additionalSchemas``.
    * - ``fooAlias``
      - Full + additional identifier
      - + Specified in the ``create`` lifecycle via ``CreateFooRequest``.
        + Returned in the ``read`` lifecycle via ``GetFooResponse``.
        + Specified in the ``update`` lifecycle via ``UpdateFooRequest``.
        + ``@cfnAdditionalIdentifier`` trait specified in ``GetFooRequest``.
    * - ``fooId``
      - Read only + primary identifier
      - + Returned in the ``read`` lifecycle via ``GetFooResponse``.
    * - ``Immutable`` from ``immutableSetting``
      - Create only
      - + ``@cfnMutability`` trait specified in ``FooProperties`` via
          ``additionalSchemas``.
    * - ``mutableProperty``
      - Full
      - + Specified in the ``create`` lifecycle via ``CreateFooRequest``.
        + Returned in the ``read`` lifecycle via ``GetFooResponse``.
        + Specified in the ``update`` lifecycle via ``UpdateFooRequest``.
    * - ``password``
      - Write only
      - + ``@cfnMutability`` trait specified in ``FooProperties`` via
          ``additionalSchemas``.
    * - ``readProperty``
      - Read only
      - + Returned in the ``read`` lifecycle via ``GetFooResponse``.
    * - ``responseCode``
      - None
      - + ``@cfnExcludeProperty`` trait specified in ``GetFooResponse``.
    * - ``secret``
      - Write only
      - + ``@cfnMutability`` trait specified in ``CreateFooRequest``.
    * - ``tags``
      - Full
      - + ``@cfnMutability`` trait specified in ``CreateFooRequest``.
    * - ``updatedAt``
      - Read only
      - + ``@cfnMutability`` trait specified in ``GetFooResponse``.
    * - ``writeProperty``
      - Write only
      - + Specified in the ``create`` lifecycle via ``CreateFooRequest``.
        + Specified in the ``update`` lifecycle via ``UpdateFooRequest``.


.. _CloudFormation Resource Schemas: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html
.. _modeling a resource provider: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-types.html
.. _develop the resource provider: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-develop.html
.. _CloudFormation Command Line Interface: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/what-is-cloudformation-cli.html
.. _resource providers: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-types.html
.. _create only: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-createonlyproperties
.. _write only: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-writeonlyproperties
.. _read only: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-readonlyproperties
