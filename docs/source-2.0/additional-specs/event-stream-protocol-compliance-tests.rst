.. _event-stream-protocol-compliance-tests:

======================================
Event Stream Protocol Compliance Tests
======================================

Smithy is a protocol-agnostic IDL that tries to abstract the serialization
format of request and response messages sent between a client and server.
Protocol specifications define the serialization format of a protocol, and
protocol compliance tests help to ensure that implementations correctly
implement a protocol specification.


--------
Overview
--------

This specification defines a trait in the ``smithy.test`` namespace that is
used to make assertions about how event streams are serialized and deserialized
for a specific protocol.

:ref:`smithy.test#eventStreamTests <eventStreamTests-trait>`
    Used to define how an event stream is serialized and deserialized given a
    specific protocol, optional initial request/response parameters, and a
    sequence of events.


.. smithy-trait:: smithy.test#eventStreamTests
.. _eventStreamTests-trait:

-----------------
eventStreamTests
-----------------

Summary
    The ``eventStreamTests`` trait is used to define how an event stream is
    serialized and deserialized given a specific protocol and set of events.
Trait selector
    .. code-block:: none

        operation :test(-[input, output]-> structure > member > union[trait|streaming])

    *An operation whose input or output contains an event stream*
Value type
    ``list`` of :ref:`EventStreamTestCase <EventStreamTestCase-struct>` structures


.. _EventStreamTestCase-struct:

EventStreamTestCase
===================

A structure defining an event stream test case.

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - id
      - ``string``
      - **Required**. The identifier of the test case. This identifier can be
        used by protocol test implementations to filter out unsupported test
        cases by ID, to generate test case names, etc. The provided ``id``
        MUST match Smithy's :token:`smithy:Identifier` ABNF. No two test cases
        can share the same ID.
    * - protocol
      - shape ID
      - **Required**. A shape ID that targets a shape marked with the
        :ref:`protocolDefinition-trait`. Because Smithy services can support
        multiple protocols, each test MUST specify which protocol is under test.
    * - initialRequestParams
      - ``document``
      - The input parameters used to generate the initial request. These
        parameters MUST be compatible with the input shape of the operation.
    * - initialRequest
      - ``document``
      - The protocol-specific initial request. If an ``initialRequestShape``
        is set, this value MUST be compatible with that shape's definition.
    * - initialRequestShape
      - shape ID
      - A shape ID that targets a structure shape used to validate the
        ``initialRequest`` member's contents. For HTTP protocols, use
        ``smithy.test#InitialHttpRequest``.
    * - initialResponseParams
      - ``document``
      - The output parameters used to generate the initial response. These
        parameters MUST be compatible with the output shape of the operation.
    * - initialResponse
      - ``document``
      - The protocol-specific initial response. If an ``initialResponseShape``
        is set, this value MUST be compatible with that shape's definition.
    * - initialResponseShape
      - shape ID
      - A shape ID that targets a structure shape used to validate the
        ``initialResponse`` member's contents. For HTTP protocols, use
        ``smithy.test#InitialHttpResponse``.
    * - events
      - ``list`` of :ref:`Event <Event-struct>` structures
      - A list of events to be sent over the event stream. This includes
        input message, output message, and error events.
    * - expectation
      - :ref:`TestExpectation <TestExpectation-union>`
      - The kind of result that is expected from the event stream. If not
        set, the result is expected to be success.
    * - vendorParams
      - ``document``
      - Defines vendor-specific parameters that are used to influence the
        request. For example, some vendors might utilize environment variables,
        configuration files on disk, or other means to influence the
        serialization formats used by clients or servers.

        If a ``vendorParamsShape`` is set, these parameters MUST be compatible
        with that shape's definition.
    * - vendorParamsShape
      - shape ID
      - A shape to be used to validate the ``vendorParams`` member contents.

        If set, the parameters in ``vendorParams`` MUST be compatible with this
        shape's definition.
    * - documentation
      - ``string``
      - A description of the test and what is being asserted.
    * - appliesTo
      - ``string``, one of "client" or "server"
      - Indicates that the test case is only to be implemented by "client" or
        "server" implementations. This property is useful for identifying and
        testing edge cases of clients and servers that are impossible or
        undesirable to test in *both* client and server implementations.

        It is assumed that test cases that do not define an ``appliesTo``
        member are implemented by both client and server implementations.
    * - tags
      - ``[string]``
      - Attaches a list of tags that allow test cases to be categorized and
        grouped.


.. _Event-struct:

Event
=====

A structure defining a single event sent over the event stream.

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - type
      - ``string``
      - **Required**. The type of event. MUST be one of ``"request"`` or
        ``"response"``.
    * - params
      - ``document``
      - The parameters used to generate the event. If set, these parameters
        MUST be compatible with a modeled event. If not set, this event
        represents an unmodeled event.
    * - headers
      - ``map<string,`` :ref:`EventHeaderValue <EventHeaderValue-union>` ``>``
      - A map of expected event headers. Headers that are not listed in this
        map are ignored unless they are explicitly forbidden through
        ``forbidHeaders``.
    * - forbidHeaders
      - ``[string]``
      - A list of header field names that must not appear in the serialized
        event.
    * - requireHeaders
      - ``[string]``
      - A list of header field names that must appear in the serialized event,
        but no assertion is made on the value. Headers listed in ``headers``
        do not need to appear in this list.
    * - body
      - ``blob``
      - The expected event body. If no body is defined, then no assertions are
        made about the body of the event.
    * - bodyMediaType
      - ``string``
      - The media type of the ``body``. This is used to help test runners to
        parse and validate the expected data against generated data.
    * - bytes
      - ``blob``
      - An optional binary representation of the entire event. This is used
        to test deserialization. If set, implementations SHOULD use this value
        to represent the binary value of received events rather than
        constructing that binary value from the other properties of the event.

        This value SHOULD NOT be used to make assertions about serialized
        events, as such assertions are unlikely to be reliable due to
        unspecified ordering, optional whitespace, undefined header ordering,
        and common event framing features such as checksums.
    * - vendorParams
      - ``document``
      - Defines vendor-specific parameters that are used to influence the
        event. If a ``vendorParamsShape`` is set, these parameters MUST be
        compatible with that shape's definition.
    * - vendorParamsShape
      - shape ID
      - A shape to be used to validate the ``vendorParams`` member contents.
        If set, the parameters in ``vendorParams`` MUST be compatible with
        this shape's definition.


.. _EventHeaderValue-union:

EventHeaderValue
----------------

Event header values use a typed union to disambiguate types that a plain
string map cannot represent. The union value type MUST be serialized and
deserialized without any wrapping from the union itself.

.. list-table::
    :header-rows: 1
    :widths: 20 80

    * - Member
      - Description
    * - ``boolean``
      - A boolean header value.
    * - ``byte``
      - A byte header value. MUST be written in the model as a base64-encoded
        string (e.g., ``Zm9v`` represents UTF-8 ``foo``).
    * - ``short``
      - A short header value.
    * - ``integer``
      - An integer header value.
    * - ``long``
      - A long header value.
    * - ``blob``
      - A blob header value.
    * - ``string``
      - A string header value.
    * - ``timestamp``
      - A timestamp header value.


.. _TestExpectation-union:

TestExpectation
===============

A union describing the expected outcome of the test case. Exactly one member
must be set.

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - success
      - ``Unit``
      - Indicates that the test is expected to complete successfully. No other
        assertions are made about the outcome.
    * - failure
      - :ref:`TestFailureExpectation <TestFailureExpectation-struct>`
      - Indicates that the test is expected to throw an error.


.. _TestFailureExpectation-struct:

TestFailureExpectation
----------------------

.. list-table::
    :header-rows: 1
    :widths: 10 25 65

    * - Property
      - Type
      - Description
    * - errorId
      - shape ID
      - If specified, the error thrown MUST be of the targeted error shape
        type.


HTTP initial message shapes
===========================

For HTTP-based protocols, two structures are provided to validate the
protocol-specific representation of initial messages:

``smithy.test#InitialHttpRequest``
    Used as the ``initialRequestShape`` to validate the initial HTTP request
    of an event stream operation. Supports the same members as
    :ref:`httpRequestTests <httpRequestTests-trait>` test cases.

``smithy.test#InitialHttpResponse``
    Used as the ``initialResponseShape`` to validate the initial HTTP response
    of an event stream operation. Supports the same members as
    :ref:`httpResponseTests <httpResponseTests-trait>` test cases.


Event stream test examples
==========================

The following examples are drawn from the ``aws.protocols#restJson1`` protocol
compliance tests.


Success example
---------------

The following example defines a test case that expects a successful outcome.
It asserts that a ``stringPayload`` event with a plain-text body is correctly
serialized in both directions over a bidirectional stream.

.. code-block:: smithy

    @eventStreamTests([
        {
            id: "DuplexStringPayload"
            protocol: restJson1
            events: [
                {
                    type: "request"
                    params: {
                        stringPayload: { payload: "foo" }
                    }
                    headers: {
                        ":message-type": { string: "event" }
                        ":event-type": { string: "stringPayload" }
                        ":content-type": { string: "text/plain" }
                    }
                    body: "foo"
                    bodyMediaType: "text/plain"
                }
                {
                    type: "response"
                    params: {
                        stringPayload: { payload: "foo" }
                    }
                    headers: {
                        ":message-type": { string: "event" }
                        ":event-type": { string: "stringPayload" }
                        ":content-type": { string: "text/plain" }
                    }
                    body: "foo"
                    bodyMediaType: "text/plain"
                }
            ]
        }
    ])
    @http(method: "POST", uri: "/DuplexStream")
    operation DuplexStream {
        input := {
            @httpPayload
            stream: EventStream
        }

        output := {
            @httpPayload
            stream: EventStream
        }
    }

    @streaming
    union EventStream {
        stringPayload: StringPayloadEvent
    }

    structure StringPayloadEvent {
        @eventPayload
        payload: String
    }


Specific failure example
------------------------

The following example defines a test case that expects a failure of a specific
error type.

.. code-block:: smithy

    @eventStreamTests([
        {
            id: "ClientErrorOutput"
            protocol: restJson1
            events: [
                {
                    type: "response"
                    params: {
                        error: { message: "foo" }
                    }
                    headers: {
                        ":message-type": { string: "exception" }
                        ":exception-type": { string: "error" }
                        ":content-type": { string: "application/json" }
                    }
                    body: """
                        {"message":"foo"}"""
                    bodyMediaType: "application/json"
                }
            ]
            expectation: {
                failure: { errorId: ErrorEvent }
            }
            appliesTo: "client"
        }
    ])
    @http(method: "POST", uri: "/OutputStream")
    operation OutputStream {
        output := {
            @httpPayload
            stream: EventStream
        }
    }

    @streaming
    union EventStream {
        error: ErrorEvent
    }

    @error("client")
    structure ErrorEvent {
        message: String
    }


Generic failure example
-----------------------

The following example defines a test case that expects a failure, but not a
specific error since the error returned is not modeled.

.. code-block:: smithy

    @eventStreamTests([
        {
            id: "ClientUnexpectedErrorOutput"
            documentation: "Clients must be able to handle structured, but unmodeled errors."
            protocol: restJson1
            events: [
                {
                    type: "response"
                    headers: {
                        ":message-type": { string: "error" }
                        ":error-code": { string: "internal-error" }
                        ":error-message": { string: "An unknown error occurred." }
                    }
                }
            ]
            expectation: {
                failure: {}
            }
            appliesTo: "client"
        }
    ])
    @http(method: "POST", uri: "/OutputStream")
    operation OutputStream {
        output := {
            @httpPayload
            stream: EventStream
        }
    }

    @streaming
    union EventStream {
        message: MessageEvent
    }

    structure MessageEvent {
        message: String
    }
