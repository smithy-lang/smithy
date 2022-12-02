$version: "2.0"

namespace smithy.example

use aws.protocols#awsQueryCompatible
use aws.protocols#awsQueryError
use aws.protocols#awsJson1_0

@awsQueryCompatible
@awsJson1_0
service MyService {
    version: "2020-02-05",
    errors: [InvalidThingException]
}

@awsQueryError(
    code: "InvalidThing",
    httpResponseCode: 400,
)
@error("client")
structure InvalidThingException {
    message: String
}
