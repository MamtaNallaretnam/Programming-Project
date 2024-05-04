package parser;

import java.util.Stack;

import scanner.Scanner;
import scanner.Token;
import scanner.TokenType;

// Uses recursive descent parsing
public class Parser {
  private Scanner s;
  private Token currentToken;
  Stack<ASTNode> stack;

  public Parser(Scanner s) {
    this.s = s;
    stack = new Stack<ASTNode>();
  }

  public AST buildAST() {
    startParse();
    return new AST(stack.pop());
  }

  public void startParse() {
    readNT();
    procE();
    if (currentToken != null)
      throw new ParseException("Expected EOF.");
  }

  private void readNT() {
    do {
      currentToken = s.readNextToken();
    } while (isCurrentTokenType(TokenType.DELETE));
    if (null != currentToken) {
      if (currentToken.getType() == TokenType.IDENTIFIER) {
        createTerminalASTNode(ASTNodeType.IDENTIFIER, currentToken.getValue());
      } else if (currentToken.getType() == TokenType.INTEGER) {
        createTerminalASTNode(ASTNodeType.INTEGER, currentToken.getValue());
      } else if (currentToken.getType() == TokenType.STRING) {
        createTerminalASTNode(ASTNodeType.STRING, currentToken.getValue());
      }
    }
  }

  private boolean isCurrentToken(TokenType type, String value) {
    if (currentToken == null)
      return false;
    if (currentToken.getType() != type || !currentToken.getValue().equals(value))
      return false;
    return true;
  }

  private boolean isCurrentTokenType(TokenType type) {
    if (currentToken == null)
      return false;
    if (currentToken.getType() == type)
      return true;
    return false;
  }

  // Building an N-ary ast node
  private void buildNAryASTNode(ASTNodeType type, int ariness) {
    ASTNode node = new ASTNode();
    node.setType(type);
    while (ariness > 0) {
      ASTNode child = stack.pop();
      if (node.getChild() != null)
        child.setSibling(node.getChild());
      node.setChild(child);
      ariness--;
    }
    stack.push(node);
  }

  private void createTerminalASTNode(ASTNodeType type, String value) {
    ASTNode node = new ASTNode();
    node.setType(type);
    node.setValue(value);
    stack.push(node);
  }

  // Expressions
  private void procE() {
    if (isCurrentToken(TokenType.RESERVED, "let")) {
      readNT();
      procD();
      if (!isCurrentToken(TokenType.RESERVED, "in"))
        throw new ParseException("E:  'in' expected");
      readNT();
      procE();
      buildNAryASTNode(ASTNodeType.LET, 2);
    } else if (isCurrentToken(TokenType.RESERVED, "fn")) {
      int treesToPop = 0;

      readNT();
      while (isCurrentTokenType(TokenType.IDENTIFIER) || isCurrentTokenType(TokenType.L_PAREN)) {
        procVB();
        treesToPop++;
      }

      if (treesToPop == 0)
        throw new ParseException("E: at least one 'Vb' expected");

      if (!isCurrentToken(TokenType.OPERATOR, "."))
        throw new ParseException("E: '.' expected");

      readNT();
      procE();

      buildNAryASTNode(ASTNodeType.LAMBDA, treesToPop + 1);
    } else
      procEW();
  }

 
  private void procEW() {
    procT();
    if (isCurrentToken(TokenType.RESERVED, "where")) {
      readNT();
      procDR();
      buildNAryASTNode(ASTNodeType.WHERE, 2);
    }
  }

  // Arithmetic Expressions
  private void procA() {
    if (isCurrentToken(TokenType.OPERATOR, "+")) { 
      readNT();
      procAT(); 
    } else if (isCurrentToken(TokenType.OPERATOR, "-")) { 
      readNT();
      procAT(); 
      buildNAryASTNode(ASTNodeType.NEG, 1);
    } else
      procAT(); 

    boolean plus = true;
    while (isCurrentToken(TokenType.OPERATOR, "+") || isCurrentToken(TokenType.OPERATOR, "-")) {
      if (currentToken.getValue().equals("+"))
        plus = true;
      else if (currentToken.getValue().equals("-"))
        plus = false;
      readNT();
      procAT(); 
      if (plus) 
        buildNAryASTNode(ASTNodeType.PLUS, 2);
      else 
        buildNAryASTNode(ASTNodeType.MINUS, 2);
    }
  }

  private void procAT() {
    procAF(); 
    boolean mult = true;
    while (isCurrentToken(TokenType.OPERATOR, "*") || isCurrentToken(TokenType.OPERATOR, "/")) {
      if (currentToken.getValue().equals("*"))
        mult = true;
      else if (currentToken.getValue().equals("/"))
        mult = false;
      readNT();
      procAF(); 
      if (mult) 
        buildNAryASTNode(ASTNodeType.MULT, 2);
      else 
        buildNAryASTNode(ASTNodeType.DIV, 2);
    }
  }

  private void procAF() {
    procAP(); 
    if (isCurrentToken(TokenType.OPERATOR, "**")) { 
      readNT();
      procAF();
      buildNAryASTNode(ASTNodeType.EXP, 2);
    }
  }

  private void procAP() {
    procR(); 
    while (isCurrentToken(TokenType.OPERATOR, "@")) { 
      readNT();
      if (!isCurrentTokenType(TokenType.IDENTIFIER))
        throw new ParseException("AP: expected Identifier");
      readNT();
      procR(); 
      buildNAryASTNode(ASTNodeType.AT, 3);
    }
  }

  // Boolean Expressions
  private void procB() {
    procBT();
    while (isCurrentToken(TokenType.RESERVED, "or")) {
      readNT();
      procBT();
      buildNAryASTNode(ASTNodeType.OR, 2);
    }
  }

  private void procBT() {
    procBS(); 
    while (isCurrentToken(TokenType.OPERATOR, "&")) {
      readNT();
      procBS(); 
      buildNAryASTNode(ASTNodeType.AND, 2);
    }
  }

  private void procBS() {
    if (isCurrentToken(TokenType.RESERVED, "not")) { 
      readNT();
      procBP();
      buildNAryASTNode(ASTNodeType.NOT, 1);
    } else
      procBP();
  }

  private void procBP() {
    procA();
    if (isCurrentToken(TokenType.RESERVED, "gr") || isCurrentToken(TokenType.OPERATOR, ">")) { 
      readNT();
      procA();
      buildNAryASTNode(ASTNodeType.GR, 2);
    } else if (isCurrentToken(TokenType.RESERVED, "ge") || isCurrentToken(TokenType.OPERATOR, ">=")) { 
      readNT();
      procA();
      buildNAryASTNode(ASTNodeType.GE, 2);
    } else if (isCurrentToken(TokenType.RESERVED, "ls") || isCurrentToken(TokenType.OPERATOR, "<")) { 
      readNT();
      procA(); 
      buildNAryASTNode(ASTNodeType.LS, 2);
    } else if (isCurrentToken(TokenType.RESERVED, "le") || isCurrentToken(TokenType.OPERATOR, "<=")) { 
      readNT();
      procA(); 
      buildNAryASTNode(ASTNodeType.LE, 2);
    } else if (isCurrentToken(TokenType.RESERVED, "eq")) { 
      readNT();
      procA(); 
      buildNAryASTNode(ASTNodeType.EQ, 2);
    } else if (isCurrentToken(TokenType.RESERVED, "ne")) { 
      readNT();
      procA(); 
      buildNAryASTNode(ASTNodeType.NE, 2);
    }
  }

  // Tuple expressions
  private void procT() {
    procTA();
    int treesToPop = 0;
    while (isCurrentToken(TokenType.OPERATOR, ",")) {
      readNT();
      procTA();
      treesToPop++;
    }
    if (treesToPop > 0)
      buildNAryASTNode(ASTNodeType.TAU, treesToPop + 1);
  }

  private void procTA() {
    procTC();
    while (isCurrentToken(TokenType.RESERVED, "aug")) {
      readNT();
      procTC();
      buildNAryASTNode(ASTNodeType.AUG, 2);
    }
  }

  private void procTC() {
    procB();
    if (isCurrentToken(TokenType.OPERATOR, "->")) {
      readNT();
      procTC();
      if (!isCurrentToken(TokenType.OPERATOR, "|"))
        throw new ParseException("TC: '|' expected");
      readNT();
      procTC();
      buildNAryASTNode(ASTNodeType.CONDITIONAL, 3);
    }
  }

  // Rators and Rands
  private void procR() {
    procRN(); 
    readNT();
    while (isCurrentTokenType(TokenType.INTEGER) ||
        isCurrentTokenType(TokenType.STRING) ||
        isCurrentTokenType(TokenType.IDENTIFIER) ||
        isCurrentToken(TokenType.RESERVED, "true") ||
        isCurrentToken(TokenType.RESERVED, "false") ||
        isCurrentToken(TokenType.RESERVED, "nil") ||
        isCurrentToken(TokenType.RESERVED, "dummy") ||
        isCurrentTokenType(TokenType.L_PAREN)) { 
      procRN(); 
      buildNAryASTNode(ASTNodeType.GAMMA, 2);
      readNT();
    }
  }

  private void procRN() {
    if (isCurrentTokenType(TokenType.IDENTIFIER) || 
        isCurrentTokenType(TokenType.INTEGER) || 
        isCurrentTokenType(TokenType.STRING)) { 
    } else if (isCurrentToken(TokenType.RESERVED, "true")) { 
      createTerminalASTNode(ASTNodeType.TRUE, "true");
    } else if (isCurrentToken(TokenType.RESERVED, "false")) { 
      createTerminalASTNode(ASTNodeType.FALSE, "false");
    } else if (isCurrentToken(TokenType.RESERVED, "nil")) { 
      createTerminalASTNode(ASTNodeType.NIL, "nil");
    } else if (isCurrentTokenType(TokenType.L_PAREN)) {
      readNT();
      procE(); 
      if (!isCurrentTokenType(TokenType.R_PAREN))
        throw new ParseException("RN: ')' expected");
    } else if (isCurrentToken(TokenType.RESERVED, "dummy")) { 
      createTerminalASTNode(ASTNodeType.DUMMY, "dummy");
    }
  }

  // Variables
  private void procVB() {
    if (isCurrentTokenType(TokenType.IDENTIFIER)) { 
      readNT();
    } else if (isCurrentTokenType(TokenType.L_PAREN)) {
      readNT();
      if (isCurrentTokenType(TokenType.R_PAREN)) { 
        createTerminalASTNode(ASTNodeType.PAREN, "");
        readNT();
      } else { 
        procVL(); 
        if (!isCurrentTokenType(TokenType.R_PAREN))
          throw new ParseException("VB: ')' expected");
        readNT();
      }
    }
  }

  
  private void procVL() {
    if (!isCurrentTokenType(TokenType.IDENTIFIER))
      throw new ParseException("VL: Identifier expected");
    else {
      readNT();
      int treesToPop = 0;
      while (isCurrentToken(TokenType.OPERATOR, ",")) { 
        readNT();
        if (!isCurrentTokenType(TokenType.IDENTIFIER))
          throw new ParseException("VL: Identifier expected");
        readNT();
        treesToPop++;
      }
      if (treesToPop > 0)
        buildNAryASTNode(ASTNodeType.COMMA, treesToPop + 1); 
    }
  }

  // Definitions
  private void procD() {
    procDA(); 
    if (isCurrentToken(TokenType.RESERVED, "within")) { 
      readNT();
      procD();
      buildNAryASTNode(ASTNodeType.WITHIN, 2);
    }
  }

  private void procDA() {
    procDR();
    int treesToPop = 0;
    while (isCurrentToken(TokenType.RESERVED, "and")) { 
      readNT();
      procDR(); 
      treesToPop++;
    }
    if (treesToPop > 0)
      buildNAryASTNode(ASTNodeType.SIMULTDEF, treesToPop + 1);
  }

  private void procDR() {
    if (isCurrentToken(TokenType.RESERVED, "rec")) { 
      readNT();
      procDB(); 
      buildNAryASTNode(ASTNodeType.REC, 1);
    } else { 
      procDB(); 
    }
  }

  private void procDB() {
    if (isCurrentTokenType(TokenType.L_PAREN)) { 
      procD();
      readNT();
      if (!isCurrentTokenType(TokenType.R_PAREN))
        throw new ParseException("DB: ')' expected");
      readNT();
    } else if (isCurrentTokenType(TokenType.IDENTIFIER)) {
      readNT();
      if (isCurrentToken(TokenType.OPERATOR, ",")) { 
        readNT();
        procVL(); 
        if (!isCurrentToken(TokenType.OPERATOR, "="))
          throw new ParseException("DB: = expected.");
        buildNAryASTNode(ASTNodeType.COMMA, 2);
        readNT();
        procE(); 
        buildNAryASTNode(ASTNodeType.EQUAL, 2);
      } else { 
        if (isCurrentToken(TokenType.OPERATOR, "=")) { 
          readNT();
          procE(); 
          buildNAryASTNode(ASTNodeType.EQUAL, 2);
        } else { 
          int treesToPop = 0;

          while (isCurrentTokenType(TokenType.IDENTIFIER) || isCurrentTokenType(TokenType.L_PAREN)) {
            procVB(); 
            treesToPop++;
          }

          if (treesToPop == 0)
            throw new ParseException("E: at least one 'Vb' expected");

          if (!isCurrentToken(TokenType.OPERATOR, "="))
            throw new ParseException("DB: = expected.");

          readNT();
          procE(); 

          buildNAryASTNode(ASTNodeType.FCNFORM, treesToPop + 2); 
        }
      }
    }
  }

  

}
