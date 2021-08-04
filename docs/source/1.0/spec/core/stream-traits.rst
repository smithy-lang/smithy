.. _stream-traits:

=================
Streaming Traits
=================

A streaming shape is a shape which represents data that is not returned all at
once. This includes both streaming binary data and event streams.

.. contents:: Table of contents
    :depth: 3
    :local:
    :backlinks: none


.. _streaming-trait:

-------------------
``streaming`` trait
-------------------

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
    * ``streaming`` shapes can only be referenced from top-level members
      of operation input or output structures.
    * Structures that contain a member that targets a ``streaming`` shape
      MUST NOT be targeted by other members.
    * The ``streaming`` trait is *structurally exclusive by target*, meaning
      only a single member of a structure can target a shape marked as
      ``streaming``.

.. tabs::

    .. code-tab:: smithy

        operation StreamingOperation {
            output: StreamingOperationOutput
        }

        structure StreamingOperationOutput {
            streamId: String
            output: StreamingBlob!
        }

        @streaming
        blob StreamingBlob


.. _requiresLength-trait:

------------------------
``requiresLength`` trait
------------------------

Summary
    Indicates that the streaming blob MUST be finite and has a known size.

    In an HTTP-based protocol, for instance, this trait indicates that the
    ``Content-Length`` header MUST be sent prior to a client or server
    sending the payload of a message. This can be useful for services that
    need to determine if a request will be accepted based on its size or
    where to store data based on the size of the stream.
Trait selector::
    ``blob[trait|streaming]``

    *A blob shape marked with the streaming trait*
Value type
    ``structure``

.. tabs::

    .. code-tab:: smithy

        @streaming
        @requiresLength
        blob FiniteStreamingBlob


.. _event-streams:

-------------
Event streams
-------------

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

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        operation PublishMessages {
            input: PublishMessagesInput
        }

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

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#PublishMessages": {
                    "type": "operation",
                    "input": {
                        "target": "smithy.example#PublishMessagesInput"
                    }
                },
                "smithy.example#PublishMessagesInput": {
                    "type": "structure",
                    "members": {
                        "room": {
                            "target": "smithy.api#String"
                        },
                        "messages": {
                            "target": "smithy.example#PublishEvents"
                        }
                    }
                },
                "smithy.example#PublishEvents": {
                    "type": "union",
                    "members": {
                        "message": {
                            "target": "smithy.example#Message"
                        },
                        "leave": {
                            "target": "smithy.example#LeaveEvent"
                        }
                    },
                    "traits": {
                        "smithy.api#streaming": {}
                    }
                },
                "smithy.example#Message": {
                    "type": "structure",
                    "members": {
                        "message": {
                            "target": "smithy.api#String"
                        }
                    }
                }
            }
        }

.. _output-eventstream:

The following example defines an operation that uses an event
stream in its output:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        operation SubscribeToMovements {
            output: SubscribeToMovementsOutput
        }

        structure SubscribeToMovementsOutput {
            movements: MovementEvents
        }

        @streaming
        union MovementEvents {
            up: Movement
            down: Movement
            left: Movement
            right: Movement
        }

        structure Movement {
            velocity: Float
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#SubscribeToMovements": {
                    "type": "operation",
                    "output": {
                        "target": "smithy.example#SubscribeToMovementsOutput"
                    }
                },
                "smithy.example#SubscribeToMovementsOutput": {
                    "type": "structure",
                    "members": {
                        "movements": {
                            "target": "smithy.example#MovementEvents"
                        }
                    }
                },
                "smithy.example#MovementEvents": {
                    "type": "union",
                    "members": {
                        "up": {
                            "target": "smithy.example#Movement"
                        },
                        "down": {
                            "target": "smithy.example#Movement"
                        },
                        "left": {
                            "target": "smithy.example#Movement"
                        },
                        "right": {
                            "target": "smithy.example#Movement"
                        }
                    },
                    "traits": {
                        "smithy.api#streaming": {}
                    }
                },
                "smithy.example#Movement": {
                    "type": "structure",
                    "members": {
                        "velocity": {
                            "target": "smithy.api#Float"
                        }
                    }
                }
            }
        }


.. _initial-messages:

Initial messages
================

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

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @http(method: "POST", uri: "/messages/{room}")
        operation PublishMessages {
            input: PublishMessagesInput
        }

        structure PublishMessagesInput {
            @httpLabel
            room: String!

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

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#PublishMessages": {
                    "type": "operation",
                    "input": {
                        "target": "smithy.example#PublishMessagesInput"
                    },
                    "traits": {
                        "smithy.api#http": {
                            "uri": "/messages/{room}",
                            "method": "POST"
                        }
                    }
                },
                "smithy.example#PublishMessagesInput": {
                    "type": "structure",
                    "members": {
                        "room": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#httpLabel:": {},
                                "smithy.api#required": {}
                            }
                        },
                        "messages": {
                            "target": "smithy.example#MessageStream",
                            "traits": {
                                "smithy.api#httpPayload": {}
                            }
                        }
                    }
                },
                "smithy.example#MessageStream": {
                    "type": "union",
                    "members": {
                        "message": {
                            "target": "smithy.example#Message"
                        }
                    },
                    "traits": {
                        "smithy.api#streaming": {}
                    }
                },
                "smithy.example#Message": {
                    "type": "structure",
                    "members": {
                        "message": {
                            "target": "smithy.api#String"
                        }
                    }
                }
            }
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

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @http(method: "GET", uri: "/messages/{room}")
        operation SubscribeToMessages {
            input: SubscribeToMessagesInput
            output: SubscribeToMessagesOutput
        }

        structure SubscribeToMessagesInput {
            @httpLabel
            room: String!
        }

        structure SubscribeToMessagesOutput {
            @httpHeader("X-Connection-Lifetime")
            connectionLifetime: Integer

            @httpPayload
            messages: MessageStream
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#PublishMessages": {
                    "type": "operation",
                    "input": {
                        "target": "smithy.example#PublishMessagesInput"
                    },
                    "traits": {
                        "smithy.api#http": {
                            "uri": "/messages/{room}",
                            "method": "POST"
                        }
                    }
                },
                "smithy.example#SubscribeToMessagesInput": {
                    "type": "structure",
                    "members": {
                        "room": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#httpLabel:": {},
                                "smithy.api#required": {}
                            }
                        }
                    }
                },
                "smithy.example#SubscribeToMessagesOutput": {
                    "type": "structure",
                    "members": {
                        "connectionLifetime": {
                            "target": "smithy.api#Integer",
                            "traits": {
                                "smithy.api#httpHeader:": "X-Connection-Lifetime"
                            }
                        },
                        "messages": {
                            "target": "smithy.example#MessageStream",
                            "traits": {
                                "smithy.api#httpPayload": {}
                            }
                        }
                    }
                }
            }
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
===============

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

    namespace smithy.example

    operation SubscribeToEvents {
        output: SubscribeToEventsOutput
    }

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
===========================

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

.. tabs::

    .. code-tab:: smithy

        structure ExampleEvent {
            @eventHeader
            a: String

            @eventHeader
            b: String

            @eventPayload
            c: Blob
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#ExampleEvent": {
                    "type": "structure",
                    "members": {
                        "a": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#eventPayload": {}
                            }
                        },
                        "b": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#eventPayload": {}
                            }
                        },
                        "c": {
                            "target": "smithy.api#Blob",
                            "traits": {
                                "smithy.api#eventPayload": {}
                            }
                        }
                    }
                }
            }
        }

The following example serializes the "a", "b", and "c" members as the payload
of the event using a protocol-specific document. For example, when using a JSON
based protocol, the event payload is serialized as a JSON object:

.. tabs::

    .. code-tab:: smithy

        structure ExampleEvent {
            a: String
            b: String
            c: Blob
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#ExampleEvent": {
                    "type": "structure",
                    "members": {
                        "a": {
                            "target": "smithy.api#String"
                        },
                        "b": {
                            "target": "smithy.api#String"
                        },
                        "c": {
                            "target": "smithy.api#Blob"
                        }
                    }
                }
            }
        }

Event stream traits
===================

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

.. tabs::

    .. code-tab:: smithy

        structure ExampleEvent {
            @eventHeader
            a: String

            @eventHeader
            b: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#ExampleEvent": {
                    "type": "structure",
                    "members": {
                        "a": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#eventHeader": {}
                            }
                        },
                        "b": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#eventHeader": {}
                            }
                        }
                    }
                }
            }
        }

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

.. tabs::

    .. code-tab:: smithy

        structure ExampleEvent {
            @eventPayload
            a: String

            @eventHeader
            b: String
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#ExampleEvent": {
                    "type": "structure",
                    "members": {
                        "a": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#eventPayload": {}
                            }
                        },
                        "b": {
                            "target": "smithy.api#String",
                            "traits": {
                                "smithy.api#eventHeader": {}
                            }
                        }
                    }
                }
            }
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
