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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * ActionListener, creates a new {@code ChartFrame} and stores it in the
 * {@code TableFrame}'s vector of {@code ChartFrame}s.
 *
 * @author Clemens Gersbacher, Holger Prothmann
 */
class ChartOpenedListener implements ActionListener {
    /**
     * Reference to the {@code TableFrame}-object.
     */
    private final TableFrame myTableFrame;

    /**
     * Constructor. Creates a new {@code ChartOpenListener}.
     *
     * @param myTableFrame calling {@code TableFrame}-object.
     */
    public ChartOpenedListener(final TableFrame myTableFrame) {
        this.myTableFrame = myTableFrame;
    }

    /**
     * Creates a new {@code ChartFrame}-object and stores it in the
     * {@code TableFrame}'s vector of {@code ChartFrame}s. This method
     * is invoked when "View->New chart" is selected in the menu of the
     * {@code TableFrame}-class.
     *
     * @param e
     */
    public void actionPerformed(final ActionEvent e) {
        ChartFrame newChartFrame = new ChartFrame(myTableFrame);
        myTableFrame.addChartFrame(newChartFrame);
    }
}
