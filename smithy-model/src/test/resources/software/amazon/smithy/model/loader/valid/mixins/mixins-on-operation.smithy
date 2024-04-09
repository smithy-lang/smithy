$version: "2.0"

namespace com.amazon.example

@readonly
@http(code: 200, method: "GET", uri: "/test")
operation ListFoos {
    input := with [MyMixin] {}
    output:= with [MyMixin] {}
}

@mixin
structure MyMixin {}
