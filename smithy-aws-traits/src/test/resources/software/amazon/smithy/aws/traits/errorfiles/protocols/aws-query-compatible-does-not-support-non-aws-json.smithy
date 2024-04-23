$version: "2.0"

namespace smithy.example

use aws.protocols#awsQuery
use aws.protocols#awsQueryCompatible
use aws.protocols#awsQueryError

@awsQueryCompatible
@awsQuery
@xmlNamespace(uri: "https://example.com")
service MyService {
    version: "2020-02-05"
    errors: [
        InvalidThingException
    ]
}

@awsQueryError(code: "InvalidThing", httpResponseCode: 400)
@error("client")
structure InvalidThingException {
    message: String
}
