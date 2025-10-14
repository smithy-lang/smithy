// See ChangedMemberTargetTest
$version: "2.0"

namespace smithy.example

list A {
    member: B2
}

list B2 {
    @noAddingTrait
    member: MyString
}

string MyString

@trait(
    selector: "member",
    breakingChanges: [
        {change: "add"}
    ]
)
structure noAddingTrait {}
