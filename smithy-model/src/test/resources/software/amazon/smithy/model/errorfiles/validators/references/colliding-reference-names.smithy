$version: "2.0"

namespace ns.foo

resource FooResource {
    identifiers: {
        fooId: String
    }
}

resource BarResource {
    identifiers: {
        barId: String
    }
}

@references([
    {resource: FooResource, name: "thing", ids: {fooId: "fooId"}}
    {resource: BarResource, name: "thing", ids: {barId: "barId"}}
])
structure MyStructure {
    @required
    fooId: String

    @required
    barId: String
}
