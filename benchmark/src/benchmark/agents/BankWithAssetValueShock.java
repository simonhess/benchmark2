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
package benchmark.agents;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import jmab.simulations.MacroSimulation;
import jmab.stockmatrix.Item;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.event.AgentArrivalEvent;
import net.sourceforge.jabm.event.RoundFinishedEvent;
import benchmark.StaticValues;
import benchmark.strategies.PriciestBorrower;

/**
 * @author Simon Hess
 * 
 * This bank class is used to simulate bankruptcies
 *
 */
@SuppressWarnings("serial")
public class BankWithAssetValueShock extends Bank {

	/**
	 * 
	 */
	private void determineBankruptcy() {
		int round = ((MacroSimulation)((SimulationController)this.scheduler).getSimulation()).getRound();
		SimulationController controller = (SimulationController)this.getScheduler();
		MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
		Population bs = macroPop.getPopulation(StaticValues.BANKS_ID);
//		if(round==100&&this.equals(bs.getAgentList().get(0))) {
//		for (Item i:this.getItemsStockMatrix(true, StaticValues.SM_LOAN)){
//			i.setValue(i.getValue()*0.75);
//		}
//		}
		ArrayList<Integer> bankIDsOrdered = new ArrayList<Integer>();
		
		for(Agent a : bs.getAgents()) {
			MacroAgent mA = (MacroAgent) a;
			bankIDsOrdered.add((int) mA.getAgentId());
		}
		Collections.sort(bankIDsOrdered);
		
		int targetBankIndex = (round%100)/10;
		int targetBankID = bankIDsOrdered.get(targetBankIndex);
		if(this.getAgentId()==targetBankID&&round%10==0) {
			
			for (Item i:this.getItemsStockMatrix(true, StaticValues.SM_LOAN)){
				//i.setValue(i.getValue()*0.75);
			}
			}
		double nW=this.getNetWealth();
		if(nW<0){
			this.defaulted=true;
			//this.dead=true;
			//this.unsubscribeFromEvents();
		}
	}
	
}
