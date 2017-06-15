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

import jmab.agents.MacroAgent;
import jmab.expectations.Expectation;
import jmab.population.MacroPopulation;
import jmab.stockmatrix.Deposit;
import jmab.stockmatrix.Item;
import jmab.stockmatrix.Loan;
import jmab.strategies.BankruptcyStrategy;
import modelDistribution.StaticValues;
import modelDistribution.agents.Bank;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.strategy.AbstractStrategy;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class BankBankruptcyBailoutDifferentiated extends AbstractStrategy implements
		BankruptcyStrategy {
	
//	private int numberBailouts; 
	private int depositId;
	private int depositExpectationId; 
	private int ownersID;
;

	/**
	 * @return the depositId
	 */
	public int getDepositId() {
		return depositId;
	}


	/**
	 * @param depositId the depositId to set
	 */
	public void setDepositId(int depositId) {
		this.depositId = depositId;
	}


	/**
	 * 
	 */
	public BankBankruptcyBailoutDifferentiated() {
		super();
//		this. numberBailouts=0;
	}


	/**
	 * @return the depositExpectationId
	 */
	public int getDepositExpectationId() {
		return depositExpectationId;
	}


	/**
	 * @param depositExpectationId the depositExpectationId to set
	 */
	public void setDepositExpectationId(int depositExpectationId) {
		this.depositExpectationId = depositExpectationId;
	}


	/**
	 * @return the owners
	 */
	public int getOwnersID() {
		return ownersID;
	}


	/**
	 * @param owners the owners to set
	 */
	public void setOwnersID(int owners) {
		this.ownersID = owners;
	}


	/* (non-Javadoc)
	 * @see jmab.strategies.BankruptcyStrategy#bankrupt()
	 */
	@Override
	public void bankrupt() {
		Bank bank = (Bank) getAgent();
		/*if (bank.getAgentId()==4228){
			System.out.println("Agent 4228");
		}*/
		Population banks = ((MacroPopulation)((SimulationController)this.scheduler).getPopulation()).getPopulation(StaticValues.BANKS_ID);
		double tot=0;
		double activeBanks=0;
		for (Agent b:banks.getAgents()){
			Bank bank1 = (Bank) b;
			List<Item> loans1=bank1.getItemsStockMatrix(true, StaticValues.SM_LOAN);
			double bank1OutstandingCredit=0;
			for(Item loan:loans1){
				bank1OutstandingCredit+=loan.getValue();
			}
			if (bank1.getAgentId()!=bank.getAgentId() && bank1OutstandingCredit>0 && bank1.getCapitalRatio()>0 && !bank.isDefaulted() && !Double.isInfinite(bank1.getCapitalRatio())) {
				tot+=bank1.getCapitalRatio();
				activeBanks+=1;
			}
		}
		double car=0.20;
		if (activeBanks>0 && (tot/(activeBanks))>0 && !Double.isInfinite((tot/(activeBanks)))){
/*		OLD VERSION MODIFIED 3/3/2016 Uniform distribution = new Uniform(0,0.1,prng);
		double car=tot/(banks.getSize()-1)+distribution.nextDouble();
*/		car=tot/(activeBanks);
		}
		else{
			car=0.20;
		}
		List<Item> loans=bank.getItemsStockMatrix(true, StaticValues.SM_LOAN);
		double loansValue=0;
		for (Item a:loans){
			Loan loan= (Loan)a;
			loansValue+=loan.getValue();
		}
		double targetNW=car*loansValue;
		double nw=bank.getNetWealth();
		bank.setBailoutCost(targetNW-nw);
		SimulationController controller = (SimulationController)bank.getScheduler();
		MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
		Population owners = macroPop.getPopulation(ownersID);
		double ownersDisbursement=targetNW-nw;
		double totalOwnersWealth=0;
		for(Agent h:owners.getAgents()){
			totalOwnersWealth+=((MacroAgent)h).getItemStockMatrix(true, StaticValues.SM_DEP).getValue();
		}
		for (Agent h:owners.getAgents()){
			MacroAgent owner = (MacroAgent) h;
			double ownerDeposit=owner.getItemStockMatrix(true, StaticValues.SM_DEP).getValue();
			double amountToPay=0;
			if (totalOwnersWealth>0 && totalOwnersWealth>ownersDisbursement){
			 amountToPay=ownersDisbursement*ownerDeposit/totalOwnersWealth;
			}
			else if (totalOwnersWealth>0 && totalOwnersWealth<=ownersDisbursement){
				amountToPay=ownerDeposit; ///totalOwnersWealth;
			}
			else if(totalOwnersWealth<=0){
				amountToPay=0;
			}
			Deposit depositOwner =(Deposit)owner.getItemStockMatrix(true, StaticValues.SM_DEP);
				if (depositOwner.getLiabilityHolder()==bank){
					depositOwner.setValue(depositOwner.getValue()-amountToPay);
				}
				else{
					depositOwner.setValue(depositOwner.getValue()-amountToPay);
					Item dBankReserve= (Item) depositOwner.getLiabilityHolder().getItemStockMatrix(true,StaticValues.SM_RESERVES);
					dBankReserve.setValue(dBankReserve.getValue()-amountToPay);
					Item defaultedBankReserves= (Item) bank.getItemStockMatrix(true, StaticValues.SM_RESERVES);
					defaultedBankReserves.setValue(defaultedBankReserves.getValue()+amountToPay);
				}
		}
		double newDepValue=0;
		for (Item deposit:bank.getItemsStockMatrix(false, depositId)){
			newDepValue+=deposit.getValue();
		}
		Expectation exp =bank.getExpectation(depositExpectationId);
		double[][] expData = exp.getPassedValues();
		for(int j = 0; j<expData.length; j++){
			expData[j][0]=newDepValue;
			expData[j][1]=newDepValue;
		}
		exp.setPassedValues(expData);
		System.out.println("bank "+ bank.getAgentId() +" defaulted");
		//System.out.println(numberBailouts);
		
	}
	

	/**
	 * Generate the byte array structure of the strategy. The structure is as follow:
	 * [depositId][depositExpectationId]
	 * @return the byte array content
	 */
	@Override
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(8);
		buf.putInt(this.depositId);
		buf.putInt(this.depositExpectationId);
		return buf.array();
	}


	/**
	 * Populates the strategy from the byte array content. The structure should be as follows:
	 * [depositId][depositExpectationId]
	 * @param content the byte array containing the structure of the strategy
	 * @param pop the Macro Population of agents
	 */
	@Override
	public void populateFromBytes(byte[] content, MacroPopulation pop) {
		ByteBuffer buf = ByteBuffer.wrap(content);
		this.depositId = buf.getInt();
		this.depositExpectationId = buf.getInt();
	}

}
