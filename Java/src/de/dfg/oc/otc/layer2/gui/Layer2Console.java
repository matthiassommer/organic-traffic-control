package de.dfg.oc.otc.layer2.gui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Diese graphische Klasse erm�glicht Ausgaben der Ebene 2. Sie ist als
 * Singleton realisiert.
 * 
 * @author hpr
 * 
 */
public final class Layer2Console extends JFrame {
	private static Layer2Console l2c;

	/**
	 * Creates a chart.
	 * 
	 * @param dataset
	 *            a dataset.
	 * @see org.jfree.chart.demo.TimeSeriesChartDemo1
	 * @return A chart.
	 */
	private JFreeChart createChart(final XYDataset dataset) {
		// title, x-axis label, y-axis label, data, orientation, create legend?,
		// generate tooltips?, generate URLs?
		final JFreeChart chart = ChartFactory.createXYLineChart("Optimization process", "Generation",
				"Objective function", dataset, PlotOrientation.VERTICAL, true, true, false);

		chart.setBackgroundPaint(Color.white);

		final XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);

		final XYItemRenderer r = plot.getRenderer();
		if (r instanceof XYLineAndShapeRenderer) {
			final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
			renderer.setBaseShapesVisible(true);
			renderer.setBaseShapesFilled(true);
		}

		return chart;
	}

	/**
	 * Returns the singleton {@code Layer2Console}; a new Layer2Console is
	 * created if necessary.
	 * 
	 * @return Singleton Layer2Console
	 */
	public static Layer2Console getInstance() {
		if (l2c == null) {
			l2c = new Layer2Console();
		}
		return l2c;
	}

	// @jve:decl-index=0:
	private XYSeries avg;
	// @jve:decl-index=0:
	private XYSeries best;
    private JTabbedPane jTabbedPane;
	private JTextPane jTextPaneConsole;
	private JTextPane jTextPaneServer;

	private Layer2Console() {
		super();
		initialize();
	}

	/**
	 * Leert die Konsole f�r EA-Ausgaben.
	 */
	public void clearEAConsole() {
		jTextPaneConsole.setText(null);
	}

	/**
	 * Return the series of the average fitness value in each generation. The
	 * chart is updated automatically after the data object is changed.
	 * 
	 * @return the series of the average fitness value in each generation
	 */
	public XYSeries getAverageSeries() {
		return avg;
	}

	/**
	 * Return the series of the best fitness value in each generation. The chart
	 * is updated automatically after the data object is changed.
	 * 
	 * @return the series of the best fitness value in each generation
	 */
	public XYSeries getBestSeries() {
		return best;
	}

	/**
	 * Creates a panel for the chart.
	 * 
	 * @return a panel
	 */
	/* @see org.jfree.chart.demo.TimeSeriesChartDemo1 */
	private ChartPanel getChartPanel() {
		final XYSeriesCollection dataSet = new XYSeriesCollection();

		best = new XYSeries("best");
		best.addChangeListener(dataSet);
		dataSet.addSeries(best);

		avg = new XYSeries("average");
		avg.addChangeListener(dataSet);
		dataSet.addSeries(avg);

		final JFreeChart chart = createChart(dataSet);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		chartPanel.setMouseZoomable(true, false);
		setContentPane(chartPanel);
		return chartPanel;
	}

	private JPanel createJContentPane() {
        JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(createJTabbedPane(), BorderLayout.CENTER);

		return panel;
	}

	private JPanel createJPanel() {
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weighty = 1;
		constraints.weightx = 1;

        JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.setName("Server");
		panel.add(createJTextPaneServer(), constraints);

		return panel;
	}

	private JPanel createJPanelEA() {
		final GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
		gridBagConstraints1.fill = GridBagConstraints.BOTH;
		gridBagConstraints1.weighty = 1;
		gridBagConstraints1.weightx = 1;

        JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.add(createJTextPaneConsole(), gridBagConstraints1);

		return panel;
	}

	private JTabbedPane createJTabbedPane() {
		jTabbedPane = new JTabbedPane();
		jTabbedPane.addTab("Console (Server)", null, createJPanel(), null);
		jTabbedPane.addTab("Console (EA)", null, createJPanelEA(), null);
		jTabbedPane.addTab("Visualization (EA)", null, getChartPanel(), null);

		return jTabbedPane;
	}

	private JScrollPane createJTextPaneConsole() {	
		jTextPaneConsole = new JTextPane();
		jTextPaneConsole.setEditable(false);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(jTextPaneConsole);
		
		return scrollPane;
	}

	private JScrollPane createJTextPaneServer() {
		jTextPaneServer = new JTextPane();
		jTextPaneServer.setEditable(false);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(jTextPaneServer);
		
		return scrollPane;
	}

	private void initialize() {
		this.setSize(584, 339);
		this.setContentPane(createJContentPane());
		this.setTitle("Layer 2");
	}

	/**
	 * Prints a info message to the console.
	 * 
	 * @param textToAdd
	 *            the text that will be printed
	 */
	public synchronized void printEAInfo(final String textToAdd) {
		printToConsole(jTextPaneConsole, textToAdd, false);
	}

	/**
	 * Prints a warning message to the console. Text color is set to red.
	 * 
	 * @param textToAdd
	 *            the text that will be printed
	 */
	public synchronized void printEAWarning(final String textToAdd) {
		printToConsole(jTextPaneConsole, textToAdd, true);
	}

	/**
	 * Prints a server info message to the console.
	 * 
	 * @param textToAdd
	 *            the text that will be printed
	 */
	public synchronized void printServerInfo(final String textToAdd) {
		printToConsole(jTextPaneServer, textToAdd, false);
	}

	/**
	 * Prints a server warning message to the console. Text color is set to red.
	 * 
	 * @param textToAdd
	 *            the text that will be printed
	 */
	public synchronized void printServerWarning(final String textToAdd) {
		printToConsole(jTextPaneServer, textToAdd, true);
	}

	/**
	 * Internal method for printing to one of the consoles.
	 * 
	 * @param jtp
	 *            console to print to
	 * @param text
	 *            text to print
	 * @param warn
	 *            {@code true} if it is a warning method (currently
	 *            ignored)
	 */
	private synchronized void printToConsole(final JTextPane jtp, final String text, final boolean warn) {
		StyledDocument doc = jtp.getStyledDocument();

		if (warn) {
			Style style = jtp.addStyle("Warning", null);
			StyleConstants.setForeground(style, Color.red);
			try {
				doc.insertString(doc.getLength(), text + System.getProperty("line.separator"), style);
			} catch (BadLocationException e) {
				System.err.println(e.getMessage());
			}
		}
		else {
			try {
				doc.insertString(doc.getLength(), text + System.getProperty("line.separator"), null);
			} catch (BadLocationException e) {
				System.err.println(e.getMessage());
			}
		}
		
		jtp.setCaretPosition(jtp.getDocument().getLength());
	}

	/**
	 * Reset the data series that are displayed in the chart panel.
	 */
	public void resetChartSeries() {
		best.clear();
		avg.clear();
	}

	/**
	 * Speichert das ChartPanel in die angegebene Datei (im PNG-Format).
	 * 
	 * @param file
	 *            zu speichernde Datei
	 */
	public synchronized void saveChartPanel(final File file) {
		final JFreeChart chart = ((ChartPanel) jTabbedPane.getComponentAt(2)).getChart();
		try {
			ChartUtilities.saveChartAsPNG(file, chart, 400, 300);
		} catch (IOException e) {
			printEAWarning("Could not write chart to file " + file + ": " + e.getMessage());
		}
	}

	/**
	 * Sets a title for the optimisation chart.
	 * 
	 * @param title
	 *            a title
	 */
	public synchronized void setChartTitle(final String title) {
		((ChartPanel) jTabbedPane.getComponentAt(2)).getChart().setTitle(title);
	}

	/**
	 * Saves the contents of the EA's console to a file. Existing files are
	 * overwritten without request.
	 * 
	 * @param file
	 *            file where the console's contents will be stored
	 */
	public synchronized void writeEAConsoleToFile(final File file) {
		try {
			final FileWriter fstream = new FileWriter(file);
			final BufferedWriter out = new BufferedWriter(fstream);
			out.write(jTextPaneConsole.getText());
			out.close();
		} catch (IOException e) {
			printEAWarning("Could not write EA console to file " + file + ": " + e.getMessage());
		}
	}
} // @jve:decl-index=0:visual-constraint="10,10"
