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
package benchmark2.strategies;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.List;

import benchmark2.StaticValues;
import benchmark2.agents.Bank;
import benchmark2.agents.Government;
import benchmark2.expectations.AdaptiveExpectationExpectedVsActualValue;
import jmab2.agents.MacroAgent;
import jmab2.population.MacroPopulation;
import jmab2.simulations.MacroSimulation;
import jmab2.stockmatrix.InterestBearingItem;
import jmab2.stockmatrix.Item;
import jmab2.stockmatrix.Loan;
import jmab2.strategies.InterestRateStrategy;
import jmab2.strategies.MarkupInterestRateStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.distribution.AbstractDelegatedDistribution;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Simon Hess
 *
 */
@SuppressWarnings("serial")
public class AdaptiveMarkupOnAdvancesRateExpectedVsActualValue extends AbstractStrategy implements
MarkupInterestRateStrategy {

	private double adaptiveParameter;
	private AbstractDelegatedDistribution distribution; 
	private int[] liabilitiesId;
	private double markup;
	private int mktId;

	/* (non-Javadoc)
	 * @see jmab2.strategies.InterestRateStrategy#computeInterestRate(jmab2.agents.MacroAgent, double, int)
	 * 
	 * This strategy computes the banks interest rate on loans based on the new loans issued in the last period
	 * and the expected loans issued for that period. If the new loans issued in the last period
	 * are greater than the expected ones the bank computes its new markup by adding a random amount to the average markup of all banks.
	 * Otherwise it subtracts an random amount of it. The interest rate on loans is computed by adding the new markup to the advances rate.
	 * 
	 * 
	 */
	@Override
	public double computeInterestRate(MacroAgent creditDemander, double amount, int length) {

		Bank lender = (Bank) this.getAgent();

		double avInterest = 0;
		SimulationController controller = (SimulationController) this.getScheduler();
		MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
		Population banks = macroPop.getPopulation(StaticValues.BANKS_ID);
		double inter = 0;
		double n = (double) banks.getSize();
		for (Agent b : banks.getAgents()) {
			Bank bank = (Bank) b;
			if (bank.getNumericBalanceSheet()[0][StaticValues.SM_LOAN] != 0 && bank.getNetWealth() > 0) {
				inter += bank.getPassedValue(StaticValues.LAG_LOANINTEREST, 1);
			} else {
				n -= 1;
			}
		}
		avInterest = inter / n;
		double pastAdvancesRate = lender.getPassedValue(StaticValues.LAG_LOANINTEREST, 1) - markup;
		double avMarkup = avInterest - pastAdvancesRate;
		
		AdaptiveExpectationExpectedVsActualValue loansExp = (AdaptiveExpectationExpectedVsActualValue) lender.getExpectation(StaticValues.EXPECTATIONS_REALNEWLOANS);
		double[][] passedValues = loansExp.getPassedValues();
		double expectedValue = passedValues [0][1];
		double actualValue = passedValues[0][0];

		if(expectedValue<actualValue) {
			// expected value < actual value = increased demand
			markup = avMarkup + adaptiveParameter * distribution.nextDouble();
		}else if(expectedValue>actualValue) {
			// expected value > actual value = decreased demand
			markup = avMarkup - adaptiveParameter * distribution.nextDouble();
		}

		if (markup < 0)
			markup = 0;

		double advancesRate = lender.getAdvancesInterestRate();

		double iR = advancesRate + markup;

		double finalRate = Math.min(Math.max(iR, lender.getInterestRateLowerBound(mktId)),
				lender.getInterestRateUpperBound(mktId));
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

}
