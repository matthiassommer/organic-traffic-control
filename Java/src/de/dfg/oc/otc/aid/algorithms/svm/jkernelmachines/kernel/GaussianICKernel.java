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
package de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.kernel;

import java.util.Map;

/**
 * Not so useful <key,value> based caching method for Gaussian kernels
 * @author picard
 *
 * @param <S>
 * @param <T>
 */
public class GaussianICKernel<S, T> extends GaussianKernel<S> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8504726802546629864L;


	private double[][] matrix;
	private Map<S, Integer> map;
	private double gamma = 1.0;
	
	/**
	 * Constructor using underlying indexed kernel k, supposed to be Gaussian.
	 * @param k the underlying kernel
	 */
	public GaussianICKernel(IndexedCacheKernel<S, T> k) {
		
		this.map = k.getMap();
		double[][] m = k.getCacheMatrix();
		
		matrix = new double[m.length][m.length];
		for(int x = 0 ; x < m.length ; x++)
		for(int y = 0 ; y < m.length ; y++) {
			double tmp = - Math.log(m[x][y]);
			if(tmp < 1e-4)
				matrix[x][y] = 0;
			else
				matrix[x][y] = tmp;
		}
		
	}
	
	@Override
	public double distanceValueOf(S t1, S t2) {
		assert(map.containsKey(t1) && map.containsKey(t2));
		
//		{
//			System.err.println("<"+t1+","+t2+"> not in matrix !!!");
//			return 0;
//		}
		int id1 = map.get(t1);
		int id2 = map.get(t2);
		
		return ((double)(float)matrix[id1][id2]);
	}

	@Override
	public double getGamma() {
		return gamma;
	}

	@Override
	public void setGamma(double gamma) {
		this.gamma = gamma;
	}

	@Override
	public double valueOf(S t1, S t2) {
		double tmp = -gamma*distanceValueOf(t1, t2);
		if(tmp >= 10) //num cleaning
			return 0;
		return Math.exp(-gamma*distanceValueOf(t1, t2));
	}

	@Override
	public double valueOf(S t1) {
		return 1.0;
	}


}
