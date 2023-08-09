$version: "2.0"

namespace smithy.example

use smithy.openapi#specificationExtension

@trait
@specificationExtension(as: "x-blob")
blob blobExt

@trait
@specificationExtension(as: "x-boolean")
boolean booleanExt

@trait
@specificationExtension(as: "x-string")
string stringExt

@trait
@specificationExtension(as: "x-byte")
byte byteExt

@trait
@specificationExtension(as: "x-short")
short shortExt

@trait
@specificationExtension(as: "x-integer")
integer integerExt

@trait
@specificationExtension(as: "x-long")
long longExt

@trait
@specificationExtension(as: "x-float")
float floatExt

@trait
@specificationExtension(as: "x-double")
double doubleExt

@trait
@specificationExtension(as: "x-big-integer")
bigInteger bigIntegerExt

@trait
@specificationExtension(as: "x-big-decimal")
bigDecimal bigDecimalExt

@trait
@specificationExtension(as: "x-timestamp")
timestamp timestampExt

@trait
@specificationExtension(as: "x-document")
document documentExt

@trait
@specificationExtension(as: "x-enum")
enum enumExt {
    FIRST = "first"
    SECOND = "second"
    THIRD = "third"
}

@trait
@specificationExtension(as: "x-int-enum")
intEnum intEnumExt {
    ONE = 1
    TWO = 2
    THREE = 3
}

@trait
@specificationExtension(as: "x-list")
list listExt {
    member: String
}

@trait
@specificationExtension(as: "x-map")
map mapExt {
    key: String
    value: Integer
}

@trait
@specificationExtension
structure structureExt {
    stringMember: String
    integerMember: Integer
}

@trait
@specificationExtension
union unionExt {
    i32: Integer
    string: String
}
