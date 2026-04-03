$version: "2.0"

namespace example.smithy

use aws.apigateway#integration
use aws.auth#sigv4
use aws.protocols#restJson1

@restJson1
@sigv4(name: "myservice")
@cors
@integration(
    type: "aws_proxy"
    uri: "arn:aws:apigateway:us-west-2:lambda:path/2015-03-31/functions/arn:aws:lambda:us-west-2:123456789012:function:{operationName}/invocations"
    httpMethod: "POST"
    payloadFormatVersion: "1.0"
)
service MyService {
    version: "2024-01-01"
    operations: [GetFoo]
}

@readonly
@http(uri: "/foo", method: "GET")
operation GetFoo {
    output := {}
}
