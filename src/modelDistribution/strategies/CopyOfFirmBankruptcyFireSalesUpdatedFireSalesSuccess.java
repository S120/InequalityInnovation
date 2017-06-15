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
package modelDistribution.strategies;

import java.nio.ByteBuffer;
import java.util.List;

import jmab.agents.AbstractFirm;
import jmab.agents.CreditSupplier;
import jmab.agents.LiabilitySupplier;
import jmab.agents.MacroAgent;
import jmab.population.MacroPopulation;
import jmab.stockmatrix.Cash;
import jmab.stockmatrix.Deposit;
import jmab.stockmatrix.Item;
import jmab.stockmatrix.Loan;
import jmab.strategies.BankruptcyStrategy;
import modelDistribution.StaticValues;
import modelDistribution.agents.CapitalFirm;
import modelDistribution.agents.ConsumptionFirm;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class CopyOfFirmBankruptcyFireSalesUpdatedFireSalesSuccess extends AbstractStrategy implements
		BankruptcyStrategy {
	
	private double haircut;
	private int ownersID;
	private double scalingFactor;
	private boolean firesalesSuccess=true;
	
	
	/**
	 * @return the haircut
	 */
	public double getHaircut() {
		return haircut;
	}


	/**
	 * @param haircut the haircut to set
	 */
	public void setHaircut(double haircut) {
		this.haircut = haircut;
	}


	/**
	 * @return the ownersID
	 */
	public int getOwnersID() {
		return ownersID;
	}


	/**
	 * @param ownersID the ownersID to set
	 */
	public void setOwnersID(int ownersID) {
		this.ownersID = ownersID;
	}


	/**
	 * @return the scalingFactor
	 */
	public double getScalingFactor() {
		return scalingFactor;
	}


	/**
	 * @param scalingFactor the scalingFactor to set
	 */
	public void setScalingFactor(double scalingFactor) {
		this.scalingFactor = scalingFactor;
	}


	/**
	 * @return the firesalesSuccess
	 */
	public boolean isFiresalesSuccess() {
		return firesalesSuccess;
	}


	/**
	 * @param firesalesSuccess the firesalesSuccess to set
	 */
	public void setFiresalesSuccess(boolean firesalesSuccess) {
		this.firesalesSuccess = firesalesSuccess;
	}


	/* (non-Javadoc)
	 * @see jmab.strategies.BankruptcyStrategy#bankrupt()
	 */
	@Override
	public void bankrupt() {

		AbstractFirm firm = (AbstractFirm) this.agent;
		/*if (firm.getAgentId()==4128){
			System.out.println("Agent 4128");
		}*/

		//1. Move all money on one deposit if there were two of them
		List<Item> deposits = firm.getItemsStockMatrix(true, StaticValues.SM_DEP);
		Deposit deposit = (Deposit)deposits.get(0);
		if(deposits.size()==2){
			Item deposit2 = deposits.get(1);
			LiabilitySupplier supplier = (LiabilitySupplier) deposit2.getLiabilityHolder();
			supplier.transfer(deposit2, deposit, deposit2.getValue());
			deposit2.getLiabilityHolder().removeItemStockMatrix(deposit2, false, deposit2.getSMId());
		}

		//2. Transfer all cash on the deposit account
		Cash cash = (Cash)firm.getItemStockMatrix(true, StaticValues.SM_CASH);
		if(cash.getValue()>0){
			LiabilitySupplier bank = (LiabilitySupplier)deposit.getLiabilityHolder();
			Item bankCash = bank.getCounterpartItem(deposit, cash);
			bankCash.setValue(bankCash.getValue()+cash.getValue());
			deposit.setValue(deposit.getValue()+cash.getValue());
			cash.setValue(0);
		}

		//3. Compute total liquidity to be distributed to creditors
		double liquidity=deposit.getValue();
		if (Double.isNaN(liquidity)||Double.isInfinite(liquidity)){
			System.out.println("Blah");
		}

		//4. Compute each creditor's share of debt
		List<Item> loans=firm.getItemsStockMatrix(false, StaticValues.SM_LOAN);
		double[] debts = new double[loans.size()];
		double[] banksLosses = new double[loans.size()];
		double totalDebt=0;
		double totalBanksLoss=0;
		for(int i=0;i<loans.size();i++){
			Loan loan=(Loan)loans.get(i);
			debts[i]=loan.getValue();
			/*if (loan.getValue()<0){
				System.out.println("Loan<0");
			}*/
			if(Double.isNaN(debts[i])||Double.isInfinite(debts[i])){
				System.out.println("Blah");
			}
			banksLosses[i]=loan.getValue();
			if(Double.isNaN(banksLosses[i])||Double.isInfinite(banksLosses[i])){
				System.out.println("Blah");
			}
			totalDebt+=loan.getValue();
			totalBanksLoss+=loan.getValue();
		}
		
		if (totalDebt>0){
			//5. Distribute liquidity according to the share of debt of each creditor
			for(int i=0;i<loans.size();i++){
				Loan loan = (Loan) loans.get(i);
				double amountToPay=liquidity*(debts[i])/totalDebt;
				/*if (amountToPay<0){
					System.out.println("Loan<0");
				}*/
				if(Double.isNaN(amountToPay)||Double.isInfinite(amountToPay)){
					System.out.println("Blah");
				}
				if (liquidity>=totalDebt){
					amountToPay=debts[i];
				}
				//lendingBank.setCurrentNonPerformingLoans(lendingBank.getCurrentNonPerformingLoans()+(debts[i]-amountToPay)); 
				deposit.setValue(deposit.getValue()-amountToPay);
				if(loan.getAssetHolder()!=deposit.getLiabilityHolder()){
					Item lBankRes = loan.getAssetHolder().getItemStockMatrix(true,StaticValues.SM_RESERVES);
					lBankRes.setValue(lBankRes.getValue()+amountToPay);
					Item dBankRes = deposit.getLiabilityHolder().getItemStockMatrix(true, StaticValues.SM_RESERVES);
					dBankRes.setValue(dBankRes.getValue()-amountToPay);
				}
				if(Double.isNaN(loan.getValue()-amountToPay)||Double.isInfinite(loan.getValue()-amountToPay)){
					System.out.println("Blah");
				}
				loan.setValue(loan.getValue()-amountToPay);
				banksLosses[i]-=amountToPay;
				if (loan.getValue()<0||banksLosses[i]<0){
					System.out.println("NPL<0");
				}
				if(Double.isNaN(banksLosses[i])||Double.isInfinite(banksLosses[i])){
					System.out.println("Blah");
				}
				totalBanksLoss-=amountToPay;
				//loan.setValue(0);
			}
			deposit.setValue(0.0);
			
			//compute the value of capital to be sold
			if (firm instanceof ConsumptionFirm){
				double capitalValue=0;
				List<Item> capital=firm.getItemsStockMatrix(true, StaticValues.SM_CAPGOOD);
				for (Item i:capital){
					capitalValue+=i.getValue()*haircut;
				}
				double ownersDisbursment=0;
				if (capitalValue>totalBanksLoss){
					ownersDisbursment=totalBanksLoss;
				}
				else{
					ownersDisbursment=capitalValue;
				}
				if(Double.isNaN(ownersDisbursment)||Double.isNaN(capitalValue)||Double.isInfinite(ownersDisbursment)){
					System.out.println("Blah");
				}
				firm.setBailoutCost(ownersDisbursment);
				SimulationController controller = (SimulationController)firm.getScheduler();
				MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
				Population owners = macroPop.getPopulation(ownersID);
				double totalOwnersWealth=0;
				for(Agent h:owners.getAgents()){
					totalOwnersWealth+=((MacroAgent)h).getItemStockMatrix(true, StaticValues.SM_DEP).getValue();
				}
				if(Double.isNaN(totalOwnersWealth)||Double.isInfinite(totalOwnersWealth)){
					System.out.println("Blah");
				}
			    Population firms = macroPop.getPopulation(StaticValues.CONSUMPTIONFIRMS_ID);
                double newDeposits=0;
			    double sectorNW=0;
			    double sectorEmployees=0;
			    /*double [] sectorExpRSales=new double[1];
			    double [] sectorRSales=new double [1];
			    double [] sectorExpNSales=new double [1];
			    double [] sectorExpWages=new double [1];*/
                double activeFirms=0;
                for (Agent i:firms.getAgents()){
                	ConsumptionFirm f= (ConsumptionFirm) i;
                	if (!f.isDefaulted()){
	                	sectorNW+=f.getNetWealth();
	                	sectorEmployees+=f.getEmployees().size();
	                	activeFirms+=1;
	                	/*sectorExpRSales[0]+=f.getExpectation(StaticValues.EXPECTATIONS_REALSALES).getExpectation();
                		sectorRSales[0]+=f.getPassedValue(idValue, lag)*/
	                	if(Double.isNaN(sectorNW)||Double.isInfinite(sectorNW)){
	        				System.out.println("Blah");
	        			}
                	}              	
                }
                if (activeFirms>0 && !Double.isNaN(sectorNW/activeFirms*scalingFactor) && !Double.isInfinite(sectorNW/activeFirms*scalingFactor)){
            		double minNewDeposits=Math.round(sectorEmployees/activeFirms*scalingFactor)*firm.getExpectation(StaticValues.EXPECTATIONS_WAGES).getExpectation();
                	newDeposits=Math.max(minNewDeposits, (sectorNW/activeFirms*scalingFactor)-firm.getNetWealth());
                	//newDeposits=Math.max(0, (sectorNW/activeFirms*scalingFactor));
                }
                if(Double.isNaN(newDeposits)||Double.isInfinite(newDeposits)){
    				System.out.println("Blah");
    			}
                //               firm.getExpectation(StaticValues.EXPECTATIONS_REALSALES).addObservation(observation);;
                double[] banksLossesStatic = new double[loans.size()];
                for(int i=0;i<loans.size();i++){
                	banksLossesStatic[i]=banksLosses[i];
                }
                double totalBanksLossStatic=totalBanksLoss;
                for (Agent h:owners.getAgents()){
					MacroAgent owner = (MacroAgent) h;
					double ownerDeposit=owner.getItemStockMatrix(true, StaticValues.SM_DEP).getValue();
					for(int i=0;i<loans.size();i++){
						Loan loan = (Loan) loans.get(i);
						//each owner (household contribute according to his share of net-wealth, each creditor is refunded according to his share of credit.
						double amountToPay=0;
						if (totalBanksLossStatic>0 && ownersDisbursment<=totalOwnersWealth){
							amountToPay=ownersDisbursment*ownerDeposit/totalOwnersWealth*(banksLossesStatic[i])/totalBanksLossStatic;
							this.firesalesSuccess=true;
							if(Double.isNaN(amountToPay)||Double.isInfinite(amountToPay)){
				    				System.out.println("Blah");
				    			}
						}
						else if (totalBanksLossStatic>0 && ownersDisbursment>totalOwnersWealth && totalOwnersWealth>0){
							amountToPay=ownerDeposit*(banksLossesStatic[i])/totalBanksLossStatic;
							this.firesalesSuccess=false;
						}
						else if (totalOwnersWealth==0){
							amountToPay=0;
							this.firesalesSuccess=false;
							
						}
						CreditSupplier lendingBank= (CreditSupplier) loan.getAssetHolder();
						Deposit depositOwner =(Deposit)owner.getItemStockMatrix(true, StaticValues.SM_DEP);
						if (depositOwner.getLiabilityHolder()==lendingBank){
							depositOwner.setValue(depositOwner.getValue()-amountToPay);
							if(Double.isNaN(depositOwner.getValue()-amountToPay)||Double.isInfinite(depositOwner.getValue())){
			    				System.out.println("Blah");
			    			}
						}
						else{
							depositOwner.setValue(depositOwner.getValue()-amountToPay);
							if(Double.isNaN(depositOwner.getValue()-amountToPay)||Double.isInfinite (depositOwner.getValue()-amountToPay)){
			    				System.out.println("Blah");
			    			}
							Item dBankReserve= (Item) depositOwner.getLiabilityHolder().getItemStockMatrix(true,StaticValues.SM_RESERVES);
							dBankReserve.setValue(dBankReserve.getValue()-amountToPay);
							Item lendingBankReserves= (Item) lendingBank.getItemStockMatrix(true, StaticValues.SM_RESERVES);
							lendingBankReserves.setValue(lendingBankReserves.getValue()+amountToPay);
						}
						loan.setValue(loan.getValue()-amountToPay);
						if(Double.isNaN(loan.getValue()-amountToPay)||Double.isInfinite(loan.getValue()-amountToPay)){
							System.out.println("Blah");
						}
						banksLosses[i]-=amountToPay;
						if(Double.isNaN(banksLosses[i])||Double.isInfinite(banksLosses[i])){
							System.out.println("Blah");
						} 
						totalBanksLoss-=amountToPay;
						if(Double.isNaN(totalBanksLoss)||Double.isInfinite(totalBanksLoss)){
							System.out.println("Blah");
						}
					}
					if (newDeposits>0){
						Deposit depositOwner =(Deposit)owner.getItemStockMatrix(true, StaticValues.SM_DEP);
						LiabilitySupplier ownersBank= (LiabilitySupplier) depositOwner.getLiabilityHolder();
						double ownerTransfer=0;
						if (totalOwnersWealth>=newDeposits){
						ownerTransfer=newDeposits*ownerDeposit/totalOwnersWealth;
						}
						else {
							ownerTransfer=ownerDeposit;
						}
						ownersBank.transfer(depositOwner, deposit, Math.min(owner.getItemStockMatrix(true, StaticValues.SM_DEP).getValue(), ownerTransfer));
					}
				}
				
			} 
			
			if (firm instanceof CapitalFirm){
				SimulationController controller = (SimulationController)firm.getScheduler();
				MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
				Population owners = macroPop.getPopulation(ownersID);
				double totalOwnersWealth=0;
				for(Agent h:owners.getAgents()){
					totalOwnersWealth+=((MacroAgent)h).getItemStockMatrix(true, StaticValues.SM_DEP).getValue();
				}
				if(Double.isNaN(totalOwnersWealth)||Double.isInfinite(totalOwnersWealth)){
					System.out.println("Blah");
				}
			    Population firms = macroPop.getPopulation(StaticValues.CAPITALFIRMS_ID);
                double newDeposits=0;
			    double sectorNW=0;
			    double sectorEmployees=0;
			    //double averageCapitalProductivity=0;
                double activeFirms=0;
                for (Agent i:firms.getAgents()){
                	CapitalFirm f= (CapitalFirm) i;
                	if (!f.isDefaulted()){
	                	sectorNW+=f.getNetWealth();
	                	sectorEmployees+=f.getEmployees().size();
	                	//averageCapitalProductivity+=f.getCapitalProductivity();
	                	activeFirms+=1;
	                	if(Double.isNaN(sectorNW)||Double.isInfinite(sectorNW)){
	    					System.out.println("Blah");
	    				}
                	}              	
                }
                //averageCapitalProductivity=averageCapitalProductivity/activeFirms;
                //((CapitalFirm) firm).setCapitalProductivity(averageCapitalProductivity);
                if (activeFirms>0 && !Double.isNaN(sectorNW/activeFirms*scalingFactor) && !Double.isInfinite(sectorNW/activeFirms*scalingFactor)){
            		double minNewDeposits=Math.round(sectorEmployees/activeFirms*scalingFactor)*firm.getExpectation(StaticValues.EXPECTATIONS_WAGES).getExpectation();
                	newDeposits=Math.max(minNewDeposits, (sectorNW/activeFirms*scalingFactor)-firm.getNetWealth());
                }
                if(Double.isNaN(newDeposits)||Double.isInfinite(newDeposits)){
					System.out.println("Blah");
				}
                if (newDeposits>0){
	                for (Agent h:owners.getAgents()){
						MacroAgent owner = (MacroAgent) h;
						Deposit depositOwner =(Deposit)owner.getItemStockMatrix(true, StaticValues.SM_DEP);
						LiabilitySupplier ownersBank= (LiabilitySupplier) depositOwner.getLiabilityHolder();
						double ownerTransfer=0;
						if (totalOwnersWealth>=newDeposits){
						ownerTransfer=newDeposits*owner.getItemStockMatrix(true, StaticValues.SM_DEP).getValue()/totalOwnersWealth;
						}
						else{
							ownerTransfer=owner.getItemStockMatrix(true, StaticValues.SM_DEP).getValue();
						}
						ownersBank.transfer(depositOwner, deposit, Math.min(owner.getItemStockMatrix(true, StaticValues.SM_DEP).getValue(), ownerTransfer));
					}
                }
			}
			//all banks recover the same share of their outstanding credit as the total available funds are residualDeposits plus K
			//discounted value and this sum is distributed across loans on the base of their weight on total outstanding loans. 
			//Abstracting from residual deposits (which in most cases would be negligible) the share recovered would be Kvalue/totLoans
			for(int i=0;i<loans.size();i++){
				Loan loan = (Loan) loans.get(i);
				CreditSupplier lendingBank= (CreditSupplier) loan.getAssetHolder();
				if(Double.isNaN(banksLosses[i])||Double.isInfinite(banksLosses[i])){
					System.out.println("Blah");
				}
				lendingBank.setCurrentNonPerformingLoans(StaticValues.SM_LOAN,lendingBank.getCurrentNonPerformingLoans(StaticValues.SM_LOAN)+banksLosses[i]);
				/*if (lendingBank.getCurrentNonPerformingLoans(StaticValues.SM_LOAN)<0){
					System.out.println("NPL<0");
				}*/
				loan.setValue(0);
			} 
		}
		else{
			SimulationController controller = (SimulationController)firm.getScheduler();
			MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
			Population owners = macroPop.getPopulation(ownersID);
			double totalOwnersWealth=0;
			for(Agent h:owners.getAgents()){
				totalOwnersWealth+=((MacroAgent)h).getItemStockMatrix(true, StaticValues.SM_DEP).getValue();
			}
			if(Double.isNaN(totalOwnersWealth)||Double.isInfinite(totalOwnersWealth)){
				System.out.println("Blah");
			}
			Population firms=null;
			if (firm instanceof ConsumptionFirm){
			    firms = macroPop.getPopulation(StaticValues.CONSUMPTIONFIRMS_ID);
			}
			else if (firm instanceof CapitalFirm){
				firms = macroPop.getPopulation(StaticValues.CAPITALFIRMS_ID);
			}
		    double newDeposits=0;
		    double sectorNW=0;
		    double sectorEmployees=0;
            double activeFirms=0;
            for (Agent i:firms.getAgents()){
            	AbstractFirm f= (AbstractFirm) i;
            	if (!f.isDefaulted()){
                	sectorNW+=f.getNetWealth();
                	sectorEmployees+=f.getEmployees().size();
                	activeFirms+=1;
                	if(Double.isNaN(sectorNW)||Double.isInfinite(sectorNW)){
        				System.out.println("Blah");
        			}
            	}              	
            }
            if (activeFirms>0 && !Double.isNaN(sectorNW/activeFirms*scalingFactor) && !Double.isInfinite(sectorNW/activeFirms*scalingFactor)){
        		double minNewDeposits=Math.round(sectorEmployees/activeFirms*scalingFactor)*firm.getExpectation(StaticValues.EXPECTATIONS_WAGES).getExpectation();
            	newDeposits=Math.max(minNewDeposits, (sectorNW/activeFirms*scalingFactor)-firm.getNetWealth());
            }
            if(Double.isNaN(newDeposits)||Double.isInfinite(newDeposits)){
				System.out.println("Blah");
			}
			for (Agent h:owners.getAgents()){
				MacroAgent owner = (MacroAgent) h;
				double ownerDeposit=owner.getItemStockMatrix(true, StaticValues.SM_DEP).getValue();
				if (newDeposits>0){
					Deposit depositOwner =(Deposit)owner.getItemStockMatrix(true, StaticValues.SM_DEP);
					LiabilitySupplier ownersBank= (LiabilitySupplier) depositOwner.getLiabilityHolder();
					double ownerTransfer=newDeposits*ownerDeposit/totalOwnersWealth;
					ownersBank.transfer(depositOwner, deposit, Math.min(owner.getItemStockMatrix(true, StaticValues.SM_DEP).getValue(), ownerTransfer));
				}
			}	
		}
	}
	
	
	
	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [haircut]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(8);
		buf.putDouble(this.haircut);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [haircut]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.haircut = buf.getDouble();
	}
	

}
