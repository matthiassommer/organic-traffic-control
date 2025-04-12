package de.dfg.oc.otc.manager.gui;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.layer1.observer.Layer1Observer.DataSource;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.TrafficType;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Displays Level of Service measures.
 */
class LOSChartPanel extends JPanel {
    private final ChartPanel chartPanel;
    private JCheckBox checkRefresh;
    private final XYSeriesCollection dataSet;
    private final JSlider firstIntervalSlider;
    private final XYSeries firstLOSSeries;
    private final JSlider secondIntervalSlider;
    private final XYSeries secondLOSSeries;
    private final OTCNode node;
    private float time = -1;

    LOSChartPanel(final OTCNode node) {
        super();
        this.node = node;
        this.setBackground(Color.green);

        firstIntervalSlider = new JSlider(1, 40, 3);
        firstIntervalSlider.setMajorTickSpacing(10);
        firstIntervalSlider.setMinorTickSpacing(5);
        firstIntervalSlider.setPaintTicks(true);
        firstIntervalSlider.setPaintLabels(true);

        secondIntervalSlider = new JSlider(1, 40, 10);
        secondIntervalSlider.setMajorTickSpacing(10);
        secondIntervalSlider.setMinorTickSpacing(5);
        secondIntervalSlider.setPaintTicks(true);
        secondIntervalSlider.setPaintLabels(true);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3, 1, 2, 2));
        buttonPanel.add(getSavePngButton());
        buttonPanel.add(getSavePdfButton());
        buttonPanel.add(getCheckRefresh());

        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new GridLayout(2, 2));

        JLabel firstLOSLabel = new JLabel("Interval for first LOS curve");
        sliderPanel.add(firstLOSLabel);

        JLabel secondLOSLabel = new JLabel("Interval for second LOS curve");
        sliderPanel.add(secondLOSLabel);

        sliderPanel.add(firstIntervalSlider);
        sliderPanel.add(secondIntervalSlider);

        topPanel.add(buttonPanel);
        topPanel.add(sliderPanel);

        dataSet = new XYSeriesCollection();

        firstLOSSeries = new XYSeries("First LOS");
        dataSet.addSeries(firstLOSSeries);

        secondLOSSeries = new XYSeries("Second LOS");
        dataSet.addSeries(secondLOSSeries);

        final JFreeChart chart = createChart(dataSet);
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(250, 150));
        chartPanel.setMouseZoomable(true, false);

        this.setLayout(new BorderLayout());
        this.add(topPanel, BorderLayout.NORTH);
        this.add(chartPanel, BorderLayout.CENTER);

    }

    /**
     * @see org.jfree.chart.demo.TimeSeriesChartDemo1
     *
     * @param dataset
     * @return
     */
    private JFreeChart createChart(final XYDataset dataset) {
        // title, x-axis label, y-axis label, data, orientation, create legend?,
        // generate tooltips?, generate URLs?
        final JFreeChart chart = ChartFactory.createXYLineChart("Evaluation", "Time", "Value", dataset,
                PlotOrientation.VERTICAL, true, true, false);

        chart.setBackgroundPaint(Color.white);

        final XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        final ValueAxis axis = plot.getDomainAxis();
        axis.setAutoRange(true);
        // 60 seconds
        axis.setFixedAutoRange(600.0);

        final XYItemRenderer r = plot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
            final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            renderer.setBaseShapesVisible(false);
            renderer.setBaseShapesFilled(false);
        }

        return chart;
    }

    final void evaluate(final float time) {
        this.time = time;
        final float cycleTime = node.getJunction().getActiveTLC().getCycleTime();
        final float firstInterval = firstIntervalSlider.getValue() * cycleTime;
        final float secondInterval = secondIntervalSlider.getValue() * cycleTime;
        final float firstLOS = node.getEvaluation(DataSource.STATISTICS, Attribute.LOS,
                TrafficType.INDIVIDUAL_TRAFFIC, firstInterval, false);
        final float secondLOS = node.getEvaluation(DataSource.STATISTICS, Attribute.LOS,
                TrafficType.INDIVIDUAL_TRAFFIC, secondInterval, false);
        if (!Float.isNaN(firstLOS)) {
            firstLOSSeries.add(time, firstLOS);
        }
        if (!Float.isNaN(secondLOS)) {
            secondLOSSeries.add(time, secondLOS);
        }
    }

    private JCheckBox getCheckRefresh() {
        checkRefresh = new JCheckBox("Refresh Chart");
        checkRefresh.setSelected(true);
        checkRefresh.addActionListener(action -> {
            if (checkRefresh.isSelected()) {
                firstLOSSeries.addChangeListener(dataSet);
                secondLOSSeries.addChangeListener(dataSet);
            } else {
                firstLOSSeries.removeChangeListener(dataSet);
                secondLOSSeries.removeChangeListener(dataSet);
            }
        });
        return checkRefresh;
    }

    final int getNodeId() {
        return node.getId();
    }

    private JButton getSavePdfButton() {
        JButton savePdfButton = new JButton();
        savePdfButton.setText("Save (pdf)");
        savePdfButton.addActionListener(action -> {
            final int width = 1000;
            final int height = 800;
            final String filename = "LOS_Node_" + node.getId() + "_Time_" + time + ".pdf";
            final JFreeChart chart = chartPanel.getChart();
            final File pdffile = new File(filename);

            final Document document = new Document(new Rectangle(width, height));
            try {
                PdfWriter writer;
                writer = PdfWriter.getInstance(document, new FileOutputStream(pdffile));
                document.open();
                final PdfContentByte cb = writer.getDirectContent();
                final PdfTemplate tp = cb.createTemplate(width, height);
                final Graphics2D g2d = tp.createGraphics(width, height, new DefaultFontMapper());
                final Rectangle2D r2d = new Rectangle2D.Double(0, 0, width, height);
                chart.draw(g2d, r2d);
                g2d.dispose();
                cb.addTemplate(tp, 0, 0);
            } catch (DocumentException | FileNotFoundException e) {
                e.printStackTrace();
            }
            document.close();
        });
        return savePdfButton;
    }

    private JButton getSavePngButton() {
        JButton savePngButton = new JButton();
        savePngButton.setText("Save (png)");
        savePngButton.addActionListener(e -> {

            final String filename = "LOS_Node_" + node.getId() + "_Time_" + time + ".png";
            final JFreeChart chart = chartPanel.getChart();
            final File pngfile = new File(filename);
            try {
                ChartUtilities.saveChartAsPNG(pngfile, chart, 400, 300);
            } catch (IOException ioe) {
                ioe.getStackTrace();
            }
        });
        return savePngButton;
    }
}
