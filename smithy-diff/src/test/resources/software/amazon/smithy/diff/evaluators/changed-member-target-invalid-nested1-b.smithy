// See ChangedMemberTargetTest
namespace smithy.example

list A {
    member: B2
}

list B2 {
    @pattern("^[a-z]+$")
    member: MyString
}

string MyString
