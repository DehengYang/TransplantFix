package com.google.javascript.jscomp;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.NodeUtil.MatchNotFunction;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;
import com.google.javascript.rhino.TokenStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

class CodeGenerator {

    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private final CodeConsumer cc;

    private final CharsetEncoder outputCharsetEncoder;

    CodeGenerator(CodeConsumer consumer, Charset outputCharset) {
        cc = consumer;
        if (outputCharset == null || outputCharset == Charsets.US_ASCII) {
            this.outputCharsetEncoder = null;
        } else {
            this.outputCharsetEncoder = outputCharset.newEncoder();
        }
    }

    CodeGenerator(CodeConsumer consumer) {
        this(consumer, null);
    }

    public void tagAsStrict() {
        add("'use strict';");
    }

    void add(String str) {
        cc.add(str);
    }

    private void addIdentifier(String identifier) {
        cc.addIdentifier(identifierEscape(identifier));
    }

    void add(Node n) {
        add(n, Context.OTHER);
    }

    void add(Node n, Context context) {
        if (!cc.continueProcessing()) {
            return;
        }
        int type = n.getType();
        String opstr = NodeUtil.opToStr(type);
        int childCount = n.getChildCount();
        Node first = n.getFirstChild();
        Node last = n.getLastChild();
        if (opstr != null && first != last) {
            Preconditions.checkState(childCount == 2, "Bad binary operator \"%s\": expected 2 arguments but got %s", opstr, childCount);
            int p = NodeUtil.precedence(type);
            addLeftExpr(first, p, context);
            cc.addOp(opstr, true);
            Context rhsContext = getContextForNoInOperator(context);
            if (last.getType() == type && NodeUtil.isAssociative(type)) {
                addExpr(last, p, rhsContext);
            } else if (NodeUtil.isAssignmentOp(n) && NodeUtil.isAssignmentOp(last)) {
                addExpr(last, p, rhsContext);
            } else {
                addExpr(last, p + 1, rhsContext);
            }
            return;
        }
        cc.startSourceMapping(n);
        switch(type) {
            case Token.TRY:
                {
                    Preconditions.checkState(first.getNext().getType() == Token.BLOCK && !first.getNext().hasMoreThanOneChild());
                    Preconditions.checkState(childCount >= 2 && childCount <= 3);
                    add("try");
                    add(first, Context.PRESERVE_BLOCK);
                    Node catchblock = first.getNext().getFirstChild();
                    if (catchblock != null) {
                        add(catchblock);
                    }
                    if (childCount == 3) {
                        add("finally");
                        add(last, Context.PRESERVE_BLOCK);
                    }
                    break;
                }
            case Token.CATCH:
                Preconditions.checkState(childCount == 2);
                add("catch(");
                add(first);
                add(")");
                add(last, Context.PRESERVE_BLOCK);
                break;
            case Token.THROW:
                Preconditions.checkState(childCount == 1);
                add("throw");
                add(first);
                cc.endStatement(true);
                break;
            case Token.RETURN:
                add("return");
                if (childCount == 1) {
                    add(first);
                } else {
                    Preconditions.checkState(childCount == 0);
                }
                cc.endStatement();
                break;
            case Token.VAR:
                if (first != null) {
                    add("var ");
                    addList(first, false, getContextForNoInOperator(context));
                }
                break;
            case Token.LABEL_NAME:
                Preconditions.checkState(!n.getString().isEmpty());
                addIdentifier(n.getString());
                break;
            case Token.NAME:
                if (first == null || first.getType() == Token.EMPTY) {
                    addIdentifier(n.getString());
                } else {
                    Preconditions.checkState(childCount == 1);
                    addIdentifier(n.getString());
                    cc.addOp("=", true);
                    if (first.getType() == Token.COMMA) {
                        addExpr(first, NodeUtil.precedence(Token.ASSIGN));
                    } else {
                        addExpr(first, 0, getContextForNoInOperator(context));
                    }
                }
                break;
            case Token.ARRAYLIT:
                add("[");
                addArrayList(first);
                add("]");
                break;
            case Token.LP:
                add("(");
                addList(first);
                add(")");
                break;
            case Token.COMMA:
                Preconditions.checkState(childCount == 2);
                addList(first, false, context);
                break;
            case Token.NUMBER:
                Preconditions.checkState(childCount == 0);
                cc.addNumber(n.getDouble());
                break;
            case Token.TYPEOF:
            case Token.VOID:
            case Token.NOT:
            case Token.BITNOT:
            case Token.POS:
                {
                    Preconditions.checkState(childCount == 1);
                    cc.addOp(NodeUtil.opToStrNoFail(type), false);
                    addExpr(first, NodeUtil.precedence(type));
                    break;
                }
            case Token.NEG:
                {
                    Preconditions.checkState(childCount == 1);
                    if (n.getFirstChild().getType() == Token.NUMBER) {
                        cc.addNumber(-n.getFirstChild().getDouble());
                    } else {
                        cc.addOp(NodeUtil.opToStrNoFail(type), false);
                        addExpr(first, NodeUtil.precedence(type));
                    }
                    break;
                }
            case Token.HOOK:
                {
                    Preconditions.checkState(childCount == 3);
                    int p = NodeUtil.precedence(type);
                    addLeftExpr(first, p + 1, context);
                    cc.addOp("?", true);
                    addExpr(first.getNext(), 1);
                    cc.addOp(":", true);
                    addExpr(last, 1);
                    break;
                }
            case Token.REGEXP:
                if (first.getType() != Token.STRING || last.getType() != Token.STRING) {
                    throw new Error("Expected children to be strings");
                }
                String regexp = regexpEscape(first.getString(), outputCharsetEncoder);
                if (childCount == 2) {
                    add(regexp + last.getString());
                } else {
                    Preconditions.checkState(childCount == 1);
                    add(regexp);
                }
                break;
            case Token.GET_REF:
                add(first);
                break;
            case Token.REF_SPECIAL:
                Preconditions.checkState(childCount == 1);
                add(first);
                add(".");
                add((String) n.getProp(Node.NAME_PROP));
                break;
            case Token.FUNCTION:
                if (n.getClass() != Node.class) {
                    throw new Error("Unexpected Node subclass.");
                }
                Preconditions.checkState(childCount == 3);
                boolean funcNeedsParens = (context == Context.START_OF_EXPR);
                if (funcNeedsParens) {
                    add("(");
                }
                add("function");
                add(first);
                add(first.getNext());
                add(last, Context.PRESERVE_BLOCK);
                cc.endFunction(context == Context.STATEMENT);
                if (funcNeedsParens) {
                    add(")");
                }
                break;
            case Token.GET:
            case Token.SET:
                Preconditions.checkState(n.getParent().getType() == Token.OBJECTLIT);
                Preconditions.checkState(childCount == 1);
                Preconditions.checkState(first.getType() == Token.FUNCTION);
                Preconditions.checkState(first.getFirstChild().getString().isEmpty());
                if (type == Token.GET) {
                    Preconditions.checkState(!first.getChildAtIndex(1).hasChildren());
                    add("get ");
                } else {
                    Preconditions.checkState(first.getChildAtIndex(1).hasOneChild());
                    add("set ");
                }
                String name = n.getString();
                Node fn = first;
                Node parameters = fn.getChildAtIndex(1);
                Node body = fn.getLastChild();
                if (!n.isQuotedString() && TokenStream.isJSIdentifier(name) && NodeUtil.isLatin(name)) {
                    add(name);
                } else {
                    double d = getSimpleNumber(name);
                    if (!Double.isNaN(d)) {
                        cc.addNumber(d);
                    } else {
                        add(jsString(n.getString(), outputCharsetEncoder));
                    }
                }
                add(parameters);
                add(body, Context.PRESERVE_BLOCK);
                break;
            case Token.SCRIPT:
            case Token.BLOCK:
                {
                    if (n.getClass() != Node.class) {
                        throw new Error("Unexpected Node subclass.");
                    }
                    boolean preserveBlock = context == Context.PRESERVE_BLOCK;
                    if (preserveBlock) {
                        cc.beginBlock();
                    }
                    boolean preferLineBreaks = type == Token.SCRIPT || (type == Token.BLOCK && !preserveBlock && n.getParent() != null && n.getParent().getType() == Token.SCRIPT);
                    for (Node c = first; c != null; c = c.getNext()) {
                        add(c, Context.STATEMENT);
                        if (c.getType() == Token.VAR) {
                            cc.endStatement();
                        }
                        if (c.getType() == Token.FUNCTION) {
                            cc.maybeLineBreak();
                        }
                        if (preferLineBreaks) {
                            cc.notePreferredLineBreak();
                        }
                    }
                    if (preserveBlock) {
                        cc.endBlock(cc.breakAfterBlockFor(n, context == Context.STATEMENT));
                    }
                    break;
                }
            case Token.FOR:
                if (childCount == 4) {
                    add("for(");
                    if (first.getType() == Token.VAR) {
                        add(first, Context.IN_FOR_INIT_CLAUSE);
                    } else {
                        addExpr(first, 0, Context.IN_FOR_INIT_CLAUSE);
                    }
                    add(";");
                    add(first.getNext());
                    add(";");
                    add(first.getNext().getNext());
                    add(")");
                    addNonEmptyStatement(last, getContextForNonEmptyExpression(context), false);
                } else {
                    Preconditions.checkState(childCount == 3);
                    add("for(");
                    add(first);
                    add("in");
                    add(first.getNext());
                    add(")");
                    addNonEmptyStatement(last, getContextForNonEmptyExpression(context), false);
                }
                break;
            case Token.DO:
                Preconditions.checkState(childCount == 2);
                add("do");
                addNonEmptyStatement(first, Context.OTHER, false);
                add("while(");
                add(last);
                add(")");
                cc.endStatement();
                break;
            case Token.WHILE:
                Preconditions.checkState(childCount == 2);
                add("while(");
                add(first);
                add(")");
                addNonEmptyStatement(last, getContextForNonEmptyExpression(context), false);
                break;
            case Token.EMPTY:
                Preconditions.checkState(childCount == 0);
                break;
            case Token.GETPROP:
                {
                    Preconditions.checkState(childCount == 2, "Bad GETPROP: expected 2 children, but got %s", childCount);
                    Preconditions.checkState(last.getType() == Token.STRING, "Bad GETPROP: RHS should be STRING");
                    boolean needsParens = (first.getType() == Token.NUMBER);
                    if (needsParens) {
                        add("(");
                    }
                    addLeftExpr(first, NodeUtil.precedence(type), context);
                    if (needsParens) {
                        add(")");
                    }
                    add(".");
                    addIdentifier(last.getString());
                    break;
                }
            case Token.GETELEM:
                Preconditions.checkState(childCount == 2, "Bad GETELEM: expected 2 children but got %s", childCount);
                addLeftExpr(first, NodeUtil.precedence(type), context);
                add("[");
                add(first.getNext());
                add("]");
                break;
            case Token.WITH:
                Preconditions.checkState(childCount == 2);
                add("with(");
                add(first);
                add(")");
                addNonEmptyStatement(last, getContextForNonEmptyExpression(context), false);
                break;
            case Token.INC:
            case Token.DEC:
                {
                    Preconditions.checkState(childCount == 1);
                    String o = type == Token.INC ? "++" : "--";
                    int postProp = n.getIntProp(Node.INCRDECR_PROP);
                    if (postProp != 0) {
                        addLeftExpr(first, NodeUtil.precedence(type), context);
                        cc.addOp(o, false);
                    } else {
                        cc.addOp(o, false);
                        add(first);
                    }
                    break;
                }
            case Token.CALL:
                if (isIndirectEval(first) || n.getBooleanProp(Node.FREE_CALL) && NodeUtil.isGet(first)) {
                    add("(0,");
                    addExpr(first, NodeUtil.precedence(Token.COMMA));
                    add(")");
                } else {
                    addLeftExpr(first, NodeUtil.precedence(type), context);
                }
                add("(");
                addList(first.getNext());
                add(")");
                break;
            case Token.IF:
                boolean hasElse = childCount == 3;
                boolean ambiguousElseClause = context == Context.BEFORE_DANGLING_ELSE && !hasElse;
                if (ambiguousElseClause) {
                    cc.beginBlock();
                }
                add("if(");
                add(first);
                add(")");
                if (hasElse) {
                    addNonEmptyStatement(first.getNext(), Context.BEFORE_DANGLING_ELSE, false);
                    add("else");
                    addNonEmptyStatement(last, getContextForNonEmptyExpression(context), false);
                } else {
                    addNonEmptyStatement(first.getNext(), Context.OTHER, false);
                    Preconditions.checkState(childCount == 2);
                }
                if (ambiguousElseClause) {
                    cc.endBlock();
                }
                break;
            case Token.NULL:
            case Token.THIS:
            case Token.FALSE:
            case Token.TRUE:
                Preconditions.checkState(childCount == 0);
                add(Node.tokenToName(type));
                break;
            case Token.CONTINUE:
                Preconditions.checkState(childCount <= 1);
                add("continue");
                if (childCount == 1) {
                    if (first.getType() != Token.LABEL_NAME) {
                        throw new Error("Unexpected token type. Should be LABEL_NAME.");
                    }
                    add(" ");
                    add(first);
                }
                cc.endStatement();
                break;
            case Token.DEBUGGER:
                Preconditions.checkState(childCount == 0);
                add("debugger");
                cc.endStatement();
                break;
            case Token.BREAK:
                Preconditions.checkState(childCount <= 1);
                add("break");
                if (childCount == 1) {
                    if (first.getType() != Token.LABEL_NAME) {
                        throw new Error("Unexpected token type. Should be LABEL_NAME.");
                    }
                    add(" ");
                    add(first);
                }
                cc.endStatement();
                break;
            case Token.EXPR_VOID:
                throw new Error("Unexpected EXPR_VOID. Should be EXPR_RESULT.");
            case Token.EXPR_RESULT:
                Preconditions.checkState(childCount == 1);
                add(first, Context.START_OF_EXPR);
                cc.endStatement();
                break;
            case Token.NEW:
                add("new ");
                int precedence = NodeUtil.precedence(type);
                if (NodeUtil.containsType(first, Token.CALL, new MatchNotFunction())) {
                    precedence = NodeUtil.precedence(first.getType()) + 1;
                }
                addExpr(first, precedence);
                Node next = first.getNext();
                if (next != null) {
                    add("(");
                    addList(next);
                    add(")");
                }
                break;
            case Token.STRING:
                if (childCount != ((n.getParent() != null && n.getParent().getType() == Token.OBJECTLIT) ? 1 : 0)) {
                    throw new IllegalStateException("Unexpected String children: " + n.getParent().toStringTree());
                }
                add(jsString(n.getString(), outputCharsetEncoder));
                break;
            case Token.DELPROP:
                Preconditions.checkState(childCount == 1);
                add("delete ");
                add(first);
                break;
            case Token.OBJECTLIT:
                {
                    boolean needsParens = (context == Context.START_OF_EXPR);
                    if (needsParens) {
                        add("(");
                    }
                    add("{");
                    for (Node c = first; c != null; c = c.getNext()) {
                        if (c != first) {
                            cc.listSeparator();
                        }
                        if (c.getType() == Token.GET || c.getType() == Token.SET) {
                            add(c);
                        } else {
                            Preconditions.checkState(c.getType() == Token.STRING);
                            String key = c.getString();
                            if (!c.isQuotedString() && !TokenStream.isKeyword(key) && TokenStream.isJSIdentifier(key) && NodeUtil.isLatin(key)) {
                                add(key);
                            } else {
                                double d = getSimpleNumber(key);
                                if (!Double.isNaN(d)) {
                                    cc.addNumber(d);
                                } else {
                                    addExpr(c, 1);
                                }
                            }
                            add(":");
                            addExpr(c.getFirstChild(), 1);
                        }
                    }
                    add("}");
                    if (needsParens) {
                        add(")");
                    }
                    break;
                }
            case Token.SWITCH:
                add("switch(");
                add(first);
                add(")");
                cc.beginBlock();
                addAllSiblings(first.getNext());
                cc.endBlock(context == Context.STATEMENT);
                break;
            case Token.CASE:
                Preconditions.checkState(childCount == 2);
                add("case ");
                add(first);
                addCaseBody(last);
                break;
            case Token.DEFAULT:
                Preconditions.checkState(childCount == 1);
                add("default");
                addCaseBody(first);
                break;
            case Token.LABEL:
                Preconditions.checkState(childCount == 2);
                if (first.getType() != Token.LABEL_NAME) {
                    throw new Error("Unexpected token type. Should be LABEL_NAME.");
                }
                add(first);
                add(":");
                addNonEmptyStatement(last, getContextForNonEmptyExpression(context), true);
                break;
            case Token.SETNAME:
                break;
            default:
                throw new Error("Unknown type " + type + "\n" + n.toStringTree());
        }
        cc.endSourceMapping(n);
    }

    static boolean isSimpleNumber(String s) {
        int len = s.length();
        for (int index = 0; index < len; index++) {
            char c = s.charAt(index);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return len > 0;
    }

    static double getSimpleNumber(String s) {
        if (isSimpleNumber(s)) {
            long l = Long.parseLong(s);
            if (l < NodeUtil.MAX_POSITIVE_INTEGER_NUMBER) {
                return l;
            }
        }
        return Double.NaN;
    }

    private boolean isIndirectEval(Node n) {
        return n.getType() == Token.NAME && "eval".equals(n.getString()) && !n.getBooleanProp(Node.DIRECT_EVAL);
    }

    private void addNonEmptyStatement(Node n, Context context, boolean allowNonBlockChild) {
        Node nodeToProcess = n;
        if (!allowNonBlockChild && n.getType() != Token.BLOCK) {
            throw new Error("Missing BLOCK child.");
        }
        if (n.getType() == Token.BLOCK) {
            int count = getNonEmptyChildCount(n, 2);
            if (count == 0) {
                if (cc.shouldPreserveExtraBlocks()) {
                    cc.beginBlock();
                    cc.endBlock(cc.breakAfterBlockFor(n, context == Context.STATEMENT));
                } else {
                    cc.endStatement(true);
                }
                return;
            }
            if (count == 1) {
                Node firstAndOnlyChild = getFirstNonEmptyChild(n);
                boolean alwaysWrapInBlock = cc.shouldPreserveExtraBlocks();
                if (alwaysWrapInBlock || isOneExactlyFunctionOrDo(firstAndOnlyChild)) {
                    cc.beginBlock();
                    add(firstAndOnlyChild, Context.STATEMENT);
                    cc.maybeLineBreak();
                    cc.endBlock(cc.breakAfterBlockFor(n, context == Context.STATEMENT));
                    return;
                } else {
                    nodeToProcess = firstAndOnlyChild;
                }
            }
            if (count > 1) {
                context = Context.PRESERVE_BLOCK;
            }
        }
        if (nodeToProcess.getType() == Token.EMPTY) {
            cc.endStatement(true);
        } else {
            add(nodeToProcess, context);
            if (nodeToProcess.getType() == Token.VAR) {
                cc.endStatement();
            }
        }
    }

    private boolean isOneExactlyFunctionOrDo(Node n) {
        if (n.getType() == Token.LABEL) {
            Node labeledStatement = n.getLastChild();
            if (labeledStatement.getType() != Token.BLOCK) {
                return isOneExactlyFunctionOrDo(labeledStatement);
            } else {
                if (getNonEmptyChildCount(n, 2) == 1) {
                    return isOneExactlyFunctionOrDo(getFirstNonEmptyChild(n));
                } else {
                    return false;
                }
            }
        } else {
            return (n.getType() == Token.FUNCTION || n.getType() == Token.DO);
        }
    }

    void addLeftExpr(Node n, int minPrecedence, Context context) {
        addExpr(n, minPrecedence, context);
    }

    void addExpr(Node n, int minPrecedence) {
        addExpr(n, minPrecedence, Context.OTHER);
    }

    private void addExpr(Node n, int minPrecedence, Context context) {
        if ((NodeUtil.precedence(n.getType()) < minPrecedence) || ((context == Context.IN_FOR_INIT_CLAUSE) && (n.getType() == Token.IN))) {
            add("(");
            add(n, clearContextForNoInOperator(context));
            add(")");
        } else {
            add(n, context);
        }
    }

    void addList(Node firstInList) {
        addList(firstInList, true, Context.OTHER);
    }

    void addList(Node firstInList, boolean isArrayOrFunctionArgument) {
        addList(firstInList, isArrayOrFunctionArgument, Context.OTHER);
    }

    void addList(Node firstInList, boolean isArrayOrFunctionArgument, Context lhsContext) {
        for (Node n = firstInList; n != null; n = n.getNext()) {
            boolean isFirst = n == firstInList;
            if (isFirst) {
                addLeftExpr(n, isArrayOrFunctionArgument ? 1 : 0, lhsContext);
            } else {
                cc.listSeparator();
                addExpr(n, isArrayOrFunctionArgument ? 1 : 0);
            }
        }
    }

    void addArrayList(Node firstInList) {
        boolean lastWasEmpty = false;
        for (Node n = firstInList; n != null; n = n.getNext()) {
            if (n != firstInList) {
                cc.listSeparator();
            }
            addExpr(n, 1);
            lastWasEmpty = n.getType() == Token.EMPTY;
        }
        if (lastWasEmpty) {
            cc.listSeparator();
        }
    }

    void addCaseBody(Node caseBody) {
        cc.beginCaseBody();
        add(caseBody);
        cc.endCaseBody();
    }

    void addAllSiblings(Node n) {
        for (Node c = n; c != null; c = c.getNext()) {
            add(c);
        }
    }

    static String jsString(String s, CharsetEncoder outputCharsetEncoder) {
        int singleq = 0, doubleq = 0;
        for (int i = 0; i < s.length(); i++) {
            switch(s.charAt(i)) {
                case '"':
                    doubleq++;
                    break;
                case '\'':
                    singleq++;
                    break;
            }
        }
        String doublequote, singlequote;
        char quote;
        if (singleq < doubleq) {
            quote = '\'';
            doublequote = "\"";
            singlequote = "\\\'";
        } else {
            quote = '\"';
            doublequote = "\\\"";
            singlequote = "\'";
        }
        return strEscape(s, quote, doublequote, singlequote, "\\\\", outputCharsetEncoder);
    }

    static String regexpEscape(String s, CharsetEncoder outputCharsetEncoder) {
        return strEscape(s, '/', "\"", "'", "\\", outputCharsetEncoder);
    }

    static String escapeToDoubleQuotedJsString(String s) {
        return strEscape(s, '"', "\\\"", "\'", "\\\\", null);
    }

    static String regexpEscape(String s) {
        return regexpEscape(s, null);
    }

    static String strEscape(String s, char quote, String doublequoteEscape, String singlequoteEscape, String backslashEscape, CharsetEncoder outputCharsetEncoder) {
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append(quote);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch(c) {
                case '\0':
                    sb.append("\\0");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\\':
                    sb.append(backslashEscape);
                    break;
                case '\"':
                    sb.append(doublequoteEscape);
                    break;
                case '\'':
                    sb.append(singlequoteEscape);
                    break;
                case '>':
                    if (i >= 2 && ((s.charAt(i - 1) == '-' && s.charAt(i - 2) == '-') || (s.charAt(i - 1) == ']' && s.charAt(i - 2) == ']'))) {
                        sb.append("\\>");
                    } else {
                        sb.append(c);
                    }
                    break;
                case '<':
                    final String END_SCRIPT = "/script";
                    final String START_COMMENT = "!--";
                    if (s.regionMatches(true, i + 1, END_SCRIPT, 0, END_SCRIPT.length())) {
                        sb.append("<\\");
                    } else if (s.regionMatches(false, i + 1, START_COMMENT, 0, START_COMMENT.length())) {
                        sb.append("<\\");
                    } else {
                        sb.append(c);
                    }
                    break;
                default:
                    if (outputCharsetEncoder != null) {
                        if (outputCharsetEncoder.canEncode(c)) {
                            sb.append(c);
                        } else {
                            appendHexJavaScriptRepresentation(sb, c);
                        }
                    } else {
                        if (c > 0x1f && c <= 0x7f) {
                            sb.append(c);
                        } else {
                            appendHexJavaScriptRepresentation(sb, c);
                        }
                    }
            }
        }
        sb.append(quote);
        return sb.toString();
    }

    static String identifierEscape(String s) {
        if (NodeUtil.isLatin(s)) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 0x1F && c < 0x7F) {
                sb.append(c);
            } else {
                appendHexJavaScriptRepresentation(sb, c);
            }
        }
        return sb.toString();
    }

    private static int getNonEmptyChildCount(Node n, int maxCount) {
        int i = 0;
        Node c = n.getFirstChild();
        for (; c != null && i < maxCount; c = c.getNext()) {
            if (c.getType() == Token.BLOCK) {
                i += getNonEmptyChildCount(c, maxCount - i);
            } else if (c.getType() != Token.EMPTY) {
                i++;
            }
        }
        return i;
    }

    private static Node getFirstNonEmptyChild(Node n) {
        for (Node c = n.getFirstChild(); c != null; c = c.getNext()) {
            if (c.getType() == Token.BLOCK) {
                Node result = getFirstNonEmptyChild(c);
                if (result != null) {
                    return result;
                }
            } else if (c.getType() != Token.EMPTY) {
                return c;
            }
        }
        return null;
    }

    enum Context {

        STATEMENT,
        BEFORE_DANGLING_ELSE,
        START_OF_EXPR,
        PRESERVE_BLOCK,
        IN_FOR_INIT_CLAUSE,
        OTHER
    }

    private Context getContextForNonEmptyExpression(Context currentContext) {
        return currentContext == Context.BEFORE_DANGLING_ELSE ? Context.BEFORE_DANGLING_ELSE : Context.OTHER;
    }

    private Context getContextForNoInOperator(Context context) {
        return (context == Context.IN_FOR_INIT_CLAUSE ? Context.IN_FOR_INIT_CLAUSE : Context.OTHER);
    }

    private Context clearContextForNoInOperator(Context context) {
        return (context == Context.IN_FOR_INIT_CLAUSE ? Context.OTHER : context);
    }

    private static void appendHexJavaScriptRepresentation(StringBuilder sb, char c) {
        try {
            appendHexJavaScriptRepresentation(c, sb);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void appendHexJavaScriptRepresentation(int codePoint, Appendable out) throws IOException {
        if (Character.isSupplementaryCodePoint(codePoint)) {
            char[] surrogates = Character.toChars(codePoint);
            appendHexJavaScriptRepresentation(surrogates[0], out);
            appendHexJavaScriptRepresentation(surrogates[1], out);
            return;
        }
        out.append("\\u").append(HEX_CHARS[(codePoint >>> 12) & 0xf]).append(HEX_CHARS[(codePoint >>> 8) & 0xf]).append(HEX_CHARS[(codePoint >>> 4) & 0xf]).append(HEX_CHARS[codePoint & 0xf]);
    }
}
