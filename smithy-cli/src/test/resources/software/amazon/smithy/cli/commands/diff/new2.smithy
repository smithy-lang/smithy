$version: "2.0"

namespace smithy.example


structure StructureOne {

    @pattern("^.*$")
    stringMember: String

    @clientOptional
    @required
    intMember: Integer
}
