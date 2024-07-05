# Need to pass in ROOT, INTERPRETER, and TEST_TYPE
# Currently mixing in bash commands, might get confusing

TEST_DIR := $(ROOT)/test/$(TEST_TYPE)
TEST_FILES := $(wildcard $(TEST_DIR)/*.test.lox)
ACTION_LIST := $(patsubst $(TEST_DIR)/%.test.lox, $(TEST_DIR)/%, $(TEST_FILES))

test: $(ACTION_LIST)

# Some tests need to exit with an error
# For those, we compare the error with .result
# and tell make to continue running with '-'
$(ACTION_LIST):
	-@ $(ROOT)/$(INTERPRETER) $@.test.lox --debug $(TEST_TYPE) >$@.tmp 2>&1
	@ if diff -q $@.result.lox $@.tmp > /dev/null; then \
		echo "Test $@: PASSED"; \
		rm $@.tmp; \
	else \
		echo "Test $@: FAILED"; \
		diff $@.result.lox $@.tmp; \
		rm $@.tmp; \
	fi

.PHONY: test $(ACTION_LIST)
