// See ChangedMemberTargetTest
$version: "2.0"

namespace smithy.example

structure A {
    member: B2
}

map B2 {
    key: MyString
    value: MyString2
}

string MyString

string MyString2
