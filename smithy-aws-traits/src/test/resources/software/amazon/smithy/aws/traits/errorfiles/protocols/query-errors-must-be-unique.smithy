$version: "2.0"

namespace smithy.example

use aws.protocols#awsJson1_0
use aws.protocols#awsQuery
use aws.protocols#awsQueryCompatible
use aws.protocols#awsQueryError

@awsQueryCompatible
@awsJson1_0
service QueryCompatibleService {
    version: "2020-02-05"
    errors: [
        DocumentNotFound
        ResourceNotFound
    ]
}

@awsQueryError(code: "ResourceNotFound", httpResponseCode: 400)
@error("client")
structure DocumentNotFound {
    message: String
}

@error("client")
structure ResourceNotFound {
    message: String
}

@awsQuery
@xmlNamespace(uri: "https://example.com/")
service QueryService {
    version: "2020-02-05"
    errors: [
        DocumentNotFound
        ResourceNotFound
    ]
}
