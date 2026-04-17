$version: "2.1"

namespace smithy.example

enum Suit {
    CLUB
    DIAMOND
    HEART
    SPADE
}

enum SuitWithValues {
    @enumValue("clubs")
    CLUB

    @enumValue("diamonds")
    DIAMOND
}

intEnum Priority {
    LOW = 1
    MEDIUM = 2
    HIGH = 3
}
