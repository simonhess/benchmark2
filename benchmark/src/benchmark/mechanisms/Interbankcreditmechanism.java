/**
 * 
 */
package benchmark.mechanisms;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import benchmark.mixing.DynamicRandomRobinInterbankMixer;

import java.util.List;

import jmab.agents.CreditDemander;
import jmab.agents.CreditSupplier;
import jmab.agents.LiabilitySupplier;
import jmab.agents.MacroAgent;
import jmab.mechanisms.AbstractCreditMechanism;
import jmab.mechanisms.Mechanism;
import jmab.simulations.MarketSimulation;
import jmab.simulations.SimpleMarketSimulation;
import jmab.stockmatrix.Item;
import jmab.stockmatrix.Loan;

/**
 * @author Joeri Schasfoort
 * This class contains the methods required to realize a transaction in the interbankcredit market, once the matching mechanism and
 * the agents' strategies have defined the terms of the transaction (GoodSupplier, GoodDemander, quantity, price/interest, maturity etc.)
 */
public class Interbankcreditmechanism extends AbstractCreditMechanism implements
		Mechanism {

	public Interbankcreditmechanism() {} // TODO is this correctly empty?

	public Interbankcreditmechanism(MarketSimulation market) {
		super(market);
	}

	/**
	 * This methods executes the credit transaction and ensures the SF consistency:
	 * a)create the new object "InterbankLoan" and add it to the SM of the CreditDemander/CreditSupplier as a liability/asset
	 * b) transfer  reserves from the creditSupplier to the creditDemander
	 * The actual value of the loan is the minimum between asked and supplied.
	 */
	private void execute(CreditDemander creditDemander, CreditSupplier creditSupplier, int idMarket) {
		double required=creditDemander.getLoanRequirement(this.idLoansSM); 
		int length=creditDemander.decideLoanLength(this.idLoansSM);
		int amortization=creditDemander.decideLoanAmortizationType(this.idLoansSM);
		double totalLoansSupply=creditSupplier.getTotalLoansSupply(this.idLoansSM);
		double offered=creditSupplier.getLoanSupply(this.idLoansSM, creditDemander,required);
		double amount=Math.max(0, Math.min(required, Math.min(offered, totalLoansSupply)));
		if(amount>0){
			SimpleMarketSimulation interbankSim = (SimpleMarketSimulation) this.getMarketSimulation();
			DynamicRandomRobinInterbankMixer mixer = (DynamicRandomRobinInterbankMixer) interbankSim.getMixer();
			boolean supplySurplus = mixer.getSupplySurplus();
			double interestRate=0;
			// If there is supply surplus in the market take the interest rate of the supplier bank, take the interest rate of the demander bank otherwise
			Bank supplierBank = (Bank) creditSupplier;
			Bank demanderBank = (Bank) creditDemander;
			if(supplySurplus) interestRate = supplierBank.getInterestRate(this.idLoansSM, creditDemander, amount, length);
			else interestRate = demanderBank.getInterestRate(this.idLoansSM, creditDemander, amount, length);
			Loan loan = new Loan(amount, creditSupplier, creditDemander, interestRate, 0, amortization, length);
			// does this add an interbank loan as well? 
			creditSupplier.addItemStockMatrix(loan, true, this.idLoansSM);
			creditDemander.addItemStockMatrix(loan, false, this.idLoansSM);
			// let the central bank do a transfer of reserves use transfer function
			Item supplierReserves=creditSupplier.getItemStockMatrix(true, StaticValues.SM_RESERVES);
			Item DemanderReserves = creditDemander.getItemStockMatrix(true, StaticValues.SM_RESERVES);
			((LiabilitySupplier) supplierReserves.getLiabilityHolder()).transfer(supplierReserves, DemanderReserves, amount);
			// remove players from the market if their needs are satisfied
			creditDemander.setLoanRequirement(this.idLoansSM,required-amount);
			if(Math.min(required, amount)==required){
				creditDemander.setActive(false, idMarket);
			}
			creditSupplier.setTotalLoansSupply(this.idLoansSM,(creditSupplier.getTotalLoansSupply(this.idLoansSM)-amount));
			if (creditSupplier.getTotalLoansSupply(this.idLoansSM)==0){
				creditSupplier.setActive(false, idMarket);
			} 
		}
	}
	
	/* (non-Javadoc)
	 * This method makes a casting of the MacroAgent to CreditDemander and CreditSupplier and invokes the method
	 * "execute(CreditDemander CreditDemander, CreditSupplier CreditSupplier, int idMarket)".
	 */
	@Override
	public void execute(MacroAgent buyer, MacroAgent seller, int idMarket) {
		execute((CreditDemander) buyer, (CreditSupplier) seller, idMarket);

	}

	/* (non-Javadoc)
	 * @see jmab.mechanisms.Mechanism#execute(jmab.agents.MacroAgent, java.util.List, int)
	 */
	@Override
	public void execute(MacroAgent buyer, List<MacroAgent> seller, int idMarket) {
		// TODO Auto-generated method stub

	}

}
