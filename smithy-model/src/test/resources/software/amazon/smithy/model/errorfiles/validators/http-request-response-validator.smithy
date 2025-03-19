$version: "2.0"

namespace ns.foo

service MyService {
    version: "2017-01-17"
    operations: [
        A
        B
        C
        D
        E
        F
        G
        H
        J
        K
        L
        ListPayload
        M
        MapPayload
        N
        O
        P
        Q
        QueryParams
        R
        SetPayload
    ]
}

@http(method: "GET", uri: "/A/{foo}", code: 200)
@readonly
operation A {
    input: Unit
    output: Unit
}

@http(method: "GET", uri: "/b/{d}", code: 200)
@readonly
operation B {
    input: BInput
    output: BOutput
}

@http(method: "PUT", uri: "/c/{a}", code: 200)
@idempotent
operation C {
    input: CInput
    output: COutput
}

@http(method: "GET", uri: "/d", code: 200)
@readonly
operation D {
    input: DInput
    output: DOutput
}

@http(method: "GET", uri: "/e/{label1}/{label2}")
@readonly
operation E {
    input: EInput
    output: Unit
}

@http(method: "PUT", uri: "/f", code: 201)
@idempotent
operation F {
    input: FInputOutput
    output: FInputOutput
}

@http(method: "GET", uri: "/g/{a}", code: 200)
@readonly
operation G {
    input: GInput
    output: GOutput
}

@http(method: "GET", uri: "/g")
@readonly
operation H {
    input: HInput
    output: Unit
}

@http(method: "GET", uri: "/j")
@readonly
operation J {
    input: JInput
    output: Unit
}

@http(method: "GET", uri: "/k")
@readonly
operation K {
    input: KInput
    output: Unit
}

@http(method: "GET", uri: "/k")
@readonly
operation L {
    input: Unit
    output: Unit
}

@http(method: "POST", uri: "/list-payload")
operation ListPayload {
    input: ListPayloadInputOutput
    output: ListPayloadInputOutput
}

@http(method: "PUT", uri: "/m")
@idempotent
operation M {
    input: MInput
    output: Unit
}

@http(method: "POST", uri: "/map-payload")
operation MapPayload {
    input: MapPayloadInputOutput
    output: MapPayloadInputOutput
}

@http(method: "GET", uri: "/n")
@readonly
operation N {
    input: NInput
    output: Unit
}

@http(method: "GET", uri: "/o/{a}")
@readonly
operation O {
    input: OInput
    output: Unit
}

@http(method: "GET", uri: "/p/{a+}")
@readonly
operation P {
    input: PInput
    output: Unit
}

@http(method: "GET", uri: "/q")
@readonly
operation Q {
    input: QInput
    output: Unit
}

@http(method: "POST", uri: "/query-params")
operation QueryParams {
    input: QueryParamsInput
    output: QueryParamsOutput
}

@http(method: "GET", uri: "/r")
@readonly
operation R {
    input := {
        @httpPrefixHeaders("")
        a: MapOfString

        @httpHeader("X-Foo")
        b: String
    }
    output := {
        @httpPrefixHeaders("")
        a: MapOfString

        @httpHeader("X-Foo")
        b: String
    }
}

@http(method: "POST", uri: "/set-payload")
operation SetPayload {
    input: SetPayloadInputOutput
    output: SetPayloadInputOutput
}

@httpError(404)
structure BadError {}

@error("client")
structure BadErrorMultipleBindings {
    @httpHeader("X-Foo")
    @httpPayload
    foo: String
}

structure BInput {}

structure BOutput {}

structure CInput {
    @httpLabel
    @required
    a: String

    @httpHeader("X-B")
    b: String

    @httpPrefixHeaders("X-C-")
    c: MapOfString

    @httpQuery("d")
    d: String

    @httpPayload
    e: Blob

    @httpHeader("X-Bb")
    otherHeader: String

    @httpQuery("otherQuery")
    otherQuery: String

    @httpHeader("X-Plural")
    headerList: StringList

    @httpQuery("queryList")
    queryList: StringList
}

structure COutput {
    @httpHeader("X-B")
    a: String

    @httpPrefixHeaders("X-B-")
    b: MapOfString

    @httpPayload
    c: Blob
}

structure DInput {
    @httpHeader("X-Foo")
    a: String

    @httpHeader("X-Foo")
    b: String

    @httpHeader("X-Baz")
    c: String

    @httpHeader("X-Baz")
    d: String

    @httpPrefixHeaders("X-Foo")
    e: MapOfString
}

structure DOutput {
    @httpHeader("X-Foo")
    a: String

    @httpHeader("X-Foo")
    b: String

    @httpPrefixHeaders("X-Foo")
    c: MapOfString
}

structure EInput {
    @httpLabel
    @required
    label1: String

    @httpLabel
    @required
    label2: MapOfString
}

structure FInputOutput {
    foo: String
}

structure GInput {
    @httpHeader("X-Foo")
    @httpLabel
    @httpPayload
    @httpQuery("a")
    @required
    a: String

    @httpHeader("Map-")
    @httpPrefixHeaders("X-C-")
    b: MapOfString
}

structure GOutput {
    @httpHeader("X-B")
    @httpPayload
    a: String

    @httpHeader("Map-")
    @httpPrefixHeaders("X-B-")
    b: MapOfString
}

structure HInput {
    @httpHeader("X-Foo")
    a: Structure
}

structure JInput {
    @httpPrefixHeaders("X-Foo-")
    a: MapOfString

    @httpPrefixHeaders("X-Baz-")
    b: MapOfString
}

structure KInput {
    @httpHeader("X-Foo")
    a: String

    @httpHeader("x-foo")
    b: String

    @httpQuery("foo")
    c: String

    @httpQuery("foo")
    d: String
}

structure ListPayloadInputOutput {
    @httpPayload
    listPayload: StringList
}

structure MapPayloadInputOutput {
    @httpPayload
    mapPayload: MapOfString
}

structure MInput {
    @httpHeader("Authorization")
    a: String
}

structure NInput {
    @httpLabel
    @required
    a: String
}

structure OInput {
    @httpLabel
    a: String
}

structure PInput {
    @httpLabel
    @required
    a: Integer
}

structure QInput {
    @httpPrefixHeaders("")
    a: MapOfString
}

structure QueryParamsInput {
    @httpQuery("named")
    namedQuery: String

    @httpQuery("otherNamed")
    otherNamedQuery: String

    @httpQueryParams
    queryParams: MapOfString
}

structure QueryParamsOutput {
    foo: String
}

structure SetPayloadInputOutput {
    @httpPayload
    setPayload: StringSet
}

structure Structure {}

list StringList {
    member: String
}

@uniqueItems
list StringSet {
    member: String
}

map MapOfString {
    key: String
    value: String
}

blob Blob

integer Integer

string String
