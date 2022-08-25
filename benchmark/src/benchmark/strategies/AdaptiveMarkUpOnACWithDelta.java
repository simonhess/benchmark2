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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import benchmark.agents.ConsumptionFirm;
import benchmark.agents.Government;
import benchmark.expectations.AdaptiveExpectationWithDelta;
import jmab.agents.AbstractFirm;
import jmab.agents.GoodDemander;
import jmab.agents.MacroAgent;
import jmab.agents.PriceSetterWithTargets;
import jmab.expectations.Expectation;
import jmab.population.MacroPopulation;
import jmab.simulations.MacroSimulation;
import jmab.stockmatrix.ConsumptionGood;
import jmab.stockmatrix.Item;
import jmab.strategies.MarkupPricingStrategy;
import jmab.strategies.TargetExpectedInventoriesOutputStrategy;
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
public class AdaptiveMarkUpOnACWithDelta extends AbstractStrategy implements
MarkupPricingStrategy {

	private double threshold; //to be set through the configuration file.
	private double adaptiveParameter;
	private AbstractDelegatedDistribution distribution;
	private double markUp;
	private int salesExpId;

	/** 
	 * This strategy changes in an adaptive way the price asked by producers on their output.
	 * If the delta value (last period's expected real sales minus last period's actual sales) is higher than 0, which means that
	 * the firm prediction was too high, the firm decreases the mark up, and vice versa.
	 * The adaptive change depends on the value of the adaptiveParamenter and it is stochastic 
	 * (i.e. a draw from a Uniform distribution).
	 * The price has a lower bound computed by the agent who uses the strategy (say for example expected
	 * average costs).
	 * N.B. in the case referenceVariable=inventories stock we are assuming that inventories are updated by 
	 * the amount of current production after the pricing tick has taken place.
	 */
	@Override
	public double computePrice() {
		
		PriceSetterWithTargets seller=(PriceSetterWithTargets) this.getAgent();
		double referenceVariable= seller.getReferenceVariableForPrice();
		double price = seller.getPrice();
		double previousLowerBound=price/(1+markUp);
		
		// Set mark up  
		
		MacroAgent producer = (MacroAgent) this.getAgent();
		
		double pastSales = producer.getPassedValue(1, 1);
		
		TargetExpectedInventoriesOutputStrategy strat = (TargetExpectedInventoriesOutputStrategy) producer.getStrategy(StaticValues.STRATEGY_PRODUCTION);
		
		AdaptiveExpectationWithDelta salesExp = (AdaptiveExpectationWithDelta) seller.getExpectation(salesExpId);	

		double delta = salesExp.getDelta();
		
		if(delta<0) {
			//markUp+=Math.abs(ra.nextGaussian())*0.01;
			//markUp+=(adaptiveParameter*distribution.nextDouble());
			//markUp=0.075+(adaptiveParameter*distribution.nextDouble());
			markUp+=0.0075;
		}else if(delta>0) {
			//markUp-=Math.abs(ra.nextGaussian())*0.01;
			//markUp-=(adaptiveParameter*distribution.nextDouble());
			//markUp=0.075-(adaptiveParameter*distribution.nextDouble());
			markUp-=0.0075;
		}
		
		if(markUp<0) markUp=0;
		
		if (seller.getPriceLowerBound()!=0){
			price=seller.getPriceLowerBound()*(1+markUp);
		}
		else{
			price=previousLowerBound*(1+markUp);
			return price;
		}
		if(Double.isNaN(price)){
			System.out.println("NaN Markup");
		}
		if(Double.isNaN(seller.getPriceLowerBound())){
			System.out.println("NaN Markup");
		}
		if (price>seller.getPriceLowerBound()){
			return price;
		}
		else {
			return seller.getPriceLowerBound();
		}
	}

	/* (non-Javadoc)
	 * @see jmab.strategies.PricingStrategy#computePriceForSpecificBuyer(jmab.agents.GoodDemander, double, boolean)
	 */
	@Override
	public double computePriceForSpecificBuyer(GoodDemander buyer,
			double demand, boolean real) {
		// TODO Auto-generated method stub
		return 0;
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
	 * @return the threshold
	 */
	public double getThreshold() {
		return threshold;
	}

	/**
	 * @param threshold the threshold to set
	 */
	public void setThreshold(double threshold) {
		this.threshold = threshold;
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
	 * @return the markUp
	 */
	public double getMarkUp() {
		return markUp;
	}

	/**
	 * @param markUp the markUp to set
	 */
	public void setMarkUp(double markUp) {
		this.markUp = markUp;
	}
	
	/**
	 * @return the salesExpId
	 */
	public int getSalesExpId() {
		return salesExpId;
	}

	/**
	 * @param salesExpId the salesExpId to set
	 */
	public void setSalesExpId(int salesExpId) {
		this.salesExpId = salesExpId;
	}


	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [threshold][adaptiveParameter][markUp]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(24);
		buf.putDouble(threshold);
		buf.putDouble(adaptiveParameter);
		buf.putDouble(this.markUp);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [threshold][adaptiveParameter][markUp]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.threshold = buf.getDouble();
		this.adaptiveParameter = buf.getDouble();
		this.markUp = buf.getDouble();
	}

}
