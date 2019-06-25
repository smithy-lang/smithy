namespace smithy.example

@protocols([{name: "aws-rest-json", auth: ["aws.v4"]}])
@aws.apigateway#authorizer("foo")
@aws.apigateway#authorizers(
    foo: {scheme: "aws.v4", type: "aws", uri: "arn:foo"},
    baz: {scheme: "aws.v4", type: "aws", uri: "arn:foo"})
service ServiceA {
  version: "2019-06-17",
  operations: [OperationA, OperationB],
  resources: [ResourceA, ResourceB],
}

// Inherits the authorizer of ServiceA
operation OperationA()

// Overrides the authorizer of ServiceA
@aws.apigateway#authorizer("baz")
operation OperationB()

// Inherits the authorizer of ServiceA
resource ResourceA {
  operations: [OperationC, OperationD]
}

// Inherits the authorizer of ServiceA
operation OperationC()

// Overrides the authorizer of ServiceA
@aws.apigateway#authorizer("baz")
operation OperationD()

// Overrides the authorizer of ServiceA
@aws.apigateway#authorizer("baz")
resource ResourceB {
  operations: [OperationE, OperationF]
}

// Inherits the authorizer of ResourceB
operation OperationE()

// Overrides the authorizer of ResourceB
@aws.apigateway#authorizer("foo")
operation OperationF()


@protocols([{name: "aws-rest-json", auth: ["aws.v4"]}])
@aws.apigateway#authorizers(
    foo: {scheme: "aws.v4", type: "aws", uri: "arn:foo"},
    baz: {scheme: "aws.v4", type: "aws", uri: "arn:foo"})
service ServiceB {
  version: "2019-06-17",
  operations: [OperationA, OperationB],
  resources: [ResourceA, ResourceB],
}
