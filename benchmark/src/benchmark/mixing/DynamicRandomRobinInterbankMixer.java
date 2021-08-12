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
package benchmark.mixing;

import java.util.ArrayList;
import java.util.List;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import jmab.agents.MacroAgent;
import jmab.mixing.AbstractMarketMixer;
import jmab.mixing.MarketMixer;
import jmab.population.MarketPopulation;
import jmab.simulations.MacroSimulation;
import jmab.simulations.MarketSimulation;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.agent.AgentList;
import net.sourceforge.jabm.event.AgentArrivalEvent;
import cern.jet.random.engine.RandomEngine;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class DynamicRandomRobinInterbankMixer extends AbstractMarketMixer implements MarketMixer {

	protected RandomEngine prng;
	protected boolean supplySurplus; // Indicate whether there is a supply surplus or not
	
	/**
	 * @param prng
	 */
	public DynamicRandomRobinInterbankMixer(){}
	public DynamicRandomRobinInterbankMixer(RandomEngine prng) {
		super();
		this.prng = prng;
	}

	private void invokeInteractions(AgentList buyers, AgentList sellers, SimulationController model) {
		buyers.shuffle(prng);
		// Determine if there is a supply or demand surplus
					double interbankSupply = 0;
					double interbankDemand = 0;
					for(Agent seller: sellers.getAgents()) {
						Bank b = (Bank) seller;
						interbankSupply += b.getLoanSupply(StaticValues.SM_INTERBANK, null,0);
					}
					for(Agent buyer: buyers.getAgents()) {
						Bank b = (Bank) buyer;
						interbankDemand += b.getLoanRequirement(StaticValues.SM_INTERBANK);
					}
		if (interbankSupply > interbankDemand) { // If there is a supply surplus one buyer is matched with multiple sellers
		supplySurplus = true;
		for (Agent buyer : buyers.getAgents()) {
			MacroAgent b = (MacroAgent)buyer;
			MacroSimulation sim = (MacroSimulation)model.getSimulation();
			if(b.isActive(sim.getActiveMarketId())){
				ArrayList<Agent> allSellers = (ArrayList<Agent>) sellers.getAgents();
				ArrayList<Agent> activeSellers = new ArrayList<Agent>();
				for(int i=0;i<allSellers.size();i++){
					MacroAgent seller = (MacroAgent)allSellers.get(i);
					if(seller.isActive(sim.getActiveMarketId())){
						activeSellers.add(seller);
					}
				}
				if(activeSellers.size()>0){
					AgentArrivalEvent event = 
							new AgentArrivalEvent(model, buyer, activeSellers);
					model.fireEvent(event);
				}
			}
		}
		}else { // If there is a demand surplus one seller is matched with multiple buyers
		supplySurplus = false;
		for (Agent seller : sellers.getAgents()) {
			MacroAgent s = (MacroAgent)seller;
			MacroSimulation sim = (MacroSimulation)model.getSimulation();
			if(s.isActive(sim.getActiveMarketId())){
				ArrayList<Agent> allBuyers = (ArrayList<Agent>) buyers.getAgents();
				ArrayList<Agent> activeBuyers = new ArrayList<Agent>();
				for(int i=0;i<allBuyers.size();i++){
					MacroAgent buyer = (MacroAgent)allBuyers.get(i);
					if(buyer.isActive(sim.getActiveMarketId())){
						activeBuyers.add(buyer);
					}
				}
				if(activeBuyers.size()>0){
					AgentArrivalEvent event = 
							new AgentArrivalEvent(model, seller, activeBuyers);
					model.fireEvent(event);
				}
			}
		}
		}
	}
	
	/* (non-Javadoc)
	 * @see jmab.mixing.AbstractMacroMIxer#invokeAgentInteractions(net.sourceforge.jabm.Population, net.sourceforge.jabm.SimulationController)
	 */
	@Override
	public void invokeAgentInteractions(MarketPopulation population,
			SimulationController simulation) {
		invokeInteractions(population.getBuyers(),population.getSellers(),simulation);
	}
	
	/**
	 * @return the prng
	 */
	public RandomEngine getPrng() {
		return prng;
	}

	/**
	 * @param prng the prng to set
	 */
	public void setPrng(RandomEngine prng) {
		this.prng = prng;
	}
	
	@Override
	public boolean closed(MarketPopulation population, MacroSimulation simulation) {
		boolean closed = super.closed(population, simulation);
		if(closed){
			population.getBuyers().setAgents(new ArrayList<Agent>());
			population.getSellers().setAgents(new ArrayList<Agent>());
		}
		return closed;
	}
	
	public boolean getSupplySurplus() {
		return supplySurplus;
	}
	public void setSupplySurplus(boolean supplySurplus) {
		this.supplySurplus = supplySurplus;
	}

}
