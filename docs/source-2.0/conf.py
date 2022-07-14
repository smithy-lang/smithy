# Include the shared conf.py file.
shared_config = "../conf.py"
with open(shared_config) as file:
    exec(file.read())

# Place version specific overrides after here.
release = u'2.0'
version = release
