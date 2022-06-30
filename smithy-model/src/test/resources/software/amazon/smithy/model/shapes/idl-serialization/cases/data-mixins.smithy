$version: "2.0"

namespace smithy.example

list MixedList with [MixinList] {}

@internal
@mixin
list MixinList {
    member: String
}

map MixedMap with [MixinMap] {}

@internal
@mixin
map MixinMap {
    key: String
    value: String
}

bigDecimal MixedBigDecimal with [MixinBigDecimal]

bigInteger MixedBigInt with [MixinBigInt]

blob MixedBlob with [MixinBlob]

boolean MixedBoolean with [MixinBoolean]

byte MixedByte with [MixinByte]

document MixedDocument with [MixinDocument]

double MixedDouble with [MixinDouble]

float MixedFloat with [MixinFloat]

integer MixedInteger with [MixinInteger]

long MixedLong with [MixinLong]

short MixedShort with [MixinShort]

string MixedString with [MixinString]

timestamp MixedTimestamp with [MixinTimestamp]

@internal
@mixin
bigDecimal MixinBigDecimal

@internal
@mixin
bigInteger MixinBigInt

@internal
@mixin
blob MixinBlob

@internal
@mixin
boolean MixinBoolean

@internal
@mixin
byte MixinByte

@internal
@mixin
document MixinDocument

@internal
@mixin
double MixinDouble

@internal
@mixin
float MixinFloat

@internal
@mixin
integer MixinInteger

@internal
@mixin
long MixinLong

@internal
@mixin
short MixinShort

@internal
@mixin
string MixinString

@internal
@mixin
timestamp MixinTimestamp
