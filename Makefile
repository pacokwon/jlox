SRC_DIR := src/main
OUT_DIR := build

SRCS := $(shell find src -name '*.java')
CLS := $(SRCS:$(SRC_DIR)/%.java=$(OUT_DIR)/%.class)

JAVA := java
JC := javac
JCFLAGS := -d $(OUT_DIR)/ -cp $(SRC_DIR)/

.PHONY: all clean run genexpr genstmt

all: $(CLS)

$(CLS): $(OUT_DIR)/%.class: $(SRC_DIR)/%.java
	$(JC) $(JCFLAGS) $<

clean:
	@ rm -rf $(OUT_DIR)/lox

run: $(CLS)
	@ $(JAVA) -cp $(OUT_DIR) lox.Lox

genexpr: $(OUT_DIR)/tool/GenerateAst.class
	$(JAVA) -cp $(OUT_DIR) tool.GenerateAst Expr $(SRC_DIR)/lox

genstmt: $(OUT_DIR)/tool/GenerateAst.class
	$(JAVA) -cp $(OUT_DIR) tool.GenerateAst Stmt $(SRC_DIR)/lox
