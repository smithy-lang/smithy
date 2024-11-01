$version: "2.0"

namespace ns.foo

use aws.api#arn
use aws.api#arnReference
use aws.api#service

@service(sdkId: "Something Empty")
service EmptyAwsService { version: "2018-03-17" }

service NonAwsService { version: "2018-03-17" }

@service(
    sdkId: "Some Value"
    arnNamespace: "service"
    cloudFormationName: "SomeService"
    endpointPrefix: "some-service"
)
service SomeService {
    version: "2018-03-17"
    resources: [
        AbsoluteResource
        RootArnResource
        SomeResource
    ]
}

@arn(template: "{arn}", absolute: true)
resource AbsoluteResource {
    identifiers: { arn: AbsoluteResourceArn }
}

resource AnotherChild {
    identifiers: { childId: ChildResourceId, someId: SomeResourceId }
}

@arn(template: "someresource/{someId}/{childId}")
resource ChildResource {
    identifiers: { childId: ChildResourceId, someId: SomeResourceId }
    resources: [
        AnotherChild
    ]
}

@arn(noRegion: true, noAccount: true, template: "rootArnResource")
resource RootArnResource {}

@arn(template: "someresource/{someId}")
resource SomeResource {
    identifiers: { someId: SomeResourceId }
    resources: [
        ChildResource
    ]
}

@arnReference(
    type: "AWS::SomeService::AbsoluteResource"
    service: "ns.foo#SomeService"
    resource: "ns.foo#AbsoluteResource"
)
string AbsoluteResourceArn

string ChildResourceId

string SomeResourceId
