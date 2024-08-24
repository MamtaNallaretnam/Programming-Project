JC = javac

# Directories
SRC_DIR = .
OUTPUT_DIR = bin
CSEM_DIR = csem
PARSER_DIR = parser
SCANNER_DIR = scanner

# Collect all Java files from the specified directories
JAVA_FILES := $(wildcard $(SRC_DIR)/*.java) \
              $(wildcard $(CSEM_DIR)/*.java) \
              $(wildcard $(PARSER_DIR)/*.java) \
              $(wildcard $(SCANNER_DIR)/*.java)

# Define the corresponding class files output locations
OBJ_FILES := $(patsubst %.java,$(OUTPUT_DIR)/%.class,$(JAVA_FILES))

# Default target to compile all Java files
all: $(OBJ_FILES)

# Rule to compile individual Java files to the bin directory
$(OUTPUT_DIR)/%.class: %.java
	@mkdir -p $(dir $@)
	$(JC) -d $(OUTPUT_DIR) $<

# Target to run the program, expects a 'file' variable
run: $(OBJ_FILES)
	java -cp $(OUTPUT_DIR) rpalMain rpal_test_programs/$(file)

# Print AST
ast: $(OBJ_FILES)
	java -cp $(OUTPUT_DIR) rpalMain rpal_test_programs/$(file) -ast

# Print ST
st: $(OBJ_FILES)
	java -cp $(OUTPUT_DIR) rpalMain rpal_test_programs/$(file) -st

# Clean up compiled class files
clean:
	rm -rf $(OUTPUT_DIR)

.PHONY: all clean run ast st
