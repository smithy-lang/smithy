metadata suppressions = [
    {
        id: "DeprecatedShape.smithy.example#MyOtherString",
        namespace: "*",
        reason: "shhhh"
    }
]

namespace smithy.example

structure MyStruct {
    @suppress(["DeprecatedShape"])
    myString: MyString

    myOtherString: MyOtherString
}

@deprecated
string MyString

@deprecated
string MyOtherString
