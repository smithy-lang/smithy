$version: "2.0"

metadata selectorTests = [
    {
        selector: "service ~> operation"
        matches: [
            smithy.example#OperationA
            smithy.example#OperationB
        ]
    }
    {
        selector: "service[trait|title] ~> operation:not([trait|http])"
        matches: [
            smithy.example#OperationB
        ]
    }
    {
        selector: "string :test(< member < list)"
        skipPreludeShapes: true
        matches: [
            smithy.example#String
        ]
    }
    {
        selector: ":not([trait|trait]) :not(< *)"
        skipPreludeShapes: true
        matches: [
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
]

namespace smithy.example

@title("Service")
service Service {
    version: "2019-06-17",
    operations: [OperationA, OperationB]
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
