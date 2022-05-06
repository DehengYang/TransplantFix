package org.jfree.chart.plot.dial;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.jfree.chart.util.HashUtilities;
import org.jfree.chart.util.PaintUtilities;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.chart.util.SerialUtilities;

public abstract class DialPointer extends AbstractDialLayer implements DialLayer, Cloneable, PublicCloneable, Serializable {

    double radius;

    int datasetIndex;

    protected DialPointer() {
        this(0);
    }

    protected DialPointer(int datasetIndex) {
        this.radius = 0.9;
        this.datasetIndex = datasetIndex;
    }

    public int getDatasetIndex() {
        return this.datasetIndex;
    }

    public void setDatasetIndex(int index) {
        this.datasetIndex = index;
        notifyListeners(new DialLayerChangeEvent(this));
    }

    public double getRadius() {
        return this.radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
        notifyListeners(new DialLayerChangeEvent(this));
    }

    public boolean isClippedToWindow() {
        return true;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DialPointer)) {
            return false;
        }
        DialPointer that = (DialPointer) obj;
        if (this.datasetIndex != that.datasetIndex) {
            return false;
        }
        if (this.radius != that.radius) {
            return false;
        }
        return super.equals(obj);
    }

    public int hashCode() {
        int result = 23;
        result = HashUtilities.hashCode(result, this.radius);
        return result;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static class Pin extends DialPointer {

        static final long serialVersionUID = -8445860485367689750L;

        private transient Paint paint;

        private transient Stroke stroke;

        public Pin() {
            this(0);
        }

        public Pin(int datasetIndex) {
            super(datasetIndex);
            this.paint = Color.red;
            this.stroke = new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL);
        }

        public Paint getPaint() {
            return this.paint;
        }

        public void setPaint(Paint paint) {
            if (paint == null) {
                throw new IllegalArgumentException("Null 'paint' argument.");
            }
            this.paint = paint;
            notifyListeners(new DialLayerChangeEvent(this));
        }

        public Stroke getStroke() {
            return this.stroke;
        }

        public void setStroke(Stroke stroke) {
            if (stroke == null) {
                throw new IllegalArgumentException("Null 'stroke' argument.");
            }
            this.stroke = stroke;
            notifyListeners(new DialLayerChangeEvent(this));
        }

        public void draw(Graphics2D g2, DialPlot plot, Rectangle2D frame, Rectangle2D view) {
            g2.setPaint(this.paint);
            g2.setStroke(this.stroke);
            Rectangle2D arcRect = DialPlot.rectangleByRadius(frame, this.radius, this.radius);
            double value = plot.getValue(this.datasetIndex);
            DialScale scale = plot.getScaleForDataset(this.datasetIndex);
            double angle = scale.valueToAngle(value);
            Arc2D arc = new Arc2D.Double(arcRect, angle, 0, Arc2D.OPEN);
            Point2D pt = arc.getEndPoint();
            Line2D line = new Line2D.Double(frame.getCenterX(), frame.getCenterY(), pt.getX(), pt.getY());
            g2.draw(line);
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof DialPointer.Pin)) {
                return false;
            }
            DialPointer.Pin that = (DialPointer.Pin) obj;
            if (!PaintUtilities.equal(this.paint, that.paint)) {
                return false;
            }
            if (!this.stroke.equals(that.stroke)) {
                return false;
            }
            return super.equals(obj);
        }

        public int hashCode() {
            int result = super.hashCode();
            result = HashUtilities.hashCode(result, this.paint);
            result = HashUtilities.hashCode(result, this.stroke);
            return result;
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.defaultWriteObject();
            SerialUtilities.writePaint(this.paint, stream);
            SerialUtilities.writeStroke(this.stroke, stream);
        }

        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            stream.defaultReadObject();
            this.paint = SerialUtilities.readPaint(stream);
            this.stroke = SerialUtilities.readStroke(stream);
        }
    }

    public static class Pointer extends DialPointer {

        static final long serialVersionUID = -4180500011963176960L;

        private double widthRadius;

        private transient Paint fillPaint;

        private transient Paint outlinePaint;

        public Pointer() {
            this(0);
        }

        public Pointer(int datasetIndex) {
            super(datasetIndex);
            this.widthRadius = 0.05;
            this.fillPaint = Color.gray;
            this.outlinePaint = Color.black;
        }

        public double getWidthRadius() {
            return this.widthRadius;
        }

        public void setWidthRadius(double radius) {
            this.widthRadius = radius;
            notifyListeners(new DialLayerChangeEvent(this));
        }

        public Paint getFillPaint() {
            return this.fillPaint;
        }

        public void setFillPaint(Paint paint) {
            if (paint == null) {
                throw new IllegalArgumentException("Null 'paint' argument.");
            }
            this.fillPaint = paint;
            notifyListeners(new DialLayerChangeEvent(this));
        }

        public Paint getOutlinePaint() {
            return this.outlinePaint;
        }

        public void setOutlinePaint(Paint paint) {
            if (paint == null) {
                throw new IllegalArgumentException("Null 'paint' argument.");
            }
            this.outlinePaint = paint;
            notifyListeners(new DialLayerChangeEvent(this));
        }

        public void draw(Graphics2D g2, DialPlot plot, Rectangle2D frame, Rectangle2D view) {
            g2.setPaint(Color.blue);
            g2.setStroke(new BasicStroke(1.0f));
            Rectangle2D lengthRect = DialPlot.rectangleByRadius(frame, this.radius, this.radius);
            Rectangle2D widthRect = DialPlot.rectangleByRadius(frame, this.widthRadius, this.widthRadius);
            double value = plot.getValue(this.datasetIndex);
            DialScale scale = plot.getScaleForDataset(this.datasetIndex);
            double angle = scale.valueToAngle(value);
            Arc2D arc1 = new Arc2D.Double(lengthRect, angle, 0, Arc2D.OPEN);
            Point2D pt1 = arc1.getEndPoint();
            Arc2D arc2 = new Arc2D.Double(widthRect, angle - 90.0, 180.0, Arc2D.OPEN);
            Point2D pt2 = arc2.getStartPoint();
            Point2D pt3 = arc2.getEndPoint();
            Arc2D arc3 = new Arc2D.Double(widthRect, angle - 180.0, 0.0, Arc2D.OPEN);
            Point2D pt4 = arc3.getStartPoint();
            GeneralPath gp = new GeneralPath();
            gp.moveTo((float) pt1.getX(), (float) pt1.getY());
            gp.lineTo((float) pt2.getX(), (float) pt2.getY());
            gp.lineTo((float) pt4.getX(), (float) pt4.getY());
            gp.lineTo((float) pt3.getX(), (float) pt3.getY());
            gp.closePath();
            g2.setPaint(this.fillPaint);
            g2.fill(gp);
            g2.setPaint(this.outlinePaint);
            Line2D line = new Line2D.Double(frame.getCenterX(), frame.getCenterY(), pt1.getX(), pt1.getY());
            g2.draw(line);
            line.setLine(pt2, pt3);
            g2.draw(line);
            line.setLine(pt3, pt1);
            g2.draw(line);
            line.setLine(pt2, pt1);
            g2.draw(line);
            line.setLine(pt2, pt4);
            g2.draw(line);
            line.setLine(pt3, pt4);
            g2.draw(line);
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof DialPointer.Pointer)) {
                return false;
            }
            DialPointer.Pointer that = (DialPointer.Pointer) obj;
            if (this.widthRadius != that.widthRadius) {
                return false;
            }
            if (!PaintUtilities.equal(this.fillPaint, that.fillPaint)) {
                return false;
            }
            if (!PaintUtilities.equal(this.outlinePaint, that.outlinePaint)) {
                return false;
            }
            return super.equals(obj);
        }

        public int hashCode() {
            int result = super.hashCode();
            result = HashUtilities.hashCode(result, this.widthRadius);
            result = HashUtilities.hashCode(result, this.fillPaint);
            result = HashUtilities.hashCode(result, this.outlinePaint);
            return result;
        }

        private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.defaultWriteObject();
            SerialUtilities.writePaint(this.fillPaint, stream);
            SerialUtilities.writePaint(this.outlinePaint, stream);
        }

        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            stream.defaultReadObject();
            this.fillPaint = SerialUtilities.readPaint(stream);
            this.outlinePaint = SerialUtilities.readPaint(stream);
        }
    }
}
