$version: "2.0"

namespace ns.foo

resource FooResource {
    identifiers: {
        fooId: String
    }
}

// The reference name is distinct from the member that targets the string shape, so the
// handle projection input.fooHandle does not shadow the input.fooId value.
@references([{resource: FooResource, name: "fooHandle"}])
string FooReference

structure MyStructure {
    @required
    fooId: FooReference
}
