$version: "2"

namespace smithy.example.documentexamples

use aws.protocols#restJson1

@restJson1
service DocumentExamples {
    version: "2024-01-01"
    operations: [
        GetArrayDocument
        GetObjectDocument
        GetScalarDocument
        GetBooleanDocument
        GetNumberDocument
        GetNullDocument
    ]
}

@readonly
@http(method: "GET", uri: "/array", code: 200)
operation GetArrayDocument {
    output := {
        data: Document
    }
}

@readonly
@http(method: "GET", uri: "/object", code: 200)
operation GetObjectDocument {
    output := {
        data: Document
    }
}

@readonly
@http(method: "GET", uri: "/scalar", code: 200)
operation GetScalarDocument {
    output := {
        data: Document
    }
}

@readonly
@http(method: "GET", uri: "/boolean", code: 200)
operation GetBooleanDocument {
    output := {
        data: Document
    }
}

@readonly
@http(method: "GET", uri: "/number", code: 200)
operation GetNumberDocument {
    output := {
        data: Document
    }
}

@readonly
@http(method: "GET", uri: "/null", code: 200)
operation GetNullDocument {
    output := {
        data: Document
    }
}

apply GetArrayDocument @examples([
    {
        title: "Array-valued document"
        documentation: "Document member whose example value is a JSON array"
        output: {
            data: [
                { name: "first", size: 1 }
                { name: "second", size: 2 }
            ]
        }
    }
])

apply GetObjectDocument @examples([
    {
        title: "Object-valued document"
        documentation: "Document member whose example value is a JSON object"
        output: {
            data: { name: "only", size: 1 }
        }
    }
])

apply GetScalarDocument @examples([
    {
        title: "Scalar-valued document"
        documentation: "Document member whose example value is a JSON string"
        output: {
            data: "just-a-string"
        }
    }
])

apply GetBooleanDocument @examples([
    {
        title: "Boolean-valued document"
        documentation: "Document member whose example value is a JSON boolean"
        output: {
            data: true
        }
    }
])

apply GetNumberDocument @examples([
    {
        title: "Number-valued document"
        documentation: "Document member whose example value is a JSON number"
        output: {
            data: 42
        }
    }
])

apply GetNullDocument @examples([
    {
        title: "Null-valued document"
        documentation: "Document member whose example value is JSON null"
        output: {
            data: null
        }
    }
])
