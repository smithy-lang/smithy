$version: "1.0"
namespace smithy.example

@aws.api#service(sdkId: "My")
@aws.iam#defineConditionKeys("foo:baz": {type: "String", documentation: "Foo baz"})
service MyService {
  version: "2019-02-20",
  operations: [Operation]
}

@aws.iam#conditionKeys(["foo:qux"])
operation Operation {}
