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
import java.util.List;

import benchmark.StaticValues;
import benchmark.agents.CapitalFirm;
import benchmark.agents.ConsumptionFirm;
import benchmark.agents.Government;
import benchmark.expectations.AdaptiveExpectationDoubleExponentialSmoothing;
import jmab.agents.InvestmentAgent;
import jmab.agents.MacroAgent;
import jmab.expectations.Expectation;
import jmab.population.MacroPopulation;
import jmab.simulations.MacroSimulation;
import jmab.stockmatrix.CapitalGood;
import jmab.stockmatrix.Item;
import jmab.strategies.RealCapitalDemandStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.distribution.AbstractDelegatedDistribution;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class RealLumpyCapitalDemandAdaptiveNPV extends AbstractStrategy implements
		RealCapitalDemandStrategy {
	
	private double adaptiveParameter;
	private AbstractDelegatedDistribution distribution; 
	int productionStockId;
	int capitalGoodId;
	double investment;

	/* (non-Javadoc)
	 * @see jmab.strategies.RealCapitalDemandStrategy#computeRealCapitalDemand(jmab.agents.MacroAgent)
	 */
	@Override
	public double computeRealCapitalDemand(MacroAgent seller) {
		
		int round = ((MacroSimulation)((SimulationController)this.scheduler).getSimulation()).getRound();
		
		if(round==1) {
			List<Item> capStock=((InvestmentAgent)this.getAgent()).getItemsStockMatrix(true, capitalGoodId);
			int investment=0;
			for (Item j:capStock){
				CapitalGood good= (CapitalGood)j;
				investment+=good.getQuantity();
			}
			this.investment =investment;
		
		}
		
		
		InvestmentAgent investor= (InvestmentAgent) this.getAgent();
		CapitalGood newCapital= (CapitalGood)seller.getItemStockMatrix(true, productionStockId);
		List<Item> capitalStock=investor.getItemsStockMatrix(true, capitalGoodId);
		double currentCapacity=0;
		double residualCapacity=0;
		for (Item i:capitalStock){
			CapitalGood oldCapital=(CapitalGood) i;
			currentCapacity+=oldCapital.getProductivity()*oldCapital.getQuantity();
			if (oldCapital.getCapitalDuration()- oldCapital.getAge()>1){ //i.e. the capital is still working in the next period
					residualCapacity+=oldCapital.getProductivity()*oldCapital.getQuantity();
				}
			}
		double desiredCapacity= (1+investor.getDesiredCapacityGrowth())*currentCapacity;
		
		ConsumptionFirm c = (ConsumptionFirm) this.getAgent();
		
		double debtValue = 0;
		for (Item i:c.getItemsStockMatrix(false, StaticValues.SM_LOAN)){
			debtValue+=i.getValue();
		}
		
		double equityValue = c.getNetWealth();
		
		double costOfDebt = c.getDebtInterests()/debtValue;
		
		Population govpop = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.GOVERNMENT_ID);
		Government gov = (Government) govpop.getAgentList().get(0);
		double costOfEquity = gov.getAggregateValue(StaticValues.LAG_AVCFIRMCOSTOFEQUITY, 1);
		
		double wacc = equityValue/(equityValue+debtValue)*costOfEquity+debtValue/(equityValue+debtValue)*costOfDebt;

		double desInv = desiredCapacity-currentCapacity;
		
		AdaptiveExpectationDoubleExponentialSmoothing expUFCFCapRatio = (AdaptiveExpectationDoubleExponentialSmoothing) investor.getExpectation(StaticValues.EXPECTATIONS_UNLEVEREDFREECASHFLOWPERCAPACITY);
		
		CapitalFirm k = (CapitalFirm)seller;
		
		double NPV = -desInv*k.getPrice();
		
		for(int i = 1; i<=newCapital.getCapitalDuration();i++) {
			double expUFCF=desInv*expUFCFCapRatio.getExpectation(i);
			NPV += expUFCF/Math.pow((1+wacc), i);
		}
		
		
		if (desiredCapacity>currentCapacity&&NPV>0){
			this.investment+=adaptiveParameter*distribution.nextDouble();
		}else{
			this.investment-=adaptiveParameter*distribution.nextDouble();
		}
		
		Expectation expRS = investor.getExpectation(StaticValues.EXPECTATIONS_REALSALES);
		
		if(expRS.getExpectation()<residualCapacity) {
			return 0;
		}
		
		return (int) Math.round(this.investment);
		
	}
	
	/**
	 * @return the productionStockId
	 */
	public int getProductionStockId() {
		return productionStockId;
	}



	/**
	 * @param productionStockId the productionStockId to set
	 */
	public void setProductionStockId(int productionStockId) {
		this.productionStockId = productionStockId;
	}



	/**
	 * @return the capitalGoodId
	 */
	public int getCapitalGoodId() {
		return capitalGoodId;
	}



	/**
	 * @param capitalGoodId the capitalGoodId to set
	 */
	public void setCapitalGoodId(int capitalGoodId) {
		this.capitalGoodId = capitalGoodId;
	}



	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [productionStockId][capitalGoodId]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(8);
		buf.putInt(this.productionStockId);
		buf.putInt(this.capitalGoodId);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [productionStockId][capitalGoodId]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.productionStockId = buf.getInt();
		this.capitalGoodId = buf.getInt();
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
	
	

}