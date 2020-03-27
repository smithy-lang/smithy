namespace smithy.example

@aws.protocols#restJson1
@aws.auth#sigv4(name: "someservice")
@aws.apigateway#authorizer("foo")
@aws.apigateway#authorizers(
    foo: {scheme: "aws.auth#sigv4", type: "aws", uri: "arn:foo"},
    baz: {scheme: "aws.auth#sigv4", type: "aws", uri: "arn:baz"})
service ServiceA {
  version: "2019-06-17",
  operations: [OperationA, OperationB]
}

// Inherits the authorizer of ServiceA
@http(method: "GET", uri: "/operationA")
operation OperationA {}

// Overrides the authorizer of ServiceA
@aws.apigateway#authorizer("baz")
@http(method: "GET", uri: "/operationB")
operation OperationB {}
