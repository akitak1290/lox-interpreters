# SOME overheads to read the file,
# for learning make, remove later!

BUILD_DIR := build

SOURCES = $(wildcard $(DIR)/$(PACKAGE)/*.java)
CLASSES = $(addprefix $(BUILD_DIR)/, $(SOURCES:.java=.class))

default: $(CLASSES)
	@: # Don't show "Nothing to be done" output.

$(BUILD_DIR)/$(DIR)/%.class: $(DIR)/%.java
	@ mkdir -p $(BUILD_DIR)/$(DIR)
	@ javac -cp $(DIR) -d $(BUILD_DIR)/$(DIR) $<
	@ @ printf "%8s %-60s %s\n" javac $<

.PHONY: default
