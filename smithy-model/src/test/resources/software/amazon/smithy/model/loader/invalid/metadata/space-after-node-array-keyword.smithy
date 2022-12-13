// Syntactic shape ID `truefalse` does not resolve to a valid shape ID: `smithy.api#truefalse`
$version: "2.0"
metadata foo = [truefalse]

// Since NodeValue has keywords and those keywords are used to resolve
// ambiguity in the grammar, this failing with a syntactic shape ID error
// could be considered correct because "truefalse" is not a valid shape ID
// in the smithy.api# namespace and does not exactly match the true or false
// keywords.
