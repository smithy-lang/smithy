$version: "2"

namespace io.smithy.example

service SmithyExample {
    operations: [
        OperationOne
        OperationTwo
        OperationThree
        OperationFour
    ]
}

operation OperationOne {
    input := {
        @idempotencyToken
        stringMember: String
        integerMember: Integer
    }
    output: Unit
}

operation OperationTwo {
    input: StructureOne
    output: Unit
}

operation OperationThree {
    input: StructureTwo
    output: Unit
}

operation OperationFour {
    input: StructureThree
    output: StructureThree
}


structure StructureOne {
    @idempotencyToken
    stringMember: String
    integerMember: Integer
}


structure StructureTwo {
    stringMember: String
    structureOne: StructureOne
}


@mixin
structure StructureMixin {
    @idempotencyToken
    stringMember: String
}


structure StructureThree with [StructureMixin] {
    integerMember: Integer
}

