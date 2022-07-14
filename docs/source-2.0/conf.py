shared_config = "../conf.py"
exec(compile(source=open(shared_config).read(), filename=shared_config, mode='exec'))

# Place any version specific overrides here.
release = u'2.0'
version = release
