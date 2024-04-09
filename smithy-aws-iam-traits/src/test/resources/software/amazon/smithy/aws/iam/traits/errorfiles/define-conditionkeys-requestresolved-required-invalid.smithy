$version: "2"

namespace smithy.example

use aws.api#service
use aws.iam#defineConditionKeys

@service(sdkId: "My Value", arnNamespace: "myservice")
@defineConditionKeys(
    "myservice:Bar": {
        type: "String"
        documentation: "The Bar string"
        externalDocumentation: "http://example.com"
        required: true
    })
service MyService {
    version: "2017-02-11"
}

