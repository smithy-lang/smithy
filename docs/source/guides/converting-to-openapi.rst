============================
Converting Smithy to OpenAPI
============================

This guide describes how Smithy models can be converted to `OpenAPI`_
specifications.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none

------------
Introduction
------------

OpenAPI is a standard for describing REST APIs. While Smithy has it's own
interface definition language that's completely independent of OpenAPI,
there are many use cases for authoring API models in Smithy and converting
them to OpenAPI using both ad-hoc and automated workflows. For example,
integration with `Amazon API Gateway`_, access to OpenAPI tools like
SwaggerUI, or access to OpenAPI client and server code generators when
Smithy generators are not available.

Smithy models can be converted to OpenAPI through smithy-build using the
``openapi`` plugin or through code using the
`software.amazon.smithy:smithy-openapi`_ Java package.

--------------------------------------
Differences between Smithy and OpenAPI
--------------------------------------

Smithy and OpenAPI take very different approaches to modeling APIs. Smithy is
*protocol agnostic*, which means it focuses on the interfaces and abstractions
that are provided to end-users rather than how the data is sent over the wire.
While Smithy can define RESTful APIs, OpenAPI *specializes* in defining *only*
RESTful APIs. Both approaches have their own strengths and weaknesses. For
example, while Smithy can define a much broader set of functionality and
services, it requires abstractions that have their own underlying complexity.
OpenAPI is more permissive in the kinds of services it can describe, making it
easier to adapt to existing web services, but at the same time making it possible
to author APIs that provide a poor customer experience when using clients in
strongly-typed languages.


Unsupported features
====================

Converting a Smithy model to OpenAPI results in a trimmed-down, lossy
representation of a model for a specific HTTP protocol. Various features in
a Smithy model are not currently supported in the OpenAPI conversion:

* :ref:`HTTP prefix headers <httpPrefixHeaders-trait>`: "Prefix headers"
  are used in Smithy to bind all headers under a common prefix into a
  single property of the input or output of an API operation. This can
  be used for things like Amazon S3's `x-amz-meta-* headers`_. OpenAPI
  does not currently support this kind of header.
* :ref:`greedy-labels`: Greedy labels are used in HTTP URIs to act as a
  placeholder for multiple segments of a URI (for example,
  ``/foo/{baz+}/bar``). Some OpenAPI vendors/tooling support greedy labels
  (for example, Amazon API Gateway) while other do not. The converter will
  pass greedy labels through into the OpenAPI document by default, but they
  can be forbidden through the ``openapi.forbidGreedyLabels`` flag.
* :ref:`Event streams <event-streams>`: Event streams are a way of sending
  many different messages over a stream. This is not currently implemented
  in the converter (see `#80 <https://github.com/awslabs/smithy/issues/80>`_).
* Streaming: Smithy allows blob and string shapes to be marked as
  streaming, meaning that their contents should not be loaded into
  memory by clients or servers. This is not currently something supported
  as a built-in feature of OpenAPI (we could potentially add an extension
  to mark a specific type as streaming).
* :ref:`Custom traits <trait-definition>`: Custom traits defined in a Smithy
  model are not converted and added to the OpenAPI specification. Copying
  Smithy traits into OpenAPI as extensions requires the use of a custom
  ``software.amazon.smithy.openapi.fromsmithy.OpenApiExtension``.
* Non-RESTful routing: HTTP routing schemes that aren't based on
  methods and unique URIs are not supported in OpenAPI (for example,
  routing to operations based on a specific header or query string
  parameter).
* Non-HTTP protocols: Protocols that do not send requests over HTTP are
  not supported with OpenAPI (for example, an MQTT-based protocol modeled
  with Smithy would need to also support an HTTP-based protocol to be
  converted to OpenAPI).
* Resources: Smithy resource metadata is not carried over into the OpenAPI
  specification.

---------------------------------------
Converting to OpenAPI with smithy-build
---------------------------------------

The ``openapi`` plugin contained in the ``software.amazon.smithy:smithy-openapi``
package can be used with smithy-build and the `Smithy Gradle plugin`_ to build
OpenAPI specifications from Smithy models.

The following example shows how to configure Gradle to build an OpenAPI
specification from a Smithy model using a buildscript dependency:

.. code-block:: kotlin
    :caption: build.gradle.kts
    :name: smithy-build-gradle

    plugins {
        java
        id("software.amazon.smithy").version("0.4.3")
    }

    buildscript {
        dependencies {
            classpath("software.amazon.smithy:smithy-openapi:0.9.10")
        }
    }

The Smithy Gradle plugin relies on a ``smithy-build.json`` file found at the
root of a project to define the actual process of building the OpenAPI
specification. The following example defines a ``smithy-build.json`` file
that builds an OpenAPI specification from a service for the
``smithy.example#Weather`` service using the ``aws.rest-json-1.1`` protocol:

.. code-block:: json
    :caption: smithy-build.json
    :name: open-api-smithy-build-json

    {
        "version": "1.0",
        "plugins": {
            "openapi": {
                "service": "smithy.example#Weather",
                "protocol": "aws.rest-json-1.1"
            }
        }
    }

.. important::

    A buildscript dependency on "software.amazon.smithy:smithy-openapi:0.9.10" is
    required in order for smithy-build to map the "openapi" plugin name to the
    correct Java library implementation.


OpenAPI configuration settings
==============================

The ``openapi`` plugin is highly configurable to support different OpenAPI
tools and vendors.


.. tip::

    You typically only need to configure the ``service`` and
    ``protocol`` settings to create a valid OpenAPI specification.

The following key-value pairs are supported:

service (string)
    **Required**. The Smithy service :ref:`shape ID <shape-id>` to convert.

protocol (string)
    The protocol name to use when converting Smithy to OpenAPI (for example,
    ``aws.rest-json-1.1``.

    Smithy will try to match the provided protocol name with an implementation
    of ``software.amazon.smithy.openapi.fromsmithy.OpenApiProtocol``
    registered with a service provider implementation of
    ``software.amazon.smithy.openapi.fromsmithy.CoreExtension``.

openapi.tags (boolean)
    Whether or not to include Smithy :ref:`tags <tags-trait>` in the result.

openapi.supportedTags ([string])
    Limits the exported ``openapi.tags`` to a specific set of tags. The value
    must be a list of strings. This property requires that ``openapi.tags``
    is set to ``true`` in order to have an effect.

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "openapi": {
                    "service": "smithy.example#Weather",
                    "openapi.tags": true,
                    "openapi.supportedTags": ["foo", "baz", "bar"]
                }
            }
        }

openapi.defaultBlobFormat (string)
    Sets the default format property used when converting blob shapes in
    Smithy to strings in OpenAPI. Defaults to "byte", meaning Base64 encoded.

openapi.use.xml (boolean)
    Enables converting Smithy XML traits to OpenAPI XML properties. (this
    feature is not yet implemented).

openapi.keepUnusedComponents (boolean)
    Set to ``true`` to prevent unused components from being removed from the
    created specification.

openapi.aws.jsonContentType (string)
    Sets the media-type to associate with the JSON payload of ``aws.json-*``
    and ``aws.rest-json-*`` protocols

openapi.forbidGreedyLabels (boolean)
    Set to true to forbid greedy URI labels. By default, greedy labels will
    appear as-is in the path generated for an operation. For example,
    "/{foo+}".

openapi.onHttpPrefixHeaders (string)
    Specifies what to do when the :ref:`httpPrefixHeaders-trait` is found in
    a model. OpenAPI does not support ``httpPrefixHeaders``. By default, the
    conversion will fail when this trait is encountered, but this behavior
    can be customized using the following values for the ``openapi.onHttpPrefixHeaders``
    setting:

    * FAIL: The default setting that causes the build to fail.
    * WARN: The header is omitted from the OpenAPI model and a warning is logged.

    .. note::

        Additional values may be supported by other mappers or protocols.

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "openapi": {
                    "service": "smithy.example#Weather",
                    "openapi.onHttpPrefixHeaders": "WARN"
                }
            }
        }

openapi.ignoreUnsupportedTrait (boolean)
    Emits warnings rather than failing when unsupported traits like
    ``eventStream`` are encountered.

openapi.disablePrimitiveInlining (boolean)
    Disables the automatic inlining of primitive ``$ref`` targets.

    Inlining these primitive references helps to make the generated
    OpenAPI models more idiomatic while leaving complex types as-is so that
    they support recursive types.

    A *primitive reference* is considered one of the following OpenAPI types:

    * integer
    * number
    * boolean
    * string

    A *primitive collection* is an array that has an "items"  property that
    targets a primitive reference, or an object with no "properties" and an
    "additionalProperties" reference that targets a primitive type.

openapi.substitutions (``Map<String, any>``)
    Defines a map of strings to any JSON value to find and replace in the
    generated OpenAPI model.

    This allows for placeholders to appear in the value of Smithy traits that
    can be converted at build-time to an appropriate value.

    String values are replaced if the string in its entirety matches
    one of the keys provided in the ``openapi.substitutions`` map. The
    corresponding value is then substituted for the string-- this could even
    result in a string changing into an object, array, etc.

    The following example will find all strings with a value of "REPLACE_ME"
    and replace the string with an array value of
    ``["this is a", " replacement"]`` and replace all strings with a value
    of ``ANOTHER_REPLACEMENT`` with ``Hello!!!``:

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "openapi": {
                    "service": "smithy.example#Weather",
                    "openapi.substitutions": {
                        "REPLACE_ME": ["this is a", " replacement"],
                        "ANOTHER_REPLACEMENT": "Hello!!!"
                    }
                }
            }
        }


JSON schema configuration settings
==================================

stripNamespaces (boolean)
    Strips Smithy namespaces from the converted shape ID that is generated
    in the definitions map of a JSON Schema document for a shape. This
    requires that shape names across all namespaces are unique.

includePrivateShapes (boolean)
    Includes shapes marked with the :ref:`private-trait`.

useJsonName (boolean)
    Uses the value of the :ref:`jsonName-trait` when creating JSON schema
    properties for structure and union shapes.

    TODO: This is enabled automatically with AWS protocols?

defaultTimestampFormat (string)
    Sets the assumed :ref:`timestampFormat-trait` value for timestamps with
    no explicit timestampFormat trait. The provided value is expected to be
    a string. Defaults to "date-time" if not set. Can be set to "date-time",
    "epoch-seconds", or "http-date".

unionStrategy (string)
    Configures how Smithy union shapes are converted to JSON Schema.

    This property must be a string set to one of the following values:

    * oneOf: Converts to a schema that uses "oneOf". This is the
      default setting used if not configured.
    * object: Converts to an empty object "{}".
    * structure: Converts to an object with properties just like a
      structure.

schemaDocumentExtensions (``Map<String, any>``)
    Adds custom top-level key-value pairs to the created OpenAPI specification.


Amazon API Gateway extensions
=============================

Smithy models can be converted to OpenAPI specifications that contain
`Amazon API Gateway extensions`_ for defining things like integrations. These
API Gateway extensions are automatically picked up by Smithy by adding a
dependency on ``software.amazon.smithy:smithy-aws-apigateway-openapi``.

.. code-block:: kotlin
    :caption: build.gradle.kts
    :name: apigateway-build-gradle

    buildscript {
        dependencies {
            classpath("software.amazon.smithy:smithy-aws-apigateway-openapi:0.9.10")
        }
    }


Binary types
------------

The list of binary media types used by an API need to be specified for
API Gateway in a top-level extension named `x-amazon-apigateway-binary-media-types`_.
Smithy will automatically detect every media type used in a service by
collecting all of the :ref:`mediaType-trait` values for all members marked
with :ref:`httppayload-trait`.


.. _apigateway-request-validators:

Request validators
------------------

Amazon API Gateway can perform request validation before forwarding a request
to an integration. You can opt-in to this feature using the
``aws.apigateway#requestValidator`` trait.

Smithy will populate the value of the `x-amazon-apigateway-request-validators`_
and `x-amazon-apigateway-request-validator`_ OpenAPI extensions using the
``aws.apigateway#requestValidator`` traits found in a service. The
``aws.apigateway#requestValidator`` trait can be applied to a service to
enable a specific kind of request validation on all operations within a
service. It can also be applied to an operation to set a specific validator
for the operation.

Smithy defines the following canned request validators:

full
    Creates a request validator configured as

    .. code-block:: json

        {
            "validateRequestBody": true,
            "validateRequestParameters": true
        }

params-only
    Creates a request validator configured as

    .. code-block:: json

        {
            "validateRequestBody": false,
            "validateRequestParameters": true
        }

body-only
    Creates a request validator configured as

    .. code-block:: json

        {
            "validateRequestBody": true,
            "validateRequestParameters": false
        }

Smithy will gather all of the utilized request validators and add their
declarations in a top-level ``x-amazon-apigateway-request-validators``
OpenAPI extension.


.. _apigateway-integrations:

Integrations
------------

Smithy models can specify the backend integration configuration that
Amazon API Gateway uses to for an operation.

* ``aws.apigateway#integration`` trait defines an API Gateway integration
  that calls an actual backend.
* ``aws.apigateway#mockIntegration`` defines an API Gateway mock integration
  that doesn't call a backend.

If the trait is applied to a service shape, then all operations in the service
use the integration. If the trait is defined on a resource shape, then all
operations of the resource and all child resources use the integration. If
the trait is applied to an operation, then the operation uses a specific
integration that overrides any integration inherited from a resource or
service.


CORS functionality
------------------

TODO


Security schemes
----------------

TODO


AWS CloudFormation substitutions
--------------------------------

OpenAPI specifications used with Amazon API Gateway are commonly deployed
through AWS CloudFormation. Values within an OpenAPI specification for things
like the region a service is deployed and resources used within the service
are often unknown until deployment-time. CloudFormation offers the ability
to use `intrinsic functions`_ in a JSON document to resolve, find, and
replace this unknown data at deployment-time.

When the ``software.amazon.smithy:smithy-aws-apigateway-openapi`` library
is loaded on the classpath, Smithy will treat specific, well-known parts
of an OpenAPI specification as an `Fn::Sub`_. This allows Smithy models
to refer to variables that aren't available until a stack is created
using the format of ``${x}`` where "x" is the variable name.

Smithy will automatically wrap the following locations of an OpenAPI
specification in an ``Fn::Sub`` if the value contained in the location
uses the ``Fn::Sub`` variable syntax (``*`` means any value):

- ``components/securitySchemes/*/x-amazon-apigateway-authorizer/providerARNs/*``
- ``components/securitySchemes/*/x-amazon-apigateway-authorizer/authorizerCredentials``
- ``components/securitySchemes/*/x-amazon-apigateway-authorizer/authorizerUri``
- ``paths/*/*/x-amazon-apigateway-integration/connectionId``
- ``paths/*/*/x-amazon-apigateway-integration/credentials``
- ``paths/*/*/x-amazon-apigateway-integration/uri``

.. note::

    This functionality can be disabled by setting the ``apigateway.disableCloudFormationSubstitution``
    OpenAPI configuration property to ``true``.


Amazon Cognito user pools
-------------------------

TODO


Other traits that influence API Gateway
---------------------------------------

``aws.api#service``
    TODO

``protocols``
    TODO

``aws.apigateway#apiKeySource``
    Specifies the source of the caller identifier that will be used to
    throttle API methods that require a key. This trait is converted into
    the `x-amazon-apigateway-api-key-source`_ OpenAPI extension.

``aws.apigateway#authorizers``
    Lambda authorizers to attach to the authentication schemes defined on
    this service.

    TODO: add more information

-------------------------------
Converting to OpenAPI with code
-------------------------------

Developers that need more advanced control over the Smithy to OpenAPI
conversion can use the ``software.amazon.smithy:smithy-openapi`` Java library
to perform the conversion.

First, you'll need to get a copy of the library. The following example
shows how to install ``software.amazon.smithy:smithy-openapi`` through Gradle:

.. code-block:: kotlin
    :caption: build.gradle.kts
    :name: code-build-gradle

    buildscript {
        dependencies {
            classpath("software.amazon.smithy:smithy-openapi:0.9.10")
        }
    }

Next, you need to create and configure an ``OpenApiConverter``:

.. code-block:: java

    import software.amazon.smithy.model.shapes.ShapeId;
    import software.amazon.smithy.openapi.fromsmithy.OpenApiConstants;
    import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
    import software.amazon.smithy.openapi.fromsmithy.model.OpenApi;

    OpenApiConverter converter = OpenApiConverter.create();

    // Add any necessary settings...
    converter.putSetting(OpenApiConstants.PROTOCOL, "aws.rest-json-1.1");

    // Create a shape ID that points to the service.
    ShapeId serviceShapeId = ShapeId.from("smithy.example#Weather");

    OpenApi result = converter.convert(myModel, serviceShapeId);

The conversion process is highly extensible through
``software.amazon.smithy.openapi.fromsmithy.CoreExtension``
`service providers`_. See the Javadocs for more information.

.. _OpenAPI: https://github.com/OAI/OpenAPI-Specification
.. _Amazon API Gateway: https://aws.amazon.com/api-gateway/
.. _software.amazon.smithy:smithy-openapi: https://search.maven.org/search?q=g:software.amazon.smithy%20and%20a:smithy-openapi
.. _x-amz-meta-* headers: https://docs.aws.amazon.com/AmazonS3/latest/API/RESTObjectPUT.html
.. _Amazon API Gateway extensions: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions.html
.. _service providers: https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html
.. _Smithy Gradle plugin: https://github.com/awslabs/smithy-gradle-plugin
.. _x-amazon-apigateway-binary-media-types: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-binary-media-types.html
.. _x-amazon-apigateway-request-validators: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-request-validators.html
.. _x-amazon-apigateway-request-validator: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-request-validator.html
.. _intrinsic functions: https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference.html
.. _`Fn::Sub`: https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-sub.html
.. _x-amazon-apigateway-api-key-source: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-api-key-source.html
