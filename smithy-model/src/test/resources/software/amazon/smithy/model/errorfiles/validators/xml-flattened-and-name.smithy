$version: "2"

namespace smithy.example

structure Struct {
    // No event because not flattened
    notFlattened: ListWithXmlName

    // Event because flattened and non-matching member name
    @xmlFlattened
    flattenedNoXmlName: ListWithXmlName

    // No event because the member name matches the xml name
    @xmlFlattened
    customMember: ListWithXmlName

    // No event because you're being explicit about the name to use
    @xmlFlattened
    @xmlName("customMember")
    flattenedMatchingXmlName: ListWithXmlName

    // No event because you're being explicit about the name to use
    @xmlFlattened
    @xmlName("Bar")
    flattenedNonMatchingXmlName: ListWithXmlName

    // Validator doesn't apply to maps
    @xmlFlattened
    flattenedMap: MapWithXmlName
}

list ListWithXmlName {
    @xmlName("customMember")
    member: String
}

map MapWithXmlName {
    @xmlName("customKey")
    key: String

    @xmlName("customValue")
    value: String
}
