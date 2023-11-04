package benchmark.strategies;

import java.util.ArrayList;
import java.util.Collections;

import benchmark.StaticValues;
import benchmark.agents.Bank;
import jmab2.agents.MacroAgent;
import jmab2.population.MacroPopulation;
import jmab2.simulations.MacroSimulation;
import jmab2.stockmatrix.Item;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

public class BankAssetValueShock extends AbstractStrategy implements ShockStrategy{

	private int targetBankIndex;
	
	public void performShock(){
		
		int round = ((MacroSimulation)((SimulationController)this.scheduler).getSimulation()).getRound();
		SimulationController controller = (SimulationController)this.getScheduler();
		MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
		Population bs = macroPop.getPopulation(StaticValues.BANKS_ID);

		ArrayList<Integer> bankIDsOrdered = new ArrayList<Integer>();
		
		for(Agent a : bs.getAgents()) {
			MacroAgent mA = (MacroAgent) a;
			bankIDsOrdered.add((int) mA.getAgentId());
		}
		Collections.sort(bankIDsOrdered);
		
		//System.out.println("targetBankIndex: "+targetBankIndex);
		
		int targetBankID = bankIDsOrdered.get(targetBankIndex);
		Bank targetBank = null;
				for(Agent a : bs.getAgents()) {
					MacroAgent mA = (MacroAgent) a;
					if(targetBankID==mA.getAgentId()) {
						targetBank=(Bank)mA;
					}
				}
		if(round%25==0) {
			double netWealth = targetBank.getNetWealth();
			double loansTotalValue = 0;
			for (Item i:targetBank.getItemsStockMatrix(true, StaticValues.SM_LOAN)){
				loansTotalValue+=i.getValue();
			}
		//	double shockFactor =1-netWealth*1.25/loansTotalValue;
			
			for (Item i:targetBank.getItemsStockMatrix(true, StaticValues.SM_LOAN)){
				i.setValue(i.getValue()*0.75);
			}
			
		this.targetBankIndex++;
		if(this.targetBankIndex==bs.size()) {
			this.targetBankIndex=0;
		}
			}
		
		
		
		
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



	public int getTargetBankIndex() {
		return targetBankIndex;
	}



	public void setTargetBankIndex(int targetBankIndex) {
		this.targetBankIndex = targetBankIndex;
	}

}
