$version: "2.0"

metadata auditLevel = "anything"

namespace smithy.example

@metadata(key: "auditLevel")
string AuditLevelA

@metadata(key: "auditLevel")
string AuditLevelB

@metadata(key: "auditLevel")
string AuditLevelC
