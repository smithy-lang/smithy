$version: "2.0"

metadata selectorTests = [
    {
        selector: "list:test(> member > string)"
        skipPreludeShapes: true
        matches: [
            smithy.example#StringList
        ]
    }
    {
        selector: ":is(string, number)"
        skipPreludeShapes: true
        matches: [
            smithy.example#SimpleString
            smithy.example#SimpleInteger
        ]
    }
    {
        selector: "member > :is(string, number)"
        skipPreludeShapes: true
        matches: [
            smithy.example#SimpleInteger,
            smithy.example#SimpleString
        ]
    }
    {
        selector: ":is(list > member > string, map > member > number)"
        skipPreludeShapes: true
        matches: [
            smithy.example#SimpleString
            smithy.example#SimpleInteger
        ]
    }
    {
        selector: ":not(string) :not(number) :not(structure) :not(service) :not(operation) :not(resource)"
        skipPreludeShapes: true
        matches: [
            smithy.example#IntegerList
            smithy.example#IntegerList$member
            smithy.example#SimpleMap
            smithy.example#SimpleMap$key
            smithy.example#SimpleMap$value
            smithy.example#StringList
            smithy.example#StringList$member
        ]
    }
    {
        selector: "list :not(> member > string)"
        skipPreludeShapes: true
        matches: [
            smithy.example#IntegerList
        ]
    }
    {
        selector: ":topdown([trait|smithy.example#dataPlane])"
        matches: [
            smithy.example#OperationA
            smithy.example#Resource
            smithy.example#ServiceA
        ]
    }
    {
        selector: ":topdown([trait|smithy.example#dataPlane], [trait|smithy.example#controlPlane])"
        matches: [
            smithy.example#OperationA
            smithy.example#ServiceA
        ]
    }
]

namespace smithy.example

string SimpleString

integer SimpleInteger

list StringList {
    member: SimpleString
}

list IntegerList {
    member: SimpleInteger
}

map SimpleMap {
    key: String,
    value: SimpleInteger
}

@trait(selector: ":test(service, resource, operation)")
structure dataPlane {}

@trait(selector: ":test(service, resource, operation)")
structure controlPlane {}

@dataPlane
service ServiceA {
    version: "2019-06-17",
    operations: [OperationA]
    resources: [Resource]
}

@controlPlane
service ServiceB {
    version: "2019-06-17",
    operations: [OperationB]
    resources: [Resource]
}

@dataPlane
operation OperationA {}

@controlPlane
operation OperationB {}

@controlPlane
resource Resource {}
