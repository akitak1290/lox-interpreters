# if then else is a shell command

TEST_FOLDERS := scanning parsing evaluating assignment if

all: jlox

jlox:
	@ $(MAKE) -f util/java.make DIR=java PACKAGE=lox

test: $(TEST_FOLDERS)
	
$(TEST_FOLDERS):
	@ echo "--Running tests for $@ function...---"
	@ $(MAKE) --no-print-directory -f util/test.make ROOT=. INTERPRETER=jlox TEST_TYPE=$@	
	@ echo "--Tests for $@ function completed---"

clean:
	@ $(MAKE) -f util/java.make clean

# testsuite1:

.PHONY: jlox test hello
