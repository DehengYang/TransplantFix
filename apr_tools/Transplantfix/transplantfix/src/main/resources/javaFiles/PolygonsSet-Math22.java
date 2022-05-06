package org.apache.commons.math3.geometry.euclidean.twod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.exception.MathInternalError;
import org.apache.commons.math3.geometry.euclidean.oned.Euclidean1D;
import org.apache.commons.math3.geometry.euclidean.oned.Interval;
import org.apache.commons.math3.geometry.euclidean.oned.IntervalsSet;
import org.apache.commons.math3.geometry.euclidean.oned.Vector1D;
import org.apache.commons.math3.geometry.partitioning.AbstractSubHyperplane;
import org.apache.commons.math3.geometry.partitioning.BSPTree;
import org.apache.commons.math3.geometry.partitioning.BSPTreeVisitor;
import org.apache.commons.math3.geometry.partitioning.BoundaryAttribute;
import org.apache.commons.math3.geometry.partitioning.SubHyperplane;
import org.apache.commons.math3.geometry.partitioning.AbstractRegion;
import org.apache.commons.math3.geometry.partitioning.utilities.AVLTree;
import org.apache.commons.math3.geometry.partitioning.utilities.OrderedTuple;
import org.apache.commons.math3.util.FastMath;

public class PolygonsSet extends AbstractRegion<Euclidean2D, Euclidean1D> {

    private Vector2D[][] vertices;

    public PolygonsSet() {
        super();
    }

    public PolygonsSet(final BSPTree<Euclidean2D> tree) {
        super(tree);
    }

    public PolygonsSet(final Collection<SubHyperplane<Euclidean2D>> boundary) {
        super(boundary);
    }

    public PolygonsSet(final double xMin, final double xMax, final double yMin, final double yMax) {
        super(boxBoundary(xMin, xMax, yMin, yMax));
    }

    private static Line[] boxBoundary(final double xMin, final double xMax, final double yMin, final double yMax) {
        final Vector2D minMin = new Vector2D(xMin, yMin);
        final Vector2D minMax = new Vector2D(xMin, yMax);
        final Vector2D maxMin = new Vector2D(xMax, yMin);
        final Vector2D maxMax = new Vector2D(xMax, yMax);
        return new Line[] { new Line(minMin, maxMin), new Line(maxMin, maxMax), new Line(maxMax, minMax), new Line(minMax, minMin) };
    }

    @Override
    public PolygonsSet buildNew(final BSPTree<Euclidean2D> tree) {
        return new PolygonsSet(tree);
    }

    @Override
    protected void computeGeometricalProperties() {
        final Vector2D[][] v = getVertices();
        if (v.length == 0) {
            final BSPTree<Euclidean2D> tree = getTree(false);
            if ((Boolean) tree.getAttribute()) {
                setSize(Double.POSITIVE_INFINITY);
                setBarycenter(Vector2D.NaN);
            } else {
                setSize(0);
                setBarycenter(new Vector2D(0, 0));
            }
        } else if (v[0][0] == null) {
            setSize(Double.POSITIVE_INFINITY);
            setBarycenter(Vector2D.NaN);
        } else {
            double sum = 0;
            double sumX = 0;
            double sumY = 0;
            for (Vector2D[] loop : v) {
                double x1 = loop[loop.length - 1].getX();
                double y1 = loop[loop.length - 1].getY();
                for (final Vector2D point : loop) {
                    final double x0 = x1;
                    final double y0 = y1;
                    x1 = point.getX();
                    y1 = point.getY();
                    final double factor = x0 * y1 - y0 * x1;
                    sum += factor;
                    sumX += factor * (x0 + x1);
                    sumY += factor * (y0 + y1);
                }
            }
            if (sum < 0) {
                setSize(Double.POSITIVE_INFINITY);
                setBarycenter(Vector2D.NaN);
            } else {
                setSize(sum / 2);
                setBarycenter(new Vector2D(sumX / (3 * sum), sumY / (3 * sum)));
            }
        }
    }

    public Vector2D[][] getVertices() {
        if (vertices == null) {
            if (getTree(false).getCut() == null) {
                vertices = new Vector2D[0][];
            } else {
                final SegmentsBuilder visitor = new SegmentsBuilder();
                getTree(true).visit(visitor);
                final AVLTree<ComparableSegment> sorted = visitor.getSorted();
                final ArrayList<List<ComparableSegment>> loops = new ArrayList<List<ComparableSegment>>();
                while (!sorted.isEmpty()) {
                    final AVLTree<ComparableSegment>.Node node = sorted.getSmallest();
                    final List<ComparableSegment> loop = followLoop(node, sorted);
                    if (loop != null) {
                        loops.add(loop);
                    }
                }
                vertices = new Vector2D[loops.size()][];
                int i = 0;
                for (final List<ComparableSegment> loop : loops) {
                    if (loop.size() < 2) {
                        final Line line = loop.get(0).getLine();
                        vertices[i++] = new Vector2D[] { null, line.toSpace(new Vector1D(-Float.MAX_VALUE)), line.toSpace(new Vector1D(+Float.MAX_VALUE)) };
                    } else if (loop.get(0).getStart() == null) {
                        final Vector2D[] array = new Vector2D[loop.size() + 2];
                        int j = 0;
                        for (Segment segment : loop) {
                            if (j == 0) {
                                double x = segment.getLine().toSubSpace(segment.getEnd()).getX();
                                x -= FastMath.max(1.0, FastMath.abs(x / 2));
                                array[j++] = null;
                                array[j++] = segment.getLine().toSpace(new Vector1D(x));
                            }
                            if (j < (array.length - 1)) {
                                array[j++] = segment.getEnd();
                            }
                            if (j == (array.length - 1)) {
                                double x = segment.getLine().toSubSpace(segment.getStart()).getX();
                                x += FastMath.max(1.0, FastMath.abs(x / 2));
                                array[j++] = segment.getLine().toSpace(new Vector1D(x));
                            }
                        }
                        vertices[i++] = array;
                    } else {
                        final Vector2D[] array = new Vector2D[loop.size()];
                        int j = 0;
                        for (Segment segment : loop) {
                            array[j++] = segment.getStart();
                        }
                        vertices[i++] = array;
                    }
                }
            }
        }
        return vertices.clone();
    }

    private List<ComparableSegment> followLoop(final AVLTree<ComparableSegment>.Node node, final AVLTree<ComparableSegment> sorted) {
        final ArrayList<ComparableSegment> loop = new ArrayList<ComparableSegment>();
        ComparableSegment segment = node.getElement();
        loop.add(segment);
        final Vector2D globalStart = segment.getStart();
        Vector2D end = segment.getEnd();
        node.delete();
        final boolean open = segment.getStart() == null;
        while ((end != null) && (open || (globalStart.distance(end) > 1.0e-10))) {
            AVLTree<ComparableSegment>.Node selectedNode = null;
            ComparableSegment selectedSegment = null;
            double selectedDistance = Double.POSITIVE_INFINITY;
            final ComparableSegment lowerLeft = new ComparableSegment(end, -1.0e-10, -1.0e-10);
            final ComparableSegment upperRight = new ComparableSegment(end, +1.0e-10, +1.0e-10);
            for (AVLTree<ComparableSegment>.Node n = sorted.getNotSmaller(lowerLeft); (n != null) && (n.getElement().compareTo(upperRight) <= 0); n = n.getNext()) {
                segment = n.getElement();
                final double distance = end.distance(segment.getStart());
                if (distance < selectedDistance) {
                    selectedNode = n;
                    selectedSegment = segment;
                    selectedDistance = distance;
                }
            }
            if (selectedDistance > 1.0e-10) {
                return null;
            }
            end = selectedSegment.getEnd();
            loop.add(selectedSegment);
            selectedNode.delete();
        }
        if ((loop.size() == 2) && !open) {
            return null;
        }
        if ((end == null) && !open) {
            throw new MathInternalError();
        }
        return loop;
    }

    private static class ComparableSegment extends Segment implements Comparable<ComparableSegment> {

        private OrderedTuple sortingKey;

        public ComparableSegment(final Vector2D start, final Vector2D end, final Line line) {
            super(start, end, line);
            sortingKey = (start == null) ? new OrderedTuple(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY) : new OrderedTuple(start.getX(), start.getY());
        }

        public ComparableSegment(final Vector2D start, final double dx, final double dy) {
            super(null, null, null);
            sortingKey = new OrderedTuple(start.getX() + dx, start.getY() + dy);
        }

        public int compareTo(final ComparableSegment o) {
            return sortingKey.compareTo(o.sortingKey);
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            } else if (other instanceof ComparableSegment) {
                return compareTo((ComparableSegment) other) == 0;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return getStart().hashCode() ^ getEnd().hashCode() ^ getLine().hashCode() ^ sortingKey.hashCode();
        }
    }

    private static class SegmentsBuilder implements BSPTreeVisitor<Euclidean2D> {

        private AVLTree<ComparableSegment> sorted;

        public SegmentsBuilder() {
            sorted = new AVLTree<ComparableSegment>();
        }

        public Order visitOrder(final BSPTree<Euclidean2D> node) {
            return Order.MINUS_SUB_PLUS;
        }

        public void visitInternalNode(final BSPTree<Euclidean2D> node) {
            @SuppressWarnings("unchecked")
            final BoundaryAttribute<Euclidean2D> attribute = (BoundaryAttribute<Euclidean2D>) node.getAttribute();
            if (attribute.getPlusOutside() != null) {
                addContribution(attribute.getPlusOutside(), false);
            }
            if (attribute.getPlusInside() != null) {
                addContribution(attribute.getPlusInside(), true);
            }
        }

        public void visitLeafNode(final BSPTree<Euclidean2D> node) {
        }

        private void addContribution(final SubHyperplane<Euclidean2D> sub, final boolean reversed) {
            @SuppressWarnings("unchecked")
            final AbstractSubHyperplane<Euclidean2D, Euclidean1D> absSub = (AbstractSubHyperplane<Euclidean2D, Euclidean1D>) sub;
            final Line line = (Line) sub.getHyperplane();
            final List<Interval> intervals = ((IntervalsSet) absSub.getRemainingRegion()).asList();
            for (final Interval i : intervals) {
                final Vector2D start = Double.isInfinite(i.getLower()) ? null : (Vector2D) line.toSpace(new Vector1D(i.getLower()));
                final Vector2D end = Double.isInfinite(i.getUpper()) ? null : (Vector2D) line.toSpace(new Vector1D(i.getUpper()));
                if (reversed) {
                    sorted.insert(new ComparableSegment(end, start, line.getReverse()));
                } else {
                    sorted.insert(new ComparableSegment(start, end, line));
                }
            }
        }

        public AVLTree<ComparableSegment> getSorted() {
            return sorted;
        }
    }
}
