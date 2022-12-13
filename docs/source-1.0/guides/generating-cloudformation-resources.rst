.. _smithy-to-cloudformation:

======================================================
Generating CloudFormation Resource Schemas from Smithy
======================================================

This guide describes how Smithy models can generate `CloudFormation Resource
Schemas`_.

------------
Introduction
------------

CloudFormation Resource Schemas are the standard method of `modeling a resource
provider`_ for use within `CloudFormation`_. These schemas can then be used
to `develop the resource provider`_ for support in CloudFormation. Generating
Resource Schemas automatically from Smithy resources removes the duplicate
effort of specifying them.

:ref:`AWS CloudFormation traits <aws-cloudformation-traits>` define how
CloudFormation Resource Schemas should be generated from Smithy resources.
Automatically generating schemas from a service's API lowers the effort needed
to generate and maintain them, keeps the schemas in sync with any changes to
the Smithy model, reduces the potential for errors in the translation, and
provides a more complete depiction of a resource in its schema. These schemas
can be utilized by the `CloudFormation Command Line Interface`_ to build,
register, and deploy resource providers.

:ref:`Other traits may also influence CloudFormation Resource Schema
generation. <generate-cloudformation-other-traits>`

Differences between Smithy resources and CloudFormation Resource Schemas
------------------------------------------------------------------------

Smithy and CloudFormation have different approaches to modeling resources. In
Smithy, a :ref:`resource <resource>` is an entity with an identity that has a
set of operations. CloudFormation resources are defined as a collection of
properties and their attributes, along with additional information on which
properties are identifiers or have restrictions on their mutability.


------------------------------------
Generating Schemas with smithy-build
------------------------------------

The ``cloudformation`` plugin contained in the ``software.amazon.smithy:smithy-aws-cloudformation``
package can be used with smithy-build and the `Smithy Gradle plugin`_ to
generate CloudFormation Resource Schemas from Smithy models.

The following example shows how to configure Gradle to generate CloudFormation
Resource Schemas from a Smithy model :ref:`using a buildscript dependency
<artifacts-from-smithy-models>`:

.. code-block:: kotlin
    :caption: build.gradle.kts
    :name: cfn-smithy-build-gradle

    plugins {
        java
        id("software.amazon.smithy").version("0.6.0")
    }

    buildscript {
        dependencies {
            classpath("software.amazon.smithy:smithy-aws-cloudformation:__smithy_version__")
        }
    }

The Smithy Gradle plugin relies on a ``smithy-build.json`` file found at the
root of a project to define the actual process of generating the CloudFormation
Resource Schemas. The following example defines a ``smithy-build.json`` file
that generates a CloudFormation Resource Schemas for the specified resource
shapes bound to the ``smithy.example#Queues`` service using the ``Smithy``
organization.

.. code-block:: json
    :caption: smithy-build.json
    :name: cfn-smithy-build-json

    {
        "version": "1.0",
        "plugins": {
            "cloudformation": {
                "service": "smithy.example#Queues",
                "organizationName": "Smithy"
            }
        }
    }

AWS Service teams SHOULD NOT set the ``organizationName`` property, and instead
use the :ref:`cloudFormationName property of the aws.api#service trait
<service-cloudformation-name>`. The following configuration and model would
generate one Resource Schema with the ``typeName`` of ``AWS:Queues:Queue``.

.. code-block:: json
    :caption: smithy-build.json

    {
        "version": "1.0",
        "plugins": {
            "cloudformation": {
                "service": "smithy.example#QueueService",
            }
        }
    }

.. code-block:: smithy
    :caption: model.smithy

    namespace smithy.example

    use aws.api#service

    @service(sdkId: "Queues", cloudFormationName: "Queues")
    service QueueService {
        version: "2020-07-02",
        resources: [Queue],
    }

.. important::

    A buildscript dependency on "software.amazon.smithy:smithy-aws-cloudformation:__smithy_version__" is
    required in order for smithy-build to map the "cloudformation" plugin name
    to the correct Java library implementation.


-------------------------------------
CloudFormation configuration settings
-------------------------------------

The ``cloudformation`` plugin provides configuration options to influence the
Resource Schemas that it generates.

.. tip::

    You typically only need to configure the ``service`` and
    ``organizationName`` settings to generate Resource Schemas.

The following settings are supported:

.. _generate-cloudformation-setting-service:

service (``string``)
    **Required**. The Smithy service :ref:`shape ID <shape-id>` to convert.
    For example, ``smithy.example#Queues``.

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "cloudformation": {
                    "service": "smithy.example#Queues",
                    "organizationName": "Smithy"
                }
            }
        }

.. _generate-cloudformation-setting-organizationName:

organizationName (``string``)
    The ``Organization`` component of the resource's `type name`_. Defaults to
    "AWS" if the :ref:`aws.api#service-trait` is present, otherwise is
    **required**.

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "cloudformation": {
                    "service": "smithy.example#Queues",
                    "organizationName": "Smithy"
                }
            }
        }

.. _generate-cloudformation-setting-serviceName:

serviceName (``string``)
    Allows overriding the ``Service`` component of the resource's `type name`_.
    This value defaults to the :ref:`cloudFormationName property of the
    aws.api#service trait <service-cloudformation-name>` if present, or the
    shape name of the specified service shape otherwise.

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "cloudformation": {
                    "service": "smithy.example#QueueService",
                    "organizationName": "Smithy",
                    "serviceName": "Queues"
                }
            }
        }

.. _generate-cloudformation-setting-externalDocs:

externalDocs (``[string]``)
    Limits the source of generated `"documentationUrl" fields`__ to the
    specified priority ordered list of names in an :ref:`externaldocumentation-trait`.
    This list is case insensitive. By default, this is a list of the following
    values: "Documentation Url", "DocumentationUrl", "API Reference", "User
    Guide", "Developer Guide", "Reference", and "Guide".

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "cloudformation": {
                    "service": "smithy.example#Queues",
                    "organizationName": "Smithy",
                    "externalDocs": [
                        "Documentation Url",
                        "Custom"
                    ]
                }
            }
        }

.. __: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-documentationUrl

.. _generate-cloudformation-setting-sourceDocs:

sourceDocs (``[string]``)
    Limits the source of generated `"sourceUrl" fields`__ to the specified
    priority ordered list of names in an :ref:`externaldocumentation-trait`.
    This list is case insensitive. By default, this is a list of the following
    values: "Source Url", "SourceUrl", "Source", and "Source Code".

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "cloudformation": {
                    "service": "smithy.example#Queues",
                    "organizationName": "Smithy",
                    "sourceDocs": [
                        "Source Url",
                        "Custom"
                    ]
                }
            }
        }

.. __: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-sourceUrl

.. _generate-cloudformation-setting-jsonAdd:

jsonAdd (``Map<String, Map<String, Node>>``)
    Adds or replaces the JSON value in the generated Resource Schemas at the
    given JSON pointer locations with a different JSON value. The value must be
    a map where each key is a resource shape ID. The value is a map where each
    key is a valid JSON pointer string as defined in :rfc:`6901`. Each value in
    the nested map is the JSON value to add or replace at the given target.

    Values are added using similar semantics of the "add" operation of
    JSON Patch, as specified in :rfc:`6902`, with the exception that adding
    properties to an undefined object will create nested objects in the
    result as needed.

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "cloudformation": {
                    "service": "smithy.example#Queues",
                    "organizationName": "Smithy",
                    "jsonAdd": {
                        "smithy.example#Queue": {
                            "/info/title": "Replaced title value",
                            "/info/nested/foo": {
                                "hi": "Adding this object created intermediate objects too!"
                            },
                            "/info/nested/foo/baz": true
                        }
                    }
                }
            }
        }

.. _generate-cloudformation-setting-disableHandlerPermissionGeneration:

disableHandlerPermissionGeneration (``boolean``)
    Sets whether to disable generating ``handler`` ``permission`` lists for
    Resource Schemas. By default, handler permissions lists are automatically
    added to schemas based on :ref:`lifecycle-operations` and permissions
    listed in the :ref:`aws.iam#requiredActions-trait` on the operation. See
    `the handlers section`_ in the CloudFormation Resource Schemas
    documentation for more information.

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "cloudformation": {
                    "service": "smithy.example#Queues",
                    "organizationName": "Smithy",
                    "disableHandlerPermissionGeneration": true
                }
            }
        }

    CloudFormation Resource Schema handlers determine what provisioning actions
    can be performed for the resource. The handlers utilized by CloudFormation
    align with some :ref:`lifecycle-operations`. These operations can also
    define other permission actions required to invoke them with the :ref:`aws.iam#requiredActions-trait`.

    When handler permission generation is enabled, all the actions required to
    invoke the operations related to the handler, including the actions for the
    operations themselves, are used to populate permission lists:

    .. code-block:: json


        "handlers": {
            "create": {
                "permissions": [
                    "dependency:GetDependencyComponent",
                    "queues:CreateQueue"
                ]
            },
            "read": {
                "permissions": [
                    "queues:GetQueue"
                ]
            },
            "update": {
                "permissions": [
                    "dependency:GetDependencyComponent",
                    "queues:UpdateQueue"
                ]
            },
            "delete": {
                "permissions": [
                    "queues:DeleteQueue"
                ]
            },
            "list": {
                "permissions": [
                    "queues:ListQueues"
                ]
            }
        },

.. _generate-cloudformation-setting-disableDeprecatedPropertyGeneration:

disableDeprecatedPropertyGeneration (``boolean``)
    Sets whether to disable generating ``deprecatedProperties`` for Resource
    Schemas. By default, deprecated members are automatically added to the
    ``deprecatedProperties`` schema property. See `the deprecatedProperties
    section`_ in the CloudFormation Resource Schemas documentation for more
    information.

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "cloudformation": {
                    "service": "smithy.example#Queues",
                    "organizationName": "Smithy",
                    "disableDeprecatedPropertyGeneration": true
                }
            }
        }

.. _generate-cloudformation-setting-disableRequiredPropertyGeneration:

disableRequiredPropertyGeneration (``boolean``)
    Sets whether to disable generating ``required`` for Resource Schemas. By
    default, required members are automatically added to the ``required``
    schema property. See `the required property section`_ in the CloudFormation
    Resource Schemas documentation for more information.

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "cloudformation": {
                    "service": "smithy.example#Queues",
                    "organizationName": "Smithy",
                    "disableRequiredPropertyGeneration": true
                }
            }
        }

.. _generate-cloudformation-setting-disableCapitalizedProperties:

disableCapitalizedProperties (``boolean``)
    Sets whether to disable automatically capitalizing names of properties of
    Resource Schemas. By default, property names of resource schemas are
    capitalized if no :ref:`cfnName <aws.cloudformation#cfnName-trait>` trait
    is applied.

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "cloudformation": {
                    "service": "smithy.example#Queues",
                    "organizationName": "Smithy",
                    "disableCapitalizedProperties": true
                }
            }
        }

----------------------------------
JSON schema configuration settings
----------------------------------

.. _generate-cloudformation-jsonschema-setting-defaultTimestampFormat:

defaultTimestampFormat (``string``)
    Sets the assumed :ref:`timestampFormat-trait` value for timestamps with
    no explicit timestampFormat trait. The provided value is expected to be
    a string. Defaults to "date-time" if not set. Can be set to "date-time",
    "epoch-seconds", or "http-date".

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "cloudformation": {
                    "service": "smithy.example#Queues",
                    "organizationName": "Smithy",
                    "defaultTimestampFormat": "epoch-seconds"
                }
            }
        }

.. _generate-cloudformation-jsonschema-setting-schemaDocumentExtensions:

schemaDocumentExtensions (``Map<String, any>``)
    Adds custom top-level key-value pairs to all of the generated
    CloudFormation Resource Schemas. Any existing value is overwritten.

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "cloudformation": {
                    "service": "smithy.example#Queues",
                    "organizationName": "Smithy",
                    "schemaDocumentExtensions": {
                        "x-my-custom-top-level-property": "Hello!",
                        "x-another-custom-top-level-property": {
                            "can be": ["complex", "value", "too!"]
                        }
                    }
                }
            }
        }

.. _generate-cloudformation-jsonschema-setting-disableFeatures:

disableFeatures (``[string]``)
    Disables JSON schema and CloudFormation schema property names from
    appearing in the generated CloudFormation Resource Schemas.

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "cloudformation": {
                    "service": "smithy.example#Queues",
                    "organizationName": "Smithy",
                    "disableFeatures": ["propertyNames"]
                }
            }
        }

.. _generate-cloudformation-other-traits:

--------------------------------------
Other traits that influence generation
--------------------------------------

In addition to the :ref:`AWS CloudFormation traits <aws-cloudformation-traits>`,
the following traits affect the generation of CloudFormation Resource Schemas.

``documentation``
    When applied to a :ref:`resource` shape, the contents will be converted
    into the ``description`` property of the generated Resource Schema.

``externalDocumentation``
    When applied to a :ref:`resource <resource>` shape, the contents will be
    converted according to the :ref:`externalDocs <generate-cloudformation-setting-externalDocs>`
    and :ref:`sourceDocs <generate-cloudformation-setting-sourceDocs>`
    settings.

.. note::

    :ref:`Custom traits <trait-shapes>` defined in a Smithy model are not
    converted and added to CloudFormation Resource Schemas. Doing so requires
    the creation of a custom ``software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Smithy2CfnExtension``.


----------------------------
Generating Schemas with code
----------------------------

Developers that need more advanced control over the generation of
CloudFormation resources from Smithy can use the
``software.amazon.smithy:smithy-aws-cloudformation`` Java library to perform
the generation.

First, you'll need to get a copy of the library. The following example shows
how to install ``software.amazon.smithy:smithy-aws-cloudformation`` through
Gradle:

.. code-block:: kotlin
    :caption: build.gradle.kts
    :name: cfn-code-build-gradle

    buildscript {
        dependencies {
            classpath("software.amazon.smithy:smithy-aws-cloudformation:__smithy_version__")
        }
    }

Next, you need to create and configure a ``CloudFormationConverter``:

.. code-block:: java

    import java.util.List;
    import software.amazon.smithy.model.shapes.ShapeId;
    import software.amazon.smithy.aws.cloudformation.schema.CfnConfig;
    import software.amazon.smithy.aws.cloudformation.schema.fromsmithy.CfnConverter;
    import software.amazon.smithy.aws.cloudformation.schema.model.ResourceSchema;

    CfnConverter converter = CfnConverter.create();

    // Add any necessary configuration settings.
    CfnConfig config = new CfnConfig();
    config.setService(ShapeId.from("smithy.example#Queues"));
    config.setOrganizationName("Smithy");

    // Generate the schemas.
    List<ResourceSchema> schemas = converter.convert(myModel);

The conversion process is highly extensible through
``software.amazon.smithy.aws.cloudformation.schema.fromsmithy.Smithy2CfnExtension``
service providers. See the `Javadocs`_ for more information.

.. _CloudFormation Resource Schemas: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html
.. _CloudFormation: https://aws.amazon.com/cloudformation/
.. _modeling a resource provider: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-types.html
.. _develop the resource provider: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-develop.html
.. _CloudFormation Command Line Interface: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/what-is-cloudformation-cli.html
.. _Smithy Resource: https://smithy.io/1.0/spec/core/model.html#resource
.. _Smithy Gradle plugin: https://github.com/awslabs/smithy-gradle-plugin
.. _type name: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-typeName
.. _Javadocs: https://smithy.io/javadoc/__smithy_version__/software/amazon/smithy/aws/cloudformation/schema/fromsmithy/Smithy2CfnExtension.html
.. _the handlers section: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-handlers
.. _the deprecatedProperties section: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-deprecatedproperties
.. _the required property section: https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-schema.html#schema-properties-required
