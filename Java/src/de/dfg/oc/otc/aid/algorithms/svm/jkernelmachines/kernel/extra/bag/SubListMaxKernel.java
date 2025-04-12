/**
    This file is part of JkernelMachines.

    JkernelMachines is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    JkernelMachines is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with JkernelMachines.  If not, see <http://www.gnu.org/licenses/>.

    Copyright David Picard - 2010

*/
package de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.kernel.extra.bag;

import java.io.Serializable;
import java.util.List;

import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.kernel.Kernel;

/**
 * max value of kernel between two bags
 * @author dpicard
 *
 * @param <S>
 * @param <T>
 */
public class SubListMaxKernel<S,T extends List<S>> extends Kernel<T> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1591055803491554966L;
	
	private int from = 0;
	private int to = 0;
	private Kernel<S> kernel;
	
	

	

	/**
	 * @param from
	 * @param to
	 * @param kernel
	 */
	public SubListMaxKernel(int from, int to, Kernel<S> kernel) {
		this.from = from;
		this.to = to;
		this.kernel = kernel;
	}

	@Override
	public double valueOf(T t1, T t2) {
		double max = 0;
		if(to > t1.size())
			to = t1.size();
		if(to > t2.size())
			to = t2.size();
		
		for(int i = from; i < to; i++)
		for(int j = from; j < to; j++)
		{
			double v = kernel.valueOf(t1.get(i), t2.get(j));
			if(v > max)
				max = v;
		}
		
		return max;
	}

	@Override
	public double valueOf(T t1) {
		return valueOf(t1, t1);
	}



	
	
}

	

