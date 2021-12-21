/**
 * 
 */
package benchmark.strategies;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import jmab.agents.MacroAgent;
import jmab.distribution.LognormalDistribution;
import jmab.population.MacroPopulation;
import jmab.simulations.MacroSimulation;
import jmab.stockmatrix.InterestBearingItem;
import jmab.stockmatrix.Item;
import jmab.stockmatrix.Loan;
import jmab.strategies.InterestRateStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.agent.AgentList;
import net.sourceforge.jabm.distribution.AbstractDelegatedDistribution;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Joeri Schasfoort
 * This class lets bank set their deposit interest rate 
 * Factors influencing this rate are: the previous rate, the rate of the competitors, 
 * the rates of alternative funding, and finally the 
 * rate of profitable investments for which it can use deposits.
 */
public class AdaptiveDepositInterestRate extends AbstractStrategy implements InterestRateStrategy {

	private double adaptiveParameter;
	private AbstractDelegatedDistribution distribution;
	private LognormalDistribution depositRateChangeDistribution;
	private int[] liabilitiesId;
	private int mktId;
	/* 
	 * Main method used to compute the deposit interest rate
	 */
	@Override
	public double computeInterestRate(MacroAgent creditDemander, double amount, int length) {
		double avInterest=0;
		SimulationController controller = (SimulationController)this.getScheduler();
		MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
		Population banks = macroPop.getPopulation(StaticValues.BANKS_ID);
		double inter=0;
		AgentList bankAgents = banks.getAgentList();
		bankAgents.shuffle(distribution.getPrng());
		Bank lender=(Bank) this.getAgent();

		for (Agent b:banks.getAgents()){
			Bank bank = (Bank) b;
			inter+=bank.getPassedValue(StaticValues.LAG_DEPOSITINTEREST, 1);
			}
		avInterest=inter/banks.getSize();

		// determine the liquidity deficit position by checking for central bank advances
		
		double excessLiquidity = lender.getExcessLiquidity();
		int liquidityDeficitPosition = 0;
		if (excessLiquidity <= 0) {liquidityDeficitPosition = 1;
		}else {liquidityDeficitPosition = -1;}
		// determine the opportunity cost position
		double previousDepositRate = lender.getDepositInterestRate();
		double depositUpperBound = lender.getInterestRateUpperBound(mktId);
		int opportunityCostPosition = 0;
		if (previousDepositRate <= depositUpperBound) {opportunityCostPosition = 1;
		}else {opportunityCostPosition = -1;}
		// determine the profit on reserves position //
		double reserveInterestRate = lender.getReserveInterestRate();
		int profitOnReservesPosition = 0;
		if (previousDepositRate <= reserveInterestRate) {profitOnReservesPosition = 1;
		}else {profitOnReservesPosition = -1;}
		// the deposit rate = average deposit rate + random if (liquidity position + opportunity cost position + profit on reserves position > 0)
		double referenceVariable = liquidityDeficitPosition + opportunityCostPosition + profitOnReservesPosition;
		
		double random = depositRateChangeDistribution.nextDouble();
		double probX = depositRateChangeDistribution.cdfLognormal(random);
		
			double iR=0;
			
			if(referenceVariable>0){
				
				if(depositUpperBound>previousDepositRate) {
					if(probX > 0.75) {
						iR=avInterest+(adaptiveParameter*distribution.nextDouble());
						return Math.min(iR, lender.getInterestRateUpperBound(mktId));
					}else {
						return previousDepositRate;
					}
				}else {
				iR=avInterest+(adaptiveParameter*distribution.nextDouble());
				return Math.min(iR, lender.getInterestRateUpperBound(mktId));}
			}else{
				
				if(depositUpperBound>previousDepositRate) {
					if(probX > 0.75) {
						iR=avInterest-(adaptiveParameter*distribution.nextDouble());
						return Math.max(iR, lender.getInterestRateLowerBound(mktId));
					}else {
						return previousDepositRate;
					}
				}else {
					iR=avInterest-(adaptiveParameter*distribution.nextDouble());
					return Math.max(iR, lender.getInterestRateLowerBound(mktId));}
			}
		
//		old strategy
//		double iR=0;
//		if(referenceVariable>0){
//			iR=avInterest+(adaptiveParameter*distribution.nextDouble());
//			return Math.min(iR, lender.getInterestRateUpperBound(mktId));
//		}else{
//			iR=avInterest-(adaptiveParameter*distribution.nextDouble());
//			return Math.max(iR, lender.getInterestRateLowerBound(mktId));
//		}
//		
	}

	/** 
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [threshold][adaptiveParameter][avInterest][mktId][increase]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(21);
		buf.putDouble(adaptiveParameter);
		buf.putInt(mktId);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [threshold][adaptiveParameter][avInterest][mktId][increase]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.adaptiveParameter = buf.getDouble();
		this.mktId = buf.getInt();
	}

	public double getAdaptiveParameter() {
		return adaptiveParameter;
	}

	public void setAdaptiveParameter(double adaptiveParameter) {
		this.adaptiveParameter = adaptiveParameter;
	}

	public AbstractDelegatedDistribution getDistribution() {
		return distribution;
	}

	public void setDistribution(AbstractDelegatedDistribution distribution) {
		this.distribution = distribution;
	}

	public int[] getLiabilitiesId() {
		return liabilitiesId;
	}

	public void setLiabilitiesId(int[] liabilitiesId) {
		this.liabilitiesId = liabilitiesId;
	}

	public int getMktId() {
		return mktId;
	}

	public void setMktId(int mktId) {
		this.mktId = mktId;
	}

	public LognormalDistribution getDepositRateChangeDistribution() {
		return depositRateChangeDistribution;
	}

	public void setDepositRateChangeDistribution(LognormalDistribution depositRateChangeDistribution) {
		this.depositRateChangeDistribution = depositRateChangeDistribution;
	}

}
