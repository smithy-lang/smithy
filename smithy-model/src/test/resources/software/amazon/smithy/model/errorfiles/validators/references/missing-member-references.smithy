$version: "2.0"

namespace ns.foo

resource FooResource {
    identifiers: {
        fooId: String
    }
}

operation Operation1 {
    input := {
        fooId: String
    }
}

operation Operation2 {
    input := @references([{
        resource: FooResource
    }]) {
        fooId: String
    }
}

resource BarResource {
    identifiers: {
        barId: BarId
    }
}

operation Operation3 {
    input := @references([{
        resource: BarResource
    }]) {
        barId: BarId
    }
}

string BarId
