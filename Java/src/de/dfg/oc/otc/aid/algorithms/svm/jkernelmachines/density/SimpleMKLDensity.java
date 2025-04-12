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

    Copyright David Picard - 2013

 */
package de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.density;

import static java.lang.Math.abs;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.kernel.Kernel;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.kernel.SimpleCacheKernel;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.kernel.ThreadedKernel;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.kernel.adaptative.ThreadedSumKernel;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.threading.ThreadedMatrixOperator;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.type.TrainingSample;
import de.dfg.oc.otc.aid.algorithms.svm.jkernelmachines.util.DebugPrinter;

/**
 * Density estimation using an adapted version of SimpleMKL.
 * 
 * <p>
 * In case C=1, it might be equivalent to a Parzen estimator, albeight
 * performing kernel selection
 * </p>
 * 
 * @author picard
 * 
 */
public class SimpleMKLDensity<T> implements DensityFunction<T> {

	private static final long serialVersionUID = -7669785464848822979L;
	private ArrayList<Kernel<T>> kernels;
	private ArrayList<Double> kernelWeights;

	private int maxIteration = 50;
	private double C = 1.;
	private double numPrec = 1.e-12, epsKTT = 0.1, epsDG = 0.01, epsGS = 1.e-8,
			eps = 1.e-8;
	private boolean checkDualGap = true, checkKTT = false;

	private SDCADensity<T> svm;

	private DecimalFormat format = new DecimalFormat("#0.0000");
	DebugPrinter debug = new DebugPrinter();

	private ArrayList<TrainingSample<T>> list;

	/**
	 * default constructor
	 */
	public SimpleMKLDensity() {
		kernels = new ArrayList<Kernel<T>>();
		kernelWeights = new ArrayList<Double>();
	}

	/**
	 * adds a kernel to the MKL problem
	 * 
	 * @param k
	 */
	public void addKernel(Kernel<T> k) {
		if (!kernels.contains(k)) {
			kernels.add(k);
			kernelWeights.add(1.0);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.lip6.jkernelmachines.density.DensityFunction#train(java.lang.Object)
	 */
	@Override
	public void train(T e) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.lip6.jkernelmachines.density.DensityFunction#train(java.util.List)
	 */
	@Override
	public void train(List<T> l) {
		list = new ArrayList<TrainingSample<T>>(l.size());
		for (int i = 0; i < l.size(); i++) {
			list.add(new TrainingSample<T>(l.get(i), 1));
		}
		// caching matrices
		ArrayList<SimpleCacheKernel<T>> km = new ArrayList<SimpleCacheKernel<T>>();
		ArrayList<Double> dm = new ArrayList<Double>();
		double dm0 = 1. / kernels.size();
		for (int i = 0; i < kernels.size(); i++) {
			Kernel<T> k = kernels.get(i);
			SimpleCacheKernel<T> csk = new SimpleCacheKernel<T>(
					new ThreadedKernel<T>(k), list);
			csk.setName(k.toString());
			km.add(csk);
			dm.add(dm0);
		}

		// ------------------------------
		// INIT
		// ------------------------------
		// creating kernel
		ThreadedSumKernel<T> tsk = buildKernel(km, dm);
		retrainSVM(tsk, l);
		double oldObj = svmObj(km, dm, l);
		ArrayList<Double> grad = gradSVM(km, dm, l);
		debug.println(1, "iter \t | \t obj \t\t | \t dualgap \t | \t KKT");
		debug.println(1, "init \t | \t " + format.format(oldObj) + " \t | \t "
				+ Double.NaN + " \t\t | \t " + Double.NaN);

		// ------------------------------
		// START
		// ------------------------------
		boolean loop = true;
		int iteration = 1;
		while (loop && iteration < maxIteration) {

			// ------------------------------
			// UPDATE WEIGHTS
			// ------------------------------
			double newObj = mklUpdate(grad, km, dm, l);

			// ------------------------------
			// numerical cleaning
			// ------------------------------
			double sum = 0;
			for (int i = 0; i < dm.size(); i++) {
				double d = dm.get(i);
				if (d < numPrec)
					d = 0.;
				sum += d;
				dm.set(i, d);
			}
			for (int i = 0; i < dm.size(); i++) {
				double d = dm.get(i);
				dm.set(i, d / sum);
			}
			// verbosity
			debug.println(3, "loop : dm after cleaning = " + dm);

			// ------------------------------
			// approximate KTT condition
			// ------------------------------
			grad = gradSVM(km, dm, l);
			debug.println(3, "loop : grad = " + grad);

			// searching min and max grad for non nul dm
			double gMin = Double.POSITIVE_INFINITY, gMax = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < dm.size(); i++) {
				double d = dm.get(i);
				if (d > 0) {
					double g = grad.get(i);
					if (g <= gMin)
						gMin = g;
					if (g >= gMax)
						gMax = g;
				}
			}
			double kttConstraint = Math.abs((gMin - gMax) / gMin);
			debug.println(3, "Condition check : KTT gmin = " + gMin
					+ " gmax = " + gMax);
			// searching grad min over zeros dm
			double gZeroMin = Double.POSITIVE_INFINITY;
			for (int i = 0; i < km.size(); i++) {
				if (dm.get(i) < numPrec) {
					double g = grad.get(i);
					if (g < gZeroMin)
						gZeroMin = g;
				}
			}
			boolean kttZero = (gZeroMin > gMax) ? true : false;

			// -------------------------------
			// Duality gap
			// -------------------------------

			// searching -grad max
			double max = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < dm.size(); i++) {
				double g = -grad.get(i);
				if (g > max)
					max = g;
			}
			// computing sum alpha
			sum = 0.;
			double alp[] = svm.getAlphas();
			for (int i = 0; i < alp.length; i++)
				sum += abs(alp[i]);
			double dualGap = (newObj + max - sum) / newObj;

			// --------------------------------
			// Verbosity
			// --------------------------------

			debug.println(1, "iter \t | \t obj \t\t | \t dualgap \t | \t KKT");
			debug.println(1, iteration + " \t | \t " + format.format(newObj)
					+ " \t | \t " + format.format(dualGap) + " \t | \t "
					+ format.format(kttConstraint));

			// ------------------------------
			// STOP CRITERIA
			// ------------------------------
			boolean stop = false;
			// ktt
			if (kttConstraint < epsKTT && kttZero && checkKTT) {
				debug.println(1, "KTT conditions met, possible stoping");
				stop = true;
			}
			// dualgap
			if (dualGap < epsDG && checkDualGap) {
				debug.println(1, "DualGap reached, possible stoping");
				stop = true;
			}
			// stagnant gradient
			if (Math.abs(oldObj - newObj) < numPrec) {
				debug.println(1,
						"No improvement during iteration, stoping (old : "
								+ oldObj + " new : " + newObj + ")");
				stop = true;
			}
			if (stop)
				loop = false;

			// storing newObj
			oldObj = newObj;

			iteration++;
		}

		kernelWeights = dm;
		// creating kernel
		tsk = buildKernel(km, dm);
		retrainSVM(tsk, l);

	}

	/**
	 * computing the objective function value
	 * 
	 * @param km list of kernels
	 * @param dm associated weights
	 * @param l list of training samples
	 * @return primal objective
	 */
	private double svmObj(List<SimpleCacheKernel<T>> km, List<Double> dm,
			final List<T> l) {

		debug.print(3, "[");
		// creating kernel
		ThreadedSumKernel<T> k = buildKernel(km, dm);
		SimpleCacheKernel<T> csk = new SimpleCacheKernel<T>(k, list);
		double kmatrix[][] = csk.getKernelMatrix(list);

		debug.print(3, "-");
		// updating svm
		retrainSVM(csk, l);
		final double alp[] = svm.getAlphas();
		debug.print(3, "-");

		// verbosity
		debug.println(4, "svmObj : alphas = " + Arrays.toString(alp));
		// debug.println(4, "svmObj : b="+svm.getB());

		// parallelized
		final double[] resLine = new double[kmatrix.length];
		ThreadedMatrixOperator objFactory = new ThreadedMatrixOperator() {
			@Override
			public void doLines(double[][] matrix, int from, int to) {
				for (int index = from; index < to; index++) {
					if (abs(alp[index]) > 0) {
						double al1 = abs(alp[index]);
						for (int j = 0; j < matrix[index].length; j++) {
							if (abs(alp[j]) > 0)
								resLine[index] += al1 * abs(alp[j])
										* matrix[index][j];
						}
					}
				}
			}
		};

		objFactory.getMatrix(kmatrix);
		double obj1 = 0;
		for (double d : resLine)
			obj1 += d;

		double obj2 = 0;
		for (int i = 0; i < l.size(); i++) {
			obj2 += abs(alp[i]);
		}

		double obj = -0.5 * obj1 + obj2;

		debug.print(3, "]");

		if (obj < 0) {
			debug.println(1,
					"A fatal error occured, please report to picard@ensea.fr");
			debug.println(1, "error obj : " + obj + " obj1:" + obj1 + " obj2:"
					+ obj2);
			debug.println(1, "alp : " + Arrays.toString(alp));
			debug.println(1, "resline : " + Arrays.toString(resLine));
			// System.exit(0);
			// return Double.POSITIVE_INFINITY;
		}

		return obj;
	}

	/**
	 * computing the gradient of the objective function
	 * 
	 * @param km list of kernels
	 * @param dm associated weights
	 * @param l list of training samples
	 * @return gradient w.r.t. kernel weights
	 */
	private ArrayList<Double> gradSVM(List<SimpleCacheKernel<T>> km,
			List<Double> dm, final List<T> l) {
		// creating kernel
		ThreadedSumKernel<T> tsk = buildKernel(km, dm);

		// updating svm
		retrainSVM(tsk, l);
		final double alp[] = svm.getAlphas();

		// computing grad
		ArrayList<Double> grad = new ArrayList<Double>();
		for (int i = 0; i < km.size(); i++) {
			Kernel<T> k = km.get(i);
			double kmatrix[][] = k.getKernelMatrix(list);

			// parallelized
			final double[] resLine = new double[kmatrix.length];
			ThreadedMatrixOperator gradFactory = new ThreadedMatrixOperator() {
				@Override
				public void doLines(double[][] matrix, int from, int to) {
					for (int index = from; index < to; index++) {
						if (alp[index] > 0) {
							double al1 = -0.5 * alp[index];
							for (int j = 0; j < matrix[index].length; j++) {
								resLine[index] += al1 * alp[j]
										* matrix[index][j];
							}
						}
					}
				}
			};

			gradFactory.getMatrix(kmatrix);
			double g = 0;
			for (double d : resLine)
				g += d;
			grad.add(i, g);
		}

		return grad;
	}

	/**
	 * performs an update of the weights in the mkl
	 * 
	 * @param km
	 * @param dm
	 * @param l
	 * @return value of objective fonction
	 */
	private double mklUpdate(List<Double> gradOld,
			List<SimpleCacheKernel<T>> km, List<Double> dmOld, List<T> l) {
		// save
		ArrayList<Double> dm = new ArrayList<Double>();
		dm.addAll(dmOld);
		ArrayList<Double> grad = new ArrayList<Double>();
		grad.addAll(gradOld);

		// init obj
		double costMin = svmObj(km, dm, l);
		double costOld = costMin;

		// norme du gradient
		double normGrad = 0;
		for (int i = 0; i < grad.size(); i++)
			normGrad += grad.get(i) * grad.get(i);
		normGrad = Math.sqrt(normGrad);
		for (int i = 0; i < grad.size(); i++) {
			double g = grad.get(i) / normGrad;
			grad.set(i, g);
		}

		// finding max dm
		double max = Double.NEGATIVE_INFINITY;
		int indMax = 0;
		for (int i = 0; i < dm.size(); i++) {
			double d = dm.get(i);
			if (d > max) {
				max = d;
				indMax = i;
			}
		}
		double gradMax = grad.get(indMax);

		// reduced gradient
		ArrayList<Double> desc = new ArrayList<Double>();
		double sum = 0;
		for (int i = 0; i < dm.size(); i++) {
			grad.set(i, grad.get(i) - gradMax);
			double d = -grad.get(i);
			if (!(dm.get(i) > 0 || grad.get(i) < 0))
				d = 0;
			sum += -d;
			desc.add(i, d);
		}
		desc.set(indMax, sum); // NB : grad.get(indMax) == 0
		// verbosity
		debug.println(3, "mklupdate : grad = " + grad);
		debug.println(3, "mklupdate : desc = " + desc);

		// optimal stepsize
		double stepMax = Double.POSITIVE_INFINITY;
		for (int i = 0; i < desc.size(); i++) {
			double d = desc.get(i);
			if (d < 0) {
				double min = -dm.get(i) / d;
				if (min < stepMax)
					stepMax = min;
			}
		}
		if (Double.isInfinite(stepMax) || stepMax == 0)
			return costMin;
		if (stepMax > 0.1)
			stepMax = 0.1;

		// small loop
		double costMax = 0;
		while (costMax < costMin) {
			ArrayList<Double> dmNew = new ArrayList<Double>();
			for (int i = 0; i < dm.size(); i++) {
				dmNew.add(i, dm.get(i) + desc.get(i) * stepMax);
			}

			// verbosity
			debug.println(3, "* descent : dm = " + dmNew);

			costMax = svmObj(km, dmNew, l);

			if (costMax < costMin) {
				costMin = costMax;
				dm = dmNew;

				// numerical cleaning
				// empty

				// keep direction in admisible cone
				for (int i = 0; i < desc.size(); i++) {
					double d = 0;
					if ((dm.get(i) > numPrec || desc.get(i) > 0))
						d = desc.get(i);
					desc.set(i, d);
				}
				sum = 0;
				for (int i = 0; i < indMax; i++)
					sum += desc.get(i);
				for (int i = indMax + 1; i < desc.size(); i++)
					sum += desc.get(i);
				desc.set(indMax, -sum);

				// nex stepMap
				stepMax = Double.POSITIVE_INFINITY;
				for (int i = 0; i < desc.size(); i++) {
					double d = desc.get(i);
					if (d < 0) {
						double Dm = dm.get(i);
						if (Dm < numPrec)
							Dm = 0.;
						double min = -Dm / d;
						if (min < stepMax)
							stepMax = min;
					}
				}
				if (Double.isInfinite(stepMax))
					stepMax = 0.;
				else
					costMax = 0;
			}

			// verbosity
			debug.print(2, "*");
			debug.println(3, " descent : costMin : " + costMin + " costOld : "
					+ costOld + " stepMax : " + stepMax);
		}

		// verbosity
		debug.println(3, "mklupdate : dm after descent = " + dm);

		// -------------------------------------
		// Golden Search
		// -------------------------------------
		double stepMin = 0;
		int indMin = 0;
		double gold = (1. + Math.sqrt(5)) / 2.;

		ArrayList<Double> cost = new ArrayList<Double>(4);
		cost.add(0, costMin);
		cost.add(1, 0.);
		cost.add(2, 0.);
		cost.add(3, costMax);

		ArrayList<Double> step = new ArrayList<Double>(4);
		step.add(0, 0.);
		step.add(1, 0.);
		step.add(2, 0.);
		step.add(3, stepMax);

		double deltaMax = stepMax;
		while (stepMax - stepMin > epsGS * deltaMax && stepMax > eps) {
			double stepMedR = stepMin + (stepMax - stepMin) / gold;
			double stepMedL = stepMin + (stepMedR - stepMin) / gold;

			// setting cost array
			cost.set(0, costMin);
			cost.set(3, costMax);
			// setting step array
			step.set(0, stepMin);
			step.set(3, stepMax);

			// cost medr
			ArrayList<Double> dMedR = new ArrayList<Double>();
			for (int i = 0; i < dm.size(); i++) {
				double d = dm.get(i);
				dMedR.add(i, d + desc.get(i) * stepMedR);
			}
			double costMedR = svmObj(km, dMedR, l);

			// cost medl
			ArrayList<Double> dMedL = new ArrayList<Double>();
			for (int i = 0; i < dm.size(); i++) {
				double d = dm.get(i);
				dMedL.add(i, d + desc.get(i) * stepMedL);
			}
			double costMedL = svmObj(km, dMedL, l);

			cost.set(1, costMedL);
			step.set(1, stepMedL);
			cost.set(2, costMedR);
			step.set(2, stepMedR);

			// search min cost
			double min = Double.POSITIVE_INFINITY;
			indMin = -1;
			for (int i = 0; i < 4; i++) {
				if (cost.get(i) < min) {
					indMin = i;
					min = cost.get(i);
				}
			}

			debug.println(3, "golden search : cost = [" + costMin + " "
					+ costMedL + " " + costMedR + " " + costMax + "]");
			debug.println(3, "golden search : step = [" + stepMin + " "
					+ stepMedL + " " + stepMedR + " " + stepMax + "]");
			debug.println(3, "golden search : costOpt=" + cost.get(indMin)
					+ " costOld=" + costOld);

			// update search
			switch (indMin) {
			case 0:
				stepMax = stepMedL;
				costMax = costMedL;
				break;
			case 1:
				stepMax = stepMedR;
				costMax = costMedR;
				break;
			case 2:
				stepMin = stepMedL;
				costMin = costMedL;
				break;
			case 3:
				stepMin = stepMedR;
				costMin = costMedR;
				break;
			default:
				debug.println(1, "Error in golden search.");
				return costMin;
			}

			// verbosity
			debug.print(2, ".");

		}
		// verbosity
		debug.println(2, "");

		// final update
		double costNew = cost.get(indMin);
		double stepNew = step.get(indMin);

		dmOld.clear();
		dmOld.addAll(dm);

		if (costNew < costOld) {
			for (int i = 0; i < dmOld.size(); i++) {
				double d = dm.get(i);
				dmOld.set(i, d + desc.get(i) * stepNew);
			}
		}

		// creating kernel
		ThreadedSumKernel<T> tsk = buildKernel(km, dm);
		// updating svm
		retrainSVM(tsk, l);

		// verbosity
		debug.print(3, "mklupdate : dm = " + dmOld);

		return costNew;
	}

	private ThreadedSumKernel<T> buildKernel(List<SimpleCacheKernel<T>> km,
			List<Double> dm) {
		long startTime = System.currentTimeMillis();
		ThreadedSumKernel<T> tsk = new ThreadedSumKernel<T>();
		for (int i = 0; i < km.size(); i++)
			if (dm.get(i) > numPrec)
				tsk.addKernel(km.get(i), dm.get(i));
		long stopTime = System.currentTimeMillis() - startTime;
		debug.println(3, "building kernel : time=" + stopTime);
		return tsk;
	}

	/**
	 * update svm classifier (update alphas)
	 * 
	 * @param k
	 * @param l
	 */
	private void retrainSVM(Kernel<T> k, List<T> l) {

		// default svm algorithm is jkernelmachines
		if (svm == null) {
			SDCADensity<T> sdca = new SDCADensity<T>(k);
			sdca.setE(5);
			svm = sdca;
		}
		// new settings
		svm.setKernel(k);
		svm.setC(C);
		svm.train(l);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.lip6.jkernelmachines.density.DensityFunction#valueOf(java.lang.Object)
	 */
	@Override
	public double valueOf(T e) {
		if (svm != null) {
			return svm.valueOf(e);
		}
		return -1;
	}

	public int getMaxIteration() {
		return maxIteration;
	}

	public void setMaxIteration(int maxIteration) {
		this.maxIteration = maxIteration;
	}

	public double getC() {
		return C;
	}

	public void setC(double c) {
		C = c;
	}

}
