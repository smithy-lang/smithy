$version: "2.0"

namespace ns.foo

@meta
@threeMeta
@trait
structure bar {
    member: BarTraitShapeMember
}

@threeMeta
@tooMeta
@trait
structure meta {
    member: String
}

@trait
structure quux {
    member: QuuxTraitShapeMember
}

@trait
structure threeMeta {
    member: String
}

@trait
structure tooMeta {
    member: String
}

@metadata(key: "config")
structure MetadataConfig {
    auditLevel: AuditLevel
}

enum AuditLevel {
    LOW
    MEDIUM
    HIGH
}

service MyService {
    version: "2017-01-19"
    operations: [
        MyOperation
    ]
}

operation MyOperation {
    input: MyOperationInput
    output: Unit
}

structure MyOperationInput {
    fizz: Include1
    buzz: Include2
}

string BarTraitShapeMember

@quux(
    member: "pop"
)
string Exclude1

string Exclude2

@bar(
    member: "baz"
)
string Include1

string Include2

string QuuxTraitShapeMember
