$version: "2"

namespace smithy.example

use aws.api#arn
use aws.api#service
use aws.iam#defineConditionKeys
use aws.iam#iamResource

@service(sdkId: "My")
@defineConditionKeys(
    "foo:baz": { type: "String", documentation: "Foo baz" }
)
service MyService {
    version: "2019-02-20"
    resources: [
        BadIamResourceName
        Beer
        InvalidResource
        ShouldNotThrowAnError
        ColonResource
        CombinedResource
    ]
}

@iamResource(name: "bad-iam-resourceName")
@arn(template: "bad-iam-resource-name/{id}")
resource BadIamResourceName {
    identifiers: { id: String }
}

@iamResource(name: "beer")
@arn(template: "beer/{beerId}")
resource Beer {
    identifiers: { beerId: String }
    resources: [
        IncompatibleResourceName
    ]
}

@arn(template: "beer/{beerId}/incompatible-resource-name")
@iamResource(name: "IncompatibleResourceName")
resource IncompatibleResourceName {
    identifiers: { beerId: String }
}

@iamResource(name: "invalidResource")
@arn(template: "invalid-resource")
resource InvalidResource {}

@iamResource(name: "shouldNotThrowError")
@arn(template: "{arn}", absolute: true)
resource ShouldNotThrowAnError {
    identifiers: { arn: String }
}

@iamResource(name: "colon")
@arn(template: "colon:{fooId}")
resource ColonResource {
    identifiers: { fooId: String }
}

@iamResource(name: "combined")
@arn(template: "combined:{fooId}/{barId}")
resource CombinedResource {
    identifiers: { fooId: String, barId: String }
}
