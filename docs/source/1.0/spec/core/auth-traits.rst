.. _authentication-traits:

=====================
Authentication traits
=====================

Authentication traits define how a client authenticates with a service.

.. contents:: Table of contents
    :depth: 1
    :local:
    :backlinks: none


.. smithy-trait:: smithy.api#authDefinition
.. _authDefinition-trait:

------------------------
``authDefinition`` trait
------------------------

Summary
    A meta-trait that marks a trait as an authentication scheme. Traits
    that are marked with this trait are applied to service shapes to
    indicate how a client can authenticate with the service.
Trait selector
    ``[trait|trait]``
Value type
    An object with the following properties:

    .. list-table::
       :header-rows: 1
       :widths: 10 23 67

       * - Property
         - Type
         - Description
       * - traits
         - [:ref:`shape-id`]
         - List of shape IDs that auth scheme implementations MUST
           understand in order to successfully use the scheme. Each shape
           MUST exist and MUST be a trait. Code generators SHOULD ensure
           that they support each listed trait.

Every operation in the closure of a service is expected to support the
authentication schemes applied to a service unless the service or operation
is marked with the :ref:`auth-trait`, which is used to change the set of
supported authentication schemes.

The following example defines a service that supports both ``httpBasicAuth``
and the hypothetical ``fooExample`` authentication scheme.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @authDefinition
        @trait(selector: "service")
        structure fooExample {}

        @fooExample
        @httpBasicAuth
        service WeatherService {
            version: "2017-02-11",
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#WeatherService": {
                    "type": "service",
                    "version": "2017-02-11",
                    "traits": {
                        "smithy.example#fooExample": {},
                        "smithy.api#httpBasicAuth": {}
                    }
                },
                "smithy.example#fooExample": {
                    "type": "structure",
                    "traits": {
                        "smithy.api#authDefinition": {},
                        "smithy.api#trait": {
                            "selector": "service"
                        }
                    }
                }
            }
        }

Because authentication scheme definitions are just specialized shapes, they
can also support configuration settings.

.. code-block:: smithy

    namespace smithy.example

    @authDefinition
    @trait(selector: "service")
    structure algorithmAuth {
        algorithm: AlgorithmAuthAlgorithm,
    }

    @private
    @enum([{value: "SHA-2"}])
    string AlgorithmAuthAlgorithm

    @algorithmAuth(algorithm: "SHA-2")
    service WeatherService {
        version: "2017-02-11",
    }


.. smithy-trait:: smithy.api#httpBasicAuth
.. _httpBasicAuth-trait:

-----------------------
``httpBasicAuth`` trait
-----------------------

Summary
    Indicates that a service supports HTTP Basic Authentication as
    defined in :rfc:`2617`.
Trait selector
    ``service``
Value type
    Annotation trait.

.. code-block:: smithy

    @httpBasicAuth
    service WeatherService {
        version: "2017-02-11",
    }


.. smithy-trait:: smithy.api#httpDigestAuth
.. _httpDigestAuth-trait:

------------------------
``httpDigestAuth`` trait
------------------------

Summary
    Indicates that a service supports HTTP Digest Authentication as defined
    in :rfc:`2617`.
Trait selector
    ``service``
Value type
    Annotation trait.

.. code-block:: smithy

    @httpDigestAuth
    service WeatherService {
        version: "2017-02-11",
    }


.. smithy-trait:: smithy.api#httpBearerAuth
.. _httpBearerAuth-trait:

------------------------
``httpBearerAuth`` trait
------------------------

Summary
    Indicates that a service supports HTTP Bearer Authentication as defined
    in :rfc:`6750`.
Trait selector
    ``service``
Value type
    Annotation trait.

.. code-block:: smithy

    @httpBearerAuth
    service WeatherService {
        version: "2017-02-11",
    }


.. smithy-trait:: smithy.api#httpApiKeyAuth
.. _httpApiKeyAuth-trait:

------------------------
``httpApiKeyAuth`` trait
------------------------

Summary
    Indicates that a service supports HTTP-specific authentication using an
    API key sent in a header or query string parameter.
Trait selector
    ``service``
Value type
    Object

The ``httpApiKeyAuth`` trait is an object that supports the following
properties:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - name
      - ``string``
      - **Required**. Defines the name of the HTTP header or query string
        parameter that contains the API key.
    * - in
      - ``string``
      - **Required**. Defines the location of where the key is serialized.
        This value can be set to ``header`` or ``query``.
    * - scheme
      - ``string``
      - Defines the security scheme to use on the ``Authorization`` header value
        This can only be set if the "in" property is set to ``header``.

The following example defines a service that accepts an API key in the "X-Api-Key"
HTTP header:

.. code-block:: smithy

    @httpApiKeyAuth(name: "X-Api-Key", in: "header")
    service WeatherService {
        version: "2017-02-11",
    }

The following example defines a service that uses an API key auth scheme through
the HTTP ``Authorization`` header:

.. code-block:: smithy

    @httpApiKeyAuth(scheme: "ApiKey", name: "Authorization", in: "header")
    service WeatherService {
        version: "2017-02-11",
    }


.. smithy-trait:: smithy.api#optionalAuth
.. _optionalAuth-trait:

----------------------
``optionalAuth`` trait
----------------------

Summary
    Indicates that an operation MAY be invoked without authentication,
    regardless of any authentication traits applied to the operation.
Trait selector
    ``operation``
Value type
    Annotation trait.

The following example defines a service that uses HTTP digest authentication,
and bound to the service is an operation that supports unauthenticated access.

.. code-block:: smithy

    @httpDigestAuth
    service WeatherService {
        version: "2017-02-11",
        operations: [PingServer]
    }

    @optionalAuth
    operation PingServer {}

The following example defines an operation that does not support
*any* authentication. This kind of operation does not require the
``optionalAuth`` trait.

.. code-block:: smithy

    @auth([])
    operation SomeUnauthenticatedOperation {}


.. smithy-trait:: smithy.api#auth
.. _auth-trait:

--------------
``auth`` trait
--------------

Summary
    Defines the priority ordered authentication schemes supported by a service
    or operation. When applied to a service, it defines the default
    authentication schemes of every operation in the service. When applied
    to an operation, it defines the list of all authentication schemes
    supported by the operation, overriding any ``auth`` trait specified
    on a service.
Trait selector
    ``:is(service, operation)``

    *Service or operation shapes*
Value type
    This trait contains a priority ordered list of unique string values that
    reference authentication scheme shape IDs defined on a service
    shape.

Operations that are not annotated with the ``auth`` trait inherit the ``auth``
trait of the service they are bound to, and if the operation is not annotated
with the ``auth`` trait, then the operation is expected to support each of
the :ref:`authentication scheme traits <authDefinition-trait>` applied to the
service. Each entry in the ``auth`` trait is a shape ID that MUST refer to an
authentication scheme trait applied to the service in which it is bound.

The following example defines two operations:

* OperationA defines an explicit list of the authentication schemes it
  supports using the ``auth`` trait.
* OperationB is not annotated with the ``auth`` trait, so the schemes
  supported by this operation inherit all of the authentication schemes
  applied to the service.

.. tabs::

    .. code-tab:: smithy

        @httpBasicAuth
        @httpDigestAuth
        @auth([httpBasicAuth])
        service AuthenticatedService {
            version: "2017-02-11",
            operations: [OperationA, OperationB]
        }

        // This operation is configured to only support httpDigestAuth.
        // It is not expected to support httpBasicAuth.
        @auth([httpDigestAuth])
        operation OperationA {}

        // This operation defines no auth trait, so it is expected to
        // support the effective authentication schemes of the service:
        // httpBasicAuth and httpDigestAuth.
        operation OperationB {}

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#AuthenticatedService": {
                    "type": "service",
                    "version": "2017-02-11",
                    "operations": [
                        {
                            "target": "smithy.example#OperationA"
                        },
                        {
                            "target": "smithy.example#OperationB"
                        }
                    ],
                    "traits": {
                        "smithy.api#httpBasicAuth": {},
                        "smithy.api#httpDigestAuth": {},
                        "smithy.api#auth": [
                            "smithy.api#httpBasicAuth"
                        ]
                    }
                },
                "smithy.example#OperationA": {
                    "type": "operation",
                    "traits": {
                        "smithy.api#auth": [
                            "smithy.api#httpDigestAuth"
                        ]
                    }
                },
                "smithy.example#OperationB": {
                    "type": "operation"
                }
            }
        }

The following ``auth`` trait is invalid because it references an
authentication scheme trait that is not applied to the service:

.. code-block:: smithy

    @httpDigestAuth
    @auth([httpBasicAuth]) // <-- Invalid!
    service InvalidExample {
        version: "2017-02-11"
    }

The following operation ``auth`` trait is invalid because it references an
authentication scheme trait that is not applied to the service:

.. code-block:: smithy

    @httpDigestAuth
    service InvalidExample {
        version: "2017-02-11",
        operations: [OperationA]
    }

    @auth([httpBasicAuth]) // <-- Invalid!
    operation OperationA {}
