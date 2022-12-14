# Include the shared conf.py file.
shared_config = "../conf.py"
with open(shared_config) as file:
    exec(file.read())

# TODO: Migrate the 1.0 docs to use this, and move this to the main conf.py.
extensions.append('sphinx_inline_tabs')

# Place version specific overrides after here.
html_title = "Smithy 2.0"
release = u'2.0'
version = release

html_theme_options['source_directory'] = "docs/source-2.0"
