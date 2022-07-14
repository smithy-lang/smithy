#####################################################
Smithy Server Generator for TypeScript error handling
#####################################################

Errors are structures associated with services and operations in the Smithy model and are used to indicate that the
server encountered a problem during request processing. Structures with the error trait are code generated as subclasses
of ``Error``.

If a code-generated ``Error`` is thrown from a handler implementation, and the ``Error`` is associated with the
operation or service in the model, then the server SDK will serialize the Error according to the rules of the protocol.
For instance, HTTP binding protocols set the status code of the response based on the ``@httpError`` trait.

If a non-code-generated Error, or a code-generated Error that is not associated with the operation or service in the
model, is thrown from a handler, then a *synthetic* ``InternalFailure`` will be rendered as the result instead.

Synthetic errors
================

*Synthetic errors* are errors that are not included in the Smithy model, but can still be thrown by the server SDK. In
general, these errors are not expected to have corresponding code generated types on the client side. These errors fall
into two categories: framework-level errors that are unavoidable, and errors that are associated with the low-level
transport protocol, such as HTTP.

For backwards compatibility purposes, the names of synthetic errors generally end with ``Exception``.

Unavoidable errors
------------------

.. _TS SSDK internal-failure-exception:

InternalFailureException
~~~~~~~~~~~~~~~~~~~~~~~~

InternalFailureException is the catch-all exception for unexpected errors. It indicates either a bug in the framework or
an exception being thrown from the handler that is not modeled. The server SDK will never include any details about
internal failures, such as a meaningful exception message, to the caller, in order to prevent unintended information
disclosure.

Service developers wishing to throw an InternalFailureException to indicate a bug in their code can simply throw a
built-in JavaScript error such as ``TypeError``.

.. _TS SSDK serialization-exception:

SerializationException
~~~~~~~~~~~~~~~~~~~~~~

SerializationException is thrown by the server SDK when a request is unparseable, or if there is a type mismatch between
a member in the serialized request and the member in the Smithy model. Since these failures occur during the
deserialization process, server developers have no ability to customize these messages, and they will short-circuit
request processing before validation occurs.

.. _TS SSDK unknown-operation-exception:

UnknownOperationException
~~~~~~~~~~~~~~~~~~~~~~~~~

UnknownOperationException is returned when the request cannot be matched to a handler known to the server SDK. This can
happen either because the server SDK does not recognize the request protocol, which is common for internet-facing
endpoints that receive robotic traffic, or if the request is in a known protocol but for an operation that is unknown to
the handler. The latter case can indicate a misconfiguration, such as an operation-level handler being used incorrectly.

Protocol errors
---------------

UnsupportedMediaTypeException
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

UnsupportedMediaTypeException represents HTTP error 415, returned when a request has a ``Content-Type`` that is not
accepted for the protocol or does not match the ``@mediaType`` trait in the model.

NotAcceptableException
~~~~~~~~~~~~~~~~~~~~~~

NotAcceptableException represents HTTP error 406, returned when a request's ``Accept`` header does not match the type used
to serialize protocol responses, or when it does not match the response payload's ``@mediaType`` value.
