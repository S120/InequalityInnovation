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
package modelDistribution.agents;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import jmab.agents.CreditDemander;
import jmab.agents.DepositDemander;
import jmab.agents.FinanceAgent;
import jmab.agents.GoodDemander;
import jmab.agents.GoodSupplier;
import jmab.agents.InvestmentAgent;
import jmab.agents.LaborSupplier;
import jmab.agents.LiabilitySupplier;
import jmab.agents.MacroAgent;
import jmab.agents.PriceSetterWithTargets;
import jmab.agents.ProfitsTaxPayer;
import jmab.events.MacroTicEvent;
import jmab.expectations.Expectation;
import jmab.population.MacroPopulation;
import jmab.simulations.MacroSimulation;
import jmab.simulations.TwoStepMarketSimulation;
import jmab.stockmatrix.CapitalGood;
import jmab.stockmatrix.Cash;
import jmab.stockmatrix.ConsumptionGood;
import jmab.stockmatrix.Deposit;
import jmab.stockmatrix.Item;
import jmab.strategies.DividendsStrategy;
import jmab.strategies.FinanceStrategy;
import jmab.strategies.InvestmentStrategy;
import jmab.strategies.SelectDepositSupplierStrategy;
import jmab.strategies.SelectLenderStrategy;
import jmab.strategies.SelectSellerStrategy;
import jmab.strategies.SelectWorkerStrategy;
import jmab.strategies.TargetExpectedInventoriesOutputStrategy;
import modelDistribution.StaticValues;
import modelDistribution.strategies.BestQualityPriceCapitalSupplierWithSwitching;
import modelDistribution.strategies.ProfitsWealthTaxStrategyWithDeficitManagement;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.agent.AgentList;
import net.sourceforge.jabm.event.AgentArrivalEvent;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
/**
 * @author user
 *
 */
@SuppressWarnings("serial")
public class ConsumptionFirmWithHierarchyNew extends ConsumptionFirmWithHierarchy implements GoodSupplier, GoodDemander, CreditDemander, 
LaborDemanderDifferentiatedWorkers, DepositDemander, PriceSetterWithTargets, ProfitsTaxPayer, FinanceAgent, InvestmentAgent {



	private double minWageDiscount;
	private double shareOfExpIncomeAsDeposit;
	protected ArrayList<MacroAgent> workers;
	protected ArrayList<MacroAgent> managers;
	protected ArrayList<MacroAgent> topManagers;
	protected ArrayList<MacroAgent> researchers;
	private double shareWorkers;
	private double shareManagers;
	private double shareTopManagers;
	private int workersDemand;
	private int managersDemand;
	private int topManagersDemand;
	private double scalingExpectations;

	
	
	public ConsumptionFirmWithHierarchyNew() {
		super();
		this.workers=new ArrayList <MacroAgent>();
		this.managers=new ArrayList <MacroAgent>();
		this.researchers=new ArrayList <MacroAgent>();
		this.topManagers=new ArrayList <MacroAgent>();
	}

	@Override
	protected void onTicArrived(MacroTicEvent event) {
		switch(event.getTic()){
		case StaticValues.TIC_COMPUTEEXPECTATIONS:
			bailoutCost=0;
			this.defaulted=false;
			computeExpectations();
			determineOutput();
			break;
		case StaticValues.TIC_CONSUMPTIONPRICE:
			computePrice();
			break;
		case StaticValues.TIC_INVESTMENTDEMAND:
			InvestmentStrategy strategy1=(InvestmentStrategy) this.getStrategy(StaticValues.STRATEGY_INVESTMENT);
			this.desiredCapacityGrowth=strategy1.computeDesiredGrowth();
			SelectSellerStrategy buyingStrategy = (SelectSellerStrategy) this.getStrategy(StaticValues.STRATEGY_BUYING);
			computeDesiredInvestment(buyingStrategy.selectGoodSupplier(this.selectedCapitalGoodSuppliers, 0.0, true));
			break;
		case StaticValues.TIC_CREDITDEMAND:
			computeCreditDemand();
			break;
		case StaticValues.TIC_LABORDEMAND:
			computeLaborDemand();
			break;
		case StaticValues.TIC_PRODUCTION:
			produce();
			break;
		case StaticValues.TIC_WAGEPAYMENT:
			payWages();
			break;
		case StaticValues.TIC_CHECKBANKRUPTCIESAFTERWAGES:
			checkBankruptcyAfterWages();
			break;
		case StaticValues.TIC_CREDINTERESTS:
			payInterests();
			break;
		case StaticValues.TIC_DIVIDENDS:
			payDividends();
			break;
		case StaticValues.TIC_DEPOSITDEMAND:
			computeLiquidAssetsAmounts();
			break;
		case StaticValues.TIC_UPDATEEXPECTATIONS:
			updateExpectations();
		}
	}
	
	

	/**
	 * 
	 */
	private void payWages() {
		//If there are wages to pay
		/*if (this.getAgentId()==4128){
			System.out.println("Agent 4128");
		}*/
		MacroPopulation macroPop = (MacroPopulation) ((SimulationController)this.scheduler).getPopulation();
		GovernmentDifferentiatedWorkers gov = (GovernmentDifferentiatedWorkers)macroPop.getPopulation(StaticValues.GOVERNMENT_ID).getAgentList().get(0);
		if(employees.size()>0 && this.isDefaulted()==false){
			//1. Have only one deposit paying wages, reallocate wealth
			List<Item> deposits = this.getItemsStockMatrix(true, StaticValues.SM_DEP);
			Deposit deposit = (Deposit)deposits.get(0);
			if(deposits.size()==2){
				Item deposit2 = deposits.get(1);
				LiabilitySupplier supplier = (LiabilitySupplier) deposit2.getLiabilityHolder();
				supplier.transfer(deposit2, deposit, deposit2.getValue());
			}
			//2. If cash holdings
			Cash cash = (Cash) this.getItemStockMatrix(true, StaticValues.SM_CASH);
			if(cash.getValue()>0){
				LiabilitySupplier bank = (LiabilitySupplier)deposit.getLiabilityHolder();
				Item bankCash = bank.getCounterpartItem(deposit, cash);
				bankCash.setValue(bankCash.getValue()+cash.getValue());
				deposit.setValue(deposit.getValue()+cash.getValue());
				cash.setValue(0);
			}
			double wageBill = this.getWageBill();
			double neededDiscount = 1;
			if(wageBill>deposit.getQuantity()){
				//System.out.println("discount");
				neededDiscount = deposit.getQuantity()/wageBill;
			}
			if(neededDiscount<this.minWageDiscount){
				/*if (this.getAgentId()==4203){
					System.out.println("Agent 4203");
				}*/
				int currentWorkers = this.employees.size();
				AgentList emplPop = new AgentList();
				for(MacroAgent ag : this.employees)
					emplPop.add(ag);
				emplPop.shuffle(prng);
				for(int i=0;i<currentWorkers;i++){
					LaborSupplier employee = (LaborSupplier) emplPop.get(i);
					Item payableStock = employee.getPayableStock(StaticValues.MKT_LABOR);
					LiabilitySupplier payingSupplier = (LiabilitySupplier) deposit.getLiabilityHolder();
					double wagePaid=employee.getWage()*neededDiscount;
					payingSupplier.transfer(deposit, payableStock, wagePaid);
				    HouseholdsDifferentiated worker= (HouseholdsDifferentiated) employee;
				    worker.setWagesPaid(wagePaid);
				    double income = worker.getGrossIncome();
					//double wealth=worker.getNetWealth();
					double wealth=worker.getNumericBalanceSheet()[0][StaticValues.SM_DEP];
				    if (worker.getPopulationId()==StaticValues.WORKERS_ID){
				    	gov.setWorkersTotalIncome(gov.getWorkersTotalIncome()+income);
			        	gov.setWorkersTotalDeposits(gov.getWorkersTotalDeposits()+wealth);

				    }
					if (worker.getPopulationId()==StaticValues.MANAGERS_ID||worker.getPopulationId()==StaticValues.RESEARCHERS_ID){
						gov.setManagersResearchersTotalIncome(gov.getManagersResearchersTotalIncome()+income);
						gov.setManagersResearchersTotalDeposits(gov.getManagersResearchersTotalDeposits()+wealth);

					}
					if (worker.getPopulationId()==StaticValues.TOPMANAGERS_ID){
						gov.setTopManagersTotalIncome(gov.getTopManagersTotalIncome()+income);
			        	gov.setTopManagersTotalDeposits(gov.getTopManagersTotalDeposits()+wealth);
					}
				}
				deposit.setValue(0);
				System.out.println("Deafult "+ this.getAgentId() + " cons firm due to wages");
				this.defaulted=true;
				
			}else{
				//3. Pay wages
				int currentWorkers = this.employees.size();
				AgentList emplPop = new AgentList();
				for(MacroAgent ag : this.employees)
					emplPop.add(ag);
				emplPop.shuffle(prng);
				for(int i=0;i<currentWorkers;i++){
					LaborSupplier employee = (LaborSupplier) emplPop.get(i);
					HouseholdsDifferentiated worker = (HouseholdsDifferentiated) employee;
					double wage = employee.getWage();
					if(wage<deposit.getValue()){
						Item payableStock = employee.getPayableStock(StaticValues.MKT_LABOR);
						LiabilitySupplier payingSupplier = (LiabilitySupplier) deposit.getLiabilityHolder();
						payingSupplier.transfer(deposit, payableStock, wage*neededDiscount);
						worker.setWagesPaid(wage*neededDiscount);
						double income = worker.getGrossIncome();
						//double wealth=worker.getNetWealth();
						double wealth=worker.getNumericBalanceSheet()[0][StaticValues.SM_DEP];
					    if (worker.getPopulationId()==StaticValues.WORKERS_ID){
					    	gov.setWorkersTotalIncome(gov.getWorkersTotalIncome()+income);
				        	gov.setWorkersTotalDeposits(gov.getWorkersTotalDeposits()+wealth);

					    }
						if (worker.getPopulationId()==StaticValues.MANAGERS_ID||worker.getPopulationId()==StaticValues.RESEARCHERS_ID){
							gov.setManagersResearchersTotalIncome(gov.getManagersResearchersTotalIncome()+income);
							gov.setManagersResearchersTotalDeposits(gov.getManagersResearchersTotalDeposits()+wealth);

						}
						if (worker.getPopulationId()==StaticValues.TOPMANAGERS_ID){
							gov.setTopManagersTotalIncome(gov.getTopManagersTotalIncome()+income);
							gov.setTopManagersTotalDeposits(gov.getTopManagersTotalDeposits()+wealth);						}
					}
				}
			}
		}
		else if (this.isDefaulted()==true){
			int currentWorkers = this.employees.size();
			AgentList emplPop = new AgentList();
			for(MacroAgent ag : this.employees){
				emplPop.add(ag);
				}
			//emplPop.shuffle(prng);
			for(int i=0;i<currentWorkers;i++){
				LaborSupplier employee = (LaborSupplier) emplPop.get(i);
			    HouseholdsDifferentiated worker= (HouseholdsDifferentiated) employee;
			    worker.setWagesPaid(0);
			    double income = worker.getGrossIncome();
				//double wealth=worker.getNetWealth();
				double wealth=worker.getNumericBalanceSheet()[0][StaticValues.SM_DEP];
			    if (worker.getPopulationId()==StaticValues.WORKERS_ID){
			    	gov.setWorkersTotalIncome(gov.getWorkersTotalIncome()+income);
		        	gov.setWorkersTotalDeposits(gov.getWorkersTotalDeposits()+wealth);

			    }
				if (worker.getPopulationId()==StaticValues.MANAGERS_ID||worker.getPopulationId()==StaticValues.RESEARCHERS_ID){
					gov.setManagersResearchersTotalIncome(gov.getManagersResearchersTotalIncome()+income);
		        	gov.setManagersResearchersTotalDeposits(gov.getManagersResearchersTotalDeposits()+wealth);

				}
				if (worker.getPopulationId()==StaticValues.TOPMANAGERS_ID){
					gov.setTopManagersTotalIncome(gov.getTopManagersTotalIncome()+income);
					gov.setTopManagersTotalDeposits(gov.getTopManagersTotalDeposits()+wealth);				}
			}
		}
		
	}

	/**
	 * Compute the labor demand by the firm. First it determine the total amount of workers required to produce
	 * the desiredOutput through the method getRequiredWorkers: if smaller than the number of current employees the firm 
	 * fires the last it had hired, otherwise it hires new workers. 
	 */
	@Override
	protected void computeLaborDemand() {
		int currentEmployees = this.employees.size();
		AgentList emplPop = new AgentList();
		for(MacroAgent ag : this.employees)
			emplPop.add(ag);
		emplPop.shuffle(prng);
		for(int i=0;i<this.turnoverLabor*currentEmployees;i++){
			fireAgent((MacroAgent)emplPop.get(i));
		}
		cleanEmployeeList();
		currentEmployees = this.employees.size();
		int currentWorkers=this.workers.size();
		int currentManagers=this.managers.size();
		int currentTopManagers=this.topManagers.size();
		int nbWorkers = this.getRequiredWorkers();
		//IF YOU CHANGE THE MATH.FLOOR OR MATH.ROUND OR MATH.CEIL REMEMBER TO DO THAT ALSO IN THE 
		//getPriceLowerBound() and computeCreditDemand () methods.
		int nbManagers= (int) Math.round(nbWorkers*(shareManagers/shareWorkers));
		int nbTopManagers= (int) Math.round(nbWorkers*(shareTopManagers/shareWorkers));
		int nbEmployees=nbManagers+nbTopManagers+nbWorkers;
		if (nbEmployees>currentEmployees){
			this.laborDemand=nbEmployees-currentEmployees;
		}
		else{
			this.laborDemand=0;
		}
		
		ArrayList<MacroAgent> toFire = new ArrayList<MacroAgent>();
		if(nbWorkers>currentWorkers){
			this.workersDemand=nbWorkers-currentWorkers;
		}else{
			this.setActive(false, StaticValues.MKT_LABOR);
			this.workersDemand=0;
			emplPop = new AgentList();
			for(MacroAgent ag : this.workers)
				emplPop.add(ag);
			emplPop.shuffle(prng);
			for(int i=0;i<currentWorkers-nbWorkers;i++){
					toFire.add((MacroAgent)emplPop.get(i));
			}
		}
		if(nbManagers>currentManagers){
			this.managersDemand=nbManagers-currentManagers;
		}else{
			this.setActive(false, StaticValues.MKT_LABORMANAGERS);
			this.managersDemand=0;
			emplPop = new AgentList();
			for(MacroAgent ag : this.managers)
				emplPop.add(ag);
			emplPop.shuffle(prng);
			for(int i=0;i<currentManagers-nbManagers;i++){
					toFire.add((MacroAgent)emplPop.get(i));
			}
		}
		if(nbTopManagers>currentTopManagers){
			this.topManagersDemand=nbTopManagers-currentTopManagers;
		}else{
			this.setActive(false, StaticValues.MKT_LABORTOPMANAGERS);
			this.topManagersDemand=0;
			emplPop = new AgentList();
			for(MacroAgent ag : this.topManagers)
				emplPop.add(ag);
			emplPop.shuffle(prng);
			for(int i=0;i<currentTopManagers-nbTopManagers;i++){
					toFire.add((MacroAgent)emplPop.get(i));
			}
		}
		for (MacroAgent employee:toFire){
			fireAgent(employee);
		}
		cleanEmployeeList();
		if(this.workersDemand>0){
			this.setActive(true, StaticValues.MKT_LABOR);
		}
		if(this.managersDemand>0){
			this.setActive(true, StaticValues.MKT_LABORMANAGERS);
		}
		if(this.topManagersDemand>0){
			this.setActive(true, StaticValues.MKT_LABORTOPMANAGERS);
		}
	}
	
	@Override
	public void cleanEmployeeList(){
		ArrayList<MacroAgent> newEmployeesList = new ArrayList<MacroAgent>();
		for(MacroAgent employee:employees){
			if(((LaborSupplier) employee).getEmployer()!=null){
				newEmployeesList.add(employee);
			}
		}
		ArrayList<MacroAgent> newWorkersList = new ArrayList<MacroAgent>();
		for(MacroAgent worker:workers){
			if(((LaborSupplier) worker).getEmployer()!=null){
				newWorkersList.add(worker);
			}
		}
		ArrayList<MacroAgent> newManagersList = new ArrayList<MacroAgent>();
		for(MacroAgent manager:managers){
			if(((LaborSupplier) manager).getEmployer()!=null){
				newManagersList.add(manager);
			}
		}
		ArrayList<MacroAgent> newTopManagersList = new ArrayList<MacroAgent>();
		for(MacroAgent topManager:topManagers){
			if(((LaborSupplier) topManager).getEmployer()!=null){
				newTopManagersList.add(topManager);
			}
		}
		this.workers=newWorkersList;
		this.managers=newManagersList;
		this.topManagers=newTopManagersList;
		this.employees=newEmployeesList;
	}
	
	
	
	public void addEmployee(LaborSupplier worker, int marketId) {
		this.laborDemand-=1;
		this.employees.add(worker);
		if (marketId==StaticValues.MKT_LABOR){
			this.workersDemand-=1;
			this.workers.add(worker);
		}
		else if (marketId==StaticValues.MKT_LABORMANAGERS){
			this.managersDemand-=1;
			this.managers.add(worker);
		}
		else if (marketId==StaticValues.MKT_LABORTOPMANAGERS){
			this.topManagersDemand-=1;
			this.topManagers.add(worker);
		}
		//worker.setEmployer(this);
	}
	
	public int getLaborDemand(int marketID) {
		if (marketID==StaticValues.MKT_LABOR){
			return this.workersDemand;
		}
		else if(marketID==StaticValues.MKT_LABORMANAGERS){
			return this.managersDemand;
		}
		else {
			return this.topManagersDemand;
		}
	}
	
	
	protected void produce() {
		double outputQty=0;
		double capacity=0;
		double capValue=0;
		if(this.workers.size()>0){
			List<Item> currentCapitalStock = this.getItemsStockMatrix(true, StaticValues.SM_CAPGOOD);
			TreeMap<Double,ArrayList<CapitalGood>> orderedCapital = new TreeMap<Double,ArrayList<CapitalGood>>();
			double amortisationCosts=0;
			for (Item item:currentCapitalStock){
				CapitalGood capital=(CapitalGood)item;
				double prod = capital.getProductivity();
				capacity+=capital.getQuantity()*prod;
				capValue+=capital.getValue();
				amortisationCosts+=capital.getPrice()*capital.getQuantity()/capital.getCapitalAmortization();
				if(orderedCapital.containsKey(prod)){
					ArrayList<CapitalGood> list = orderedCapital.get(prod);
					list.add(capital);
				}else{
					ArrayList<CapitalGood> list = new ArrayList<CapitalGood>();
					list.add(capital);
					orderedCapital.put(prod, list);
				}

			}
			double residualWorkers=this.workers.size();
			for (Double key:orderedCapital.descendingKeySet()){
				for(CapitalGood capital:orderedCapital.get(key)){
					double employedWorkers = capital.getQuantity()/capital.getCapitalLaborRatio();
					if (employedWorkers<residualWorkers){
						outputQty+=capital.getProductivity()*capital.getQuantity();
						residualWorkers-=employedWorkers;
					}
					else{
						outputQty+=capital.getCapitalLaborRatio()*residualWorkers*capital.getProductivity();
						residualWorkers=0;
						//we are assuming that we can use a fraction of capital in the production process. Otherwise we 
						//have set outputQty+= Math.floor (capital.getCapitalLaborRatio()*residualWorkers)*capital.getProductivity();
					} 
				}
			}
			ConsumptionGood inventories = (ConsumptionGood)this.getItemStockMatrix(true, this.getProductionStockId());
			inventories.setQuantity(inventories.getQuantity()+outputQty);
			if (outputQty>0){
				inventories.setUnitCost((amortisationCosts+this.getWageBill())/outputQty);
				}
		}
		else{
			List<Item> currentCapitalStock = this.getItemsStockMatrix(true, StaticValues.SM_CAPGOOD);
//			TreeMap<Double,ArrayList<CapitalGood>> orderedCapital = new TreeMap<Double,ArrayList<CapitalGood>>();
			for (Item item:currentCapitalStock){
				CapitalGood capital=(CapitalGood)item;
				double prod = capital.getProductivity();
				capacity+=capital.getQuantity()*prod;
				capValue+=capital.getValue();
			}
		}
		ConsumptionGood inventories = (ConsumptionGood)this.getItemStockMatrix(true, this.getProductionStockId());
		if (inventories.getQuantity()>0){
			this.setActive(true, StaticValues.MKT_CONSGOOD);
		}
		else{
			this.setActive(false, StaticValues.MKT_CONSGOOD);
		}
		
		this.addValue(StaticValues.LAG_PRODUCTION, outputQty);
		this.addValue(StaticValues.LAG_CAPACITY, capacity);
		this.addValue(StaticValues.LAG_CAPITALFINANCIALVALUE,capValue);
	}

	@Override
	public double getPriceLowerBound() {
		double expectedAverageCosts=0;
		if(this.getDesiredOutput()>0){
			int nbWorkers = this.getRequiredWorkers();
			//IF YOU CHANGE THE MATH.FLOOR OR MATH.ROUND OR MATH.CEIL REMEMBER TO DO THAT ALSO IN THE 
			//comupreLabordemand() and computeCreditDemand () methods.
			int nbManagers= (int) Math.round(nbWorkers*(shareManagers/shareWorkers));
			int nbTopManagers= (int) Math.round(nbWorkers*(shareTopManagers/shareWorkers));
			int nbEmployees=nbManagers+nbTopManagers+nbWorkers;
			double expectedVariableCosts=this.getExpectation(StaticValues.EXPECTATIONS_WAGES).getExpectation()*nbEmployees;
		    if (Double.isNaN(expectedVariableCosts)){
		    	System.out.println("Error");
		    }
			expectedAverageCosts=(expectedVariableCosts)/this.getDesiredOutput();
			
		
		}else{
			//We compute how many workers were needed to produce to amount of inventories left as in the getRequiredWorkersMethod, but using the quantity of 
			//inventories instead of the desiredOutput
			//First we order capital vintages according to their productivity
			List<Item> currentCapitalStock = this.getItemsStockMatrix(true, StaticValues.SM_CAPGOOD);
			//if (currentCapitalStock.size()==0){
				//System.out.println("Error");
			//}
			TreeMap<Double,ArrayList<CapitalGood>> orderedCapital = new TreeMap<Double,ArrayList<CapitalGood>>();
			for (Item item:currentCapitalStock){
				CapitalGood capital=(CapitalGood)item;
				double prod = capital.getProductivity();
				if(orderedCapital.containsKey(prod)){
					ArrayList<CapitalGood> list = orderedCapital.get(prod);
					list.add(capital);
				}else{
					ArrayList<CapitalGood> list = new ArrayList<CapitalGood>();
					list.add(capital);
					orderedCapital.put(prod, list);
				}	
			}
			//Then we calculate the number of workers need to produce the quantity of inventories left
			//as they first employed the more productive vintages
			ConsumptionGood inventoriesLeft= (ConsumptionGood) this.getItemStockMatrix(true, StaticValues.SM_CONSGOOD);
			if (inventoriesLeft.getQuantity()==0){
				expectedAverageCosts=0;
			}
			else{
			double residualOutput=inventoriesLeft.getQuantity();
			double requiredWorkers=0;
			for (Double key:orderedCapital.descendingKeySet()){
				for(CapitalGood capital:orderedCapital.get(key)){	
					if (residualOutput>capital.getProductivity()*capital.getQuantity()){
						requiredWorkers+=capital.getQuantity()/capital.getCapitalLaborRatio();
						residualOutput-=capital.getProductivity()*capital.getQuantity();
					}
					else{
						double requiredCapital=residualOutput/capital.getProductivity();
						requiredWorkers+=requiredCapital/capital.getCapitalLaborRatio();
						residualOutput-=requiredCapital*capital.getProductivity();
					}
				}
			}
			//IF YOU CHANGE THE MATH.FLOOR OR MATH.ROUND OR MATH.CEIL REMEMBER TO DO THAT ALSO IN THE 
			// computeCreditDemand () and getPriceLowerBound() methods.
			int nbWorkers= (int) Math.round(requiredWorkers);
			int nbManagers= (int) Math.round(nbWorkers*(shareManagers/shareWorkers));
			int nbTopManagers= (int) Math.round(nbWorkers*(shareTopManagers/shareWorkers));
			int nbEmployeesRequired=nbManagers+nbTopManagers+nbWorkers;
			double expectedVariableCosts=this.getExpectation(StaticValues.EXPECTATIONS_WAGES).getExpectation()*nbEmployeesRequired;
			//if (Double.isNaN(expectedVariableCosts)){
				//System.out.println("Error");
			//}
			expectedAverageCosts=(expectedVariableCosts)/inventoriesLeft.getQuantity();
		}
		}
		expectedVariableCosts=expectedAverageCosts;
		
		return expectedAverageCosts;
		
	}
	
	
	/**
	 *
	 */
	@Override
	protected void computeCreditDemand() {
		this.computeDebtPayments();
		Expectation expectation1=this.getExpectation(StaticValues.EXPECTATIONS_NOMINALSALES);
		Expectation expectation2=this.getExpectation(StaticValues.EXPECTATIONS_REALSALES);
		double expRealSales=expectation2.getExpectation();
		ConsumptionGood inventories = (ConsumptionGood)this.getItemStockMatrix(true, StaticValues.SM_CONSGOOD); 
		double uc=inventories.getUnitCost();
		int inv = (int)inventories.getQuantity();
		double expRevenues=expectation1.getExpectation();
		int nbWorkers = this.getRequiredWorkers();
		//IF YOU CHANGE THE MATH.FLOOR OR MATH.ROUND OR MATH.CEIL REMEMBER TO DO THAT ALSO IN THE 
		//comupreLabordemand() and computeCreditDemand () methods.
		int nbManagers= (int) Math.round(nbWorkers*(shareManagers/shareWorkers));
		int nbTopManagers= (int) Math.round(nbWorkers*(shareTopManagers/shareWorkers));
		int nbEmployees=nbManagers+nbTopManagers+nbWorkers;
		Expectation expectation = this.getExpectation(StaticValues.EXPECTATIONS_WAGES);
		double expWages = expectation.getExpectation();
		DividendsStrategy strategyDiv=(DividendsStrategy)this.getStrategy(StaticValues.STRATEGY_DIVIDENDS);
		double profitShare=strategyDiv.getProfitShare();
		TargetExpectedInventoriesOutputStrategy strategyProd= (TargetExpectedInventoriesOutputStrategy) this.getStrategy(StaticValues.STRATEGY_PRODUCTION);
		ProfitsWealthTaxStrategyWithDeficitManagement taxStrategy= (ProfitsWealthTaxStrategyWithDeficitManagement) this.getStrategy(StaticValues.STRATEGY_TAXES);
		double profitTaxRate=taxStrategy.getProfitTaxRate();
		double shareInvenstories=strategyProd.getInventoryShare();
		List<Item> capStocks = this.getItemsStockMatrix(true, StaticValues.SM_CAPGOOD);
		double capitalAmortization = 0;
		for(Item c:capStocks){
			CapitalGood cap = (CapitalGood)c;
			if(cap.getAge()>=0 && cap.getAge()<cap.getCapitalAmortization())
				capitalAmortization+=cap.getQuantity()*cap.getPrice()/cap.getCapitalAmortization();
		}
		
		double expectedProfits=expRevenues-(nbEmployees*expWages)+this.interestReceived-this.debtInterests+(shareInvenstories*expRealSales-inv)*uc-capitalAmortization;
		double expectedTaxes=expectedProfits*profitTaxRate;
		double expectedDividends=expectedProfits*(1-profitTaxRate)*profitShare;
		double Inv=this.desiredRealCapitalDemand*((CapitalFirm)this.selectedCapitalGoodSuppliers.get(0)).getPrice();
		double totalFinancialRequirement=(nbEmployees*expWages)+
				Inv+
				this.debtBurden - this.interestReceived + expectedTaxes + expectedDividends-expRevenues+this.shareOfExpIncomeAsDeposit*(nbEmployees*expWages);
		FinanceStrategy strategy =(FinanceStrategy)this.getStrategy(StaticValues.STRATEGY_FINANCE);
		this.creditDemanded=strategy.computeCreditDemand(totalFinancialRequirement);
		if (Double.isNaN(creditDemanded)){
			System.out.println("Blah");
		}
		if(creditDemanded>0){
			this.setActive(true, StaticValues.MKT_CREDIT);
		}
	}
	
	
	@Override
	public void onAgentArrival(AgentArrivalEvent event) {
		MacroSimulation macroSim = (MacroSimulation)event.getSimulationController().getSimulation();
		int marketID=macroSim.getActiveMarket().getMarketId();
		switch(marketID){
		case StaticValues.MKT_CAPGOOD:
			TwoStepMarketSimulation sim = (TwoStepMarketSimulation)macroSim.getActiveMarket();
			if(sim.isFirstStep()){				
				this.selectedCapitalGoodSuppliers=event.getObjects();
			}else if(sim.isSecondStep()){
				//InvestmentStrategy strategy1=(InvestmentStrategy) this.getStrategy(StaticValues.STRATEGY_INVESTMENT);
				//this.desiredCapacityGrowth=strategy1.computeDesiredGrowth();
				int nbSellers = this.selectedCapitalGoodSuppliers.size()+1;//There are nbSellers+1 options for the firm to invest
				for(int i=0; i<nbSellers&&this.desiredRealCapitalDemand>0&&this.selectedCapitalGoodSuppliers.size()>0 && this.isActive(marketID);i++){
					/*if (i>0){
						System.out.println("Constrained K");
					}*/
					BestQualityPriceCapitalSupplierWithSwitching buyingStrategy = (BestQualityPriceCapitalSupplierWithSwitching) this.getStrategy(StaticValues.STRATEGY_BUYING);
					MacroAgent selSupplier;
					if (i==0){
						selSupplier = buyingStrategy.getPreviousSupplier();
					}
					else{
						selSupplier = buyingStrategy.selectGoodSupplier(this.selectedCapitalGoodSuppliers, 0.0, true);
					}
					computeDesiredInvestment(selSupplier);
					macroSim.getActiveMarket().commit(this, selSupplier,marketID);
					this.selectedCapitalGoodSuppliers.remove(selSupplier);
				}
				/*if (this.desiredRealCapitalDemand>0){
					System.out.println("Constrained K");
				}*/
			}
			break;
		case StaticValues.MKT_CREDIT:
			SelectLenderStrategy borrowingStrategy = (SelectLenderStrategy) this.getStrategy(StaticValues.STRATEGY_BORROWING);
			MacroAgent lender= (MacroAgent)borrowingStrategy.selectLender(event.getObjects(), this.getLoanRequirement(StaticValues.SM_LOAN), this.getLoanLength());
			macroSim.getActiveMarket().commit(this, lender,marketID);
			break;
		case StaticValues.MKT_DEPOSIT:
			SelectDepositSupplierStrategy depStrategy = (SelectDepositSupplierStrategy) this.getStrategy(StaticValues.STRATEGY_DEPOSIT);
			MacroAgent depSupplier= (MacroAgent)depStrategy.selectDepositSupplier(event.getObjects(), this.getDepositAmount());
			macroSim.getActiveMarket().commit(this, depSupplier,marketID);
			break;
		case StaticValues.MKT_LABOR:
			SelectWorkerStrategy strategyLabor = (SelectWorkerStrategy) this.getStrategy(StaticValues.STRATEGY_LABOR);
			MacroAgent worker= (MacroAgent)strategyLabor.selectWorker(event.getObjects());
			macroSim.getActiveMarket().commit(this, worker,marketID);
			break;
		case StaticValues.MKT_LABORMANAGERS:
			SelectWorkerStrategy strategyLaborManager = (SelectWorkerStrategy) this.getStrategy(StaticValues.STRATEGY_LABOR);
			MacroAgent manager= (MacroAgent)strategyLaborManager.selectWorker(event.getObjects());
			macroSim.getActiveMarket().commit(this, manager,marketID);
			break;
		case StaticValues.MKT_LABORTOPMANAGERS:
			SelectWorkerStrategy strategyLaborTopManager = (SelectWorkerStrategy) this.getStrategy(StaticValues.STRATEGY_LABOR);
			MacroAgent topManager= (MacroAgent)strategyLaborTopManager.selectWorker(event.getObjects());
			macroSim.getActiveMarket().commit(this, topManager,marketID);
			break;
			
		}
	}
	
	@Override
	protected void updateExpectations() {
		if (this.scalingExpectations==0.0){
			Item inventories =this.getItemStockMatrix(true, StaticValues.SM_CONSGOOD); 
			double nW=this.getNetWealth();
			this.addValue(StaticValues.LAG_NETWEALTH,nW);
			inventories.setAge(-2);
			this.cleanSM();
		}
		else{
			Item inventories =this.getItemStockMatrix(true, StaticValues.SM_CONSGOOD); 
			double nW=this.getNetWealth();
			this.addValue(StaticValues.LAG_NETWEALTH,nW);
			inventories.setAge(-2);
			this.cleanSM();
			//IF DEFAULTED SET EXPECTATIONS AND LAST PERIOD VALUES EQUAL TO THE SECTOR AVERAGES
			if (this.defaulted){
				double sectorExpRSales=0;
			    double sectorRSales=0;
			    double sectorExpNSales=0;
			    double sectorNSales=0;
			    double sectorExpWages=0;
			    double sectorWages=0;
			    double activeFirms=0;
			    double sectorOCF=0;
			    SimulationController controller = (SimulationController)this.getScheduler();
				MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
				Population consumptionFirms = macroPop.getPopulation(StaticValues.CONSUMPTIONFIRMS_ID);
				for (Agent f:consumptionFirms.getAgents()){
	             	ConsumptionFirm firm= (ConsumptionFirm) f;
	             	if (!firm.isDefaulted()&&firm.getEmployees().size()>0){
	             	sectorExpRSales+=firm.getExpectation(StaticValues.EXPECTATIONS_REALSALES).getExpectation();
	            	sectorRSales+=firm.getPassedValue(StaticValues.LAG_REALSALES, 0);
	            	sectorExpNSales+=firm.getExpectation(StaticValues.EXPECTATIONS_NOMINALSALES).getExpectation();    	
	            	sectorNSales+=firm.getPassedValue(StaticValues.LAG_NOMINALSALES, 0);
	            	sectorExpWages+=firm.getExpectation(StaticValues.EXPECTATIONS_WAGES).getExpectation();    	
	            	sectorWages+=firm.getWageBill()/(double) firm.getEmployees().size();
	            	sectorOCF+=firm.getPassedValue(StaticValues.LAG_OPERATINGCASHFLOW, 0);
	            	activeFirms+=1;
	             	}
				}
				if(Double.isNaN(sectorExpWages)||Double.isNaN(sectorExpNSales)||Double.isNaN(sectorExpRSales)||Double.isNaN(sectorRSales)||Double.isNaN(sectorNSales)||Double.isNaN(sectorWages)){
					System.out.println("Blah");
				}
				if (activeFirms>0){
					if (this.getExpectation(StaticValues.EXPECTATIONS_REALSALES).getExpectation()<(sectorRSales/activeFirms*this.scalingExpectations)||this.getExpectation(StaticValues.EXPECTATIONS_NOMINALSALES).getExpectation()<(sectorNSales/activeFirms*this.scalingExpectations)){
						this.getExpectation(StaticValues.EXPECTATIONS_WAGES).getPassedValues()[0][0]=sectorWages/activeFirms;
		             	this.getExpectation(StaticValues.EXPECTATIONS_WAGES).getPassedValues()[0][1]=sectorExpWages/activeFirms;
		             	this.getExpectation(StaticValues.EXPECTATIONS_REALSALES).getPassedValues()[0][0]=sectorRSales/activeFirms*this.scalingExpectations;
		             	this.getExpectation(StaticValues.EXPECTATIONS_REALSALES).getPassedValues()[0][1]=sectorExpRSales/activeFirms*this.scalingExpectations;
		             	this.getExpectation(StaticValues.EXPECTATIONS_NOMINALSALES).getPassedValues()[0][0]=sectorNSales/activeFirms*this.scalingExpectations;
		             	this.getExpectation(StaticValues.EXPECTATIONS_NOMINALSALES).getPassedValues()[0][1]=sectorExpNSales/activeFirms*this.scalingExpectations;
					    this.addValue(StaticValues.LAG_OPERATINGCASHFLOW,sectorOCF/activeFirms*this.scalingExpectations);
					}
				}
			}
		}
		
	}
	
	
	private void checkBankruptcyAfterWages() {
		if (this.defaulted){
			bankruptcy();
		}
		
	}
	
	

	/**
	 * @return the minWageDiscount
	 */
	public double getMinWageDiscount() {
		return minWageDiscount;
	}

	/**
	 * @param minWageDiscount the minWageDiscount to set
	 */
	public void setMinWageDiscount(double minWageDiscount) {
		this.minWageDiscount = minWageDiscount;
	}

	/**
	 * @return the shareOfExpIncomeAsDeposit
	 */
	public double getShareOfExpIncomeAsDeposit() {
		return shareOfExpIncomeAsDeposit;
	}

	/**
	 * @param shareOfExpIncomeAsDeposit the shareOfExpIncomeAsDeposit to set
	 */
	public void setShareOfExpIncomeAsDeposit(double shareOfExpIncomeAsDeposit) {
		this.shareOfExpIncomeAsDeposit = shareOfExpIncomeAsDeposit;
	}

	/**
	 * @return the workers
	 */
	public ArrayList<MacroAgent> getWorkers() {
		return workers;
	}

	/**
	 * @param workers the workers to set
	 */
	public void setWorkers(ArrayList<MacroAgent> workers) {
		this.workers = workers;
	}

	/**
	 * @return the managers
	 */
	public ArrayList<MacroAgent> getManagers() {
		return managers;
	}

	/**
	 * @param managers the managers to set
	 */
	public void setManagers(ArrayList<MacroAgent> managers) {
		this.managers = managers;
	}

	/**
	 * @return the topManagers
	 */
	public ArrayList<MacroAgent> getTopManagers() {
		return topManagers;
	}

	/**
	 * @param topManagers the topManagers to set
	 */
	public void setTopManagers(ArrayList<MacroAgent> topManagers) {
		this.topManagers = topManagers;
	}

	/**
	 * @return the researchers
	 */
	public ArrayList<MacroAgent> getResearchers() {
		return researchers;
	}

	/**
	 * @param researchers the researchers to set
	 */
	public void setResearchers(ArrayList<MacroAgent> researchers) {
		this.researchers = researchers;
	}

	/**
	 * @return the shareWorkers
	 */
	public double getShareWorkers() {
		return shareWorkers;
	}

	/**
	 * @param shareWorkers the shareWorkers to set
	 */
	public void setShareWorkers(double shareWorkers) {
		this.shareWorkers = shareWorkers;
	}

	/**
	 * @return the shareManagers
	 */
	public double getShareManagers() {
		return shareManagers;
	}

	/**
	 * @param shareManagers the shareManagers to set
	 */
	public void setShareManagers(double shareManagers) {
		this.shareManagers = shareManagers;
	}

	/**
	 * @return the shareTopManagers
	 */
	public double getShareTopManagers() {
		return shareTopManagers;
	}

	/**
	 * @param shareTopManagers the shareTopManagers to set
	 */
	public void setShareTopManagers(double shareTopManagers) {
		this.shareTopManagers = shareTopManagers;
	}

	/**
	 * @return the workersDemand
	 */
	public int getWorkersDemand() {
		return workersDemand;
	}

	/**
	 * @param workersDemand the workersDemand to set
	 */
	public void setWorkersDemand(int workersDemand) {
		this.workersDemand = workersDemand;
	}

	/**
	 * @return the managersDemand
	 */
	public int getManagersDemand() {
		return managersDemand;
	}

	/**
	 * @param managersDemand the managersDemand to set
	 */
	public void setManagersDemand(int managersDemand) {
		this.managersDemand = managersDemand;
	}

	/**
	 * @return the topManagersDemand
	 */
	public int getTopManagersDemand() {
		return topManagersDemand;
	}

	/**
	 * @param topManagersDemand the topManagersDemand to set
	 */
	public void setTopManagersDemand(int topManagersDemand) {
		this.topManagersDemand = topManagersDemand;
	}

	/**
	 * @return the scalingExpectations
	 */
	public double getScalingExpectations() {
		return scalingExpectations;
	}

	/**
	 * @param scalingExpectations the scalingExpectations to set
	 */
	public void setScalingExpectations(double scalingExpectations) {
		this.scalingExpectations = scalingExpectations;
	}
	
	
}
