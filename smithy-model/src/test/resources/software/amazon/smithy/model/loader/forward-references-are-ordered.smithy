$version: "2"
namespace smithy.example

service Example {
    version: "2006-03-01"
    errors: [
        Error1
        Error2
        Error3
        Error4
        Error5
        Error6
    ]
}

@error("client")
structure Error1 {}

@error("client")
structure Error2 {}

@error("client")
structure Error3 {}

@error("client")
structure Error4 {}

@error("client")
structure Error5 {}

@error("client")
structure Error6 {}
