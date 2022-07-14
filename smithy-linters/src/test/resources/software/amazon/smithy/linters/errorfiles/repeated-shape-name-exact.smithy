$version: "2.0"

metadata validators = [{
    name: "RepeatedShapeName",
    configuration: {
        exactMatch: true
    }
}]

namespace smithy.example

structure RepeatingStructure {
    repeatingStructure: String,

    // This is fine because it's not an exact match
    repeatingStructureMember: String,
}

union RepeatingUnion {
    repeatingUnion: String,

    // This is fine because it's not an exact match
    repeatingUnionMember: String,
}
