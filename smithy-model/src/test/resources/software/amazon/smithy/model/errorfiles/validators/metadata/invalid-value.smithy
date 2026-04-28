$version: "2.0"

metadata auditLevel = "unknown"

namespace smithy.example

@metadata(key: "auditLevel")
enum AuditLevel {
    LOW
    MEDIUM
    HIGH
}
