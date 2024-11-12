$version: "2"

namespace smithy.example

use aws.api#service
use aws.iam#defineConditionKeys
use aws.iam#serviceResolvedConditionKeys

@service(sdkId: "My Value", arnNamespace: "myservice")
@defineConditionKeys(
    "myservice:Bar": {
        type: "String"
        documentation: "The Bar string"
        externalDocumentation: "http://example.com"
    },
    "myservice:Baz": {
        type: "String"
        documentation: "The Baz string"
        externalDocumentation: "http://baz.com"
        required: true
    }
    "Foo": {
        type: "String"
        documentation: "The Foo string"
        externalDocumentation: "http://foo.com"
        required: false
    }
)
@serviceResolvedConditionKeys(["myservice:Baz"])
service MyService {
    version: "2017-02-11"
}
