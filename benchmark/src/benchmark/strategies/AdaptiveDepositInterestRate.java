/**
 * 
 */
package benchmark.strategies;

import java.nio.ByteBuffer;
import java.util.List;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import jmab.stockmatrix.InterestBearingItem;
import jmab.stockmatrix.Item;
import jmab.strategies.InterestRateStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
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
		for (Agent b:banks.getAgents()){
			Bank bank = (Bank) b;
			inter+=bank.getPassedValue(StaticValues.LAG_DEPOSITINTEREST, 1);
			}
		avInterest=inter/banks.getSize();
		Bank lender=(Bank) this.getAgent();
		// determine the liquidity position by comparing the liquidity ratio with the target liquidity ratio
		double liquidityRatio=lender.getLiquidityRatio();
		double targetLiquidityRatio=lender.getTargetedLiquidityRatio();
		int liquidityPosition = 0;
		if ((liquidityRatio-targetLiquidityRatio)/targetLiquidityRatio < 0) {liquidityPosition = 1;
		}else {liquidityPosition = -1;}
		// determine the funding position
		double previousFundingRate = lender.getFundingRate();
		double interestPay=0;
		double totValue=0;
		for(int liabilityId:liabilitiesId){
			List<Item> liabilities = lender.getItemsStockMatrix(false, liabilityId);
			for(Item item:liabilities){
				InterestBearingItem liability = (InterestBearingItem) item;
				interestPay += liability.getInterestRate()*liability.getValue();
				totValue +=liability.getValue();
			}
		}
		double fundingRate = interestPay/totValue;
		int fundingPosition = 0;
		if ((fundingRate - previousFundingRate) / previousFundingRate > 0) {fundingPosition = 1;
		}else {fundingPosition = -1;}
		lender.setFundingRate(fundingRate);
		// profit mark-up //
		double previousInterestRate = lender.getPassedValue(StaticValues.LAG_LOANINTEREST, 1);
		double currentInterestRate = lender.getBankInterestRate();
		int profitabilityPosition = 0;
		if ((currentInterestRate-previousInterestRate)/previousInterestRate > 0) {profitabilityPosition = 1;
		}else {profitabilityPosition = -1;}
		// the deposit rate = average deposit rate + random if (liquidity mark-up + funding-mark-up + profit-mark-up > threshold)
		double referenceVariable = liquidityPosition + fundingPosition + profitabilityPosition;
		double iR=0;
		if(referenceVariable<=0){
			iR=avInterest+(adaptiveParameter*avInterest*distribution.nextDouble());
		}else{
			iR=avInterest-(adaptiveParameter*avInterest*distribution.nextDouble());
		}
		return Math.min(Math.max(iR, lender.getInterestRateLowerBound(mktId)),lender.getInterestRateUpperBound(mktId));
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

}
