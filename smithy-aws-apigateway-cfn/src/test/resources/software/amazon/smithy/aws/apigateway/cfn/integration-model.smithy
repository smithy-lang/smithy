$version: "2.0"

namespace com.example

use aws.apigateway#integration
use aws.protocols#restJson1

@restJson1
service MyService {
    version: "2024-01-01"
    operations: [GetItems]
}

@http(method: "GET", uri: "/items")
@integration(
    type: "aws_proxy"
    uri: "arn:${AWS::Partition}:apigateway:${AWS::Region}:lambda:path/2015-03-31/functions/${MyLambda.Arn}/invocations"
    httpMethod: "POST"
    credentials: "${ApiGatewayRole.Arn}"
    connectionType: "VPC_LINK"
    connectionId: "${MyVpcLink}"
    integrationTarget: "${MyALBListener.Arn}"
)
operation GetItems {
    output := {}
}
