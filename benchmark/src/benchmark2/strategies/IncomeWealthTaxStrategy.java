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

import jmab2.agents.IncomeTaxPayer;
import jmab2.population.MacroPopulation;
import jmab2.strategies.TaxPayerStrategy;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class IncomeWealthTaxStrategy extends AbstractStrategy implements TaxPayerStrategy {

	private double wealthTaxRate;
	private double incomeTaxRate;
	private double maxIncomeTaxRate;
	private double minIncomeTaxRate;
	
	/* (non-Javadoc)
	 * @see jmab2.strategies.TaxPayerStrategy#computeTaxes()
	 */
	@Override
	public double computeTaxes() {
		IncomeTaxPayer taxPayer = (IncomeTaxPayer)this.getAgent();
		double income = taxPayer.getGrossIncome();
		double wealth=taxPayer.getNetWealth();
		return Math.max(wealthTaxRate*wealth+incomeTaxRate*income, 0);
	}
	
	/* (non-Javadoc)
	 * @see jmab2.strategies.TaxPayerStrategy#updateRates(double)
	 */
	@Override
	public void updateRates(double multiplier) {
		this.wealthTaxRate = this.wealthTaxRate*multiplier;
		if (this.incomeTaxRate >= this.maxIncomeTaxRate) {
			this.incomeTaxRate = this.maxIncomeTaxRate;
					}
		else if (this.incomeTaxRate <= minIncomeTaxRate){
			this.incomeTaxRate = this.minIncomeTaxRate;
		}
		else {
			this.incomeTaxRate = this.incomeTaxRate*multiplier;
		}
	}
	/**
	 * @return the wealthTaxRate
	 */
	public double getWealthTaxRate() {
		return wealthTaxRate;
	}

	/**
	 * @param wealthTaxRate the wealthTaxRate to set
	 */
	public void setWealthTaxRate(double wealthTaxRate) {
		this.wealthTaxRate = wealthTaxRate;
	}

	/**
	 * @return the incomeTaxRate
	 */
	public double getIncomeTaxRate() {
		return incomeTaxRate;
	}

	/**
	 * @param incomeTaxRate the incomeTaxRate to set
	 */
	public void setIncomeTaxRate(double incomeTaxRate) {
		this.incomeTaxRate = incomeTaxRate;
	}

	public double getMaxIncomeTaxRate() {
		return maxIncomeTaxRate;
	}

	public void setMaxIncomeTaxRate(double maxIncomeTaxRate) {
		this.maxIncomeTaxRate = maxIncomeTaxRate;
	}

	public double getMinIncomeTaxRate() {
		return minIncomeTaxRate;
	}

	public void setMinIncomeTaxRate(double minIncomeTaxRate) {
		this.minIncomeTaxRate = minIncomeTaxRate;
	}

	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [wealthTaxRate][incomeTaxRate]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(16);
		buf.putDouble(wealthTaxRate);
		buf.putDouble(incomeTaxRate);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [wealthTaxRate][incomeTaxRate]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.wealthTaxRate = buf.getDouble();
		this.incomeTaxRate = buf.getDouble();
	}
	
}
