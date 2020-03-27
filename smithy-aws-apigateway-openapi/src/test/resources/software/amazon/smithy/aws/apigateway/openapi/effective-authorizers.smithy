namespace smithy.example

@protocols([{name: "aws.rest-json-1.1", auth: ["aws.v4"]}])
@aws.apigateway#authorizer("foo")
@aws.apigateway#authorizers(
    foo: {scheme: "aws.v4", type: "aws", uri: "arn:foo"},
    baz: {scheme: "aws.v4", type: "aws", uri: "arn:baz"})
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
