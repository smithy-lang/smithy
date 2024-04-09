.. _http-protocol-compliance-tests:

==============================
HTTP Protocol Compliance Tests
==============================

Smithy is a protocol-agnostic IDL that tries to abstract the serialization
format of request and response messages sent between a client and server.
Protocol specifications define the serialization format of a protocol, and
protocol compliance tests help to ensure that implementations correctly
implement a protocol specification.


--------
Overview
--------

This specification defines two traits in the ``smithy.test`` namespace that
are used to make assertions about client and server protocol implementations.

:ref:`smithy.test#httpRequestTests <httpRequestTests-trait>`
    Used to define how an HTTP request is serialized given a specific
    protocol, authentication scheme, and set of input parameters.
:ref:`smithy.test#httpResponseTests <httpResponseTests-trait>`
   Used to define how an HTTP response is serialized given a specific
   protocol, authentication scheme, and set of output or error parameters.

Additionally, it defines one trait specifically for the behavior of server
protocol implementations.

:ref:`smithy.test#httpMalformedRequestTests <httpMalformedRequestTests-trait>`
   Used to define how a server rejects a malformed HTTP request given a
   specific protocol and HTTP message.

Protocol implementation developers use these traits to ensure that their
implementation is correct. This can be done through code generation of test
cases or by dynamically loading test cases at runtime. For example, a Java
implementation could generate JUnit test cases to assert that the
expectations defined in a model match the behavior of a generated client
or server.


Parameter format
================

The ``params`` property used in both the ``httpRequestTests`` trait and
``httpResponseTests`` trait test cases represents parameters that are used
to serialize HTTP requests and responses. In order to compare implementation
specific results against the expected result of each test case across
different programming languages, parameters are defined in the same format
specified in :ref:`trait-node-values` with the following additional
constraints:

* Timestamp values must be converted to a Unix timestamp represented
  as an integer.
* Client implementations that automatically provide values for members marked
  with the :ref:`idempotencyToken-trait` MUST use a constant value of
  ``00000000-0000-4000-8000-000000000000``.


.. smithy-trait:: smithy.test#httpRequestTests
.. _httpRequestTests-trait:

----------------
httpRequestTests
----------------

Summary
    The ``httpRequestTests`` trait is used to define how an HTTP request is
    serialized given a specific protocol, authentication scheme, and set of
    input parameters.
Trait selector
    .. code-block:: none

        operation
Value type
    ``list`` of ``HttpRequestTestCase`` structures

The ``httpRequestTests`` trait is a list of ``HttpRequestTestCase`` structures
that support the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - id
      - ``string``
      - **Required**. The identifier of the test case. This identifier can
        be used by protocol test implementations to filter out unsupported
        test cases by ID, to generate test case names, etc. The provided
        ``id`` MUST match Smithy's :token:`smithy:Identifier` ABNF. No two
        ``httpRequestTests`` test cases can share the same ID.
    * - protocol
      - shape ID
      - **Required**. A shape ID that targets a shape marked with the
        :ref:`protocolDefinition-trait`. Because Smithy services can support
        multiple protocols, each test MUST specify which protocol is under
        test.
    * - method
      - ``string``
      - **Required**. The expected serialized HTTP request method.
    * - uri
      - ``string``
      - **Required**. The request-target of the HTTP request, not including
        the query string (for example, "/foo/bar").
    * - host
      - ``string``
      - The host or endpoint provided as input used to generate the HTTP
        request (for example, "example.com").

        ``host`` MAY contain a path to indicate a base path from which each
        operation in the service is appended to. For example, given a ``host``
        of ``example.com/foo/bar`` and an operation path of ``/MyOperation``,
        the resolved host of the operation is ``example.com`` and the resolved
        path is ``/foo/bar/MyOperation``.
    * - resolvedHost
      - ``string``
      - The expected host present in the ``Host`` header of the request, not
        including the path or scheme (for example, "prefix.example.com"). If no
        resolvedHost is defined, then no assertions are made about the resolved
        host for the request.

        This can differ from the ``host`` provided to the client if the
        operation has a member with the :ref:`endpoint-trait`.

        Server implementations SHOULD ignore discrepancies in paths when
        comparing the ``host`` and ``resolvedHost`` properties.
    * - authScheme
      - shape ID
      - A shape ID that specifies the optional authentication scheme to
        assume. It's possible that specific authentication schemes might
        influence the serialization logic of an HTTP request. The targeted
        shape MUST be marked with the :ref:`authDefinition-trait` trait.
    * - queryParams
      - ``list<string>``
      - A list of the expected serialized query string parameters.

        Each element in the list is a query string key value pair
        that starts with the query string parameter name optionally
        followed by "=", optionally followed by the query string
        parameter value. For example, "foo=bar", "foo=", and "foo"
        are all valid values.

        .. note::

            This kind of list is used instead of a map so that query string
            parameter values for lists can be represented using repeated
            key-value pairs.

        The query string parameter name and the value MUST appear in the
        format in which it is expected to be sent over the wire; if a key or
        value needs to be percent-encoded, then it MUST appear
        percent-encoded in this list.

        A serialized HTTP request is not in compliance with the protocol
        if any query string parameter defined in ``queryParams`` is not
        defined in the request or if the value of a query string parameter
        in the request differs from the expected value.

        ``queryParams`` applies no constraints on additional query parameters.
    * - forbidQueryParams
      - ``list<string>``
      - A list of query string parameter names that must not appear in the
        serialized HTTP request.

        Each value MUST appear in the format in which it is sent over the
        wire; if a key needs to be percent-encoded, then it MUST appear
        percent-encoded in this list.
    * - requireQueryParams
      - ``list<string>``
      - A list of query string parameter names that MUST appear in the
        serialized request URI, but no assertion is made on the value.

        Each value MUST appear in the format in which it is sent over the
        wire; if a key needs to be percent-encoded, then it MUST appear
        percent-encoded in this list.
    * - headers
      - ``map<string, string>``
      - A map of expected HTTP headers. Each key represents a header field
        name and each value represents the expected header value. An HTTP
        request is not in compliance with the protocol if any listed header
        is missing from the serialized request or if the expected header
        value differs from the serialized request value.

        ``headers`` applies no constraints on additional headers.
    * - forbidHeaders
      - [``string``]
      - A list of header field names that must not appear in the serialized
        HTTP request.
    * - requireHeaders
      - [``string``]
      - A list of header field names that must appear in the serialized
        HTTP message, but no assertion is made on the value. Headers listed
        in ``headers`` do not need to appear in this list.
    * - body
      - ``string``
      - The expected HTTP message body. If no request body is defined,
        then no assertions are made about the body of the message. Because
        the ``body`` parameter is a string, binary data MUST be represented
        in ``body`` by base64 encoding the data (for example, use "Zm9vCg=="
        and not "foo").
    * - bodyMediaType
      - ``string``
      - The media type of the ``body``. This is used to help test runners
        to parse and validate the expected data against generated data.
    * - params
      - ``document``
      - For clients, defines the input parameters used to generate the HTTP
        request. For servers, defines the input parameters extracted from the
        HTTP request. These parameters MUST be compatible with the input of the
        operation.

        Parameter values that contain binary data MUST be defined using
        values that can be represented in plain text (for example, use "foo"
        and not "Zm9vCg=="). While this limits the kinds of binary values
        that can be tested in protocol tests, it allows protocol tests to
        demonstrate the requirement of many protocols that binary data is
        automatically base64 encoded and decoded.
    * - vendorParams
      - ``document``
      - Defines vendor-specific parameters that are used to influence the
        request. For example, some vendors might utilize environment
        variables, configuration files on disk, or other means to influence
        the serialization formats used by clients or servers.

        If a ``vendorParamsShape`` is set, these parameters MUST be compatible
        with that shape's definition.
    * - vendorParamsShape
      - shape ID
      - A shape to be used to validate the ``vendorParams`` member contents.

        If set, the parameters in ``vendorParams`` MUST be compatible with this
        shape's definition.
    * - documentation
      - ``string``
      - A description of the test and what is being asserted defined in
        CommonMark_.
    * - tags
      - ``[string]``
      - Attaches a list of tags that allow test cases to be categorized and
        grouped.
    * - appliesTo
      - ``string``, one of "client" or "server"
      - Indicates that the test case is only to be implemented by "client" or
        "server" implementations. This property is useful for identifying and
        testing edge cases of clients and servers that are impossible or
        undesirable to test in *both* client and server implementations. For
        example, a "server" test might be useful to ensure a service can
        gracefully receive a request that optionally contains a payload.

        Is is assumed that test cases that do not define an ``appliesTo``
        member are implemented by both client and server implementations.


HTTP request example
====================

The following example defines a protocol compliance test for a JSON protocol
that uses :ref:`HTTP binding traits <http-traits>`.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    use smithy.test#httpRequestTests

    @endpoint(hostPrefix: "{hostLabel}.prefix.")
    @http(method: "POST", uri: "/")
    @httpRequestTests([
        {
            id: "say_hello"
            protocol: exampleProtocol
            params: {
                "hostLabel": "foo"
                "greeting": "Hi"
                "name": "Teddy"
                "query": "Hello there"
            }
            method: "POST"
            host: "example.com"
            resolvedHost: "foo.prefix.example.com"
            uri: "/"
            queryParams: [
                "Hi=Hello%20there"
            ]
            headers: {
                "X-Greeting": "Hi"
            }
            body: "{\"name\": \"Teddy\"}"
            bodyMediaType: "application/json"
        }
    ])
    operation SayHello {
        input: SayHelloInput
        output: Unit
    }

    @input
    structure SayHelloInput {
        @required
        @hostLabel
        hostLabel: String

        @httpHeader("X-Greeting")
        greeting: String

        @httpQuery("Hi")
        query: String

        name: String
    }


.. smithy-trait:: smithy.test#httpResponseTests
.. _httpResponseTests-trait:

-----------------
httpResponseTests
-----------------

Summary
    The ``httpResponseTests`` trait is used to define how an HTTP response
    is serialized given a specific protocol, authentication scheme, and set
    of output or error parameters.
Trait selector
    .. code-block:: none

        :test(operation, structure[trait|error])
Value type
    ``list`` of ``HttpResponseTestCase`` structures

The ``httpResponseTests`` trait is a list of ``HttpResponseTestCase``
structures that support the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - id
      - ``string``
      - **Required**. The identifier of the test case. This identifier can
        be used by protocol test implementations to filter out unsupported
        test cases by ID, to generate test case names, etc. The provided
        ``id`` MUST match Smithy's :token:`smithy:Identifier` ABNF. No two
        ``httpResponseTests`` test cases can share the same ID.
    * - protocol
      - ``string``
      - **Required**. A shape ID that targets a shape marked with the
        :ref:`protocolDefinition-trait` trait. Because Smithy services can
        support multiple protocols, each test MUST specify which protocol is
        under test.
    * - code
      - ``integer``
      - **Required**. The expected HTTP response status code.
    * - authScheme
      - shape ID
      - A shape ID that specifies the optional authentication scheme to
        assume. It's possible that specific authentication schemes might
        influence the serialization logic of an HTTP response. The targeted
        shape MUST be marked with the :ref:`authDefinition-trait` trait.
    * - headers
      - ``map<string, string>``
      - A map of expected HTTP headers. Each key represents a header field
        name and each value represents the expected header value. An HTTP
        response is not in compliance with the protocol if any listed header
        is missing from the serialized response or if the expected header
        value differs from the serialized response value.

        ``headers`` applies no constraints on additional headers.
    * - forbidHeaders
      - ``list<string>``
      - A list of header field names that must not appear in the serialized
        HTTP response.
    * - requireHeaders
      - ``list<string>``
      - A list of header field names that must appear in the serialized
        HTTP response, but no assertion is made on the value. Headers listed
        in ``headers`` do not need to appear in this list.
    * - body
      - ``string``
      - The expected HTTP message body. If no response body is defined,
        then no assertions are made about the body of the message.
    * - bodyMediaType
      - ``string``
      - The media type of the ``body``. This is used to help test runners
        to parse and validate the expected data against generated data.
        Binary media type formats require that the contents of ``body`` are
        base64 encoded.
    * - params
      - ``document``
      - For clients, defines the output or error parameters extracted from the
        HTTP response. For servers, defines the output or error parameters used
        to generate the HTTP response. These parameters MUST be compatible with
        the targeted operation's output or the targeted error structure.

        Parameter values that contain binary data MUST be defined using
        values that can be represented in plain text (for example, use "foo"
        and not "Zm9vCg=="). While this limits the kinds of binary values
        that can be tested in protocol tests, it allows protocol tests to
        demonstrate the requirement of many protocols that binary data is
        automatically base64 encoded and decoded.
    * - vendorParams
      - ``document``
      - Defines vendor-specific parameters that are used to influence the
        response. For example, some vendors might utilize environment
        variables, configuration files on disk, or other means to influence
        the serialization formats used by clients or servers.

        If a ``vendorParamsShape`` is set, these parameters MUST be compatible
        with that shape's definition.
    * - vendorParamsShape
      - shape ID
      - A shape to be used to validate the ``vendorParams`` member contents.

        If set, the parameters in ``vendorParams`` MUST be compatible with this
        shape's definition.
    * - documentation
      - ``string``
      - A description of the test and what is being asserted defined in
        CommonMark_.
    * - tags
      - ``[string]``
      - Attaches a list of tags that allow test cases to be categorized and
        grouped.
    * - appliesTo
      - ``string``, one of "client" or "server"
      - Indicates that the test case is only to be implemented by "client" or
        "server" implementations. This property is useful for identifying and
        testing edge cases of clients and servers that are impossible or
        undesirable to test in *both* client and server implementations. For
        example, a "client" test might be useful to ensure a client can
        gracefully receive a response that optionally contains a payload.

        Is is assumed that test cases that do not define an ``appliesTo``
        member are implemented by both client and server implementations.


HTTP response example
=====================

The following example defines a protocol compliance test for a JSON protocol
that uses :ref:`HTTP binding traits <http-traits>`.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    use smithy.test#httpResponseTests

    @http(method: "POST", uri: "/")
    @httpResponseTests([
        {
            id: "say_goodbye"
            protocol: exampleProtocol
            params: {farewell: "Bye"}
            code: 200
            headers: {
                "X-Farewell": "Bye"
                "Content-Length": "0"
            }
        }
    ])
    operation SayGoodbye {
        input: SayGoodbyeInput
        output: SayGoodbyeOutput
    }

    @input
    structure SayGoodbyeInput {}

    @output
    structure SayGoodbyeOutput {
        @httpHeader("X-Farewell")
        farewell: String
    }


HTTP error response example
===========================

The ``httpResponseTests`` trait can be applied to error structures to define
how an error HTTP response is serialized. Client protocol compliance test
implementations SHOULD ensure that each error with the ``httpResponseTests``
trait associated with an operation can be properly deserialized.

The following example defines a protocol compliance test for a JSON protocol
that uses :ref:`HTTP binding traits <http-traits>`.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    use smithy.test#httpResponseTests

    @error("client")
    @httpError(400)
    @httpResponseTests([
        {
            id: "invalid_greeting"
            protocol: exampleProtocol
            params: {foo: "baz", message: "Hi"}
            code: 400
            headers: {"X-Foo": "baz"}
            body: "{\"message\": \"Hi\"}"
            bodyMediaType: "application/json"
        }
    ])
    structure InvalidGreeting {
        @httpHeader("X-Foo")
        foo: String

        message: String
    }


.. smithy-trait:: smithy.test#httpMalformedRequestTests
.. _httpMalformedRequestTests-trait:

-------------------------
httpMalformedRequestTests
-------------------------

Summary
    The ``httpMalformedRequestTests`` trait is used to define how a malformed
    HTTP request is rejected given a specific protocol and HTTP message.
    Protocol implementations MUST assert that requests are rejected during
    request processing.

Trait selector
    .. code-block:: none

        operation
Value type
    ``list`` of ``HttpMalformedRequestTestCase`` structures

The ``httpMalformedRequestTests`` trait is a list of
``HttpMalformedRequestTestCase`` structures that support the following members:

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - id
      - ``string``
      - **Required**. The identifier of the test case. This identifier can
        be used by protocol test implementations to filter out unsupported
        test cases by ID, to generate test case names, etc. The provided
        ``id`` MUST match Smithy's :token:`smithy:Identifier` ABNF. No two
        ``httpMalformedRequestTests`` test cases can share the same ID.
    * - protocol
      - shape ID
      - **Required**. A shape ID that targets a shape marked with the
        :ref:`protocolDefinition-trait`. Because Smithy services can support
        multiple protocols, each test MUST specify which protocol is under
        test.
    * - request
      - :ref:`HttpMalformedRequestDefinition <HttpMalformedRequestDefinition-struct>`
      - **Required**. A structure that describes the request.
    * - response
      - :ref:`HttpMalformedResponseDefinition <HttpMalformedResponseDefinition-struct>`
      - **Required**. A structure that describes the required response.
    * - documentation
      - ``string``
      - A description of the test and what is being asserted defined in
        CommonMark_.
    * - tags
      - ``[string]``
      - Attaches a list of tags that allow test cases to be categorized and
        grouped.

        Using tags to describe types of failures gives implementations control
        of test execution across different suites of tests. For example, it
        allows tests to be executed that exercise booleans being converted into
        numerics, even if there are such tests written for values appearing in
        paths, query strings, headers, and message bodies across different
        protocols.
    * - testParameters
      - ``map<string, list<string>>``
      - Optional parameters that are substituted into each member of
        ``request``, ``response``, as well as the test's ``tags`` and
        ``documentation``.

        The lists of values for each key must be identical
        in length. One test permutation is generated for each index the
        parameter lists. For example, parameters with 5 values for each key
        will generate 5 tests in total.

        Parameter values are substituted using the conventions described by
        the documentation for CodeWriter_. They are available as named
        parameters, and implementations must support both the ``L`` and ``S``
        formatters.

        .. note::

            If ``testParameters`` is not null or empty, then substitution
            is performed on every string in ``request`` and ``response``
            even when there is no substitution requested. This means that
            explicit `$` characters must be represented as `$$` so as to not be
            interpreted as substitution expressions by the code generator.

.. _HttpMalformedRequestDefinition-struct:

HttpMalformedRequestDefinition
==============================

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - method
      - ``string``
      - **Required**. The HTTP request method.
    * - uri
      - ``string``
      - **Required**. The request-target of the HTTP request, not including
        the query string (for example, "/foo/bar").
    * - host
      - ``string``
      - The host or endpoint provided as input used to generate the HTTP
        request (for example, "example.com").
    * - queryParams
      - ``list<string>``
      - A list of the serialized query string parameters to include in the
        request.

        Each element in the list is a query string key value pair
        that starts with the query string parameter name optionally
        followed by "=", optionally followed by the query string
        parameter value. For example, "foo=bar", "foo=", and "foo"
        are all valid values.

        .. note::

            This kind of list is used instead of a map so that query string
            parameter values for lists can be represented using repeated
            key-value pairs.

        The query string parameter name and the value MUST appear in the
        format in which it is expected to be sent over the wire; if a key or
        value needs to be percent-encoded, then it MUST appear
        percent-encoded in this list.
    * - headers
      - ``map<string, string>``
      - A map of HTTP headers to include in the request. Each key represents a
        header field name and each value represents the expected header value.
    * - body
      - ``string``
      - The HTTP message body to include in the request. Because the ``body``
        parameter is a string, binary data MUST be represented in ``body`` by
        base64 encoding the data (for example, use "Zm9vCg==" and not "foo").

.. _HttpMalformedResponseDefinition-struct:


HttpMalformedResponseDefinition
===============================

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - headers
      - ``map<string, string>``
      - A map of expected HTTP headers. Each key represents a header field
        name and each value represents the expected header value. An HTTP
        response is not in compliance with the protocol if any listed header
        is missing from the serialized response or if the expected header
        value differs from the serialized response value.

        ``headers`` applies no constraints on additional headers.
    * - code
      - ``integer``
      - **Required**. The expected HTTP response status code.
    * - body
      - :ref:`HttpMalformedResponseBodyDefinition <HttpMalformedResponseBodyDefinition-struct>`
      - The expected response body.

.. _HttpMalformedResponseBodyDefinition-struct:


HttpMalformedResponseBodyDefinition
-----------------------------------

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - assertion
      - :ref:`HttpMalformedResponseBodyAssertion <HttpMalformedResponseBodyAssertion-union>`
      - **Required**. The assertion to be applied to the response body.
    * - mediaType
      - ``string``
      - **Required**. The media type of the ``body``. This is used to help test
        runners to parse and validate the expected data against generated data.
        Binary media type formats require that the contents of ``body`` are
        base64 encoded.

.. _HttpMalformedResponseBodyAssertion-union:

HttpMalformedResponseBodyAssertion
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A union describing the assertion to run against the response body. As it is a
union, exactly one member must be set.

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - contents
      - ``string``
      - Defines the expected serialized response body, which will be matched
        exactly.
    * - messageRegex
      - ``string``
      - A regex to evaluate against the ``message`` field in the response body.
        For responses that may have some variance from platform to platform,
        such as those that include messages from a parser.

HTTP malformed request example
==============================

The following example defines a malformed request test for a JSON protocol
that uses :ref:`HTTP binding traits <http-traits>`. In this example, the server
is rejecting many different variants of invalid numerics, and uses
``testParameters`` to test three different invalid values, and tags each test
with a descriptive string that allows implementations to run, or skip,
specific types of malformed values.

.. code:: smithy

    $version: "2"
    namespace smithy.example

    use smithy.test#httpMalformedRequestTests
    @http(method: "POST", uri: "/InvertNumber/{numberValue}")
    @httpMalformedRequestTests([
        {
            id: "MalformedLongsInPathsRejected",
            documentation: """
            Malformed values in the path should be rejected""",
            protocol: exampleProtocol,
            request: {
                method: "POST",
                uri: "/InvertNumber/$value:L"
            },
            response: {
                code: 400,
                headers: {
                    "errorType": "BadNumeric"
                },
                body: {
                    assertion: {
                        contents: """
                        {"errorMessage": "Invalid value \"$value:L\""}"""
                    },
                    mediaType: "application/json"
                }

            },
            testParameters : {
                "value" : ["true", "1.001", "2ABC"],
                "tag" : ["boolean_coercion", "float_truncation", "trailing_chars"]
            },
            tags: [ "$tag:L" ]
        }
    ])
    operation InvertNumber {
        input: InvertNumberInput
    }

    structure InvertNumberInput {
        @httpLabel
        @required
        numberValue: Long
    }

.. _CommonMark: https://spec.commonmark.org/
.. _CodeWriter: https://smithy.io/javadoc/__smithy_version__/software/amazon/smithy/utils/CodeWriter.html
