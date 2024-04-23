namespace smithy.example

use aws.auth#sigv4

@sigv4(name: "service")
@aws.apigateway#authorizer("foo")
@aws.apigateway#authorizers(
    foo: { scheme: sigv4, type: "aws", uri: "arn:foo" }
    baz: { scheme: sigv4, type: "aws", uri: "arn:foo" }
)
service ServiceA {
    version: "2019-06-17"
    operations: [
        OperationA
        OperationB
    ]
    resources: [
        ResourceA
        ResourceB
    ]
}

// Inherits the authorizer of ServiceA
operation OperationA {}

// Overrides the authorizer of ServiceA
@aws.apigateway#authorizer("baz")
operation OperationB {}

// Inherits the authorizer of ServiceA
resource ResourceA {
    operations: [
        OperationC
        OperationD
    ]
}

// Inherits the authorizer of ServiceA
operation OperationC {}

// Overrides the authorizer of ServiceA
@aws.apigateway#authorizer("baz")
operation OperationD {}

// Overrides the authorizer of ServiceA
@aws.apigateway#authorizer("baz")
resource ResourceB {
    operations: [
        OperationE
        OperationF
    ]
}

// Inherits the authorizer of ResourceB
operation OperationE {}

// Overrides the authorizer of ResourceB
@aws.apigateway#authorizer("foo")
operation OperationF {}

@sigv4(name: "service")
@aws.apigateway#authorizers(
    foo: { scheme: sigv4, type: "aws", uri: "arn:foo" }
    baz: { scheme: sigv4, type: "aws", uri: "arn:foo" }
)
service ServiceB {
    version: "2019-06-17"
    operations: [
        OperationA
        OperationB
    ]
    resources: [
        ResourceA
        ResourceB
    ]
}
