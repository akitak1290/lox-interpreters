
all: jlox

jlox:
	@ $(MAKE) -f util/java.make DIR=java PACKAGE=lox

.PHONY: jlox
