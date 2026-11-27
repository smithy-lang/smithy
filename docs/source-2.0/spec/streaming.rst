.. _stream-traits:

=========
Streaming
=========

Smithy operations can send and receive streams of data (:ref:`data-streams`)
or streams of events (:ref:`event-streams`).


.. _data-streams:

Data streams
============

Operations can contain :dfn:`data streams`, a potentially unbounded sequence of
bytes or UTF-8 characters. A data stream is created by applying the
:ref:`streaming-trait` to a blob. This stream can then be referenced
by a single top-level operation input or output structure member. For example:

.. code-block:: smithy

    @http(method: "GET", uri: "/streaming-operation")
    operation StreamingOperation {
        input := {}
        output := {
            @httpPayload
            output: StreamingBlob = ""
        }
    }

    @streaming
    blob StreamingBlob

Data streams are unbounded by default. In rare cases, it may be necessary to send
bounded data of a known size but indicate to applications that the data SHOULD NOT
be stored in memory. Data streams can be configured to require a known size by
applying the :ref:`requiresLength-trait`. For example:

.. code-block:: smithy

    // This stream must has a known size.
    @streaming
    @requiresLength
    blob FiniteStreamingBlob


.. _event-streams:

Event streams
=============

An event stream is an abstraction that allows multiple messages to be sent
asynchronously between a client and server. Event streams support both duplex
and simplex streaming. The serialization format and framing of messages sent
over event streams is defined by the :ref:`protocol <protocolDefinition-trait>`
of a service.

An event stream is formed when an input or output member of an operation
targets a union marked with the :ref:`streaming-trait`. An event stream is
capable of streaming any number of named event structure shapes defined by a
union. Each member of the targeted union MUST target a structure shape. The
member names of the union define the name that is used to identify each event
that is sent over the event stream.

.. _input-eventstream:

The following example defines an operation that uses an event
stream in its input by referencing a member that targets a union:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    operation PublishMessages {
        input: PublishMessagesInput
    }

    @input
    structure PublishMessagesInput {
        room: String
        messages: PublishEvents
    }

    @streaming
    union PublishEvents {
        message: Message
        leave: LeaveEvent
    }

    structure Message {
        message: String
    }

    structure LeaveEvent {}


.. _output-eventstream:

The following example defines an operation that uses an event
stream in its output:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    operation SubscribeToMovements {
        input: SubscribeToMovementsInput,
        output: SubscribeToMovementsOutput
    }

    @input
    structure SubscribeToMovementsInput {}

    @output
    structure SubscribeToMovementsOutput {
        movements: MovementEvents
    }

    @streaming
    union MovementEvents {
        up: Movement
        down: Movement
        left: Movement
        right: Movement
        throttlingError: ThrottlingError
    }

    structure Movement {
        velocity: Float
    }

    /// An example error emitted when the client is throttled
    /// and should terminate the event stream.
    @error("client")
    @retryable(throttling: true)
    structure ThrottlingError {}


Modeled errors in event streams
-------------------------------

Event streams MAY target shapes marked with the :ref:`error-trait`. These
events are considered terminal errors and MUST terminate the event stream
when received.


.. _initial-messages:

Initial messages
----------------

An *initial message* is comprised of the top-level input or output members
of an operation that do not target the event stream union. Initial
messages provide an opportunity for a client or server to provide metadata
about an event stream before transmitting events.


.. _initial-request:

Initial-request
~~~~~~~~~~~~~~~

An *initial-request* is an initial message that can be sent from a client to
a server for an operation with an input event stream. The structure of an
initial-request is the input of an operation with no value provided for the
event stream member. An initial-request, if sent, is sent from a client to a
server before sending any event stream events.

When using :ref:`HTTP bindings <http-traits>`, initial-request fields are
mapped to specific locations in the HTTP request such as headers or the
URI. In other bindings or protocols, the initial-request can be
sent however is necessary for the protocol.

The following example defines an operation with an input event stream with
an initial-request. The client will first send the initial-request to the
service, followed by the events sent in the payload of the HTTP message.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @http(method: "POST", uri: "/messages/{room}")
    operation PublishMessages {
        input: PublishMessagesInput
    }

    @input
    structure PublishMessagesInput {
        @httpLabel
        @required
        room: String

        @httpPayload
        messages: MessageStream
    }

    @streaming
    union MessageStream {
        message: Message
    }

    structure Message {
        message: String
    }


.. _initial-response:

Initial-response
~~~~~~~~~~~~~~~~

An *initial-response* is an initial message that can be sent from a server
to a client for an operation with an output event stream. The structure of
an initial-response is the output of an operation with no value provided for
the event stream member. An initial-response, if sent, is sent from the
server to the client before sending any event stream events.

When using :ref:`HTTP bindings <http-traits>`, initial-response fields are
mapped to HTTP headers. In other protocols, the initial-response can be sent
however is necessary for the protocol.

The following example defines an operation with an output event stream with
an initial-response. The client will first receive and process the
initial-response, followed by the events sent in the payload of the HTTP
message.

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @http(method: "GET", uri: "/messages/{room}")
    operation SubscribeToMessages {
        input: SubscribeToMessagesInput
        output: SubscribeToMessagesOutput
    }

    @input
    structure SubscribeToMessagesInput {
        @httpLabel
        @required
        room: String
    }

    @output
    structure SubscribeToMessagesOutput {
        @httpHeader("X-Connection-Lifetime")
        connectionLifetime: Integer

        @httpPayload
        messages: MessageStream
    }


Initial message client and server behavior
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Initial messages, if received, MUST be provided to applications
before event stream events.

It is a backward compatible change to add an initial-request or
initial-response to an existing operation; clients MUST NOT fail if an
unexpected initial-request or initial-response is received. Clients and
servers MUST NOT fail if an initial-request or initial-response is not
received for an initial message that contains only optional members.


.. _event-stream-client-behavior:

Client behavior
---------------

Clients that send or receive event streams are expected to
provide an abstraction to end-users that allows values to be produced or
consumed asynchronously for each named member of the targeted union. Adding
new events to an event stream union is considered a backward compatible
change; clients SHOULD NOT fail when an unknown event is received. Clients
MUST provide access to the :ref:`initial-message <initial-messages>` of an
event stream when necessary.

Clients SHOULD expose type-safe functionality that is used to dispatch based
on the name of an event. For example, given the following event stream:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    operation SubscribeToEvents {
        input: SubscribeToEventsInput,
        output: SubscribeToEventsOutput
    }

    @input
    structure SubscribeToEventsInput {}

    @output
    structure SubscribeToEventsOutput {
        events: Events
    }

    @streaming
    union Events {
        a: Event1
        b: Event2
        c: Event3
    }

    structure Event1 {}
    structure Event2 {}
    structure Event3 {}

An abstraction SHOULD be provided that is used to dispatch based on the
name of an event (that is, ``a``, ``b``, or ``c``) and provide the associated
type (for example, when ``a`` is received, an event of type ``Event1`` is
provided).


.. _event-message-serialization:

Event message serialization
---------------------------

While the framing and serialization of an event stream is protocol-specific,
traits can be used to influence the serialization of an event stream event.
Structure members that are sent as part of an event stream are serialized
in either a header or the payload of an event.

The :ref:`eventHeader-trait` is used to serialize a structure member as an
event header. The payload of an event is defined by either marking a single
member with the :ref:`eventpayload-trait`, or by combining all members that
are not marked with the ``eventHeader`` or ``eventPayload`` trait into a
protocol-specific document.

The following example serializes the "a" and "b" members as event
headers and the "c" member as the payload.

.. code-block:: smithy

    structure ExampleEvent {
        @eventHeader
        a: String

        @eventHeader
        b: String

        @eventPayload
        c: Blob
    }

The following example serializes the "a", "b", and "c" members as the payload
of the event using a protocol-specific document. For example, when using a JSON
based protocol, the event payload is serialized as a JSON object:

.. code-block:: smithy

    structure ExampleEvent {
        a: String
        b: String
        c: Blob
    }

Event stream traits
-------------------

.. smithy-trait:: smithy.api#eventHeader
.. _eventheader-trait:

``eventHeader`` trait
~~~~~~~~~~~~~~~~~~~~~

Summary
    Binds a member of a structure to be serialized as an event header when
    sent through an event stream.
Trait selector
    .. code-block:: none

        structure >
        :test(member > :test(boolean, byte, short, integer, long, blob, string, timestamp))

    *Member of a structure that targets a boolean, byte, short, integer, long, blob, string, or timestamp shape*
Value type
    Annotation trait.
Conflicts with
   :ref:`eventpayload-trait`

.. important::

    Not all protocols support event headers. For example, MQTT version 3.1.1
    does not support custom message headers. It is a protocol-level concern
    as to if and how event stream headers are serialized.

The following example defines multiple event headers:

.. code-block:: smithy

    structure ExampleEvent {
        @eventHeader
        a: String

        @eventHeader
        b: String
    }

.. smithy-trait:: smithy.api#eventPayload
.. _eventpayload-trait:

``eventPayload`` trait
~~~~~~~~~~~~~~~~~~~~~~

Summary
    Binds a member of a structure to be serialized as the payload of an
    event sent through an event stream.
Trait selector
    .. code-block:: none

        structure > :test(member > :test(blob, string, structure, union))

    *Structure member that targets a blob, string, structure, or union*
Value type
    Annotation trait.
Conflicts with
   :ref:`eventheader-trait`
Validation
    1. This trait is *structurally exclusive by member*, meaning only a
       single member of a structure can be targeted by the trait.
    2. If the ``eventPayload`` trait is applied to a structure member,
       then all other members of the structure MUST be marked with the
       ``eventHeader`` trait.

Event payload is serialized using the following logic:

* A blob and string is serialized using the bytes of the string or blob.
* A structure and union is serialized as a protocol-specific document.

The following example defines an event header and sends a blob as the payload
of an event:

.. code-block:: smithy

    structure ExampleEvent {
        @eventPayload
        a: String

        @eventHeader
        b: String
    }

The following structure is **invalid** because the "a" member is bound to the
``eventPayload``, and the "b" member is not bound to an ``eventHeader``.

.. code-block:: smithy

    structure ExampleEvent {
        @eventPayload
        a: String

        b: String
        // ^ Error: not bound to an eventHeader.
    }


.. smithy-trait:: smithy.api#streaming
.. _streaming-trait:

``streaming`` trait
===================

Summary
    Indicates that the data represented by the shape needs to be streamed.

    When applied to a blob, this simply means that the data could be very
    large and thus should not be stored in memory or that the size is unknown
    at the start of the request.

    When applied to a union, it indicates that shape represents an
    :ref:`event stream <event-streams>`.
Trait selector::
    ``:is(blob, union)``
Value type
    Annotation trait
Validation
    * Members that target a streaming blob MUST be marked with the
      :ref:`required-trait` or :ref:`default-trait`.
    * ``streaming`` shapes can only be referenced from top-level members
      of operation input or output structures.
    * Structures that contain a member that targets a ``streaming`` shape
      MUST NOT be targeted by other members.
    * The ``streaming`` trait is *structurally exclusive by target*, meaning
      only a single member of a structure can target a shape marked as
      ``streaming``.
    * If a service supports the :ref:`httpPayload-trait`, any member that
      targets a ``streaming`` shape must also be marked as ``@httpPayload``.


Deserializing streaming blobs
-----------------------------

It is often impossible for a server to know if a zero-length payload was sent
by a client, or if no payload was explicitly sent by a client. As a result,
required streaming members SHOULD be interpreted by deserializers as if they
have a default zero-length value when not present.


.. smithy-trait:: smithy.api#requiresLength
.. _requiresLength-trait:

``requiresLength`` trait
========================

Summary
    Indicates that the streaming blob MUST be finite and have a known
    size when sending data from a client to a server.

    In an HTTP-based protocol, this trait indicates that the
    ``Content-Length`` header MUST be sent prior to a client sending the
    payload of a request. This can be useful for services that need to
    determine if a request will be accepted based on its size or where to
    store data based on the size of the stream.
Trait selector::
    ``blob[trait|streaming]``

    *A blob shape marked with the streaming trait*
Value type
    ``structure``
Validation
    * ``requiresLength`` shapes can only be referenced from top-level members
      of operation input structures.

.. code-block:: smithy

    @streaming
    @requiresLength
    blob FiniteStreamingBlob


.. smithy-trait:: smithy.api#trailer
.. _trailer-trait:

``trailer`` trait
=================

Summary
    Indicates that the value of a structure member can only be resolved after
    the sibling streaming blob member of the structure has been fully
    consumed.
Trait selector
    .. code-block:: none

        structure :not(<-[input]- operation)
                  :test(> member > blob[trait|streaming])
        > member :not(> [trait|streaming])

    *A member of a structure that is not used as operation input and that
    contains a member that targets a streaming blob, excluding the streaming
    member itself*
Value type
    Annotation trait.
Conflicts with
    :ref:`httpHeader-trait`, :ref:`httpPrefixHeaders-trait`,
    :ref:`httpPayload-trait`, :ref:`httpQuery-trait`,
    :ref:`httpQueryParams-trait`, :ref:`httpLabel-trait`,
    :ref:`httpResponseCode-trait`

.. warning::
    This trait is unstable and MAY change in the future.

Some values can only be known after a stream has been sent, such as a checksum
of the streamed data. The ``trailer`` trait marks members that carry these
values, called :dfn:`trailer members`. The trait only defines when the value of
a trailer member can be resolved. Protocols define how the value is populated.
For example, an HTTP-based protocol could send the value in an HTTP trailer
field or in the trailing headers of an ``aws-chunked`` encoded body. Trailer
members MUST NOT be serialized into the protocol-specific document of a
message, such as the JSON object or XML document that is sent before the
stream.

Protocols that define how trailer members are populated SHOULD list
``smithy.api#trailer`` in the ``traits`` property of their
:ref:`protocolDefinition <protocolDefinition-trait>` trait. Trailer members
of operations marked with the :ref:`http-trait` receive an
``HttpPayload.UnboundTrailer`` validation event. The event is a ``WARNING`` if
at least one protocol is applied to the services that contain the member's
structure and every one of those protocols lists ``smithy.api#trailer``.
Otherwise, it is a ``DANGER``. See :ref:`http-payload-binding`.

The ``trailer`` trait can only be applied to the non-streaming members of a
structure that contains a member targeting a streaming blob, and only when that
structure is not used as operation input. Members of structures that contain an
:ref:`event stream <event-streams>` are not currently supported.

The following example defines an operation that downloads a file and sends a
checksum of the file's contents after the contents have been sent:

.. code-block:: smithy

    $version: "2"
    namespace smithy.example

    @readonly
    operation DownloadFile {
        input := {
            @required
            fileId: String
        }
        output := {
            @required
            content: StreamingBlob

            @required
            @trailer
            checksum: String
        }
    }

    @streaming
    blob StreamingBlob


.. _reading-trailer-members:

Reading trailer members
-----------------------

Code generators MUST expose trailer members using a late-resolving type, such
as a callback, future, or promise. Consumers of a trailer member, such as a
client reading a response, MUST observe the following behavior:

* Accessing the value before the streaming payload has been fully consumed
  MUST NOT yield a value.
* Accessing the value after reading the streaming payload has failed or been
  abandoned MUST yield an error.
* After the streaming payload has been successfully consumed, accessing the
  value MUST yield the value that was delivered. If no value was delivered:

  * If the member is marked with the :ref:`required-trait`, accessing the
    value MUST yield an error.
  * Otherwise, if the member is marked with the :ref:`default-trait`,
    accessing the value MUST yield the default value.
  * Otherwise, accessing the value MUST yield no value.

In the example above, a client that fully reads ``content`` but does not
receive a ``checksum`` MUST yield an error when ``checksum`` is accessed,
because ``checksum`` is required.


.. _writing-trailer-members:

Writing trailer members
-----------------------

Producers of a trailer member, such as a server writing a response, MUST be
able to supply the value of the member at any point until the streaming payload
has been fully written. A value that is never supplied is not delivered, and
consumers resolve the member as described in :ref:`reading-trailer-members`.
