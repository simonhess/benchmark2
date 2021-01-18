package benchmark.strategies;

import jmab.strategies.SingleStrategy;

/**
 * @author Joeri Schasfoort, Alessandro Caiani and Antoine Godin
 * This interface forms the basis for all monetary strategies
 */
public interface MonetaryPolicyStrategy extends SingleStrategy {
	
	public double computeAdvancesRate();
	
}
