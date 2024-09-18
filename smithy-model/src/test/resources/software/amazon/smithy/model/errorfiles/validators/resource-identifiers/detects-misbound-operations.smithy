$version: "2.0"

namespace smithy.example

service FooService {
    version: "2020-07-02"
    operations: [GetFoo]
    resources: [Foo, BoundNoMatch]
}

resource Foo {
    identifiers: {
        fooId: String
    }
    create: CreateFoo
}

operation CreateFoo {
    input := {
        @required
        something: String
    }
    output := {
        @required
        fooId: String

        @required
        something: String
    }
}

operation GetFoo {
    input := {
        @required
        fooId: String
    }
    output := {
        @required
        something: String
    }
}

resource Unbound {
    identifiers: {
        id: String
    }
}

resource BoundNoMatch {
    identifiers: {
        id: String
    }
}
