package de.dfg.oc.otc.manager.gui;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import de.dfg.oc.otc.layer1.Layer1Exception;
import de.dfg.oc.otc.layer1.observer.Layer1Observer.DataSource;
import de.dfg.oc.otc.manager.OTCManagerException;
import de.dfg.oc.otc.manager.OTCNode;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.List;

/**
 *
 */
class SituationChartPanel extends JPanel {
    private final int attribute;
    private final ChartPanel chartPanel;
    private final XYSeriesCollection dataSet;
    private final int numSeries;
    private final XYSeries[] situationSeries;
    private final SliderInput sliderInput;
    private final OTCNode node;
    private JCheckBox checkRefresh;
    private int interval = -1;
    private float time = -1;

    SituationChartPanel(final OTCNode node, final int attribute) {
        super();
        this.node = node;
        this.attribute = attribute;
        sliderInput = new SliderInput();

        interval = 90;
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3, 1, 2, 2));
        buttonPanel.add(getSavePngButton());
        buttonPanel.add(getSavePdfButton());
        buttonPanel.add(getCheckRefresh());

        topPanel.add(buttonPanel);
        topPanel.add(sliderInput);

        dataSet = new XYSeriesCollection();
        final String[] descriptions = node.getSituationAnalyser().getSituationDescription(DataSource.STATISTICS,
                attribute);
        numSeries = descriptions.length - 1;
        situationSeries = new XYSeries[numSeries];
        for (int i = 0; i < numSeries; i++) {
            situationSeries[i] = new XYSeries(descriptions[i + 1]);
            // situationSeries[i].addChangeListener(dataSet);
            dataSet.addSeries(situationSeries[i]);
        }

        final JFreeChart chart = createChart(dataSet, descriptions[0]);
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(250, 200));
        chartPanel.setMouseZoomable(true, false);

        this.setLayout(new BorderLayout());
        this.add(topPanel, BorderLayout.NORTH);
        this.add(chartPanel, BorderLayout.CENTER);
    }

    /**
     * Creates a chart.
     *
     * @param dataset a dataset.
     * @return A chart.
     */
    private JFreeChart createChart(final XYDataset dataset, final String description) {
        // title, x-axis label, y-axis label, data, orientation, create legend?,
        // generate tooltips?, generate URLs?
        final JFreeChart chart = ChartFactory.createXYLineChart("Situation (Attribute: " + description + ")", "Time",
                "Value", dataset, PlotOrientation.VERTICAL, true, true, false);

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
        final int interval = sliderInput.getValue();
        float[] situation;

        try {
            situation = node.getSituation(DataSource.STATISTICS, attribute, interval);
        } catch (Layer1Exception | OTCManagerException e) {
            return;
        }

        if (situation.length != situationSeries.length) {
            return;
        }

        for (int i = 0; i < situation.length; i++) {
            situationSeries[i].add(time, situation[i]);
        }

        if (this.interval != interval) {
            final XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
            final ValueAxis axis = plot.getRangeAxis();
            final XYAnnotation lineAnnotation = new XYLineAnnotation(time, 0, time, axis.getUpperBound());
            final XYTextAnnotation textAnnotation = new XYTextAnnotation(String.valueOf(interval), time, axis.getUpperBound() / 2);
            textAnnotation.setRotationAngle(Math.PI / 2);
            textAnnotation.setPaint(Color.white);
            plot.addAnnotation(lineAnnotation);
            plot.addAnnotation(textAnnotation);
            this.interval = interval;
        }
    }

    private JCheckBox getCheckRefresh() {
        if (checkRefresh == null) {
            checkRefresh = new JCheckBox("Refresh Chart");
            checkRefresh.setSelected(true);
            checkRefresh.addActionListener(action -> {
                if (checkRefresh.isSelected()) {
                    for (int i = 0; i < numSeries; i++) {
                        situationSeries[i].addChangeListener(dataSet);
                    }
                } else {
                    for (int i = 0; i < numSeries; i++) {
                        situationSeries[i].removeChangeListener(dataSet);
                    }
                }
            });
        }
        return checkRefresh;
    }

    private JButton getSavePdfButton() {
        JButton savePdfButton = new JButton("Save (pdf)");
        savePdfButton.addActionListener(action -> {
            final int width = 1000;
            final int height = 800;
            final String filename = "Situation_"
                    + node.getSituationAnalyser().getSituationDescription(DataSource.STATISTICS, attribute)[0]
                    + "_Node_" + node.getId() + "_Time_" + time + ".pdf";
            final JFreeChart chart = chartPanel.getChart();
            final File pdf = new File(filename);

            final Document document = new Document(new Rectangle(width, height));
            try {
                PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdf));
                document.open();

                final PdfContentByte cb = writer.getDirectContent();
                final PdfTemplate tp = cb.createTemplate(width, height);
                final Graphics2D g2d = tp.createGraphics(width, height, new DefaultFontMapper());
                final Rectangle2D r2d = new Rectangle2D.Double(0, 0, width, height);

                chart.draw(g2d, r2d);
                g2d.dispose();
                cb.addTemplate(tp, 0, 0);

                writer.close();
            } catch (DocumentException | FileNotFoundException e) {
                e.printStackTrace();
            }
            document.close();
        });

        return savePdfButton;
    }

    private JButton getSavePngButton() {
        JButton savePngButton = new JButton("Save (png)");
        savePngButton.addActionListener(e -> {

            final String filename = "Situation_"
                    + node.getSituationAnalyser().getSituationDescription(DataSource.STATISTICS, attribute)[0]
                    + "_Node_" + node.getId() + "_Time_" + time + ".png";
            final JFreeChart chart = chartPanel.getChart();
            final File png = new File(filename);
            try {
                ChartUtilities.saveChartAsPNG(png, chart, 400, 300);
            } catch (IOException ioe) {
                ioe.getStackTrace();
            }
        });

        return savePngButton;
    }

    final void printDataToFile(final String filename) {
        final String[] descriptions = node.getSituationAnalyser().getSituationDescription(DataSource.STATISTICS,
                attribute);
        FileOutputStream outputfile;
        PrintStream outputStream;
        final String linesep = System.getProperty("line.separator");

        for (int i = 0; i < numSeries; i++) {
            try {
                outputfile = new FileOutputStream(descriptions[i + 1] + "_" + descriptions[0] + "_" + filename);
                outputStream = new PrintStream(outputfile);
            } catch (FileNotFoundException e1) {
                continue;
            }

            final List seriesList = situationSeries[i].getItems();
            XYDataItem theItem;
            for (Object aSeriesList : seriesList) {
                theItem = (XYDataItem) aSeriesList;
                outputStream.print(theItem.getX() + "; " + theItem.getY() + linesep);
            }

            try {
                outputStream.close();
                outputfile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class SliderInput extends JPanel {
        private final JSlider intervalSlider;
        private JTextField intervalField;

        public SliderInput() {
            super();
            final JPanel sliderPanel = new JPanel();
            final JPanel fieldPanel = new JPanel();

            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
            fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));

            JLabel sliderLabel = new JLabel("Interval for Situation");
            JLabel fieldLabel = new JLabel("Value");

            intervalSlider = new JSlider(1, 900, 90);
            intervalSlider.setMajorTickSpacing(100);
            intervalSlider.setMinorTickSpacing(50);

            intervalSlider.setPaintTicks(true);
            intervalSlider.setPaintLabels(true);
            intervalSlider.setToolTipText("Interval in seconds");
            intervalSlider.addChangeListener(e -> intervalField.setText(String.valueOf(intervalSlider.getValue())));

            intervalField = new JTextField("90");
            intervalField.addKeyListener(new KeyListener() {
                public void keyTyped(final KeyEvent e) {
                    if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                        int value;
                        try {
                            value = Integer.parseInt(intervalField.getText());
                        } catch (NumberFormatException e1) {
                            intervalField.setText(String.valueOf(intervalSlider.getValue()));
                            return;
                        }
                        if (value > 900) {
                            value = 900;
                        } else if (value < 1) {
                            value = 1;
                        }
                        intervalSlider.setValue(value);
                    }
                }

                @Override
                public void keyPressed(final KeyEvent e) {
                }

                @Override
                public void keyReleased(final KeyEvent e) {
                }
            });

            sliderPanel.add(sliderLabel);
            sliderPanel.add(intervalSlider);
            fieldPanel.add(fieldLabel);
            fieldPanel.add(intervalField);
            this.add(sliderPanel);
            this.add(fieldPanel);
        }

        public int getValue() {
            return intervalSlider.getValue();
        }
    }
}