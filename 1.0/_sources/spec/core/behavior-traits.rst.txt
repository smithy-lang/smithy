===============
Behavior traits
===============

Behavior traits are used to alter the behavior of operations.

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

.. code-block:: smithy

    operation AllocateWidget {
        input: AllocateWidgetInput
    }

    @input
    structure AllocateWidgetInput {
        @idempotencyToken
        clientToken: String,
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

.. code-block:: smithy

    @idempotent
    operation DeleteSomething {
        input: DeleteSomethingInput,
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

.. code-block:: smithy

    @readonly
    operation GetSomething {
        input: GetSomethingInput,
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

.. code-block:: smithy

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
        continuation token. When this value is present and not empty in
        operation output, it indicates that there are more results to retrieve.
        To get the next page of results, the client passes the received output
        continuation token to the input continuation token of the next request.
        This output member MUST NOT be marked as ``required`` and SHOULD target
        a string shape. It can, but SHOULD NOT target a map shape.

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

.. code-block:: smithy

    namespace smithy.example

    @readonly
    @paginated(inputToken: "nextToken", outputToken: "nextToken",
               pageSize: "maxResults", items: "foos")
    operation GetFoos {
        input: GetFoosInput,
        output: GetFoosOutput
    }

    @input
    structure GetFoosInput {
        maxResults: Integer,
        nextToken: String
    }

    @output
    structure GetFoosOutput {
        nextToken: String,

        @required
        foos: StringList,
    }

    list StringList {
        member: String
    }

Attaching the ``paginated`` trait to a service provides default pagination
configuration settings to all ``paginated`` operations bound within the closure
of the service. Pagination settings configured on an operation override any
inherited service setting.

The following example defines a paginated operation that inherits some
settings from a service.

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @paginated(inputToken: "nextToken", outputToken: "nextToken",
                   pageSize: "maxResults")
        service Example {
            version: "2019-06-27",
            operations: [GetFoos],
        }

        @readonly @paginated(items: "foos")
        operation GetFoos {
            input: GetFoosInput,
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
    path    :`Identifier` *("." `Identifier`)

The following example defines a paginated operation which uses a result
wrapper where the output token and items are referenced by paths.

.. code-block:: smithy

    namespace smithy.example

    @readonly
    @paginated(inputToken: "nextToken", outputToken: "result.nextToken",
               pageSize: "maxResults", items: "result.foos")
    operation GetFoos {
        input: GetFoosInput,
        output: GetFoosOutput
    }

    @input
    structure GetFoosInput {
        maxResults: Integer,
        nextToken: String
    }

    @output
    structure GetFoosOutput {
        @required
        result: ResultWrapper
    }

    structure ResultWrapper {
        nextToken: String,

        @required
        foos: StringList,
    }

    list StringList {
        member: String
    }


Pagination Behavior
===================

#. If an operation returns a naturally size-limited subset of data
   (e.g., a top-ten list of users sorted by rank), then the operation
   SHOULD NOT be paginated.

#. Only one list or map per operation can be paginated.

#. Paginated responses SHOULD NOT return the same item of a paginated result
   set more than once.

#. Services SHOULD NOT return items in a paginated result set that have been
   deleted during the pagination process, but before reaching the relevant
   page.

#. Services MAY include newly created items in a paginated result set on a
   not yet seen page. If pagination is ordered and newly created items are
   returned, then newly created items MUST appear in order on the appropriate
   page.


Client behavior
===============

Smithy clients SHOULD provide abstractions that can be used to automatically
iterate over paginated responses. The following steps describe the process a
client MUST follow when iterating over paginated API calls:

#. Send the initial request to a paginated operation. This request MAY
   include input parameters that are used to influence the starting point
   at which pagination occurs.

#. If the received response does not contain a continuation token in the
   referenced ``outputToken`` member (either the member is not set or is set to
   an empty value), then there are no more results to retrieve and the process
   is complete.

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
            input: PutSomethingInput,
            output: PutSomethingOutput
        }


.. smithy-trait:: smithy.api#requestCompression
.. _requestCompression-trait:

----------------------------
``requestCompression`` trait
----------------------------

Summary
    Indicates that an operation supports compressing requests from clients to
    services.
Trait selector
    ``operation``
Value type
    ``structure``
Validation
    1. Operation input members must not have both the :ref:`streaming-trait` and
       :ref:`requiresLength-trait` applied.
       This avoids the client reading and compressing the entire stream for the
       length of the compressed data to set the ``Content-Length`` header.
    2. ``encodings`` must not be empty, and all members must be valid,
       case-insensitive, supported compression algorithm values.


The ``requestCompression`` trait is a structure that contains the following
members:

.. list-table::
    :header-rows: 1
    :widths: 10 10 80

    * - Property
      - Type
      - Description
    * - encodings
      - ``list<string>``
      - Defines the priority-ordered list of compression algorithms supported by
        the service operation. Supported compression algorithms are: "gzip".


The following example defines an operation that supports ``gzip`` compression
for requests from clients to services with both a streaming and non-streaming
member.

.. code-block:: smithy

    namespace smithy.example

    @requestCompression(
        encodings: ["gzip"]
    )
    operation GetFoos {
        input: GetFoosInput
    }

    @input
    structure GetFoosInput {
        @required
        streamingMember: StreamingFoo,
        member: String
    }

    @streaming
    blob StreamingFoo


Request compression behavior
============================

Operations with the ``requestCompression`` trait applied direct clients to
compress requests sent to services and set the corresponding encoding in the
``Content-Encoding`` header.

Operations with the ``requestCompression`` trait applied have request
compression enabled by default, and clients SHOULD attempt to compress a request
when a compression algorithm is supported (see ``DISABLE_REQUEST_COMPRESSION``
in :ref:`compression-client-implementation` to disable request compression).

Since compression is NOT required, services supporting operations with the
``requestCompression`` trait applied MUST be able to receive non-compressed
requests, for cases such as clients not supporting certain compression
algorithms or older versions of clients that have out-of-date supported
compression algorithms.

The client MUST only compress the request using the first supported algorithm in
``encodings`` and stop considering algorithms that appear later in the list.

If a client does compress the request, they MUST also set the
``Content-Encoding`` header per :rfc:`9110#section-8.4`:

    If one or more encodings have been applied to a representation, the sender
    that applied the encodings MUST generate a Content-Encoding header field
    that lists the content codings in the order in which they were applied.

If a request has a structure member bound to the ``Content-Encoding`` header,
the trait MAY still be applied. If the request is compressed, then the
compression encoding MUST be appended to the ``Content-Encoding`` header after
any user-provided encoding(s).

As an example, using the ``PutWithContentEncoding`` operation below with the
``customEncoding`` member set to ``brotli``, the header would be
``"Content-Encoding": "brotli, gzip"``.

.. code-block:: smithy

    namespace smithy.example

    @requestCompression(
        encodings: ["gzip"]
    )
    operation PutWithContentEncoding {
        input: PutWithContentEncodingInput
    }

    @input
    structure PutWithContentEncodingInput {
        @httpHeader("Content-Encoding")
        customEncoding: String, // brotli

        @httpPayload
        data: String
    }

Clients MUST encode the request content prior to request signing, otherwise the
request signature will mismatch.

Because small payloads may become longer when compressed, operations SHOULD only
be compressed when the payload size is greater than or equal to the value of the
minimum compression threshold (see ``REQUEST_MIN_COMPRESSION_SIZE_BYTES`` in
:ref:`compression-client-implementation`).

If the request contains a member with the :ref:`streaming-trait` applied without
the :ref:`requiresLength-trait` applied, the request MUST be compressed if
enabled regardless of the minimum compression threshold.

Operations with input members with both :ref:`streaming-trait` and
:ref:`requiresLength-trait` are not allowed to use request compression. This
avoids the client reading and compressing the entire stream for the length of
the compressed data to set the ``Content-Length`` header.


.. _compression-client-implementation:

Client Implementation
=====================

.. warning::

    The client configuration settings defined in this specification are still
    evolving and subject to change.

Smithy clients MUST expose a setting ``DISABLE_REQUEST_COMPRESSION`` to disable
request compression. The value MUST be a boolean value which defaults to
``false``. In order of precedence from highest to lowest, the setting shall be
exposed:

- Per-request configuration (SHOULD, if request-level configuration is supported
  by the client)
- Service client configuration - ``DisableRequestCompression`` (MUST, with
  idiomatic casing/spacing for that client programming language)

Smithy clients MUST expose a setting ``REQUEST_MIN_COMPRESSION_SIZE_BYTES`` to
specify the minimum size in bytes that a request body should be to trigger
compression. The value MUST be a non-negative integer in the range ``0`` and
``10485760`` inclusive, and defaults to ``10240`` bytes. If the value is outside
of the range, the client MUST throw an error. In order of precedence from
highest to lowest, the setting shall be exposed:

- Per-request configuration (SHOULD, if request-level configuration is supported
  by that client)
- Service client configuration - ``RequestMinCompressionSizeBytes`` (MUST, with
  idiomatic casing/spacing for that client programming language)

If a client's language or standard library does not support a compression
algorithm, they are not required to implement that algorithm. If none of the
compression algorithms are supported by the client, then the request does not
need to be compressed.

If the client's language or standard library exposes a "level" or "quality"
setting to control the tradeoff between speed and the compressed size, clients
MUST set this setting to an idiomatic default (either the default value for
their language, or else a "balanced" mode).


Backward compatibility
======================

Once a compression algorithm is added to ``encodings``, the algorithm SHOULD NOT
be removed. If an algorithm is removed, the service MUST continue to support the
removed algorithm on the server-side because older clients or non-official
clients may continue sending compressed requests.
