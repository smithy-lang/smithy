======================
AWS Core Specification
======================

Various AWS-specific traits are used to integrate Smithy models with other
AWS products like AWS CloudFormation and tools like the AWS SDKs.

.. smithy-trait:: aws.api#service
.. _aws.api#service-trait:

-------------------------
``aws.api#service`` trait
-------------------------

Summary
    An AWS service is defined using the ``aws.api#service`` trait. This
    trait provides information about the service like the name used to
    generate AWS SDK client classes and the namespace used in ARNs.
Trait selector
    ``service``
Value type
    ``structure`` that contains the following members:

    * :ref:`service-sdk-id` (required)
    * :ref:`service-cloudformation-name`
    * :ref:`service-arn-namespace`
    * :ref:`service-cloudtrail-event-source`
    * :ref:`service-doc-id`
    * :ref:`service-endpoint-prefix`

The following example defines an AWS service that uses the default values of
``cloudFormationService``, ``arnNamespace``, ``cloudTrailEventSource`` and
``docId``:

.. code-block:: smithy

    $version: "2"

    namespace aws.fooBaz

    use aws.api#service

    @service(sdkId: "Some Value")
    service FooBaz {
        version: "2018-03-17"
    }

The following example provides explicit values for all properties:

.. code-block:: smithy

    $version: "2"

    namespace aws.fooBaz

    use aws.api#service

    @service(
        sdkId: "Some Value"
        cloudFormationName: "FooBaz"
        arnNamespace: "myservice"
        cloudTrailEventSource: "myservice.amazon.aws"
        docId: "some-value-2018-03-17"
        endpointPrefix: "my-endpoint"
    )
    service FooBaz {
        version: "2018-03-17"
    }


.. _service-sdk-id:

``sdkId``
=========

The ``sdkId`` property is a **required** ``string`` value that specifies
the AWS SDK service ID (e.g., "API Gateway"). This value is used for
generating client names in SDKs and for linking between services.

* The value MUST be unique across all AWS services.
* The value must match the following regex: ``^[a-zA-Z][a-zA-Z0-9]*( [a-zA-Z0-9]+)*$``.
  To summarize, the value can only contain alphanumeric characters and spaces. However,
  the first character cannot be a number, and when using spaces, each space must be
  between two alphanumeric characters.
* The value MUST NOT contain "AWS", "Aws", or "Amazon".
* The value SHOULD NOT case-insensitively end with "API", "Client", or "Service".
* The value MUST NOT change once a service is publicly released. If the value
  does change, the service will be considered a brand new service in the AWS SDKs
  and Tools.


Choosing an SDK service ID
--------------------------

The ``sdkId`` value should reasonably represent the service it identifies. ``sdkId``
MUST NOT be an arbitrary value; for example for Amazon DynamoDB, an appropriate
"serviceId" would be "DynamoDB" while an inappropriate value would be "Foo".

The following steps can be taken to produce a ``sdkId`` that should generally work
for most services:

1. Pick a base to derive the "sdkId". If an official abbreviation for a service
   is available, then use that as the base. An example of an official service
   abbreviation is ``Amazon S3`` for ``Amazon Simple Storage Service``.
   If the service has no official service abbreviation, then use the service's
   official name as specified by the :ref:`title-trait` (for example,
   ``Amazon Simple Storage Service``).
2. Remove "Service", "Client", and "API" from the end of the base string.
   The only acceptable reason for including these in the base is if one of
   those words are actually part of the official name of a service.
3. Remove any use of AWS or Amazon from the base.
4. Strip off any leading or trailing whitespace.
5. Remove any characters that are not alphanumeric or spaces.
6. Remove any leading digits until the value begins with a letter.

See :ref:`aws-service-appendix-a` for a table containing various AWS services
and their SDK service IDs.


Using SDK service ID for client naming
--------------------------------------

Unless explicitly overridden though other traits or configuration, AWS SDKs
SHOULD use the ``sdkId`` property when choosing the name of a client class.

For the AWS CLI, the typical value need to use a command involves both
lower-casing all characters of the ``sdkId`` and removing all spaces. So
for the "serviceId" of "API Gateway", the CLI command would be:

::

    $ aws apigateway

In the AWS SDK for PHP, a client class name SHOULD be derived by lower-casing
all letters that are not the first letter of a word, removing all spaces, and
appending the word "Client" to the final transformed "serviceId". So for the
"serviceId" of "API Gateway", the PHP client would be: ``ApiGatewayClient``.

Other AWS SDKs SHOULD follow a similar pattern when choosing client names.

**Note**:
For backwards compatibility reasons, some services will include "service" or "API" as a suffix.
New SDK major versions SHOULD strip ``service`` and ``api`` suffixes from ``sdkId`` when generating
a client name.

.. _service-cloudformation-name:

``cloudFormationName``
======================

The ``cloudFormationName`` property is a ``string`` value that specifies
the `AWS CloudFormation service name`_ (e.g., ``ApiGateway``). When not set,
this value defaults to the name of the service shape. This value is part of
the CloudFormation resource type name that is automatically assigned to
resources in the service (e.g., ``AWS::<NAME>::resourceName``). This value
must match the following regex: ``^[A-Z][A-Za-z0-9]+$``.


.. _service-arn-namespace:

``arnNamespace``
================

The ``arnNamespace`` property is a ``string`` value that defines the
`ARN service namespace`_ of the service (e.g., "apigateway"). This value is
used in ARNs assigned to resources in the service. If not set, this value
defaults to the lowercase name of the service shape. This value must match
the following regex: ``^[a-z0-9.\-]{1,63}$``.

If not set, this value defaults to the name of the service shape converted
to lowercase. This value is combined with resources contained within the
service to form ARNs for resources. Only resources that explicitly define
the :ref:`aws.api#arn-trait` are assigned ARNs, and their relative ARNs
are combined with the service's arnNamespace to form an ARN.


.. _service-cloudtrail-event-source:

``cloudTrailEventSource``
=========================

The ``cloudTrailEventSource`` property is a ``string`` value that defines the
AWS customer-facing *eventSource* property contained in CloudTrail
`event records`_ emitted by the service. If not specified, this value defaults
to the ``arnNamespace`` plus .amazonaws.com. For example:

* AWS CloudFormation has an ``arnNamespace`` of ``cloudformation`` and an
  event source of ``cloudformation.amazonaws.com``.
* Amazon EC2 has an ``arnNamespace`` of ``ec2`` and an event source of
  ``ec2.amazonaws.com``.
* Amazon Simple Workflow Service has an ``arnNamespace`` of ``swf`` and
  an event source of ``swf.amazonaws.com``.

This value SHOULD follow the convention of ``{arnNamespace}.amazonaws.com``,
but there are some exceptions. For example, the event source for
Amazon CloudWatch is ``monitoring.amazonaws.com``. Such services will
need to explicitly configure the ``cloudTrailEventSource`` setting.


.. _service-doc-id:

``docId``
===========================

The ``docId`` property is a ``string`` value that is used to implement linking
between service and SDK documentation for AWS services.

This will default to the ``sdkId`` value in lower case followed by the service
``version`` property, separated by dashes. For the example below, the value
for this property would default to ``some-value-2018-03-17``.

.. code-block:: smithy

    $version: "2"

    namespace aws.fooBaz

    use aws.api#service

    @service(sdkId: "Some Value")
    service FooBaz {
        version: "2018-03-17"
    }


.. _service-endpoint-prefix:

``endpointPrefix``
==================

The ``endpointPrefix`` property is a ``string`` value that identifies which endpoint
in a given region should be used to connect to the service. For example, most
services in the AWS standard partition have endpoints which follow the format:
``{endpointPrefix}.{region}.amazonaws.com``. A service with the endpoint prefix
``example`` in the region ``us-west-2`` might have the endpoint
``example.us-west-2.amazonaws.com``. For a full listing of possible endpoints,
check the `AWS Regions and Endpoints`_ page.

This value is not unique across services and is subject to change. Therefore,
it MUST NOT be used for client naming or for any other purpose that requires
a static, unique identifier. :ref:`service-sdk-id` should be used for those
purposes. Additionally, this value can be used to attempt to resolve endpoints.


.. smithy-trait:: aws.api#arn
.. _aws.api#arn-trait:

---------------------
``aws.api#arn`` trait
---------------------

Trait summary
    Defines an ARN of a Smithy resource shape.
Trait selector
    ``resource``
Trait value
    ``structure``

The ``aws.api#arn`` trait is a structure that supports the following
members:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - template
      - ``string``
      - **Required** Defines the ARN template. The provided string contains
        URI-template style label placeholders that contain the name of one of
        the identifiers defined in the ``identifiers`` property of the
        resource. These labels can be substituted at runtime with the actual
        identifiers of the resource. Every identifier name of the resource
        MUST have corresponding label of the same name. Note that
        percent-encoding **is not** performed on these placeholder values;
        they are to be replaced literally. For relative ARN templates that
        have not set ``absolute`` to ``true``, the template string contains
        only the resource part of the ARN (for example, ``foo/{MyResourceId}``).
        Relative ARNs MUST NOT start with "/".
    * - noRegion
      - ``boolean``
      - Set to true to specify that the ARN does not contain a region.
        If not set, or if set to false, the resolved ARN will contain a
        placeholder for the region. This can only be set to true if
        ``absolute`` is not set or is false.
    * - noAccount
      - ``boolean``
      - Set to true to specify that the ARN does not contain an account ID.
        If not set, or if set to false, the resolved ARN will contain a
        placeholder for the customer account ID. This can only be set to
        true if absolute is not set or is false.
    * - absolute
      - ``boolean``
      - Set to true to indicate that the ARN template contains a fully-formed
        ARN that does not need to be merged with the service. This type of
        ARN MUST be used when the identifier of a resource is an ARN or is
        based on the ARN identifier of a parent resource.


Format of an ARN
================

An ARN is a structured URI made up of the following components:

.. code-block:: none
    :class: no-copybutton

    arn:partition:service:region:account-id:resource
                             \       /
                         Both are optional

partition
    The partition that the resource is in. For standard AWS regions, the
    partition is "aws". If you have resources in other partitions, the
    partition is aws-partitionname. For example, the partition for resources
    in the China (Beijing) region is aws-cn.
service
    The service namespace that identifies the AWS product (for example,
    Amazon S3 is "s3", IAM is "iam", and Amazon RDS is "rds"). For a list
    of namespaces, see `AWS Service Namespaces`_. The namespace used by
    Smithy services is defined by the ``arnNamespace`` property of the
    :ref:`aws.api#service-trait`.
region
    The region the resource resides in. Note that the ARNs for some resources
    do not require a region, so this component MAY be omitted.
account-id
    The ID of the AWS account that owns the resource, without the hyphens. For
    example, ``123456789012``. Note that the ARNs for some resources don't
    require an account number, so this component MAY be omitted.
resource
    Defines a specific resource within a service. The content of this segment of
    an ARN varies by service. It often includes an indicator of the type of
    resource—for example, an IAM user or Amazon RDS database —followed by a
    slash (/) or a colon (:), followed by the resource name itself. Some
    services allow paths for resource names, as described in Paths in ARNs.

Some example ARNs from various services include:

.. code-block:: none
    :class: no-copybutton

    // Elastic Beanstalk application version
    arn:aws:elasticbeanstalk:us-east-1:123456789012:environment/My App/MyEnvironment

    // IAM user name
    arn:aws:iam::123456789012:user/David

    // Amazon RDS instance used for tagging
    arn:aws:rds:eu-west-1:123456789012:db:mysql-db

    // Object in an Amazon S3 bucket
    arn:aws:s3:::my_corporate_bucket/exampleobject.png


Relative ARN templates
======================

``arn`` traits with relative templates are combined with the service to form an
absolute ARN template. This ARN template can only be expanded at runtime with
actual values for the partition, region name, account ID, and identifier
label placeholders.

For example, given the following service:

.. code-block:: smithy

    $version: "2"

    namespace aws.fooBaz

    use aws.api#service
    use aws.api#arn

    @service(sdkId: "Some Value")
    service FooBaz {
        version: "2018-03-17"
        resources: [MyResource]
    }

    @arn(template: "myresource/{myId}")
    resource MyResource {
        identifiers: {myId: MyResourceId}
    }

The ARN template assigned to ``MyResource`` when used with the ``FooBaz``
service expands to ``arn:{AWS::partition}:myservice:{AWS::Region}:{AWS::AccountId}:myresource/{myId}``
at runtime. The label ``{myId}`` indicates that the value of the resource's
identifier is to be inserted into the ARN template when resolving it at
runtime.


Using an ARN as a resource identifier
=====================================

*Absolute* ARN templates are used to provide an entire ARN to a resource that
is not combined with the service ARN namespace. When a resource uses an ARN as
its identifier, an absolute ARN template MUST be defined on the resource
that uses a placeholder containing the name of the identifier of the
resource.

.. code-block:: smithy

    use aws.api#arn
    use aws.api#arnReference

    @arn(template: "{arn}", absolute: true)
    resource MyResource {
        identifiers: {arn: Arn}
    }

    @arnReference(service: FooBaz, resource: MyResource)
    string Arn


.. smithy-trait:: aws.api#arnReference
.. _aws.api#arnReference-trait:

------------------------------
``aws.api#arnReference`` trait
------------------------------

Trait summary
    Specifies that a string shape contains a fully formed AWS ARN.
Trait selector
    ``string``
Trait value
    ``structure``

Smithy models can refer to AWS resources using ARNs. The
``aws.api#arnReference`` can be applied to a string shape to indicate
that the string contains an ARN and what resource is targeted by the
ARN.

The ``aws.api#arnReference`` trait is a structure that supports the following
optional members:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - type
      - ``string``
      -  The AWS `CloudFormation resource type`_ contained in the ARN.
         Example: "AWS::IAM::Role"
    * - service
      - ``string``
      - The Smithy service absolute shape ID that is referenced by the ARN.
        The targeted service is not required to be found in the model,
        allowing for external shapes to be referenced without needing to
        take on an additional dependency.
    * - resource
      - ``string``
      - An absolute shape ID that references the Smithy resource type
        contained in the ARN (e.g., ``com.foo#SomeResource``). The targeted
        resource is not required to be found in the model, allowing for
        external shapes to be referenced without needing to take on an
        additional dependency. If the shape is found in the model, it MUST
        target a resource shape, and the resource MUST be found within the
        closure of the referenced service shape.

The following example defines a string shape that targets an AWS resource.
The CloudFormation name of the resource and the Smithy service and resource
shape IDs are provided to give tooling additional information about the
referenced resource.

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.api#arnReference

    @arnReference(
        type: "AWS::SomeService::SomeResource"
        service: com.foo#SomeService
        resource: com.foo#SomeResource)
    string SomeResourceId

The following example defines an ARN reference that doesn't provide an context
about the referenced shape. While this is valid, it is not as useful as the
previous example:

.. code-block:: smithy

    $version: "2"

    namespace smithy.example

    use aws.api#arnReference

    @arnReference
    string SomeResourceId


.. smithy-trait:: aws.api#data
.. _aws.api#data-trait:

----------------------
``aws.api#data`` trait
----------------------

Summary
    Indicates that the target contains data of the specified classification.
Trait selector
    ``:test(simpleType, list, structure, union, member)``
Value type
    ``string`` that is one of: ``content``, ``account``, ``usage``,
    ``tagging``, or ``permissions``. See :ref:`data-classifications` for more
    information.

Data classifications are resolved hierarchically: the data classification
of a member inherits the effective data classification applied to a parent
structure, union, or list unless overridden.

.. code-block:: smithy

    use aws.api#data

    @data("permissions")
    structure MyStructure {
        name: String

        @data("content")
        content: String

        tags: TagList
    }

    @data("tagging")
    list TagList {
        member: String
    }

The effective data classifications in the previous example are as follows:

.. list-table::
    :header-rows: 1
    :widths: 40 60

    * - Shape ID
      - Data Classification
    * - ``smithy.example#MyStructure``
      - "permissions"
    * - ``smithy.example#MyStructure$name``
      - "permissions"
    * - ``smithy.example#MyStructure$content``
      - "content"
    * - ``smithy.example#MyStructure$tags``
      - "tagging"
    * - ``smithy.example#TagList``
      - "tagging"

.. note::

    This trait should be used in conjunction with the
    :ref:`sensitive-trait` as necessary.


.. _data-classifications:

Data Classifications
====================

The following table describes the available data classifications that can be
applied through the ``aws.api#data`` trait.

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - Type
      - Description
    * - ``content``
      - Designates software (including machine images), data, text, audio,
        video or images that customers or any customer end user transfers to
        AWS for processing, storage or hosting by AWS services in connection
        with the customer’s accounts and any computational results that
        customers or any customer end user derive from the foregoing through
        their use of AWS services. Data of this classification should be marked
        with the :ref:`sensitive-trait`.
    * - ``account``
      - Designates information about customers that customers provide to AWS in
        connection with the creation or administration of customers’ accounts.
        Data of this classification should be marked with the :ref:`sensitive-trait`.
    * - ``usage``
      - Designates data related to a customer’s account, such as resource
        identifiers, usage statistics, logging data, and analytics.
    * - ``tagging``
      - Designates metadata tags applied to AWS resources.
    * - ``permissions``
      - Designates security and access roles, rules, usage policies, and
        permissions. Data of this classification should be marked with the
        :ref:`sensitive-trait`.


.. smithy-trait:: aws.api#controlPlane
.. _aws.api#controlPlane-trait:

------------------------------
``aws.api#controlPlane`` trait
------------------------------

Summary
    Indicates that a service, resource, or operation is considered part of
    the *control plane*.
Trait selector
    ``:test(service, resource, operation)``
Value type
    Annotation trait
Conflicts with
    :ref:`aws.api#dataPlane-trait`

This trait is effectively inherited by shapes bound within a service or
resource. When applied to a service or resource shape, all resources and
operations bound within the shape are also considered part of the control
plane unless an operation or resource is marked with the
:ref:`aws.api#dataPlane-trait`.

.. code-block:: smithy

    use aws.api#controlPlane

    @controlPlane
    operation PutThings {
        input: PutThingsInput
        output: PutThingsOutput
    }


.. smithy-trait:: aws.api#dataPlane
.. _aws.api#dataPlane-trait:

---------------------------
``aws.api#dataPlane`` trait
---------------------------

Summary
    Indicates that a service, resource, or operation is considered part of
    the *data plane*.
Trait selector
    ``:test(service, resource, operation)``
Value type
    Annotation trait
Conflicts with
    :ref:`aws.api#controlPlane-trait`

This trait is effectively inherited by shapes bound within a service or
resource. When applied to a service or resource shape, all resources and
operations bound within the shape are also considered part of the data
plane unless an operation or resource is marked with the
:ref:`aws.api#controlPlane-trait`.

.. code-block:: smithy

    use aws.api#dataPlane

    @dataPlane
    operation PutThings {
        input: PutThingsInput
        output: PutThingsOutput
    }


.. _endpoint-discovery:

-------------------------
Client Endpoint Discovery
-------------------------

Services running on cellular infrastructure may wish to enable automatic
endpoint discovery in clients. The AWS SDKs provide functionality to
automatically discover, cache, and connect to service endpoints. The
following traits provide the information needed to perform this.


.. smithy-trait:: aws.api#clientEndpointDiscovery
.. _client-endpoint-discovery-trait:

``aws.api#clientEndpointDiscovery`` trait
=========================================

Trait summary
    The ``clientEndpointDiscovery`` trait indicates the operation that the
    client should use to discover endpoints for the service and the error
    returned when the endpoint being accessed has expired.
Trait selector
    ``service``
Trait value
    ``structure``

The ``aws.api#clientEndpointDiscovery`` trait is a structure that supports the
following members:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - operation
      - ``shapeId``
      - **REQUIRED** The operation used to discover endpoints for the service. The
        operation MUST be bound to the service.
    * - error
      - ``shapeId``
      - **RECOMMENDED** An error shape which indicates to a client that an endpoint they are
        using is no longer valid. If present, this error MUST be bound to any operation marked with
        the ``clientDiscoveredEndpoint`` trait that is bound to the service.

The input of the operation targeted by ``operation`` MAY contain none, either,
or both of the following members:

- a ``string`` member named ``Operation``
- a ``map`` member named ``Identifiers`` whose key and value types are
  ``string`` types.

The operation output MUST contain a member ``Endpoints`` that is a list of
``Endpoint`` structures, which are made up of two members:

- a ``string`` member named ``Address``
- a ``long`` member named ``CachePeriodInMinutes``


.. smithy-trait:: aws.api#clientDiscoveredEndpoint
.. _client-discovered-endpoint-trait:

``aws.api#clientDiscoveredEndpoint`` trait
==========================================

Trait summary
    The ``clientDiscoveredEndpoint`` trait indicates that the target operation
    should use the client's endpoint discovery logic.
Trait selector
    ``operation``
Trait value
    ``structure``

The ``aws.api#clientDiscoveredEndpoint`` trait is a structure that supports the
following members:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - required
      - ``boolean``
      - This field denotes whether or not this operation requires the use of a
        specific endpoint. If this field is false, the standard regional
        endpoint for a service can handle this request. The client will start
        sending requests to the standard regional endpoint while working to
        discover a more specific endpoint.


.. smithy-trait:: aws.api#clientEndpointDiscoveryId
.. _client-endpoint-discovery-id-trait:

``aws.api#clientEndpointDiscoveryId`` trait
===========================================

Summary
    The ``clientEndpointDiscoveryId`` trait indicates which member(s) of the
    operation input should be used to discover an endpoint for the service.
Trait selector
    ``operation[trait|aws.api#clientDiscoveredEndpoint] -[input]-> structure > :test(member[trait|required] > string)``
Trait value
    Annotation trait


Example Model
=============

The following model illustrates an API that uses a ``DescribeEndpoints``
operation to perform endpoint discovery for a ``GetObject`` operation
using an ``clientEndpointDiscoveryId``.

.. code-block:: smithy

    @aws.api#clientEndpointDiscovery(
        operation: DescribeEndpoints
        error: InvalidEndpointError
    )
    service FooService {
      version: "2019-09-10"
      operations: [DescribeEndpoints, GetObject]
    }

    operation DescribeEndpoints {
        input: DescribeEndpointsInput
        output: DescribeEndpointsOutput
        errors: [InvalidEndpointError]
    }

    @error("client")
    @httpError(421)
    structure InvalidEndpointError {}

    @input
    structure DescribeEndpointsInput {
      Operation: String
      Identifiers: Identifiers
    }

    map Identifiers {
      key: String
      value: String
    }

    @output
    structure DescribeEndpointsOutput {
      Endpoints: Endpoints
    }

    list Endpoints {
      member: Endpoint
    }

    structure Endpoint {
      Address: String
      CachePeriodInMinutes: Long
    }

    @aws.api#clientDiscoveredEndpoint(required: true)
    operation GetObject {
        input: GetObjectInput
        output: GetObjectOutput
    }

    @input
    structure GetObjectInput {
      @clientEndpointDiscoveryId
      @required
      Id: String
    }

    @output
    structure GetObjectOutput {
      Object: Blob
    }


Client Behavior
===============

When a client calls an operation which has the ``clientDiscoveredEndpoint``
trait where ``required`` is set to ``true`` or where the client has explicitly
enabled endpoint discovery, the client MUST attempt to perform endpoint
discovery synchronously.

When a client calls an operation which has the ``clientDiscoveredEndpoint``
trait where ``required`` is set to ``false``, the client SHOULD attempt to
perform endpoint discovery asynchronously.

To perform endpoint discovery, the client MUST first make a request
to the operation targeted by the value of ``operation`` on the service's
``clientEndpointDiscovery`` trait or attempt to retrieve a previously cached
response.

When calling the endpoint operation, the client MUST provide the following
parameters if they are in the endpoint operation's input shape:

* ``Operation`` - the name of the client operation to be called.
* ``Identifiers`` - a map of member name to member value of all
  members in the client operation's input shape that have the
  ``clientEndpointDiscoveryId`` trait.

The client MUST then use an endpoint from the ``Endpoints`` list in the
response. The client SHOULD prioritize endpoints by the order in which they
appear in the list.


Caching
-------

In order to reduce the necessary number of calls needed, clients SHOULD cache
the endpoints returned in the response. Clients SHOULD evict an endpoint from
the cache after a number of minutes defined in the ``CachePeriodInMinutes``
member of the ``Endpoint`` shape. Clients SHOULD attempt to refresh the cache
before the final endpoint in the cache expires. Clients MAY choose to refresh
the cache after cache period of the highest priority endpoint.

If a call to refresh the cache fails, the client SHOULD continue to use the
previous endpoint until the cache can be successfully refreshed, or until the
service returns the error targeted by the ``error`` property of the service's
``clientEndpointDiscovery`` trait.

Cache keys MUST include the AWS Access Key ID used to make the request.
Additionally, they MUST include the values of the ``Operation`` and
``Identifiers`` members passed in with the call to the endpoint discovery
operation if those members are present. Cache keys MAY include additional
context.

Clients SHOULD use an LRU cache implementation with an initial cache limit of
1000 entries. The cache limit SHOULD be configurable by the client.

Clients SHOULD scope the cache globally or to a specific client instance.


.. smithy-trait:: aws.protocols#httpChecksum
.. _aws.protocols#httpChecksum-trait:

------------------------------------
``aws.protocols#httpChecksum`` trait
------------------------------------

Summary
    Indicates that an operation's HTTP request or response supports checksum
    validation.
Trait selector
    ``operation``
Value type
    ``structure``

The ``httpChecksum`` trait is a structure that contains the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - requestAlgorithmMember
      - ``string``
      - Defines a top-level operation input member that is used to configure
        request checksum behavior. The input member MUST target a string shape
        marked with the :ref:`enum-trait`. Each value in the enum represents a
        supported checksum algorithm. Algorithms MUST be one of the following
        supported values: "CRC32C", "CRC32", "SHA1", or "SHA256".
    * - requestChecksumRequired
      - ``boolean``
      - Indicates an operation requires a checksum in its HTTP request. By
        default, the checksum used for a service is an MD5 checksum passed
        in the Content-MD5 header. When the input member represented by the
        ``requestAlgorithmMember`` property is set, the default behavior is
        disabled.
    * - requestValidationModeMember
      - ``string``
      - Defines a top-level operation input member used to opt-in to
        best-effort validation of a checksum returned in the HTTP response of
        the operation. The input member MUST target a string shape marked with
        the :ref:`enum-trait` that contains the value "ENABLED".
    * - responseAlgorithms
      - ``set<string>``
      - Defines the checksum algorithms clients SHOULD look for when validating
        checksums returned in the HTTP response. Each algorithm must be one of
        the following supported values: "CRC32C", "CRC32", "SHA1", or "SHA256".

The ``httpChecksum`` trait MUST define at least one of the request checksumming
behavior, by setting the ``requestAlgorithmMember`` or
``requestChecksumRequired`` property, or the response checksumming behavior, by
setting the ``requestValidationModeMember`` property.

The following is an example of the ``httpChecksum`` trait that defines required
request checksum behavior with support for the "CRC32C", "CRC32", "SHA1", and
"SHA256" algorithms *and* response checksum behavior with support for the
"CRC32C", "CRC32", "SHA1", and "SHA256" algorithms, enabled by the
``validationMode`` member.

Users of the ``PutSomething`` operation will opt in to request checksums by
selecting an algorithm in the ``checksumAlgorithm`` input property.

Users of the ``PutSomething`` operation will opt in to response checksums by
setting the ``validationMode`` input property to "ENABLED".

.. code-block:: smithy

    @httpChecksum(
        requestChecksumRequired: true,
        requestAlgorithmMember: "checksumAlgorithm",
        requestValidationModeMember: "validationMode",
        responseAlgorithms: ["CRC32C", "CRC32", "SHA1", "SHA256"]
    )
    operation PutSomething {
        input: PutSomethingInput
        output: PutSomethingOutput
    }

    structure PutSomethingInput {
        @httpHeader("x-amz-request-algorithm")
        checksumAlgorithm: ChecksumAlgorithm

        @httpHeader("x-amz-response-validation-mode")
        validationMode: ValidationMode

        @httpPayload
        content: Blob
    }

    enum ChecksumAlgorithm {
        CRC32C
        CRC32
        SHA1
        SHA256
    }

    enum ValidationMode {
        ENABLED
    }


The following trait, which does not define request or response checksum
behavior, will fail validation.

.. code-block:: smithy

    @httpChecksum()
    operation PutSomething {
        input: PutSomethingInput
        output: PutSomethingOutput
    }


.. _aws.protocols#httpChecksum-trait_behavior:

Client behavior
===============

HTTP request checksums
----------------------

When a client calls an operation which has the ``httpChecksum`` trait where
``requestChecksumRequired`` is set to ``true``, the client MUST include a
checksum in the HTTP request.

When a client calls an operation which has the ``httpChecksum`` trait where
``requestAlgorithmMember`` is set, the client MUST look up the input member
represented by ``requestAlgorithmMember`` property. The value of this member is
the checksum algorithm that the client MUST use to compute the request payload
checksum.

The computed checksum MUST be supplied at a resolved location as per the
:ref:`resolving checksum location <aws.protocols#httpChecksum-trait_resolving-checksum-location>`
section. If the resolved location is ``header``, the client MUST put the
checksum into the HTTP request headers. If the resolved location is
``trailer``, the client MUST put the checksum into the `chunked trailer part`_
of the body. The header or trailer name to use with an algorithm is resolved as
per the :ref:`resolving checksum name <aws.protocols#httpChecksum-trait_resolving-checksum-name>`
section.

If no ``requestAlgorithmMember`` property is set, the client MUST compute an
MD5 checksum of the request payload and place it in the ``Content-MD5`` header.

If the HTTP header corresponding to a checksum is set explicitly, the client
MUST use the explicitly set header and skip computing the payload checksum.

.. seealso:: See :ref:`client behavior<aws.protocols#httpChecksum-trait_header-conflict-behavior>`
    for more details.

HTTP response checksums
-----------------------

When a client receives a response for an operation which has the ``httpChecksum``
trait where the ``requestValidationModeMember`` property is set, the client
MUST look up the input member represented by the property's value. If the input
member is set to ``ENABLED``, the client MUST perform best-effort validation of
checksum values returned in the HTTP response.

A client MUST use the list of supported algorithms modeled in the
``responseAlgorithms`` property to look up the checksum(s) for which validation
MUST be performed. The client MUST look for the HTTP header in the response as
per the :ref:`resolving checksum name <aws.protocols#httpChecksum-trait_resolving-checksum-name>`
section.

A client MUST raise an error if the response checksum to validate does not
match computed checksum of the response payload for the same checksum algorithm.

If a checksum is provided in a response that is not listed in the
``responseAlgorithms`` property, the client MUST ignore the value and MUST NOT
attempt to validate it.

A client MUST provide a mechanism for customers to identify whether checksum
validation was performed on a response and which checksum algorithm was
validated.


Service behavior
================

HTTP request checksums
----------------------

When a service receives a request for an operation which has the ``httpChecksum``
trait where either the ``requestAlgorithmMember`` or ``requestChecksumRequired``
property are set, the service MUST validate an HTTP request checksum.

When a service receives a request where the ``requestAlgorithmMember`` is set,
the service MUST look up the input member represented by the checksum
``requestAlgorithmMember`` property. The value of this member is the checksum
algorithm that the service MUST use to compute a checksum of the request payload.

The computed checksum MUST be validated against the checksum supplied at a
resolved location as per the :ref:`resolving checksum location
<aws.protocols#httpChecksum-trait_resolving-checksum-location>` section. The
header or trailer name to use with an algorithm is resolved as per the
:ref:`resolving checksum name <aws.protocols#httpChecksum-trait_resolving-checksum-name>`
section.

If no ``requestAlgorithmMember`` is set, the service MUST compute an MD5
checksum of the request payload and validate it against the ``Content-MD5``
header.

When using the ``httpChecksum`` trait, services MUST always accept checksum
values in HTTP headers. For operations with streaming payload input, services
MUST additionally accept checksum values sent in the `chunked trailer part`_ of
the request body. Service MUST only send response checksums in HTTP headers.

HTTP response checksums
-----------------------

When a service sends a response for an operation which has the ``httpChecksum``
trait where the ``requestValidationModeMember`` property is set, the service
MUST look up the input member represented by the property's value. If the input
member is set to ``ENABLED``, the service MUST provide checksum(s) for the
response payload.

A service MUST provide a checksum for at least one algorithm defined in the
``responseAlgorithms`` property. The service MUST place the computed checksum(s)
in the HTTP header of the response as per the :ref:`resolving checksum name
<aws.protocols#httpChecksum-trait_resolving-checksum-name>` section.

A service MAY provide checksums for algorithms which are not defined in the
``responseAlgorithms`` property.


.. _aws.protocols#httpChecksum-trait_resolving-checksum-name:

Resolving checksum name
=======================

The checksum name MUST be used as both header name and trailer name. A checksum
name MUST conform to the pattern ``x-amz-checksum-*``, where `*` is the defined
algorithm name in lower case. For example, the checksum name for the ``SHA256``
algorithm is ``x-amz-checksum-sha256``.

.. _aws.protocols#httpChecksum-trait_header-conflict-behavior:

A member with the :ref:`httpHeader-trait` MAY conflict with a ``httpChecksum``
header name, allowing a checksum to be supplied directly. A member with the
:ref:`httpPrefixHeaders-trait` SHOULD NOT conflict with the ``x-amz-checksum-*``
prefix.


.. _aws.protocols#httpChecksum-trait_resolving-checksum-location:

Resolving checksum location
===========================

Valid values for resolved location are:

* ``Header`` - Indicates the checksum is placed in an HTTP header.
* ``Trailer`` - Indicates the checksum is placed in the `chunked trailer part`_ of the body.

For an HTTP request, clients resolve the location based on the signing method
used for the API request. The following table describes possible scenarios:

.. list-table::
    :header-rows: 1
    :widths: 30 40 30

    * - Payload type
      - Signing Method used
      - Resolved Location
    * - Normal Payload
      - `Header-based auth`_
      - Header
    * - Normal Payload
      - :ref:`Unsigned payload auth<aws.auth#unsignedPayload-trait>`
      - Header
    * - Streaming Payload
      - `Header-based auth`_
      - Header
    * - Streaming Payload
      - `Streaming-signing auth`_
      - Trailer
    * - Streaming Payload
      - :ref:`Unsigned payload auth<aws.auth#unsignedPayload-trait>`
      - Trailer

For an HTTP response, clients only support validating checksums sent in an
HTTP header. Thus, the resolved location is always ``Header``.

.. seealso:: See :ref:`aws.protocols#httpChecksum-trait_behavior` for more details.

.. _Header-based auth: https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html
.. _Streaming-signing auth: https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-streaming.html
.. _chunked trailer part: https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-streaming.html


.. _aws.protocols#httpChecksum-trait_with-checksum-required:

Behavior with :ref:`httpChecksumRequired <httpChecksumRequired-trait>`
======================================================================

Behavior defined by the ``httpChecksum`` trait's ``requestChecksumRequired``
property supersedes the :ref:`httpChecksumRequired <httpChecksumRequired-trait>`
trait. Setting only the ``requestChecksumRequired`` property of the
``httpChecksum`` trait is equivalent to applying the ``httpChecksumRequired``
trait.


.. smithy-trait:: aws.api#tagEnabled

----------------------------
``aws.api#tagEnabled`` trait
----------------------------

Trait summary
    Indicates the service supports resource level tagging consistent with AWS
    services.
Trait selector
    ``service``
Trait value
    ``structure``

The ``aws.api#tagEnabled`` trait is a structure that supports the following
members:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - disableDefaultOperations
      - ``boolean``
      - Set to true to indicate that the service does not have default tagging
        operations that create, read, update, and delete tags on resources.

The default operations for resource tagging operations are named TagResource,
UntagResource, and ListTagsForResource. All three operations are required to be
in the service, and each operation must satisfy corresponding validation
constraints unless ``disableDefaultOperations`` is set to **true**.

The following is a minimal snippet showing the inclusion of the named required
operations for the ``aws.api#tagEnabled``  Weather service.

.. code-block:: smithy

    @tagEnabled
    service Weather {
        operations: [TagResource, UntagResource, ListTagsForResource]
    }

.. _tag-resource-api:

``TagResource``
===============

The tag resource operation for an ``aws.api#tagEnabled`` service creates and updates
tag associations on a resource:

* Operation name must case-sensitively match ``TagResource``.
* Must have exactly one input member that targets a string shape and has a
  member name that matches ``^([R|r]esource)?([A|a]rn|ARN)$`` to accept
  the resource ARN.
* Must have exactly one input member that targets a list shape, with list
  member targeting a structure that consists of two members that target a
  string shape representing the tag key or name and the tag value. This
  member name must match: ``^[T|t]ag(s|[L|l]ist)$``

The following snippet is a valid definition of a tag resource operation and
its input:

.. code-block:: smithy

    structure Tag { key: String, value: String }

    list TagList { member: Tag }

    operation TagResource {
        input := {
            @required
            resourceArn: String
            @length(max: 128)
            tags: TagList
        }
        output := { }
    }

.. _untag-resource-api:

``UntagResource``
=================

The untag resource operation removes tag associations on a resource.

* Operation name must case-sensitively match ``UntagResource``.
* Must have exactly one input member that targets a string shape and has a
  member name that matches: ``^([R|r]esource)?([A|a]rn|ARN)$`` to accept
  the resource ARN.
* Must have exactly one input member that targets a list shape, with list
  member targeting a string representing tag keys or names to remove. This
  member name must match: ``^[T|t]ag[K|k]eys$``

The following snippet is an example of the untag resource operation and its
input:

.. code-block:: smithy

    list TagKeys { member: String }

    operation UntagResource {
        input := {
            @required
            arn: String
            @required
            tagKeys: TagKeys
        }
        output := { }
    }

.. _listtags-resource-api:

``ListTagsForResource``
=======================

The list tags for resource operation retrieves all tag associations on a
resource.

* Operation name must case-sensitively match ``ListTagsForResource``.
* Must have exactly one input member that targets a string shape and has a
  member name that matches: ``^([R|r]esource)?([A|a]rn|ARN)$`` to accept
  the resource ARN.
* Must have exactly one output member that targets a list shape, with list
  member targeting a structure that consists of two members that target a
  string shape representing the tag key or name and the tag value. This
  member name must match: ``^[T|t]ag(s|[L|l]ist)$``

The following snippet is an example of the list tags for resource operation and
its input:

.. code-block:: smithy

    structure Tag { key: String, value: String }

    list TagList { member: Tag }

    operation ListTagsForResource {
        input := {
            @required
            arn: String
        }
        output := {
            @length(max: 128)
            tags: TagList
        }
    }

The following example shows a typical service with the default tagging
operations. It can be understood that Forecast resource tags are managed
through the operates attached to the service.

.. code-block:: smithy

    @tagEnabled
    service Weather {
        resources: [Forecast]
        operations: [TagResource, UntagResource, ListTagsForResource]
    }

    @arn(template: "city/{cityId}/forecast/{forecastId}")
    @taggable
    resource Forecast {
        identifiers: {
            forecastId: ForecastId
        }
    }


.. smithy-trait:: aws.api#taggable

--------------------------
``aws.api#taggable`` trait
--------------------------

Trait summary
    Indicates the resource supports AWS tag associations and identifies resource
    specific operations that perform CRUD on the associated tags. Managing tag
    associations on a resource through service-wide operations is possible if
    the resource has an :ref:`aws.api#arn-trait`.

Trait selector
    ``resource``
Trait value
    ``structure``

The ``aws.api#taggable`` trait is a structure that supports the following
members:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - property
      - ``string``
      - The name of the resource property representing tags for the resource.
        Specifying this enables Smithy to understand which resource lifecycle
        operations operate on tags.
    * - apiConfig
      - :ref:`Taggable resource API config structure <taggable-apiconfig-structure>`
      - Configuration structure for specifying resource instance tagging
        operations, if applicable.

Resource specific tagging operations tagApi, untagApi, and listTagsApi have
corresponding requirements to :ref:`tag-resource-api`, :ref:`untag-resource-api`,
and :ref:`listtags-resource-api`. The differences is that these operations may
have any name, and the resource ARN input member requirement is replaced by the
expected identifier binding rules for instance operations.

The following is an example of a resource with its own resource specific
tagging operations. Note the service has disabled default tagging operations
and the resource lacks an :ref:`aws.api#arn-trait`.

.. code-block:: smithy

    @tagEnabled(disableDefaultOperations: true)
    service Weather {
        resources: [City]
    }
    operation TagCity {
        input := {
            @required
            cityId: CityId
            @length(max: 128)
            tags: TagList
        }
        output := { }
    }

    operation UntagCity {
        input := {
            @required
            cityId: CityId
            @required
            @notProperty
            tagKeys: TagKeys
        }
        output := { }
    }

    operation ListTagsForCity {
        input := {
            @required
            cityId: CityId
        }
        output := {
            @length(max: 128)
            tags: TagList
        }
    }

    @taggable(
        property: "tags",
        apiConfig: {
            tagApi: TagCity,
            untagApi: UntagCity,
            listTagsApi: ListTagsForCity
        })
    resource City {
        properties: {
            tags: TagList
        }
        operations: [TagCity, UntagCity, ListTagsForCity],
    }


.. _taggable-apiconfig-structure:

Taggable resource API config structure
======================================

Configuration structure for specifying resource instance tagging operations,
if applicable.

**Properties**

.. list-table::
    :header-rows: 1
    :widths: 30 10 60

    * - Property
      - Type
      - Description
    * - tagApi
      - ``ShapeID``
      - **Required** Defines the operation used to create and update tags
        associations for the resource. The value MUST be a valid
        :ref:`shape-id` that targets an ``operation`` shape.
    * - untagApi
      - ``ShapeID``
      - **Required** Defines the operation used to deletes tag associations
        from the resource. The value MUST be a valid :ref:`shape-id` that
        targets an ``operation`` shape.
    * - listTagsApi
      - ``ShapeID``
      - **Required** Defines the operation used to list tags for the resource.
        The value MUST be a valid :ref:`shape-id` that targets an ``operation``
        shape.


--------
Appendix
--------


.. _aws-service-appendix-a:

Appendix A: Example SDK service IDs
===================================

The following, non-exhaustive, table defines the SDK service ID of many
existing AWS services.

.. csv-table::
    :header: "sdkId", "title trait"

    ACM, AWS Certificate Manager
    API Gateway, Amazon API Gateway
    Application Auto Scaling, Application Auto Scaling
    AppStream, Amazon AppStream
    Athena, Amazon Athena
    Auto Scaling, Auto Scaling
    Batch, AWS Batch
    Budgets, AWS Budgets
    CloudDirectory, Amazon CloudDirectory
    CloudFormation, AWS CloudFormation
    CloudFront, Amazon CloudFront
    CloudHSM, Amazon CloudHSM
    CloudHSM V2, AWS CloudHSM V2
    CloudSearch, Amazon CloudSearch
    CloudSearch Domain, Amazon CloudSearch Domain
    CloudTrail, AWS CloudTrail
    CloudWatch, Amazon CloudWatch
    CodeBuild, AWS CodeBuild
    CodeCommit, AWS CodeCommit
    CodeDeploy, AWS CodeDeploy
    CodePipeline, AWS CodePipeline
    CodeStar, AWS CodeStar
    Cognito Identity, Amazon Cognito Identity
    Cognito Identity Provider, Amazon Cognito Identity Provider
    Cognito Sync, Amazon Cognito Sync
    Config Service, AWS Config
    Cost and Usage Report Service, AWS Cost and Usage Report Service
    Data Pipeline, AWS Data Pipeline
    DAX, Amazon DynamoDB Accelerator (DAX)
    Device Farm, AWS Device Farm
    Direct Connect, AWS Direct Connect
    Application Discovery Service, AWS Application Discovery Service
    Database Migration Service, AWS Database Migration Service
    Directory Service, AWS Directory Service
    DynamoDB, Amazon DynamoDB
    DynamoDB Streams, Amazon DynamoDB Streams
    EC2, Amazon Elastic Compute Cloud
    ECR, Amazon EC2 Container Registry
    ECS, Amazon EC2 Container Service
    EFS, Amazon Elastic File System
    ElastiCache, Amazon ElastiCache
    Elastic Beanstalk, AWS Elastic Beanstalk
    Elastic Transcoder, Amazon Elastic Transcoder
    Elastic Load Balancing, Elastic Load Balancing
    Elastic Load Balancing v2, Elastic Load Balancing
    EMR, Amazon Elastic MapReduce
    Elasticsearch Service, Amazon Elasticsearch Service
    CloudWatch Events, Amazon CloudWatch Events
    Firehose, Amazon Kinesis Firehose
    GameLift, Amazon GameLift
    Glacier, Amazon Glacier
    Glue, AWS Glue
    Greengrass, AWS Greengrass
    Health, AWS Health APIs and Notifications
    IAM, AWS Identity and Access Management
    ImportExport, AWS Import/Export
    Inspector, Amazon Inspector
    IoT, AWS IoT
    IoT Data Plane, AWS IoT Data Plane
    Kinesis, Amazon Kinesis
    Kinesis Analytics, Amazon Kinesis Analytics
    KMS, AWS Key Management Service
    Lambda, AWS Lambda
    Lex Model Building Service, Amazon Lex Model Building Service
    Lex Runtime Service, Amazon Lex Runtime Service
    Lightsail, Amazon Lightsail
    CloudWatch Logs, Amazon CloudWatch Logs
    Machine Learning, Amazon Machine Learning
    Marketplace Entitlement Service, AWS Marketplace Entitlement Service
    Marketplace Commerce Analytics, AWS Marketplace Commerce Analytics
    Marketplace Metering, AWS Marketplace Metering
    Migration Hub, AWS Migration Hub
    Mobile, AWS Mobile
    MTurk, Amazon Mechanical Turk
    OpsWorks, AWS OpsWorks
    OpsWorksCM, AWS OpsWorks for Chef Automate
    Organizations, AWS Organizations
    Pinpoint, Amazon Pinpoint
    Polly, Amazon Polly
    RDS, Amazon Relational Database Service
    Redshift, Amazon Redshift
    Rekognition, Amazon Rekognition
    Resource Groups Tagging API, AWS Resource Groups Tagging API
    Route 53, Amazon Route 53
    Route 53 Domains, Amazon Route 53 Domains
    S3, Amazon Simple Storage Service
    SimpleDB, Amazon SimpleDB
    Service Catalog, AWS Service Catalog
    SES, Amazon Simple Email Service
    Shield, AWS Shield
    SMS, AWS Server Migration Service
    Snowball, Amazon Import/Export Snowball
    SNS, Amazon Simple Notification Service
    SQS, Amazon Simple Queue Service
    SSM, Amazon Simple Systems Manager (SSM)
    SFN, AWS Step Functions
    Storage Gateway, AWS Storage Gateway
    STS, AWS Security Token Service
    Support, AWS Support
    SWF, Amazon Simple Workflow Service
    WAF, AWS WAF
    WAF Regional, AWS WAF Regional
    WorkDocs, Amazon WorkDocs
    WorkSpaces, Amazon WorkSpaces
    XRay, AWS X-Ray


.. _event records: https://docs.aws.amazon.com/awscloudtrail/latest/userguide/cloudtrail-event-reference-record-contents.html
.. _AWS CloudFormation service name: http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws.template-resource-type-ref.html
.. _ARN service namespace: http://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html#genref-aws-service-namespaces
.. _AWS signature version 4: https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html
.. _Amazon Resource Name (ARN): https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html
.. _AWS Service Namespaces: https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html#genref-aws-service-namespaces
.. _CloudFormation resource type: https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-template-resource-type-ref.html
.. _AWS Regions and Endpoints: https://docs.aws.amazon.com/general/latest/gr/rande.html
