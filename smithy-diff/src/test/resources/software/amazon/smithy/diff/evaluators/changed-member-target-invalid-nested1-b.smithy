// See ChangedMemberTargetTest
$version: "2.0"

namespace smithy.example

list A {
    member: B2
}

list B2 {
    @sensitive
    member: MyString
}

string MyString
