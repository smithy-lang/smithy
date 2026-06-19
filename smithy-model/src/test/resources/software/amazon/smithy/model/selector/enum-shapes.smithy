$version: "2.0"

namespace smithy.example

/// Enum shapes carry the synthetic enum trait internally rather than smithy.api#enum, so they exercise the
/// trait redirect that [trait|enum] relies on.
enum Suit {
    DIAMOND
    CLUB
    HEART
    SPADE
}

string PlainString
