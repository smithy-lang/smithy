$version: "2.0"

metadata selectorTests = [
    {
        selector: "service ~> operation"
        matches: [
            smithy.example#OperationA
            smithy.example#OperationB
            smithy.example#CreateBar
            smithy.example#GetBar
            smithy.example#UpdateBar
            smithy.example#DeleteBar
            smithy.example#ListBar
            smithy.example#PutBar
            smithy.example#NamedOperation
            smithy.example#NamedCollectionOperation
        ]
    }
    {
        selector: "service[trait|title] ~> operation:not([trait|http])"
        matches: [
            smithy.example#OperationB
            smithy.example#CreateBar
            smithy.example#GetBar
            smithy.example#UpdateBar
            smithy.example#DeleteBar
            smithy.example#ListBar
            smithy.example#PutBar
            smithy.example#NamedOperation
            smithy.example#NamedCollectionOperation
        ]
    }
    {
        selector: "string :test(< member < list)"
        skipPreludeShapes: true
        matches: [
            smithy.example#String
            smithy.example#BarId
        ]
    }
    {
        selector: ":not([trait|trait]) :not(< *)"
        skipPreludeShapes: true
        matches: [
            smithy.example#Service
            smithy.example#List
            smithy.example#Structure
        ]
    }
    {
        selector: "[trait|streaming] :test(<) :not(< member < structure <-[input, output]- operation)"
        matches: [
            smithy.example#StreamBlob
        ]
    }
    {
        selector: "[trait|trait] :not(<-[trait]-)"
        skipPreludeShapes: true
        matches: [
            smithy.example#Regex
        ]
    }
    {
        selector: "resource >"
        matches: [
            smithy.example#BarId
            smithy.example#Color
            smithy.example#CreateBar
            smithy.example#GetBar
            smithy.example#UpdateBar
            smithy.example#DeleteBar
            smithy.example#ListBar
            smithy.example#PutBar
            smithy.example#NamedOperation
            smithy.example#NamedCollectionOperation
        ]
    }
    {
        selector: "resource <"
        matches: [
            smithy.example#Service
        ]
    }
    {
        selector: "resource <-[resource]-"
        matches: [
            smithy.example#Service
        ]
    }
    {
        selector: "resource -[property]->"
        matches: [
            smithy.example#Color
        ]
    }
    {
        selector: "resource -[identifier]->"
        matches: [
            smithy.example#BarId
        ]
    }
    {
        selector: "resource -[operation]->"
        matches: [
            smithy.example#NamedOperation
        ]
    }
    {
        selector: "resource -[collectionOperation]->"
        matches: [
            smithy.example#NamedCollectionOperation
        ]
    }
    {
        selector: "resource -[create]->"
        matches: [
            smithy.example#CreateBar
        ]
    }
    {
        selector: "resource -[read]->"
        matches: [
            smithy.example#GetBar
        ]
    }
    {
        selector: "resource -[update]->"
        matches: [
            smithy.example#UpdateBar
        ]
    }
    {
        selector: "resource -[delete]->"
        matches: [
            smithy.example#DeleteBar
        ]
    }
    {
        selector: "resource -[list]->"
        matches: [
            smithy.example#ListBar
        ]
    }
    {
        selector: "resource -[put]->"
        matches: [
            smithy.example#PutBar
        ]
    }
    {
        selector: "[id|name = CreateBar] ~>"
        matches: [
            smithy.example#CreateBarInput
            smithy.example#BarInstanceInput
            smithy.example#BarInstanceInput$barId
            smithy.example#BarId
            smithy.example#CreateBarInput$color
            smithy.example#Color
            smithy.example#Color$BLUE
            smithy.example#Color$GREEN
            smithy.example#Color$RED
            smithy.example#CreateBarOutput
            smithy.example#CreateBarOutput$barId
        ]
    }
    {
        selector: "[id|member = barId] >"
        matches: [
            smithy.example#BarId
        ]
    }
    {
        selector: "[id|member = barId] ~>"
        matches: [
            smithy.example#BarId
        ]
    }
]

namespace smithy.example

@title("Service")
service Service {
    version: "2019-06-17",
    operations: [OperationA, OperationB]
    resources: [Bar]
}

// Inherits the authorizer of ServiceA
@http(method: "GET", uri: "/operationA")
operation OperationA {}

operation OperationB {
    input: Input
}

string String

list List {
    member: String
}

structure Input {
    @required
    content: StreamFile
}

structure Structure {
    @required
    content: StreamBlob
}

@streaming
blob StreamBlob

@streaming
blob StreamFile

@trait
string Regex

resource Bar {
    properties: {
        color: Color
    }
    identifiers: {
        barId: BarId
    }
    create: CreateBar
    read: GetBar
    update: UpdateBar
    delete: DeleteBar
    list: ListBar
    put: PutBar
    operations: [NamedOperation]
    collectionOperations: [NamedCollectionOperation]
}

string BarId

enum Color {
    RED
    GREEN
    BLUE
}

@mixin
structure BarInstanceInput for Bar {
    @required
    $barId
}

operation CreateBar {
    input := for Bar {
        $color
    }
    output := with [BarInstanceInput] {}
}

@readonly
operation GetBar {
    input := with [BarInstanceInput] {}
    output := for Bar {
        @required
        $barId

        $color
    }
}

operation UpdateBar {
    input := for Bar with [BarInstanceInput] {
        $color
    }
}

@idempotent
operation DeleteBar {
    input := with [BarInstanceInput] {}
}

@readonly
operation ListBar {
    input := for Bar {
        $color
    }
    output := {
        bars: BarList
    }
}

list BarList {
    member: BarId
}

@idempotent
operation PutBar {
    input := with [BarInstanceInput] {
        color: Color
    }
}

operation NamedOperation {
    input := with [BarInstanceInput] {}
}

operation NamedCollectionOperation {}
