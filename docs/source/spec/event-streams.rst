.. _event-streams:

==========================
Event stream specification
==========================

An event stream is an abstraction that allows messages to be sent
asynchronously over a persistent connection between a client and server.
Event streams support both duplex and simplex streaming. The serialization
format and framing of messages sent over event streams is defined by the
:ref:`protocol <protocols-trait>` of a service.

Event streams are defined on operations using the
:ref:`inputEventStream-trait` and :ref:`outputEventStream-trait`. These traits
reference a member of the input or output of an operation that targets the
shape sent over the event stream. A member that targets a structure is a
*single-event event stream*, and a member that targets a union is a
*multi-event event stream*.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


.. _single-event-event-stream:

--------------------------
Single-event event streams
--------------------------

A *single-event event stream* is an event stream that streams zero or more
instances of a specific structure shape. It is formed when the
``inputEventStream`` or ``outputEventStream`` trait references an input or
output member that targets a structure.

.. _single-event-input-eventstream:

The following example defines an operation that uses a single-event event
stream in its input by referencing a member that targets a structure:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @inputEventStream(messages)
        operation PublishMessages(PublishMessagesInput)

        structure PublishMessagesInput {
          room: String,
          messages: Message,
        }

        structure Message {
          message: String,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "smithy.example": {
                "shapes": {
                    "PublishMessages": {
                        "type": "operation",
                        "input": "PublishMessagesInput",
                        "inputEventStream": "messages"
                    },
                    "PublishMessagesInput": {
                        "type": "structure",
                        "members": {
                            "room": {
                                "target": "String"
                            },
                            "messages": {
                                "target": "Message"
                            }
                        }
                    },
                    "Message": {
                        "type": "structure",
                        "members": {
                            "message": {
                                "target": "String"
                            }
                        }
                    }
                }
            }
        }

.. _single-event-output-eventstream:

The following example defines an operation that uses a single-event event
stream in its output:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @outputEventStream(movements)
        operation SubscribeToMovements() -> SubscribeToMovementsOutput

        structure SubscribeToMovementsOutput {
          movements: Movement,
        }

        structure Movement {
          angle: Float,
          velocity: Float,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "smithy.example": {
                "shapes": {
                    "SubscribeToMovements": {
                        "type": "operation",
                        "output": "SubscribeToMovementsOutput",
                        "outputEventStream": "movements"
                    },
                    "SubscribeToMovementsOutput": {
                        "type": "structure",
                        "members": {
                            "movements": {
                                "target": "Movement"
                            }
                        }
                    },
                    "Movement": {
                        "type": "structure",
                        "members": {
                            "angle": {
                                "target": "Float"
                            },
                            "velocity": {
                                "target": "Float"
                            }
                        }
                    }
                }
            }
        }


Single-event client behavior
============================

Clients that send or receive single-event event streams are expected to
provide an abstraction to end-users that allows values to be produced or
consumed asynchronously for the targeted event structure. Because a
single-event event stream does not utilize named events like a multi-event
event stream, functionality used to dispatch based on named events is
unnecessary. Clients MUST provide access to the
:ref:`initial-message <initial-messages>` of an event stream when necessary.


.. _multi-event-event-stream:

-------------------------
Multi-event event streams
-------------------------

A *multi-event event stream* is an event stream that streams any number of
named event structure shapes defined by a union. It is formed when an
``inputEventStream`` or ``outputEventStream`` trait references an input or
output member that targets a union. Each member of the targeted union MUST
target a structure shape. The member names of the
union define the name that is used to identify each event that is sent over
the event stream.

.. _multi-event-input-eventstream:

The following example defines an operation that uses a multi-event event
stream in its input by referencing a member that targets a union:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @inputEventStream(messages)
        operation PublishMessages(PublishMessagesInput)

        structure PublishMessagesInput {
          room: String,
          messages: PublishEvents,
        }

        union PublishEvents {
          message: Message,
          leave: LeaveEvent,
        }

        structure Message {
          message: String,
        }

        structure LeaveEvent {}

    .. code-tab:: json

        {
            "smithy": "1.0",
            "smithy.example": {
                "shapes": {
                    "PublishMessages": {
                        "type": "operation",
                        "input": "PublishMessagesInput",
                        "inputEventStream": "messages"
                    },
                    "PublishMessagesInput": {
                        "type": "structure",
                        "members": {
                            "room": {
                                "target": "String"
                            },
                            "messages": {
                                "target": "PublishEvents"
                            }
                        }
                    },
                    "PublishEvents": {
                        "type": "union",
                        "members": {
                            "message": "Message",
                            "leave": "LeaveEvent"
                        }
                    },
                    "Message": {
                        "type": "structure",
                        "members": {
                            "message": {
                                "target": "String"
                            }
                        }
                    }
                }
            }
        }

.. _multi-event-output-eventstream:

The following example defines an operation that uses a multi-event event
stream in its output:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @outputEventStream(movements)
        operation SubscribeToMovements() -> SubscribeToMovementsOutput

        structure SubscribeToMovementsOutput {
          movements: MovementEvents,
        }

        union MovementEvents {
          up: Movement,
          down: Movement,
          left: Movement,
          right: Movement,
        }

        structure Movement {
          velocity: Float,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "smithy.example": {
                "shapes": {
                    "SubscribeToMovements": {
                        "type": "operation",
                        "output": "SubscribeToMovementsOutput",
                        "outputEventStream": "movements"
                    },
                    "SubscribeToMovementsOutput": {
                        "type": "structure",
                        "members": {
                            "movements": {
                                "target": "Message"
                            }
                        }
                    },
                    "MovementEvents": {
                        "type": "union",
                        "members": {
                            "up": {
                                "target": "Movement"
                            },
                            "down": {
                                "target": "Movement"
                            },
                            "left": {
                                "target": "Movement"
                            },
                            "right": {
                                "target": "Movement"
                            }
                        }
                    },
                    "Movement": {
                        "type": "structure",
                        "members": {
                            "velocity": {
                                "target": "Float"
                            }
                        }
                    }
                }
            }
        }


Multi-event client behavior
===========================

Clients that send or receive multi-event event streams are expected to
provide an abstraction to end-users that allows values to be produced or
consumed asynchronously for each named member of the targeted union. Adding
new events to an event stream union is considered a backward compatible
change; clients SHOULD NOT fail when an unknown event is received. Clients
MUST provide access to the :ref:`initial-message <initial-messages>` of an
event stream when necessary.

Clients SHOULD expose type-safe functionality that is used to dispatch based
on the name of an event. For example, given the following event stream,

.. code-block:: smithy

    namespace smithy.example

    @outputEventStream(movements)
    operation SubscribeToEvents() -> SubscribeToEventsOutput

    structure SubscribeToEventsOutput {
      events: Events,
    }

    union Events {
      a: Event1,
      b: Event2,
      c: Event3,
    }

    structure Event1 {}
    structure Event2 {}
    structure Event3 {}

An abstraction SHOULD be provided that is used to dispatch based on the
name of an event (that is, ``a``, ``b``, or ``c``) and provide the associated
type (for example, when ``a`` is received, an event of type ``Event1`` is
provided).


.. _initial-messages:

----------------
Initial messages
----------------

An *initial message* is comprised of the top-level input or output members
of an operation that are not referenced by the ``inputEventStream`` or
``outputEventStream`` traits. Initial messages provide an opportunity for a
client or server to provide metadata about an event stream before transmitting
events.

.. important::

    Not all protocols support intial messages. Check trait binding and
    protocol documentation before adding initial messages to an operation.


.. _initial-request:

Initial-request
===============

An *initial-request* is an initial message that can be sent from a client to
a server for an operation marked with the ``inputEventStream`` trait. The
structure of an initial-request is the input of an operation with no value
provided for the event stream member referenced by the ``inputEventStream``
trait. An initial-request, if sent, is sent from a client to a server before
sending any event stream events.

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

        @inputEventStream(messages)
        @http(method: POST, uri: "/messages/{room}")
        operation PublishMessages(PublishMessagesInput)

        structure PublishMessagesInput {
          @httpLabel
          room: String,

          @httpPayload
          messages: Message,
        }

        structure Message {
          message: String,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "smithy.example": {
                "shapes": {
                    "PublishMessages": {
                        "type": "operation",
                        "input": "PublishMessagesInput",
                        "inputEventStream": "messages",
                        "http": {
                            "uri": "/messages/{room}",
                            "method": "POST"
                        }
                    },
                    "PublishMessagesInput": {
                        "type": "structure",
                        "members": {
                            "room": {
                                "target": "String",
                                "httpLabel:": true
                            },
                            "messages": {
                                "target": "Message",
                                "httpPayload": true
                            }
                        }
                    },
                    "Message": {
                        "type": "structure",
                        "members": {
                            "message": {
                                "target": "String"
                            }
                        }
                    }
                }
            }
        }


.. _initial-response:

Initial-response
================

An *initial-response* is an initial message that can be sent from a server
to a client for an operation marked with the ``outputEventStream`` trait.
The structure of an initial-response is the output of an operation with no
value provided for the event stream member referenced by the
``outputEventStream`` trait. An initial-response, if sent, is sent from the
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

        @outputEventStream(messages)
        @http(method: GET, uri: "/messages/{room}")
        operation SubscribeToMessages(SubscribeToMessagesInput) -> SubscribeToMessagesOutput

        structure SubscribeToMessagesInput {
          @httpLabel
          room: String
        }

        structure SubscribeToMessagesOutput {
          @httpHeader("X-Connection-Lifetime")
          connectionLifetime: Integer,

          @httpPayload
          messages: Message,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "smithy.example": {
                "shapes": {
                    "PublishMessages": {
                        "type": "operation",
                        "input": "PublishMessagesInput",
                        "inputEventStream": "messages",
                        "http": {
                            "uri": "/messages/{room}",
                            "method": "POST"
                        }
                    },
                    "SubscribeToMessagesInput": {
                        "type": "structure",
                        "members": {
                            "room": {
                                "target": "String",
                                "httpLabel:": true
                            }
                        }
                    },
                    "SubscribeToMessagesOutput": {
                        "type": "structure",
                        "members": {
                            "connectionLifetime": {
                                "target": "Integer",
                                "httpHeader:": "X-Connection-Lifetime"
                            },
                            "messages": {
                                "target": "Message",
                                "httpPayload": true
                            }
                        }
                    }
                }
            }
        }


Initial message client and server behavior
==========================================

Initial messages, if received, MUST be provided to applications
before event stream events.

It is a backward compatible change to add an initial-request or
initial-response to an existing operation; clients MUST NOT fail if an
unexpected initial-request or initial-response is received. Clients and
servers MUST NOT fail if an initial-request or initial-response is not
received for an initial message that contains only optional members.


.. _event-message-serialization:

---------------------------
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

.. tabs::

    .. code-tab:: smithy

        structure ExampleEvent {
          @eventHeader
          a: String,

          @eventHeader
          b: String,

          @eventPayload
          c: Blob,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "smithy.example": {
                "shapes": {
                    "ExampleEvent": {
                        "type": "structure",
                        "members": {
                            "a": {
                                "target": "String",
                                "eventPayload": true
                            },
                            "b": {
                                "target": "String",
                                "eventPayload": true
                            },
                            "c": {
                                "target": "Blob",
                                "eventPayload": true
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
          a: String,
          b: String,
          c: Blob,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "smithy.example": {
                "shapes": {
                    "ExampleEvent": {
                        "type": "structure",
                        "members": {
                            "a": {"target": "String"},
                            "b": {"target": "String"},
                            "c": {"target": "Blob"}
                        }
                    }
                }
            }
        }


-------------------
Event stream traits
-------------------

.. _inputEventstream-trait:

``inputEventStream`` trait
==========================

Summary
    Configures an operation to use an event stream for its input.
Trait selector
    ``operation:test(-[input]->)``

    An operation that defines an input shape.
Value type
    String value that is the name of a member of the operation input
    structure. The referenced member MUST exist on the input structure
    of the operation, MUST NOT be marked as required, and MUST target a
    structure or a union that targets only structure shapes.
Examples
    * :ref:`Single-event event stream example <single-event-input-eventstream>`
    * :ref:`Multi-event event stream example <multi-event-input-eventstream>`


.. _outputEventStream-trait:

``outputEventStream`` trait
===========================

Summary
    Configures an operation to use an event stream for its output.
Trait selector
    ``operation:test(-[output]->)``

    An operation that defines an output shape.
Value type
    String value that is the name of a member of the operation output
    structure. The referenced member MUST exist on the output structure
    of the operation, MUST NOT be marked as required, and MUST target
    a structure or a union that targets only structure shapes.
Examples
    * :ref:`Single-event event stream example <single-event-output-eventstream>`
    * :ref:`Multi-event event stream example <multi-event-output-eventstream>`


.. _eventheader-trait:

``eventHeader`` trait
=====================

Summary
    Binds a member of a structure to be serialized as an event header when
    sent through an event stream.
Trait selector
    .. code-block:: css

        member:of(structure):test( > :each(boolean, byte, short, integer, long, blob, string, timestamp))

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
          a: String,

          @eventHeader
          b: String,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "smithy.example": {
                "shapes": {
                    "ExampleEvent": {
                        "type": "structure",
                        "members": {
                            "a": {
                                "target": "String",
                                "eventHeader": true
                            },
                            "b": {
                                "target": "String",
                                "eventHeader": true
                            }
                        }
                    }
                }
            }
        }


.. _eventpayload-trait:

``eventPayload`` trait
======================

Summary
    Binds a member of a structure to be serialized as the payload of an
    event sent through an event stream.
Trait selector
    .. code-block:: css

        member:of(structure):test(> :each(blob, string, structure, union))

    *Structure member that targets a blob, string, structure, or union*
Value type
    Annotation trait.
Conflicts with
   :ref:`eventheader-trait`
Validation
    1. This trait is *structurally exclusive*, meaning only a single member
       of a structure can be targeted by the trait.
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
          a: String,

          @eventHeader
          b: String,
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "smithy.example": {
                "shapes": {
                    "ExampleEvent": {
                        "type": "structure",
                        "members": {
                            "a": {
                                "target": "String",
                                "eventPayload": true
                            },
                            "b": {
                                "target": "String",
                                "eventHeader": true
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
      a: String,

      b: String,
      // ^ Error: not bound to an eventHeader.
    }
