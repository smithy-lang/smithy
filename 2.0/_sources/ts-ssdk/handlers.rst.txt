###############################################
Smithy Server Generator for TypeScript handlers
###############################################

The primary abstraction of a Smithy server is ``ServiceHandler``, the interface to the server's
generated implementation that a service's code will build and call directly.

.. code-block:: typescript

    export interface ServiceHandler<Context = {}, RequestType = HttpRequest, ResponseType = HttpResponse> {
        handle(request: RequestType, context: Context): Promise<ResponseType>;
    }

A ``ServiceHandler`` is simply a function that takes in a request (by default, an ``HttpRequest``) and an optional
``Context`` of arbitrary type, and returns a response (by default, an ``HttpResponse``).

The Smithy Server Generator for TypeScript generates two different sets of handlers: one handler for the service as a
whole, and another handler for each individual operation in the service. This allows the service to choose the handler
that works best for their situation.

The primary difference between service-level and operation-level handlers is where routing (the matching of requests
to Smithy operations) is performed. Operation-level handlers assume that routing is handled outside of the server SDK,
which is commonly the case when a service like Amazon API Gateway is used. Service-level handlers are used when all
requests from an endpoint are handled by a handler, which would be the case when using Node.js's built-in HTTP server.

Consider a service with the following Smithy declaration:

.. code-block:: smithy

    @restJson1
    service StringWizard {
        version: "2018-05-10",
        operations: [Echo, Length]
    }

The SSDK will generate three different implementations of ``ServiceHandler``. One for ``StringWizard``:

.. code-block:: typescript

    export class StringWizardServiceHandler<Context> implements ServiceHandler<Context> { /* ... */ }

As well as one for ``Echo`` and one for ``Length``:

.. code-block:: typescript

    export class EchoHandler<Context> implements ServiceHandler<Context> { /* ... */ }

    export class LengthHandler<Context> implements ServiceHandler<Context> { /* ... */ }

ServiceHandler factories
========================

Each handler class is protocol-agnostic; the implementation details of protocol serialization, deserialization, and
routing are passed into the handler class's constructor. For convenience, a factory method is generated for the
service's protocol that supply the necessary parameters to the handlers' constructors.

For instance, for the ``Length`` operation of ``StringWizard``, a corresponding handler factory function is generated:

.. code-block:: typescript

    export const getLengthHandler = <Context>(operation: Operation<LengthServerInput, LengthServerOutput, Context>):
        ServiceHandler<Context, HttpRequest, HttpResponse> => { /* ... */ }

The only parameter of ``getLengthParameter`` is an implementation of
``Operation<LengthServerInput, LengthServerOutput, Context>``. ``Operation`` is a type distributed in
``@aws-smithy/server-common`` that server developers implement in order to perform the business logic of an operation
described in their Smithy model.

.. code-block:: typescript

    export type Operation<I, O, Context = {}> = (input: I, context: Context) => Promise<O>;

The Lambda implementation of the ``Length`` operation can then be as simple as:

.. code-block:: typescript

    // getLengthHandler is the code generated handler factory
    const handler = getLengthHandler(async (input) => {
        return {
            length: input.string?.length,
            $metadata: {}
        };
    });

    export const lambdaHandler: APIGatewayProxyHandler = async (event): Promise<APIGatewayProxyResult> => {
        // This uses the shim from @aws-smithy/server-apigateway to convert APIGateway events to HttpRequests
        const httpRequest = convertEvent(event);

        const httpResponse = await handler.handle(httpRequest, {});

        // This uses the shim from @aws-smithy/server-apigateway to convert HttpResponses to APIGateway events
        return convertVersion1Response(httpResponse);
    };

Since ``getLengthHandler`` is code generated against the input and output types of the ``Length`` operation, this code
has the additional benefit of being type-safe, even though the incoming event is simply a raw HTTP request.
Additionally, although ``getLengthHandler`` can only service requests for the ``Length`` operation, it still asserts
that the incoming request matches the modeled expectations for ``Length``. This means if the developer accidentally
deploys the code for ``Length`` to the Lambda function for ``Echo``, the handler will reject the request instead of
passing it onto the business logic and executing the wrong code.

The handler factory function for services, is similar, but instead of requiring an implementation of ``Operation``,
it requires an implementation of every ``Operation`` in the service. For instance, for ``StringWizardService``,
the handler factory function looks like this:

.. code-block:: typescript

    export const getStringWizardServiceHandler = <Context>(service: StringWizardService<Context>):
        __ServiceHandler<Context, __HttpRequest, __HttpResponse> => { /* ... */ }

``StringWizardService`` is a generated interface with the following definition:

.. code-block:: typescript

    export interface StringWizardService<Context> {
      Echo: Operation<EchoServerInput, EchoServerOutput, Context>
      Length: Operation<LengthServerInput, LengthServerOutput, Context>
    }

This conveys the same type-safety benefits as the operation-level handler factory, as well as ensuring that any
service handler has an implementation for every operation in the service. This means type checks will fail if your
model adds an operation, but the service's source code is not properly updated to add an implementation for it.

.. _TS SSDK context:

Contexts
========

All handlers take an arbitrary ``Context`` of a type specified at runtime via the handler's ``Context`` generic type
argument. This allows the service developer to pass unmodeled data from the request or runtime environment to their
business logic.

For instance, a server running in AWS Lambda behind Amazon API Gateway could define a context that includes the calling
user's ARN, in order to do authorization checks in their business logic:

.. code-block:: typescript

    interface HandlerContext {
      user: string;
    }

and then modify their entry-point implementation to extract the user's identity from the incoming request and pass it to
the handler:

.. code-block:: typescript

    export const lambdaHandler: APIGatewayProxyHandler = async (event): Promise<APIGatewayProxyResult> => {
        const httpRequest = convertEvent(event);

        const userArn = event.requestContext.identity.userArn;
        if (!userArn) {
          throw new Error("IAM Auth is not enabled");
        }
        const context = { user: userArn };

        const httpResponse = await handler.handle(httpRequest, context);

        return convertVersion1Response(httpResponse);
    };

The value of ``Context`` is not constrained or modified by the server SDK in any way; it is passed through unmodified to
the ``Operation`` implementation.
