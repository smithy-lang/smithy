$version: "2.0"

namespace smithy.example

@trait(breakingChanges: [{path: "/foo", change: "any"}])
list badList {
    member: String
}

@uniqueItems
@trait(breakingChanges: [{path: "/foo", change: "any"}])
list badSet {
    member: String
}
