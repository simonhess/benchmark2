package benchmark2.strategies;

import benchmark2.agents.CentralBank;
import jmab2.population.MacroPopulation;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Simon Hess With this strategy the central bank sets its reserve deposit rate equal to the rate on advances (Floor System).
 */
public class ReservesEqualsAdvancesStrategy extends AbstractStrategy implements
		ReservesRateStrategy {
	
	private double staticReservesRate;


	@Override
	public double computeReservesRate() {
		CentralBank agent= (CentralBank) this.getAgent();
		return agent.getAdvancesInterestRate();
	}
	
	public double getStaticReservesRate() {
		return staticReservesRate;
	}

	public void setStaticReservesRate(double staticReservesRate) {
		this.staticReservesRate = staticReservesRate;
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

	

}
