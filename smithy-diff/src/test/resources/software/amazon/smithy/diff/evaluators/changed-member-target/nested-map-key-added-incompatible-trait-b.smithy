// See ChangedMemberTargetTest
$version: "2.0"

namespace smithy.example

structure A {
    member: B2
}

map B2 {
    @noAddingTrait
    key: MyString

    value: MyString
}

string MyString

@trait(
    selector: "member",
    breakingChanges: [
        {change: "add"}
    ]
)
structure noAddingTrait {}
