# Overview
This document describes how to test and lint the ``install`` scripts used
to install the Smithy CLI distributables.

These installers get packaged with the respective distributable (`install` for linux/mac, `install.bat` for windows).

## Shell installer (`install`)

### Running the tests
The test suite utilizes [`bats-core`](https://github.com/bats-core/bats-core) to run
the tests. To run the tests, first make sure you first install `bats-core`:

     $ brew install bats-core


And then run bats-core from within this `config` directory:

     $ bats .


### Linting
To help catch potential issues in the shell scripts, you should lint the scripts.
To lint the shell scripts, use [`shellcheck`](https://github.com/koalaman/shellcheck).
It can be installed with `brew`:

     $ brew install shellcheck


Then can be ran on both the ``install`` shell script and the ``install.bats``
test file::

    $ shellcheck install install.bats


## Batch installer (`install.bat`)
Unfortunately, it's not exactly straightforward to test batch (`.bat`) files on linux or mac.

If you do find yourself on a windows machine, you can bootstrap an test installation environment
by creating a folder, which contains `install.bat` and a dummy executable, `bin/smithy`.

Running the installer will perform the installation as if you were installing the real
distributable (which is exactly what `bats` does above).
