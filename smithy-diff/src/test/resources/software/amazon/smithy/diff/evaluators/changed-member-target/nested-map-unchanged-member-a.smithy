// See ChangedMemberTargetTest
$version: "2.0"

namespace smithy.example

structure A {
    member: B1
}

map B1 {
    key: MyString
    value: MyString
}

string MyString
