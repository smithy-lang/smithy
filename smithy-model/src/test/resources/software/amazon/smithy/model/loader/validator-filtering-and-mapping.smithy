metadata validators = [
  { // Picks up two shapes
    name: "hello",
    selector: ":test(string, integer)"
  },
  { // Picks up no shapes
    name: "hello",
    namespaces: ["not.smithy.example"]
  },
  { // Picks up 4 shapes
    name: "hello",
    id: "customHello",
    message: "Test {super}"
  },
]

namespace smithy.example

string MyString

integer MyList

boolean MyBoolean

long MyLong
