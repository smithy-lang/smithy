.. _aws-iam_traits:

==============
AWS IAM traits
==============

IAM Policy Traits are used to describe the permission structure of a service
in relation to AWS IAM. Services integrated with AWS IAM define resource types,
actions, and condition keys that IAM users can use to construct IAM policies.

`Actions`_ and `resource types`_ are automatically inferred from a service
model via operations and resources, respectively. Actions can also be annotated
with other `actions that they depend on`_ to be invoked.

`Condition keys`_ are available for IAM users to define restrictions in IAM
policies for resources in a service. Condition keys for services defined in
Smithy are automatically inferred. These can be disabled or augmented. For
more information, see :ref:`deriving-condition-keys`.


.. _aws-iam_traits-principal:

----------------
Principal traits
----------------

.. smithy-trait:: aws.iam#supportedPrincipalTypes
.. _aws.iam#supportedPrincipalTypes-trait:

``aws.iam#supportedPrincipalTypes`` trait
=========================================

Summary
    The `IAM principal types`_ that can use the service or operation.
Trait selector
    ``:test(service, operation)``
Value type
    ``list<string>`` where each string is an IAM principal type: ``Root``,
    ``IAMUser``, ``IAMRole``, or ``FederatedUser``.

Operations that are not annotated with the ``supportedPrincipalTypes`` trait
inherit the ``supportedPrincipalTypes`` of the service they are bound to.

The following example defines two operations:

* ``OperationA`` defines an explicit list of the `IAM principal types`_ it
  supports using the ``supportedPrincipalTypes`` trait.
* ``OperationB`` is not annotated with the ``supportedPrincipalTypes`` trait,
  so the `IAM principal types`_ supported by this operation are the principal
  types applied to the service.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.iam#supportedPrincipalTypes

    @supportedPrincipalTypes(["Root", "IAMUser", "IAMRole", "FederatedUser"])
    service MyService {
        version: "2020-07-02"
        operations: [OperationA, OperationB]
    }

    @supportedPrincipalTypes(["Root"])
    operation OperationA {}

    operation OperationB {}


.. _aws-iam_traits-actions:

-------------
Action traits
-------------

.. smithy-trait:: aws.iam#iamAction
.. _aws.iam#iamAction-trait:

``aws.iam#iamAction`` trait
===========================

Summary
    Indicates properties of a Smithy operation in AWS IAM.
Trait selector
    ``operation``
Value type
    ``structure``

The ``aws.iam#iamAction`` trait is a structure that supports the following
members:

.. list-table::
    :header-rows: 1
    :widths:  10 20 70

    * - Property
      - Type
      - Description
    * - name
      - ``string``
      - The name of the resource in AWS IAM.
    * - documentation
      - ``string``
      - A brief description of what granting the user permission to invoke an
        operation would entail.
    * - relativeDocumentation
      - ``string``
      - A relative URL path that defines more information about the operation
        within a set of IAM-related documentation.
    * - requiredActions
      - ``list<string>`` where each string value is the name of another action.
      - The list of actions that the invoker must be authorized to perform when
        executing the targeted operation.
    * - resources
      - `ActionResources object`_
      - The resources an IAM action can be authorized against.
    * - createsResources
      - ``list<string>`` where each string value is the name of a resource.
      - The list of resources that performing this IAM action will create. If
        this member is present, all inferred created resources are ignored.

The following example defines a simple operation with a name in AWS IAM that
deviates from the :ref:`shape name of the shape ID <shape-id>` of the operation.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.iam#iamAction

    @iamAction(name: "PutEvent")
    operation OperationA {}


.. _aws.iam#iamAction-trait-ActionResources:

``ActionResources`` object
--------------------------

The ``ActionResources`` object is a container for information on the resources
that an IAM action may be authorized against. The ``ActionResources`` object
contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - required
      - ``map`` of resource name to `ActionResource object`_
      - Resources that will always be authorized against for functionality of
        the IAM action. If this member is present, all inferred required
        resources are ignored.
    * - optional
      - ``map`` of resource name to `ActionResource object`_
      - Resources that will be authorized against based on optional behavior of
        the IAM action. If this member is present, all inferred optional
        resources are ignored.

        For example, an action may create an instance that can optionally be
        configured based on a snapshot that would be authorized against. Most
        actions do not need this property.


.. _aws.iam#iamAction-trait-ActionResource:

``ActionResource`` object
-------------------------

The ``ActionResource`` object is a container for information about a resource
that an IAM action can be authorized against. The ``ActionResource`` object
contains the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - conditionKeys
      - ``list<string>``
      - The condition keys used for authorizing against this resource.


.. _aws-iam_traits-actions-deprecated-traits:

Deprecated action traits
========================

.. smithy-trait:: aws.iam#actionName
.. _aws.iam#actionName-trait:

``aws.iam#actionName`` trait
----------------------------

.. danger::
    This trait is deprecated. The ``name`` property of the
    :ref:`aws.iam#iamAction-trait` should be used instead.

Summary
    Provides a custom IAM action name.
Trait selector
    ``operation``
Value type
    ``string``

Operations not annotated with the ``actionName`` trait, default to the
:ref:`shape name of the shape ID <shape-id>` of the targeted operation.

The following example defines two operations:

* ``OperationA`` is not annotated with the ``actionName`` trait, and
  resolves the action name of ``OperationA``.
* ``OperationB`` has the ``actionName`` trait, so has the action
  name ``OverridingActionName``.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.iam#actionName

    service MyService {
        version: "2020-07-02"
        operations: [OperationA, OperationB]
    }

    operation OperationA {}

    @actionName("OverridingActionName")
    operation OperationB {}

.. smithy-trait:: aws.iam#actionPermissionDescription
.. _aws.iam#actionPermissionDescription-trait:

``aws.iam#actionPermissionDescription`` trait
---------------------------------------------

.. danger::
    This trait is deprecated. The ``documentation`` property of the
    :ref:`aws.iam#iamAction-trait` should be used instead.

Summary
    A brief description of what granting the user permission to invoke an
    operation would entail.
Trait selector
    ``operation``
Value type
    ``string``

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.iam#actionPermissionDescription

    @actionPermissionDescription("This will allow the user to Foo.")
    operation FooOperation {}


.. smithy-trait:: aws.iam#requiredActions
.. _aws.iam#requiredActions-trait:

``aws.iam#requiredActions`` trait
---------------------------------

.. danger::
    This trait is deprecated. The ``requiredActions`` property of the
    :ref:`aws.iam#iamAction-trait` should be used instead.

Summary
    Other actions that the invoker must be authorized to perform when
    executing the targeted operation.
Trait selector
    ``operation``
Value type
    ``list<string>`` where each string value references other actions
    required for the service to authorize.

Defines the actions, in addition to the targeted operation, that a user must
be authorized to execute in order invoke an operation. The following example
indicates that, in order to invoke the ``MyOperation`` operation, the invoker
must also be authorized to execute the ``otherservice:OtherOperation``
operation for it to complete successfully.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.api#service
    use aws.iam#requiredActions

    @service(sdkId: "My Value", arnNamespace: "myservice")
    service MyService {
        version: "2017-02-11"
        resources: [MyResource]
    }

    resource MyResource {
        identifiers: {foo: String}
        operations: [MyOperation]
    }

    @requiredActions(["otherservice:OtherOperation"])
    operation MyOperation {}

.. _aws-iam_traits-resources:

---------------
Resource Traits
---------------

.. smithy-trait:: aws.iam#iamResource
.. _aws.iam#iamResource-trait:

``aws.iam#iamResource`` trait
=============================

Summary
    Indicates properties of a Smithy resource in AWS IAM.
Trait selector
    ``resource``
Value type
    ``structure``

The ``aws.iam#iamResource`` trait is a structure that supports the following
members:

.. list-table::
    :header-rows: 1
    :widths:  10 20 70

    * - Property
      - Type
      - Description
    * - name
      - ``string``
      - The name of the resource in AWS IAM.
    * - relativeDocumentation
      - ``string``
      - A relative URL path that defines more information about the resource
        within a set of IAM-related documentation.
    * - disableConditionKeyInheritance
      - ``boolean``
      - When set to ``true``, decouples this IAM resource's condition keys from
        those of its parent resource(s). This can be used in combination with
        the :ref:`aws.iam#conditionKeys-trait` trait to isolate a resource's
        condition keys from those of its parent(s).

The following example defines a simple resource with a name in AWS IAM that
deviates from the :ref:`shape name of the shape ID <shape-id>` of the resource.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.iam#iamResource

    @iamResource(name: "super")
    resource SuperResource {
        identifiers: {
            superId: String,
        }
    }



.. _aws-iam_traits-condition-keys:

--------------------
Condition key traits
--------------------

.. smithy-trait:: aws.iam#defineConditionKeys
.. _aws.iam#defineConditionKeys-trait:

``aws.iam#defineConditionKeys`` trait
=====================================

Summary
    Defines the set of condition keys that appear within a service in
    addition to :ref:`inferred <deriving-condition-keys>` and global condition
    keys.
Trait selector
    ``service``
Value type
    ``map`` of IAM identifiers to condition key ``structure``

The ``aws.iam#defineConditionKeys`` trait defines additional condition keys
that appear within a service. Keys in the map must be valid IAM identifiers,
meaning they must adhere to the following regular expression:
``"^([A-Za-z0-9][A-Za-z0-9-\\.]{0,62}:[^:]+)$"``.
Each condition key structure supports the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - type
      - ``string``
      - **Required**. The type of contents of the condition key. The type must
        be one of: ``ARN``, ``Binary``, ``Bool``, ``Date``, ``IPAddress``,
        ``Numeric``, ``String``, ``ArrayOfARN``, ``ArrayOfBinary``,
        ``ArrayOfBool``, ``ArrayOfDate``, ``ArrayOfIPAddress``,
        ``ArrayOfNumeric``, ``ArrayOfString``. See :ref:`condition-key-types`
        for more information.
    * - documentation
      - ``string``
      - Defines documentation about the condition key.
    * - externalDocumentation
      - ``string``
      - A valid URL that defines more information about the condition key.
    * - relativeDocumentation
      - ``string``
      - A relative URL path that defines more information about the condition key
        within a set of IAM-related documentation.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.api#service
    use aws.iam#defineConditionKeys

    @service(sdkId: "My Value", arnNamespace: "myservice")
    @defineConditionKeys(
        "myservice:Bar": {
            type: "String"
            documentation: "The Bar string"
            externalDocumentation: "http://example.com"
        })
    service MyService {
        version: "2017-02-11"
        resources: [MyResource]
    }

.. note::

    Condition keys that refer to global ``"aws:*"`` keys are allowed to not be
    defined on the service.



.. smithy-trait:: aws.iam#conditionKeys
.. _aws.iam#conditionKeys-trait:

``aws.iam#conditionKeys`` trait
===============================

Summary
    Applies condition keys, by name, to a resource or operation.
Trait selector
    ``:test(resource, operation)``
Value type
    ``list<string>``

Condition keys derived automatically can be applied to a resource or operation
explicitly. Condition keys applied this way MUST be either :ref:`inferred <deriving-condition-keys>`
or explicitly defined via the :ref:`aws.iam#defineConditionKeys-trait` trait.

The following example's ``MyResource`` resource has the
``myservice:MyResourceFoo`` and  ``myservice:Bar`` condition keys. The
``MyOperation`` operation has the ``aws:region`` condition key.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.api#service
    use aws.iam#definedContextKeys
    use aws.iam#conditionKeys

    @service(sdkId: "My Value", arnNamespace: "myservice")
    @defineConditionKeys("myservice:Bar": { type: "String" })
    service MyService {
        version: "2017-02-11"
        resources: [MyResource]
    }

    @conditionKeys(["myservice:Bar"])
    resource MyResource {
        identifiers: {foo: String}
        operations: [MyOperation]
    }

    @conditionKeys(["aws:region"])
    operation MyOperation {}

.. note::

    Condition keys that refer to global ``"aws:*"`` keys can be referenced
    without being defined on the service.



.. smithy-trait:: aws.iam#serviceResolvedConditionKeys
.. _aws.iam#serviceResolvedConditionKeys-trait:

``aws.iam#serviceResolvedConditionKeys`` trait
==============================================

Summary
    Specifies the list of IAM condition keys which must be resolved by the
    service, as opposed to the value being pulled from the request.
Trait selector
    ``service``
Value type
    ``list<string>``

All condition keys defined with the ``serviceResolvedConditionKeys`` trait
MUST also be defined via the :ref:`aws.iam#defineConditionKeys-trait` trait.
:ref:`Inferred resource condition keys <deriving-condition-keys>` MUST NOT be
included with the ``serviceResolvedConditionKeys`` trait.

The following example defines two service-specific condition keys:

* ``myservice:ActionContextKey1`` is expected to be resolved by the service.
* ``myservice:ActionContextKey2`` is expected to be pulled from the request.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    @defineConditionKeys(
        "myservice:ActionContextKey1": { type: "String" },
        "myservice:ActionContextKey2": { type: "String" }
    )
    @serviceResolvedConditionKeys(["myservice:ActionContextKey1"])
    @service(sdkId: "My Value", arnNamespace: "myservice")
    service MyService {
        version: "2018-05-10"
    }


.. smithy-trait:: aws.iam#conditionKeyValue
.. _aws.iam#conditionKeyValue-trait:

``aws.iam#conditionKeyValue`` trait
===================================

Summary
    Uses the associated member’s value for the specified condition key.
Trait selector
    ``member``
Value type
    ``string``

Members not annotated with the ``conditionKeyValue`` trait, default to the
:ref:`shape name of the shape ID <shape-id>` of the targeted member. All
condition keys defined with the ``conditionKeyValue`` trait MUST also be
defined via the :ref:`aws.iam#defineConditionKeys-trait` trait.

In the input shape for ``OperationA``, the trait ``conditionKeyValue``
explicitly binds ``ActionContextKey1`` to the field ``key``.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    @defineConditionKeys(
        "myservice:ActionContextKey1": { type: "String" }
    )
    @service(sdkId: "My Value", arnNamespace: "myservice")
    service MyService {
        version: "2020-07-02"
        operations: [OperationA]
    }

    @conditionKeys(["myservice:ActionContextKey1"])
    operation OperationA {
        input := {
            @conditionKeyValue("ActionContextKey1")
            key: String
        }
        output := {
            out: String
        }
    }


.. smithy-trait:: aws.iam#disableConditionKeyInference
.. _aws.iam#disableConditionKeyInference-trait:

``aws.iam#disableConditionKeyInference`` trait
==============================================

Summary
    Declares that the condition keys of a resource should not be
    :ref:`inferred <deriving-condition-keys>`.
Trait selector
    ``:test(service, resource)``
Value type
    Annotation trait

When a service is marked with the ``aws.iam#disableConditionKeyInference``
trait, all the resources bound to the service will not have condition
keys automatically inferred from its identifiers and the identifiers
of its ancestors.

The following example shows resources ``MyResource1`` and ``MyResource2``
have had condition key inference disabled because they are bound to a
service marked with ``aws.iam#disableConditionKeyInference`` trait.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.api#service
    use aws.iam#disableConditionKeyInference

    @service(sdkId: "My Value", arnNamespace: "myservice")
    @disableConditionKeyInference
    service MyService {
        version: "2017-02-11"
        resources: [MyResource1, MyResource2]
    }

    resource MyResource1 {
        identifiers: {
            foo: String
        }
    }

    resource MyResource2 {
        identifiers: {
            foo: String
        }
    }

A resource marked with the ``aws.iam#disableConditionKeyInference`` trait will
not have its condition keys automatically inferred from its identifiers and
the identifiers of its ancestors. if present.

The following example shows a resource, ``MyResource``, that has condition key
inference disabled.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.api#service
    use aws.iam#disableConditionKeyInference

    @service(sdkId: "My Value", arnNamespace: "myservice")
    service MyService {
        version: "2017-02-11"
        resources: [MyResource]
    }

    @disableConditionKeyInference
    resource MyResource {
        identifiers: {
            foo: String
            bar: String
        }
    }


.. _condition-key-types:

Condition Key Types
=======================

The following table describes the available types a condition key can have.
Condition keys in IAM policies can be evaluated with `condition operators`_.

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - Type
      - Description
    * - ``ARN``
      - A String type that contains an `Amazon Resource Name (ARN)`_.
    * - ``Binary``
      - A String type that contains base-64 encoded binary data.
    * - ``Bool``
      - A general boolean type.
    * - ``Date``
      - A String type that conforms to the ``datetime`` profile of `ISO 8601`_.
    * - ``IPAddress``
      - A String type that conforms to :rfc:`4632`.
    * - ``Numeric``
      - A general type for integers and floats.
    * - ``String``
      - A general string type.
    * - ``ArrayOfARN``
      - An unordered list of ARN types.
    * - ``ArrayOfBinary``
      - An unordered list of Binary types.
    * - ``ArrayOfBool``
      - An unordered list of Bool types.
    * - ``ArrayOfDate``
      - An unordered list of Date types.
    * - ``ArrayOfIPAddress``
      - An unordered list of IPAddress types.
    * - ``ArrayOfNumeric``
      - An unordered list of Numeric types.
    * - ``ArrayOfString``
      - An unordered list of String types.

.. _deriving-condition-keys:

Deriving condition keys
=======================

Smithy will automatically derive condition key information for a service, as
well as its resources and operations.

A resource's condition keys include those that are inferred from their
identifiers, including the resource's ancestors, and those applied via the
:ref:`aws.iam#conditionKeys-trait` trait. Condition keys for resource
identifiers are automatically inferred unless explicitly configured not to via
the :ref:`aws.iam#disableConditionKeyInference-trait` trait.

An action's condition keys, including for actions for operations bound to
resources, are only derived from those applied via the :ref:`aws.iam#conditionKeys-trait`
trait.

Given the following model,

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.api#service
    use aws.iam#defineConditionKeys
    use aws.iam#conditionKeys
    use aws.iam#iamResource

    @service(sdkId: "My Value", arnNamespace: "myservice")
    @defineConditionKeys("myservice:Bar": { type: "String" })
    service MyService {
        version: "2017-02-11"
        resources: [MyResource]
    }

    @conditionKeys(["myservice:Bar"])
    resource MyResource {
        identifiers: {foo: String}
        operations: [MyOperation]
        resources: [MyInnerResource, MyDetachedResource, MyCustomResource]
    }

    @iamResource(name: "InnerResource")
    resource MyInnerResource {
        identifiers: {yum: String}
    }

    @disableConditionKeyInference
    @iamResource(disableConditionKeyInheritance: true)
    resource MyDetachedResource {
        identifiers: {yum: String}
    }

    @disableConditionKeyInference
    @iamResource(disableConditionKeyInheritance: true)
    @conditionKeys(["aws:region"])
    resource MyCustomResource {
        identifiers: {yum: String}
    }

    @conditionKeys(["aws:region"])
    operation MyOperation {}

The computed condition keys for the service are:

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - Name
      - Condition Keys
    * - ``MyResource``
      -
          * ``myservice:MyResourceFoo``
          * ``myservice:Bar``
    * - ``InnerResource``
      -
          * ``myservice:MyResourceFoo``
          * ``myservice:Bar``
          * ``myservice:InnerResourceYum``
    * - ``MyDetachedResource``
      - None
    * - ``MyCustomResource``
      -
          * ``aws:region``
    * - ``MyOperation``
      -
          * ``aws:region``

.. _AWS Identity and Access Management: https://aws.amazon.com/iam/
.. _Condition keys: https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_condition-keys.html
.. _Actions: https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_action.html
.. _resource types: https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_resource.html
.. _actions that they depend on: https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_actions-resources-contextkeys.html
.. _condition operators: https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_condition_operators.html
.. _Amazon Resource Name (ARN): https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html
.. _ISO 8601: http://www.w3.org/TR/NOTE-datetime
.. _IAM principal types: https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html
