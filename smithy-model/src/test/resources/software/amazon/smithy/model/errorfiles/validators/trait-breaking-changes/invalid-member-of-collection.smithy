$version: "1.0"

namespace smithy.example

@trait(breakingChanges: [{path: "/foo", change: "any"}])
list badList {
    member: String
}

@trait(breakingChanges: [{path: "/foo", change: "any"}])
set badSet {
    member: String
}
