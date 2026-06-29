$version: "2.1"

namespace smithy.example

@pattern(#re """
    ^\d{5}(-\d{4})?$
    """)
string ZipCode

structure CborExample {
    @default(#hex """
        89 50 4e 47 0d 0a 1a 0a
        """)
    pngHeader: Blob
}
