package benchmark2.strategies;

import jmab2.population.MacroPopulation;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Simon Hess
 * With this strategy the central bank sets its advances rate at the passed value
 */
public class MonetaryPolicyStatic extends AbstractStrategy implements
MonetaryPolicyStrategy {
	
	private double staticAdvancesRate;

	public double computeAdvancesRate() {
		return staticAdvancesRate;
	}

	@Override
	public byte[] getBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		// TODO Auto-generated method stub
		
	}
	
	public double getStaticAdvancesRate() {
		return staticAdvancesRate;
	}

	public void setStaticAdvancesRate(double staticAdvancesRate) {
		this.staticAdvancesRate = staticAdvancesRate;
	}
	
}
