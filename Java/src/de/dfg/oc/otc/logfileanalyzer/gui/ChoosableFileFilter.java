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

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.StringTokenizer;

/**
 * Provides a filter for the "Open file"-dialogue.
 *
 * @author Clemens Gersbacher, Holger Prothmann
 */
public class ChoosableFileFilter extends FileFilter implements java.io.FileFilter {
    /**
     * {@code true} to show folders in the dialog.
     */
    private boolean acceptDirs = true;

    /**
     * Textual description of the accepted file-type.
     */
    private final String description;

    /**
     * A {@code String} defining the accepted files (e.g. "*.log" accepts
     * all files having the extend "log")
     */
    private final String pattern;

    /**
     * Creates a new file filter.
     *
     * @param description a textual description of the accepted file-type
     * @param pattern     a {@code String} defining the accepted files (e.g.
     *                    "*.log" accepts all files having the extend "log")
     * @param acceptDirs  {@code true} to show folders in the dialog
     */
    public ChoosableFileFilter(final String description, final String pattern, final boolean acceptDirs) {
        super();
        this.pattern = pattern;
        this.description = description;
        this.acceptDirs = acceptDirs;
    }

    /**
     * Checks if a given file fits in the defined pattern.
     *
     * @param file file to be checked
     * @return {@code true} if the file matches the pattern (and will be
     * displayed)
     */
    public final boolean accept(final File file) {
        if (file.isDirectory() && acceptDirs) {
            return true;
        }

        String filename = file.getName();
        StringTokenizer tokenizer = new StringTokenizer(pattern, "*");
        int index = 0;
        String currentToken = null;

        while (tokenizer.hasMoreTokens()) {
            currentToken = tokenizer.nextToken();
            if (index == 0 && pattern.indexOf('*') != 0 && filename.indexOf(currentToken) != 0) {
                return false;
            }

            if (filename.indexOf(currentToken, index) == -1) {
                return false;
            } else {
                index = filename.indexOf(currentToken, index);
            }
        }

        if (currentToken != null) {
            if (filename.length() - index > currentToken.length()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a textual description of the accepted file-type.
     *
     * @return a textual description of the accepted file-type
     */
    public final String getDescription() {
        return description;
    }
}
