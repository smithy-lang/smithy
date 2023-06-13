$version: "2"
namespace smithy.example

use aws.api#arn

@aws.api#service(sdkId: "My")
@aws.iam#defineConditionKeys("foo:baz": {type: "String", documentation: "Foo baz"})
service MyService {
    version: "2019-02-20",
    resources: [
        BadIamResourceName,
        Beer,
        InvalidResource
    ]
}

@aws.iam#iamResource(name: "bad-iam-resourceName")
@arn(template: "bad-iam-resource-name/{id}")
resource BadIamResourceName {
    identifiers: {
        id: String
    }
}

@aws.iam#iamResource(name: "beer")
@arn(template: "beer/{beerId}")
resource Beer {
    identifiers: {
        beerId: String
    }
    resources: [IncompatibleResourceName]
}

@arn(template: "beer/{beerId}/incompatible-resource-name")
@aws.iam#iamResource(name: "IncompatibleResourceName")
resource IncompatibleResourceName {
    identifiers: {
        beerId: String
    }
}

@aws.iam#iamResource(name: "invalidResource")
@arn(template: "invalid-resource")
resource InvalidResource {}
