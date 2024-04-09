---------------
Generating Code
---------------

A "code writer" is the main abstraction used to generate code. It can be
used to write basically any kind of code, including whitespace sensitive
and brace-based. The following example generates some Python code:

.. code-block:: java

    SimpleCodeWriter writer = new SimpleCodeWriter();

    writer.write("def Foo(str):")
          .indent()
          .write("print str");

    String code = writer.toString();

There are few kinds of code writers:

- `AbstractCodeWriter`_: An abstract class that can be extended to
  create language-specific writers.
- `SimpleCodeWriter`_: An implementation of ``AbstractCodeWriter`` with no
  added methods or support for ``Symbol``\ s.
- `SymbolWriter`_: An abstract class that extends ``AbstractCodeWriter``,
  available in `software.amazon.smithy:smithy-codegen-core <https://search.maven.org/artifact/software.amazon.smithy/smithy-codegen-core>`__.
  This class adds abstractions for managing imports and dependencies.
  Smithy code generators should extend this class.
  See :doc:`decoupling-codegen-with-symbols`.


``AbstractCodeWriter`` is a lightweight template engine
=======================================================

An ``AbstractCodeWriter`` can be used as a lightweight templating
language. It supports interpolation, formatting,
:ref:`intercepting named sections of the generated content <codegen-intercepting>`,
conditionals, and loops. This removes the need to add a dependency on a Java
templating engine and the need to integrate Smithy Symbols and dependency
management into other templating languages. The following example uses Java 17
text blocks to generate a contiguous section of code:

.. code-block:: java

    writer.pushState();

    // Add variables that can be referenced in templates.
    writer.putContext("name", settings.getModuleName());
    writer.putContext("version", settings.getModuleVersion());
    writer.putContext("description", settings.getModuleDescription());

    writer.write("""
        [flake8]
        # We ignore E203, E501 for this project due to black
        ignore = E203,E501

        [metadata]
        name = ${name:L}
        version = ${version:L}
        description = ${description:L}
        license = Apache-2.0
        python_requires = >=3.10
        classifiers =
            Development Status :: 2 - Pre-Alpha
            Intended Audience :: Developers
            Intended Audience :: System Administrators
            Natural Language :: English
            License :: OSI Approved :: Apache Software License
            Programming Language :: Python
            Programming Language :: Python :: 3
            Programming Language :: Python :: 3 :: Only
            Programming Language :: Python :: 3.10
        """);

    writer.popState();


Interpolation
=============

Various methods like ``write()`` and ``writeInline()`` take a template
string and a variadic list of arguments that are *interpolated*, or
replaced, into the expression.

In the following example, ``$L`` is interpolated and replaced with the
relative argument, ``"there!"``.

.. code-block:: java

    CodeWriter writer = new SimpleCodeWriter();
    writer.write("Hello, $L", "there!");
    assert(writer.toString().equals("Hello, there!\n"));

The ``$`` character is escaped using ``$$``.

.. code-block:: java

    SimpleCodeWriter writer = new SimpleCodeWriter().write("$$L");
    assert(writer.toString().equals("$L\n"));

The default character used to start an expression is ``$``, but this can
be changed for the current state of the ``AbstractCodeWriter`` by
calling ``setExpressionStart(char)``. This might be useful for
programming languages that make heavy use of ``$`` like PHP or Kotlin. A
custom start character can be escaped using two start characters in a
row. For example, given a custom start character of ``#``, ``#`` can be
escaped using ``##``.

.. code-block:: java

    SimpleCodeWriter writer = new SimpleCodeWriter();
    writer.setExpressionStart('#');
    writer.write("#L ##L $L", "hi");
    assert(writer.toString().equals("hi #L $L\n"));


Formatters
==========

An ``AbstractCodeWriter`` supports three kinds of interpolations:
relative, positional, and named. Each of these kinds of interpolations
pass a value to a *formatter*. Formatters are named functions that
accept an object as input, accepts a string that contains the current
indentation (it can be ignored if not useful), and returns a string as
output. ``AbstractCodeWriter`` registers two built-in formatters:

- ``L`` (literal): Outputs a literal value of an ``Object`` using the
  following implementation: (1) A null value is formatted as "". (2) An
  empty ``Optional`` value is formatted as "". (3) A non-empty
  ``Optional`` value is formatted using the value inside the
  ``Optional``. (3) All other values are formatted using the result of
  calling Java's ``String#valueOf``.
- ``S`` (string): Adds double quotes around the result of formatting a
  value first using the default literal "L" implementation described
  above and then wrapping the value in an escaped string safe for use
  in Java according to
  https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6.
  This should work for many programming languages, but this formatter
  can be overridden if needed.
- ``C`` (call): Used to break up a template and execute code at
  specific locations. ``$C`` stands for "call" and is used to run a
  ``Runnable`` or ``Consumer<AbstractCodeWriter>`` that is expected to
  write to the same writer. Any text written to the writer is used as
  the interpolation result. Note that a single trailing newline is
  removed from the captured text. If a ``Runnable`` is provided, it is
  required to have a reference to the writer. A ``Consumer`` is
  provided a reference to the writer as a single argument. Using a
  ``Consumer`` makes it possible to create more generic methods for
  handling different sections of code.
- …: Custom formatters can be registered using
  ``AbstractCodeWriter#putFormatter``. Registering custom formatters
  with a writer for common formatting tasks is a great way to simplify
  a code generator.


Relative parameters
===================

Placeholders in the form of "$" followed by a formatter name are treated
as relative parameters. The first instance of a relative parameter
interpolates the first positional argument, the second, etc. All
relative arguments must be used as part of an expression and relative
interpolation cannot be mixed with positional variables.

.. code-block:: java

    SimpleCodeWriter writer = new SimpleCodeWriter();
    writer.write("$L $L $L", "a", "b", "c");
    assert(writer.toString().equals("a b c\n"));


Positional parameters
=====================

Placeholders in the form of "$" followed by a positive number, followed
by a formatter name are treated as positional parameters. The number
refers to the 1-based index of the argument to interpolate. All
positional arguments must be used as part of an expression and relative
interpolation cannot be mixed with positional variables.

.. code-block:: java

    SimpleCodeWriter writer = new SimpleCodeWriter();
    writer.write("$1L $2L $3L, $3L $2L $1L", "a", "b", "c");
    assert(writer.toString().equals("a b c c b a\n"));


Named parameters
================

Named parameters are parameters that take a value from the context of
the current state. They take the following form
``$<variable>:<formatter>``, where ``<variable>`` is a string that
starts with a lowercase letter, followed by any number of
``[A-Za-z0-9_#$.]`` characters, and ``<formatter>`` is the name of a
formatter.

.. code-block:: java

    SimpleCodeWriter writer = new SimpleCodeWriter();
    writer.pushState();
    writer.putContext("foo", "a");
    writer.putContext("bar", "b");
    writer.write("$foo:L $bar:L");
    writer.popState();
    assert(writer.toString().equals("a b\n"));


.. _inline-block-alignment:

Inline block alignment
======================

Sometimes it's necessary to maintain the exact indentation level of an
interpolated property even if newlines are written when interpolating.
For example, say we wanted to indent a variable list of names,
``Bob\nKaren\nLuis``, like this:

.. code-block:: none

    Names: Bob
           Karen
           Luis

Using normal ``$L`` expansion:

.. code-block:: java

    writer.write("$L: $L", "Names", "Bob\nKaren\nLuis");

``$L`` does not preserve the desired indentation, resulting in:

.. code-block:: none

    Names: Bob
    Karen
    Luis

Indentation can be preserved to match the desired list from the first
example by using the inline block alignment operator (that is, putting
``|`` before the closing brace):

.. code-block:: java

    writer.write("$L: ${L|}", "Names", "Bob\nKaren\nLuis");

If all the characters on the line in the template leading up to the
interpolation are spaces or tabs, then those characters are applied
before each new line. This means that block alignment works even with
tab-based languages:

.. code-block:: java

    writer.write("""
        if (true) {
        \t\t${C|}
        }
        """,
       writer.call(w -> w.write("Hi\nHello"))
    );

Outputs:

.. code-block:: none

    if (true) {
    \t\tHi
    \t\tHello
    }


Breaking up large templates with the ``$C`` formatter
=====================================================

The ``$C`` formatter can be used to break up large codegen templates
without losing the readability benefits of `Java text blocks`_.
The ``$C`` formatter pairs well with inline block alignment, allowing
you to generate indented sections of code within a larger template.

The following example uses the ``call`` method of an
``AbstractCodeWriter`` to properly type the ``Function``, and a method
reference is provided to invoke a method that accepts the writer.

.. code-block:: java

    void someMethod() {
        writer.write("""
            if (true) {
                ${C|}
            } else {
                ${C|}
            }
            """,
            writer.call(this::handleTrue),
            writer.call(this::handleFalse));
    }

    void handleTrue(CodeWriter writer) {
        writer.write("True!");
    }

    void handleFalse(CodeWriter writer) {
        writer.write("False!");
    }

.. tip::

    When generating code, try to show the overall structure of the
    code that will be generated as much as possible in larger blocks of
    templated text that leverage ``${C|}``, template conditionals (e.g.,
    ``${?foo}${/foo}``), and template loops (e.g., ``${#foo}${/foo}``).


Pushing and popping states
==========================

``AbstractCodeWriter`` maintains a stack of transformation states,
including the text used to indent, a prefix to add before each line,
newline character, the number of times to indent, a map of context
values, whether whitespace is trimmed from the end of newlines, whether
the automatic insertion of newlines is disabled, the character used to
start code expressions (defaults to ``$``), and formatters.

State can be pushed onto the stack using ``pushState`` which copies the
current state. Mutations can then be made to the top-most state of the
``AbstractCodeWriter`` and do not affect previous states. The previous
transformation state of the ``AbstractCodeWriter`` can later be restored
using ``popState``.

.. code-block:: java

    SimpleCodeWriter writer = new SimpleCodeWriter();
    writer
        .pushState()
        .write("/**")
        .setNewlinePrefix(" * ")
        .write("This is some docs.")
        .write("And more docs.\n\n\n")
        .write("Foo.")
        .popState()
        .write(" */");

The above example outputs:

.. code-block:: none

   /**
    * This is some docs.
    * And more docs.
    *
    * Foo.
    */

``AbstractCodeWriter`` maintains some global state that is not affected
by ``pushState()`` and ``popState()``:

-  The number of successive blank lines to trim.
-  Whether a trailing newline is inserted or removed from the result of
   converting the ``AbstractCodeWriter`` to a string.


Limiting blank lines
====================

Many coding standards recommend limiting the number of successive blank
lines. This can be handled automatically by ``AbstractCodeWriter`` by
calling ``trimBlankLines()``. The removal of blank lines is handled when
the ``AbstractCodeWriter`` is converted to a string. Lines that consist
solely of spaces or tabs are considered blank. If the number of blank
lines exceeds the allowed threshold, they are omitted from the result.

.. code-block:: java

    SimpleCodeWriter writer = new SimpleCodeWriter();
    writer.trimBlankLines();
    writer.write("hello\n\n\n\nhello");
    assert(writer.toString().equals("hello\n\nhello\n"));

In the above example, ``\n\n\n\n`` results in two blank lines (two
newlines outputs an entirely blank line). ``AbstractCodeWriter`` trims
the successive blank line, resulting in ``"hello\n\nhello\n"`` (the
trailing newline is added by ``AbstractCodeWriter`` by default
separately). Two blank lines could be allowed if the above example was
updated to pass ``2`` into ``trimBlankLines``:

.. code-block:: java

    writer.trimBlankLines(2);


Trimming trailing spaces
========================

Many coding standards do not allow trailing spaces on lines. Trailing
spaces can be automatically trimmed from each line by calling
``trimTrailingSpaces()``.

.. code-block:: java

    SimpleCodeWriter writer = new SimpleCodeWriter();
    writer.trimTrailingSpaces();
    writer.write("hello  ");
    assert(writer.toString().equals("hello"));


Code sections
=============

Named sections can be marked in the code writer that can be intercepted
and modified by *section interceptors*. This gives the
``AbstractCodeWriter`` an extension system for augmenting generated
code. A section of code can be captured using a *block section* or an
*inline section*.


Block sections
--------------

The primary method for creating sections of code is block sections. A
block section is created by passing a string or an implementation of
``CodeSection`` to ``pushState()``. A string gives the state a name and
captures all the output written inside this state to an internal buffer.
This buffer is then passed to each registered interceptor for that name.
These interceptors can choose to use the default contents of the section
or emit entirely different content.

.. code-block:: java

    SimpleCodeWriter writer = new SimpleCodeWriter();

    writer.onSection("example", text -> {
        writer.write("Intercepted: " + text);
    });

    writer.pushState("example");
    writer.write("Original contents");
    writer.popState();
    assert(writer.toString().equals("Intercepted: Original contents\n"));

A better method for creating and intercepting code sections is to use an
instance of a ``CodeSection``. A ``CodeSection`` is a simple interface
that is just required to return the name of the ``CodeSection`` (in
fact, using a string for ``pushState`` internally creates a
``CodeSection``).

Java records are an easy way to implement ``CodeSection``\ s:

.. code-block:: java

    record NameEvent(String sectionName, String person) implements CodeSection;

``CodeInterceptor``\ s can be registered to intercept sections by class.
A simple way to create one-off interceptors is using
``CodeSection#appender``:

.. code-block:: java

    writer.onSection(CodeInterceptor.appender(NameEvent.class, (w, section) -> {
        w.write("$L", section.sectionName()));
    }));

    writer.onSection(CodeInterceptor.appender(NameEvent.class, (w, section) -> {
        w.writeInline("Who? ");
    }));

    writer.onSection(CodeInterceptor.appender(NameEvent.class, (w, section) -> {
        w.write("$L!", section.person());
    }));

When a ``CodeSection`` is given to ``pushState`` or ``injectState``,
``CodeInterceptor``\ s are applied in the order they were registered.

.. code-block:: java

    NameEvent event = new NameEvent("Zak", "McKracken");
    writer.injectSection(event);

When applied, the ``writer`` contains the following output:

.. code-block:: none

    Zak
    Who? McKracken!


Inline sections
---------------

An *inline section* is created using a special ``AbstractCodeWriter``
interpolation format that appends "@" followed by the section name.
Inline sections function just like block sections, but they can
appear inline inside other content passed in calls to
``AbstractCodeWriter#write()``.

Inline sections are created in a format string inside braced arguments
after the formatter. For example, ``${L@foo}`` is an inline section that
uses the literal "L" value of a relative argument as the default value
of the section and allows interceptors registered for the "foo" section
to make calls to the ``AbstractCodeWriter`` to modify the section.

.. code-block:: java

    SimpleCodeWriter writer = new SimpleCodeWriter();

    // Add an intercept for the "example" section.
    writer.onSection("example", text -> writer.write("Intercepted: " + text));

    // Write to the writer and define an inline "example" section.
    // If nothing intercepts this section, "foo" is written to it.
    writer.write("Leading...${L@example}...Trailing...", "foo");

    assert(writer.toString().equals("Leading...Intercepted: foo...Trailing...\n"));

.. note::

    An inline section that makes no calls to ``AbstractCodeWriter#write()``
    expands to an empty string.


Template conditions and loops
=============================

Conditional blocks can be defined in code writer templates using the
following syntax:

.. code-block:: java

    writer.write("""
        ${?foo}
        Foo is set: ${foo:L}
        ${/foo}""");

Assuming ``foo`` is *truthy* and set to "hi", then the above template
outputs: "Foo is set: hi" In the above example, "?" indicates that the
expression is a conditional block to check if the named context property
"foo" is truthy. If it is, then the contents of the block up to the
matching closing block, ``${/foo}``, are evaluated. If the condition is
not satisfied, then contents of the block are skipped.

You can check if a named context property is *falsey* using "^":

.. code-block:: java

    writer.write("""
        ${^foo}
        Foo is not set
        ${/foo}""");

Assuming ``foo`` is set to "hi", then the above template outputs
nothing. If ``foo`` is falsey, then the above template outputs "Foo is
not set".


Truthy and falsey values
------------------------

The following values are considered falsey:

-  properties that are not found
-  null values
-  false
-  empty `String <https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/String.html>`__
-  empty `Iterable <https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Iterable.html>`__
-  empty `Map <https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Map.html>`__
-  empty `Optional <https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Optional.html>`__

Values that are not falsey are considered truthy.


Loops
-----

Loops can be created to repeat a section of a template for each value
stored in a list or each key value pair stored in a map. Loops are
created using ``#``.

The following template with a "foo" value of
``{"key1": "a", "key2": "b", "key3": "c"}``:

.. code-block:: java

    writer.write("""
        ${#foo}
        - ${key:L}: ${value:L} (first: ${key.first:L}, last: ${key.last:L})
        ${/foo}
        """);

Evaluates to:

.. code-block:: none

    - key1: a (first: true, last: false)
    - key2: b (first: false, last: false)
    - key3: c (first: false, last: true)

Each iteration of the loop pushes a new state in the writer that sets
the following context properties:

-  ``key``: contains the current 0-based index of an iterator or the
   current key of a map entry
-  ``value``: contains the current value of an iterator or current value
   of a map entry
-  ``key.first``: set to true if the loop is on the first iteration
-  ``key.false``: set to true if the loop is on the last iteration

A custom variable name can be used in loop variable bindings. For
example:

.. code-block:: java

    writer.write("""
        ${#foo as key1, value1}
        - ${key1:L}: ${value1:L} (first: ${key1.first:L}, last: ${key1.last:L})
        ${/foo}""");


Whitespace control
------------------

Conditional blocks and loop blocks that occur on lines that only contain
whitespace are not written to the template output. For example, if
``foo`` in the following template is falsey, then the template expands
to an empty string:

.. code-block:: java

    writer.write("""
        ${?foo}
        Foo is set: ${foo:L}
        ${/foo}""");

Whitespace that comes before a template expression can be removed by
putting ``-`` at the beginning of the expression.

Assuming that the first positional argument is "hi":

.. code-block:: java

    writer.write("""
        Greeting:
            ${-L}""");

Expands to:

.. code-block:: none

    Greeting:hi\n

Whitespace that comes after a template expression can be removed by
adding ``-`` to the end of the expression:

.. code-block:: java

    writer.write("""
        ${L-}

        .""");

Expands to:

.. code-block:: none

    hi.\n\n

Leading whitespace cannot be removed when using
:ref:`inline block alignment <inline-block-alignment>`
(``|``). The following is *invalid*:

.. code-block:: java

    writer.write("${-C|}");
                 // ^ ^ invalid combination


.. _AbstractCodeWriter: https://github.com/smithy-lang/smithy/blob/main/smithy-utils/src/main/java/software/amazon/smithy/utils/AbstractCodeWriter.java
.. _SimpleCodeWriter: https://github.com/smithy-lang/smithy/blob/main/smithy-utils/src/main/java/software/amazon/smithy/utils/SimpleCodeWriter.java
.. _SymbolWriter: https://github.com/smithy-lang/smithy/blob/main/smithy-codegen-core/src/main/java/software/amazon/smithy/codegen/core/SymbolWriter.java
.. _Java text blocks: https://docs.oracle.com/en/java/javase/13/text_blocks/index.html
