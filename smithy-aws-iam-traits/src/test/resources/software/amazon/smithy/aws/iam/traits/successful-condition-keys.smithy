$version: "1.0"
namespace smithy.example

use aws.api#arnReference
use aws.api#service
use aws.iam#conditionKeys
use aws.iam#defineConditionKeys
use aws.iam#disableConditionKeyInference
use aws.iam#iamResource

@service(sdkId: "My")
@defineConditionKeys(
  "foo:baz": {
    type: "String",
    documentation: "Foo baz",
    relativeDocumentation: "condition-keys.html"
  }
)
service MyService {
  version: "2019-02-20",
  operations: [Operation1],
  resources: [Resource1]
}

@conditionKeys(["aws:accountId", "foo:baz"])
operation Operation1 {}

@conditionKeys(["aws:accountId", "foo:baz"])
resource Resource1 {
  identifiers: {
    id1: ArnString,
  },
  resources: [Resource2, Resource3, Resource4]
}

@iamResource(name: "ResourceTwo")
resource Resource2 {
  identifiers: {
    id1: ArnString,
    id2: FooString,
  },
  read: GetResource2,
  list: ListResource2,
}

@disableConditionKeyInference
@iamResource(disableConditionKeyInheritance: true)
resource Resource3 {
  identifiers: {
    id1: ArnString
    id2: FooString
    id3: String
  }
}

@disableConditionKeyInference
@iamResource(disableConditionKeyInheritance: true)
@conditionKeys(["foo:baz"])
resource Resource4 {
  identifiers: {
    id1: ArnString
    id2: FooString
    id4: String
  }
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

@arnReference(type: "ec2:Instance")
string ArnString
