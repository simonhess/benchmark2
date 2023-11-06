/**
 * 
 */
package benchmark2.strategies;

import benchmark2.StaticValues;
import benchmark2.agents.Bank;
import jmab2.agents.MacroAgent;
import jmab2.population.MacroPopulation;
import jmab2.strategies.InterestRateStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.distribution.AbstractDelegatedDistribution;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Joeri Schasfoort
 * This strategy changes in an adaptive way the price asked by banks for interbank loans
 * If the value of the ratio of liquidity over target liquidity is higher than a threshold,
 * then it lowers the price to make its loans more attractive, and vice versa. 
 * There is a lower bound at the central bank interest rate for reserves and and upper bound at the central bank advances rate
 * Using this strategy banks set their interbank ask as a risk adjusted mark-up for stashing their 
 * liquidity at the central bank.
 */
@SuppressWarnings("serial")
public class InterbankAskCBMarkUp extends AbstractStrategy implements InterestRateStrategy {

	private double adaptiveParameter;
	private int reserveId;
	private int advancesId;
	private AbstractDelegatedDistribution distribution; 
	
	/* 
	 * Main method used to compute the interest rate as a mark-up of the CB reserve deposit rate
	 */
	@Override
	public double computeInterestRate(MacroAgent creditDemander, double amount, int length) {
		double avInterest=0;
		SimulationController controller = (SimulationController)this.getScheduler();
		MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
		Population banks = macroPop.getPopulation(StaticValues.BANKS_ID);
		double inter=0;
		for (Agent b:banks.getAgents()){
			Bank bank = (Bank) b;
			inter+=bank.getPassedValue(StaticValues.LAG_INTERBANKINTEREST, 1);
			}
		avInterest=inter/banks.getSize();
		Bank bank = (Bank)this.agent;
		double centralBankDepositRate = bank.getReserveInterestRate();
		double centralBankAdvancesRate = bank.getAdvancesInterestRate();
		double interBankRiskPremium = bank.getInterBankRiskPremium();
		double interbankAskPrice = 0;
		double excessLiquidity = bank.getExcessLiquidity();
		if(excessLiquidity <= 0){
			interbankAskPrice=avInterest+(adaptiveParameter*distribution.nextDouble());
			return Math.min(interbankAskPrice, centralBankAdvancesRate+ interBankRiskPremium);
		}else{
			interbankAskPrice=avInterest-(adaptiveParameter*distribution.nextDouble());
			return Math.max(interbankAskPrice, centralBankDepositRate+ interBankRiskPremium);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see jmab2.strategies.SingleStrategy#getBytes()
	 */
	@Override
	public byte[] getBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see jmab2.strategies.SingleStrategy#populateFromBytes(byte[], jmab2.population.MacroPopulation)
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


	public double getAdaptiveParameter() {
		return adaptiveParameter;
	}


	public void setAdaptiveParameter(double adaptiveParameter) {
		this.adaptiveParameter = adaptiveParameter;
	}


	public int getAdvancesId() {
		return advancesId;
	}


	public void setAdvancesId(int advancesId) {
		this.advancesId = advancesId;
	}


	public AbstractDelegatedDistribution getDistribution() {
		return distribution;
	}


	public void setDistribution(AbstractDelegatedDistribution distribution) {
		this.distribution = distribution;
	}




}
