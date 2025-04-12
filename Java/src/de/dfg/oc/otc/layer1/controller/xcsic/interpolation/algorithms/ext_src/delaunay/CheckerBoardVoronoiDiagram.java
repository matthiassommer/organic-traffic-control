package de.dfg.oc.otc.layer1.controller.xcsic.interpolation.algorithms.ext_src.delaunay;

/*
 * Copyright (c) 2005, 2007 by L. Paul Chew.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.List;

/**
 * Created by Anthony Stein
 *
 * Eine Erweiterung der <code>DelaunayAp</code> Klasse
 * für die Anwendung auf das Checkerboard-Problem.
 *
 *
 * @author Paul Chew
 *
 * Created July 2005. Derived from an earlier, messier version.
 *
 * Modified December 2007. Updated some of the Triangulation methods. Added the
 * "Colorful" checkbox. Reorganized the interface between DelaunayAp and
 * DelaunayPanel. Added code to find a Voronoi cell.
 *
 */
@SuppressWarnings("serial")
public class CheckerBoardVoronoiDiagram extends JApplet
        implements Runnable, ActionListener, MouseListener {

    private boolean debug = false;             // Used for debugging
    private Component currentSwitch = null;    // Entry-switch that mouse is in

    private static String windowTitle = "Voronoi/Delaunay Window";
    private JRadioButton voronoiButton = new JRadioButton("Voronoi Diagram");
    private JRadioButton delaunayButton =
                                    new JRadioButton("Delaunay Triangulation");
    private JButton clearButton = new JButton("Clear");
    private JCheckBox colorfulBox = new JCheckBox("More Colorful");
    private VoronoiPanel delaunayPanel = new VoronoiPanel(this);
    private JLabel circleSwitch = new JLabel("Show Empty Circles");
    private JLabel delaunaySwitch = new JLabel("Show Delaunay Edges");
    private JLabel voronoiSwitch = new JLabel("Show Voronoi Edges");

    /**
     * Main program (used when run as application instead of applet).
     */
    public static void main (String[] args) {
        DelaunayAp applet = new DelaunayAp();    // Create applet
        applet.init();                           // Applet initialization
        JFrame dWindow = new JFrame();           // Create window
        dWindow.setSize(700, 500);               // Set window size
        dWindow.setTitle(windowTitle);           // Set window title
        dWindow.setLayout(new BorderLayout());   // Specify layout manager
        dWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                                                 // Specify closing behavior
        dWindow.add(applet, "Center");           // Place applet into window
        dWindow.setVisible(true);                // Show the window
    }

    /**
     * Initialize the applet.
     * As recommended, the actual use of Swing components takes place in the
     * event-dispatching thread.
     */
    public void init () {
        try {SwingUtilities.invokeAndWait(this);}
        catch (Exception e) {System.err.println("Initialization failure");}
    }

    /**
     * Set up the applet's GUI.
     * As recommended, the init method executes this in the event-dispatching
     * thread.
     */
    public void run () {
        setLayout(new BorderLayout());

        // Add the mouse-entry switches
        JPanel switchPanel = new JPanel();
        switchPanel.add(circleSwitch);
        switchPanel.add(new Label("     "));            // Spacing
        switchPanel.add(delaunaySwitch);
        switchPanel.add(new Label("     "));            // Spacing
        switchPanel.add(voronoiSwitch);
        this.add(switchPanel, "South");

        // Build the delaunay panel
        delaunayPanel.setBackground(Color.gray);
        this.add(delaunayPanel, "Center");

        // Register the listeners
        clearButton.addActionListener(this);
        circleSwitch.addMouseListener(this);
        delaunaySwitch.addMouseListener(this);
        voronoiSwitch.addMouseListener(this);

        // Initialize the radio buttons
        voronoiButton.doClick();
    }

    /**
     * A button has been pressed; redraw the picture.
     */
    public void actionPerformed(ActionEvent e) {
        if (debug)
            System.out.println(((AbstractButton)e.getSource()).getText());
        if (e.getSource() == clearButton) delaunayPanel.clear();
        delaunayPanel.repaint();
    }

    /**
     * If entering a mouse-entry switch then redraw the picture.
     */
    public void mouseEntered(MouseEvent e) {
        currentSwitch = e.getComponent();
        if (currentSwitch instanceof JLabel) delaunayPanel.repaint();
        else currentSwitch = null;
    }

    /**
     * If exiting a mouse-entry switch then redraw the picture.
     */
    public void mouseExited(MouseEvent e) {
        currentSwitch = null;
        if (e.getComponent() instanceof JLabel) delaunayPanel.repaint();
    }

    /**
     * If mouse has been pressed inside the delaunayPanel then add a new site.
     */
    public void mousePressed(MouseEvent e) {
        if (e.getSource() != delaunayPanel) return;
        Pnt point = new Pnt(e.getX(), e.getY());
        if (debug ) System.out.println("Click " + point);
        delaunayPanel.addSite(point);
        delaunayPanel.repaint();
    }

    /**
     * Not used, but needed for MouseListener.
     */
    public void mouseReleased(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {}

    /**
     * @return true iff the "colorful" box is selected
     */
    public boolean isColorful() {
//        return colorfulBox.isSelected();
    	return true;
    }

    /**
     * @return true iff doing Voronoi diagram.
     */
    public boolean isVoronoi() {
        return voronoiButton.isSelected();
    }

    /**
     * @return true iff within circle switch
     */
    public boolean showingCircles() {
        return currentSwitch == circleSwitch;
    }

    /**
     * @return true iff within delaunay switch
     */
    public boolean showingDelaunay() {
        return currentSwitch == delaunaySwitch;
    }

    /**
     * @return true iff within voronoi switch
     */
    public boolean showingVoronoi() {
        return currentSwitch == voronoiSwitch;
    }

    /**
     * Fügt dem Checkerboard Voronoi-Diagramm eine neue Stützstelle mit der übergebenen Farbe zu.
     * @param x Die X-Koordinate der neuen Stützstelle
     * @param y Die Y-Koordinate der neuen Stützstelle
     * @param col Die Farbe der neuen Voronoi-Zelle, die die neue Stützstelle umschließt
     */
    public void addSiteWithColor(double x, double y, Color col) {
    	Pnt newSite = new Pnt(x, y);
    	this.delaunayPanel.setColor(newSite, col);
    	this.delaunayPanel.addSite(newSite);
    }
    
    public void repaintPanel() {
    	this.delaunayPanel.repaint();
    }
}

/**
 * Graphics Panel for DelaunayAp.
 */
@SuppressWarnings("serial")
class VoronoiPanel extends JPanel {

    public static Color voronoiColor = Color.magenta;
    public static Color delaunayColor = Color.green;
    public static int pointRadius = 2;

    private CheckerBoardVoronoiDiagram controller;              // Controller for DT
    private Triangulation dt;                   // Delaunay triangulation
    private Map<Object, Color> colorTable;      // Remembers colors for display
    private Triangle initialTriangle;           // Initial triangle
    private static int initialSize = 10000 ;     // Size of initial triangle
    private Graphics g;                         // Stored graphics context
    private Random random = new Random();       // Source of random numbers

    /**
     * Create and initialize the DT.
     */
    public VoronoiPanel (CheckerBoardVoronoiDiagram controller) {
        this.controller = controller;
        initialTriangle = new Triangle(
                new Pnt(-initialSize, -initialSize),
                new Pnt( initialSize, -initialSize),
                new Pnt(           0,  initialSize));
        dt = new Triangulation(initialTriangle);
        colorTable = new HashMap<Object, Color>();
    }

    /**
     * Add a new site to the DT.
     * @param point the site to add
     */
    public void addSite(Pnt point) {
    	synchronized(dt){
    		dt.delaunayPlace(point);
    	}
    }

    /**
     * Re-initialize the DT.
     */
    public void clear() {
        dt = new Triangulation(initialTriangle);
    }

    /**
     * Get the color for the spcified item; generate a new color if necessary.
     * @param item we want the color for this item
     * @return item's color
     */
    private Color getColor (Object item) {
        if (colorTable.containsKey(item)) return colorTable.get(item);
        Color color = new Color(Color.HSBtoRGB(random.nextFloat(), 1.0f, 1.0f));
        colorTable.put(item, color);
        return color;
    }

    /**
     * Fügt der Farbtabelle, welche jeder Stützstelle bzw. deren Voronoi-Zelle eine
     * Farbe hinzufügt einen neuen Wert hinzu!
     * @param item Die entsprechende Stützstelle
     * @param col Die Farbe, die zugewiesen werden soll
     */
    public void setColor (Object item, Color col) {
    	if (!colorTable.containsKey(item))
    		colorTable.put(item, col);
    }
    
    /* Basic Drawing Methods */

    /**
     * Draw a point.
     * @param point the Pnt to draw
     */
    public void draw (Pnt point) {
        int r = pointRadius;
        int x = (int) point.coord(0);
        int y = (int) point.coord(1);
        g.fillOval(x-r, y-r, r+r, r+r);
    }

    /**
     * Draw a circle.
     * @param center the center of the circle
     * @param radius the circle's radius
     * @param fillColor null implies no fill
     */
    public void draw (Pnt center, double radius, Color fillColor) {
        int x = (int) center.coord(0);
        int y = (int) center.coord(1);
        int r = (int) radius;
        if (fillColor != null) {
            Color temp = g.getColor();
            g.setColor(fillColor);
            g.fillOval(x-r, y-r, r+r, r+r);
            g.setColor(temp);
        }
        g.drawOval(x-r, y-r, r+r, r+r);
    }

    /**
     * Draw a polygon.
     * @param polygon an array of polygon vertices
     * @param fillColor null implies no fill
     */
    public void draw (Pnt[] polygon, Color fillColor) {
        int[] x = new int[polygon.length];
        int[] y = new int[polygon.length];
        for (int i = 0; i < polygon.length; i++) {
            x[i] = (int) polygon[i].coord(0);
            y[i] = (int) polygon[i].coord(1);
        }
        if (fillColor != null) {
            Color temp = g.getColor();
            g.setColor(fillColor);
            g.fillPolygon(x, y, polygon.length);
            g.setColor(temp);
        }
        g.drawPolygon(x, y, polygon.length);
    }

    /* Higher Level Drawing Methods */

    /**
     * Handles painting entire contents of DelaunayPanel.
     * Called automatically; requested via call to repaint().
     * @param g the Graphics context
     */
    public void paintComponent (Graphics g) {
        super.paintComponent(g);
        this.g = g;

        // Flood the drawing area with a "background" color
        Color temp = g.getColor();
        if (!controller.isVoronoi()) g.setColor(delaunayColor);
        else if (dt.contains(initialTriangle)) g.setColor(this.getBackground());
        else g.setColor(voronoiColor);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        g.setColor(temp);

        // If no colors then we can clear the color table
        if (!controller.isColorful()) colorTable.clear();

        // Draw the appropriate picture
        if (controller.isVoronoi())
            drawAllVoronoi(controller.isColorful(), true);
        else drawAllDelaunay(controller.isColorful());

        // Draw any extra info due to the mouse-entry switches
        temp = g.getColor();
        g.setColor(Color.white);
        if (controller.showingCircles()) drawAllCircles();
        if (controller.showingDelaunay()) drawAllDelaunay(false);
        if (controller.showingVoronoi()) drawAllVoronoi(false, false);
        g.setColor(temp);
    }

    /**
     * Draw all the Delaunay triangles.
     * @param withFill true iff drawing Delaunay triangles with fill colors
     */
    public void drawAllDelaunay (boolean withFill) {
        for (Triangle triangle : dt) {
            Pnt[] vertices = triangle.toArray(new Pnt[0]);
            draw(vertices, withFill? getColor(triangle) : null);
        }
    }

    /**
     * Draw all the Voronoi cells.
     * @param withFill true iff drawing Voronoi cells with fill colors
     * @param withSites true iff drawing the site for each Voronoi cell
     */
    public synchronized void drawAllVoronoi (boolean withFill, boolean withSites) {
        // Keep track of sites done; no drawing for initial triangles sites
        HashSet<Pnt> done = new HashSet<Pnt>(initialTriangle);
        for (Triangle triangle : dt)
            for (Pnt site: triangle) {
                if (done.contains(site)) continue;
                done.add(site);
                List<Triangle> list = dt.surroundingTriangles(site, triangle);
                Pnt[] vertices = new Pnt[list.size()];
                int i = 0;
                for (Triangle tri: list)
                    vertices[i++] = tri.getCircumcenter();
                draw(vertices, withFill? getColor(site) : null);
                if (withSites) draw(site);
        	}
    }

    /**
     * Draw all the empty circles (one for each triangle) of the DT.
     */
    public void drawAllCircles () {
        // Loop through all triangles of the DT
        for (Triangle triangle: dt) {
            // Skip circles involving the initial-triangle vertices
            if (triangle.containsAny(initialTriangle)) continue;
            Pnt c = triangle.getCircumcenter();
            double radius = c.subtract(triangle.get(0)).magnitude();
            draw(c, radius, null);
        }
    }

}
