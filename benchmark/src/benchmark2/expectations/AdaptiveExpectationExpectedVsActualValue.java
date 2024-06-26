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
package benchmark2.expectations;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Random;

import benchmark2.StaticValues;
import benchmark2.agents.CapitalFirm;
import benchmark2.agents.ConsumptionFirm;
import benchmark2.agents.Government;
import jmab2.agents.MacroAgent;
import jmab2.expectations.Expectation;
import jmab2.expectations.PassedValues;
import jmab2.population.MacroPopulation;
import jmab2.simulations.AbstractMacroSimulation;
import jmab2.simulations.MacroSimulation;
import jmab2.stockmatrix.AbstractGood;
import jmab2.stockmatrix.ConsumptionGood;
import jmab2.strategies.TargetExpectedInventoriesOutputStrategy;
import net.sourceforge.jabm.EventScheduler;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.distribution.AbstractDelegatedDistribution;

/**
 * @author Simon Hess
 * 
 * With this strategy expectation are computed based on expected and the actual value. If the expected value is smaller than
 * the actual value the predicted value is increased. If the expected value is greater than the actual one the predicted
 *  values is decreased. 
 *
 */
public class AdaptiveExpectationExpectedVsActualValue implements Expectation {
	
	private double[][] passedValues;
	private int nbPeriod;
	private int nbVariables;
	private double expectation;
	private double[][] weights;
	private double adaptiveParam;
	private AbstractDelegatedDistribution distribution;

	/**
	 * 
	 */

	
	public AdaptiveExpectationExpectedVsActualValue() {
		super();
	}

	/* (non-Javadoc)
	 * @see jmab2.expectations.Expectation#getWeights()
	 */
	@Override
	public double[][] getWeights() {
		return this.weights;
	}

	/* (non-Javadoc)
	 * @see jmab2.expectations.Expectation#setWeights(double[][])
	 */
	@Override
	public void setWeights(double[][] weights) {
		this.weights=weights;
	}

	/* (non-Javadoc)
	 * @see jmab2.expectations.Expectation#getPassedValues()
	 */
	@Override
	public double[][] getPassedValues() {
		return this.passedValues;
	}

	/* (non-Javadoc)
	 * @see jmab2.expectations.Expectation#setPassedValues(double[][])
	 */
	@Override
	public void setPassedValues(double[][] passedValues) {
		this.passedValues=passedValues;
	}

	/* (non-Javadoc)
	 * @see jmab2.expectations.Expectation#getNumberPeriod()
	 */
	@Override
	public int getNumberPeriod() {
		return this.nbPeriod;
	}

	/* (non-Javadoc)
	 * @see jmab2.expectations.Expectation#setNumberPeriod(int)
	 */
	
	@Override
	public void setNumberPeriod(int nbPeriod) {
		this.nbPeriod=nbPeriod;
		this.passedValues= new double [nbPeriod][];
		// The weights matrix instead is injected thrhough the .xml configuration file
	}


	/**
	 * @return the nbVariables
	 */
	public int getNbVariables() {
		return nbVariables;
	}

	/**
	 * @param nbVariables the nbVariables to set
	 */
	public void setNbVariables(int nbVariables) {
		this.nbVariables=1;
		for (int i=0; i<nbPeriod-1; i++){
			this.passedValues[i]= new double [this.nbVariables+1]; // +1 beacuase we put also past expectations
			//  The weights matrix instead is injected thrhough the .xml configuration file
		}
		
		
	}

	/**
	 * @param expectation the expectation to set
	 */
	public void setExpectation(double expectation) {
		this.expectation = expectation;
	}

	/* (non-Javadoc)
	 * @see jmab2.expectations.Expectation#getAdaptiveParam()
	 */
	

	/* (non-Javadoc)
	 * @see jmab2.expectations.Expectation#updateExpectation()
	 */
	@Override
	public void updateExpectation() {
		double result=passedValues [0][1];
		
		double expectedValue = passedValues [0][1];
		
		double actualValue = passedValues[0][0];
		
		if(expectedValue<actualValue) {
			result = passedValues [0][1]+(adaptiveParam*distribution.nextDouble());
		}else if(expectedValue>actualValue) {
			result = passedValues [0][1]-(adaptiveParam*distribution.nextDouble());
	}
		
		this.expectation=result;
	}

	/* (non-Javadoc)
	 * @see jmab2.expectations.Expectation#getExpectation()
	 */
	@Override
	public double getExpectation() {
		return this.expectation;
	}

	/* (non-Javadoc)
	 * @see jmab2.expectations.Expectation#addObservation(double[])
	 */
	@Override
	
	public void addObservation(double[] observation) {
		for(int i=1;i<nbPeriod;i++){
			for(int j=0;j<nbVariables+1;j++){
				this.passedValues[nbPeriod-i][j]=this.passedValues[nbPeriod-i-1][j];
			}
		}
		for(int j=0;j<nbVariables;j++){
			this.passedValues[0][j]=observation[j];
		}
		this.passedValues[0][nbVariables]=this.expectation;
	}

	/**
	 * @return the adaptiveParam
	 */
	public double getAdaptiveParam() {
		return adaptiveParam;
	}

	/**
	 * @param adaptiveParam the adaptiveParam to set
	 */
	public void setAdaptiveParam(double adaptiveParam) {
		this.adaptiveParam = adaptiveParam;
	}
	
	/**
	 * Generates the byte array representing all the informations stored in this expectation. The structure is the following:
	 * [nbPeriod][nbVariables][adaptiveParam][expectation][passedValues][weights]
	 */
	@Override
	public byte[] getByteArray() {
		ByteBuffer buffer = ByteBuffer.allocate(24+16*(nbPeriod*(nbVariables+1)));
		buffer.putInt(nbPeriod);
		buffer.putInt(nbVariables);
		buffer.putDouble(adaptiveParam);
		buffer.putDouble(expectation);
		for(int i = 0; i<passedValues.length ; i++){
			for(int j = 0; j<passedValues[i].length ; j++){
				buffer.putDouble(passedValues[i][j]);
			}
		}
		for(int i = 0; i<weights.length ; i++){
			for(int j = 0; j<weights[i].length ; j++){
				buffer.putDouble(weights[i][j]);
			}
		}
		return buffer.array();
	}
	
	/**
	 * Populates the expectation with the byte array content, the structure is the following
	 * [nbPeriod][nbVariables][adaptiveParam][expectation][passedValues][weights]
	 * @param content a byte array structure containing all relevant data necessary to populate the expectation
	 */
	public void populateExpectation(byte[] content){
		ByteBuffer reader = ByteBuffer.wrap(content);
		this.nbPeriod = reader.getInt();
		this.nbVariables = reader.getInt();
		this.adaptiveParam = reader.getDouble();
		this.expectation = reader.getDouble();
		this.passedValues = new double[nbPeriod][nbVariables+1];
		this.weights = new double[nbPeriod][nbVariables+1];
		for(int i = 0; i<passedValues.length ; i++){
			for(int j = 0; j<passedValues[i].length ; j++){
				passedValues[i][j]=reader.getDouble();
			}
		}
		for(int i = 0; i<weights.length ; i++){
			for(int j = 0; j<weights[i].length ; j++){
				weights[i][j]=reader.getDouble();
			}
		}
	}

	public AbstractDelegatedDistribution getDistribution() {
		return distribution;
	}

	public void setDistribution(AbstractDelegatedDistribution distribution) {
		this.distribution = distribution;
	}

}
