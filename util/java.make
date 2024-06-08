# SOME overheads to read the file,
# for learning make, remove later!

BUILD_DIR := build

SOURCES = $(wildcard $(DIR)/$(PACKAGE)/*.java)
CLASSES = $(addprefix $(BUILD_DIR)/, $(SOURCES:.java=.class))

default: jlox $(CLASSES)
	@: # Don't show "Nothing to be done" output.

jlox:
	@ echo "#!/usr/bin/bash" > jlox
	@ echo 'script_dir=$$(dirname "$$0")' >> jlox
	@ echo 'java -cp $${script_dir}/build/java lox.Lox $$@' >> jlox
	@ chmod 744 jlox

$(BUILD_DIR)/$(DIR)/%.class: $(DIR)/%.java
	@ mkdir -p $(BUILD_DIR)/$(DIR)
	@ javac -cp $(DIR) -d $(BUILD_DIR)/$(DIR) $<
	@ @ printf "%8s %-60s %s\n" javac $<

.PHONY: default jlox
