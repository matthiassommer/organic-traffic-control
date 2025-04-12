/*
 * LogFileAnalyzer for Learning Classifier Systems 
 * 
 * Copyright (C) 2008 
 * Clemens Gersbacher <clgersbacher@web.de>, 
 * Holger Prothmann <holger.prothmann@kit.edu>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package de.dfg.oc.otc.logfileanalyzer.gui;

import java.awt.event.WindowListener;

/**
 * WindowListener, cleans up after a {@code ChartFrame} was closed.
 *
 * @author Clemens Gersbacher, Holger Prothmann
 */
class ChartClosedListener implements WindowListener {
    /**
     * {@code ChartFrame} this {@code ChartClosedListener} is attached.
     * to
     */
    private final ChartFrame myChartFrame;

    /**
     * Creates a new {@code ChartClosedListener} that listens to
     * {@code WindowEvent}s of the {@code ChartFrame} given as
     * parameter.
     *
     * @param myChartFrame {@code ChartFrame} this {@code ChartClosedListener}
     *                     is attached to
     */
    public ChartClosedListener(final ChartFrame myChartFrame) {
        this.myChartFrame = myChartFrame;
    }

    public void windowActivated(final java.awt.event.WindowEvent e) {
    }

    public void windowClosed(final java.awt.event.WindowEvent e) {
    }

    /**
     * Removes a {@code ChartFrame} from the {@code chartFrames}
     * -Vector of the {@code TableFrame}-class when the
     * {@code ChartFrame} gets closed.
     *
     * @param e
     */
    public void windowClosing(final java.awt.event.WindowEvent e) {
        myChartFrame.getMyTableFrame().removeChartFrame(myChartFrame);
        myChartFrame.dispose();
    }

    public void windowDeactivated(final java.awt.event.WindowEvent e) {
    }

    public void windowDeiconified(final java.awt.event.WindowEvent e) {
    }

    public void windowIconified(final java.awt.event.WindowEvent e) {
    }

    public void windowOpened(final java.awt.event.WindowEvent e) {
    }
}
