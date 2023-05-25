$version: "2.0"

namespace com.test

resource MyResource {
    identifiers: {
        id: String
        member: String
        key: String
    }
    properties: {
        value: String
        other: String
    }
}

list WrongMemberList for MyResource {
    $id
}

list ExtraElidedMemberList for MyResource {
    $member
    $other
}

@mixin
list MixinList {
    member: Integer
}

list ConflictingTypesList for MyResource with [MixinList] {
    $member
}

map WrongMembersMap for MyResource {
    $id
    $other
}

map ExtraMemberMap for MyResource {
    $key
    $value
    $other
}

@mixin
map MixinMap {
    key: Integer
    value: Integer
}

map ConflictingTypesMap for MyResource with [MixinMap] {
    $key
    $value
}
