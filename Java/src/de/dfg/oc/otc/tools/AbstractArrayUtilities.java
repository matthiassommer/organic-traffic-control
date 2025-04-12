package de.dfg.oc.otc.tools;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;

/**
 * Provides a toString-method for arrays.
 *
 * @author hpr
 */
public abstract class AbstractArrayUtilities {
    /**
     * Return a string representation of a given array.
     *
     * @param array to array that string representation is needed
     * @return a string representation of the given array
     */
    public static String arrayToString(Object array) {
        if (array == null) {
            return "[NULL]";
        } else {
            if (array instanceof Hashtable) {
                array = ((Hashtable) array).entrySet().toArray();
            } else if (array instanceof HashSet) {
                array = ((HashSet) array).toArray();
            } else if (array instanceof Collection) {
                array = ((Collection) array).toArray();
            }

            final int length = Array.getLength(array);
            final int lastItem = length - 1;
            final StringBuilder sb = new StringBuilder("[");

            Object obj;
            for (int i = 0; i < length; i++) {
                obj = Array.get(array, i);
                if (obj != null) {
                    sb.append(obj);
                } else {
                    sb.append("[NULL]");
                }
                if (i < lastItem) {
                    sb.append(", ");
                }
            }

            sb.append("]");
            return sb.toString();
        }
    }
}
