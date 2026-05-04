$version: "2.0"

namespace smithy.example

use aws.apigateway#resourcePolicy
use aws.protocols#restJson1

@restJson1
@resourcePolicy({
    "Version": "2012-10-17"
    "Statement": [
        {
            "Effect": "Allow"
            "Principal": "*"
            "Action": "execute-api:Invoke"
            "Resource": ["execute-api:/*"]
        }
        {
            "Effect": "Deny"
            "Principal": "*"
            "Action": "execute-api:Invoke"
            "Resource": ["execute-api:/*"]
            "Condition": {
                "IpAddress": {
                    "aws:SourceIp": "192.0.2.0/24"
                }
            }
        }
    ]
})
service Service {
    version: "2006-03-01"
    operations: [Operation1]
}

@http(uri: "/1", method: "GET")
operation Operation1 {}
