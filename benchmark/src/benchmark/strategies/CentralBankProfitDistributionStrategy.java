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
import benchmark.agents.Bank;
import benchmark.agents.CapitalFirm;
import benchmark.agents.ConsumptionFirm;
import benchmark.agents.GovernmentAntiCyclical;
import benchmark.agents.Households;
import jmab.agents.AbstractFirm;
import jmab.agents.LiabilitySupplier;
import jmab.agents.AbstractBank;
import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import jmab.stockmatrix.Deposit;
import jmab.stockmatrix.Item;
import jmab.strategies.DividendsStrategy;
import jmab.strategies.SingleStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Simon Hess
 *
 * This strategy distributes the central bank profits to households and firms equally i.e. every agent receives the same share of the profits
 *
 */
@SuppressWarnings("serial")
public interface CentralBankProfitDistributionStrategy extends SingleStrategy {

	/* (non-Javadoc)
	 * @see jmab.strategies.DividendsStrategy#payDividends()
	 */
	public void distributeCBProfits();


}
