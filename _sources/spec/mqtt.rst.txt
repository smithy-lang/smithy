.. _mqtt-bindings:

======================
MQTT Protocol Bindings
======================

This document defines traits that bind Smithy operations, inputs, and
outputs to the `MQTT <https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html>`_
messaging transport protocol. In addition to traits, it defines the
requirements, limitations, and expected client behavior when decorating
Smithy models with MQTT metadata.

.. warning::

    The MQTT traits defined in this specification are still evolving and
    subject to change.

.. contents:: Table of contents
    :depth: 2
    :local:
    :backlinks: none


------------
Introduction
------------

Smithy is a protocol-agnostic model format that abstracts the transmission
and serialization of operations, their inputs, and outputs. This abstraction
allows MQTT services to be modeled in Smithy using MQTT protocol binding
traits. A Smithy service that supports MQTT protocol bindings can also
support other protocol bindings like :ref:`HTTP bindings <http-traits>`.
The MQTT protocol binding traits defined in this document do not define the
serialization format of complex payloads; the serialization format of complex
payloads is protocol-specific.


MQTT overview
=============

MQTT is a stateful protocol in which zero or more clients establish
long-lived sessions with a server that acts as a message broker between
them. Clients subscribe to topics and publish and receive messages. All
messages have a hierarchical topic, and clients subscribed to a particular
topic or to a matching topic filter receive messages published to that
topic. Messages are published with a requested quality of service (QoS) level
which determines to what degree the broker must ensure the message has
been delivered before discarding it. Smithy does not define the QoS levels
that are supported by a service or operation.

There is no support at the MQTT protocol level for responses or errors. A
message may be published in reply to another or to alert a peer to error
conditions, but this must be done according to semantic conventions
established by the clients and server.

Smithy models explicitly define the PUBLISH and SUBSCRIBE MQTT control
packets using the :ref:`mqttPublish-trait` and :ref:`mqttSubscribe-trait`.
Clients publish and subscribe to MQTT topics using separate operations.
The CONNECT, DISCONNECT, and other MQTT control packets SHOULD be handled
as implementation details in Smithy clients that connect to a service that
utilizes MQTT protocol bindings.


.. _mqtt-topic-templates:

--------------------
MQTT topic templates
--------------------

An *MQTT topic template* declares an MQTT topic to be used for a given
operation and to bind components of the topic to fields in the operations's
input structure. The :ref:`mqttPublish-trait` and :ref:`mqttSubscribe-trait`
are defined using MQTT topic templates.

.. _mqtt-topic-label:

*Labels* are used in the topic template to bind operation input structure
members to placeholders in the topic. Labels are defined in the topic template
using an opening brace ("{"), followed by a member name, followed by a closing
brace ("}"). Each member name referenced in a label MUST case-sensitively
correspond to a single member by name in the input structure of an operation
that is targeted by both the :ref:`required-trait` and the
:ref:`mqttTopicLabel-trait`. The values of the corresponding members
are substituted into the topic template at runtime to resolve the actual
MQTT topic.

The following example defines a publish operation with two labels, ``{first}``
and ``{second}``, in the MQTT topic template:

.. tabs::

    .. code-tab:: smithy

        @mqttPublish("{first}/{second}")
        operation ExampleOperation(ExampleOperationInput)

        structure ExampleOperationInput {
          @required
          @mqttTopicLabel
          first: String,

          @required
          @mqttTopicLabel
          second: String,

          message: String,
        }

    .. code-tab:: json

        {
            "smithy": "0.1.0",
            "smithy.example": {
                "shapes": {
                    "ExampleOperation": {
                        "type": "operation",
                        "input": "ExampleOperationInput",
                        "mqttPublish": "{first}/{second}"
                    },
                    "ExampleOperationInput": {
                        "type": "structure",
                        "members": {
                            "first": {
                                "target": "String",
                                "required": true,
                                "mqttTopicLabel": true
                            },
                            "second": {
                                "target": "String",
                                "required": true,
                                "mqttTopicLabel": true
                            },
                            "message": {
                                "target": "String"
                            }
                        }
                    }
                }
            }
        }

MQTT topic templates MUST adhere to the following constraints:

* The topic template MUST adhere to the constraints defined in
  `section 4.7 <https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718106>`_
  of the MQTT specification (e.g., it MUST consist of one or more
  UTF-8 characters).
* The topic template MUST not contain wildcard topic characters "+" and "#".
* Labels present in a topic template MUST span an entire topic level.
  For example, "foo/baz/{bar}" is **valid** while "foo/baz-{bar}" is
  **invalid**.
* The "{" and "}" characters are reserved for use as topic labels and
  MUST NOT be used as literal characters.
* The text inside of each label MUST case-sensitively match a single member by
  name of the input structure of an operation.
* Operation input structures MUST NOT contain extraneous members marked with
  the ``mqttTopicLabel`` trait that do not have corresponding labels
  in the topic template.


.. _mqttPublish-trait:

---------------------
``mqttPublish`` trait
---------------------

Trait summary
    Binds an operation to send a PUBLISH control packet via the MQTT protocol.
Trait selector
    ``operation:not(-[output]->)``

    *An operation that does not define output*
Trait value
    ``string`` value that is a valid
    :ref:`MQTT topic template <mqtt-topic-templates>`. The provided topic
    defines the MQTT topic to which messages are published. The MQTT topic
    template MAY contain :ref:`label placeholders <mqtt-topic-label>` that
    reference top-level input members of the operation by case-sensitive
    member name.
Conflicts with
    :ref:`mqttSubscribe-trait`, :ref:`inputEventStream-trait`

Input members that are not marked with the :ref:`mqttTopicLabel-trait` come
together to form the protocol-specific payload of the PUBLISH message.

The following example defines an operation that publishes messages to the
``foo/{bar}`` topic:

.. tabs::

    .. code-tab:: smithy

        namespace smithy.example

        @mqttPublish("foo/{bar}")
        operation PostFoo(PostFooInput)

        structure PostFooInput {
          @required
          @mqttTopicLabel
          bar: String,

          someValue: String,
          anotherValue: Boolean,
        }

    .. code-tab:: json

        {
            "smithy": "0.1.0",
            "smithy.example": {
                "shapes": {
                    "PostFoo": {
                        "type": "operation",
                        "input": "PostFooInput",
                        "mqttPublish": "foo/{bar}"
                    },
                    "PostFooInput": {
                        "type": "structure",
                        "members": {
                            "bar": {
                                "target": "String",
                                "required": true,
                                "mqttTopicLabel": true
                            },
                            "message": {
                                "target": "String"
                            },
                            "anotherValue": {
                                "target": "Boolean"
                            }
                        }
                    }
                }
            }
        }

The "bar" member of the above ``PostFoo`` operation is marked with the
:ref:`mqttTopicLabel-trait`, indicating that the member provides a
value for the "{bar}" label of the MQTT topic template. The "message" and
"anotherValue" members come together to form a protocol-specific document
that is sent in the payload of the message.


Publish validation
==================

* Publish operations MUST NOT define output.
* Publish operations MUST NOT utilize input event streams.
* Publish operations SHOULD NOT define errors.
* Publish MQTT topics MUST NOT conflict with other publish MQTT topics or
  the resolved MQTT topics of subscribe operations.


.. _mqttSubscribe-trait:

-----------------------
``mqttSubscribe`` trait
-----------------------

Trait summary
    Binds an operation to send one or more SUBSCRIBE control packets
    via the MQTT protocol.
Trait selector
    ``operation[trait|outputEventStream]``

    *An operation with an outputEventStream trait*
Trait value
    ``string`` value that is a valid :ref:`MQTT topic template <mqtt-topic-templates>`.
    The MQTT topic template MAY contain label placeholders that reference
    top-level input members of the operation by case-sensitive member name.
Conflicts with
    :ref:`mqttPublish-trait`

No message is published when using an operation marked with the
``mqttSubscribe`` trait. All members of the input of the operation
MUST be marked with valid ``mqttTopicLabel`` traits.

The operation MUST have an :ref:`outputEventStream-trait`. The top-level
output member referenced by this trait represents the message that is sent
over the MQTT topic. An abstraction for automatically subscribing to and
asynchronously receiving events SHOULD be provided by Smithy clients. When
that abstraction is destroyed, the client SHOULD provide the ability to
automatically UNSUBSCRIBE from topics.

.. important::

    Events MAY contain a member marked with
    :ref:`eventPayload-trait`, which allows for a custom
    payload to be sent as the payload of a message.

The following example operation subscribes to the ``events/{id}``
topic using a :ref:`single-event event stream <single-event-event-stream>`:

.. tabs::

    .. code-tab:: smithy

        @mqttSubscribe("events/{id}")
        @outputEventStream(events)
        operation SubscribeForEvents(SubscribeForEventsInput) -> SubscribeForEventsOutput

        structure SubscribeForEventsInput {
          @required
          @mqttTopicLabel
          id: String,
        }

        structure SubscribeForEventsOutput {
          events: Event,
        }

        structure Event {
          message: String,
        }

    .. code-tab:: json

        {
            "smithy": "0.1.0",
            "smithy.example": {
                "shapes": {
                    "SubscribeForEvents": {
                        "type": "operation",
                        "input": "SubscribeForEventsInput",
                        "mqttSubscribe": "events/{id}",
                        "outputEventStream": "events"
                    },
                    "SubscribeForEventsInput": {
                        "type": "structure",
                        "members": {
                            "id": {
                                "target": "String",
                                "required": true,
                                "mqttTopicLabel": true
                            }
                        }
                    },
                    "SubscribeForEventsOutput": {
                        "type": "structure",
                        "members": {
                            "events": {
                                "target": "Event"
                            }
                        }
                    },
                    "Event": {
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


Subscribe validation
====================

* Subscribe operations MUST NOT define event streams with an
  :ref:`initial-response <initial-response>`; only a single member can appear
  in the output of a subscribe operation.
* Every member of the input of a subscribe operation MUST be marked with the
  :ref:`mqttTopicLabel-trait`.
* Subscribe operations SHOULD NOT define errors.
* Subscribe MQTT topics MUST NOT conflict with other topics.
* Event stream events over MQTT SHOULD NOT contain the
  :ref:`eventHeader-trait`. Support for this trait MAY be
  added to this specification once MQTT adds support for variable length
  custom headers to messages.


.. _mqttTopicLabel-trait:

------------------------
``mqttTopicLabel`` trait
------------------------

Trait summary
    Binds a structure member to an :ref:`MQTT topic label <mqtt-topic-label>`.
Trait selector
    ``member[trait|required]:test( > :test(string, byte, short, integer, long, boolean, timestamp))``

    *Required structure member that targets a string, byte, short, integer, long, boolean, or timestamp*
Trait value
    Annotation trait

The ``mqttTopicLabel`` trait binds the value of a structure member
so that it provides a value at runtime for a corresponding MQTT topic template
label specified in a :ref:`mqttPublish-trait` and :ref:`mqttSubscribe-trait`.
All labels defined in an MQTT topic template MUST have corresponding input
structure members with the same case-sensitive member name that is marked
with the ``mqttTopicLabel`` trait, marked with the ``required`` trait, and
targets a string, byte, short, integer, long, boolean, or timestamp shape.


Label serialization
===================

The value of the member is substituted into an MQTT topic template using the
following serialization:

* Strings are serialized as is, but "/" is replaced with %2F.
* Numeric values are serialized using an exact string representation of
  the number.
* Boolean values are serialized as the strings ``true`` or ``false``.
* Timestamp values are serialized as ``date-time`` strings as specified
  in :rfc:`3339`.


---------------
Topic conflicts
---------------

MQTT topics in Smithy are fully-typed; MQTT topics modeled in Smithy are
associated with exactly one shape that defines the payload that can be
published to a topic. Multiple operations and events in a model MAY resolve
to the same MQTT topic if and only if each conflicting topic targets the
same shape in the Smithy model.

Two resolved topics are considered conflicting if all of the following
conditions are met:

* Both topics contain the same case-sensitive static levels and labels
  in the same topic level positions (regardless of the label name).
* One topic is not more specific than the other; both topics have the
  same number of levels.
* The topic payloads target different shapes.

The following table provides examples of when topics do and do not conflict:

.. list-table::
    :header-rows: 1
    :widths: 40 40 20

    * - Topic A
      - Topic B
      - Conflict?
    * - ``a/{x}``
      - ``a/{y}``
      - Yes
    * - ``{x}/{y}``
      - ``{y}/{x}``
      - Yes
    * - ``a/{b}/c/{d}``
      - ``a/{d}/c/{b}``
      - Yes
    * - ``a/b/c``
      - ``A/B/C``
      - No
    * - ``{x}/{y}``
      - ``{x}/{y}/{z}``
      - No
    * - ``a/{x}``
      - ``b/{x}``
      - No
    * - ``a/b/c``
      - ``a/b/notC``
      - No
    * - ``a/b/c``
      - ``a/b/c/d``
      - No


----------------
Model definition
----------------

The following Smithy model defines the traits and shapes used to define
MQTT protocol bindings.

.. code-block:: smithy

    $version: "0.1.0"
    namespace smithy.api

    trait mqttPublish {
      shape: MqttTopicString,
      selector: "operation:not(-[output]->)",
      conflicts: [mqttSubscribe, inputEventStream]
    }

    trait mqttSubscribe {
      shape: MqttTopicString,
      selector: "operation[trait|outputEventStream]",
      conflicts: [mqttPublish]
    }

    // Matches one or more characters that are not "#" or "+".
    @pattern("^[^#+]+$")
    @private
    string MqttTopicString

    trait mqttTopicLabel {
      selector: "member[trait|required]:test(> :test(string, byte, short, integer, long, boolean, timestamp))",
    }


.. _MQTT PUBLISH: http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718037
.. _MQTT topic level: https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718106
