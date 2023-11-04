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
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import benchmark.agents.CapitalFirm;
import benchmark.agents.CentralBank;
import benchmark.agents.ConsumptionFirm;
import benchmark.expectations.AdaptiveExpectationExpectedVsActualValue;
import jmab2.agents.AbstractFirm;
import jmab2.agents.GoodDemander;
import jmab2.agents.MacroAgent;
import jmab2.agents.PriceSetterWithTargets;
import jmab2.expectations.Expectation;
import jmab2.expectations.PassedValues;
import jmab2.population.MacroPopulation;
import jmab2.simulations.AbstractMacroSimulation;
import jmab2.strategies.MarkupPricingStrategy;
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
public class AdaptiveMarkUpOnACExpectedVsActualValue extends AbstractStrategy implements
MarkupPricingStrategy {

	private double threshold; //to be set through the configuration file.
	private double adaptiveParameter;
	private AbstractDelegatedDistribution distribution;
	private double markUp;

	/** 
	 * This strategy changes in an adaptive way the price asked by producers on their output. The price change is determined by the firm's 
	 * expected and actual sales with the average price of all firms as base. If the expectations are lower than the actual ones, 
	 * which implies an increased demand, the firm will choose a price above the average price. If the expectations are greater than the actual
	 *  ones, which implies an decreased demand, the firm will choose a price below the average price.
	 */
	@Override
	public double computePrice() {
		
		PriceSetterWithTargets seller=(PriceSetterWithTargets) this.getAgent();
		double price = seller.getPrice();
		double previousLowerBound=price/(1+markUp);
		
		// Set mark up  
		
		AdaptiveExpectationExpectedVsActualValue salesExp = (AdaptiveExpectationExpectedVsActualValue) seller.getExpectation(StaticValues.EXPECTATIONS_REALSALES);
		
		double[][] passedValues = salesExp.getPassedValues();
		
		AbstractMacroSimulation macroSim = (AbstractMacroSimulation)((SimulationController)this.scheduler).getSimulation();
		
		double avPrice = 0;
		
		MacroPopulation macroPop = (MacroPopulation) macroSim.getPopulation();
		Population pop = null;
		if(agent instanceof ConsumptionFirm) {
			pop = macroPop.getPopulation(StaticValues.CONSUMPTIONFIRMS_ID);
			//avPrice=gov.getAggregateValue(StaticValues.LAG_AVCPRICE, 1);
		}else if(agent instanceof CapitalFirm) {
			pop = macroPop.getPopulation(StaticValues.CAPITALFIRMS_ID);
			//avPrice=gov.getAggregateValue(StaticValues.LAG_AVKPRICE, 1);
		}

		for (Agent a:pop.getAgents()){
			AbstractFirm firm= (AbstractFirm) a;
			avPrice+=firm.getPassedValue(StaticValues.LAG_PRICE, 1);
		}
		avPrice=avPrice/pop.getSize();	
		
		double avMarkup=(avPrice/previousLowerBound)-1;
		
		double expectedValue = passedValues [0][1];
		
		double actualValue = passedValues[0][0];
		
		if(expectedValue<actualValue) {
			// expected value < actual value = increased demand
		markUp=avMarkup+(adaptiveParameter*distribution.nextDouble());	
		}else if(expectedValue>actualValue) {
			// expected value > actual value = decreased demand
		markUp=avMarkup-(adaptiveParameter*distribution.nextDouble());
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
	 * @see jmab2.strategies.PricingStrategy#computePriceForSpecificBuyer(jmab2.agents.GoodDemander, double, boolean)
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
