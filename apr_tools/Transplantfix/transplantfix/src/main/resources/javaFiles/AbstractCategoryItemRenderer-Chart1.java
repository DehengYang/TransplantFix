package org.jfree.chart.renderer.category;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.RenderingSource;
import org.jfree.chart.annotations.CategoryAnnotation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.labels.CategorySeriesLabelGenerator;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategorySeriesLabelGenerator;
import org.jfree.chart.plot.CategoryCrosshairState;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.text.TextUtilities;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.chart.util.GradientPaintTransformer;
import org.jfree.chart.util.Layer;
import org.jfree.chart.util.LengthAdjustmentType;
import org.jfree.chart.util.ObjectList;
import org.jfree.chart.util.ObjectUtilities;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.chart.util.RectangleAnchor;
import org.jfree.chart.util.RectangleEdge;
import org.jfree.chart.util.RectangleInsets;
import org.jfree.chart.util.SortOrder;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.CategoryDatasetSelectionState;
import org.jfree.data.category.SelectableCategoryDataset;
import org.jfree.data.general.DatasetUtilities;

public abstract class AbstractCategoryItemRenderer extends AbstractRenderer implements CategoryItemRenderer, Cloneable, PublicCloneable, Serializable {

    private static final long serialVersionUID = 1247553218442497391L;

    private CategoryPlot plot;

    private ObjectList itemLabelGeneratorList;

    private CategoryItemLabelGenerator baseItemLabelGenerator;

    private ObjectList toolTipGeneratorList;

    private CategoryToolTipGenerator baseToolTipGenerator;

    private ObjectList urlGeneratorList;

    private CategoryURLGenerator baseURLGenerator;

    private CategorySeriesLabelGenerator legendItemLabelGenerator;

    private CategorySeriesLabelGenerator legendItemToolTipGenerator;

    private CategorySeriesLabelGenerator legendItemURLGenerator;

    private List backgroundAnnotations;

    private List foregroundAnnotations;

    private transient int rowCount;

    private transient int columnCount;

    protected AbstractCategoryItemRenderer() {
        this.itemLabelGeneratorList = new ObjectList();
        this.toolTipGeneratorList = new ObjectList();
        this.urlGeneratorList = new ObjectList();
        this.legendItemLabelGenerator = new StandardCategorySeriesLabelGenerator();
        this.backgroundAnnotations = new ArrayList();
        this.foregroundAnnotations = new ArrayList();
    }

    public int getPassCount() {
        return 1;
    }

    public CategoryPlot getPlot() {
        return this.plot;
    }

    public void setPlot(CategoryPlot plot) {
        if (plot == null) {
            throw new IllegalArgumentException("Null 'plot' argument.");
        }
        this.plot = plot;
    }

    public CategoryItemLabelGenerator getItemLabelGenerator(int row, int column, boolean selected) {
        CategoryItemLabelGenerator generator = (CategoryItemLabelGenerator) this.itemLabelGeneratorList.get(row);
        if (generator == null) {
            generator = this.baseItemLabelGenerator;
        }
        return generator;
    }

    public CategoryItemLabelGenerator getSeriesItemLabelGenerator(int series) {
        return (CategoryItemLabelGenerator) this.itemLabelGeneratorList.get(series);
    }

    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator) {
        setSeriesItemLabelGenerator(series, generator, true);
    }

    public void setSeriesItemLabelGenerator(int series, CategoryItemLabelGenerator generator, boolean notify) {
        this.itemLabelGeneratorList.set(series, generator);
        if (notify) {
            notifyListeners(new RendererChangeEvent(this));
        }
    }

    public CategoryItemLabelGenerator getBaseItemLabelGenerator() {
        return this.baseItemLabelGenerator;
    }

    public void setBaseItemLabelGenerator(CategoryItemLabelGenerator generator) {
        setBaseItemLabelGenerator(generator, true);
    }

    public void setBaseItemLabelGenerator(CategoryItemLabelGenerator generator, boolean notify) {
        this.baseItemLabelGenerator = generator;
        if (notify) {
            notifyListeners(new RendererChangeEvent(this));
        }
    }

    public CategoryToolTipGenerator getToolTipGenerator(int row, int column, boolean selected) {
        CategoryToolTipGenerator result = null;
        result = getSeriesToolTipGenerator(row);
        if (result == null) {
            result = this.baseToolTipGenerator;
        }
        return result;
    }

    public CategoryToolTipGenerator getSeriesToolTipGenerator(int series) {
        return (CategoryToolTipGenerator) this.toolTipGeneratorList.get(series);
    }

    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator) {
        setSeriesToolTipGenerator(series, generator, true);
    }

    public void setSeriesToolTipGenerator(int series, CategoryToolTipGenerator generator, boolean notify) {
        this.toolTipGeneratorList.set(series, generator);
        if (notify) {
            notifyListeners(new RendererChangeEvent(this));
        }
    }

    public CategoryToolTipGenerator getBaseToolTipGenerator() {
        return this.baseToolTipGenerator;
    }

    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator) {
        setBaseToolTipGenerator(generator, true);
    }

    public void setBaseToolTipGenerator(CategoryToolTipGenerator generator, boolean notify) {
        this.baseToolTipGenerator = generator;
        if (notify) {
            notifyListeners(new RendererChangeEvent(this));
        }
    }

    public CategoryURLGenerator getURLGenerator(int row, int column, boolean selected) {
        CategoryURLGenerator generator = (CategoryURLGenerator) this.urlGeneratorList.get(row);
        if (generator == null) {
            generator = this.baseURLGenerator;
        }
        return generator;
    }

    public CategoryURLGenerator getSeriesURLGenerator(int series) {
        return (CategoryURLGenerator) this.urlGeneratorList.get(series);
    }

    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator) {
        setSeriesURLGenerator(series, generator, true);
    }

    public void setSeriesURLGenerator(int series, CategoryURLGenerator generator, boolean notify) {
        this.urlGeneratorList.set(series, generator);
        if (notify) {
            notifyListeners(new RendererChangeEvent(this));
        }
    }

    public CategoryURLGenerator getBaseURLGenerator() {
        return this.baseURLGenerator;
    }

    public void setBaseURLGenerator(CategoryURLGenerator generator) {
        setBaseURLGenerator(generator, true);
    }

    public void setBaseURLGenerator(CategoryURLGenerator generator, boolean notify) {
        this.baseURLGenerator = generator;
        if (notify) {
            notifyListeners(new RendererChangeEvent(this));
        }
    }

    public void addAnnotation(CategoryAnnotation annotation) {
        addAnnotation(annotation, Layer.FOREGROUND);
    }

    public void addAnnotation(CategoryAnnotation annotation, Layer layer) {
        if (annotation == null) {
            throw new IllegalArgumentException("Null 'annotation' argument.");
        }
        if (layer.equals(Layer.FOREGROUND)) {
            this.foregroundAnnotations.add(annotation);
            notifyListeners(new RendererChangeEvent(this));
        } else if (layer.equals(Layer.BACKGROUND)) {
            this.backgroundAnnotations.add(annotation);
            notifyListeners(new RendererChangeEvent(this));
        } else {
            throw new RuntimeException("Unknown layer.");
        }
    }

    public boolean removeAnnotation(CategoryAnnotation annotation) {
        boolean removed = this.foregroundAnnotations.remove(annotation);
        removed = removed & this.backgroundAnnotations.remove(annotation);
        notifyListeners(new RendererChangeEvent(this));
        return removed;
    }

    public void removeAnnotations() {
        this.foregroundAnnotations.clear();
        this.backgroundAnnotations.clear();
        notifyListeners(new RendererChangeEvent(this));
    }

    public CategorySeriesLabelGenerator getLegendItemLabelGenerator() {
        return this.legendItemLabelGenerator;
    }

    public void setLegendItemLabelGenerator(CategorySeriesLabelGenerator generator) {
        if (generator == null) {
            throw new IllegalArgumentException("Null 'generator' argument.");
        }
        this.legendItemLabelGenerator = generator;
        fireChangeEvent();
    }

    public CategorySeriesLabelGenerator getLegendItemToolTipGenerator() {
        return this.legendItemToolTipGenerator;
    }

    public void setLegendItemToolTipGenerator(CategorySeriesLabelGenerator generator) {
        this.legendItemToolTipGenerator = generator;
        fireChangeEvent();
    }

    public CategorySeriesLabelGenerator getLegendItemURLGenerator() {
        return this.legendItemURLGenerator;
    }

    public void setLegendItemURLGenerator(CategorySeriesLabelGenerator generator) {
        this.legendItemURLGenerator = generator;
        fireChangeEvent();
    }

    public int getRowCount() {
        return this.rowCount;
    }

    public int getColumnCount() {
        return this.columnCount;
    }

    protected CategoryItemRendererState createState(PlotRenderingInfo info) {
        CategoryItemRendererState state = new CategoryItemRendererState(info);
        int[] visibleSeriesTemp = new int[this.rowCount];
        int visibleSeriesCount = 0;
        for (int row = 0; row < this.rowCount; row++) {
            if (isSeriesVisible(row)) {
                visibleSeriesTemp[visibleSeriesCount] = row;
                visibleSeriesCount++;
            }
        }
        int[] visibleSeries = new int[visibleSeriesCount];
        System.arraycopy(visibleSeriesTemp, 0, visibleSeries, 0, visibleSeriesCount);
        state.setVisibleSeriesArray(visibleSeries);
        return state;
    }

    public CategoryItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, CategoryPlot plot, CategoryDataset dataset, PlotRenderingInfo info) {
        setPlot(plot);
        if (dataset != null) {
            this.rowCount = dataset.getRowCount();
            this.columnCount = dataset.getColumnCount();
        } else {
            this.rowCount = 0;
            this.columnCount = 0;
        }
        CategoryItemRendererState state = createState(info);
        CategoryDatasetSelectionState selectionState = null;
        if (dataset instanceof SelectableCategoryDataset) {
            SelectableCategoryDataset scd = (SelectableCategoryDataset) dataset;
            selectionState = scd.getSelectionState();
        }
        if (selectionState == null && info != null) {
            ChartRenderingInfo cri = info.getOwner();
            if (cri != null) {
                RenderingSource rs = cri.getRenderingSource();
                selectionState = (CategoryDatasetSelectionState) rs.getSelectionState(dataset);
            }
        }
        state.setSelectionState(selectionState);
        return state;
    }

    public Range findRangeBounds(CategoryDataset dataset) {
        return findRangeBounds(dataset, false);
    }

    protected Range findRangeBounds(CategoryDataset dataset, boolean includeInterval) {
        if (dataset == null) {
            return null;
        }
        if (getDataBoundsIncludesVisibleSeriesOnly()) {
            List visibleSeriesKeys = new ArrayList();
            int seriesCount = dataset.getRowCount();
            for (int s = 0; s < seriesCount; s++) {
                if (isSeriesVisible(s)) {
                    visibleSeriesKeys.add(dataset.getRowKey(s));
                }
            }
            return DatasetUtilities.findRangeBounds(dataset, visibleSeriesKeys, includeInterval);
        } else {
            return DatasetUtilities.findRangeBounds(dataset, includeInterval);
        }
    }

    public double getItemMiddle(Comparable rowKey, Comparable columnKey, CategoryDataset dataset, CategoryAxis axis, Rectangle2D area, RectangleEdge edge) {
        return axis.getCategoryMiddle(columnKey, dataset.getColumnKeys(), area, edge);
    }

    public void drawBackground(Graphics2D g2, CategoryPlot plot, Rectangle2D dataArea) {
        plot.drawBackground(g2, dataArea);
    }

    public void drawOutline(Graphics2D g2, CategoryPlot plot, Rectangle2D dataArea) {
        plot.drawOutline(g2, dataArea);
    }

    public void drawDomainLine(Graphics2D g2, CategoryPlot plot, Rectangle2D dataArea, double value, Paint paint, Stroke stroke) {
        if (paint == null) {
            throw new IllegalArgumentException("Null 'paint' argument.");
        }
        if (stroke == null) {
            throw new IllegalArgumentException("Null 'stroke' argument.");
        }
        Line2D line = null;
        PlotOrientation orientation = plot.getOrientation();
        if (orientation == PlotOrientation.HORIZONTAL) {
            line = new Line2D.Double(dataArea.getMinX(), value, dataArea.getMaxX(), value);
        } else if (orientation == PlotOrientation.VERTICAL) {
            line = new Line2D.Double(value, dataArea.getMinY(), value, dataArea.getMaxY());
        }
        g2.setPaint(paint);
        g2.setStroke(stroke);
        g2.draw(line);
    }

    public void drawRangeLine(Graphics2D g2, CategoryPlot plot, ValueAxis axis, Rectangle2D dataArea, double value, Paint paint, Stroke stroke) {
        Range range = axis.getRange();
        if (!range.contains(value)) {
            return;
        }
        PlotOrientation orientation = plot.getOrientation();
        Line2D line = null;
        double v = axis.valueToJava2D(value, dataArea, plot.getRangeAxisEdge());
        if (orientation == PlotOrientation.HORIZONTAL) {
            line = new Line2D.Double(v, dataArea.getMinY(), v, dataArea.getMaxY());
        } else if (orientation == PlotOrientation.VERTICAL) {
            line = new Line2D.Double(dataArea.getMinX(), v, dataArea.getMaxX(), v);
        }
        g2.setPaint(paint);
        g2.setStroke(stroke);
        g2.draw(line);
    }

    public void drawDomainMarker(Graphics2D g2, CategoryPlot plot, CategoryAxis axis, CategoryMarker marker, Rectangle2D dataArea) {
        Comparable category = marker.getKey();
        CategoryDataset dataset = plot.getDataset(plot.getIndexOf(this));
        int columnIndex = dataset.getColumnIndex(category);
        if (columnIndex < 0) {
            return;
        }
        final Composite savedComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, marker.getAlpha()));
        PlotOrientation orientation = plot.getOrientation();
        Rectangle2D bounds = null;
        if (marker.getDrawAsLine()) {
            double v = axis.getCategoryMiddle(columnIndex, dataset.getColumnCount(), dataArea, plot.getDomainAxisEdge());
            Line2D line = null;
            if (orientation == PlotOrientation.HORIZONTAL) {
                line = new Line2D.Double(dataArea.getMinX(), v, dataArea.getMaxX(), v);
            } else if (orientation == PlotOrientation.VERTICAL) {
                line = new Line2D.Double(v, dataArea.getMinY(), v, dataArea.getMaxY());
            }
            g2.setPaint(marker.getPaint());
            g2.setStroke(marker.getStroke());
            g2.draw(line);
            bounds = line.getBounds2D();
        } else {
            double v0 = axis.getCategoryStart(columnIndex, dataset.getColumnCount(), dataArea, plot.getDomainAxisEdge());
            double v1 = axis.getCategoryEnd(columnIndex, dataset.getColumnCount(), dataArea, plot.getDomainAxisEdge());
            Rectangle2D area = null;
            if (orientation == PlotOrientation.HORIZONTAL) {
                area = new Rectangle2D.Double(dataArea.getMinX(), v0, dataArea.getWidth(), (v1 - v0));
            } else if (orientation == PlotOrientation.VERTICAL) {
                area = new Rectangle2D.Double(v0, dataArea.getMinY(), (v1 - v0), dataArea.getHeight());
            }
            g2.setPaint(marker.getPaint());
            g2.fill(area);
            bounds = area;
        }
        String label = marker.getLabel();
        RectangleAnchor anchor = marker.getLabelAnchor();
        if (label != null) {
            Font labelFont = marker.getLabelFont();
            g2.setFont(labelFont);
            g2.setPaint(marker.getLabelPaint());
            Point2D coordinates = calculateDomainMarkerTextAnchorPoint(g2, orientation, dataArea, bounds, marker.getLabelOffset(), marker.getLabelOffsetType(), anchor);
            TextUtilities.drawAlignedString(label, g2, (float) coordinates.getX(), (float) coordinates.getY(), marker.getLabelTextAnchor());
        }
        g2.setComposite(savedComposite);
    }

    public void drawRangeMarker(Graphics2D g2, CategoryPlot plot, ValueAxis axis, Marker marker, Rectangle2D dataArea) {
        if (marker instanceof ValueMarker) {
            ValueMarker vm = (ValueMarker) marker;
            double value = vm.getValue();
            Range range = axis.getRange();
            if (!range.contains(value)) {
                return;
            }
            final Composite savedComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, marker.getAlpha()));
            PlotOrientation orientation = plot.getOrientation();
            double v = axis.valueToJava2D(value, dataArea, plot.getRangeAxisEdge());
            Line2D line = null;
            if (orientation == PlotOrientation.HORIZONTAL) {
                line = new Line2D.Double(v, dataArea.getMinY(), v, dataArea.getMaxY());
            } else if (orientation == PlotOrientation.VERTICAL) {
                line = new Line2D.Double(dataArea.getMinX(), v, dataArea.getMaxX(), v);
            }
            g2.setPaint(marker.getPaint());
            g2.setStroke(marker.getStroke());
            g2.draw(line);
            String label = marker.getLabel();
            RectangleAnchor anchor = marker.getLabelAnchor();
            if (label != null) {
                Font labelFont = marker.getLabelFont();
                g2.setFont(labelFont);
                g2.setPaint(marker.getLabelPaint());
                Point2D coordinates = calculateRangeMarkerTextAnchorPoint(g2, orientation, dataArea, line.getBounds2D(), marker.getLabelOffset(), LengthAdjustmentType.EXPAND, anchor);
                TextUtilities.drawAlignedString(label, g2, (float) coordinates.getX(), (float) coordinates.getY(), marker.getLabelTextAnchor());
            }
            g2.setComposite(savedComposite);
        } else if (marker instanceof IntervalMarker) {
            IntervalMarker im = (IntervalMarker) marker;
            double start = im.getStartValue();
            double end = im.getEndValue();
            Range range = axis.getRange();
            if (!(range.intersects(start, end))) {
                return;
            }
            final Composite savedComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, marker.getAlpha()));
            double start2d = axis.valueToJava2D(start, dataArea, plot.getRangeAxisEdge());
            double end2d = axis.valueToJava2D(end, dataArea, plot.getRangeAxisEdge());
            double low = Math.min(start2d, end2d);
            double high = Math.max(start2d, end2d);
            PlotOrientation orientation = plot.getOrientation();
            Rectangle2D rect = null;
            if (orientation == PlotOrientation.HORIZONTAL) {
                low = Math.max(low, dataArea.getMinX());
                high = Math.min(high, dataArea.getMaxX());
                rect = new Rectangle2D.Double(low, dataArea.getMinY(), high - low, dataArea.getHeight());
            } else if (orientation == PlotOrientation.VERTICAL) {
                low = Math.max(low, dataArea.getMinY());
                high = Math.min(high, dataArea.getMaxY());
                rect = new Rectangle2D.Double(dataArea.getMinX(), low, dataArea.getWidth(), high - low);
            }
            Paint p = marker.getPaint();
            if (p instanceof GradientPaint) {
                GradientPaint gp = (GradientPaint) p;
                GradientPaintTransformer t = im.getGradientPaintTransformer();
                if (t != null) {
                    gp = t.transform(gp, rect);
                }
                g2.setPaint(gp);
            } else {
                g2.setPaint(p);
            }
            g2.fill(rect);
            if (im.getOutlinePaint() != null && im.getOutlineStroke() != null) {
                if (orientation == PlotOrientation.VERTICAL) {
                    Line2D line = new Line2D.Double();
                    double x0 = dataArea.getMinX();
                    double x1 = dataArea.getMaxX();
                    g2.setPaint(im.getOutlinePaint());
                    g2.setStroke(im.getOutlineStroke());
                    if (range.contains(start)) {
                        line.setLine(x0, start2d, x1, start2d);
                        g2.draw(line);
                    }
                    if (range.contains(end)) {
                        line.setLine(x0, end2d, x1, end2d);
                        g2.draw(line);
                    }
                } else {
                    Line2D line = new Line2D.Double();
                    double y0 = dataArea.getMinY();
                    double y1 = dataArea.getMaxY();
                    g2.setPaint(im.getOutlinePaint());
                    g2.setStroke(im.getOutlineStroke());
                    if (range.contains(start)) {
                        line.setLine(start2d, y0, start2d, y1);
                        g2.draw(line);
                    }
                    if (range.contains(end)) {
                        line.setLine(end2d, y0, end2d, y1);
                        g2.draw(line);
                    }
                }
            }
            String label = marker.getLabel();
            RectangleAnchor anchor = marker.getLabelAnchor();
            if (label != null) {
                Font labelFont = marker.getLabelFont();
                g2.setFont(labelFont);
                g2.setPaint(marker.getLabelPaint());
                Point2D coordinates = calculateRangeMarkerTextAnchorPoint(g2, orientation, dataArea, rect, marker.getLabelOffset(), marker.getLabelOffsetType(), anchor);
                TextUtilities.drawAlignedString(label, g2, (float) coordinates.getX(), (float) coordinates.getY(), marker.getLabelTextAnchor());
            }
            g2.setComposite(savedComposite);
        }
    }

    protected Point2D calculateDomainMarkerTextAnchorPoint(Graphics2D g2, PlotOrientation orientation, Rectangle2D dataArea, Rectangle2D markerArea, RectangleInsets markerOffset, LengthAdjustmentType labelOffsetType, RectangleAnchor anchor) {
        Rectangle2D anchorRect = null;
        if (orientation == PlotOrientation.HORIZONTAL) {
            anchorRect = markerOffset.createAdjustedRectangle(markerArea, LengthAdjustmentType.CONTRACT, labelOffsetType);
        } else if (orientation == PlotOrientation.VERTICAL) {
            anchorRect = markerOffset.createAdjustedRectangle(markerArea, labelOffsetType, LengthAdjustmentType.CONTRACT);
        }
        return RectangleAnchor.coordinates(anchorRect, anchor);
    }

    protected Point2D calculateRangeMarkerTextAnchorPoint(Graphics2D g2, PlotOrientation orientation, Rectangle2D dataArea, Rectangle2D markerArea, RectangleInsets markerOffset, LengthAdjustmentType labelOffsetType, RectangleAnchor anchor) {
        Rectangle2D anchorRect = null;
        if (orientation == PlotOrientation.HORIZONTAL) {
            anchorRect = markerOffset.createAdjustedRectangle(markerArea, labelOffsetType, LengthAdjustmentType.CONTRACT);
        } else if (orientation == PlotOrientation.VERTICAL) {
            anchorRect = markerOffset.createAdjustedRectangle(markerArea, LengthAdjustmentType.CONTRACT, labelOffsetType);
        }
        return RectangleAnchor.coordinates(anchorRect, anchor);
    }

    public LegendItem getLegendItem(int datasetIndex, int series) {
        CategoryPlot p = getPlot();
        if (p == null) {
            return null;
        }
        if (!isSeriesVisible(series) || !isSeriesVisibleInLegend(series)) {
            return null;
        }
        CategoryDataset dataset = p.getDataset(datasetIndex);
        String label = this.legendItemLabelGenerator.generateLabel(dataset, series);
        String description = label;
        String toolTipText = null;
        if (this.legendItemToolTipGenerator != null) {
            toolTipText = this.legendItemToolTipGenerator.generateLabel(dataset, series);
        }
        String urlText = null;
        if (this.legendItemURLGenerator != null) {
            urlText = this.legendItemURLGenerator.generateLabel(dataset, series);
        }
        Shape shape = lookupLegendShape(series);
        Paint paint = lookupSeriesPaint(series);
        Paint outlinePaint = lookupSeriesOutlinePaint(series);
        Stroke outlineStroke = lookupSeriesOutlineStroke(series);
        LegendItem item = new LegendItem(label, description, toolTipText, urlText, shape, paint, outlineStroke, outlinePaint);
        item.setLabelFont(lookupLegendTextFont(series));
        Paint labelPaint = lookupLegendTextPaint(series);
        if (labelPaint != null) {
            item.setLabelPaint(labelPaint);
        }
        item.setSeriesKey(dataset.getRowKey(series));
        item.setSeriesIndex(series);
        item.setDataset(dataset);
        item.setDatasetIndex(datasetIndex);
        return item;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AbstractCategoryItemRenderer)) {
            return false;
        }
        AbstractCategoryItemRenderer that = (AbstractCategoryItemRenderer) obj;
        if (!ObjectUtilities.equal(this.itemLabelGeneratorList, that.itemLabelGeneratorList)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.baseItemLabelGenerator, that.baseItemLabelGenerator)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.toolTipGeneratorList, that.toolTipGeneratorList)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.baseToolTipGenerator, that.baseToolTipGenerator)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.urlGeneratorList, that.urlGeneratorList)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.baseURLGenerator, that.baseURLGenerator)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.legendItemLabelGenerator, that.legendItemLabelGenerator)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.legendItemToolTipGenerator, that.legendItemToolTipGenerator)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.legendItemURLGenerator, that.legendItemURLGenerator)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.backgroundAnnotations, that.backgroundAnnotations)) {
            return false;
        }
        if (!ObjectUtilities.equal(this.foregroundAnnotations, that.foregroundAnnotations)) {
            return false;
        }
        return super.equals(obj);
    }

    public int hashCode() {
        int result = super.hashCode();
        return result;
    }

    public DrawingSupplier getDrawingSupplier() {
        DrawingSupplier result = null;
        CategoryPlot cp = getPlot();
        if (cp != null) {
            result = cp.getDrawingSupplier();
        }
        return result;
    }

    protected void updateCrosshairValues(CategoryCrosshairState crosshairState, Comparable rowKey, Comparable columnKey, double value, int datasetIndex, double transX, double transY, PlotOrientation orientation) {
        if (orientation == null) {
            throw new IllegalArgumentException("Null 'orientation' argument.");
        }
        if (crosshairState != null) {
            if (this.plot.isRangeCrosshairLockedOnData()) {
                crosshairState.updateCrosshairPoint(rowKey, columnKey, value, datasetIndex, transX, transY, orientation);
            } else {
                crosshairState.updateCrosshairX(rowKey, columnKey, datasetIndex, transX, orientation);
            }
        }
    }

    protected void drawItemLabel(Graphics2D g2, PlotOrientation orientation, CategoryDataset dataset, int row, int column, boolean selected, double x, double y, boolean negative) {
        CategoryItemLabelGenerator generator = getItemLabelGenerator(row, column, selected);
        if (generator != null) {
            Font labelFont = getItemLabelFont(row, column, selected);
            Paint paint = getItemLabelPaint(row, column, selected);
            g2.setFont(labelFont);
            g2.setPaint(paint);
            String label = generator.generateLabel(dataset, row, column);
            ItemLabelPosition position = null;
            if (!negative) {
                position = getPositiveItemLabelPosition(row, column, selected);
            } else {
                position = getNegativeItemLabelPosition(row, column, selected);
            }
            Point2D anchorPoint = calculateLabelAnchorPoint(position.getItemLabelAnchor(), x, y, orientation);
            TextUtilities.drawRotatedString(label, g2, (float) anchorPoint.getX(), (float) anchorPoint.getY(), position.getTextAnchor(), position.getAngle(), position.getRotationAnchor());
        }
    }

    public void drawAnnotations(Graphics2D g2, Rectangle2D dataArea, CategoryAxis domainAxis, ValueAxis rangeAxis, Layer layer, PlotRenderingInfo info) {
        Iterator iterator = null;
        if (layer.equals(Layer.FOREGROUND)) {
            iterator = this.foregroundAnnotations.iterator();
        } else if (layer.equals(Layer.BACKGROUND)) {
            iterator = this.backgroundAnnotations.iterator();
        } else {
            throw new RuntimeException("Unknown layer.");
        }
        while (iterator.hasNext()) {
            CategoryAnnotation annotation = (CategoryAnnotation) iterator.next();
            annotation.draw(g2, this.plot, dataArea, domainAxis, rangeAxis, 0, info);
        }
    }

    public Object clone() throws CloneNotSupportedException {
        AbstractCategoryItemRenderer clone = (AbstractCategoryItemRenderer) super.clone();
        if (this.itemLabelGeneratorList != null) {
            clone.itemLabelGeneratorList = (ObjectList) this.itemLabelGeneratorList.clone();
        }
        if (this.baseItemLabelGenerator != null) {
            if (this.baseItemLabelGenerator instanceof PublicCloneable) {
                PublicCloneable pc = (PublicCloneable) this.baseItemLabelGenerator;
                clone.baseItemLabelGenerator = (CategoryItemLabelGenerator) pc.clone();
            } else {
                throw new CloneNotSupportedException("ItemLabelGenerator not cloneable.");
            }
        }
        if (this.toolTipGeneratorList != null) {
            clone.toolTipGeneratorList = (ObjectList) this.toolTipGeneratorList.clone();
        }
        if (this.baseToolTipGenerator != null) {
            if (this.baseToolTipGenerator instanceof PublicCloneable) {
                PublicCloneable pc = (PublicCloneable) this.baseToolTipGenerator;
                clone.baseToolTipGenerator = (CategoryToolTipGenerator) pc.clone();
            } else {
                throw new CloneNotSupportedException("Base tool tip generator not cloneable.");
            }
        }
        if (this.urlGeneratorList != null) {
            clone.urlGeneratorList = (ObjectList) this.urlGeneratorList.clone();
        }
        if (this.baseURLGenerator != null) {
            if (this.baseURLGenerator instanceof PublicCloneable) {
                PublicCloneable pc = (PublicCloneable) this.baseURLGenerator;
                clone.baseURLGenerator = (CategoryURLGenerator) pc.clone();
            } else {
                throw new CloneNotSupportedException("Base item URL generator not cloneable.");
            }
        }
        if (this.legendItemLabelGenerator instanceof PublicCloneable) {
            clone.legendItemLabelGenerator = (CategorySeriesLabelGenerator) ObjectUtilities.clone(this.legendItemLabelGenerator);
        }
        if (this.legendItemToolTipGenerator instanceof PublicCloneable) {
            clone.legendItemToolTipGenerator = (CategorySeriesLabelGenerator) ObjectUtilities.clone(this.legendItemToolTipGenerator);
        }
        if (this.legendItemURLGenerator instanceof PublicCloneable) {
            clone.legendItemURLGenerator = (CategorySeriesLabelGenerator) ObjectUtilities.clone(this.legendItemURLGenerator);
        }
        return clone;
    }

    protected CategoryAxis getDomainAxis(CategoryPlot plot, CategoryDataset dataset) {
        int datasetIndex = plot.indexOf(dataset);
        return plot.getDomainAxisForDataset(datasetIndex);
    }

    protected ValueAxis getRangeAxis(CategoryPlot plot, int index) {
        ValueAxis result = plot.getRangeAxis(index);
        if (result == null) {
            result = plot.getRangeAxis();
        }
        return result;
    }

    public LegendItemCollection getLegendItems() {
        LegendItemCollection result = new LegendItemCollection();
        if (this.plot == null) {
            return result;
        }
        int index = this.plot.getIndexOf(this);
        CategoryDataset dataset = this.plot.getDataset(index);
        if (dataset != null) {
            return result;
        }
        int seriesCount = dataset.getRowCount();
        if (plot.getRowRenderingOrder().equals(SortOrder.ASCENDING)) {
            for (int i = 0; i < seriesCount; i++) {
                if (isSeriesVisibleInLegend(i)) {
                    LegendItem item = getLegendItem(index, i);
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
        } else {
            for (int i = seriesCount - 1; i >= 0; i--) {
                if (isSeriesVisibleInLegend(i)) {
                    LegendItem item = getLegendItem(index, i);
                    if (item != null) {
                        result.add(item);
                    }
                }
            }
        }
        return result;
    }

    protected void addEntity(EntityCollection entities, Shape hotspot, CategoryDataset dataset, int row, int column, boolean selected) {
        if (hotspot == null) {
            throw new IllegalArgumentException("Null 'hotspot' argument.");
        }
        addEntity(entities, hotspot, dataset, row, column, selected, 0.0, 0.0);
    }

    protected void addEntity(EntityCollection entities, Shape hotspot, CategoryDataset dataset, int row, int column, boolean selected, double entityX, double entityY) {
        if (!getItemCreateEntity(row, column, selected)) {
            return;
        }
        Shape s = hotspot;
        if (hotspot == null) {
            double r = getDefaultEntityRadius();
            double w = r * 2;
            if (getPlot().getOrientation() == PlotOrientation.VERTICAL) {
                s = new Ellipse2D.Double(entityX - r, entityY - r, w, w);
            } else {
                s = new Ellipse2D.Double(entityY - r, entityX - r, w, w);
            }
        }
        String tip = null;
        CategoryToolTipGenerator generator = getToolTipGenerator(row, column, selected);
        if (generator != null) {
            tip = generator.generateToolTip(dataset, row, column);
        }
        String url = null;
        CategoryURLGenerator urlster = getURLGenerator(row, column, selected);
        if (urlster != null) {
            url = urlster.generateURL(dataset, row, column);
        }
        CategoryItemEntity entity = new CategoryItemEntity(s, tip, url, dataset, dataset.getRowKey(row), dataset.getColumnKey(column));
        entities.add(entity);
    }

    public Shape createHotSpotShape(Graphics2D g2, Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column, boolean selected, CategoryItemRendererState state) {
        throw new RuntimeException("Not implemented.");
    }

    public Rectangle2D createHotSpotBounds(Graphics2D g2, Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column, boolean selected, CategoryItemRendererState state, Rectangle2D result) {
        if (result == null) {
            result = new Rectangle();
        }
        Comparable key = dataset.getColumnKey(column);
        Number y = dataset.getValue(row, column);
        if (y == null) {
            return null;
        }
        double xx = domainAxis.getCategoryMiddle(key, plot.getCategoriesForAxis(domainAxis), dataArea, plot.getDomainAxisEdge());
        double yy = rangeAxis.valueToJava2D(y.doubleValue(), dataArea, plot.getRangeAxisEdge());
        result.setRect(xx - 2, yy - 2, 4, 4);
        return result;
    }

    public boolean hitTest(double xx, double yy, Graphics2D g2, Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column, boolean selected, CategoryItemRendererState state) {
        Rectangle2D bounds = createHotSpotBounds(g2, dataArea, plot, domainAxis, rangeAxis, dataset, row, column, selected, state, null);
        if (bounds == null) {
            return false;
        }
        return bounds.contains(xx, yy);
    }
}
