#!/usr/bin/env python

from setuptools import setup, find_packages

requires = ['sphinx==1.7.5',
            'pygments==2.2.0',
            'sphinx-tabs==1.1.7']

# Register the custom Smithy loader with Pygments.
# See: http://pygments.org/docs/plugins/
setup (
  name='smithy',
  packages=find_packages(),
  entry_points =
  """
  [pygments.lexers]
  smithy = smithylexer:SmithyLexer
  """,
)
