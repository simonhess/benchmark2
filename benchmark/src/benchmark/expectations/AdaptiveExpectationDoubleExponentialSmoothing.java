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
package benchmark.expectations;

import java.nio.ByteBuffer;

import jmab2.expectations.Expectation;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class AdaptiveExpectationDoubleExponentialSmoothing implements Expectation {
	
	private double[][] passedValues;
	private int nbPeriod;
	private int nbVariables;
	private double expectation;
	private double[][] weights;
	private double adaptiveParam;
	private double alpha;
	private double beta;
	private double level;
	private double trend;
	
	/**
	 * 
	 */
	public AdaptiveExpectationDoubleExponentialSmoothing() {
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
		
		double pastLevel=this.level;
		double pastTrend=this.trend;
		this.level=alpha*passedValues[0][0]+(1-alpha)*(pastLevel+pastTrend);
		this.trend = beta*(this.level-pastLevel)+(1-beta)*this.trend;
		double result=this.level+this.trend;
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
	 * Returns the prediction t periods ahead
	 */
	public double getExpectation(int t) {
		return this.level+t*this.trend;
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

	public double getAlpha() {
		return alpha;
	}

	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	public double getBeta() {
		return beta;
	}

	public void setBeta(double beta) {
		this.beta = beta;
	}

	public double getTrend() {
		return trend;
	}

	public void setTrend(double trend) {
		this.trend = trend;
	}

	public double getLevel() {
		return level;
	}

	public void setLevel(double level) {
		this.level = level;
	}

}
