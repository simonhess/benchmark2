/**
 * 
 */
package benchmark.strategies;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import jmab.population.MacroPopulation;
import jmab.stockmatrix.Item;
import jmab.strategies.SupplyCreditStrategy;
import net.sourceforge.jabm.EventScheduler;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Joeri Schasfoort and Simon Hess
 * This strategy computes the interbank credit supply or demand based on the liquidity ratio
 */
@SuppressWarnings("serial")
public class InterbankSupplyDemandLR extends AbstractStrategy implements
		SupplyCreditStrategy {

	/* (non-Javadoc)
	 * main method used to compute the interbank credit or demand
	 * 
	 */
	@Override
	public double computeCreditSupply() {
		Bank supplier=(Bank) this.getAgent();
		// get the excess liquidity that can be lend out
		double excessLiquidity = supplier.getExcessLiquidity();
		
		return excessLiquidity;
	}
	
	/**
	 * 
	 */
	public InterbankSupplyDemandLR() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param agent
	 */
	public InterbankSupplyDemandLR(Agent agent) {
		super(agent);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param scheduler
	 * @param agent
	 */
	public InterbankSupplyDemandLR(EventScheduler scheduler, Agent agent) {
		super(scheduler, agent);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see jmab.strategies.SingleStrategy#getBytes()
	 */
	@Override
	public byte[] getBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see jmab.strategies.SingleStrategy#populateFromBytes(byte[], jmab.population.MacroPopulation)
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		// TODO Auto-generated method stub

	}

}


