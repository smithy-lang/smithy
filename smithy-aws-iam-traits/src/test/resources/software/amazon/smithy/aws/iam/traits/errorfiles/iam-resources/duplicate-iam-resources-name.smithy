$version: "2"
namespace smithy.example

use aws.api#arn

@aws.api#service(sdkId: "My")
@aws.iam#defineConditionKeys("foo:baz": {type: "String", documentation: "Foo baz"})
service MyService {
    version: "2019-02-20",
    resources: [
        BadIamResourceName1,
        BadIamResourceName2,
        Beer,
        ShouldNotThrowAnErrorFirst,
        ShouldNotThrowAnErrorSecond
    ]
}

resource Beer {}

@aws.iam#iamResource(name: "Beer")
resource BadIamResourceName1 {}

@aws.iam#iamResource(name: "Beer")
resource BadIamResourceName2 {}

@aws.iam#iamResource(name: "shouldNotThrowErrorSecond")
resource ShouldNotThrowAnErrorFirst {
    identifiers: {
        beerId: String
    }
}

@aws.iam#iamResource(name: "shouldNotThrowErrorFirst")
resource ShouldNotThrowAnErrorSecond {
    identifiers: {
        arn: String
    }
}
