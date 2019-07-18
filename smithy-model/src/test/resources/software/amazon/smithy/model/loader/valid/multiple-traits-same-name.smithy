namespace foo.alpha

@hello
string MyString1

@foo.alpha#hello
string MyString2

@foo.beta#hello
string MyString3

@foo.gamma#hello
string MyString4

@trait(selector: "*")
structure hello {}


namespace foo.beta

@hello
string MyString1

@foo.alpha#hello
string MyString2

@foo.beta#hello
string MyString3

@foo.gamma#hello
string MyString4

@trait(selector: "*")
structure hello {}


namespace foo.gamma

@hello
string MyString1

@foo.alpha#hello
string MyString2

@foo.beta#hello
string MyString3

@foo.gamma#hello
string MyString4

@trait(selector: "*")
structure hello {}
