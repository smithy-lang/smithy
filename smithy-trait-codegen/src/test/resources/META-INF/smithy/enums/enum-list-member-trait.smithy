$version: "2.0"

namespace test.smithy.traitcodegen.enums

@trait(selector: "structure")
structure EnumListMemberTrait {
    value: EnumList
}

list EnumList {
    member: SomeEnum
}

enum SomeEnum {
    SOME = "some"
    NONE = "none"
}
