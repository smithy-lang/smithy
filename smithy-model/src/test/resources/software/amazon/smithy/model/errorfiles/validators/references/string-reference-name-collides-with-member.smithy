$version: "2.0"

namespace ns.foo

resource FooResource {
    identifiers: {
        fooId: String
    }
}

@references([{resource: FooResource, name: "thing"}])
string FooReference

structure MyStructure {
    @required
    thing: FooReference
}
