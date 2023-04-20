// Syntax error at line 8, column 9: Expected one of COLON(':'), WALRUS(':='); but found DOC_COMMENT('/// These are invalid because they come before the walrus\n')
$version: "2.0"

namespace smithy.example

operation MyOperation {
    output
        /// These are invalid because they come before the walrus
        /// operator.
        := {}
}
