package com.andreamazzon.handout10;

import static org.junit.jupiter.api.Assertions.*;

import java.text.DecimalFormat;
import java.util.Random;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloMultiAssetBlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.models.BachelierModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This class performs some tests on an exchange option. In particular, we check
 * the dependence of the price on the correlation between the assets and the
 * accuracy of the Monte-Carlo method, both when the (two-dimensional)
 * BrownianMotion object driving the underlying gets a given seed and when the
 * seed is random and the price is computed a given number of times.
 *
 * @author Andrea Mazzon
 *
 */
public class ExchangeOptionTests {

	private final static DecimalFormat formatterPercentage = new DecimalFormat("0.0000 %");
	private final static DecimalFormat formatterCorrelation = new DecimalFormat("0.00");
	private final static DecimalFormat formatterValue = new DecimalFormat("0.0000");

	/*
	 * Parameters for the model: remember that we want to construct a
	 * two-dimensional process
	 */
	private final double[] initialPrices = { 100.0, 100.0 };

	private final double[] volatilities = { 0.25, 0.3 };

	private final double riskFreeRate = 0.2;

	private double correlation = 0.3;
	/*
	 * correlation matrix: to be given to MonteCarloMultiAssetBlackScholesModel to
	 * construct the simulation of two possibly correlated geometric Brownian
	 * motions
	 */
	private final double[][] correlationMatrix = { { 1.0, correlation }, { correlation, 1.0 } };

	// parameters for simulation and time discretization
	private final int numberOfSimulations = 20000;// number of paths
	final int seed = 1897;

	private final double initialTime = 0;
	private final double maturity = 1.0;
	private final int numberOfTimeSteps = 100;
	private final double timeStep = maturity / numberOfTimeSteps;
	private TimeDiscretization times = new TimeDiscretizationFromArray(initialTime, numberOfTimeSteps, timeStep);

	// tolerance for the test, in percentage
	private final double percentageTolerance = 2;

	// allowed failure percentage for the second test
	private final double allowedFailurePercentage = 10;

	// we call the overloaded constructor
	final ExchangeOption exchangeOption = new ExchangeOption(maturity);

	/*
	 * This method computes the analytical value of an exchange option with maturity
	 * given by the field of the class, for a two-dimensional Brownian motion with
	 * parameters also given above, using the analytic formula you can find in the
	 * exercise sheet.
	 */
	private double computeAnalyticalValue() {
		final double sigma = Math.sqrt(volatilities[0] * volatilities[0]
				- 2 * volatilities[0] * volatilities[1] * correlation + volatilities[1] * volatilities[1]);

		// The interest rate must be zero, because we are discounting!
		return AnalyticFormulas.blackScholesOptionValue(initialPrices[0], 0, sigma, maturity, initialPrices[1]);
	}

	/**
	 * It prints the price of the exchange option for different values of the
	 * correlation between the two assets
	 *
	 * @throws CalculationException
	 */
	@Test
	public void testCorrelation() throws CalculationException {
		/*
		 * We simulate a two-dimensional Brownian motion (B=(B^1,B^2) such that
		 * W_1 = B_1
		 * W_2 = rho B_1 + sqrt(1-rho^2)B^2
		 */
		final BrownianMotion brownian = new BrownianMotionFromMersenneRandomNumbers(times, 2, numberOfSimulations, seed);

		System.out.println("Correlation      Price");

		double newCorrelation;
		for (int i = 0; i <= 20; i++) {
			newCorrelation = (i - 10) * 0.1;// the correlation goes from -1 to 1
			double[][] newCorrelationMatrix = { { 1.0, newCorrelation }, { newCorrelation, 1.0 } };
			/*
			 * finmath class, we use it to get the simulation of a two dimensional
			 * Black-Scholes model: two geometric Brownian motions, with possibly dependent
			 * stochastic drivers (correlation specified by the correlation matrix). Note
			 * that the class implements AssetModelMonteCarloSimulationModel.
			 */
			final AssetModelMonteCarloSimulationModel simulationTwoDimGeometricBrownian = new MonteCarloMultiAssetBlackScholesModel(
					brownian, initialPrices, riskFreeRate, volatilities, newCorrelationMatrix);

			final double monteCarloPrice = exchangeOption.getValue(simulationTwoDimGeometricBrownian);

			System.out.print(formatterCorrelation.format(newCorrelation) + "         ");

			System.out.println(formatterValue.format(monteCarloPrice));
		}
	} 

	@Test
	void testExchangeForGivenSeed() {
		try {
			/*
			 * We simulate a two-dimensional Brownian motion (B=(B^1,B^2) such that
			 * W_1 = B_1
			 * W_2 = rho B_1 + sqrt(1-rho^2)B^2
			 */
			final BrownianMotion brownian = new BrownianMotionFromMersenneRandomNumbers(times, 2, numberOfSimulations, seed);
			/*
			 * finmath class, we use it to get the simulation of a two dimensional
			 * Black-Scholes model: two geometric Brownian motions, with possibly dependent
			 * stochastic drivers (correlation specified by the correlation matrix). Note
			 * that the class implements AssetModelMonteCarloSimulationModel.
			 */
			final AssetModelMonteCarloSimulationModel simulationTwoDimGeometricBrownian = new MonteCarloMultiAssetBlackScholesModel(
					brownian, initialPrices, riskFreeRate, volatilities, correlationMatrix);
			/*
			 * We now create an object of ExchangeOption with maturity given above, and call
			 * its getValue(final MonteCarloSimulationModel model) method, passing it
			 * simulationTwoDimGeometricBrownian. This method is an overloading of
			 * getValue(double evaluationTime, AssetModelMonteCarloSimulationModel model)
			 * inherited from AbstractMonteCarloProduct. It returns a double which is the
			 * price of the option.
			 */

			final double monteCarloPrice = exchangeOption.getValue(simulationTwoDimGeometricBrownian);
			final double analyticalPrice = computeAnalyticalValue();
			final double error = Math.abs(monteCarloPrice - analyticalPrice) / analyticalPrice;

			System.out.println("Simulated price of the exchange option using multi asset MC: "
					+ formatterValue.format(monteCarloPrice));
			System.out.println("Analytical price: " + formatterValue.format(analyticalPrice));
			System.out.println("Percentage error: " + formatterPercentage.format(error));
			System.out.println();
			Assert.assertTrue(100 * error < percentageTolerance);
		} catch (CalculationException e) {
			fail("Exception!");
		}
	}

	/**
	 * Repeats the computation of the Monte Carlo price of an exchange option for
	 * different random seeds, and checks if this price is close to the analytical
	 * one up to a given tolerance, a given percentage of times.
	 *
	 * @throws CalculationException
	 */
	@Test
	public void testExchangeWithRandomSeeds() throws CalculationException {

		try {

			final int numberOfRepetitions = 300;// number of Monte Carlo computations

			// number of times we don't approximate the result as we would like
			int numberOfFailures = 0;

			// we want to get a random integer: we use the Random class of Java
			final Random randomGenerator = new Random();

			/*
			 * We do the first computation for the first seed. In this way, we construct
			 * here the object of type MonteCarloMultiAssetBlackScholesModel, and then for
			 * every other computation we can just take the clone with modified seed.
			 */
			int randomSeed = randomGenerator.nextInt();

			final BrownianMotion brownian = new BrownianMotionFromMersenneRandomNumbers(times, 2, numberOfSimulations,
					randomSeed/* the seed changes at every iterations */);

			// the seed is "hidden" in the BrownianMotion object
			final AssetModelMonteCarloSimulationModel firstSimulationTwoDimGeometricBrownian = new MonteCarloMultiAssetBlackScholesModel(
					brownian, initialPrices, riskFreeRate, volatilities, correlationMatrix);

			double monteCarloPrice = exchangeOption.getValue(firstSimulationTwoDimGeometricBrownian);
			double analyticalPrice = computeAnalyticalValue();
			double error = Math.abs(monteCarloPrice - analyticalPrice) / analyticalPrice;

			if (100 * error > percentageTolerance) {
				numberOfFailures++; // counter updated if the relative error exceeds the tolerance
			}

			for (int i = 0; i < numberOfRepetitions - 1; i++) {

				randomSeed = randomGenerator.nextInt();

				// We could also do the following
				//final BrownianMotion newBrownian = new BrownianMotionFromMersenneRandomNumbers(times, 2, numberOfSimulations,
				//		randomSeed/* the seed changes at every iterations */);
				//
				// the seed is "hidden" in the BrownianMotion object
				//firstSimulationTwoDimGeometricBrownian = new MonteCarloMultiAssetBlackScholesModel(
				//		newBrownian, initialPrices, riskFreeRate, volatilities, correlationMatrix);
				/*
				 * get the price of your exchange option as above, now constructing your
				 * MonteCarloMultiAssetBlackScholesModel with randomSeed.
				 */

				final AssetModelMonteCarloSimulationModel simulationTwoDimGeometricBrownian = firstSimulationTwoDimGeometricBrownian
						.getCloneWithModifiedSeed(randomSeed);

				monteCarloPrice = exchangeOption.getValue(simulationTwoDimGeometricBrownian);
				analyticalPrice = computeAnalyticalValue();
				error = Math.abs(monteCarloPrice - analyticalPrice) / analyticalPrice;

				if (100 * error > percentageTolerance) {
					numberOfFailures++; // counter updated if the relative error exceeds the tolerance
				}

			}
			final double ratioFailure = ((double) numberOfFailures) / numberOfRepetitions;
			final double ratioSuccess = 1 - ratioFailure;
			System.out.println("The percentage of times when the percentage error is smaller than "
					+ percentageTolerance + " is " + formatterPercentage.format(ratioSuccess));
			System.out.println();
			Assert.assertTrue(100 * ratioFailure < allowedFailurePercentage);
		} catch (CalculationException e) {
			fail("Exception!");
		}
	}
	
}
