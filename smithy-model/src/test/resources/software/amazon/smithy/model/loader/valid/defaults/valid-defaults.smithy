$version: "2.0"

namespace smithy.example

structure Foo {
    a: String = ""
    b: Boolean = true
    c: StringList     =     []
    d: Document = {}
    e: Document = "hi"
    f: Document = true
    g: Document = false
    h: Document = []
    i: Timestamp = 0
    j: Blob = ""
    k: Byte = 1
    l: Short = 1
    m: Integer = 10
    n: Long = 100
    o: Float = 0
    p: Double= 0
    q: StringMap = {} // comment
    r: BigInteger = 0
    s: BigDecimal = 0
}

list StringList {
    member: String
}

map StringMap {
    key: String
    value: String
}
