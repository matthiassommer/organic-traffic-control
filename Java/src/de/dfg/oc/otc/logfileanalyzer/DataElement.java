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

package de.dfg.oc.otc.logfileanalyzer;

import javax.swing.table.DefaultTableModel;

/**
 * A {@code DataElement} stores all classifiers sets (i. e. population,
 * match set, and action set) of one iteration. Each classifier set is stored in
 * a {@code DefaultTableModel} that can be directly displayed in the user
 * interface. Furthermore, a {@code DataElement} contains references to the
 * {@code DataElement}s of the previous and next iteration.
 *
 * @author Clemens Gersbacher, Holger Prothmann
 */
public class DataElement {
    /**
     * Contains the action set.
     */
    private DefaultTableModel actionSet;

    /**
     * The LCS input.
     */
    private String input = "";

    /**
     * The iteration number.
     */
    private double iteration = -1;

    /**
     * Contains the match set.
     */
    private DefaultTableModel matchSet;

    /**
     * Reference to the {@code DataElement} of the next iteration.
     */
    private DataElement nextElement;

    /**
     * Contains the classifier population.
     */
    private DefaultTableModel population;

    /**
     * Reference to the {@code DataElement} of the previous iteration.
     */
    private DataElement previousElement;

    /**
     * Creates an empty {@code DataElement}.
     */
    public DataElement() {
        String[] columnNames = LogFileAnalyzer.getInstance().getColumnNames();
        int numberOfColumns = columnNames.length;

        population = new DefaultTableModel(0, numberOfColumns);
        matchSet = new DefaultTableModel(0, numberOfColumns);
        actionSet = new DefaultTableModel(0, numberOfColumns);

        population.setColumnIdentifiers(columnNames);
        matchSet.setColumnIdentifiers(columnNames);
        actionSet.setColumnIdentifiers(columnNames);

        nextElement = this;
        previousElement = this;
    }

    /**
     * Returns the {@code DefaultTableModel} containing the action set.
     *
     * @return the {@code DefaultTableModel} containing the action set
     */
    public final DefaultTableModel getActionSet() {
        return actionSet;
    }

    /**
     * Returns the input of this {@code DataElement}.
     *
     * @return the input of this {@code DataElement}
     */
    public final String getInput() {
        return input;
    }

    /**
     * Returns the iteration of this {@code DataElement}.
     *
     * @return the iteration of this {@code DataElement}
     */
    public final double getIteration() {
        return iteration;
    }

    /**
     * Returns the {@code DefaultTableModel} containing the match set.
     *
     * @return the {@code DefaultTableModel} containing the match set
     */
    public final DefaultTableModel getMatchSet() {
        return matchSet;
    }

    /**
     * Returns a reference to the {@code DataElement} containing the
     * classifier sets for the next iteration. If there is no next element, the
     * reference points to {@code this}. References are managed by the
     * {@code DataMemory}.
     *
     * @return the {@code DataElement} containing the classifier sets for
     * the next iteration
     */
    public final DataElement getNextElement() {
        return nextElement;
    }

    /**
     * Returns the {@code DefaultTableModel} containing the population.
     *
     * @return the {@code DefaultTableModel} containing the population
     */
    public final DefaultTableModel getPopulation() {
        return population;
    }

    /**
     * Returns a reference to the {@code DataElement} containing the
     * classifier sets for the previous iteration. If there is no previous
     * element, the reference points to {@code this}. References are
     * managed by the {@code DataMemory}.
     *
     * @return the {@code DataElement} containing the classifier sets for
     * the previous iteration
     */
    public final DataElement getPreviousElement() {
        return previousElement;
    }

    /**
     * Sets the {@code DefaultTableModel} containing the action set.
     *
     * @param actionSet the {@code DefaultTableModel} containing the action set
     */
    public final void setActionSet(final DefaultTableModel actionSet) {
        this.actionSet = actionSet;
    }

    /**
     * Sets the input of this {@code DataElement}.
     *
     * @param input the input of this {@code DataElement}
     */
    public final void setInput(final String input) {
        this.input = input;
    }

    /**
     * Sets the iteration number.
     *
     * @param iteration the iteration number
     */
    public final void setIteration(final double iteration) {
        this.iteration = iteration;
    }

    /**
     * Sets the {@code DefaultTableModel} containing the match set.
     *
     * @param matchSet the {@code DefaultTableModel} containing the match set
     */
    public final void setMatchSet(final DefaultTableModel matchSet) {
        this.matchSet = matchSet;
    }

    /**
     * Sets the reference to the {@code DataElement} containing the
     * classifier sets for the next iteration.
     *
     * @param element reference to the {@code DataElement} containing the
     *                classifier sets for the next iteration
     */
    public final void setNextElement(final DataElement element) {
        this.nextElement = element;
    }

    /**
     * Sets the {@code DefaultTableModel} containing the population.
     *
     * @param population the {@code DefaultTableModel} containing the population
     */
    public final void setPopulation(final DefaultTableModel population) {
        this.population = population;
    }

    /**
     * Sets the reference to the {@code DataElement} containing the
     * classifier sets for the previous iteration.
     *
     * @param element reference to the {@code DataElement} containing the
     *                classifier sets for the previous iteration
     */
    public final void setPreviousElement(final DataElement element) {
        this.previousElement = element;
    }
}
