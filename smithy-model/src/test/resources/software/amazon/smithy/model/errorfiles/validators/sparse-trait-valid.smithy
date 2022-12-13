$version: "2.0"

namespace smithy.example

@sparse
list SparseList {
    member: String
}

@sparse
map SparseMap {
    key: String,
    value: String
}
