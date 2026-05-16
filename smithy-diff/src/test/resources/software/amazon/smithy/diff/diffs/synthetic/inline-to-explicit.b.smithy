$version: "2.1"

namespace smithy.example

list TagList {
    member: String
}

structure MyStructure {
    tags: TagList
}
