# SOME overheads to read the file,
# for learning make, remove later!

BUILD_DIR := build

SOURCES = $(wildcard $(DIR)/$(PACKAGE)/*.java)
CLASSES = $(addprefix $(BUILD_DIR)/, $(SOURCES:.java=.class))

CURR_DIR = .

default: jlox generators $(CLASSES)
	@: # Don't show "Nothing to be done" output.

jlox: packages
	@ echo "#!/usr/bin/bash" > jlox
	@ echo 'script_dir=$$(dirname "$$0")' >> jlox
	@ echo 'java -cp $${script_dir}/build/java lox.Lox $$@' >> jlox
	@ chmod 744 jlox

packages:
ifeq (, $(shell which java))
	# $(error "no java in $(PATH), consider doing apt-get install lzop")
	@ echo "Java Development Kit will be installed..."
	@ sudo apt-get install javac
endif

generators: GenerateAst.class
	@ java -cp $(CURR_DIR)/java tool.GenerateAst $(CURR_DIR)/java/lox
	@ rm $(CURR_DIR)/java/tool/GenerateAst.class

GenerateAst.class:
	@ javac $(CURR_DIR)/java/tool/GenerateAst.java

$(BUILD_DIR)/$(DIR)/%.class: $(DIR)/%.java
	@ mkdir -p $(BUILD_DIR)/$(DIR)
	@ javac -cp $(DIR) -d $(BUILD_DIR)/$(DIR) $<
	@ @ printf "%8s %-60s %s\n" javac $<

clean:
	@ echo "Removing previous built..."
	@ rm -rf build
ifeq ("./jlox", $(wildcard ./jlox))
	@ rm jlox
endif

.PHONY: default jlox packages generators clean
