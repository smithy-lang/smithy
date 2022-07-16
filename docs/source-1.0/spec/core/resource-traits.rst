===============
Resource traits
===============

Resource traits augment resources and resource operation semantics.

.. smithy-trait:: smithy.api#noReplace
.. _noReplace-trait:

-------------------
``noReplace`` trait
-------------------

Summary
    Indicates that the :ref:`put lifecycle <put-lifecycle>` operation of a
    resource can only be used to create a resource and cannot replace an
    existing resource.
Trait selector
    ``resource:test(-[put]->)``

    *A resource with a put lifecycle operation*
Value type
    Annotation trait.

By default, ``put`` lifecycle operations are assumed to both create and
replace an existing resource. Some APIs, however, do not support this
behavior and require that a resource is first deleted before it can be
replaced.

For example, this is the behavior of Amazon DynamoDB's CreateTable_
operation. The "Table" resource identifier, "TableName", is provided by the
client, making it appropriate to model in Smithy as a ``put`` lifecycle
operation. However, ``UpdateTable`` is used to update a table and attempting
to call ``CreateTable`` on a table that already exists will return an error.

.. tabs::

    .. code-tab:: smithy

        @noReplace
        resource Table {
            put: CreateTable
        }

        @idempotent
        operation CreateTable {
            // ...
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#Table": {
                    "type": "resource",
                    "put": {
                        "target": "smithy.example#CreateTable"
                    },
                    "traits": {
                        "smithy.api#noReplace": {}
                    }
                },
                "smithy.example#CreateTable": {
                    "type": "operation",
                    "traits": {
                        "smithy.api#idempotent": {}
                    }
                }
            }
        }


.. smithy-trait:: smithy.api#references
.. _references-trait:

--------------------
``references`` trait
--------------------

Summary
    Defines a design-time reference to :ref:`resource` shapes. Resource
    references allow tooling to understand the relationships between
    resources and how to dereference the location of a resource.
Trait selector
    ``:is(structure, string)``

    *Any structure or string*
Value type
    ``list`` of ``Reference`` structures

.. rubric:: ``Reference`` structure

The ``references`` trait is a list of ``Reference`` structures that contain
the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 23 67

    * - Property
      - Type
      - Description
    * - service
      - :ref:`shape-id`
      - The absolute shape ID of the service to which the resource is bound.
        As with the ``resource`` property, the provided shape ID is not
        required to be resolvable at build time.
    * - resource
      - :ref:`shape-id`
      - **Required**. The absolute shape ID of the referenced resource.

        The provided shape ID is not required to be part of the model;
        references may refer to resources in other models without directly
        depending on the external package in which the resource is defined.
        The reference will not be resolvable at build time but MAY be resolvable
        at runtime if the tool has loaded more than one model.
    * - ids
      - ``map<string, string>``
      - Defines a mapping of each resource identifier name to a structure
        member name that provides its value. Each key in the map MUST refer
        to one of the identifier names in the identifiers property of the
        resource, and each value in the map MUST refer to a valid structure
        member name that targets a string shape.

        - This property MUST be omitted if the ``references`` trait is applied
          to a string shape.
        - This property MAY be omitted if the identifiers of the resource
          can be :ref:`mapped implicitly <implicit-ids>`.
    * - rel
      - ``string``
      - Defines the semantics of the relationship. The ``rel`` property SHOULD
        contain a link relation as defined in :rfc:`5988#section-4` (i.e.,
        this value SHOULD contain either a `standard link relation`_ or URI).

.. rubric:: Runtime resolution of references

References MAY NOT be resolvable at runtime in the following circumstances:

#. The members that make up the ``ids`` are not present in a structure at
   runtime (e.g., a member is not marked as :ref:`required-trait`)
#. The targeted resource and/or service shape is not part of the model
#. The reference is bound to a specific service that is unknown to the tool

.. rubric:: Implicit identifier mappings example

The following example creates a reference to a ``HistoricalForecast`` resource
(a resource that requires the "forecastId" and "historicalId" identifiers):

.. code-block:: smithy

    namespace smithy.example

    resource HistoricalForecast {
        identifiers: {
            forecastId: ForecastId,
            historicalId: HistoricalForecastId,
        }
    }

    @references([{resource: HistoricalForecast}])
    structure HistoricalReference {
        forecastId: ForecastId,
        historicalId: HistoricalForecastId
    }

Notice that in the above example, the identifiers of the resource were not
explicitly mapped to structure members. This is because the targeted structure
contains members with names that match the names of the identifiers of the
``HistoricalForecast`` resource.

.. rubric:: Explicit identifier mappings example

Explicit mappings between identifier names and structure member names can be
defined if needed. For example:

.. code-block:: smithy

    namespace smithy.example

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

.. rubric:: Additional examples

The following example defines several references:

.. tabs::

    .. code-tab:: smithy

        @references([
            {resource: Forecast},
            {resource: ShapeName},
            {resource: Meteorologist},
            {
                resource: com.foo.baz#Object,
                service: com.foo.baz#Service,
                ids: {bucket: "bucketName", object: "objectKey"},
            ])
        structure ForecastInformation {
            someId: SomeShapeIdentifier,

            @required
            forecastId: ForecastId,

            @required
            meteorologistId: MeteorologistId,

            @required
            otherData: SomeOtherShape,

            @required
            bucketName: BucketName,

            @required
            objectKey: ObjectKey,
        }

.. rubric:: References on string shapes

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


.. _implicit-ids:

Implicit ids
============

The "ids" property of a reference MAY be omitted in any of the following
conditions:

1. The shape that the references trait is applied to is a string shape.
2. The shape that the references trait is applied to is a structure shape
   and all of the identifier names of the resource have corresponding member
   names that target string shapes.


.. smithy-trait:: smithy.api#resourceIdentifier
.. _resourceIdentifier-trait:

----------------------------
``resourceIdentifier`` trait
----------------------------

Summary
    Indicates that the targeted structure member provides an identifier for a
    resource.
Trait selector
    ``structure > :test(member[trait|required] > string)``

    *Any required member of a structure that targets a string*
Value type
    ``string``

The ``resourceIdentifier`` trait may only be used on members of structures that
serve as input shapes for operations bound to resources. The string value
provided must correspond to the name of an identifier for said resource. The
trait is not required when the name of the input structure member is an exact
match for the name of the resource identifier.

.. code-block:: smithy

    resource File {
        identifiers: {
            directory: "String",
            fileName: "String",
        },
        read: GetFile,
    }

    @readonly
    operation GetFile {
        input: GetFileInput,
        output: GetFileOutput,
        errors: [NoSuchResource]
    }

    @input
    structure GetFileInput {
        @required
        directory: String,

        // resourceIdentifier is used because the input member name
        // does not match the resource identifier name
        @resourceIdentifier("fileName")
        @required
        name: String,
    }


.. _CreateTable: https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_CreateTable.html
.. _standard link relation: https://www.iana.org/assignments/link-relations/link-relations.xhtml
