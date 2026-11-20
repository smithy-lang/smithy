$version: "2.0"

namespace ns.foo

resource FooResource {
    identifiers: {
        fooId: String
    }
}

// The reference name collides with the structure's own member of the same name, so the
// handle projection input.fooId would shadow that member.
@references([
    {resource: FooResource, name: "fooId", ids: {fooId: "fooId"}}
])
structure MyStructure {
    @required
    fooId: String
}
