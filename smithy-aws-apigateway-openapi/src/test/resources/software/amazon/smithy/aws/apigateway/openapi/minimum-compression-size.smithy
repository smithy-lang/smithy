$version: "2.0"

namespace smithy.example

use aws.apigateway#minimumCompressionSize
use aws.protocols#restJson1

@restJson1
@minimumCompressionSize(10240)
service Service {
    version: "2006-03-01"
    operations: [Operation1]
}

@http(uri: "/1", method: "GET")
operation Operation1 {}
