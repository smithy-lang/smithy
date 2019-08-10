======================
AWS Core Specification
======================

Various AWS-specific traits are used to integrate Smithy models with other
AWS products like AWS CloudFormation and tools like the AWS SDKs.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


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
    ``object`` that contains the following properties:

    * :ref:`service-sdk-id` (required)
    * :ref:`service-cloudformation-name`
    * :ref:`service-arn-namespace`
    * :ref:`service-cloudtrail-event-source`
    * :ref:`service-abbreviation`

The following example defines an AWS service that uses the default values of
``cloudFormationService``, ``arnNamespace``, and ``cloudTrailEventSource``:

.. tabs::

    .. code-tab:: smithy

        $version: "0.4.0"
        namespace aws.fooBaz

        use aws.api#service

        @service(sdkId: "Some Value")
        service FooBaz {
            version: "2018-03-17",
        }

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "aws.fooBaz": {
                "shapes": {
                    "FooBaz": {
                        "type": "service",
                        "version": "2018-03-17",
                        "aws.api#service": {
                            "sdkId": "Some Value"
                        }
                    }
                }
            }
        }

The following example provides explicit values for all properties:

.. tabs::

    .. code-tab:: smithy

        $version: "0.4.0"
        namespace aws.fooBaz

        use aws.api#service

        @service(
            sdkId: "Some Value",
            cloudFormationName: "FooBaz",
            arnNamespace: "myservice",
            cloudTrailEventSource: "myservice.amazon.aws")
        service FooBaz {
            version: "2018-03-17",
        }

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "aws.fooBaz": {
                "shapes": {
                    "FooBaz": {
                        "type": "service",
                        "version": "2018-03-17",
                        "aws.api#service": {
                            "sdkId": "Some Value",
                            "cloudFormationName": "FooBaz",
                            "arnNamespace": "myservice",
                            "cloudTrailEventSource": "myservice.amazon.aws"
                        }
                    }
                }
            }
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
* The value must not case-insensitively end with "API", "Client", or "Service".
* The value MUST NOT change change once a service is publicly released. If the value
  does change, the service will be considered a brand new service in the AWS SDKs
  and Tools.


Choosing an SDK service ID
--------------------------

The ``sdkId`` value should reasonably represent the service it identifies. ``sdkId``
MUST NOT be an arbitrary value; for example for Amazon DynamoDB, an appropriate
"serviceId" would be "DynamoDB" while an inappropriate value would be "Foo".

The following steps can be taken to produce a ``sdkId`` that should generally work
for most services:

1. Pick a base to derive the "sdkId". If available, use the ``abbreviation``
   property of the ``aws.api#service`` trait as the base. An example of an official
   service abbreviation is ``Amazon S3`` for ``Amazon Simple Storage Service``.
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
*eventSource* property contained in CloudTrail `event records`_
emitted by the service. If not specified, this value defaults to the
``arnNamespace`` plus .amazonaws.com. For example:

* AWS CloudFormation has an ``arnNamespace`` of ``cloudformation`` and an
  event source of ``cloudformation.amazonaws.com``.
* Amazon EC2 has an ``arnNamespace`` of ``ec2`` and an event source of
  ``ec2.amazonaws.com``.
* Amazon Simple Workflow Service has an ``arnNamespace`` of ``swf`` and
  an event source of ``swf.amazonaws.com``.

This convention has some exceptions. For example, the event source for
Amazon CloudWatch is ``monitoring.amazonaws.com``. Such services will
need to explicitly configure the ``cloudTrailEventSource`` setting.


.. _service-abbreviation:

``abbreviation``
================

The ``abbreviation`` property is a ``string`` value that defines the official
abbreviation of a service. For example, the official abbreviation of
"Amazon Simple Storage Service" is "Amazon S3", and the abbreviation of
"Amazon Kinesis Firehose" is "Firehose".

See :ref:`aws-service-appendix-a` for a table containing various AWS services
and their abbreviations.


.. _aws.api#arn-trait:

---------------------
``aws.api#arn`` trait
---------------------

Trait summary
    Defines an ARN of a Smithy resource shape.
Trait selector
    ``resource``
Trait value
    ``object``

The ``aws.api#arn`` trait is an object that supports the following
properties:

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

An ARN is is a structured URI made up of the following components:

.. code-block:: none

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

.. tabs::

    .. code-tab:: smithy

        $version: "0.4.0"
        namespace aws.fooBaz

        use aws.api#service
        use aws.api#arn

        @service(sdkId: "Some Value")
        service FooBaz {
            version: "2018-03-17",
            resources: [MyResource],
        }

        @arn(template: "myresource/{myId}")
        resource MyResource {
            identifiers: {myId: MyResourceId},
        }

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "FooBaz": {
                        "type": "service",
                        "version": "2018-03-17",
                        "resources": ["MyResource"],
                        "aws.api#service": {
                            "sdkId": "Some Value"
                        }
                    },
                    "MyResource": {
                        "type": "resource",
                        "identifiers": {"myId": "MyResourceId"},
                        "aws.api#arn": {
                            "template": "myresource/{myId}"
                        }
                    }
                }
            }
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

.. tabs::

    .. code-tab:: smithy

        use aws.api#arn
        use aws.api#arnReference

        @arn(template: "{arn}", absolute: true)
        resource MyResource {
            identifiers: {arn: Arn}
        }

        @arnReference(service: FooBaz, resource: MyResource)
        string Arn

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "MyResource": {
                        "type": "resource",
                        "identifiers": {
                            "arn": "Arn"
                        },
                        "aws.api#arn": {
                            "template": "{arn}",
                            "absolute": true
                        }
                    },
                    "Arn": {
                        "type": "string",
                        "aws.api#arnReference": {
                            "service": "FooBaz",
                            "resource": "MyResource"
                        }
                    }
                }
            }
        }


.. _aws.api#arnReference-trait:

------------------------------
``aws.api#arnReference`` trait
------------------------------

Trait summary
    Specifies that a string shape contains a fully formed AWS ARN.
Trait selector
    ``string``
Trait value
    ``object``

Smithy models can refer to AWS resources using ARNs. The
``aws.api#arnReference`` can be applied to a string shape to indicate
that the string contains an ARN and what resource is targeted by the
ARN.

The ``aws.api#arnReference`` trait is an object that supports the following
optional properties:

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
      - The Smithy service shape ID that is referenced by the ARN. This
        shape ID MAY be relative to the current namespace. The targeted
        service is not required to be found in the model, allowing for
        external shapes to be referenced without needing to take on an
        additional dependency.
    * - resource
      - ``string``
      - A shape ID that references the Smithy resource type contained in the
        ARN (e.g., ``com.foo#SomeResource``). This shape ID MAY be relative to
        the current namespace. The targeted resource is not required to be
        found in the model, allowing for external shapes to be referenced
        without needing to take on an additional dependency. If the shape is
        found in the model, it MUST target a resource shape, and the resource
        MUST be found within the closure of the referenced service shape.

The following example defines a string shape that targets an AWS resource.
The CloudFormation name of the resource and the Smithy service and resource
shape IDs are provided to give tooling additional information about the
referenced resource.

.. tabs::

    .. code-tab:: smithy

        $version: "0.4.0"
        namespace smithy.example

        use aws.api#arnReference

        @arnReference(
            type: "AWS::SomeService::SomeResource",
            service: com.foo#SomeService,
            resource: com.foo#SomeResource)
        string SomeResourceId

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "SomeResourceId": {
                        "type": "string",
                        "aws.api#arnReference": {
                            "type": "AWS::SomeService::SomeResource",
                            "service": "com.foo#SomeService",
                            "resource": "com.foo#SomeResource"
                        }
                    }
                }
            }
        }

The following example defines an ARN reference that doesn't provide an context
about the referenced shape. While this is valid, it is not as useful as the
previous example:

.. tabs::

    .. code-tab:: smithy

        $version: "0.4.0"
        namespace smithy.example

        use aws.api#arnReference

        @arnReference
        string SomeResourceId

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "SomeResourceId": {
                        "type": "string",
                        "aws.api#arnReference": {}
                    }
                }
            }
        }


.. _aws.api#data-trait:

----------------------
``aws.api#data`` trait
----------------------

Summary
    Designates the target as containing data of a known classification level.
Trait selector
    ``:test(simpleType, collection, structure, union, member)``
Value type
    ``string`` that is one of: ``content``, ``account``, ``usage``,
    ``tagging``, or ``permissions``. See :ref:`data-classifications` for more
    information.

Data classifications are resolved hierarchically: the data classification
of a member inherits the effective data classification applied to a parent
structure, union, or collection unless overridden.

.. tabs::

    .. code-tab:: smithy

        use aws.api#data

        @data("permissions")
        structure MyStructure {
            name: String,

            @data("content")
            content: String,

            tags: TagList,
        }

        @data("tagging")
        list TagList {
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "MyStructure": {
                        "type": "structure",
                        "members": {
                            "content": {
                                "target": "String",
                                "aws.api#data": "content"
                            },
                            "tags": {
                                "target": "TagList"
                            },
                            "name": {
                                "target": "String",
                            }
                        }
                    },
                    "TagList": {
                        "type": "list",
                        "member": {
                            "target": "String"
                        },
                        "aws.api#data": "tagging"
                    }
                }
            }
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
      - Customer content means any software (including machine images), data,
        text, audio, video or images that customers or any customer end user
        transfers to AWS for processing, storage or hosting by AWS services in
        connection with the customer’s accounts and any computational results
        that customers or any customer end user derive from the foregoing
        through their use of AWS services.
    * - ``account``
      - Account information means information about customers that customers
        provide to AWS in connection with the creation or administration of
        customers’ accounts.
    * - ``usage``
      - Service Attributes means service usage data related to a customer’s
        account, such as resource identifiers, metadata tags, security and
        access roles, rules, usage policies, permissions, usage statistics,
        logging data, and analytics.
    * - ``tagging``
      - Designates metadata tags applied to AWS resources.
    * - ``permissions``
      - Designates security and access roles, rules, usage policies, and
        permissions.


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

.. tabs::

    .. code-tab:: smithy

        use aws.api#controlPlane

        @controlPlane
        operation PutThings(PutThingsInput) -> PutThingsOutput

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "PutThings": {
                        "type": "operation",
                        "input": "PutThingsInput",
                        "output": "PutThingsOutput",
                        "aws.api#controlPlane": true
                    }
                }
            }
        }


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

.. tabs::

    .. code-tab:: smithy

        use aws.api#controlPlane

        @dataPlane
        operation PutThings(PutThingsInput) -> PutThingsOutput

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "PutThings": {
                        "type": "operation",
                        "input": "PutThingsInput",
                        "output": "PutThingsOutput",
                        "aws.api#dataPlane": true
                    }
                }
            }
        }


.. _aws.api#unsignedPayload-trait:

---------------------------------
``aws.api#unsignedPayload`` trait
---------------------------------

Summary
    Indicates that the payload of an operation is not to be part of the
    signature computed for the request of an operation.

    Providing a list of strings will limit the effect of this trait to
    only specific authentication schemes by name. An empty list of strings
    causes this trait to apply to all authentication schemes used with the
    the operation.
Trait selector
    ``operation``
Value type
    List of authentication scheme strings

Most requests sent to AWS services require that the payload of the request is
signed. However, in some cases, a service that streams large amounts of data
with an unknown size at the time a request is initiated might require that the
payload of a request is not signed.

The following example defines an operation that indicates the payload of the
operation MUST NOT be used as part of the request signature calculation:

.. tabs::

    .. code-tab:: smithy

        use aws.api#unsignedPayload

        @unsignedPayload
        operation PutThings(PutThingsInput) -> PutThingsOutput

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "PutThings": {
                        "type": "operation",
                        "input": "PutThingsInput",
                        "output": "PutThingsOutput",
                        "aws.api#unsignedPayload": []
                    }
                }
            }
        }

The following example defines an operation that requires an unsigned payload
only when using the "aws.v4" authentication scheme:

.. tabs::

    .. code-tab:: smithy

        use aws.api#unsignedPayload

        @unsignedPayload(["aws.v4"])
        operation PutThings(PutThingsInput) -> PutThingsOutput

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "smithy.example": {
                "shapes": {
                    "PutThings": {
                        "type": "operation",
                        "input": "PutThingsInput",
                        "output": "PutThingsOutput",
                        "aws.api#unsignedPayload": ["aws.v4"]
                    }
                }
            }
        }


Unsigned Payloads and signature version 4
=========================================

Using an unsigned payload with `AWS signature version 4`_ requires that the
literal string ``UNSIGNED-PAYLOAD`` is used when constructing a
`canonical request`_, and the same value is sent in the
`x-amz-content-sha256`_ header when sending an HTTP request.


.. _aws-authentication:

--------------
Authentication
--------------


.. _aws.v4-auth:

``aws.v4`` Authentication
=========================

The ``aws.v4`` authentication scheme is used to indicate that a service
supports `AWS signature version 4`_. This authentication scheme does not
require any configuration settings. The service name used in the
signature version 4 credential scope defaults to the resolved value of the
``aws.api#service`` trait :ref:`service-arn-namespace` property (that is,
if the value is explicitly defined, then use it, otherwise use the name of
the service converted to lowercase characters).

.. tabs::

    .. code-tab:: smithy

        $version: "0.4.0"
        namespace aws.fooBaz

        use aws.api#service

        @service(sdkId: "Some Value")
        @protocols([{name: "aws.rest-json", auth: ["aws.v4"]}])
        service FooBaz {
            version: "2018-03-17",
        }

    .. code-tab:: json

        {
            "smithy": "0.4.0",
            "aws.fooBaz": {
                "shapes": {
                    "FooBaz": {
                        "type": "service",
                        "version": "2018-03-17",
                        "protocols": [{"name": "aws.rest-json", "auth": ["aws.v4"]}],
                        "aws.api#service": {
                            "sdkId": "Some Value"
                        }
                    }
                }
            }
        }


--------
Appendix
--------


.. _aws-service-appendix-a:

Appendix A: Example SDK service IDs
===================================

The following, non-exhaustive, table defines the SDK service ID of many
existing AWS services.

.. csv-table::
    :header: "sdkId", "title trait", "abbreviation"
    :widths: 20, 20, 10

    ACM, AWS Certificate Manager, ACM
    API Gateway, Amazon API Gateway, None
    Application Auto Scaling, Application Auto Scaling, None
    AppStream, Amazon AppStream, None
    Athena, Amazon Athena, None
    Auto Scaling, Auto Scaling, None
    Batch, AWS Batch, AWS Batch
    Budgets, AWS Budgets, AWSBudgets
    CloudDirectory, Amazon CloudDirectory, None
    CloudFormation, AWS CloudFormation, None
    CloudFront, Amazon CloudFront, CloudFront
    CloudHSM, Amazon CloudHSM, CloudHSM
    CloudHSM V2, AWS CloudHSM V2, CloudHSM V2
    CloudSearch, Amazon CloudSearch, None
    CloudSearch Domain, Amazon CloudSearch Domain, None
    CloudTrail, AWS CloudTrail, CloudTrail
    CloudWatch, Amazon CloudWatch, CloudWatch
    CodeBuild, AWS CodeBuild, None
    CodeCommit, AWS CodeCommit, CodeCommit
    CodeDeploy, AWS CodeDeploy, CodeDeploy
    CodePipeline, AWS CodePipeline, CodePipeline
    CodeStar, AWS CodeStar, CodeStar
    Cognito Identity, Amazon Cognito Identity, None
    Cognito Identity Provider, Amazon Cognito Identity Provider, None
    Cognito Sync, Amazon Cognito Sync, None
    Config Service, AWS Config, Config Service
    Cost and Usage Report Service, AWS Cost and Usage Report Service, None
    Data Pipeline, AWS Data Pipeline, None
    DAX, Amazon DynamoDB Accelerator (DAX), Amazon DAX
    Device Farm, AWS Device Farm, None
    Direct Connect, AWS Direct Connect, None
    Application Discovery Service, AWS Application Discovery Service, None
    Database Migration Service, AWS Database Migration Service, None
    Directory Service, AWS Directory Service, Directory Service
    DynamoDB, Amazon DynamoDB, DynamoDB
    DynamoDB Streams, Amazon DynamoDB Streams, None
    EC2, Amazon Elastic Compute Cloud, Amazon EC2
    ECR, Amazon EC2 Container Registry, Amazon ECR
    ECS, Amazon EC2 Container Service, Amazon ECS
    EFS, Amazon Elastic File System, EFS
    ElastiCache, Amazon ElastiCache, None
    Elastic Beanstalk, AWS Elastic Beanstalk, Elastic Beanstalk
    Elastic Transcoder, Amazon Elastic Transcoder, None
    Elastic Load Balancing, Elastic Load Balancing, None
    Elastic Load Balancing v2, Elastic Load Balancing, Elastic Load Balancing v2
    EMR, Amazon Elastic MapReduce, Amazon EMR
    Elasticsearch Service, Amazon Elasticsearch Service, None
    CloudWatch Events, Amazon CloudWatch Events, None
    Firehose, Amazon Kinesis Firehose, Firehose
    GameLift, Amazon GameLift, None
    Glacier, Amazon Glacier, None
    Glue, AWS Glue, None
    Greengrass, AWS Greengrass, None
    Health, AWS Health APIs and Notifications, AWSHealth
    IAM, AWS Identity and Access Management, IAM
    ImportExport, AWS Import/Export, None
    Inspector, Amazon Inspector, None
    IoT, AWS IoT, None
    IoT Data Plane, AWS IoT Data Plane, None
    Kinesis, Amazon Kinesis, Kinesis
    Kinesis Analytics, Amazon Kinesis Analytics, Kinesis Analytics
    KMS, AWS Key Management Service, KMS
    Lambda, AWS Lambda, None
    Lex Model Building Service, Amazon Lex Model Building Service, None
    Lex Runtime Service, Amazon Lex Runtime Service, None
    Lightsail, Amazon Lightsail, None
    CloudWatch Logs, Amazon CloudWatch Logs, None
    Machine Learning, Amazon Machine Learning, None
    Marketplace Entitlement Service, AWS Marketplace Entitlement Service, None
    Marketplace Commerce Analytics, AWS Marketplace Commerce Analytics, None
    Marketplace Metering, AWS Marketplace Metering, None
    Migration Hub, AWS Migration Hub, None
    Mobile, AWS Mobile, None
    MTurk, Amazon Mechanical Turk, Amazon MTurk
    OpsWorks, AWS OpsWorks, None
    OpsWorksCM, AWS OpsWorks for Chef Automate, OpsWorksCM
    Organizations, AWS Organizations, Organizations
    Pinpoint, Amazon Pinpoint, None
    Polly, Amazon Polly, None
    RDS, Amazon Relational Database Service, Amazon RDS
    Redshift, Amazon Redshift, None
    Rekognition, Amazon Rekognition, None
    Resource Groups Tagging API, AWS Resource Groups Tagging API, None
    Route 53, Amazon Route 53, Route 53
    Route 53 Domains, Amazon Route 53 Domains, None
    S3, Amazon Simple Storage Service, Amazon S3
    SimpleDB, Amazon SimpleDB, None
    Service Catalog, AWS Service Catalog, None
    SES, Amazon Simple Email Service, Amazon SES
    Shield, AWS Shield, AWS Shield
    SMS, AWS Server Migration Service, SMS
    Snowball, Amazon Import/Export Snowball, Amazon Snowball
    SNS, Amazon Simple Notification Service, Amazon SNS
    SQS, Amazon Simple Queue Service, Amazon SQS
    SSM, Amazon Simple Systems Manager (SSM), Amazon SSM
    SFN, AWS Step Functions, AWS SFN
    Storage Gateway, AWS Storage Gateway, None
    STS, AWS Security Token Service, AWS STS
    Support, AWS Support, None
    SWF, Amazon Simple Workflow Service, Amazon SWF
    WAF, AWS WAF, WAF
    WAF Regional, AWS WAF Regional, WAF Regional
    WorkDocs, Amazon WorkDocs, None
    WorkSpaces, Amazon WorkSpaces, None
    XRay, AWS X-Ray, None


.. _event records: https://docs.aws.amazon.com/awscloudtrail/latest/userguide/cloudtrail-event-reference-record-contents.html
.. _AWS CloudFormation service name: http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws.template-resource-type-ref.html
.. _ARN service namespace: http://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html#genref-aws-service-namespaces
.. _AWS signature version 4: https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html
.. _canonical request: https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html
.. _x-amz-content-sha256: https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html
.. _Amazon Resource Name (ARN): https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html
.. _AWS Service Namespaces: https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html#genref-aws-service-namespaces
.. _CloudFormation resource type: https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-template-resource-type-ref.html
