/**
 * 
 */
package benchmark2.strategies;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import benchmark2.StaticValues;
import benchmark2.agents.Bank;
import jmab2.population.MacroPopulation;
import jmab2.stockmatrix.Item;
import net.sourceforge.jabm.EventScheduler;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Joeri Schasfoort
 *
 */
@SuppressWarnings("serial")
public class BankIlliquidityFixedShareHaircut extends AbstractStrategy implements
IlliquidityStrategy {

	private double haircut;
	private double loansFireSalePercentage;
	private int loanId;
	private int reserveId;

	public BankIlliquidityFixedShareHaircut() {
		super();
	}

	/**
	 * This method forces illiquid banks = 
	 * To: 
	 * 1. Select a strong counterparty (liquid bank) - multiple banks
	 * 2. Compute a haircut on some of their loans
	 * 3. Force a transaction haircut loans for assets
	 */
	@Override
	public void illiquid() {
		// cast the bank that is going into illiquidity as the bank
		Bank bank = (Bank) getAgent();
		// get the broader population of banks
		Population banks = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.BANKS_ID);
		// determine the most liquid bank
		Bank mostLiquidBank = bank;
		double mostLiquid = bank.getLiquidityRatio();
		for(Agent b:banks.getAgents()){
			Bank tempB = (Bank) b;
			if(mostLiquid<tempB.getLiquidityRatio()){
				mostLiquid=tempB.getLiquidityRatio();
				mostLiquidBank=tempB;
			}
		}
		// get the loan portfolio of the bank
		List<Item> loans = bank.getItemsStockMatrix(true, loanId);
		// sell a proportion of its longest loans
		// compute the loans to sell (percentage of portfolio, always youngest loans)
		int amount = (int)Math.floor(loans.size() * loansFireSalePercentage);
		// create empty list youngestLoans
		Map<Integer,Item> youngestLoans = new TreeMap<Integer,Item>();
		// loop getting the youngest loan
		for(Item loan:loans){
			// place each loan in array according to age
			youngestLoans.put(loan.getAge(),loan); 
		}
		// calculate value of loans and the haircut discount
		double youngestLoansValue = 0;
		Integer[] keys=(Integer[]) youngestLoans.keySet().toArray();
		// add and remove loans in liquid bank / this bank + calculate value
		for(int i=0;i<amount;i++){
			Integer key = keys[i];
			Item loan = youngestLoans.get(key);
			youngestLoansValue += loan.getValue();
			loan.setAssetHolder(mostLiquidBank);
			mostLiquidBank.addItemStockMatrix(loan, true, loanId);
			bank.removeItemStockMatrix(loan, true, loanId);
		}

		// apply haircut
		double haircutTotal = youngestLoansValue*haircut;
		// transfer reserves from liquid bank to current bank
		Item resbank = bank.getItemStockMatrix(true, reserveId); // or reserveId?
		resbank.setValue(resbank.getValue()+(youngestLoansValue-haircutTotal));
		Item resliquidbank = bank.getItemStockMatrix(true, reserveId);
		resliquidbank.setValue(resliquidbank.getValue()-(youngestLoansValue-haircutTotal));	
	}

	/**
	 * @param agent
	 */
	public BankIlliquidityFixedShareHaircut(Agent agent) {
		super(agent);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param scheduler
	 * @param agent
	 */
	public BankIlliquidityFixedShareHaircut(EventScheduler scheduler, Agent agent) {
		super(scheduler, agent);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see jmab2.strategies.SingleStrategy#getBytes()
	 */
	@Override
	public byte[] getBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see jmab2.strategies.SingleStrategy#populateFromBytes(byte[], jmab2.population.MacroPopulation)
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		// TODO Auto-generated method stub

	}

	public double getHaircut() {
		return haircut;
	}

	public void setHaircut(double haircut) {
		this.haircut = haircut;
	}

	public double getLoansFireSalePercentage() {
		return loansFireSalePercentage;
	}

	public void setLoansFireSalePercentage(double loansFireSalePercentage) {
		this.loansFireSalePercentage = loansFireSalePercentage;
	}

	public int getLoanId() {
		return loanId;
	}

	public void setLoanId(int loanId) {
		this.loanId = loanId;
	}

	public int getReserveId() {
		return reserveId;
	}

	public void setReserveId(int reserveId) {
		this.reserveId = reserveId;
	}



}
