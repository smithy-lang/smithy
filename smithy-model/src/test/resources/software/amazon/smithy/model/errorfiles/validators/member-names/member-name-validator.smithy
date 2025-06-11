$version: "2.0"

namespace ns.foo

// Valid lowerCamelCase member names
structure ValidMemberNames {
    a: String
    validName: String
    xmlRequest: String
    fooId: String
    camelCase123: String
    httpResponseCode: String
    jsonData: String
}

// Invalid member names - start with uppercase
structure InvalidUppercaseStart {
    InvalidName: String
    A: String
    HttpHeader: String
}

// Invalid member names - contain underscores
structure InvalidWithUnderscores {
    invalid_name: String
    foo_bar: String
    XML_REQUEST: String
}

// Invalid member names - start with numbers (if somehow possible)
structure InvalidNumberStart {
    // Note: This would actually be caught by IDL parser, but included for completeness
}

// Test unions as well
union ValidUnion {
    validOption: String
    xmlData: String
}

union InvalidUnion {
    InvalidOption: String
    invalid_option: String
}