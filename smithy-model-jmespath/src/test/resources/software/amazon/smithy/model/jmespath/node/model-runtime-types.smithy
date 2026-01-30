$version: "2.0"

namespace smithy.example

@length(min: 4, max: 8)
string SizedString1

@length(max: 1)
string SizedString2

@length(min: 8)
string SizedString3

@range(min: 100, max: 1000)
integer SizedInteger1

@range(max: 2)
integer SizedInteger2

@range(min: 2)
integer SizedInteger3

list StringList {
    member: String,
}

@length(min: 5, max: 1000)
list SizedStringList {
    member: String,
}

map StringListMap {
    key: String,
    value: StringList,
}

@length(min: 5, max: 1000)
map SizedStringListMap {
    key: String,
    value: StringList,
}

union MyUnion {
    foo: String
}

structure MyStruct {
    foo: String
}

structure RecursiveStruct {
    foo: StringList,
    bar: RecursiveStructList,
}

@length(min: 1, max: 1)
list RecursiveStructList {
    member: RecursiveStruct
}
