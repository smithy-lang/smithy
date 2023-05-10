// Syntax error at line 6, column 12: Expected COLON(':') but found EQUAL('=')
$version: "2.0"
namespace smithy.example

operation MyOperation {
    errors = [
        := {}
    ]
}
