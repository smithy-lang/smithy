.. _wire-protocol-selection:

=======================
Wire protocol selection
=======================

Clients and servers communicate using protocols which, in Smithy, are denoted
on a service by traits that are marked as :ref:`protocol definitions <protocolDefinition-trait>`.
Wire protocol selection is the concept of determining which of the service’s
supported wire protocols should be used to handle input and output routing and
serialization.

Clients are responsible for selecting a compatible protocol either at runtime
or during code generation. Services are responsible for accepting inputs of
their available protocols and producing appropriate outputs. The following
guidance establishes processes that runtimes and code generators can use to
fulfill these responsibilities.

.. _protocol-identification:

Protocol identification
=======================

Each protocol definition MUST define a set of characteristics for servers to
use in identifying that an input was sent by a client using the protocol.

These characteristics SHOULD include a clear, non-payload signal as to the
specific protocol being used. Protocols known to have conflicts SHOULD use
:ref:`the @trait trait’s conflicts property <trait-trait>` to
denote them.

.. _client-protocol-selection:

Client protocol selection
=========================

Client code generators MUST maintain a priority ordered list of protocols they
support during code generation or during client runtime. The selected protocol
will be used for communication between the client and related service. If a
client code generator does not support any protocol that the service models, it
MUST fail at code generation time.

Client code generators SHOULD allow for runtime selection of a protocol; this
should be done if and only if they supply serialization libraries that are
decoupled from the service being code generated for. Clients SHOULD fail if a
protocol is runtime selected that is not supported by the client or the service.

If a client code generator does not support runtime protocol selection, it MUST
select the first entry in its priority ordered list that is also supported by
the service. Client code generators MAY offer the option to specify a protocol
to use during code generation. Clients MUST fail if the protocol is specified
during code generation is not supported by the client or the service.

.. _server-protocol-selection:

Server protocol selection
=========================

Server code generators MUST maintain a list of precision ordered protocols they
support during code generation. If a server code generator does not support any
given protocol that the service models, it SHOULD fail at code generation time.

Servers MUST maintain a precision ordered list of protocols they support at
runtime. To determine which protocol an inbound input utilizes, the service
MUST iterate through the list and use :ref:`protocol identification <protocol-identification>`
to determine the protocol. Services MUST reject the input if no suitable
protocol that the service supports is identified.

.. _evolving-available-protocols:

Evolving available protocols
----------------------------

Services MAY, at any time, introduce support for other wire protocols. New
protocols MUST NOT affect the ability for clients to communicate with servers
using existing protocols. Removing support for a protocol MUST NOT be done
while any active clients are communicating with the service.

.. _aws-service-protocol-precision:

AWS service protocol precision
------------------------------

AWS services will use the following precision ordering for protocols they
support:

#. ``rpcv2Cbor``
#. ``awsJson1_0``
#. ``awsJson1_1``
#. ``awsQuery``
#. ``ec2Query``
#. ``restJson1``
#. ``restXml``

.. _wire-protocol-selection-in-practice:

Wire protocol selection in practice
===================================

The following are examples of how this behavior evaluates in real world
scenarios. This model defines the service used for the rest of the section.

.. code-block:: smithy

    namespace smithy.example

    use aws.protocols#awsJson1_0
    use smithy.protocols#rpcv2Cbor

    @rpcv2Cbor
    @awsJson1_0
    service CoffeeShop {
        version: "2020-07-02"
        operations: [GetMenuItem]
    }

    operation GetMenuItem {
        input := {
            @required
            name: String
        }
        output := {
            name: String
            price: Double
        }
    }

The code generators used for the rest of this section are defined as follows:

- *LangAGenerator*’s protocol priorities: [``rpcv2Cbor``, ``awsJson1_0``,
  ``awsJson1_1``]

  - *LangAGenerator* does not allow client runtime selection of a protocol, but
    supports an option to specify a protocol to use during code generation.
- *LangBGenerator*’s protocol priorities: [``rpcv2Cbor``, ``awsJson1_0``,
  ``awsJson1_1``]
  - *LangBGenerator* allows client runtime selection of a protocol.
- *LangCGenerator*’s protocol priorities: [``awsJson1_1``]
  - *LangCGenerator* does not allow client runtime selection of a protocol.
- *LangDGenerator*’s protocol priorities: [``rpcv2Cbor``]

  - *LangDGenerator* does not allow client runtime selection of a protocol or
    support an option to specify a protocol to use during code generation.

.. _client-protocol-selection-in-practice:

Client protocol selection in practice
-------------------------------------

.. _client-protocol-LangAGenerator:

*LangAGenerator*
~~~~~~~~~~~~~~~~

*LangAGenerator* supports both protocols for CoffeeShop.

- *LangAGenerator* with default configuration will generate a CoffeeShop client
  that uses ``rpcv2Cbor`` at runtime.
- *LangAGenerator* with the protocol option set to ``awsJson1_0`` will generate a
  CoffeeShop client that uses ``awsJson1_0`` at runtime.
- *LangAGenerator* with the protocol option set to ``awsJson1_1`` will fail when
  attempting to generate the CoffeeShop client, as CoffeeShop does not support
  the ``awsJson1_1`` protocol.
- *LangAGenerator* with the protocol option set to ``awsQuery`` will fail when
  attempting to generate the CoffeeShop client, as *LangAGenerator* does not
  support the ``awsQuery`` protocol.

.. _client-protocol-LangBGenerator:

*LangBGenerator*
~~~~~~~~~~~~~~~~

*LangBGenerator* supports both protocols for CoffeeShop.

- A CoffeeShop client generated with *LangBGenerator* will use ``rpcv2Cbor`` at
  runtime by default.
- A CoffeeShop client generated with *LangBGenerator* that has its protocol
  runtime configuration set to ``awsJson1_0`` will configure the client to use
  ``awsJson1_0`` at runtime.
- A CoffeeShop client generated with *LangBGenerator* that has its protocol
  runtime configuration set to ``awsJson1_1`` will fail when attempting to
  instantiate the CoffeeShop client, as CoffeeShop does not support the
  ``awsJson1_1`` protocol.
- A CoffeeShop client generated with *LangBGenerator* that has the protocol
  runtime configuration set to ``awsQuery`` will fail when attempting to
  instantiate the CoffeeShop client, as clients created by *LangBGenerator* do
  not support the ``awsQuery`` protocol.

.. _client-protocol-LangCGenerator:

*LangCGenerator*
~~~~~~~~~~~~~~~~

*LangCGenerator* fails when attempting to generate the CoffeeShop client, as it
does not support any of the CoffeeShop service’s protocols.

.. _client-protocol-LangDGenerator:

*LangDGenerator*
~~~~~~~~~~~~~~~~

*LangDGenerator* supports both protocols for CoffeeShop.

* *LangDGenerator* will generate a CoffeeShop client that uses ``rpcv2Cbor`` at
  runtime.

.. _server-protocol-selection-in-practice:

Server protocol selection in practice
-------------------------------------

.. _server-protocol-LangAGenerator:

*LangAGenerator*
~~~~~~~~~~~~~~~~

*LangAGenerator* supports both protocols for CoffeeShop.

- A CoffeeShop service generated with *LangAGenerator* will support both the
  ``rpcv2Cbor`` and ``awsJson1_0`` protocols.
- A CoffeeShop service generated with *LangAGenerator* that receives an input
  matching ``rpcv2Cbor`` will iterate through its supported protocols in
  priority order.

  - First, the service will attempt to identify the input as ``rpcv2Cbor``
    and succeed.
  - The service will not attempt to identify the input as ``awsJson1_0``.
- A CoffeeShop service generated with *LangAGenerator* that receives an input
  matching ``awsJson1_0`` will iterate through its supported protocols in
  priority order.

  - First, the service will attempt to identify the input as ``rpcv2Cbor``
    and will fail to do so.

  - Then, the service will attempt to identify the input as ``awsJson1_0``
    and succeed.
- A CoffeeShop service generated with *LangAGenerator* that receives an input
  matching ``awsJson1_1`` will iterate through its supported protocols in =
  priority order.

  - First, the service will attempt to identify the input as ``rpcv2Cbor``
    and will fail to do so.

  - Then, the service will attempt to identify the input as ``awsJson1_0``
    and will fail to do so.

  - The service will not attempt to match any other protocol and will reject
    the input.
- A CoffeeShop service generated with *LangAGenerator* that receives an input not
  matching any known protocol will iterate through its supported protocols in
  priority order.

  - First, the service will attempt to identify the input as ``rpcv2Cbor``
    and will fail to do so.

  - Then, the service will attempt to identify the input as ``awsJson1_0``
    and will fail to do so.

  - The service will not attempt to match any other protocol and will reject
    the input.

.. _server-protocol-LangBGenerator:

*LangBGenerator*
~~~~~~~~~~~~~~~~

For service protocol selection, *LangBGenerator* is the same as :ref:`LangAGenerator <server-protocol-LangAGenerator>`.

.. _server-protocol-LangCGenerator:

*LangCGenerator*
~~~~~~~~~~~~~~~~

*LangCGenerator* fails when attempting to generate the CoffeeShop service, as it
does not support any of the CoffeeShop service’s protocols.

.. _server-protocol-LangDGenerator:

*LangDGenerator*
~~~~~~~~~~~~~~~~

*LangDGenerator* fails when attempting to generate the CoffeeShop service, as it
does not support the ``awsJson1_0`` protocol that CoffeeShop supports.
