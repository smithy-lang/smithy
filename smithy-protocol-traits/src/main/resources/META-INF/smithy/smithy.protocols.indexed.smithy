$version: "2.0"

namespace smithy.protocols

/// Defines a stable wire index for a structure or union member.
@trait(
    selector: ":is(structure, union) :not([trait|mixin]) > member"
    breakingChanges: [
        {
            change: "update"
        }
    ]
)
@range(min: 1, max: 65535)
integer idx

/// Requires every structure and union in a service closure to be indexed.
@trait(selector: "service")
structure indexed {}
