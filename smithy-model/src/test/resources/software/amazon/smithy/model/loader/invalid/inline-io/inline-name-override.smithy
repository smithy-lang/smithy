// Syntax error at line 6, column 14: Expected LBRACE('{') but found IDENTIFIER('CustomName') | Model
$version: "2.0"
namespace smithy.example

operation MyOperation {
    input := CustomName {}
}
