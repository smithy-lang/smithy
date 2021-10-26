#!/usr/bin/env python

from setuptools import setup, find_packages

requires = ["sphinx==4.2.0", "pygments==2.10.0", "sphinx-tabs==3.2.0"]

# Register the custom Smithy loader with Pygments.
# See: http://pygments.org/docs/plugins/
setup(
    name="smithy",
    packages=find_packages(),
    install_requires=requires,
    entry_points="""
    [pygments.lexers]
    smithy = smithy.lexer:SmithyLexer
    """,
)
