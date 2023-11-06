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

import java.nio.ByteBuffer;

import benchmark2.StaticValues;
import benchmark2.agents.Bank;
import jmab2.agents.InterestRateSetterWithTargets;
import jmab2.agents.MacroAgent;
import jmab2.population.MacroPopulation;
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
public class AdaptiveInterestRateAverageThreshold extends AbstractStrategy implements
		InterestRateStrategy {

	private double adaptiveParameter;
	private AbstractDelegatedDistribution distribution; 
	private boolean increase;
	private int mktId;

	/* (non-Javadoc)
	 * @see jmab2.strategies.InterestRateStrategy#computeInterestRate(jmab2.agents.MacroAgent, double, int)
	 */
	@Override
	public double computeInterestRate(MacroAgent creditDemander, double amount,
			int length) {
		SimulationController controller = (SimulationController)this.getScheduler();
		double threshold = 0;
		MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
		Population banks = macroPop.getPopulation(StaticValues.BANKS_ID);
		if (mktId==StaticValues.MKT_DEPOSIT){
			double tot=0;
			for (Agent b:banks.getAgents()){
				Bank bank = (Bank) b;
				tot+=bank.getLiquidityRatio();
				}
			threshold=tot/banks.getSize();
		}
		else if (mktId==StaticValues.MKT_CREDIT){
			double tot=0;
//			int n=banks.getSize();
			for (Agent b:banks.getAgents()){
				Bank bank = (Bank) b;
				//if (bank.getCapitalRatio()!=Double.POSITIVE_INFINITY){
				tot+=bank.getCapitalRatio();
				//}
				//else{
					//n-=1;
				//}
				}
			threshold=tot/banks.getSize();
		}
		InterestRateSetterWithTargets lender=(InterestRateSetterWithTargets) this.getAgent();
		double referenceVariable= lender.getReferenceVariableForInterestRate(mktId);
		double iR = lender.getInterestRate(mktId);
		if(referenceVariable>threshold){
			if(increase)
				iR+=(adaptiveParameter*iR*distribution.nextDouble());
			else
				iR-=(adaptiveParameter*iR*distribution.nextDouble());
		}else{
			if(increase)
				iR-=(adaptiveParameter*iR*distribution.nextDouble());
			else
				iR+=(adaptiveParameter*iR*distribution.nextDouble());
		}
		return Math.min(Math.max(iR, lender.getInterestRateLowerBound(mktId)),lender.getInterestRateUpperBound(mktId));
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
	 * @return the increase
	 */
	public boolean isIncrease() {
		return increase;
	}

	/**
	 * @param increase the increase to set
	 */
	public void setIncrease(boolean increase) {
		this.increase = increase;
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
	

	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [threshold][adaptiveParameter][mktId][increase]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(13);
		buf.putDouble(adaptiveParameter);
		buf.putInt(mktId);
		if(increase)
			buf.put((byte)1);
		else
			buf.put((byte)0);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [threshold][adaptiveParameter][mktId][increase]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.adaptiveParameter = buf.getDouble();
		this.mktId = buf.getInt();
		this.increase=buf.get()==(byte)1;
	}

}
