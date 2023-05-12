/**
 * 
 */
package benchmark.strategies;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import benchmark.agents.Government;
import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import jmab.stockmatrix.Item;
import jmab.strategies.InterestRateStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.distribution.AbstractDelegatedDistribution;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Simon Hess
 * With this strategy banks set their interbank interest rate to the market rate under the assumption of perfect competition.
 * Thereby a factor which represents the actual reserves in the banking system in relation to the target reserves is subtracted from
 * the rate on advances. 
 */
@SuppressWarnings("serial")
public class InterbankAskPerfectCompetition extends AbstractStrategy implements InterestRateStrategy {

	private int reserveId;
	private int advancesId;
	
	/* 
	 * Main method used to compute the interest rate as a mark-up of the CB reserve deposit rate
	 */
	@Override
	public double computeInterestRate(MacroAgent creditDemander, double amount, int length) {
		SimulationController controller = (SimulationController)this.getScheduler();
		MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
		Population banks = macroPop.getPopulation(StaticValues.BANKS_ID);
		double totalReserves = 0;
		double totalRequiredReserves = 0;
		
		for (Agent b:banks.getAgents()){
			Bank bank = (Bank) b;
			double depositsValue=0;
			for(Item i:bank.getItemsStockMatrix(false, StaticValues.SM_DEP)){
				depositsValue+=i.getValue();
				}
			double reservesValue=0;
			for(Item i:bank.getItemsStockMatrix(true, StaticValues.SM_RESERVES)){
				reservesValue+=i.getValue();
				}
			totalReserves+=reservesValue;
			totalRequiredReserves+=depositsValue*bank.getTargetedLiquidityRatio();
			}
		
		Bank bank = (Bank)this.agent;
		double centralBankDepositRate = bank.getReserveInterestRate();
		double centralBankAdvancesRate = bank.getAdvancesInterestRate();
		
		double targetLiquidityCoverageRatio =0;
		if(totalRequiredReserves==0) {
			targetLiquidityCoverageRatio=1;
		}else {
			targetLiquidityCoverageRatio = Math.max(0,totalReserves/totalRequiredReserves);
		}

		double interbankRate = centralBankAdvancesRate-(centralBankAdvancesRate-centralBankDepositRate)*Math.min(1, targetLiquidityCoverageRatio);
				
		return interbankRate;
		
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


	public int getReserveId() {
		return reserveId;
	}


	public void setReserveId(int reserveId) {
		this.reserveId = reserveId;
	}

	public int getAdvancesId() {
		return advancesId;
	}


	public void setAdvancesId(int advancesId) {
		this.advancesId = advancesId;
	}

}
