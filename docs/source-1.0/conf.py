shared_config = "../conf.py"
exec(compile(source=open(shared_config).read(), filename=shared_config, mode='exec'))

# Place any version specific overrides here.
release = u'1.0'
version = release

html_theme_options['announcement'] = '''<strong>⚠️ You are viewing version 1.0.</strong><br/>
Version 2.0 is available at <a href="https://awslabs.github.io/smithy/2.0/">https://awslabs.github.io/smithy/2.0/</a>.'''
