$version: "2.0"

metadata validators = [
    {
        name: "EmitEachSelector",
        id: "BadShape.List",
        message: "Lists are not allowed",
        namespace: "smithy.example",
        configuration: {
            selector: "list"
        }
    },
    {
        name: "EmitEachSelector",
        id: "BadShape.Map",
        message: "Maps are not allowed",
        severity: "WARNING",
        namespace: "smithy.example",
        configuration: {
            selector: "map"
        }
    },
    {
        name: "EmitEachSelector",
        id: "BadTrait",
        message: "Documentation is not allowed",
        namespace: "smithy.example",
        configuration: {
            selector: "[trait|documentation]"
        }
    },
    {
        name: "EmitEachSelector",
        id: "BadTrait.Default",
        message: "Default is not allowed",
        namespace: "smithy.example",
        configuration: {
            selector: "[trait|default]"
        }
    },
    {
        name: "EmitEachSelector",
        id: "BadTrait.Required",
        message: "Required is not allowed",
        namespace: "smithy.example",
        configuration: {
            selector: "[trait|required]"
        }
    }
]

metadata suppressions = [
    {
        id: "BadShape.Map", // ignore BadShape.Map, but leave BadShape.List alone.
        namespace: "*",
        reason: "Allow maps",
    },
    {
        id: "BadTrait",
        namespace: "*", // Ignore BadTrait and BadTrait's children.
    }
]

namespace smithy.example

list MyList1 {
    member: String
}

@suppress(["BadShape"])
list MyList2 {
    member: String
}

@suppress(["BadShape.List"])
list MyList3 {
    member: String
}

map MyMap {
    key: String
    value: String
}

structure Foo {
    @required
    a: String

    /// Docs
    b: String

    c: String = ""

    d: String
}
