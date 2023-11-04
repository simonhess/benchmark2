/*
 * JMAB - Java Macroeconomic Agent Based Modeling Toolkit
 * Copyright (C) 2013 Alessandro Caiani and Antoine Godin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package benchmark.strategies;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.List;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import benchmark.agents.Government;
import jmab2.agents.MacroAgent;
import jmab2.population.MacroPopulation;
import jmab2.simulations.MacroSimulation;
import jmab2.stockmatrix.InterestBearingItem;
import jmab2.stockmatrix.Item;
import jmab2.stockmatrix.Loan;
import jmab2.strategies.InterestRateStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.distribution.AbstractDelegatedDistribution;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class AdaptiveMarkupOnAdvancesRateDF extends AbstractStrategy implements
InterestRateStrategy {

	private double adaptiveParameter;
	private AbstractDelegatedDistribution distribution; 
	private int[] liabilitiesId;
	private double markup;
	private double pastMarkup;

	private int mktId;

	/* (non-Javadoc)
	 * @see jmab2.strategies.InterestRateStrategy#computeInterestRate(jmab2.agents.MacroAgent, double, int)
	 * 
	 * This strategy computes the banks interest rate on loans based on the derivative follower (DF) strategy: Banks behave in the current period
	 * like in the previous period if the behavior in the last period led to higher net interest income from new loans. For example, if an
	 * increase of the mark up led to more net interest income in the last period it keeps on increasing the mark up in the current period,
	 * vice versa.
	 * 
	 * 
	 */
	@Override
	public double computeInterestRate(MacroAgent creditDemander, double amount,
			int length) {
		
		Bank lender=(Bank) this.getAgent();
		
		int round = ((MacroSimulation)((SimulationController)this.scheduler).getSimulation()).getRound();
		
		if(round==1) {
			pastMarkup=markup*0.995;
		}
		
		double newLoansLastPeriod=0;
		double newLoansPenultimatePeriod=0; 
		
		for (Item i:lender.getItemsStockMatrix(true, StaticValues.SM_LOAN)){
			Loan loan = (Loan) i;
			if(loan.getAge()==1) {
				newLoansLastPeriod+=loan.getInitialAmount();
			}else if(loan.getAge()==2) {
				newLoansPenultimatePeriod+=loan.getInitialAmount();
			}
		}
		
//		double inflation = gov.getAggregateValue(StaticValues.LAG_ALLPRICE, 1);
//		double inflation2 = gov.getAggregateValue(StaticValues.LAG_ALLPRICE, 2);
//		double netInterestIncomeLastPeriodNewLoans = newLoansLastPeriod*markup/inflation;
//		double netInterestIncomePenultimatePeriodNewLoans = newLoansPenultimatePeriod*pastMarkup/inflation2;
		
		double netInterestIncomeLastPeriodNewLoans = newLoansLastPeriod*markup;
		double netInterestIncomePenultimatePeriodNewLoans = newLoansPenultimatePeriod*pastMarkup;
		
		double tempMarkup = markup;
		
		markup+=adaptiveParameter*distribution.nextDouble()*Math.signum(netInterestIncomeLastPeriodNewLoans-netInterestIncomePenultimatePeriodNewLoans)*Math.signum(markup-pastMarkup);
		pastMarkup = tempMarkup;
		
		if (markup < 0) markup = 0;
		
		double advancesRate = lender.getAdvancesInterestRate();
		
		double iR=advancesRate+markup;
		
		double finalRate = Math.min(Math.max(iR, lender.getInterestRateLowerBound(mktId)),lender.getInterestRateUpperBound(mktId));
		return finalRate;
	}

	/**
	 * @return the adaptiveParameter
	 */
	public double getAdaptiveParameter() {
		return adaptiveParameter;
	}

	/**
	 * @param adaptiveParameter the adaptiveParameter to set
	 */
	public void setAdaptiveParameter(double adaptiveParameter) {
		this.adaptiveParameter = adaptiveParameter;
	}

	/**
	 * @return the distribution
	 */
	public AbstractDelegatedDistribution getDistribution() {
		return distribution;
	}

	/**
	 * @param distribution the distribution to set
	 */
	public void setDistribution(AbstractDelegatedDistribution distribution) {
		this.distribution = distribution;
	}

	/**
	 * @return the mkId
	 */
	public int getMktId() {
		return mktId;
	}


	/**
	 * @param mkId the mkId to set
	 */
	public void setMktId(int mktId) {
		this.mktId = mktId;
	}

	public int[] getLiabilitiesId() {
		return liabilitiesId;
	}

	public void setLiabilitiesId(int[] liabilitiesId) {
		this.liabilitiesId = liabilitiesId;
	}

	public double getMarkup() {
		return markup;
	}

	public void setMarkup(double markup) {
		this.markup = markup;
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
	
	/**
	 * @return the pastMarkup
	 */
	public double getPastMarkup() {
		return pastMarkup;
	}

	/**
	 * @param pastMarkup the pastMarkup to set
	 */
	public void setPastMarkup(double pastMarkup) {
		this.pastMarkup = pastMarkup;
	}

}
