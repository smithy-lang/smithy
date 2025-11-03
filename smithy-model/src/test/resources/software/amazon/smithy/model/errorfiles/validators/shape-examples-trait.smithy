$version: "2"

namespace smithy.example

@shapeExamples({
    allowed: [
        1
        2
        3
    ]
    disallowed: [
        0
        4
    ]
})
@range(min: 1, max: 3)
byte ValidExamplesByte

@shapeExamples({
    allowed: [
        0
        4
    ]
    disallowed: [
        1
        2
        3
    ]
})
@range(min: 1, max: 3)
byte ConflictingExamplesByte

@shapeExamples({
    allowed: [
        1
        2
        3
    ]
    disallowed: [
        0
        4
    ]
})
@range(min: 1, max: 3)
short ValidExamplesShort

@shapeExamples({
    allowed: [
        0
        4
    ]
    disallowed: [
        1
        2
        3
    ]
})
@range(min: 1, max: 3)
short ConflictingExamplesShort

@shapeExamples({
    allowed: [
        1
        2
        3
    ]
    disallowed: [
        0
        4
    ]
})
@range(min: 1, max: 3)
integer ValidExamplesInteger

@shapeExamples({
    allowed: [
        0
        4
    ]
    disallowed: [
        1
        2
        3
    ]
})
@range(min: 1, max: 3)
integer ConflictingExamplesInteger

@shapeExamples({
    allowed: [
        1
        2
        3
    ]
    disallowed: [
        0
        4
    ]
})
@range(min: 1, max: 3)
long ValidExamplesLong

@shapeExamples({
    allowed: [
        0
        4
    ]
    disallowed: [
        1
        2
        3
    ]
})
@range(min: 1, max: 3)
long ConflictingExamplesLong

@shapeExamples({
    allowed: [
        1.0
        2.0
        3.0
    ]
    disallowed: [
        0.0
        4.0
    ]
})
@range(min: 1, max: 3)
float ValidExamplesFloat

@shapeExamples({
    allowed: [
        0.0
        4.0
    ]
    disallowed: [
        1.0
        2.0
        3.0
    ]
})
@range(min: 1, max: 3)
float ConflictingExamplesFloat

@shapeExamples({
    allowed: [
        1.0
        2.0
        3.0
    ]
    disallowed: [
        0.0
        4.0
    ]
})
@range(min: 1, max: 3)
double ValidExamplesDouble

@shapeExamples({
    allowed: [
        0.0
        4.0
    ]
    disallowed: [
        1.0
        2.0
        3.0
    ]
})
@range(min: 1, max: 3)
double ConflictingExamplesDouble

@shapeExamples({
    allowed: [
        1
        2
        3
    ]
    disallowed: [
        0
        4
    ]
})
@range(min: 1, max: 3)
bigInteger ValidExamplesBigInteger

@shapeExamples({
    allowed: [
        0
        4
    ]
    disallowed: [
        1
        2
        3
    ]
})
@range(min: 1, max: 3)
bigInteger ConflictingExamplesBigInteger

@shapeExamples({
    allowed: [
        1.0
        2.0
        3.0
    ]
    disallowed: [
        0.0
        4.0
    ]
})
@range(min: 1, max: 3)
bigDecimal ValidExamplesBigDecimal

@shapeExamples({
    allowed: [
        0.0
        4.0
    ]
    disallowed: [
        1.0
        2.0
        3.0
    ]
})
@range(min: 1, max: 3)
bigDecimal ConflictingExamplesBigDecimal

@shapeExamples({
    allowed: [
        "a"
        "aa"
        "aaa"
    ]
    disallowed: [
        ""
        "b"
        "ab"
        "ba"
        "aaaa"
    ]
})
@length(min: 1, max: 3)
@pattern("^a+$")
string ValidExamplesString

@shapeExamples({
    allowed: [
        ""
        "b"
        "ab"
        "ba"
        "aaaa"
    ]
    disallowed: [
        "a"
        "aa"
        "aaa"
    ]
})
@length(min: 1, max: 3)
@pattern("^a+$")
string ConflictingExamplesString

@shapeExamples({
    allowed: [
        "YQ=="
        "YWE="
        "YWFh"
    ]
    disallowed: [
        ""
        "YWFhYQ=="
    ]
})
@length(min: 1, max: 3)
blob ValidExamplesBlob

@shapeExamples({
    allowed: [
        ""
        "YWFhYQ=="
    ]
    disallowed: [
        "YQ=="
        "YWE="
        "YWFh"
    ]
})
@length(min: 1, max: 3)
blob ConflictingExamplesBlob

@shapeExamples({
    allowed: [
        { a: "b" }
        { a: "bb" }
        { a: "bbb" }
    ]
    disallowed: [
        { a: "" }
        { a: "bbbbb" }
        { }
        { a: null }
    ]
})
structure ValidExamplesStructure {
    @length(min: 1, max: 3)
    @required
    a: String
}

@shapeExamples({
    allowed: [
        { a: "" }
        { a: "bbbb" }
        { }
        { a: null }
    ]
    disallowed: [
        { a: "b" }
        { a: "bb" }
        { a: "bbb" }
    ]
})
structure ConflictingExamplesStructure {
    @length(min: 1, max: 3)
    @required
    a: String
}

@shapeExamples({
    allowed: [
        ["a"]
        ["a", "aa"]
        ["a", "aa", "aaa"]
    ]
    disallowed: [
        []
        [""]
        ["aaaa"]
        ["a", "a", "a", "a"]
    ]
})
@length(min: 1, max: 3)
list ValidExamplesList {
    @length(min: 1, max: 3)
    member: String
}

@shapeExamples({
    allowed: [
        []
        [""]
        ["aaaa"]
        ["a", "a", "a", "a"]
    ]
    disallowed: [
        ["a"]
        ["a", "aa"]
        ["a", "aa", "aaa"]
    ]
})
@length(min: 1, max: 3)
list ConflictingExamplesList {
    @length(min: 1, max: 3)
    member: String
}

@shapeExamples({
    allowed: [
        { "a": "bbb" }
        { "a": "b", "bbb": "aaa" }
    ]
    disallowed: [
        { "": "" }
        { "a": "" }
        { "": "a" }
    ]
})
@length(min: 1, max: 3)
map ValidExamplesMap {
    @length(min: 1, max: 3)
    key: String

    @length(min: 1, max: 3)
    value: String
}

@shapeExamples({
    allowed: [
        { "": "" }
        { "a": "" }
        { "": "a" }
    ]
    disallowed: [
        { "a": "bbb" }
        { "a": "b", "bbb": "aaa" }
    ]
})
@length(min: 1, max: 3)
map ConflictingExamplesMap {
    @length(min: 1, max: 3)
    key: String

    @length(min: 1, max: 3)
    value: String
}

structure DirectMembersStructure {

    @shapeExamples({
        allowed: [
            1
            2
            3
        ]
        disallowed: [
            null
            0
            4
        ]
    })
    @range(min: 1, max: 3)
    @required
    validExamplesByte: Byte

    @shapeExamples({
        allowed: [
            null
            0
            4
        ]
        disallowed: [
            1
            2
            3
        ]
    })
    @range(min: 1, max: 3)
    @required
    conflictingExamplesByte: Byte

    @shapeExamples({
        allowed: [
            1
            2
            3
        ]
        disallowed: [
            null
            0
            4
        ]
    })
    @range(min: 1, max: 3)
    @required
    validExamplesShort: Short

    @shapeExamples({
        allowed: [
            null
            0
            4
        ]
        disallowed: [
            1
            2
            3
        ]
    })
    @range(min: 1, max: 3)
    @required
    conflictingExamplesShort: Short


    @shapeExamples({
        allowed: [
            1
            2
            3
        ]
        disallowed: [
            null
            0
            4
        ]
    })
    @range(min: 1, max: 3)
    @required
    validExamplesInteger: Integer

    @shapeExamples({
        allowed: [
            null
            0
            4
        ]
        disallowed: [
            1
            2
            3
        ]
    })
    @range(min: 1, max: 3)
    @required
    conflictingExamplesInteger: Integer

    @shapeExamples({
        allowed: [
            1
            2
            3
        ]
        disallowed: [
            null
            0
            4
        ]
    })
    @range(min: 1, max: 3)
    @required
    validExamplesLong: Long

    @shapeExamples({
        allowed: [
            null
            0
            4
        ]
        disallowed: [
            1
            2
            3
        ]
    })
    @range(min: 1, max: 3)
    @required
    conflictingExamplesLong: Long

    @shapeExamples({
        allowed: [
            1.0
            2.0
            3.0
        ]
        disallowed: [
            null
            0.0
            4.0
        ]
    })
    @range(min: 1, max: 3)
    @required
    validExamplesFloat: Float

    @shapeExamples({
        allowed: [
            null
            0.0
            4.0
        ]
        disallowed: [
            1.0
            2.0
            3.0
        ]
    })
    @range(min: 1, max: 3)
    @required
    conflictingExamplesFloat: Float

    @shapeExamples({
        allowed: [
            1.0
            2.0
            3.0
        ]
        disallowed: [
            null
            0.0
            4.0
        ]
    })
    @range(min: 1, max: 3)
    @required
    validExamplesDouble: Double

    @shapeExamples({
        allowed: [
            null
            0.0
            4.0
        ]
        disallowed: [
            1.0
            2.0
            3.0
        ]
    })
    @range(min: 1, max: 3)
    @required
    conflictingExamplesDouble: Double

    @shapeExamples({
        allowed: [
            1
            2
            3
        ]
        disallowed: [
            null
            0
            4
        ]
    })
    @range(min: 1, max: 3)
    @required
    validExamplesBigInteger: BigInteger

    @shapeExamples({
        allowed: [
            null
            0
            4
        ]
        disallowed: [
            1
            2
            3
        ]
    })
    @range(min: 1, max: 3)
    @required
    conflictingExamplesBigInteger: BigInteger

    @shapeExamples({
        allowed: [
            1.0
            2.0
            3.0
        ]
        disallowed: [
            null
            0.0
            4.0
        ]
    })
    @range(min: 1, max: 3)
    @required
    validExamplesBigDecimal: BigDecimal

    @shapeExamples({
        allowed: [
            null
            0.0
            4.0
        ]
        disallowed: [
            1.0
            2.0
            3.0
        ]
    })
    @range(min: 1, max: 3)
    @required
    conflictingExamplesBigDecimal: BigDecimal

    @shapeExamples({
        allowed: [
            "a"
            "aa"
            "aaa"
        ]
        disallowed: [
            null
            ""
            "b"
            "ab"
            "ba"
            "aaaa"
        ]
    })
    @length(min: 1, max: 3)
    @pattern("^a+$")
    @required
    validExamplesString: String

    @shapeExamples({
        allowed: [
            null
            ""
            "b"
            "ab"
            "ba"
            "aaaa"
        ]
        disallowed: [
            "a"
            "aa"
            "aaa"
        ]
    })
    @length(min: 1, max: 3)
    @pattern("^a+$")
    @required
    conflictingExamplesString: String

    @shapeExamples({
        allowed: [
            "YQ=="
            "YWE="
            "YWFh"
        ]
        disallowed: [
            null
            ""
            "YWFhYQ=="
        ]
    })
    @length(min: 1, max: 3)
    @required
    validExamplesBlob: Blob

    @shapeExamples({
        allowed: [
            null
            ""
            "YWFhYQ=="
        ]
        disallowed: [
            "YQ=="
            "YWE="
            "YWFh"
        ]
    })
    @length(min: 1, max: 3)
    @required
    conflictingExamplesBlob: Blob

    @shapeExamples({
        allowed: [
            { a: "b" }
            { a: "bb" }
            { a: "bbb" }
        ]
        disallowed: [
            null
            { a: "" }
            { a: "bbbbb" }
            { }
            { a: null }
        ]
    })
    @required
    validExamplesStructure: TestStructure

    @shapeExamples({
        allowed: [
            null
            { a: "" }
            { a: "bbbbb" }
            { }
            { a: null }
        ]
        disallowed: [
            { a: "b" }
            { a: "bb" }
            { a: "bbb" }
        ]
    })
    @required
    conflictingExamplesStructure: TestStructure

    @shapeExamples({
        allowed: [
            ["a"]
            ["a", "aa"]
            ["a", "aa", "aaa"]
        ]
        disallowed: [
            null
            []
            [""]
            ["aaaa"]
            ["a", "a", "a", "a"]
        ]
    })
    @length(min: 1, max: 3)
    @required
    validExamplesList: TestList

    @shapeExamples({
        allowed: [
            null
            []
            [""]
            ["aaaa"]
            ["a", "a", "a", "a"]
        ]
        disallowed: [
            ["a"]
            ["a", "aa"]
            ["a", "aa", "aaa"]
        ]
    })
    @length(min: 1, max: 3)
    @required
    conflictingExamplesList: TestList

    @shapeExamples({
        allowed: [
            { "a": "bbb" }
            { "a": "b", "bbb": "aaa" }
        ]
        disallowed: [
            null
            { "": "" }
            { "a": "" }
            { "": "a" }
        ]
    })
    @required
    validExamplesMap: TestMap

    @shapeExamples({
        allowed: [
            null
            { "": "" }
            { "a": "" }
            { "": "a" }
        ]
        disallowed: [
            { "a": "bbb" }
            { "a": "b", "bbb": "aaa" }
        ]
    })
    @required
    conflictingExamplesMap: TestMap

    @shapeExamples({
        allowed: [
            true
            false
        ]
        disallowed: [
            null
        ]
    })
    @required
    validExamplesBoolean: Boolean

    @shapeExamples({
        allowed: [
            null
        ]
        disallowed: [
            true
            false
        ]
    })
    @required
    conflictingExamplesBoolean: Boolean

    @shapeExamples({
        disallowed: [
            null
        ]
    })
    @required
    validExamplesTimestamp: Timestamp

    @shapeExamples({
        allowed: [
            null
        ]
    })
    @required
    conflictingExamplesTimestamp: Timestamp

    @shapeExamples({
        allowed: [
            true
            "foo"
            123
            {}
            []
        ]
        disallowed: [
            null
        ]
    })
    @required
    validExamplesDocument: Document

    @shapeExamples({
        allowed: [
            null
        ]
        disallowed: [
            true
            "foo"
            123
            {}
            []
        ]
    })
    @required
    conflictingExamplesDocument: Document

}

structure IndirectMembersStructure {

    @shapeExamples({
        allowed: [
            null
            1
            2
            3
        ]
        disallowed: [
            0
            4
        ]
    })
    validExamplesByte: TestByte

    @shapeExamples({
        allowed: [
            0
            4
        ]
        disallowed: [
            null
            1
            2
            3
        ]
    })
    conflictingExamplesByte: TestByte

    @shapeExamples({
        allowed: [
            null
            1
            2
            3
        ]
        disallowed: [
            0
            4
        ]
    })
    validExamplesShort: TestShort

    @shapeExamples({
        allowed: [
            0
            4
        ]
        disallowed: [
            null
            1
            2
            3
        ]
    })
    conflictingExamplesShort: TestShort


    @shapeExamples({
        allowed: [
            null
            1
            2
            3
        ]
        disallowed: [
            0
            4
        ]
    })
    validExamplesInteger: TestInteger

    @shapeExamples({
        allowed: [
            0
            4
        ]
        disallowed: [
            null
            1
            2
            3
        ]
    })
    conflictingExamplesInteger: TestInteger

    @shapeExamples({
        allowed: [
            null
            1
            2
            3
        ]
        disallowed: [
            0
            4
        ]
    })
    validExamplesLong: TestLong

    @shapeExamples({
        allowed: [
            0
            4
        ]
        disallowed: [
            null
            1
            2
            3
        ]
    })
    conflictingExamplesLong: TestLong

    @shapeExamples({
        allowed: [
            null
            1.0
            2.0
            3.0
        ]
        disallowed: [
            0.0
            4.0
        ]
    })
    validExamplesFloat: TestFloat

    @shapeExamples({
        allowed: [
            0.0
            4.0
        ]
        disallowed: [
            null
            1.0
            2.0
            3.0
        ]
    })
    conflictingExamplesFloat: TestFloat

    @shapeExamples({
        allowed: [
            null
            1.0
            2.0
            3.0
        ]
        disallowed: [
            0.0
            4.0
        ]
    })
    validExamplesDouble: TestDouble

    @shapeExamples({
        allowed: [
            0.0
            4.0
        ]
        disallowed: [
            null
            1.0
            2.0
            3.0
        ]
    })
    conflictingExamplesDouble: TestDouble

    @shapeExamples({
        allowed: [
            null
            1
            2
            3
        ]
        disallowed: [
            0
            4
        ]
    })
    validExamplesBigInteger: TestBigInteger

    @shapeExamples({
        allowed: [
            0
            4
        ]
        disallowed: [
            null
            1
            2
            3
        ]
    })
    conflictingExamplesBigInteger: TestBigInteger

    @shapeExamples({
        allowed: [
            null
            1.0
            2.0
            3.0
        ]
        disallowed: [
            0.0
            4.0
        ]
    })
    validExamplesBigDecimal: TestBigDecimal

    @shapeExamples({
        allowed: [
            0.0
            4.0
        ]
        disallowed: [
            null
            1.0
            2.0
            3.0
        ]
    })
    conflictingExamplesBigDecimal: TestBigDecimal

    @shapeExamples({
        allowed: [
            null
            "a"
            "aa"
            "aaa"
        ]
        disallowed: [
            ""
            "b"
            "ab"
            "ba"
            "aaaa"
        ]
    })
    validExamplesString: TestString

    @shapeExamples({
        allowed: [
            ""
            "b"
            "ab"
            "ba"
            "aaaa"
        ]
        disallowed: [
            null
            "a"
            "aa"
            "aaa"
        ]
    })
    conflictingExamplesString: TestString

    @shapeExamples({
        allowed: [
            null
            "YQ=="
            "YWE="
            "YWFh"
        ]
        disallowed: [
            ""
            "YWFhYQ=="
        ]
    })
    validExamplesBlob: TestBlob

    @shapeExamples({
        allowed: [
            ""
            "YWFhYQ=="
        ]
        disallowed: [
            null
            "YQ=="
            "YWE="
            "YWFh"
        ]
    })
    conflictingExamplesBlob: TestBlob

    @shapeExamples({
        allowed: [
            null
            { a: "b" }
            { a: "bb" }
            { a: "bbb" }
        ]
        disallowed: [
            { a: "" }
            { a: "bbbbb" }
            { }
            { a: null }
        ]
    })
    validExamplesStructure: TestStructure

    @shapeExamples({
        allowed: [
            { a: "" }
            { a: "bbbbb" }
            { }
            { a: null }
        ]
        disallowed: [
            null
            { a: "b" }
            { a: "bb" }
            { a: "bbb" }
        ]
    })
    conflictingExamplesStructure: TestStructure

    @shapeExamples({
        allowed: [
            null
            []
            ["a"]
            ["a", "aa"]
            ["a", "aa", "aaa"]
            ["a", "a", "a", "a"]
        ]
        disallowed: [
            [""]
            ["aaaa"]
        ]
    })
    validExamplesList: TestList

    @shapeExamples({
        allowed: [
            []
            [""]
            ["aaaa"]
            ["a", "a", "a", "a"]
        ]
        disallowed: [
            null
            ["a"]
            ["a", "aa"]
            ["a", "aa", "aaa"]
        ]
    })
    conflictingExamplesList: TestList

    @shapeExamples({
        allowed: [
            null
            { "a": "bbb" }
            { "a": "b", "bbb": "aaa" }
        ]
        disallowed: [
            { "": "" }
            { "a": "" }
            { "": "a" }
        ]
    })
    validExamplesMap: TestMap

    @shapeExamples({
        allowed: [
            { "": "" }
            { "a": "" }
            { "": "a" }
        ]
        disallowed:  [
            null
            { "a": "bbb" }
            { "a": "b", "bbb": "aaa" }
        ]
    })
    conflictingExamplesMap: TestMap

    @shapeExamples({
        allowed: [
            null
        ]
    })
    validExamplesBoolean: TestBoolean

    @shapeExamples({
        allowed: [
            null
        ]
    })
    validExamplesTimestamp: Timestamp

    @shapeExamples({
        disallowed: [
            null
        ]
    })
    conflictingExamplesTimestamp: Timestamp

    @shapeExamples({
        allowed: [
            null
            "A"
        ]
        disallowed: [
            "B"
        ]
    })
    validExamplesEnum: TestEnum

    @shapeExamples({
        allowed: [
            "B"
        ]
        disallowed: [
            null
            "A"
        ]
    })
    conflictingExamplesEnum: TestEnum

    @shapeExamples({
        allowed: [
            null
            0
        ]
        disallowed: [
            1
        ]
    })
    validExamplesIntEnum: TestIntEnum

    @shapeExamples({
        allowed: [
            1
        ]
        disallowed: [
            null
            0
        ]
    })
    conflictingExamplesIntEnum: TestIntEnum

    @shapeExamples({
        allowed: [
            null
            {a: "a"}
            {b: 1}
        ]
        disallowed: [
            {}
            {a: ""}
            {b: 4}
        ]
    })
    validExamplesUnion: TestUnion

    @shapeExamples({
        allowed: [
            {}
            {a: ""}
            {b: 4}
        ]
        disallowed: [
            null
            {a: "a"}
            {b: 1}
        ]
    })
    conflictingExamplesUnion: TestUnion

    @shapeExamples({
        allowed: [
            null
            true
            "foo"
            123
            {}
            []
        ]
    })
    validExamplesDocument: Document

    @shapeExamples({
        disallowed: [
            null
            true
            "foo"
            123
            {}
            []
        ]
    })
    conflictingExamplesDocument: Document
}

@range(min: 1, max: 3)
byte TestByte

@range(min: 1, max: 3)
short TestShort

@range(min: 1, max: 3)
integer TestInteger

@range(min: 1, max: 3)
long TestLong

@range(min: 1, max: 3)
float TestFloat

@range(min: 1, max: 3)
double TestExamplesDouble

@range(min: 1, max: 3)
double TestDouble

@range(min: 1, max: 3)
bigInteger TestBigInteger

@range(min: 1, max: 3)
bigDecimal TestBigDecimal

@length(min: 1, max: 3)
@pattern("^a+$")
string TestString

@length(min: 1, max: 3)
blob TestBlob

structure TestStructure {
    @length(min: 1, max: 3)
    @required
    a: String
}

list TestList {
    @length(min: 1, max: 3)
    member: String
}

map TestMap {
    @length(min: 1, max: 3)
    key: String

    @length(min: 1, max: 3)
    value: String
}

boolean TestBoolean

timestamp TestTimestamp

enum TestEnum {
    A
}

intEnum TestIntEnum {
    A = 0
}

union TestUnion {
    a: TestString
    b: TestInteger
}

document TestDocument

@shapeExamples({
    allowed: [
        {a: "a", b: "b"}
    ]
    disallowed: [
        {a: "", b: "b"}
    ]
})
structure WarningGeneratingExamplesStructure {
    @length(min: 1)
    @required
    a: String
}