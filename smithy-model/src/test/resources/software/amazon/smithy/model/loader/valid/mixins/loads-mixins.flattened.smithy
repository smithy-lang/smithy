$version: "2.0"

namespace smithy.example

structure B {}

structure F {}

blob MixedBlob

boolean MixedBoolean

string MixedString

byte MixedByte

short MixedShort

integer MixedInteger

long MixedLong

float MixedFloat

double MixedDouble

bigInteger MixedBigInt

bigDecimal MixedBigDecimal

timestamp MixedTimestamp

document MixedDocument

list MixedList {
    member: String
}

map MixedMap {
    key: String
    value: String
}

map MixedMapRedefineValue {
    key: String
    value: String
}

service MixedService {}

resource MixedResource {}

operation MixedOperation {}
