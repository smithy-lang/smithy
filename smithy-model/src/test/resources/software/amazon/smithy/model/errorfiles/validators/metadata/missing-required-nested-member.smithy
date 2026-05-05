$version: "2.0"

metadata auditLevel = [
    {
        level: "HIGH"
    }
]

namespace smithy.example

@metadata(key: "auditLevel")
list AuditLevelMetadata {
    member: NamespaceAuditLevel
}

structure NamespaceAuditLevel {
    @required
    namespace: String

    @required
    level: AuditLevel
}

enum AuditLevel {
    LOW
    MEDIUM
    HIGH
}
