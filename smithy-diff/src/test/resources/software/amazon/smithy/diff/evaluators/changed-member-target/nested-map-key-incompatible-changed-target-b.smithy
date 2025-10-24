// See ChangedMemberTargetTest
$version: "2.0"

namespace smithy.example

structure A {
    member: B2
}

map B2 {
    key: MyEnum
    value: MyString
}

string MyString

enum MyEnum {
    FOO = "foo"
}
