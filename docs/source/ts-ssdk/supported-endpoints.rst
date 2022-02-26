##########################################################
Smithy Server Generator for TypeScript supported endpoints
##########################################################

An endpoint is a non-generated component, typically off-the-shelf, that is responsible for reading from and writing to
the network, as well as handling the low-level protocol, such as HTTP. While the server SDK should be able to support
any endpoint, some endpoints are supported directly via integration libraries sometimes referred to as shims.

Amazon API Gateway REST APIs and AWS Lambda
===========================================

Support for API Gateway REST APIs fronting AWS Lambda is available via the NPM package
`@aws-smithy/server-apigateway`_.

Incoming events of the type ``APIGatewayProxyEvent`` from the ``aws-lambda`` package can be converted into the SDK's
``HttpRequest`` type by calling ``convertEvent``. Request headers with the same key are combined into a single request
header by joining values with ``,``. Request bodies are decoded from base64 automatically if
``APIGatewayProxyEvent.isBase64Encoded`` is ``true``.

An ``HttpResponse`` returned from a server SDK handler can be converted into an ``APIGatewayProxyResult`` via the
function ``convertVersion1Response``. Structured response bodies are never base64 encoded.

.. warning:: Due to incompatibilities between the way that Smithy HTTP bindings and API Gateway HTTP APIs match requests
    to operations, use of HTTP APIs with the Smithy Server Generator for TypeScript is not recommended.

.. _@aws-smithy/server-apigateway: https://www.npmjs.com/package/@aws-smithy/server-apigateway
