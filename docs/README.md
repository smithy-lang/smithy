# Smithy specification

This package is the Smithy website, specification, and project documentation.


## Building

The Smithy docs are built using [Sphinx](https://www.sphinx-doc.org/en/master),
which requires python.

Once you have python installed, you need to install the dependencies and local
modules:

```
make install
```

Then to build static output, run:

```
make html
```

To view the output, just open up `build/html/index.html` in your browser:

```
open build/html/index.html
```

## Live Editing

If you want to make modifications without having to manually build each time,
you can run a local web server that will auto-build on changes. First, install
the dependencies:

```
make install-server
```

And then run the server:

```
make serve
```
