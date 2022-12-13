$version: "2"

namespace smithy.example

// Does not match - :not(string) :(float)
// Matches string - :not([trait|length]) :not([trait|pattern])
string MyString

// Matches - :not(string)
// Does not match - :not(string) :not(float)
float MyFloat

// does not match - list :not(> member > string)
list StringList {
    member: MyString,
}

// Matches - list :not(> member > string)
list FloatList {
    member: MyFloat,
}

// Does not match - :not(string) :not([trait|sensitive])
@sensitive
string SensitiveString

// structure > member
//       :test(> string :not([trait|length]))
//       :test(:not([trait|length]))
structure StructA {
    noLengthOnEither: MyString,

    lengthOnTarget: LengthString,

    @length(min: 10)
    lengthOnMember: MyString,
}

@length(min: 100)
string LengthString

@trait
@protocolDefinition
structure someProtocol {}

@someProtocol
service HasProtocolTraits {
    version: "XYZ"
}

// service :not(-[trait]-> [trait|protocolDefinition])
service HasNoProtocolTraits {
    version: "XYZ"
}
