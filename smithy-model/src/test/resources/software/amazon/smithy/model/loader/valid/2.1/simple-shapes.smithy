$version: "2.1"

namespace smithy.example

string MyString

byte MyByte

short MyShort

integer MyInteger

long MyLong

float MyFloat

double MyDouble

bigInteger MyBigInteger

bigDecimal MyBigDecimal

boolean MyBoolean

blob MyBlob

timestamp MyTimestamp

list MyList {
    member: String
}

map MyMap {
    key: String
    value: String
}

structure MyStruct {
    foo: String
    bar: Integer
}

union MyUnion {
    a: String
    b: Integer
}
