===============
Behavior traits
===============

Behavior traits are used to alter the behavior of operations.

.. contents:: Table of contents
    :depth: 1
    :local:
    :backlinks: none


.. smithy-trait:: smithy.api#idempotencyToken
.. _idempotencyToken-trait:

--------------------------
``idempotencyToken`` trait
--------------------------

Summary
    Defines the input member of an operation that is used by the server to
    identify and discard replayed requests.
Trait selector
    ``structure > :test(member > string)``

    *Any structure member that targets a string*
Value type
    Annotation trait

Only a single member of the input of an operation can be targeted by the
``idempotencyToken`` trait; only top-level structure members of the input of an
operation are considered.

A unique identifier (typically a UUID_) SHOULD be used by the client when
providing the value for the request token member. When the request token is
present, the service MUST ensure that the request is not replayed within a
service-defined period of time. This allows the client to safely retry
operation invocations, including operations that are not read-only, that fail
due to networking issues or internal server errors. The service uses the
provided request token to identify and discard duplicate requests.

Client implementations MAY automatically provide a value for a request token
member if and only if the member is not explicitly provided.

.. tabs::

    .. code-tab:: smithy

        operation AllocateWidget {
            input: AllocateWidgetInput
        }

        structure AllocateWidgetInput {
            @idempotencyToken
            clientToken: String
        }


.. smithy-trait:: smithy.api#idempotent
.. _idempotent-trait:

--------------------
``idempotent`` trait
--------------------

Summary
    Indicates that the intended effect on the server of multiple identical
    requests with an operation is the same as the effect for a single such
    request.
Trait selector
    ``operation``
Value type
    Annotation trait
Conflicts with
    :ref:`readonly-trait`

.. tabs::

    .. code-tab:: smithy

        @idempotent
        operation DeleteSomething {
            input: DeleteSomethingInput
            output: DeleteSomethingOutput
        }

.. note::

    All operations that are marked as :ref:`readonly-trait` are inherently
    idempotent.


.. smithy-trait:: smithy.api#readonly
.. _readonly-trait:

------------------
``readonly`` trait
------------------

Summary
    Indicates that an operation is effectively read-only.
Trait selector
    ``operation``
Value type
    Annotation trait
Conflicts with
    :ref:`idempotent-trait`

.. tabs::

    .. code-tab:: smithy

        @readonly
        operation GetSomething {
            input: GetSomethingInput
            output: GetSomethingOutput
        }


.. smithy-trait:: smithy.api#retryable
.. _retryable-trait:

-------------------
``retryable`` trait
-------------------

Summary
    Indicates that an error MAY be retried by the client.
Trait selector
    ``structure[trait|error]``

    *A structure shape with the error trait*
Value type
    ``structure``

The retryable trait is a structure that contains the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - throttling
      - ``boolean``
      - Indicates that the error is a retryable throttling error.

.. tabs::

    .. code-tab:: smithy

        @error("server")
        @retryable
        @httpError(503)
        structure ServiceUnavailableError {}

        @error("client")
        @retryable(throttling: true)
        @httpError(429)
        structure ThrottlingError {}


.. _pagination:

.. smithy-trait:: smithy.api#paginated
.. _paginated-trait:

-------------------
``paginated`` trait
-------------------

Summary
    The ``paginated`` trait indicates that an operation intentionally limits
    the number of results returned in a single response and that multiple
    invocations might be necessary to retrieve all results.
Trait selector
    ``:is(operation, service)``

    *An operation or service*
Value type
    ``structure``

Pagination is the process of dividing large result sets into discrete
pages. Smithy provides a built-in pagination mechanism that utilizes a
cursor.

The ``paginated`` trait is a structure that contains the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - inputToken
      - ``string``
      - The name of the operation input member that contains a continuation
        token. When this value is provided as input, the service returns
        results from where the previous response left off. This input member
        MUST NOT be marked as ``required`` and SHOULD target a string shape.
        It can, but SHOULD NOT target a map shape.

        When contained within a service, a paginated operation MUST either
        configure ``inputToken`` on the operation itself or inherit it from
        the service that contains the operation.
    * - outputToken
      - ``string``
      - The path to the operation output member that contains an optional
        continuation token. When this value is present in operation output,
        it indicates that there are more results to retrieve. To get the next
        page of results, the client passes the received output continuation
        token to the input continuation token of the next request. This
        output member MUST NOT be marked as ``required`` and SHOULD target a
        string shape. It can, but SHOULD NOT target a map shape.

        When contained within a service, a paginated operation MUST either
        configure ``outputToken`` on the operation itself or inherit it from
        the service that contains the operation.
    * - items
      - ``string``
      - The path to an output member of the operation that contains
        the data that is being paginated across many responses. The named
        output member, if specified, MUST target a list or map.
    * - pageSize
      - ``string``
      - The name of an operation input member that limits the maximum number
        of results to include in the operation output. This input member
        SHOULD NOT be required and MUST target an integer shape.

        .. warning::

            Do not attempt to fill response pages to meet the value provided
            for the ``pageSize`` member of a paginated operation. Attempting to
            match a target number of elements results in an unbounded API with
            an unpredictable latency.

The following example defines a paginated operation that sets each value
explicitly on the operation.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @readonly
        @paginated(inputToken: "nextToken", outputToken: "nextToken",
                   pageSize: "maxResults", items: "foos")
        operation GetFoos {
            input: GetFoosInput
            output: GetFoosOutput
        }

        structure GetFoosInput {
            maxResults: Integer
            nextToken: String
        }

        structure GetFoosOutput {
            nextToken: String
            foos: StringList!
        }

        list StringList {
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#GetFoos": {
                    "type": "operation",
                    "input": {
                        "target": "smithy.example#GetFoosInput"
                    },
                    "output": {
                        "target": "smithy.example#GetFoosOutput"
                    },
                    "traits": {
                        "smithy.api#readonly": {},
                        "smithy.api#paginated": {
                            "inputToken": "nextToken",
                            "outputToken": "nextToken",
                            "pageSize": "maxResults",
                            "items": "foos"
                        }
                    }
                },
                "smithy.example#GetFoosInput": {
                    "type": "structure",
                    "members": {
                        "maxResults": {
                            "target": "smithy.api#Integer"
                        },
                        "nextToken": {
                            "target": "smithy.api#String"
                        }
                    }
                },
                "smithy.example#GetFoosOutput": {
                    "type": "structure",
                    "members": {
                        "nextToken": {
                            "target": "smithy.api#String"
                        },
                        "foos": {
                            "target": "smithy.example#StringList",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                },
                "smithy.example#StringList": {
                    "type": "list",
                    "member": {
                        "target": "smithy.api#String"
                    }
                }
            }
        }

Attaching the ``paginated`` trait to a service provides default pagination
configuration settings to all operations bound within the closure of the
service. Pagination settings configured on an operation override any inherited
service setting.

The following example defines a paginated operation that inherits some
settings from a service.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @paginated(inputToken: "nextToken", outputToken: "nextToken",
                   pageSize: "maxResults")
        service Example {
            version: "2019-06-27"
            operations: [GetFoos]
        }

        @readonly @paginated(items: "foos")
        operation GetFoos {
            input: GetFoosInput
            output: GetFoosOutput
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#Example": {
                    "type": "service",
                    "version": "2019-06-27",
                    "traits": {
                        "smithy.api#paginated": {
                            "inputToken": "nextToken",
                            "outputToken": "nextToken",
                            "pageSize": "maxResults"
                        }
                    }
                },
                "smithy.example#GetFoos": {
                    "type": "operation",
                    "input": {
                        "target": "smithy.example#GetFoosInput"
                    },
                    "output": {
                        "target": "smithy.example#GetFoosOutput"
                    },
                    "traits": {
                        "smithy.api#readonly": {},
                        "smithy.api#paginated": {
                            "items": "foos"
                        }
                    }
                }
            }
        }

The values for ``outputToken`` and ``items`` are paths. :dfn:`Paths` are a series of
identifiers separated by dots (``.``) where each identifier represents a
member name in a structure. The first member name MUST correspond to a member
of the output structure and each subsequent member name MUST correspond to a
member in the previously referenced structure. Paths MUST adhere to the
following ABNF.

.. productionlist:: smithy
    path    :`identifier` *("." `identifier`)

The following example defines a paginated operation which uses a result
wrapper where the output token and items are referenced by paths.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @readonly
        @paginated(inputToken: "nextToken", outputToken: "result.nextToken",
                   pageSize: "maxResults", items: "result.foos")
        operation GetFoos {
            input: GetFoosInput
            output: GetFoosOutput
        }

        structure GetFoosInput {
            maxResults: Integer
            nextToken: String
        }

        structure GetFoosOutput {
            result: ResultWrapper!
        }

        structure ResultWrapper {
            nextToken: String
            foos: StringList!
        }

        list StringList {
            member: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#GetFoos": {
                    "type": "operation",
                    "input": {
                        "target": "smithy.example#GetFoosInput"
                    },
                    "output": {
                        "target": "smithy.example#GetFoosOutput"
                    },
                    "traits": {
                        "smithy.api#readonly": {},
                        "smithy.api#paginated": {
                            "inputToken": "nextToken",
                            "outputToken": "result.nextToken",
                            "pageSize": "maxResults",
                            "items": "result.foos"
                        }
                    }
                },
                "smithy.example#GetFoosInput": {
                    "type": "structure",
                    "members": {
                        "maxResults": {
                            "target": "smithy.api#Integer"
                        },
                        "nextToken": {
                            "target": "smithy.api#String"
                        }
                    }
                },
                "smithy.example#GetFoosOutput": {
                    "type": "structure",
                    "members": {
                        "result": {
                            "target": "smithy.example#ResultWrapper",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                },
                "smithy.example#ResultWrapper": {
                    "type": "structure",
                    "members": {
                        "nextToken": {
                            "target": "smithy.api#String"
                        },
                        "foos": {
                            "target": "smithy.example#StringList",
                            "traits": {
                                "smithy.api#required": {}
                            }
                        }
                    }
                },
                "smithy.example#StringList": {
                    "type": "list",
                    "member": {
                        "target": "smithy.api#String"
                    }
                }
            }
        }

Pagination Behavior
===================

#. If an operation returns a naturally size-limited subset of data
   (e.g., a top-ten list of users sorted by rank), then the operation
   SHOULD NOT be paginated.

#. Only one list or map per operation can be paginated.

#. Paginated responses MUST NOT return the same item of a paginated result
   set more than once (i.e., a paginated result set is a disjoint union of the
   subsets partitioned by the referenced ``pageSize`` input member and the SLA
   defined by the service).

#. If a paginated request returns data in a sorted order that is not an
   immutable strict total ordering of items, then the paginated request MUST
   provide a temporally static view of the underlying data that does not
   modify the order topology during pagination. For example, a game’s
   leaderboard of top-scoring players cannot have players move from position
   #10 to position #12 during pagination, the last player on page N has to
   have a higher score than the first player on page N+1, no players that
   exist when pagination begins are to be skipped, and players MUST NOT be
   repeated due to moves in the underlying data.

#. If pagination is ordered and newly created resources are returned, then
   newly created resources MUST appear in order on the appropriate page.


Client behavior
===============

Smithy clients SHOULD provide abstractions that can be used to automatically
iterate over paginated responses. The following steps describe the process a
client MUST follow when iterating over paginated API calls:

#. Send the initial request to a paginated operation.

#. If the received response does not contain a continuation token in the
   referenced ``outputToken`` member, then there are no more results to
   retrieve and the process is complete.

#. If there is a continuation token in the referenced ``outputToken`` member
   of the response, then the client sends a subsequent request using the same
   input parameters as the original call, but including the last received
   continuation token. Clients are free to change the designated ``pageSize``
   input parameter at this step as needed.

#. If a client receives an identical continuation token from a service in back
   to back calls, then the client MAY choose to stop sending requests. This
   scenario implies a "tail" style API operation where clients are running in
   an infinite loop to send requests to a service in order to retrieve results
   as they are available.

#. Return to step 2.


Continuation tokens
===================

The ``paginated`` trait indicates that an operation utilizes cursor-based
pagination. When a paginated operation truncates its output, it MUST return a
continuation token in the operation output that can be used to get the next
page of results. This token can then be provided along with the original input
to request additional results from the operation.

#. **Continuation tokens SHOULD be opaque.**

   Plain text continuation tokens inappropriately expose implementation details
   to the client, resulting in consumers building systems that manually
   construct continuation tokens. Making backwards compatible changes to a
   plain text continuation token format is extremely hard to manage.

#. **Continuation tokens SHOULD be versioned.**

   The parameters and context needed to paginate an API call can evolve over
   time. To future-proof these APIs, services SHOULD include some kind of
   version identifier in their continuation tokens. Once the version identifier
   of a token is recognized, a service will then know the appropriate operation
   for decoding and returning the next response for a paginated request.

#. **Continuation tokens SHOULD expire after a period of time.**

   Continuation tokens SHOULD expire after a short period of time (e.g., 24
   hours is a reasonable default for many services). This allows services
   to quickly phase out deprecated continuation token formats, and helps to set
   the expectation that continuation tokens are ephemeral and MUST NOT be used
   after extended periods of time. Services MUST reject a request with a client
   error when a client uses an expired continuation token.

#. **Continuation tokens MUST be bound to a fixed set of filtering parameters.**

   Services MUST reject a request that changes filtering input parameters while
   paging through responses. Services MUST require clients to send the same
   filtering request parameters used in the initial pagination request to all
   subsequent pagination requests.

   :dfn:`Filtering parameters` are defined as parameters that remove certain
   elements from appearing in the result set of a paginated API call. Filtering
   parameters do not influence the presentation of results (e.g., the
   designated ``pageSize`` input parameter partitions a result set into smaller
   subsets but does not change the sum of the parts). Services MUST allow
   clients to change presentation based parameters while paginating through a
   result set.

#. **Continuation tokens MUST NOT influence authorization.**

   A service MUST NOT evaluate authorization differently depending on the
   presence, absence, or contents of a continuation token.


Backward compatibility
======================

Many tools use the ``paginated`` trait to expose additional functionality to
things like generated code. To support these use cases, the following changes
to the ``paginated`` trait are considered backward incompatible:

1. Removing the ``paginated`` trait.
2. Adding, removing, or changing the ``inputToken``, ``outputToken``, or
   ``items`` members.
3. Removing or changing the ``pageSize`` member.

The following changes are considered backward compatible:

1. Adding the ``paginated`` trait to an existing operation.
2. Adding the ``pageSize`` member to an existing ``paginated`` trait.


.. _UUID: https://tools.ietf.org/html/rfc4122


.. smithy-trait:: smithy.api#httpChecksumRequired
.. _httpChecksumRequired-trait:

------------------------------
``httpChecksumRequired`` trait
------------------------------

Summary
    Indicates that an operation requires a checksum in its HTTP request. By
    default, the checksum used for a service is a MD5 checksum passed in the
    Content-MD5 header.
Trait selector
    ``operation``
Value type
    Annotation trait.
See
    :rfc:`1864`

.. tabs::

    .. code-tab:: smithy

        @httpChecksumRequired
        operation PutSomething {
            input: PutSomethingInput
            output: PutSomethingOutput
        }


.. smithy-trait:: smithy.api#httpChecksum
.. _httpChecksum-trait:

----------------------
``httpChecksum`` trait
----------------------

Summary
    Indicates that an operation's HTTP request or response supports checksum
    validation. At least one of request or response checksum properties MUST
    be specified within the trait.

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
    * - request
      - ``[`` :ref:`HttpChecksumProperty structure <checksum-property>` ``]``
      - The ``request`` property defines a list of checksum validation
        behavior for HTTP requests.

    * - response
      - ``[`` :ref:`HttpChecksumProperty structure <checksum-property>` ``]``
      - The ``response`` property defines a list of checksum validation
        behavior for HTTP responses.

.. tabs::

    .. code-tab:: smithy

        @httpChecksum(
            request: [
                { algorithm: "sha256", in: "header", name: "x-checksum-sha256"},
                { algorithm: "crc32", in: "header", name: "x-checksum-crc32"}
            ],
            response: [
                { algorithm: "sha256", in: "header", name: "x-checksum-sha256"},
                { algorithm: "crc32", in: "header", name: "x-checksum-crc32"}
            ]
        )
        operation PutSomething {
            input: PutSomethingInput,
            output: PutSomethingOutput
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#Example": {
                    "type": "service",
                    "version": "2019-06-27",
                },
                "smithy.example#PutSomething": {
                    "type": "operation",
                    "input": {
                        "target": "smithy.example#PutSomethingInput"
                    },
                    "output": {
                        "target": "smithy.example#PutSomethingOutput"
                    },
                    "traits": {
                        "smithy.api#httpChecksum": {
                            "request": [
                                { "algorithm": "sha256", "in": "header", "name": "x-checksum-sha256"},
                                { "algorithm": "crc32", "in": "header", "name": "x-checksum-crc32"}
                            ],
                            "response": [
                                { "algorithm": "sha256", "in": "header", "name": "x-checksum-sha256"},
                                { "algorithm": "crc32", "in": "header", "name": "x-checksum-crc32"}
                            ]
                        }
                    }
                }
            }
        }


.. _checksum-property:

HttpChecksumProperty structure
================================

``HttpChecksumProperty`` defines checksum validation behavior using the
following members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description

    * - algorithm
      - ``string``
      - **Required**. The algorithm string represents a checksum algorithm
        supported for the HTTP request or response. A valid algorithm
        should follow ABNF:

        .. code-block:: abnf

            algorithms = 1*(lower-alpha / DIGIT)
            lower-alpha = %x61-7A

        That is, it should be comprised only of digits and lowercase letters.

    * - in
      - ``string``
      - **Required**. The location where checksum can be supplied. Valid values
        are:

        * ``header`` - Indicates the checksum is placed in an HTTP header.
        * ``trailer`` - Indicates the checksum is placed in the `chunked trailer part`_
          of the body.

        An algorithm MAY be supported at multiple locations. The first
        supported ``in`` member value within the list of HttpChecksum property
        takes precedence and SHOULD be used.

        .. seealso:: See :ref:`httpChecksum_trait_behavior` for more details.

    * - name
      - ``string``
      - **Required**. The name used as a header or trailer name for a checksum
        type. Recommended ABNF for name is as follows:

        .. code-block:: abnf

            name = lower-alpha *(["-"](lower-alpha / DIGIT))
            lower-alpha = %x61-7A

        That is, it should start with a lowercase letter followed by any
        number of lowercase letters, digits, or non-sequential hyphens.

        A member with the :ref:`httpHeader-trait` or :ref:`httpPrefixHeaders-trait`
        MAY conflict with a ``httpChecksum`` header name, allowing a checksum
        to be supplied directly.

        .. seealso:: See :ref:`client behavior<httpChecksum_trait_header_conflict_behavior>`
            for more details.

.. _chunked trailer part: https://tools.ietf.org/html/rfc7230#section-4.1.2


.. _httpChecksum_trait_behavior:

HttpChecksum Behavior
=====================

.. rubric:: Client Behavior

If the ``httpChecksum`` trait has a modeled ``request`` section for the HTTP
request, the client SHOULD compute the request payload checksum, using an
algorithm defined within the request section. The computed checksum SHOULD
be supplied at the first supported checksum location as per the order of
:ref:`checksum properties<checksum-property>` defined within the request
section.

If the location is ``header``, the client MUST put the checksum into the
HTTP request headers. If the resolved location is ``trailer``, the client MUST
put the checksum into the `chunked trailer part`_ of the body. The header or
trailer name to use with an algorithm location is defined by the ``name``
member of the :ref:`checksum property<checksum-property>`.

If the ``httpChecksum`` trait has a modeled ``response`` section for the HTTP
response, the client MUST look for a supported checksum at all supported
locations as per the defined checksum properties. If a checksum is found, the
client MUST validate the received checksum value by computing the corresponding
checksum of the received payload.

.. _httpChecksum_trait_header_conflict_behavior:

If the HTTP header corresponding to a checksum is set explicitly, the
implementation MUST use the explicitly set header and skip computing the
payload checksum.

.. _chunked trailer part: https://tools.ietf.org/html/rfc7230#section-4.1.2


.. rubric:: Service Behavior

If the ``httpChecksum`` trait has a modeled ``request`` section for the HTTP
request, the service MUST validate the received checksum by computing
corresponding checksum of the request payload to ensure data integrity.

If the ``httpChecksum`` trait models a ``response`` section for the HTTP
response, the service MUST send at least one supported payload checksum at the
first supported location for the checksum algorithm.


.. _checksum_trait_with_checksum_required:

Behavior with HttpChecksumRequired
----------------------------------

When both the ``httpChecksum`` trait with a modeled ``request`` section, and
the :ref:`httpChecksumRequired <httpChecksumRequired-trait>` trait are applied
on an operation, the client MUST prefer using a checksum algorithm modeled for
request in the ``httpChecksum`` trait over an MD5 digest, and place checksum
as per the client behavior defined in the
:ref:`httpChecksum trait behavior<httpChecksum_trait_behavior>` section.

The service MUST accept a checksum value defined in the ``httpChecksum``
trait's request property to satisfy checksum validation requirements for
the operation's HTTP Request.
