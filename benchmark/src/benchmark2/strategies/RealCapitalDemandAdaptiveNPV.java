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
import java.util.List;

import benchmark2.StaticValues;
import benchmark2.agents.Bank;
import benchmark2.agents.CapitalFirm;
import benchmark2.agents.ConsumptionFirm;
import benchmark2.agents.Government;
import benchmark2.expectations.AdaptiveExpectationDoubleExponentialSmoothing;
import jmab2.agents.InvestmentAgent;
import jmab2.agents.MacroAgent;
import jmab2.expectations.Expectation;
import jmab2.population.MacroPopulation;
import jmab2.simulations.MacroSimulation;
import jmab2.stockmatrix.CapitalGood;
import jmab2.stockmatrix.Item;
import jmab2.strategies.RealCapitalDemandStrategy;
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
public class RealCapitalDemandAdaptiveNPV extends AbstractStrategy implements
		RealCapitalDemandStrategy {
	
	private double adaptiveParameter;
	private AbstractDelegatedDistribution distribution; 
	int productionStockId;
	int capitalGoodId;
	double investment;

	/* (non-Javadoc)
	 * @see jmab2.strategies.RealCapitalDemandStrategy#computeRealCapitalDemand(jmab2.agents.MacroAgent)
	 */
	@Override
	public double computeRealCapitalDemand(MacroAgent seller) {
		
		int round = ((MacroSimulation)((SimulationController)this.scheduler).getSimulation()).getRound();
		
		if(round==1) {
			List<Item> capStock=((InvestmentAgent)this.getAgent()).getItemsStockMatrix(true, capitalGoodId);
			int investment=0;
			for (Item j:capStock){
				CapitalGood good= (CapitalGood)j;
				if(good.getAge()==0)
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
		
		double avInterest=0;
		Population banks =((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.BANKS_ID);
		double inter=0;
		double n=(double) banks.getSize();
		for (Agent b:banks.getAgents()){
			Bank bank = (Bank) b;
			if (bank.getNumericBalanceSheet()[0][StaticValues.SM_LOAN]!=0&&bank.getNetWealth()>0){
				inter+=bank.getPassedValue(StaticValues.LAG_LOANINTEREST, 1);
			}
			else{
				n-=1;
			}
		}
		avInterest=inter/n;
		
		Population govpop = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.GOVERNMENT_ID);
		Government gov = (Government) govpop.getAgentList().get(0);
		double costOfEquity = gov.getAggregateValue(StaticValues.LAG_AVCFIRMCOSTOFEQUITY, 1);
		double equityRatio = gov.getAggregateValue(StaticValues.LAG_AVCFIRMEQUITYRATIO, 1);
		
		double wacc = equityRatio*costOfEquity+(1-equityRatio)*avInterest;
		
		AdaptiveExpectationDoubleExponentialSmoothing expUFCFCapRatio = (AdaptiveExpectationDoubleExponentialSmoothing) investor.getExpectation(StaticValues.EXPECTATIONS_UNLEVEREDFREECASHFLOWPERCAPACITY);
		
		CapitalFirm k = (CapitalFirm)seller;
		
		double NPV = -k.getPrice();
		
		for(int i = 1; i<=newCapital.getCapitalDuration();i++) {
			double expUFCF=expUFCFCapRatio.getExpectation(i);
			NPV += expUFCF/Math.pow((1+wacc), i);
		}
		
		if (desiredCapacity>currentCapacity&&NPV>0){
			this.investment+=adaptiveParameter*distribution.nextDouble();
		}else{
			this.investment-=adaptiveParameter*distribution.nextDouble();
		}
		
//		int newCapitalRealDemand = (int) Math.round(this.investment);
//		
//		return newCapitalRealDemand;
		
		return (int) Math.round(this.investment);
		
//		return this.investment;
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

	public double getInvestment() {
		return investment;
	}

	public void setInvestment(double investment) {
		this.investment = investment;
	}
	
	

}
