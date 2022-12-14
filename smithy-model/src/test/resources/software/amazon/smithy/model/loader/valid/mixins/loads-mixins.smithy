$version: "2.0"

namespace smithy.example

@mixin
structure A {}

structure B with [A] {}

@mixin
structure C {}

@mixin
structure D with [C] {}

@mixin
structure E with [D] {}

structure F with [A, E] {}

@mixin
blob MixinBlob

blob MixedBlob with [MixinBlob]

@mixin
boolean MixinBoolean

boolean MixedBoolean with [MixinBoolean]

@mixin
string MixinString

string MixedString with [MixinString]

@mixin
byte MixinByte

byte MixedByte with [MixinByte]

@mixin
short MixinShort

short MixedShort with [MixinShort]

@mixin
integer MixinInteger

integer MixedInteger with [MixinInteger]

@mixin
long MixinLong

long MixedLong with [MixinLong]

@mixin
float MixinFloat

float MixedFloat with [MixinFloat]

@mixin
double MixinDouble

double MixedDouble with [MixinDouble]

@mixin
bigInteger MixinBigInt

bigInteger MixedBigInt with [MixinBigInt]

@mixin
bigDecimal MixinBigDecimal

bigDecimal MixedBigDecimal with [MixinBigDecimal]

@mixin
timestamp MixinTimestamp

timestamp MixedTimestamp with [MixinTimestamp]

@mixin
document MixinDocument

document MixedDocument with [MixinDocument]

@mixin
list MixinList {
    member: String
}

list MixedList with [MixinList] {}

@mixin
map MixinMap {
    key: String
    value: String
}

map MixedMap with [MixinMap] {}

map MixedMapRedefineValue with [MixinMap] {
    value: String
}

@mixin
service MixinService {}

service MixedService with [MixinService] {}

@mixin
resource MixinResource {}

resource MixedResource with [MixinResource] {}

@mixin
operation MixinOperation {}

operation MixedOperation with [MixinOperation] {}
