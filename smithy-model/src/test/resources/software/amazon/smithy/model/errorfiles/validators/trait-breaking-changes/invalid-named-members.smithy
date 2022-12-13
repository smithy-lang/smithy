$version: "2.0"

namespace smithy.example

@trait(breakingChanges: [{path: "/foo/bar", change: "any"}])
structure a {
    baz: String
}

@trait(breakingChanges: [{path: "/foo/bar", change: "any"}])
union b {
    bam: String
}
