# Include the shared conf.py file.
shared_config = "../conf.py"
with open(shared_config) as file:
    exec(file.read())

# TODO: Migrate to use the 2.0 tabs library.
extensions.append("sphinx_tabs.tabs")

# Place version specific overrides after here.
html_title = "Smithy 1.0"
release = u'1.0'
version = release

html_theme_options['source_directory'] = "docs/source-1.0"
html_theme_options['announcement'] = '''<strong>⚠️ You are viewing version 1.0.</strong><br/>
Version 2.0 is available at <a href="https://smithy.io/2.0/">https://smithy.io/2.0/</a>.'''
