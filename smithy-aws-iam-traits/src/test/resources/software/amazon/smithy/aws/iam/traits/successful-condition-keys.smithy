$version: "1.0"
namespace smithy.example

@aws.api#service(sdkId: "My")
@aws.iam#defineConditionKeys("foo:baz": {type: "String", documentation: "Foo baz"})
service MyService {
  version: "2019-02-20",
  operations: [Operation1],
  resources: [Resource1]
}

@aws.iam#conditionKeys(["aws:accountId", "foo:baz"])
operation Operation1 {}

@aws.iam#conditionKeys(["aws:accountId", "foo:baz"])
resource Resource1 {
  identifiers: {
    id1: ArnString,
  },
  resources: [Resource2]
}

@aws.iam#iamResource(name: "ResourceTwo")
resource Resource2 {
  identifiers: {
    id1: ArnString,
    id2: FooString,
  },
  read: GetResource2,
  list: ListResource2,
}

@readonly
operation GetResource2 {
    input: GetResource2Input
}

structure GetResource2Input {
  @required
  id1: ArnString,

  @required
  id2: FooString
}

@documentation("This is Foo")
string FooString

@readonly
operation ListResource2 {
    input: ListResource2Input,
    output: ListResource2Output
}

structure ListResource2Input {
  @required
  id1: ArnString,
}

structure ListResource2Output {}

@aws.api#arnReference(type: "ec2:Instance")
string ArnString
