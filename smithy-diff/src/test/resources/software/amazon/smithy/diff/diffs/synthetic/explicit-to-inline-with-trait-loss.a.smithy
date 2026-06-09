$version: "2.1"

namespace smithy.example

@sparse
list TagList {
    member: String
}

structure MyStructure {
    tags: TagList
}
