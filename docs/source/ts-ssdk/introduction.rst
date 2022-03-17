##########################################################
Introduction to the Smithy Server Generator for TypeScript
##########################################################

The Smithy Server Generator generates a lightweight server-side framework for request handling known as a server SDK,
or SSDK. A server SDK enables server applications, also referred to as services, modeled in Smithy by performing the
reverse of a client SDK: it deserializes the inputs and serializes the outputs of Smithy operations. Smithy services are
always written model-first, which encourages developers to focus on their service's contract with its clients, instead
of leaving the contract to be defined implicitly from their implementation choices.

A Smithy model defines a :ref:`service <service>` with one or more :ref:`operations <operation>`. Each operation has an
input shape and an output shape, as well as a set of associated :ref:`error <error-trait>` shapes. This structure is
universally applicable to Smithy services, regardless of protocol. Server SDKs are generated from these models into the
targeted programming language using the same structure, with interfaces called handlers serving as the entrypoint at
both the service and operation level.

Data flow
=========

Smithy services are generally request-reply services, the basic unit of work for which is one request, corresponding to
an invocation of a modeled operation. An incoming request will first be serviced by an
:doc:`endpoint <supported-endpoints>`, which is responsible for reading and writing bytes from the wire, and parsing and
validating the low-level transport protocol, such as HTTP. These endpoints are separate from the server SDK;
examples include Amazon API Gateway, Node.js's HTTP module, or Express.

Next, the request passes through a shim layer, which converts the endpoint's request and response types into the ones
used by the server SDK. These shim layers can be one of the prebuilt libraries published alongside the server SDK,
or purpose built for an endpoint with no corresponding library. Since they are just type conversions, their logic should
be easy to understand, and have no dependencies on any particular Smithy implementation detail or specific
Smithy-modeled service.

In this phase, a service developer also has an opportunity to create a :ref:`context <TS SSDK context>` for the
operation invocation. Contexts generally encapsulate out-of-band, unmodeled data, such as the result of authentication
or pertinent metadata from the endpoint. Contexts are passed as-is to the operation implementation via the server SDK.

After conversion, the service developer invokes the server SDK directly by passing the request to a
:doc:`handler <handlers>`. This is the first time in request processing that the developer yields control of
execution to the SSDK. All of the preceding steps must be written explicitly.

The generated implementation of the handler first performs routing, which determines which operation the supplied
request is intended to invoke. If the request does not correspond to any of the handler's known operations, an
:ref:`UnknownOperationException <TS SSDK unknown-operation-exception>` is generated and returned by the handler. Next,
the handler deserializes the HTTP request and parses it into an object of a type generated from the operation's input.
If the input is unparseable, a :ref:`SerializationException <TS SSDK serialization-exception>` is generated and
returned. Finally, the handler performs :doc:`input validation <validation>` on the deserialized object. If
validation succeeds, the supplied operation implementation is invoked, yielding control back to the service developer.

The developer's operation implementation receives the deserialized input object and the context supplied to the handler.
It must return either an object conforming to the type of the output object, or throw an
:doc:`error <error-handling>`. In either case, the result is serialized into a response appropriate to the
service's protocol and returned from the handler, where the service developer must pass the response through the shim
layer before passing the converted response back to the endpoint.

This execution flow allows the service developer to choose not only their endpoint, but the programming model of their
service. For instance, all of the shim conversion and handler invocation can be refactored into a convenience method,
or the service developer could choose to incorporate their favorite open source middleware library, of which the server
SDK would simply be one layer. It also allows open-ended request preprocessing and response postprocessing to happen
independent of Smithy. For instance, a developer could add support for request or response compression, or a custom
authentication and authorization framework could be plugged into the application before the server SDK is invoked,
without having to fight against a more heavyweight abstraction.
