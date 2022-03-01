SHELL=/bin/bash -o pipefail

SRC_DIR := src/main
TEST_DIR := src/test/resources
REPORTS_DIR := reports
OUT_DIR := build

SRCS := $(shell find src -name '*.java')
CLS := $(SRCS:$(SRC_DIR)/%.java=$(OUT_DIR)/%.class)
TESTS := $(shell find src -name '*.lox')
REPORTS := $(TESTS:$(TEST_DIR)/%.lox=$(REPORTS_DIR)/%.report)

JAVA := java
JC := javac
JCFLAGS := -d $(OUT_DIR)/ -cp $(SRC_DIR)/

.PHONY: all test clean run genexpr genstmt

all: $(CLS)

test: $(REPORTS)

$(CLS): $(OUT_DIR)/%.class: $(SRC_DIR)/%.java
	$(JC) $(JCFLAGS) $<

$(REPORTS): reports/%.report: $(TEST_DIR)/%.lox $(CLS) | $(REPORTS_DIR)
	@ echo -ne "[$*]: "
	@ $(JAVA) -cp $(OUT_DIR) lox.Lox $< 2>&1 | tee $@
	@ echo PASSED

$(REPORTS_DIR):
	@ mkdir -p $@

clean:
	@ rm -rf $(OUT_DIR)/lox
	@ rm -rf $(REPORTS_DIR)

run: $(CLS)
	@ $(JAVA) -cp $(OUT_DIR) lox.Lox

genexpr: $(OUT_DIR)/tool/GenerateAst.class
	$(JAVA) -cp $(OUT_DIR) tool.GenerateAst Expr $(SRC_DIR)/lox

genstmt: $(OUT_DIR)/tool/GenerateAst.class
	$(JAVA) -cp $(OUT_DIR) tool.GenerateAst Stmt $(SRC_DIR)/lox
