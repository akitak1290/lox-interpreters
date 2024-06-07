# SOME overheads to read the file,
# for learning make, remove later!

JAVAC=javac
sources = $(wildcard *.java)
classes = $(sources:.java=.class)

all: program

program: $(classes)

%.class: %.java
	$(JAVAC) -d . $<

.PHONY: all program
