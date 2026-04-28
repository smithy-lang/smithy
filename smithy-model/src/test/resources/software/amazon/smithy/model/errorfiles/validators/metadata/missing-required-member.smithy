$version: "2.0"

metadata auditLevel = {}

namespace smithy.example

@metadata(key: "auditLevel")
structure AuditLevelMetadata {
    @required
    level: AuditLevel

    reason: String
}

enum AuditLevel {
    LOW
    MEDIUM
    HIGH
}
