$version: "2.0"

namespace ns.foo

structure StructureWithMembers {
    a: String
    b: String
}

structure StructureWithoutMembers {}

union Union {
    byte: Byte
    double: Double
}

list List {
    member: String
}

map Map {
    key: String
    value: String
}

bigDecimal BigDecimal

bigInteger BigInteger

blob Blob

boolean Boolean

byte Byte

document Document

double Double

float Float

integer Integer

long Long

short Short

string String
