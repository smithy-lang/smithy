$version: "2.0"

namespace smithy.example

service Example {
    version: "1.0.0"
    operations: [
        ChangeCard
    ]
}

operation ChangeCard {
    input: Card
    output: Card
}

structure Card {
    suit: Suit
}

enum Suit {
    DIAMOND
    CLUB
    HEART
    SPADE
}
