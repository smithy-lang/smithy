#!/usr/bin/env python

from setuptools import setup, find_packages

requires = [
    "sphinx>=5.0.0",
    "pygments==2.15.0",
    "sphinx_copybutton==0.5.0",
    # Used by new docs.
    "sphinx-inline-tabs==2022.1.2b11",
    # Used by old docs.
    "sphinx-tabs>=3.4.4"
]

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
