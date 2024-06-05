$version: "2"

namespace ns.foo

service ErrorBound {
    version: "2020-07-02"
    operations: [
        WithError
        WithoutError
    ]
    errors: [ServiceError]
}

service ErrorBound2 {
    version: "2020-07-02"
    operations: [
        WithoutError2
    ]
    errors: [ServiceError2]
}

service NoError {
    version: "2020-07-02"
    operations: [
        WithError
        WithoutError
    ]
}

@examples([
    {
        "title": "Testing 1",
        "error": {
            "shapeId": "ns.foo#OperationError"
            "content": {
                "foo": "baz"
            }
        }
    }
    {
        "title": "Testing 2"
        "error": {
            "shapeId": "ns.foo#ServiceError"
            "content": {
                "foo": "baz"
            }
        }
    }
])
operation WithError {
    input := {}
    errors: [OperationError]
}

@examples([
    {
        "title": "Testing 3",
        "error": {
            "shapeId": "ns.foo#OperationError"
            "content": {
                "foo": "baz"
            }
        }
    }
    {
        "title": "Testing 4"
        "error": {
            "shapeId": "ns.foo#ServiceError"
            "content": {
                "foo": "baz"
            }
        }
    }
])
operation WithoutError {
    input := {}
    output := {}
}

@examples([
    {
        "title": "Testing 5"
        "error": {
            "shapeId": "ns.foo#ServiceError2"
            "content": {
                "foo": "baz"
            }
        }
    }
])
operation WithoutError2 {
    input := {}
    output := {}
}

@error("client")
structure ServiceError {
    foo: String
}

@error("client")
structure ServiceError2 {
    foo: String
}

@error("client")
structure OperationError {
    foo: String
}
