$version: "2.0"

namespace smithy.example

structure Foo {
    @httpPrefixHeaders("")
    headers: SparseMap
}

@sparse
map SparseMap {
    key: String,
    value: String
}
