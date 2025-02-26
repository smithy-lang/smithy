# Smithy specification

This package is the Smithy website, specification, and project documentation.


## Building

The Smithy docs are built using [Sphinx](https://www.sphinx-doc.org/en/master),
which requires python3.

Once you have python3 installed, you need to install the dependencies and local
modules:

```
make install
```

Then to build static output, run:

```
make html
```

To view the output, run:

```
make serve
```
