$version: "2.0"
namespace com.example

map A {
    key: smithy.api#String,
    value: smithy.api#String
}
map B{key:String, value:String}

@deprecated
map C {
    key: String,
    value: String
}

@deprecated
@since("1.0")
map D { key : String ,value: String }

@deprecated
@since("1.0")
map E {
    @internal @since("1.1") key: String ,
    value:String, // trailing comma
}

@deprecated @since("1.0")
map F {
    @internal
    @since("1.1")
    key: String,

    @internal
    @since("1.1")
    value: String
}

@deprecated @since("1.0")
map G {
    @internal
    @since("1.1")
    key: String
,
    @since("1.2")
    value: String
}

string String

map H{key:String value:String}

map I {
    key:String
    value:String
}

map J {key:String,,,value:String}
