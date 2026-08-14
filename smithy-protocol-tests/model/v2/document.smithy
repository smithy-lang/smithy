$version: "2.0"

namespace smithy.protocoltests.corpus

// DocumentProtocolTestService tests document scalars.
@mixin
service DocumentProtocolTestService {
    operations: [
        DocumentMembers
        ListOfDocuments
        MapOfDocuments
        DocumentUnion
    ]
}

operation DocumentMembers {
    input: DocumentMembersInput
    output: DocumentMembersOutput
}

structure DocumentMembersInput {
    documentValue: Document
    nestedStruct: DocumentStruct
}

structure DocumentMembersOutput {
    documentValue: Document
    nestedStruct: DocumentStruct
}

structure DocumentStruct {
    documentMember: Document
    stringMember: String
}

operation ListOfDocuments {
    input: ListOfDocumentsInput
    output: ListOfDocumentsOutput
}

structure ListOfDocumentsInput {
    values: DocumentList
}

structure ListOfDocumentsOutput {
    values: DocumentList
}

list DocumentList {
    member: Document
}

operation MapOfDocuments {
    input: MapOfDocumentsInput
    output: MapOfDocumentsOutput
}

structure MapOfDocumentsInput {
    values: DocumentMap
}

structure MapOfDocumentsOutput {
    values: DocumentMap
}

map DocumentMap {
    key: String
    value: Document
}

operation DocumentUnion {
    input: DocumentUnionInput
    output: DocumentUnionOutput
}

structure DocumentUnionInput {
    value: DocumentUnionShape
}

structure DocumentUnionOutput {
    value: DocumentUnionShape
}

union DocumentUnionShape {
    documentValue: Document
    stringValue: String
    integerValue: Integer
}
