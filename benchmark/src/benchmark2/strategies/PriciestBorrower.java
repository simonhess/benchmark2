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

import java.util.ArrayList;

import benchmark2.agents.Bank;
import jmab2.population.MacroPopulation;
import jmab2.strategies.SingleStrategy;
import net.sourceforge.jabm.EventScheduler;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class PriciestBorrower extends AbstractStrategy implements SingleStrategy {

	private int loansId;

	/**
	 * 
	 */
	public PriciestBorrower() {
	}

	/**
	 * @param agent
	 */
	public PriciestBorrower(Agent agent) {
		super(agent);
	}

	/**
	 * @param scheduler
	 * @param agent
	 */
	public PriciestBorrower(EventScheduler scheduler, Agent agent) {
		super(scheduler, agent);
	}
	
	/* (non-Javadoc)
	 * @see jmab.strategy.BorrowingStrategy#selectLender(java.util.ArrayList)
	 */
	public Agent selectBorrower(ArrayList<Agent> borrowers,double amount, int length) {
		double maxRate=Double.NEGATIVE_INFINITY;
		Bank maxBorrower=(Bank) borrowers.get(0);
		for(Agent borrower : borrowers){
			double tempRate=((Bank)borrower).getInterestRate(loansId, null,amount, length);
			if(tempRate>maxRate){
				maxRate=tempRate;
				maxBorrower=(Bank)borrower;
			}
		}
		return maxBorrower;
	}
	
	/* (non-Javadoc)
	 * @see jmab2.strategies.SingleStrategy#getBytes()
	 */
	@Override
	public byte[] getBytes() {
		return new byte[1];//TODO cannot be null and probably not byte[0].
	}

	/* (non-Javadoc)
	 * @see jmab2.strategies.SingleStrategy#populateFromBytes(byte[], jmab2.population.MacroPopulation)
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
	}

	public int getLoansId() {
		return loansId;
	}

	public void setLoansId(int loansId) {
		this.loansId = loansId;
	}

}
