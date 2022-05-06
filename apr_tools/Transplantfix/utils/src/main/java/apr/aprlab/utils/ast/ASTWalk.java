package apr.aprlab.utils.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Javadoc;
import apr.aprlab.utils.general.ExceptionUtil;

public class ASTWalk {

    public static final Logger logger = LogManager.getLogger(ASTWalk.class);

    public enum WalkOrder {

        PREORDER, POSTORDER, BREADTHFIRST
    }

    public static void walk(ASTNode node, Consumer<ASTNode> consumer) {
        walk(node, WalkOrder.PREORDER, consumer);
    }

    public static void walk(ASTNode node, WalkOrder wo, Consumer<ASTNode> consumer) {
        Iterable<ASTNode> nodeIterable = getNodeIterable(wo, node);
        for (ASTNode curNode : nodeIterable) {
            consumer.accept(curNode);
        }
    }

    public static Iterator<ASTNode> getNodeIterator(WalkOrder walkOrder, ASTNode node) {
        switch(walkOrder) {
            case PREORDER:
                return new PreOrderIterator(node);
            case POSTORDER:
                return new PostOrderIterator(node);
            case BREADTHFIRST:
                return new BreadthFirstIterator(node);
            default:
                ExceptionUtil.raise("unsupported walk order: %s", walkOrder);
        }
        return null;
    }

    public static Iterator<ASTNode> getNodeIterator(ASTNode node) {
        return getNodeIterator(WalkOrder.PREORDER, node);
    }

    public static Iterable<ASTNode> getNodeIterable(ASTNode node) {
        return getNodeIterable(WalkOrder.PREORDER, node);
    }

    public static Iterable<ASTNode> getNodeIterable(WalkOrder walkOrder, ASTNode node) {
        switch(walkOrder) {
            case PREORDER:
                return () -> new PreOrderIterator(node);
            case POSTORDER:
                return () -> new PostOrderIterator(node);
            default:
                ExceptionUtil.raise("unsupported walk order: %s", walkOrder);
        }
        return null;
    }

    public static <T extends ASTNode> List<T> findall(ASTNode node, Class<T> nodeType, Consumer<T> consumer) {
        List<T> found = new ArrayList<>();
        walk(node, nodeType, consumer);
        return found;
    }

    public static <T extends ASTNode> void walk(ASTNode astNode, Class<T> nodeType, Consumer<T> consumer) {
        walk(astNode, WalkOrder.PREORDER, node -> {
            if (nodeType.isAssignableFrom(node.getClass())) {
                consumer.accept(nodeType.cast(node));
            }
        });
    }

    public static class PreOrderIterator implements Iterator<ASTNode> {

        private final Stack<ASTNode> stack = new Stack<>();

        public PreOrderIterator(ASTNode node) {
            stack.add(node);
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public ASTNode next() {
            ASTNode next = stack.pop();
            List<ASTNode> children = ASTUtil.getChildren(next);
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.add(children.get(i));
            }
            return next;
        }
    }

    public static class BreadthFirstIterator implements Iterator<ASTNode> {

        private final Queue<ASTNode> queue = new LinkedList<>();

        public BreadthFirstIterator(ASTNode node) {
            queue.add(node);
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }

        @Override
        public ASTNode next() {
            ASTNode next = queue.remove();
            queue.addAll(ASTUtil.getChildren(next));
            return next;
        }
    }

    public static class PostOrderIterator implements Iterator<ASTNode> {

        private final Stack<List<ASTNode>> nodesStack = new Stack<>();

        private final Stack<Integer> cursorStack = new Stack<>();

        private final ASTNode root;

        private boolean hasNext = true;

        public PostOrderIterator(ASTNode root) {
            this.root = root;
            fillStackToLeaf(root);
        }

        private void fillStackToLeaf(ASTNode node) {
            while (true) {
                List<ASTNode> childNodes = ASTUtil.getChildren(node);
                if (childNodes.isEmpty()) {
                    break;
                }
                nodesStack.push(childNodes);
                cursorStack.push(0);
                node = childNodes.get(0);
            }
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public ASTNode next() {
            final List<ASTNode> nodes = nodesStack.peek();
            final int cursor = cursorStack.peek();
            final boolean levelHasNext = cursor < nodes.size();
            if (levelHasNext) {
                ASTNode node = nodes.get(cursor);
                fillStackToLeaf(node);
                return nextFromLevel();
            } else {
                nodesStack.pop();
                cursorStack.pop();
                hasNext = !nodesStack.empty();
                if (hasNext) {
                    return nextFromLevel();
                }
                return root;
            }
        }

        private ASTNode nextFromLevel() {
            final List<ASTNode> nodes = nodesStack.peek();
            final int cursor = cursorStack.pop();
            cursorStack.push(cursor + 1);
            return nodes.get(cursor);
        }
    }

    public static List<ASTNode> getNodeIterableExcludeJavadoc(ASTNode node) {
        List<ASTNode> visited = new ArrayList<ASTNode>();
        Stack<ASTNode> stack = new Stack<ASTNode>();
        stack.push(node);
        while (!stack.isEmpty()) {
            ASTNode popNode = stack.pop();
            visited.add(popNode);
            if (!(popNode instanceof Javadoc)) {
                List<ASTNode> children = ASTUtil.getChildren(popNode);
                for (int i = children.size() - 1; i >= 0; i--) {
                    stack.add(children.get(i));
                }
            }
        }
        return visited;
    }
}
