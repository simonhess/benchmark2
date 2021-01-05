package benchmark.strategies;

import java.util.List;

import benchmark.agents.Bank;
import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import jmab.stockmatrix.Deposit;
import jmab.stockmatrix.Item;
import jmab.stockmatrix.Loan;
import jmab.strategies.InterestRateStrategy;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Joeri Schasfoort, Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class InterbankAsFunctionOfLoanRateStrategy extends AbstractStrategy implements
		InterestRateStrategy {

	private int loanId;
	private int reserveId;
	private int mktId;
	
	// 
	/* (non-Javadoc)
	 * @see jmab.strategies.InterestRateStrategy#computeInterestRate(jmab.agents.MacroAgent, double, int)
	 */
	@Override
	public double computeInterestRate(MacroAgent creditDemander, double amount,
			int length) {
		// take current loan rate, take current reservedepositRate. Divide the difference by 20
		// and add to reservedepositRate
		// cast your agent as creditsupplier
		Bank bank = (Bank)this.agent;
		List<Item> loans = bank.getItemsStockMatrix(true, loanId);
		double totalLoansInterest = 0;
		double totalMaturity = 0;
		double totalValue = 0;
		for(Item l:loans){
			Loan loan = (Loan)l;
			double mat=loan.getLength() - loan.getAge();
			double val=loan.getValue();
			totalLoansInterest += loan.getInterestRate() * val * mat;
			totalMaturity +=  mat * val *mat;
			totalValue += val * mat;
		}
		double averageLoanInterestRate = totalLoansInterest / totalValue;
		double averageLoanMaturity = totalMaturity/ totalValue;
		Deposit reserve = (Deposit)bank.getItemStockMatrix(true, reserveId);
		double riskFreeRate = reserve.getInterestRate();
		double totalRiskPremium = averageLoanInterestRate - riskFreeRate;
		double matRiskPremium = totalRiskPremium / averageLoanMaturity;
		return Math.min(Math.max(riskFreeRate+matRiskPremium*length, bank.getInterestRateLowerBound(mktId)),bank.getInterestRateUpperBound(mktId));

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

	public int getMktId() {
		return mktId;
	}

	public void setMktId(int mktId) {
		this.mktId = mktId;
	}
	
}
