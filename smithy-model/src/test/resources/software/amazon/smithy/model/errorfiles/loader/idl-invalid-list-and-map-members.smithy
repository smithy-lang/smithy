$version: "2.0"

namespace com.test

list NoMemberList {}

map NoMemberMap {}

map MissingValueMap {
    key: String
}

map MissingKeyMap {
    value: String
}

list WrongMemberList {
    foo: String
}

map WrongMemberMap {
    foo: String
    value: String
}

map OtherWrongMemberMap {
    key: String
    foo: String
}

map BothWrongMembersMap {
    foo: String
    bar: String
}

list ExtraMemberList {
    member: String
    foo: String
}

map ExtraMemberMap {
    key: String
    value: String
    foo: String
}
