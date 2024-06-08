# some hacky stuff
TARGET := "$@"

jlox:
	@ echo "#!/usr/bin/bash" > jlox
	@ echo 'script_dir=$$(dirname "$$0")' >> jlox
	@ echo 'java -cp $${script_dir}/build/java lox.Lox $$@' >> jlox
	@ echo 'Done'

.Phony: jlox
