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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jmab.agents.AbstractHousehold;
import jmab.agents.BondDemander;
import jmab.agents.BondSupplier;
import jmab.agents.DepositDemander;
import jmab.agents.GoodDemander;
import jmab.agents.IncomeTaxPayer;
import jmab.agents.LaborSupplier;
import jmab.agents.LiabilitySupplier;
import jmab.agents.MacroAgent;
import jmab.agents.WageSetterWithTargets;
import jmab.events.MacroTicEvent;
import jmab.population.MacroPopulation;
import jmab.simulations.MacroSimulation;
import jmab.simulations.TwoStepMarketSimulation;
import jmab.stockmatrix.Cash;
import jmab.stockmatrix.ConsumptionGood;
import jmab.stockmatrix.Deposit;
import jmab.stockmatrix.Item;
import jmab.strategies.BondDemandStrategy;
import jmab.strategies.ConsumptionStrategy;
import jmab.strategies.SelectDepositSupplierStrategy;
import jmab.strategies.SelectSellerStrategy;
import jmab.strategies.TaxPayerStrategy;
import jmab.strategies.WageStrategy;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.event.AgentArrivalEvent;
import net.sourceforge.jabm.event.RoundFinishedEvent;
import benchmark.StaticValues;


/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class Households extends AbstractHousehold implements GoodDemander, LaborSupplier,
		DepositDemander, IncomeTaxPayer, WageSetterWithTargets, BondDemander {
	
	private double demand;
	private double cashAmount;
	private double depositAmount;
	private double reservesAmount;
	private int employmentWageLag;
	protected double shareDeposits;
	protected double preferredDepositRatio;
	protected double preferredCashRatio;
	protected double preferredReserveRatio;
	protected double interestsReceived;
	protected double reservesInterestsReceived;
	protected double dividendsReceived;
	protected double bondInterestReceived;
	private long bondDemand;
	private BondSupplier selectedBondSupplier;
	private double bondPrice;
	private double bondInterestRate;
	private double unemploymentBenefitAmount;
	private double bailoutcost;
	private double liquidAssetsSum;

	/* (non-Javadoc)
	 * @see jmab.agents.MacroAgent#onRoundFinished(net.sourceforge.jabm.event.RoundFinishedEvent)
	 */
	@Override
	public void onRoundFinished(RoundFinishedEvent event) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see jmab.agents.MacroAgent#initialiseCounterpart(net.sourceforge.jabm.agent.Agent, int)
	 */
	@Override
	public void initialiseCounterpart(Agent counterpart, int marketID) {
		// TODO Auto-generated method stub

	}

	
	/**
	 * @return the shareDeposits
	 */
	public double getShareDeposits() {
		return shareDeposits;
	}

	/**
	 * @param shareDeposits the shareDeposits to set
	 */
	public void setShareDeposits(double shareDeposits) {
		this.shareDeposits = shareDeposits;
	}

	/**
	 * @param cashAmount the cashAmount to set
	 */
	public void setCashAmount(double cashAmount) {
		this.cashAmount = cashAmount;
	}

	/**
	 * @param depositAmount the depositAmount to set
	 */
	public void setDepositAmount(double depositAmount) {
		this.depositAmount = depositAmount;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.DepositDemander#getDepositAmount()
	 */
	@Override
	public double getDepositAmount() {
		return this.depositAmount;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.DepositDemander#getCashAmount()
	 */
	@Override
	public double getCashAmount() {
		return this.cashAmount;
	}
	
	public double getReservesAmount() {
		return reservesAmount;
	}

	public void setReservesAmount(double reservesAmount) {
		this.reservesAmount = reservesAmount;
	}
	
	public double getPreferredDepositRatio() {
		return preferredDepositRatio;
	}

	public void setPreferredDepositRatio(double preferredDepositRatio) {
		this.preferredDepositRatio = preferredDepositRatio;
	}

	public double getPreferredCashRatio() {
		return preferredCashRatio;
	}

	public void setPreferredCashRatio(double preferredCashRatio) {
		this.preferredCashRatio = preferredCashRatio;
	}

	public double getPreferredReserveRatio() {
		return preferredReserveRatio;
	}

	public void setPreferredReserveRatio(double preferredReserveRatio) {
		this.preferredReserveRatio = preferredReserveRatio;
	}


	/**
	 * @return the dividendsReceived
	 */
	public double getDividendsReceived() {
		return dividendsReceived;
	}

	/**
	 * @param dividendsReceived the dividendsReceived to set
	 */
	public void setDividendsReceived(double dividendsReceived) {
		this.dividendsReceived = dividendsReceived;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.LaborSupplier#getPayableStock(int)
	 */
	@Override
	public Item getPayableStock(int idMarket) {
		switch(idMarket){
		case StaticValues.MKT_LABOR:
			
		}
		// Determine most preferred payment method based on liquidity preferences
		double preferredPayableStock = Math.max(Math.max(preferredDepositRatio,preferredCashRatio),preferredReserveRatio);
		if (preferredPayableStock == preferredDepositRatio) {
		return this.getItemStockMatrix(true, StaticValues.SM_DEP);
		} else if (preferredPayableStock == preferredCashRatio){
		return this.getItemStockMatrix(true, StaticValues.SM_CASH);
		} else if (preferredPayableStock == preferredReserveRatio){
		return this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
		}
		else
		return null;
	}

	@Override
	public void onAgentArrival(AgentArrivalEvent event) {
		MacroSimulation macroSim = (MacroSimulation)event.getSimulationController().getSimulation();
		int marketID=macroSim.getActiveMarket().getMarketId();
		switch(marketID){
		case StaticValues.MKT_CONSGOOD:
			SelectSellerStrategy sellerStrategy = (SelectSellerStrategy) this.getStrategy(StaticValues.STRATEGY_BUYING);
			MacroAgent seller = sellerStrategy.selectGoodSupplier(event.getObjects(), this.demand, true); 
			macroSim.getActiveMarket().commit(this, seller, marketID);
			break;
		case StaticValues.MKT_DEPOSIT:
			SelectDepositSupplierStrategy depStrategy = (SelectDepositSupplierStrategy)this.getStrategy(StaticValues.STRATEGY_DEPOSIT);
			MacroAgent depositSupplier = depStrategy.selectDepositSupplier(event.getObjects(), this.depositAmount);
			macroSim.getActiveMarket().commit(this, depositSupplier, marketID);
			break;
		case StaticValues.MKT_BONDS:
			TwoStepMarketSimulation sim = (TwoStepMarketSimulation)macroSim.getActiveMarket();
			if(sim.isFirstStep()){
				this.selectedBondSupplier=(BondSupplier)event.getObjects().get(0);
				this.bondPrice=this.selectedBondSupplier.getBondPrice();
				this.bondInterestRate=this.selectedBondSupplier.getBondInterestRate();
			}else if(sim.isSecondStep())
				macroSim.getActiveMarket().commit(this, this.selectedBondSupplier,marketID);
			break;
		}
	}
	

	/* (non-Javadoc)
	 * @see jmab.agents.SimpleAbstractAgent#onTicArrived(int)
	 */
	@Override
	protected void onTicArrived(MacroTicEvent event) {
		switch(event.getTic()){
		case StaticValues.TIC_COMPUTEEXPECTATIONS:
			setBailoutcost(0);
			setLiquidAssetsSum(0);
			this.computeExpectations();
			break;
		case StaticValues.TIC_LABORSUPPLY:
			computeWage();
			break;
		case StaticValues.TIC_CONSUMPTIONDEMAND:
			computeConsumptionDemand();
			break;
		case StaticValues.TIC_DEPOSITDEMAND:
			computeLiquidAssetsAmounts();
			break;
		case StaticValues.TIC_UPDATEEXPECTATIONS:
			updateExpectations();
			break;
		case StaticValues.TIC_BONDDEMAND:
			determineBondDemand();
			break;
		case StaticValues.TIC_BANKRUPTCY:
			// Compute sum of all liquid assets before the bankruptcies are conducted
			double liquidAssets=0;
			List<Item> deposits=this.getItemsStockMatrix(true, StaticValues.SM_DEP);
			List<Item> cash=this.getItemsStockMatrix(true, StaticValues.SM_CASH);
			List<Item> reserves=this.getItemsStockMatrix(true, StaticValues.SM_RESERVES);
			for (Item i: deposits){
				liquidAssets+=i.getValue();
			}
			for (Item i: cash){
				liquidAssets+=i.getValue();
			}
			for (Item i: reserves){
				liquidAssets+=i.getValue();
			}
			setLiquidAssetsSum(liquidAssets);
			break;
		}
	}

	/**
	 * 
	 */
	private void computeWage() {
		WageStrategy strategy= (WageStrategy)this.getStrategy(StaticValues.STRATEGY_WAGE);
		this.wage=strategy.computeWage();
		if(this.employer==null){
			this.setActive(true, StaticValues.MKT_LABOR);
		}
	}

	/**
	 * 
	 */
	private void updateExpectations() {
		double[] price=new double[1];
		price[0]=0;
		List<Item> cons = this.getItemsStockMatrix(true, StaticValues.SM_CONSGOOD);
		int qty=0;
		for(Item item:cons){
			ConsumptionGood c = (ConsumptionGood)item;
			price[0]+=c.getPrice()*c.getQuantity();
			qty+=c.getQuantity();
		}
		if (qty!=0){
		price[0]=price[0]/qty;}
		else{
			price[0]=this.getExpectation(StaticValues.EXPECTATIONS_CONSPRICE).getExpectation();
		}
		this.getExpectation(StaticValues.EXPECTATIONS_CONSPRICE).addObservation(price);
		double nW=this.getNetWealth();
		this.addValue(StaticValues.LAG_NETWEALTH,nW);
		double employed;
		if(this.employer==null)
			employed=0;
		else
			employed=1;
		this.addValue(StaticValues.LAG_EMPLOYED,employed);
		this.cleanSM();
	}

	/**
	 * 
	 */
	private void computeLiquidAssetsAmounts() {
		double liquidAssets=0;
		List<Item> deposits=this.getItemsStockMatrix(true, StaticValues.SM_DEP);
		List<Item> cash=this.getItemsStockMatrix(true, StaticValues.SM_CASH);
		List<Item> reserves=this.getItemsStockMatrix(true, StaticValues.SM_RESERVES);
		for (Item i: deposits){
			liquidAssets+=i.getValue();
		}
		for (Item i: cash){
			liquidAssets+=i.getValue();
		}
		for (Item i: reserves){
			liquidAssets+=i.getValue();
		}
		this.setDepositAmount(this.preferredDepositRatio*liquidAssets);
		this.setCashAmount(this.preferredCashRatio*liquidAssets);
		this.setReservesAmount(this.preferredReserveRatio*liquidAssets);
		this.setActive(true, StaticValues.MKT_DEPOSIT);
	}
	
	public void reallocateLiquidity(double amount, List<Item> payingStocks, Item targetStock){
		BigDecimal amountBD = BigDecimal.valueOf(amount);
		//The amount raised is equal to what is already on the target stock
		BigDecimal amountRaised=BigDecimal.valueOf(targetStock.getValue());
		for(int i=0;i<payingStocks.size()&&amountRaised.compareTo(amountBD) ==-1;i++){
			//For each item in the list
			Item payingStock = payingStocks.get(i);
			//If the payingStock is not the target stock (otherwise, there's nothing to do).
			if(payingStock!=targetStock){
				//compute different amounts
				BigDecimal thisAmount=BigDecimal.valueOf(payingStock.getValue());
				BigDecimal remAmount=thisAmount.add(amountRaised).subtract(amountBD).max(new BigDecimal("0"));
				BigDecimal transferAmount=thisAmount.subtract(remAmount);
				amountRaised = amountRaised.add(transferAmount);
				//who is the supplier of the paying stock?
				LiabilitySupplier payingSupplier = (LiabilitySupplier) payingStock.getLiabilityHolder();
				// Do the transfer
				payingSupplier.transfer(payingStock, targetStock, transferAmount.doubleValue());
			}
		}
	}

	/**
	 * 
	 */
	private void computeConsumptionDemand() {
		this.updateIncome();
		ConsumptionStrategy strategy = (ConsumptionStrategy)this.getStrategy(StaticValues.STRATEGY_CONSUMPTION);
		this.setDemand(strategy.computeRealConsumptionDemand(), StaticValues.MKT_CONSGOOD);
		this.addValue(StaticValues.LAG_CONSUMPTION, this.demand);
		if (this.demand>0){
			this.setActive(true, StaticValues.MKT_CONSGOOD);
		}
	}

	/* (non-Javadoc)
	 * @see jmab.agents.GoodDemander#getPayingStocks(int, jmab.goods.Item)
	 */
	@Override
	public List<Item> getPayingStocks(int idGood, Item payableStock) {
		List<Item> result = new ArrayList<Item>();
		if(payableStock instanceof Deposit){
			if(payableStock.getLiabilityHolder().getPopulationId()==StaticValues.BANKS_ID) {
				result.addAll(this.getItemsStockMatrix(true, StaticValues.SM_DEP));
				result.addAll(this.getItemsStockMatrix(true, StaticValues.SM_CASH));
				result.addAll(this.getItemsStockMatrix(true, StaticValues.SM_RESERVES));
			}else {
				result.addAll(this.getItemsStockMatrix(true, StaticValues.SM_RESERVES));
				result.addAll(this.getItemsStockMatrix(true, StaticValues.SM_DEP));
				result.addAll(this.getItemsStockMatrix(true, StaticValues.SM_CASH));
			}		
		}else{
			result.addAll(this.getItemsStockMatrix(true, StaticValues.SM_CASH));
			result.addAll(this.getItemsStockMatrix(true, StaticValues.SM_DEP));
			result.addAll(this.getItemsStockMatrix(true, StaticValues.SM_RESERVES));
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.GoodDemander#getDemand()
	 */
	@Override
	public double getDemand(int idMarket) {
		return demand;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.GoodDemander#setDemand(double)
	 */
	@Override
	public void setDemand(double demand, int idMarket) {
		this.demand=demand;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.TaxPayer#payTaxes(jmab.goods.Item)
	 */
	@Override
	public void payTaxes(Item account) {
		TaxPayerStrategy strategy = (TaxPayerStrategy)this.getStrategy(StaticValues.STRATEGY_TAXES);
		double taxes=strategy.computeTaxes();
		//Prepare the re-allocation of funds
		//1 Get the payable stock
		Item payableStock = this.getPayableStock(StaticValues.MKT_LABOR);
		//2 Get the paying stocks
		List<Item> payingStocks = this.getPayingStocks(0,payableStock);
		//3 Get the first occurrence of an item of the same sort than the payable stock within the paying stocks
		Item targetStock=null;
		for(Item item:payingStocks){
			if(item.getSMId()==payableStock.getSMId()){
				targetStock=item;
				break;
			}
		}
		// Reallocate
		reallocateLiquidity(taxes, payingStocks, targetStock);
		taxes=Math.min(taxes, targetStock.getValue());
		this.addValue(StaticValues.LAG_TAXES, taxes);
		LiabilitySupplier payingSupplier = (LiabilitySupplier) targetStock.getLiabilityHolder();
		payingSupplier.transfer(targetStock, account, taxes);
		double nW=this.getNetWealth();
		this.addValue(StaticValues.LAG_NETWEALTH, nW);
		this.dividendsReceived=0;
	}

	/**
	 * 
	 */
	private void updateIncome() {
		this.addValue(StaticValues.LAG_INCOME, this.getNetIncome());
	}

	/* (non-Javadoc)
	 * @see jmab.agents.AbstractHousehold#getIncome()
	 */
	@Override
	public double getNetIncome() {
			TaxPayerStrategy strategy = (TaxPayerStrategy)this.getStrategy(StaticValues.STRATEGY_TAXES);
			double taxes=strategy.computeTaxes();
			if(this.isEmployed()){
				double grossIncome = this.getGrossIncome();
				double netIncome= grossIncome-taxes;
				return netIncome;
			}
			else{
				double grossIncome = this.getUnemploymentBenefitAmount()+this.getGrossIncome();
				double netIncome= grossIncome-taxes;
				return netIncome;
			}
		//return this.getPassedValue(StaticValues.LAG_INCOME, 1);	
	}

	/* (non-Javadoc)
	 * @see jmab.agents.WageSetterWithTargets#getWageLowerBound()
	 */
	@Override
	public double getWageLowerBound() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.WageSetterWithTargets#getWageUpperBound()
	 */
	@Override
	public double getWageUpperBound() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.WageSetterWithTargets#getReferenceVariableForWage()
	 */
	@Override
	public double getMicroReferenceVariableForWage() {
			double averageUnemployment = 0;
			for(int i=1; i<=this.employmentWageLag;i++){
				if(this.getPassedValue(StaticValues.LAG_EMPLOYED, i)==0) averageUnemployment++;
			}
			return averageUnemployment/employmentWageLag;
	}

	/**
	 * @return the employmentWageLag
	 */
	public int getEmploymentWageLag() {
		return employmentWageLag;
	}

	/**
	 * @param employmentWageLag the employmentWageLag to set
	 */
	public void setEmploymentWageLag(int employmentWageLag) {
		this.employmentWageLag = employmentWageLag;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.WageSetterWithTargets#getMacroReferenceVariableForWage()
	 */
	@Override
	public double getMacroReferenceVariableForWage() {
		return this.getAggregateValue(StaticValues.LAG_AGGUNEMPLOYMENT, 1);
	}
	
	/**
	 * @return the employer
	 */
	public MacroAgent getEmployer() {
		return employer;
	}

	/**
	 * @param employer the employer to set
	 */
	public void setEmployer(MacroAgent employer) {
		this.employer = employer;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.DepositDemander#interestPaid(double)
	 */
	@Override
	public void interestPaid(double interests) {
		this.interestsReceived=interests;
		
	}
	
	public double getInterestReceived(){
		return this.interestsReceived;
	}
	
	@Override
	public double getGrossIncome(){
		return super.getGrossIncome()+this.interestsReceived+this.dividendsReceived+this.reservesInterestsReceived;
	}
	
	/**
	 * Populates the agent characteristics using the byte array content. The structure is as follows:
	 * [sizeMacroAgentStructure][MacroAgentStructure][demand][cashAmount][depositAmount][shareDeposits][interestReceived][dividendsReceived]
	 * [employmentWageLag][matrixSize][stockMatrixStructure][expSize][ExpectationStructure][passedValSize][PassedValStructure]
	 * [stratsSize][StrategiesStructure]
	 */
	@Override
	public void populateAgent(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		byte[] macroBytes = new byte[buf.getInt()];
		buf.get(macroBytes);
		super.populateCharacteristics(macroBytes, pop);
		demand = buf.getDouble();
		cashAmount = buf.getDouble();
		depositAmount = buf.getDouble();
		shareDeposits = buf.getDouble();
		dividendsReceived = buf.getDouble();
		employmentWageLag = buf.getInt();
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
	 * Generates the byte array containing all relevant informations regarding the household agent. The structure is as follows:
	 * [sizeMacroAgentStructure][MacroAgentStructure][demand][cashAmount][depositAmount][shareDeposits][interestReceived][dividendsReceived]
	 * [employmentWageLag][matrixSize][stockMatrixStructure][expSize][ExpectationStructure][passedValSize][PassedValStructure]
	 * [stratsSize][StrategiesStructure]
	 */
	@Override
	public byte[] getBytes() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			byte[] charBytes = super.getAgentCharacteristicsBytes();
			out.write(ByteBuffer.allocate(4).putInt(charBytes.length).array());
			out.write(charBytes);
			ByteBuffer buf = ByteBuffer.allocate(44);
			buf.putDouble(demand);
			buf.putDouble(cashAmount);
			buf.putDouble(depositAmount);
			buf.putDouble(shareDeposits);
			buf.putDouble(dividendsReceived);
			buf.putInt(employmentWageLag);
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
				Item it;
				switch(stockId){
				case StaticValues.SM_CONSGOOD:
					it = new ConsumptionGood(itemData, pop, this);
					break;
				case StaticValues.SM_DEP:
					it = new Deposit(itemData, pop, this);
					MacroAgent depHolder = it.getLiabilityHolder();
					if(depHolder.getAgentId()!=this.agentId)
						depHolder.addItemStockMatrix(it, false, stockId);
					break;
				default:
					it = new Cash(itemData, pop, this);
					MacroAgent cashHolder = it.getLiabilityHolder();
					if(cashHolder.getAgentId()!=this.agentId)
						cashHolder.addItemStockMatrix(it, false, stockId);
					break;
				}
				this.addItemStockMatrix(it, true, stockId);
				
			}
		}	
		
	}

	@Override
	public void setLaborActive(boolean active) {
		this.setActive(active,StaticValues.MKT_LABOR);
		
	}

	@Override
	public void reservesInterestPaid(double interests) {
		// TODO Auto-generated method stub
		this.reservesInterestsReceived = interests;
	}
	
	public double getReservesInterestReceived(){
		return this.reservesInterestsReceived;
	}

	public long getBondsDemand(double price, BondSupplier issuer) {
		return bondDemand;
	}
	public long getBondsDemand(){
		return bondDemand;
	}

	@Override
	public void setBondInterestsReceived(double interests) {
		// TODO Auto-generated method stub
		
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
	 * Determines the demand for bonds
	 */
	private void determineBondDemand() {
		BondDemandStrategy strategy = (BondDemandStrategy)this.getStrategy(StaticValues.STRATEGY_BONDDEMAND);
		//this.bondDemand=strategy.bondDemand(this.selectedBondSupplier);
		//this.bondDemand = (int) (Math.round(this.getNetWealth()/10)/bondPrice);
		this.bondDemand = 0;
		if (this.bondDemand>0){
			this.setActive(true, StaticValues.MKT_BONDS);
		}
	}
	
	/**
	 * @return the bondPrice
	 */
	public double getBondPrice() {
		return bondPrice;
	}

	/**
	 * @param bondPrice the bondPrice to set
	 */
	public void setBondPrice(double bondPrice) {
		this.bondPrice = bondPrice;
	}
	
	/**
	 * @return the bondDemand
	 */
	public long getBondDemand() {
		return bondDemand;
	}

	/**
	 * @param bondDemand the bondDemand to set
	 */
	public void setBondDemand(int bondDemand) {
		this.bondDemand = bondDemand;
	}

	public double getUnemploymentBenefitAmount() {
		return unemploymentBenefitAmount;
	}

	public void setUnemploymentBenefitAmount(double unemploymentBenefitAmount) {
		this.unemploymentBenefitAmount = unemploymentBenefitAmount;
	}

	public double getBailoutcost() {
		return bailoutcost;
	}

	public void setBailoutcost(double bailoutcost) {
		this.bailoutcost = bailoutcost;
	}

	public double getLiquidAssetsSum() {
		return liquidAssetsSum;
	}

	public void setLiquidAssetsSum(double liquidAssetsSum) {
		this.liquidAssetsSum = liquidAssetsSum;
	}

}
