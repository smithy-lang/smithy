$version: "2.1"

namespace smithy.example

@length(min: 1, max: 10)
list TagList {
    member: String
}

structure MyStructure {
    tagsOne: TagList

    @length(min: 1, max: 10)
    tagsTwo: [String]
}
