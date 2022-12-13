$version: "2.0"
namespace smithy.example

enum A { FOO BAR BAZ }
enum B { FOO, BAR, BAZ // Test
       }

enum C { FOO }

enum D { FOO
         /// BAR
         BAR = "bar"
       }
