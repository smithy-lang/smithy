$version: "2.0"

namespace smithy.example

use aws.api#arn
use aws.api#service
use aws.iam#iamResource

@service(sdkId: "My")
service MyService {
    version: "2020-07-02"
    resources: [SuperResource]
}

@iamResource(
    name: "super"
    relativeDocumentation: "API-Super.html"
    disableConditionKeyInheritance: false
)
@arn(template: "super/{id1}")
resource SuperResource {
    identifiers: {
        id1: String
    }
    read: GetResource
}

@readonly
operation GetResource {
    input: GetResourceInput
    output: GetResourceOutput
}

structure GetResourceInput {
    @required
    id1: String
}

structure GetResourceOutput {
    super: Super
}

structure Super {
    id1: String
}
