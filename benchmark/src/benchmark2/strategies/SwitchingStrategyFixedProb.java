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

import jmab2.population.MacroPopulation;
import jmab2.strategies.AbstractSwitchingStrategy;

/**
 * @author Simon Hess
 * According to this strategy the probability of switching is fixed to the passed probability value
 */
public class SwitchingStrategyFixedProb extends AbstractSwitchingStrategy {
	
	double probability;
	/**
	 * @return the probability
	 */
	public double getProbability() {
		return probability;
	}


	/**
	 * @param probability the probability to set
	 */
	public void setProbability(double probability) {
		this.probability = probability;
	}


	@Override
	public byte[] getBytes() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		// TODO Auto-generated method stub
		
	}


	@Override
	protected double getProbability(double previous, double potential) {
		// TODO Auto-generated method stub
		return probability;
	}



	
}
