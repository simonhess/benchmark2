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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import benchmark.StaticValues;
import cern.jet.random.engine.RandomEngine;
import benchmark.report.TotalCreditComputer;
import benchmark.strategies.FixedShareOfProfitsToPopulationAsShareOfWealthDividends;
import benchmark.report.AveragePriceAllProducersComputer;
import benchmark.report.NominalGDPComputer;
import jmab2.agents.BondDemander;
import jmab2.agents.BondSupplier;
import jmab2.agents.LaborDemander;
import jmab2.agents.LaborSupplier;
import jmab2.agents.LiabilitySupplier;
import jmab2.agents.MacroAgent;
import jmab2.agents.SimpleAbstractAgent;
import jmab2.agents.TaxPayer;
import jmab2.events.MacroTicEvent;
import jmab2.population.MacroPopulation;
import jmab2.report.AveragePriceComputer;
import jmab2.report.UnemploymentRateComputer;
import jmab2.simulations.MacroSimulation;
import jmab2.stockmatrix.Bond;
import jmab2.stockmatrix.CapitalGood;
import jmab2.stockmatrix.ConsumptionGood;
import jmab2.stockmatrix.Deposit;
import jmab2.stockmatrix.Item;
import jmab2.strategies.InterestRateStrategy;
import jmab2.strategies.SelectWorkerStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.agent.AgentList;
import net.sourceforge.jabm.event.AgentArrivalEvent;
import net.sourceforge.jabm.event.RoundFinishedEvent;

/**
 * @author Alessandro Caiani and Antoine Godin
 * Note that the government uses a reserve account in the central bank rather than a deposit account due to
 * the bond market.
 */
@SuppressWarnings("serial")
public class Government extends SimpleAbstractAgent implements LaborDemander, BondSupplier{

	protected ArrayList<MacroAgent> employees;
	protected int laborDemand;
	protected double turnoverLabor;
	protected int fixedLaborDemand;
	protected int[] taxedPopulations;
	protected double bondPrice;
	protected int bondMaturity;
	protected double bondInterestRate;
	protected UnemploymentRateComputer uComputer;
	protected TotalCreditComputer aggregateCreditComputer;
	protected NominalGDPComputer nominalGdpComputer;  
	protected AveragePriceComputer cAvpComputer;
	protected AveragePriceComputer kAvpComputer;
	protected AveragePriceAllProducersComputer avpAllProdComputer; 

	protected double wageBill;
	protected double totInterestsBonds;
	protected RandomEngine prng;
	
	protected double targetCapacityUtlization;
	



	/**
	 * 
	 */
	public Government() {
		super();
		this.employees= new ArrayList<MacroAgent>();
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.MacroAgent#onRoundFinished(net.sourceforge.jabm.event.RoundFinishedEvent)
	 */
	@Override
	public void onRoundFinished(RoundFinishedEvent event) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see jmab2.agents.MacroAgent#initialiseCounterpart(net.sourceforge.jabm.agent.Agent, int)
	 */
	@Override
	public void initialiseCounterpart(Agent counterpart, int marketID) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see jmab2.agents.LaborDemander#getPayingStocks(int, jmab.goods.Item)
	 */
	@Override
	public List<Item> getPayingStocks(int idMarket, Item payableStock) {
		List<Item> result = new ArrayList<Item>();
		if (idMarket == StaticValues.MKT_CAPGOOD || idMarket == StaticValues.MKT_CONSGOOD) {
		List<Item>depositAccounts = this.getItemsStockMatrix(true, StaticValues.SM_RESERVES);
		result.add(depositAccounts.get(1));
		}
		else {
		result.add(this.getItemStockMatrix(true, StaticValues.SM_RESERVES));
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.LaborDemander#addEmployee(jmab2.agents.LaborSupplier)
	 */
	@Override
	public void addEmployee(LaborSupplier worker) {
		this.laborDemand-=1;
		this.employees.add(worker);
		worker.setEmployer(this);
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.LaborDemander#getEmployees()
	 */
	@Override
	public List<MacroAgent> getEmployees() {
		return this.employees;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.SimpleAbstractAgent#onTicArrived(AgentTicEvent)
	 */
	@Override
	protected void onTicArrived(MacroTicEvent event) {
		switch(event.getTic()){
		case StaticValues.TIC_GOVERNMENTLABOR:
			computeLaborDemand();
			break;
		case StaticValues.TIC_TAXES:
			collectTaxes(event.getSimulationController());
			break;
		case StaticValues.TIC_BONDINTERESTS:
			payInterests();
			break;
		case StaticValues.TIC_BONDSUPPLY:
			determineBondsInterestRate();
			emitBonds();
			break;
		case StaticValues.TIC_COMPUTEAGGREGATES:
			this.updateAggregateVariables();
			break;
		case StaticValues.TIC_UPDATEEXPECTATIONS:
			this.updateExpectations();
			break;
		}
	}

	/**
	 * 
	 */
	protected void updateAggregateVariables() {
		
		MacroSimulation sim = (MacroSimulation)((SimulationController)this.scheduler).getSimulation();
		this.setAggregateValue(StaticValues.LAG_AGGUNEMPLOYMENT, 
				uComputer.computeVariable(sim));
		this.setAggregateValue(StaticValues.LAG_AGGCREDIT, aggregateCreditComputer.computeVariable(sim));
		this.setAggregateValue(StaticValues.LAG_NOMINALGDP, 
				nominalGdpComputer.computeVariable(sim));
		this.setAggregateValue(StaticValues.LAG_INFLATION, 
				cAvpComputer.computeVariable(sim));
		double unemployment = this.getAggregateValue(StaticValues.LAG_AGGUNEMPLOYMENT, 0);
		double nominalGDP = this.getAggregateValue(StaticValues.LAG_NOMINALGDP, 0);
		//double inflation = this.getAggregateValue(StaticValues.LAG_INFLATION, 0);
		
		this.setAggregateValue(StaticValues.LAG_ALLPRICE, 
				avpAllProdComputer.computeVariable(sim));
		this.setAggregateValue(StaticValues.LAG_CPRICE, 
				cAvpComputer.computeVariable(sim));
		this.setAggregateValue(StaticValues.LAG_KPRICE, 
				kAvpComputer.computeVariable(sim));
		
		double realGDP = nominalGDP/this.getAggregateValue(StaticValues.LAG_ALLPRICE, 0);
		
		this.setAggregateValue(StaticValues.LAG_REALGDP, 
				realGDP);
		
		// Calculate real capacity utilization and potential capacity utilization
		
		MacroPopulation macroPop = (MacroPopulation) sim.getPopulation();
		Population pop = macroPop.getPopulation(StaticValues.CONSUMPTIONFIRMS_ID);
		
		double capacity = 0;
		double production = 0;

		for (Agent i:pop.getAgents()){
			ConsumptionFirm firm= (ConsumptionFirm) i;
			if (!firm.isDead()){
			capacity += firm.getPassedValue(StaticValues.LAG_CAPACITY, 0);
			production += firm.getPassedValue(StaticValues.LAG_PRODUCTION, 0);
			}
		}
		
		double capUtilization = 0;
		
		if(capacity!=0) capUtilization = production/capacity;
		else capUtilization = this.getTargetCapacityUtlization();
		
		double potentialCapUtilizationRatio = capUtilization/this.getTargetCapacityUtlization();
		
		double nominalGDPCFirms = nominalGdpComputer.computeCFirmGDP(sim, 1);
		
		double realGDPCFirms = nominalGDPCFirms/ this.getAggregateValue(StaticValues.LAG_CPRICE, 0);
		
		double potentialGDPCFirms = realGDPCFirms/potentialCapUtilizationRatio;
		
		
		double potentialGDP =realGDP-realGDPCFirms+potentialGDPCFirms;
		
		this.setAggregateValue(StaticValues.LAG_POTENTIALGDP, 
				potentialGDP);
		
		Population cfpop = macroPop.getPopulation(StaticValues.CONSUMPTIONFIRMS_ID);
		Population kfpop = macroPop.getPopulation(StaticValues.CAPITALFIRMS_ID);
		Population hhpop = macroPop.getPopulation(StaticValues.HOUSEHOLDS_ID);
		Population bpop = macroPop.getPopulation(StaticValues.BANKS_ID);
		
		double tG = 0;
		double avCPrice = 0;
		double avKPrice = 0;
		double cFirmTotalProfit = 0;
		double cFirmTotalEquity = 0;
		double cFirmTotalLoans = 0;

		double nonBankMoneySupply = this.getNumericBalanceSheet()[0][StaticValues.SM_DEP]
				+this.getNumericBalanceSheet()[0][StaticValues.SM_RESERVES]
				+this.getNumericBalanceSheet()[0][StaticValues.SM_CASH];
		
		for (Agent i:cfpop.getAgents()){
			ConsumptionFirm firm= (ConsumptionFirm) i;
			tG+=firm.getPassedValue(StaticValues.LAG_TAXES, 0);
			avCPrice+=firm.getPrice();
			cFirmTotalProfit+=firm.getPassedValue(StaticValues.LAG_PROFITAFTERTAX, 0);
			cFirmTotalEquity+=firm.getPassedValue(StaticValues.LAG_NETWEALTH, 1);
			nonBankMoneySupply +=firm.getNumericBalanceSheet()[0][StaticValues.SM_DEP];
			nonBankMoneySupply += firm.getNumericBalanceSheet()[0][StaticValues.SM_RESERVES];
			nonBankMoneySupply += firm.getNumericBalanceSheet()[0][StaticValues.SM_CASH];
			for (Item loan:firm.getItemsStockMatrix(false, StaticValues.SM_LOAN)){
				cFirmTotalLoans+=loan.getValue();
			}
		}
		
		for (Agent i:kfpop.getAgents()){
			CapitalFirm firm= (CapitalFirm) i;
			tG+=firm.getPassedValue(StaticValues.LAG_TAXES, 0);
			avKPrice+=firm.getPrice();
			nonBankMoneySupply +=firm.getNumericBalanceSheet()[0][StaticValues.SM_DEP];
			nonBankMoneySupply += firm.getNumericBalanceSheet()[0][StaticValues.SM_RESERVES];
			nonBankMoneySupply += firm.getNumericBalanceSheet()[0][StaticValues.SM_CASH];
		}
		
		for (Agent i:hhpop.getAgents()){
			Households hh= (Households) i;
			tG+=hh.getPassedValue(StaticValues.LAG_TAXES, 0);
			nonBankMoneySupply +=hh.getNumericBalanceSheet()[0][StaticValues.SM_DEP];
			nonBankMoneySupply += hh.getNumericBalanceSheet()[0][StaticValues.SM_RESERVES];
			nonBankMoneySupply += hh.getNumericBalanceSheet()[0][StaticValues.SM_CASH];
		}
		
		for (Agent i:bpop.getAgents()){
			Bank b= (Bank) i;
			tG+=b.getPassedValue(StaticValues.LAG_TAXES, 0);
		}

		this.setAggregateValue(StaticValues.LAG_GOVTAX, 
				tG);
		
		avCPrice/=cfpop.getSize();
		avKPrice/=kfpop.getSize();
		
		this.setAggregateValue(StaticValues.LAG_AVCPRICE, 
				avCPrice);
		this.setAggregateValue(StaticValues.LAG_AVKPRICE, 
				avKPrice);
		double avCFirmCostOfEquity = cFirmTotalProfit/cFirmTotalEquity;
		this.setAggregateValue(StaticValues.LAG_NONBANKMONEYSUPPLY, 
				nonBankMoneySupply);
		
		this.setAggregateValue(StaticValues.LAG_AVCFIRMCOSTOFEQUITY, avCFirmCostOfEquity);
		this.setAggregateValue(StaticValues.LAG_AVCFIRMEQUITYRATIO, cFirmTotalEquity/(cFirmTotalEquity+cFirmTotalLoans));
	}
	
	protected void updateExpectations() {
		// TODO Auto-generated method stub
		this.cleanSM();
	}

	public void onAgentArrival(AgentArrivalEvent event) {
		MacroSimulation macroSim = (MacroSimulation)event.getSimulationController().getSimulation();
		int marketID=macroSim.getActiveMarket().getMarketId();
		switch(marketID){
		case StaticValues.MKT_LABOR: //se should use random robin mixer in the case of government labor market
			SelectWorkerStrategy strategy = (SelectWorkerStrategy) this.getStrategy(StaticValues.STRATEGY_LABOR);
			List<MacroAgent> workers= (List<MacroAgent>)strategy.selectWorkers(event.getObjects(),this.laborDemand);
			for(MacroAgent worker:workers)
				macroSim.getActiveMarket().commit(this, worker,marketID);
			break;
		}
	}

	/**
	 * Updates the interest rate
	 */
	protected void determineBondsInterestRate() {
		InterestRateStrategy strategy = (InterestRateStrategy)this.getStrategy(StaticValues.STRATEGY_BONDINTERESTRATE);
		this.bondInterestRate = strategy.computeInterestRate(null, 0, 0);
	}

	/**
	 * Emits bonds. If there is a deficit, the deposit account of the government is negative. The deficit is then divided
	 * by the price of bonds, rounded to the largest int. Bonds are then emitted and added to the StockMatrix.
	 */
	protected void emitBonds() {
		Item deposit=this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
		double deficit=deposit.getValue();
		long quantity = 0;
		if(deficit<0){
			quantity = (long)Math.ceil(Math.abs(deficit)/this.bondPrice);
		}
		Bond remainingBonds = (Bond)this.getItemStockMatrix(false, StaticValues.SM_BONDS,this);
		if(remainingBonds!=null){
			quantity+=remainingBonds.getQuantity();
			this.removeItemStockMatrix(remainingBonds, false, StaticValues.SM_BONDS);
		}
		Bond newIssue = new Bond(quantity*bondPrice, quantity, this, this, this.bondMaturity, this.bondInterestRate,
				this.bondPrice);
		this.addItemStockMatrix(newIssue, false, StaticValues.SM_BONDS);
		this.setActive(true, StaticValues.MKT_BONDS);
	}

	/**
	 * Pays bonds interests to their asset holder
	 */
	protected void payInterests() {
		List<Item> bonds=this.getItemsStockMatrix(false, StaticValues.SM_BONDS);
		Item deposit=this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
		SimulationController simulationController = (SimulationController)this.getScheduler();
		MacroPopulation populations = (MacroPopulation)simulationController.getPopulation();
		Collection <Agent> bpop = populations.getPopulation(StaticValues.BANKS_ID).getAgents();
		Collection <BondDemander> banks = new ArrayList<BondDemander>(); // Create list with all banks
		Collection <Agent> hhpop = populations.getPopulation(StaticValues.HOUSEHOLDS_ID).getAgents();
		Collection <BondDemander> hhs = new ArrayList<BondDemander>(); // Create list with all households
		for (Agent i:bpop){
			banks.add((BondDemander)i); // Cast Bank Agents to BondDemanders
		}
		for (Agent i:hhpop){
			hhs.add((BondDemander)i); // Cast Bank Agents to BondDemanders
		}
		double interestsBonds=0;
		for(Item b:bonds){
			Bond bond=(Bond)b;
			if(bond.getAssetHolder() instanceof Government){
				bond.setQuantity(0);
			}else{
				BondDemander holder = (BondDemander)bond.getAssetHolder();
				holder.setBondInterestsReceived(bond.getValue()*bond.getInterestRate());
				interestsBonds+=bond.getValue()*bond.getInterestRate();
				if(holder instanceof CentralBank){
					deposit.setValue(deposit.getValue()-bond.getValue()*bond.getInterestRate());
					if(bond.getAge()==bond.getMaturity()){
						deposit.setValue(deposit.getValue()-bond.getValue());
						bond.setQuantity(0);
					}
				}
				else if(holder instanceof Bank){
					// Remove bond holding banks from list
					if (banks.contains(holder)) {
					banks.remove(holder); // Remove all banks which hold bonds
					}
					Item hDep = holder.getItemStockMatrix(true, StaticValues.SM_RESERVES);
					hDep.setValue(hDep.getValue()+bond.getValue()*bond.getInterestRate());
					deposit.setValue(deposit.getValue()-bond.getValue()*bond.getInterestRate());
					if(bond.getAge()==bond.getMaturity()){
						hDep.setValue(hDep.getValue()+bond.getValue());
						deposit.setValue(deposit.getValue()-bond.getValue());
						bond.setQuantity(0);
					}
				}
				else if(holder instanceof Households){
					// Remove bond holding banks from list
					if (hhs.contains(holder)) {
					hhs.remove(holder); // Remove all households which hold bonds
					}
					Households hh = (Households) holder;
					CentralBank cb = (CentralBank) deposit.getLiabilityHolder();
					cb.transfer(deposit, hh.getPayableStock(StaticValues.MKT_LABOR), bond.getValue()*bond.getInterestRate());
					if(bond.getAge()==bond.getMaturity()){
						cb.transfer(deposit, hh.getPayableStock(StaticValues.MKT_LABOR), bond.getValue());
						bond.setQuantity(0);
					}
				}
			}
		}
		// set BondInterestRecieved of all banks which hold no bonds to zero
		for (BondDemander i:banks){
			i.setBondInterestsReceived(0.00);
		}
		totInterestsBonds=interestsBonds;
	}

	/**
	 * Asks every tax payer to pay taxes
	 * @param simulationController
	 */
	protected void collectTaxes(SimulationController simulationController) {
		Item account = this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
		MacroPopulation populations = (MacroPopulation)simulationController.getPopulation();
//		double taxesRevenues=0;
		for(int popId:this.taxedPopulations){
			Population pop = populations.getPopulation(popId);
			for(Agent a:pop.getAgents()){
				TaxPayer agent=(TaxPayer) a;
				if(!agent.isDead())
					agent.payTaxes(account);
			}
		}
		
	}

	/**
	 * Sets the labor demand equal to the fixed labor demand
	 */
	protected void computeLaborDemand() {
		int currentWorkers = this.employees.size();
		int nbWorkers = this.fixedLaborDemand;
		if(nbWorkers>currentWorkers){
			this.setActive(true, StaticValues.MKT_LABOR);
			this.laborDemand=nbWorkers-currentWorkers;
		}else{
			this.setActive(false, StaticValues.MKT_LABOR);
			this.laborDemand=0;
			AgentList emplPop = new AgentList();
			for(MacroAgent ag : this.employees)
				emplPop.add(ag);
			emplPop.shuffle(prng);
			for(int i=0;i<currentWorkers-nbWorkers;i++){
				fireAgent((MacroAgent)emplPop.get(i));
			}
		}
		cleanEmployeeList();
		if(employees.size()>0){
			Deposit deposit = (Deposit) this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
			payWages(deposit,StaticValues.MKT_LABOR);
		}
		
		cleanEmployeeList();
	}

	protected void payWages(Item payingItem, int idMarket) {
		double wages=0;
		int currentWorkers = this.employees.size();
		AgentList emplPop = new AgentList();
		for(MacroAgent ag : this.employees)
			emplPop.add(ag);
		emplPop.shuffle(prng);
		for(int i=0;i<currentWorkers;i++){
			LaborSupplier employee = (LaborSupplier) emplPop.get(i);
			double wage = employee.getWage();
			Item payableStock = employee.getPayableStock(idMarket);
			LiabilitySupplier payingSupplier = (LiabilitySupplier) payingItem.getLiabilityHolder();
			payingSupplier.transfer(payingItem, payableStock, wage);
			wages+=wage;
		}
		wageBill=wages;
		
	}
	
	protected void cleanEmployeeList(){
		ArrayList<MacroAgent> newEmployee = new ArrayList<MacroAgent>();
		for(MacroAgent employee:employees){
			if(((LaborSupplier) employee).getEmployer()!=null){
				newEmployee.add(employee);
			}
		}
		this.employees=newEmployee;
	}
	
	/**
	 * Fires the employee and removes the connection between employee and employer
	 * @param employee the employee to fire
	 */
	protected void fireAgent(MacroAgent employee){
		LaborSupplier emp = (LaborSupplier) employee;
		emp.setEmployer(null);
		employee.setActive(true, StaticValues.MKT_LABOR);//the fired workers is reactivated in the labor market
	}
	
	/**
	 * @return the bondInterestRate
	 */
	public double getBondInterestRate() {
		return bondInterestRate;
	}

	/**
	 * @param bondInterestRate the bondInterestRate to set
	 */
	public void setBondInterestRate(double bondInterestRate) {
		this.bondInterestRate = bondInterestRate;
	}

	/**
	 * @return the laborDemand
	 */
	public int getLaborDemand() {
		return laborDemand;
	}

	/**
	 * @param laborDemand the laborDemand to set
	 */
	public void setLaborDemand(int laborDemand) {
		this.laborDemand = laborDemand;
	}

	/**
	 * @param employees the employees to set
	 */
	public void setEmployees(ArrayList<MacroAgent> employees) {
		this.employees = employees;
	}

	/**
	 * @return the fixedLaborDemand
	 */
	public int getFixedLaborDemand() {
		return fixedLaborDemand;
	}

	/**
	 * @param fixedLaborDemand the fixedLaborDemand to set
	 */
	public void setFixedLaborDemand(int fixedLaborDemand) {
		this.fixedLaborDemand = fixedLaborDemand;
	}

	/**
	 * @return the taxedPopulations
	 */
	public int[] getTaxedPopulations() {
		return taxedPopulations;
	}

	/**
	 * @param taxedPopulations the taxedPopulations to set
	 */
	public void setTaxedPopulations(int[] taxedPopulations) {
		this.taxedPopulations = taxedPopulations;
	}

	/**
	 * @return the bondValue
	 */
	public double getBondPrice() {
		return bondPrice;
	}

	/**
	 * @param bondValue the bondValue to set
	 */
	public void setBondPrice(double bondPrice) {
		this.bondPrice = bondPrice;
	}

	/**
	 * @return the bondMaturity
	 */
	public int getBondMaturity() {
		return bondMaturity;
	}

	/**
	 * @param bondMaturity the bondMaturity to set
	 */
	public void setBondMaturity(int bondMaturity) {
		this.bondMaturity = bondMaturity;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.BondSupplier#getPayableStock(int)
	 */
	@Override
	public Item getPayableStock(int idBondSM) {
		return this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.BondSupplier#getSupply()
	 */
	@Override
	public long getBondSupply() {
		return (long) this.getItemStockMatrix(false, StaticValues.SM_BONDS,this).getQuantity();
	}

	/**
	 * @return the uComputer
	 */
	public UnemploymentRateComputer getuComputer() {
		return uComputer;
	}

	/**
	 * @param uComputer the uComputer to set
	 */
	public void setuComputer(UnemploymentRateComputer uComputer) {
		this.uComputer = uComputer;
	}

	/* (non-Javadoc)
	 * @see jmab2.agents.LaborDemander#getWageBill()
	 */
	@Override
	public double getWageBill() {
		double wageBill=0;
		for(MacroAgent employee:employees){
			wageBill+=((LaborSupplier)employee).getWage();
		}
		return wageBill;
	}

	/**
	 * @return the turnoverLabor
	 */
	public double getTurnoverLabor() {
		return turnoverLabor;
	}

	/**
	 * @param turnoverLabor the turnoverLabor to set
	 */
	public void setTurnoverLabor(double turnoverLabor) {
		this.turnoverLabor = turnoverLabor;
	}

	/**
	 * @param wageBill the wageBill to set
	 */
	public void setWageBill(double wageBill) {
		this.wageBill = wageBill;
	}

	/**
	 * @return the totInterestsBonds
	 */
	public double getTotInterestsBonds() {
		return totInterestsBonds;
	}

	/**
	 * @param totInterestsBonds the totInterestsBonds to set
	 */
	public void setTotInterestsBonds(double totInterestsBonds) {
		this.totInterestsBonds = totInterestsBonds;
	}
	
	



	

	/**
	 * Populates the agent characteristics using the byte array content. The structure is as follows:
	 * [sizeMacroAgentStructure][MacroAgentStructure][bondPrice][bondInterestRate][turnoverLabor][laborDemand][fixedLaborDemand][bondMaturity]
	 * [sizeTaxedPop][taxedPopulations][matrixSize][stockMatrixStructure][expSize][ExpectationStructure][passedValSize][PassedValStructure]
	 * [stratsSize][StrategiesStructure]
	 */
	@Override
	public void populateAgent(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		byte[] macroBytes = new byte[buf.getInt()];
		buf.get(macroBytes);
		super.populateCharacteristics(macroBytes, pop);
		bondPrice = buf.getDouble();
		bondInterestRate = buf.getDouble();
		turnoverLabor = buf.getDouble();
		laborDemand = buf.getInt();
		fixedLaborDemand = buf.getInt();
		bondMaturity = buf.getInt();
		int lengthTaxedPopulatiobns = buf.getInt();
		taxedPopulations = new int[lengthTaxedPopulatiobns];
		for(int i = 0 ; i < lengthTaxedPopulatiobns ; i++){
			taxedPopulations[i] = buf.getInt();
		}
		int matSize = buf.getInt();
		if(matSize>0){
			byte[] smBytes = new byte[matSize];
			buf.get(smBytes);
			this.populateStockMatrixBytes(smBytes, pop);
		}
		int expSize = buf.getInt();
		if(expSize>0){
			byte[] expBytes = new byte[expSize];
			buf.get(expBytes);
			this.populateExpectationsBytes(expBytes);
		}
		int lagSize = buf.getInt();
		if(lagSize>0){
			byte[] lagBytes = new byte[lagSize];
			buf.get(lagBytes);
			this.populatePassedValuesBytes(lagBytes);
		}
		int stratSize = buf.getInt();
		if(stratSize>0){
			byte[] stratBytes = new byte[stratSize];
			buf.get(stratBytes);
			this.populateStrategies(stratBytes, pop);
		}
	}
	
	/**
	 * protected ArrayList<MacroAgent> employees;
	protected UnemploymentRateComputer uComputer; 
	 * Generates the byte array containing all relevant informations regarding the household agent. The structure is as follows:
	 * [sizeMacroAgentStructure][MacroAgentStructure][bondPrice][bondInterestRate][turnoverLabor][laborDemand][fixedLaborDemand][bondMaturity]
	 * [sizeTaxedPop][taxedPopulations][sizeEmployees][employeePopId+employeeId][matrixSize][stockMatrixStructure]
	 * [expSize][ExpectationStructure][passedValSize][PassedValStructure][stratsSize][StrategiesStructure]
	 */
	@Override
	public byte[] getBytes() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			byte[] charBytes = super.getAgentCharacteristicsBytes();
			out.write(ByteBuffer.allocate(4).putInt(charBytes.length).array());
			out.write(charBytes);
			ByteBuffer buf = ByteBuffer.allocate(40+4*taxedPopulations.length);
			buf.putDouble(bondPrice);
			buf.putDouble(bondInterestRate);
			buf.putDouble(turnoverLabor);
			buf.putInt(laborDemand);
			buf.putInt(fixedLaborDemand);
			buf.putInt(bondMaturity);
			buf.putInt(taxedPopulations.length);
			for(int i = 0 ; i < taxedPopulations.length ; i++){
				buf.putInt(taxedPopulations[i]);
			}
			out.write(buf.array());
			byte[] smBytes = super.getStockMatrixBytes();
			out.write(ByteBuffer.allocate(4).putInt(smBytes.length).array());
			out.write(smBytes);
			byte[] expBytes = super.getExpectationsBytes();
			out.write(ByteBuffer.allocate(4).putInt(expBytes.length).array());
			out.write(expBytes);
			byte[] passedValBytes = super.getPassedValuesBytes();
			out.write(ByteBuffer.allocate(4).putInt(passedValBytes.length).array());
			out.write(passedValBytes);
			byte[] stratsBytes = super.getStrategiesBytes();
			out.write(ByteBuffer.allocate(4).putInt(stratsBytes.length).array());
			out.write(stratsBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out.toByteArray();
	}

	/**
	 * Populates the stockMatrix with the byte array content. The structure of the stock matrix is the following:
	 * [nbStockTypes]
	 * for each type of stocks
	 * 	[IdStock][nbItems]
	 * 		for each Item
	 * 			[itemSize][itemStructure]
	 * 		end for
	 * end for 	
	 */
	@Override
	public void populateStockMatrixBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		int nbStockTypes = buf.getInt();
		for(int i = 0 ; i < nbStockTypes ; i++){
			int stockId = buf.getInt();
			int nbStocks = buf.getInt();
			for(int j = 0 ; j < nbStocks ; j++){
				int itemSize = buf.getInt();
				byte[] itemData = new byte[itemSize];
				buf.get(itemData);
				Item it = new Deposit(itemData, pop, this);
				MacroAgent cashHolder = it.getLiabilityHolder();
				cashHolder.addItemStockMatrix(it, false, stockId);
				this.addItemStockMatrix(it, true, stockId);
			}
		}	
	}

	@Override
	public void setLaborActive(boolean active) {
		this.setActive(active, StaticValues.MKT_LABOR);
		
	}

	public RandomEngine getPrng() {
		return prng;
	}

	public void setPrng(RandomEngine prng) {
		this.prng = prng;
	}
	
	/**
	 * @return the aggregateCreditComputer
	 */
	public TotalCreditComputer getAggregateCreditComputer() {
		return aggregateCreditComputer;
	}

	/**
	 * @param aggregateCreditComputer the aggregateCreditComputer to set
	 */
	public void setAggregateCreditComputer(
			TotalCreditComputer aggregateCreditComputer) {
		this.aggregateCreditComputer = aggregateCreditComputer;
	}

	/**
	 * @return the nominalGdpComputer
	 */
	public NominalGDPComputer getNominalGdpComputer() {
		return nominalGdpComputer;
	}

	/**
	 * @param nominalGdpComputer the nominalGdpComputer to set
	 */
	public void setNominalGdpComputer(NominalGDPComputer nominalGdpComputer) {
		this.nominalGdpComputer = nominalGdpComputer;
	}

	/**
	 * @return the avpComputer
	 */
	public AveragePriceComputer getCAvpComputer() {
		return cAvpComputer;
	}
	
	/**
	 * @param avpComputer the avpComputer to set
	 */
	public void setCAvpComputer(AveragePriceComputer cAvpComputer) {
		this.cAvpComputer = cAvpComputer;
	}

	
	public AveragePriceComputer getkAvpComputer() {
		return kAvpComputer;
	}

	public void setkAvpComputer(AveragePriceComputer kAvpComputer) {
		this.kAvpComputer = kAvpComputer;
	}
	
	public AveragePriceAllProducersComputer getAvpAllProdComputer() {
		return avpAllProdComputer;
	}

	public void setAvpAllProdComputer(AveragePriceAllProducersComputer avpAllProdComputer) {
		this.avpAllProdComputer = avpAllProdComputer;
	}
	
	
	public double getTargetCapacityUtlization() {
		return targetCapacityUtlization;
	}

	public void setTargetCapacityUtlization(double targetCapacityUtlization) {
		this.targetCapacityUtlization = targetCapacityUtlization;
	}
	
}
