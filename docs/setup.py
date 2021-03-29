#!/usr/bin/env python

from setuptools import setup, find_packages

requires = ["sphinx>=1.7.0,<1.8.0", "pygments==2.7.4", "sphinx-tabs==1.1.7"]

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
