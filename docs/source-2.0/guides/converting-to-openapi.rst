.. _smithy-to-openapi:

============================
Converting Smithy to OpenAPI
============================

This guide describes how Smithy models can be converted to `OpenAPI`_
specifications.

------------
Introduction
------------

OpenAPI is a standard for describing RESTful APIs. While Smithy has it's own
interface definition language that's completely independent of OpenAPI,
there are use cases for authoring API models in Smithy and converting
them to OpenAPI using both ad-hoc and automated workflows. For example,
integration with `Amazon API Gateway`_, access to OpenAPI tools like
SwaggerUI, or access to OpenAPI client and server code generators when
Smithy generators are not available.

Smithy models can be converted to OpenAPI through smithy-build using the
``openapi`` plugin or through code using the
`software.amazon.smithy:smithy-openapi`_ Java package.

.. note::

    The Smithy ``openapi`` plugin currently supports OpenAPI 3.0.2 and OpenAPI 3.1.0.


--------------------------------------
Differences between Smithy and OpenAPI
--------------------------------------

Smithy and OpenAPI take very different approaches to modeling APIs. Smithy is
*protocol agnostic*, which means it focuses on the interfaces and abstractions
that are provided to end-users rather than how the data is sent over the wire.
While Smithy can define RESTful APIs, RPC APIs, pub/sub APIs, and more, OpenAPI
*only* defines RESTful APIs. Both approaches have their own strengths and
weaknesses. For example, while Smithy can define a much broader set of
functionality, services, and client behaviors, it requires abstractions that
have their own underlying complexity. OpenAPI is more permissive in the kinds
of services it can describe, making it easier to adapt to existing web
services, but at the same time making it easier to author APIs that provide
a poor customer experience when using clients in strongly-typed languages.


Unsupported features
====================

Converting a Smithy model to OpenAPI is a lossy conversion. Various features
in a Smithy model are not currently supported in the OpenAPI conversion.

**Unsupported features**

* :ref:`endpoint-trait` and :ref:`hostLabel-trait`: These traits are used
  to dynamically alter the endpoint of an operation based on input. They
  are not supported in OpenAPI.
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
  can be forbidden through the ``forbidGreedyLabels`` flag.
* Non-RESTful routing: HTTP routing schemes that aren't based on
  methods and unique URIs are not supported in OpenAPI (for example,
  routing to operations based on a specific header or query string
  parameter).
* Non-HTTP protocols: Protocols that do not send requests over HTTP are
  not supported with OpenAPI (for example, an MQTT-based protocol modeled
  with Smithy would need to also support an HTTP-based protocol to be
  converted to OpenAPI).
* :ref:`aws.api#arn-trait`: This trait does not influence resources defined
  in the OpenAPI model natively, nor via any of the `Amazon API Gateway extensions`_.

  .. seealso:: :ref:`other-traits`

**Compatibility notes**

* Streaming: Smithy allows blob and string shapes to be marked as
  streaming, meaning that their contents should not be loaded into
  memory by clients or servers. While this isn't technically unsupported in
  OpenAPI, some vendors like API Gateway do not currently support streaming
  large payloads.

**Lossy metadata**

* Resources: Smithy resource metadata is not carried over into the OpenAPI
  specification.
* :ref:`Custom traits <trait-shapes>`: Custom traits defined in a Smithy
  model are not converted and added to the OpenAPI specification. Copying
  Smithy traits into OpenAPI as extensions requires the use of a custom
  ``software.amazon.smithy.openapi.fromsmithy.OpenApiExtension``.


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
    :name: openapi-smithy-build-gradle

    plugins {
        java
        id("software.amazon.smithy").version("0.6.0")
    }

    buildscript {
        dependencies {
            classpath("software.amazon.smithy:smithy-openapi:__smithy_version__")
            // The openapi plugin configured in the smithy-build.json example below
            // uses the restJson1 protocol defined in the aws-traits package. This
            // additional dependency must added to use that protocol.
            classpath("software.amazon.smithy:smithy-aws-traits:__smithy_version__")
        }
    }

    dependencies {
        // The dependency for restJson1 is required here too.
        implementation("software.amazon.smithy:smithy-aws-traits:__smithy_version__")
    }

The Smithy Gradle plugin relies on a ``smithy-build.json`` file found at the
root of a project to define the actual process of building the OpenAPI
specification. The following example defines a ``smithy-build.json`` file
that builds an OpenAPI specification from a service for the
``example.weather#Weather`` service using the ``aws.protocols#restJson1`` protocol:

.. code-block:: json
    :caption: smithy-build.json
    :name: open-api-smithy-build-json

    {
        "version": "2.0",
        "plugins": {
            "openapi": {
                "service": "example.weather#Weather",
                "protocol": "aws.protocols#restJson1"
            }
        }
    }

.. important::

    A buildscript dependency on "software.amazon.smithy:smithy-openapi:__smithy_version__" is
    required in order for smithy-build to map the "openapi" plugin name to the
    correct Java library implementation.


------------------------------
OpenAPI configuration settings
------------------------------

The ``openapi`` plugin is highly configurable to support different OpenAPI
tools and vendors.

.. tip::

    You typically only need to configure the ``service`` and
    ``protocol`` settings to create a valid OpenAPI specification.

The following key-value pairs are supported:

.. _generate-openapi-setting-service:

service (``string``)
    **Required**. The Smithy service :ref:`shape ID <shape-id>` to convert.
    For example, ``example.weather#Weather``.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather"
                }
            }
        }

    .. note::

        Any :ref:`rename <service-closure>` defined in the given service
        affects the generated schema names when converting to OpenAPI.

.. _generate-openapi-setting-protocol:

protocol (``string``)
    The protocol shape ID to use when converting Smithy to OpenAPI.
    For example, ``aws.protocols#restJson1``.

    .. important::

        * ``protocol`` is required if a service supports multiple protocols.
        * A Smithy model can only be converted to OpenAPI if a corresponding
          ``software.amazon.smithy.openapi.fromsmithy.OpenApiProtocol``
          implementation is registered by a ``software.amazon.smithy.openapi.fromsmithy.CoreExtension``
          service provider found on the classpath.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "protocol": "aws.protocols#restJson1"
                }
            }
        }

.. _generate-openapi-setting-version:

version (``string``)
    Specifies the OpenAPI specification version.
    Currently supports OpenAPI 3.0.2 and OpenAPI 3.1.0.
    This option defaults to ``3.0.2``.

    .. note::
        The JSON schema version used for model schemas is the latest JSON schema
        version supported by the specified OpenAPI version. For example, OpenAPI version
        ``3.1.0`` will use JSON schema version ``draft2020-12`` for model schemas.

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "openapi": {
                    "service": "smithy.example#Weather",
                    "version": "3.1.0"
                }
            }
        }

.. _generate-openapi-setting-tags:

tags (``boolean``)
    Whether or not to include Smithy :ref:`tags <tags-trait>` in the result
    as `OpenAPI tags`_. The following example adds all tags in the Smithy
    model to the OpenAPI model.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "tags": true
                }
            }
        }

.. _generate-openapi-setting-supportedTags:

supportedTags (``[string]``)
    Limits the exported ``tags`` to a specific set of tags. The value
    must be a list of strings. This property requires that ``tags`` is set to
    ``true`` in order to have an effect.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "tags": true,
                    "supportedTags": ["foo", "baz", "bar"]
                }
            }
        }

.. _generate-openapi-setting-defaultBlobFormat:

defaultBlobFormat (``string``)
    Sets the default format property used when converting blob shapes in
    Smithy to strings in OpenAPI. Defaults to "byte", meaning Base64 encoded.
    See `OpenAPI Data types`_ for more information.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "defaultBlobFormat": "byte"
                }
            }
        }

.. _generate-openapi-setting-externalDocs:

externalDocs (``[string]``)
    Limits the source of converted "externalDocs" fields to the specified
    priority ordered list of names in an :ref:`externaldocumentation-trait`.
    This list is case insensitive. By default, this is a list of the following
    values: "Homepage", "API Reference", "User Guide", "Developer Guide",
    "Reference", and "Guide".

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "externalDocs": [
                        "Homepage",
                        "Custom"
                    ]
                }
            }
        }

.. _generate-openapi-setting-keepUnusedComponents:

keepUnusedComponents (``boolean``)
    Set to ``true`` to prevent unused OpenAPI ``components`` from being
    removed from the created specification.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "keepUnusedComponents": true
                }
            }
        }

.. _generate-openapi-setting-jsonContentType:

jsonContentType (``string``)
    Sets a custom media-type to associate with the JSON payload of
    JSON-based protocols.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "jsonContentType": "application/x-amz-json-1.1"
                }
            }
        }

.. _generate-openapi-setting-forbidGreedyLabels:

forbidGreedyLabels (``boolean``)
    Set to true to forbid greedy URI labels. By default, greedy labels will
    appear as-is in the path generated for an operation. For example,
    "/{foo+}".

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "forbidGreedyLabels": true
                }
            }
        }

.. _generate-openapi-setting-removeGreedyParameterSuffix:

removeGreedyParameterSuffix (``boolean``)
    Set to true to remove the ``+`` suffix on the parameter name. By default, greedy
    labels will have a corresponding parameter name generated that will include
    the ``+`` suffix. Given a label "/{foo+}", the parameter name will be "foo+".
    If enabled, the parameter name will instead be "foo".

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "removeGreedyParameterSuffix": true
                }
            }
        }

.. _generate-openapi-setting-onHttpPrefixHeaders:

onHttpPrefixHeaders (``string``)
    Specifies what to do when the :ref:`httpPrefixHeaders-trait` is found in
    a model. OpenAPI does not support ``httpPrefixHeaders``. By default, the
    conversion will fail when this trait is encountered, but this behavior
    can be customized using the following values for the ``onHttpPrefixHeaders``
    setting:

    * FAIL: The default setting that causes the build to fail.
    * WARN: The header is omitted from the OpenAPI model and a warning is logged.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "onHttpPrefixHeaders": "WARN"
                }
            }
        }

.. _generate-openapi-setting-ignoreUnsupportedTraits:

ignoreUnsupportedTraits (``boolean``)
    Emits warnings rather than failing when unsupported traits like
    ``endpoint`` and ``hostLabel`` are encountered.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "ignoreUnsupportedTraits": true
                }
            }
        }

.. _generate-openapi-setting-substitutions:

substitutions (``Map<String, any>``)
    Defines a map of strings to any JSON value to find and replace in the
    generated OpenAPI model.

    String values are replaced if the string in its entirety matches
    one of the keys provided in the ``substitutions`` map. The
    corresponding value is then substituted for the string; this could
    even result in a string changing into an object, array, etc.

    The following example will find all strings with a value of "REPLACE_ME"
    and replace the string with an array value of
    ``["this is a", " replacement"]`` and replace all strings with a value
    of ``ANOTHER_REPLACEMENT`` with ``Hello!!!``:

    .. warning::

        When possible, prefer ``jsonAdd`` instead because the update
        performed on the generated document is more explicit and resilient to
        change.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "substitutions": {
                        "REPLACE_ME": ["this is a", " replacement"],
                        "ANOTHER_REPLACEMENT": "Hello!!!"
                    }
                }
            }
        }

.. _generate-openapi-setting-jsonAdd:

jsonAdd (``Map<String, Node>``)
    Adds or replaces the JSON value in the generated OpenAPI document at the
    given JSON pointer locations with a different JSON value. The value must
    be a map where each key is a valid JSON pointer string as defined in
    :rfc:`6901`. Each value in the map is the JSON value to add or replace
    at the given target.

    Values are added using similar semantics of the "add" operation of
    JSON Patch, as specified in :rfc:`6902`, with the exception that adding
    properties to an undefined object will create nested objects in the
    result as needed.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "jsonAdd": {
                        "/info/title": "Replaced title value",
                        "/info/nested/foo": {
                            "hi": "Adding this object created intermediate objects too!"
                        },
                        "/info/nested/foo/baz": true
                    }
                }
            }
        }

.. _generate-openapi-setting-useIntegerType:

useIntegerType (``boolean``)
    Set to true to use the "integer" type when converting ``byte``, ``short``,
    ``integer``, and ``long`` shapes to OpenAPI. Configuring this setting to
    true, like the example below, is recommended.

    By default, these shape types are converted to OpenAPI with a type of
    "number".

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "useIntegerType": true
                }
            }
        }


.. _generate-openapi-setting-disableIntegerFormat:

disableIntegerFormat (``boolean``)
    Set to true to disable setting the ``format`` property when using the
    "integer" type that is enabled by the :ref:`useIntegerType <generate-openapi-setting-useIntegerType>`
    configuration setting.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "useIntegerType": true,
                    "disableIntegerFormat": true
                }
            }
        }

    With this enabled (the default), the ``format`` property is set to ``int32``
    or ``int64`` for Integer or Long shapes respectively.

    .. code-block:: json

        {
            "Foo": {
                "type": "object",
                "properties": {
                    "myInteger": {
                        "type": "integer",
                        "format": "int32"
                    },
                    "myLong": {
                        "type": "integer",
                        "format": "int64"
                    }
                }
            }
        }

.. _generate-openapi-setting-onErrorStatusConflict:

onErrorStatusConflict (``String``)
    Specifies how to resolve multiple error responses that share a same HTTP status code.
    This behavior can be customized using the following values for the ``onErrorStatusConflict`` setting:

    ``oneOf``
        Use OpenAPI's ``oneOf`` keyword to combine error responses with same HTTP status code. The ``oneOf`` option
        wraps schemas for contents of conflicting errors responses schemas into a synthetic union schema using
        OpenAPI's ``oneOf`` keyword.
    ``properties``
        Use ``properties`` field of OpenAPI schema object to combine error responses with same HTTP status code.
        The ``properties`` option combines the conflicting error structure shapes into one union error shape that
        contains all members from each and every conflicting error.

    .. note::
            ``oneOf`` keyword is not supported by Amazon API Gateway.

    Both options generate a single combined response object called "UnionError XXX Response" in the
    OpenAPI model output, where "XXX" is the status code shared by multiple errors. Both options drop
    the ``@required`` trait from all members of conflicting error structures, making them optional.

    .. warning::
        When using ``properties`` option, make sure that conflicting error structure shapes do not have member(s)
        that have same name while having different target shapes. If member shapes with same name
        (in conflicting error structures) target
        different shapes, error shapes will not be able to be merged into one union error shape, and
        an exception will be thrown.

    .. warning::
        Regardless of the setting, an exception will be thrown if any one of conflicting error structure shape
        has a member shape with ``@httpPayload`` trait.

    By default, this setting is set to ``oneOf``.

    .. code-block:: json

        {
            "version": "1.0",
            "plugins": {
                "openapi": {
                    "service": "smithy.example#Weather",
                    "onErrorStatusConflict": "properties"
                }
            }
        }

----------------------------------
JSON schema configuration settings
----------------------------------

.. _generate-openapi-jsonschema-setting-alphanumericOnlyRefs:

alphanumericOnlyRefs (``boolean``)
    Creates JSON schema names that strip out non-alphanumeric characters.

    This is necessary for compatibility with some vendors like
    Amazon API Gateway that only allow alphanumeric shape names.

    .. note::

        This setting is enabled by default when
        ``software.amazon.smithy:smithy-aws-apigateway-openapi`` is on the classpath
        and ``apiGatewayType`` is not set to ``DISABLED``.

.. _generate-openapi-jsonschema-setting-useJsonName:

useJsonName (``boolean``)
    Uses the value of the :ref:`jsonName-trait` when creating JSON schema
    properties for structure and union shapes. This property MAY be
    automatically set to ``true`` depending on the protocol being converted.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "useJsonName": true
                }
            }
        }

.. _generate-openapi-jsonschema-setting-defaultTimestampFormat:

defaultTimestampFormat (``string``)
    Sets the assumed :ref:`timestampFormat-trait` value for timestamps with
    no explicit timestampFormat trait. The provided value is expected to be
    a string. Defaults to "date-time" if not set. Can be set to "date-time",
    "epoch-seconds", or "http-date".

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "defaultTimestampFormat": "epoch-seconds"
                }
            }
        }

.. _generate-openapi-jsonschema-setting-unionStrategy:

unionStrategy (``string``)
    Configures how Smithy union shapes are converted to JSON Schema.

    This property must be a string set to one of the following values:

    * ``oneOf``: Converts to a schema that uses "oneOf". This is the
      default setting used if not configured.
    * ``object``: Converts to an empty object "{}".
    * ``structure``: Converts to an object with properties just like a
      structure.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "unionStrategy": "oneOf"
                }
            }
        }

.. _generate-openapi-jsonschema-setting-mapStrategy:

mapStrategy (``string``)
    Configures how Smithy map shapes are converted to JSON Schema.

    This property must be a string set to one of the following values:

    * ``propertyNames``:  Converts to a schema that uses a combination of
      "propertyNames" and "additionalProperties". This is the default setting
      used if not configured.
    * ``patternProperties``: Converts to a schema that uses
      "patternProperties". If a map's key member or its target does not have a
      "pattern" trait, a default indicating one or more of any character (".+")
      is applied.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "mapStrategy": "propertyNames"
                }
            }
        }

.. _generate-openapi-jsonschema-setting-schemaDocumentExtensions:

schemaDocumentExtensions (``Map<String, any>``)
    Adds custom top-level key-value pairs to the created OpenAPI specification.
    Any existing value is overwritten.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "schemaDocumentExtensions": {
                        "x-my-custom-top-level-property": "Hello!",
                        "x-another-custom-top-level-property": {
                            "can be": ["complex", "value", "too!"]
                        }
                    }
                }
            }
        }

.. _generate-openapi-jsonschema-setting-disableFeatures:

disableFeatures (``[string]``)
    Disables JSON schema and OpenAPI property names from appearing in the
    generated OpenAPI model.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "disableFeatures": ["propertyNames"]
                }
            }
        }


.. _generate-openapi-setting-supportNonNumericFloats:

supportNonNumericFloats (``boolean``)
    Set to true to add support for NaN, Infinity, and -Infinity in float
    and double shapes. These values will be serialized as strings. The
    JSON Schema document will be updated to refer to them as a "oneOf" of
    number and string.

    By default, these non-numeric values are not supported.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "supportNonNumericFloats": true
                }
            }
        }

    When this is disabled (the default), references to floats/doubles will
    look like this:

    .. code-block:: json

        {
            "floatMember": {
                "type": "number"
            }
        }

    With this enabled, references to floats/doubles will look like this:

    .. code-block:: json

        {
            "floatMember": {
                "oneOf": [
                    {
                        "type": "number"
                    },
                    {
                        "type": "string",
                        "enum": [
                            "NaN",
                            "Infinity",
                            "-Infinity"
                        ]
                    }
                ]
            }
        }


.. _generate-openapi-setting-disableDefaultValues:

disableDefaultValues (``boolean``)
    Set to true to disable adding default values.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "disableDefaultValues": true
                }
            }
        }

    With this disabled, default values will not appear in the output:

    .. code-block:: json

        {
            "Foo": {
                "type": "object",
                "properties": {
                    "bam": {
                        "type": "array",
                        "items": {
                            "type": "string"
                        }
                    },
                    "bar": {
                        "type": "number"
                    },
                    "bat": {
                        "$ref": "#/definitions/MyEnum"
                    },
                    "baz": {
                        "type": "string"
                    }
                }
            }
        }

    With this enabled (the default), default values will be added, with ``$ref``
    pointers wrapped in an ``allOf``:

    .. code-block:: json

        {
            "Foo": {
                "type": "object",
                "properties": {
                    "bam": {
                        "type": "array",
                        "items": {
                            "type": "string"
                        },
                        "default": []
                    },
                    "bar": {
                        "type": "number",
                        "default": 0
                    },
                    "bat": {
                        "allOf": [
                            {
                                "$ref": "#/definitions/MyEnum"
                            },
                            {
                                "default": "FOO"
                            }
                        ]
                    },
                    "baz": {
                        "type": "string",
                        "default": ""
                    }
                }
            }
        }


.. _generate-openapi-setting-disableIntEnums:

disableIntEnums (``boolean``)
    Set to true to disable setting the ``enum`` property for intEnum shapes.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "disableIntEnums": true
                }
            }
        }

    With this disabled, intEnum shapes will be inlined and the ``enum`` property
    will not be set:

    .. code-block:: json

        {
            "Foo": {
                "type": "object",
                "properties": {
                    "bar": {
                        "type": "number"
                    }
                }
            }
        }

    With this enabled (the default), intEnum shapes will have the ``enum``
    property set and the schema will use a ``$ref``.

    .. code-block:: json

        {
            "Foo": {
                "type": "object",
                "properties": {
                    "bar": {
                        "$ref": "#/definitions/MyIntEnum"
                    }
                }
            },
            "MyIntEnum": {
                "type": "number",
                "enum": [
                    1,
                    2
                ]
            }
        }


----------------
Security schemes
----------------

Smithy :ref:`authentication traits <authentication-traits>` applied to a service,
resource, or operation are converted to `OpenAPI security schemes`_ that are
defined and attached to the corresponding OpenAPI definitions.

Smithy will look for service providers on the classpath that implement
``software.amazon.smithy.openapi.fromsmithy.Smithy2OpenApiExtension``. These
service providers register ``software.amazon.smithy.openapi.fromsmithy.SecuritySchemeConverter``
implementations used to convert Smithy authentication traits to
OpenAPI security schemes.

Smithy provides built-in support for the following authentication traits:

* :ref:`aws.auth#sigv4 <aws.auth#sigv4-trait>`
* :ref:`httpApiKeyAuth <httpApiKeyAuth-trait>`
* :ref:`httpBasicAuth <httpBasicAuth-trait>`
* :ref:`httpBearerAuth <httpBearerAuth-trait>`
* :ref:`httpDigestAuth <httpDigestAuth-trait>`

For example, given the following Smithy model:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    use aws.protocols#restJson1

    @restJson1
    @httpApiKeyAuth(name: "x-api-key", in: "header")
    service Foo {
        version: "2006-03-01"
        operations: [ExampleOperation]
    }

    @http(method: "GET", uri: "/")
    operation ExampleOperation {}

Smithy will generate the following OpenAPI model:

.. code-block:: json

    {
        "openapi": "3.0.2",
        "info": {
            "title": "Foo",
            "version": "2006-03-01"
        },
        "paths": {
            "/": {
                "get": {
                    "operationId": "ExampleOperation",
                    "responses": {
                        "200": {
                            "description": "ExampleOperation response"
                        }
                    }
                }
            }
        },
        "components": {
            "securitySchemes": {
                "smithy.api#httpApiKeyAuth": {
                    "type": "apiKey",
                    "name": "x-api-key",
                    "in": "header"
                }
            }
        },
        "security": [
            {
                "smithy.api#httpApiKeyAuth": [ ]
            }
        ]
    }

--------------------------------
``@examples`` trait conversion
--------------------------------

In Smithy, example values of input structure members and the corresponding
output or error structure members for an operation are grouped together
into one set of example values for an operation. Below is an example "unit" of ``FooOperation``
operation shape, which shows this logical grouping.

.. code-block:: smithy

    apply FooOperation @examples(
        [
            {
                title: "valid example",
                documentation: "valid example doc",
                input: {
                    bar: "1234"
                },
                output: {
                    baz: "5678"
                },
            }
        ]
    )

However, example values in OpenAPI are scattered throughout the model, with each example value
contained by the OpenAPI object the example value is for.
The following is an example OpenAPI model for the above Smithy example value.

.. code-block:: json

        "paths": {
            "/": {
                "get": {
                    "operationId": "FooOperation",
                    "requestBody": {
                        "content": {
                            "application/json": {
                                "examples": {
                                    "FooOperation_example1": {
                                        "summary": "valid example",
                                        "description": "valid example doc",
                                        "value": {
                                            "bar": "1234"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "responses": {
                        "200": {
                            "description": "FooOperation response",
                            "content": {
                                "application/json": {
                                    "examples": {
                                        "FooOperation_example1": {
                                            "summary": "valid example",
                                            "description": "valid example doc",
                                            "value": {
                                                "baz": "5678"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


-----------------------------
Amazon API Gateway extensions
-----------------------------

Smithy models can be converted to OpenAPI specifications that contain
`Amazon API Gateway extensions`_ for defining things like
:ref:`integrations <aws.apigateway#integration-trait>` . These
API Gateway extensions are automatically picked up by Smithy by adding a
dependency on ``software.amazon.smithy:smithy-aws-apigateway-openapi``.

.. code-block:: kotlin
    :caption: build.gradle.kts
    :name: apigateway-build-gradle

    buildscript {
        dependencies {
            classpath("software.amazon.smithy:smithy-aws-apigateway-openapi:__smithy_version__")
        }
    }


Amazon API Gateway configuration settings
=========================================

apiGatewayType (``string``)
    Defines the type of API Gateway to define in the generated OpenAPI model.
    This setting influences which API Gateway specific plugins apply
    to the generated OpenAPI model.

    This setting can be set to one of the following:

    * ``REST``: Generates a `REST API`_. This is the default setting if not
      configured.
    * ``HTTP``: Generates an `HTTP API`_.
    * ``DISABLED``: Disables all API Gateway modifications made to the
      OpenAPI model. This is useful if ``software.amazon.smithy:smithy-aws-apigateway-openapi``
      is inadvertently placed on the classpath by a dependency.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "apiGatewayType": "REST"
                }
            }
        }

disableCloudFormationSubstitution (``boolean``)
    Disables automatically converting ``${}`` templates in specific properties
    into CloudFormation intrinsic functions.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "disableCloudFormationSubstitution": true
                }
            }
        }

    .. seealso:: :ref:`openapi-cfn-substitutions`


additionalAllowedCorsHeaders (``[string]``)
    Sets additional allowed CORS headers on the preflight requests. If this
    option is not set, the default ``amz-sdk-invocation-id`` and ``amz-sdk-request``
    headers will be added. By setting this option to an empty array, those default
    headers will be omitted.

    .. code-block:: json

        {
            "version": "2.0",
            "plugins": {
                "openapi": {
                    "service": "example.weather#Weather",
                    "additionalAllowedCorsHeaders": ["foo-header", "bar-header"]
                }
            }
        }

Binary types
============

The list of binary media types used by an API need to be specified for
API Gateway in a top-level extension named `x-amazon-apigateway-binary-media-types`_.
Smithy will automatically detect every media type used in a service by
collecting all of the :ref:`mediaType-trait` values for all members marked
with :ref:`httppayload-trait`.


.. _apigateway-request-validators:

Request validators
==================

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
============

Smithy models can specify the backend integration configuration that
Amazon API Gateway uses to for an operation.

* The :ref:`aws.apigateway#integration-trait` defines an API Gateway
  integration that calls an actual backend.
* The :ref:`aws.apigateway#mockIntegration-trait` defines an API Gateway mock
  integration that doesn't call a backend.

If either of the above traits are applied to a service shape, then all
operations in the service inherit the applied integration. If either trait is
applied to a resource shape, then all operations of the resource and all child
resources inherit the applied integration. If either trait is applied to an
operation, then the operation uses a specific integration that overrides any
integration inherited from a resource or service.


CORS functionality
==================

When the ``smithy.api#cors`` trait is applied to a service and
``apiGatewayType`` is set to ``REST``, then Smithy performs the following
additions during the OpenAPI conversion:

* Adds CORS-preflight OPTIONS requests using mock API Gateway integrations.
* Adds CORS-specific headers to every response in the API, including ``Access-Control-Allow-Origin``,
  ``Access-Control-Expose-Headers``, and ``Access-Control-Allow-Credentials`` where appropriate.
* Adds static CORS response headers to API Gateway "gateway" responses.  These are added only when
  no gateway responses are defined in the OpenAPI model.


.. _authorizers:

Authorizers
===========

The `x-amazon-apigateway-authorizer`_ security scheme extension is added
using the :ref:`aws.apigateway#authorizers-trait` and
:ref:`aws.apigateway#authorizer-trait`.

The ``aws.apigateway#authorizers`` trait defines `Lambda authorizers`_ to
attach to authentication schemes defined on a service. Authorizers are
first defined on a service, and then attached to the service, resources,
or operations using the ``aws.apigateway#authorizer-trait``.

The following Smithy model:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    use aws.apigateway#authorizer
    use aws.apigateway#authorizers
    use aws.auth#sigv4
    use aws.protocols#restJson1

    @restJson1
    @sigv4(name: "service")
    @authorizer("foo")
    @authorizers(
        foo: {scheme: sigv4, type: "aws", uri: "arn:foo"}
        baz: {scheme: sigv4, type: "aws", uri: "arn:foo"}
    )
    service Example {
      version: "2019-06-17"
      operations: [OperationA, OperationB]
      resources: [ResourceA, ResourceB]
    }

    // Inherits the authorizer of the service
    operation OperationA {}

    // Overrides the authorizer of the service
    @authorizer("baz")
    operation OperationB {}

    // Inherits the authorizer of the service
    resource ResourceA {
      operations: [OperationC, OperationD]
    }

    // Inherits the authorizer of the service
    operation OperationC {}

    // Overrides the authorizer of the service
    @authorizer("baz")
    operation OperationD {}

    // Overrides the authorizer of the service
    @authorizer("baz")
    resource ResourceB {
      operations: [OperationE, OperationF]
    }

    // Inherits the authorizer of ResourceB
    operation OperationE {}

    // Overrides the authorizer of ResourceB
    @authorizer("foo")
    operation OperationF {}

Is converted to the following OpenAPI model:

.. code-block:: json

    {
        "openapi": "3.0.2",
        "info": {
            "title": "Example",
            "version": "2019-06-17"
        },
        "paths": {
            "/a": {
                "get": {
                    "operationId": "OperationA",
                    "responses": {
                        "200": {
                            "description": "OperationA response"
                        }
                    }
                }
            },
            "/b": {
                "get": {
                    "operationId": "OperationB",
                    "responses": {
                        "200": {
                            "description": "OperationB response"
                        }
                    },
                    "security": [
                        {
                            "baz": []
                        }
                    ]
                }
            },
            "/c": {
                "get": {
                    "operationId": "OperationC",
                    "responses": {
                        "200": {
                            "description": "OperationC response"
                        }
                    }
                }
            },
            "/d": {
                "get": {
                    "operationId": "OperationD",
                    "responses": {
                        "200": {
                            "description": "OperationD response"
                        }
                    },
                    "security": [
                        {
                            "baz": []
                        }
                    ]
                }
            },
            "/e": {
                "get": {
                    "operationId": "OperationE",
                    "responses": {
                        "200": {
                            "description": "OperationE response"
                        }
                    },
                    "security": [
                        {
                            "baz": []
                        }
                    ]
                }
            },
            "/f": {
                "get": {
                    "operationId": "OperationF",
                    "responses": {
                        "200": {
                            "description": "OperationF response"
                        }
                    }
                }
            }
        },
        "components": {
            "securitySchemes": {
                "baz": {
                    "type": "apiKey",
                    "description": "AWS Signature Version 4 authentication",
                    "name": "Authorization",
                    "in": "header",
                    "x-amazon-apigateway-authorizer": {
                        "type": "aws",
                        "authorizerUri": "arn:foo"
                    },
                    "x-amazon-apigateway-authtype": "awsSigv4"
                },
                "foo": {
                    "type": "apiKey",
                    "description": "AWS Signature Version 4 authentication",
                    "name": "Authorization",
                    "in": "header",
                    "x-amazon-apigateway-authorizer": {
                        "type": "aws",
                        "authorizerUri": "arn:foo"
                    },
                    "x-amazon-apigateway-authtype": "awsSigv4"
                }
            }
        },
        "security": [
            {
                "foo": []
            }
        ]
    }


.. _openapi-cfn-substitutions:

AWS CloudFormation substitutions
================================

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

    This functionality can be disabled by setting the ``disableCloudFormationSubstitution``
    configuration property to ``true``.


Amazon Cognito User Pools
=========================

Smithy adds Cognito User Pool based authentication to the OpenAPI model when
the :ref:`aws.auth#cognitoUserPools-trait` is added to a service shape.
When this trait is present, Smithy will add a ``securitySchemes`` components
entry:

.. code-block:: json

    {
        "aws.auth#cognitoUserPools": {
            "type": "apiKey",
            "description": "Amazon Cognito User Pools authentication",
            "name": "Authorization",
            "in": "header",
            "x-amazon-apigateway-authtype": "cognito_user_pools",
            "x-amazon-apigateway-authorizer": {
                "type": "cognito_user_pools",
                "providerARNs": [
                    "arn:aws:cognito-idp:us-east-1:123:userpool/123"
                ]
            }
        }
    }

In the entry, ``providerARNs`` will be populated from the ``providerArns`` list
from the trait.

Amazon API Gateway API key usage plans
======================================

Smithy enables `API Gateway's API key usage plans`_ when a scheme based on the
:ref:`httpApiKeyAuth-trait` is set and configured as :ref:`an authorizer
<aws.apigateway#authorizers-trait>` with no ``type`` property set.

The following Smithy model enables API Gateway's API key usage plans on the
``OperationA`` operation:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    use aws.apigateway#authorizer
    use aws.apigateway#authorizers
    use aws.protocols#restJson1

    @restJson1
    @httpApiKeyAuth(name: "x-api-key", in: "header")
    @authorizer("api_key")
    @authorizers(api_key: {scheme: "smithy.api#httpApiKeyAuth"})
    service Example {
      version: "2019-06-17"
      operations: [OperationA]
    }

    operation OperationA {}


.. _other-traits:

Other traits that influence API Gateway
=======================================

``aws.apigateway#apiKeySource``
    Specifies the source of the caller identifier that will be used to
    throttle API methods that require a key. This trait is converted into
    the `x-amazon-apigateway-api-key-source`_ OpenAPI extension.

``aws.apigateway#authorizers``
    Lambda authorizers to attach to the authentication schemes defined on
    this service.

    .. seealso:: See :ref:`authorizers`


Amazon API Gateway limitations
==============================

The ``default`` property in OpenAPI is not currently supported by Amazon
API Gateway. The ``default`` property is automatically removed from OpenAPI
models when they are generated for Amazon API Gateway. Additionally, ``default``
values will not be set on ``$ref`` pointers or wrapped in an ``allOf`` as
described in :ref:`disableDefaultValues <generate-openapi-setting-disableDefaultValues>`.


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
    :name: openapi-code-build-gradle

    buildscript {
        dependencies {
            classpath("software.amazon.smithy:smithy-openapi:__smithy_version__")
        }
    }

Next, you need to create and configure an ``OpenApiConverter``:

.. code-block:: java

    import software.amazon.smithy.model.shapes.ShapeId;
    import software.amazon.smithy.openapi.OpenApiConfig;
    import software.amazon.smithy.openapi.fromsmithy.OpenApiConverter;
    import software.amazon.smithy.openapi.model.OpenApi;

    OpenApiConverter converter = OpenApiConverter.create();

    // Add any necessary configuration settings.
    OpenApiConfig config = new OpenApiConfig();
    config.setService(ShapeId.from("example.weather#Weather"));
    converter.config(config);

    // Generate the OpenAPI schema.
    OpenApi result = converter.convert(myModel);

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
.. _OpenAPI tags: https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#tagObject
.. _OpenAPI Data types: https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#data-types
.. _HTTP API: https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api.html
.. _REST API: https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-rest-api.html
.. _OpenAPI security schemes: https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#securitySchemeObject
.. _x-amazon-apigateway-authorizer: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-authorizer.html
.. _Lambda authorizers: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-swagger-extensions-authorizer.html
.. _API Gateway's API key usage plans: https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-api-usage-plans.html
