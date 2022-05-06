package com.google.javascript.jscomp;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.javascript.rhino.InputId;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class NodeTraversal {

    private final AbstractCompiler compiler;

    private final Callback callback;

    private Node curNode;

    public static final DiagnosticType NODE_TRAVERSAL_ERROR = DiagnosticType.error("JSC_NODE_TRAVERSAL_ERROR", "{0}");

    private final Deque<Scope> scopes = new ArrayDeque<Scope>();

    private final Deque<Node> scopeRoots = new ArrayDeque<Node>();

    Deque<ControlFlowGraph<Node>> cfgs = new LinkedList<ControlFlowGraph<Node>>();

    private String sourceName;

    private InputId inputId;

    private ScopeCreator scopeCreator;

    private ScopedCallback scopeCallback;

    public interface Callback {

        boolean shouldTraverse(NodeTraversal nodeTraversal, Node n, Node parent);

        void visit(NodeTraversal t, Node n, Node parent);
    }

    public interface ScopedCallback extends Callback {

        void enterScope(NodeTraversal t);

        void exitScope(NodeTraversal t);
    }

    public abstract static class AbstractPostOrderCallback implements Callback {

        @Override
        public final boolean shouldTraverse(NodeTraversal nodeTraversal, Node n, Node parent) {
            return true;
        }
    }

    public abstract static class AbstractScopedCallback implements ScopedCallback {

        @Override
        public final boolean shouldTraverse(NodeTraversal nodeTraversal, Node n, Node parent) {
            return true;
        }

        @Override
        public void enterScope(NodeTraversal t) {
        }

        @Override
        public void exitScope(NodeTraversal t) {
        }
    }

    public abstract static class AbstractShallowCallback implements Callback {

        @Override
        public final boolean shouldTraverse(NodeTraversal nodeTraversal, Node n, Node parent) {
            return parent == null || !parent.isFunction() || n == parent.getFirstChild();
        }
    }

    public abstract static class AbstractShallowStatementCallback implements Callback {

        @Override
        public final boolean shouldTraverse(NodeTraversal nodeTraversal, Node n, Node parent) {
            return parent == null || NodeUtil.isControlStructure(parent) || NodeUtil.isStatementBlock(parent);
        }
    }

    public abstract static class AbstractNodeTypePruningCallback implements Callback {

        private final Set<Integer> nodeTypes;

        private final boolean include;

        public AbstractNodeTypePruningCallback(Set<Integer> nodeTypes) {
            this(nodeTypes, true);
        }

        public AbstractNodeTypePruningCallback(Set<Integer> nodeTypes, boolean include) {
            this.nodeTypes = nodeTypes;
            this.include = include;
        }

        @Override
        public boolean shouldTraverse(NodeTraversal nodeTraversal, Node n, Node parent) {
            return include == nodeTypes.contains(n.getType());
        }
    }

    public NodeTraversal(AbstractCompiler compiler, Callback cb) {
        this(compiler, cb, new SyntacticScopeCreator(compiler));
    }

    public NodeTraversal(AbstractCompiler compiler, Callback cb, ScopeCreator scopeCreator) {
        this.callback = cb;
        if (cb instanceof ScopedCallback) {
            this.scopeCallback = (ScopedCallback) cb;
        }
        this.compiler = compiler;
        this.inputId = null;
        this.sourceName = "";
        this.scopeCreator = scopeCreator;
    }

    private void throwUnexpectedException(Exception unexpectedException) {
        String message = unexpectedException.getMessage();
        if (inputId != null) {
            message = unexpectedException.getMessage() + "\n" + formatNodeContext("Node", curNode) + (curNode == null ? "" : formatNodeContext("Parent", curNode.getParent()));
        }
        compiler.throwInternalError(message, unexpectedException);
    }

    private String formatNodeContext(String label, Node n) {
        if (n == null) {
            return "  " + label + ": NULL";
        }
        return "  " + label + "(" + n.toString(false, false, false) + "): " + formatNodePosition(n);
    }

    public void traverse(Node root) {
        try {
            inputId = NodeUtil.getInputId(root);
            sourceName = "";
            curNode = root;
            pushScope(root);
            traverseBranch(root, null);
            popScope();
        } catch (Exception unexpectedException) {
            throwUnexpectedException(unexpectedException);
        }
    }

    public void traverseRoots(Node... roots) {
        traverseRoots(Lists.newArrayList(roots));
    }

    public void traverseRoots(List<Node> roots) {
        if (roots.isEmpty()) {
            return;
        }
        try {
            Node scopeRoot = roots.get(0).getParent();
            Preconditions.checkState(scopeRoot != null);
            inputId = NodeUtil.getInputId(scopeRoot);
            sourceName = "";
            curNode = scopeRoot;
            pushScope(scopeRoot);
            for (Node root : roots) {
                Preconditions.checkState(root.getParent() == scopeRoot);
                traverseBranch(root, scopeRoot);
            }
            popScope();
        } catch (Exception unexpectedException) {
            throwUnexpectedException(unexpectedException);
        }
    }

    private static final String MISSING_SOURCE = "[source unknown]";

    private String formatNodePosition(Node n) {
        String sourceFileName = getBestSourceFileName(n);
        if (sourceFileName == null) {
            return MISSING_SOURCE + "\n";
        }
        int lineNumber = n.getLineno();
        int columnNumber = n.getCharno();
        String src = compiler.getSourceLine(sourceFileName, lineNumber);
        if (src == null) {
            src = MISSING_SOURCE;
        }
        return sourceFileName + ":" + lineNumber + ":" + columnNumber + "\n" + src + "\n";
    }

    void traverseWithScope(Node root, Scope s) {
        Preconditions.checkState(s.isGlobal());
        inputId = null;
        sourceName = "";
        curNode = root;
        pushScope(s);
        traverseBranch(root, null);
        popScope();
    }

    void traverseAtScope(Scope s) {
        Node n = s.getRootNode();
        if (n.isFunction()) {
            if (inputId == null) {
                inputId = NodeUtil.getInputId(n);
            }
            sourceName = getSourceName(n);
            curNode = n;
            pushScope(s);
            Node args = n.getFirstChild().getNext();
            Node body = args.getNext();
            traverseBranch(args, n);
            traverseBranch(body, n);
            popScope();
        } else {
            traverseWithScope(n, s);
        }
    }

    protected void traverseInnerNode(Node node, Node parent, Scope refinedScope) {
        Preconditions.checkNotNull(parent);
        if (refinedScope != null && getScope() != refinedScope) {
            curNode = node;
            pushScope(refinedScope);
            traverseBranch(node, parent);
            popScope();
        } else {
            traverseBranch(node, parent);
        }
    }

    public Compiler getCompiler() {
        return (Compiler) compiler;
    }

    public int getLineNumber() {
        Node cur = curNode;
        while (cur != null) {
            int line = cur.getLineno();
            if (line >= 0) {
                return line;
            }
            cur = cur.getParent();
        }
        return 0;
    }

    public String getSourceName() {
        return sourceName;
    }

    public CompilerInput getInput() {
        return compiler.getInput(inputId);
    }

    public JSModule getModule() {
        CompilerInput input = getInput();
        return input == null ? null : input.getModule();
    }

    public Node getCurrentNode() {
        return curNode;
    }

    public static void traverse(AbstractCompiler compiler, Node root, Callback cb) {
        NodeTraversal t = new NodeTraversal(compiler, cb);
        t.traverse(root);
    }

    public static void traverseRoots(AbstractCompiler compiler, List<Node> roots, Callback cb) {
        NodeTraversal t = new NodeTraversal(compiler, cb);
        t.traverseRoots(roots);
    }

    public static void traverseRoots(AbstractCompiler compiler, Callback cb, Node... roots) {
        NodeTraversal t = new NodeTraversal(compiler, cb);
        t.traverseRoots(roots);
    }

    @SuppressWarnings("fallthrough")
    private void traverseBranch(Node n, Node parent) {
        int type = n.getType();
        if (type == Token.SCRIPT) {
            inputId = n.getInputId();
            sourceName = getSourceName(n);
        }
        curNode = n;
        if (!callback.shouldTraverse(this, n, parent))
            return;
        switch(type) {
            case Token.FUNCTION:
                traverseFunction(n, parent);
                break;
            default:
                for (Node child = n.getFirstChild(); child != null; ) {
                    Node next = child.getNext();
                    traverseBranch(child, n);
                    child = next;
                }
                break;
        }
        curNode = n;
        callback.visit(this, n, parent);
    }

    private void traverseFunction(Node n, Node parent) {
        Preconditions.checkState(n.getChildCount() == 3);
        Preconditions.checkState(n.isFunction());
        final Node fnName = n.getFirstChild();
        boolean isFunctionExpression = (parent != null) && NodeUtil.isFunctionExpression(n);
        if (!isFunctionExpression) {
            traverseBranch(fnName, n);
        }
        curNode = n;
        pushScope(n);
        if (isFunctionExpression) {
            traverseBranch(fnName, n);
        }
        final Node args = fnName.getNext();
        final Node body = args.getNext();
        traverseBranch(args, n);
        Preconditions.checkState(body.getNext() == null && body.isBlock(), body);
        traverseBranch(body, n);
        popScope();
    }

    @SuppressWarnings("unchecked")
    public Node getEnclosingFunction() {
        if (scopes.size() + scopeRoots.size() < 2) {
            return null;
        } else {
            if (scopeRoots.isEmpty()) {
                return scopes.peek().getRootNode();
            } else {
                return scopeRoots.peek();
            }
        }
    }

    private void pushScope(Node node) {
        Preconditions.checkState(curNode != null);
        scopeRoots.push(node);
        cfgs.push(null);
        if (scopeCallback != null) {
            scopeCallback.enterScope(this);
        }
    }

    private void pushScope(Scope s) {
        Preconditions.checkState(curNode != null);
        scopes.push(s);
        cfgs.push(null);
        if (scopeCallback != null) {
            scopeCallback.enterScope(this);
        }
    }

    private void popScope() {
        if (scopeCallback != null) {
            scopeCallback.exitScope(this);
        }
        if (scopeRoots.isEmpty()) {
            scopes.pop();
        } else {
            scopeRoots.pop();
        }
        cfgs.pop();
    }

    public Scope getScope() {
        Scope scope = scopes.isEmpty() ? null : scopes.peek();
        if (scopeRoots.isEmpty()) {
            return scope;
        }
        Iterator<Node> it = scopeRoots.descendingIterator();
        while (it.hasNext()) {
            scope = scopeCreator.createScope(it.next(), scope);
            scopes.push(scope);
        }
        scopeRoots.clear();
        return scope;
    }

    public ControlFlowGraph<Node> getControlFlowGraph() {
        if (cfgs.peek() == null) {
            ControlFlowAnalysis cfa = new ControlFlowAnalysis(compiler, false, true);
            cfa.process(null, getScopeRoot());
            cfgs.pop();
            cfgs.push(cfa.getCfg());
        }
        return cfgs.peek();
    }

    public Node getScopeRoot() {
        if (scopeRoots.isEmpty()) {
            return scopes.peek().getRootNode();
        } else {
            return scopeRoots.peek();
        }
    }

    boolean inGlobalScope() {
        return getScopeDepth() <= 1;
    }

    int getScopeDepth() {
        return scopes.size() + scopeRoots.size();
    }

    public boolean hasScope() {
        return !(scopes.isEmpty() && scopeRoots.isEmpty());
    }

    public void report(Node n, DiagnosticType diagnosticType, String... arguments) {
        JSError error = JSError.make(getBestSourceFileName(n), n, diagnosticType, arguments);
        compiler.report(error);
    }

    private static String getSourceName(Node n) {
        String name = n.getSourceFileName();
        return name == null ? "" : name;
    }

    InputId getInputId() {
        return inputId;
    }

    public JSError makeError(Node n, CheckLevel level, DiagnosticType type, String... arguments) {
        return JSError.make(getBestSourceFileName(n), n, level, type, arguments);
    }

    public JSError makeError(Node n, DiagnosticType type, String... arguments) {
        return JSError.make(getBestSourceFileName(n), n, type, arguments);
    }

    private String getBestSourceFileName(Node n) {
        return n == null ? sourceName : n.getSourceFileName();
    }
}
