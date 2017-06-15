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

import jmab.agents.CreditDemander;
import jmab.agents.DepositDemander;
import jmab.agents.FinanceAgent;
import jmab.agents.GoodSupplier;
import jmab.agents.LaborSupplier;
import jmab.agents.LiabilitySupplier;
import jmab.agents.MacroAgent;
import jmab.agents.PriceSetterWithTargets;
import jmab.agents.ProfitsTaxPayer;
import jmab.events.MacroTicEvent;
import jmab.expectations.Expectation;
import jmab.population.MacroPopulation;
import jmab.simulations.MacroSimulation;
import jmab.stockmatrix.CapitalGood;
import jmab.stockmatrix.Cash;
import jmab.stockmatrix.Deposit;
import jmab.stockmatrix.Item;
import jmab.strategies.DividendsStrategy;
import jmab.strategies.FinanceStrategy;
import jmab.strategies.RandDOutcome;
import jmab.strategies.SelectDepositSupplierStrategy;
import jmab.strategies.SelectLenderStrategy;
import jmab.strategies.SelectWorkerStrategy;
import jmab.strategies.TargetExpectedInventoriesOutputStrategy;
import modelDistribution.StaticValues;
import modelDistribution.strategies.ProfitsWealthTaxStrategyWithDeficitManagement;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.agent.AgentList;
import net.sourceforge.jabm.event.AgentArrivalEvent;

/**
 * Class representing Capital Producers 
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class CapitalFirmWithHierarchy extends CapitalFirm implements GoodSupplier,
		CreditDemander, LaborDemanderDifferentiatedWorkers, DepositDemander, PriceSetterWithTargets, ProfitsTaxPayer, FinanceAgent {

	private double minWageDiscount;
	private double shareOfExpIncomeAsDeposit;
	protected ArrayList<MacroAgent> workers;
	protected ArrayList<MacroAgent> managers;
	protected ArrayList<MacroAgent> topManagers;
	protected ArrayList<MacroAgent> researchers;
	private double shareWorkers;
	private double shareManagers;
	private double shareTopManagers;
	private double shareResearchers;
	private int workersDemand;
	private int managersDemand;
	private int researchersDemand;
	private int topManagersDemand;
	private double scalingExpectations;




	/**
	 * 
	 */
	public CapitalFirmWithHierarchy() {
		super();
		this.workers=new ArrayList <MacroAgent>();
		this.managers=new ArrayList <MacroAgent>();
		this.researchers=new ArrayList <MacroAgent>();
		this.topManagers=new ArrayList <MacroAgent>();
	}
	
	/* (non-Javadoc)
	 * @see jmab.agents.SimpleAbstractAgent#onTicArrived(AgentTicEvent)
	 */
	@Override
	protected void onTicArrived(MacroTicEvent event) {

		switch(event.getTic()){
		case StaticValues.TIC_COMPUTEEXPECTATIONS:
			bailoutCost=0;
			this.defaulted=false;
			computeExpectations();
			determineOutput();
			break;
		case StaticValues.TIC_CAPITALPRICE:
			computePrice();
			break;
		case StaticValues.TIC_RDDECISION:
			researchDecision();
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
		case StaticValues.TIC_RDOUTCOME:
			researchOutcome();
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
				neededDiscount = deposit.getQuantity()/wageBill;
			}
			if(neededDiscount<this.minWageDiscount){
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
					HouseholdsDifferentiated worker = (HouseholdsDifferentiated) employee;
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
						gov.setTopManagersTotalDeposits(gov.getTopManagersTotalDeposits()+wealth);					}
				}
				deposit.setValue(0);
				System.out.println("Default "+ this.getAgentId() + " cap firm due to wages");
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
					double wage = employee.getWage();
					HouseholdsDifferentiated worker = (HouseholdsDifferentiated) employee;
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
	 * Computes labor demand. In this case, labor demand is equal to the quantity of workers needed to produced
	 * desired output plus quantity of workers corresponding the desired level of investment in R&D.
	 */
	@Override
	protected void computeLaborDemand() {
		//Expectation expectation = this.getExpectation(StaticValues.EXPECTATIONS_WAGES);
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
		int currentResearchers=this.researchers.size();
		int nbWorkers = this.getRequiredWorkers();
		//IF YOU CHANGE THE MATH.FLOOR OR MATH.ROUND OR MATH.CEIL REMEMBER TO DO THAT ALSO IN THE 
				// computeCreditDemand () methods.
		int nbManagers= (int) Math.round(nbWorkers*((shareManagers-shareResearchers)/shareWorkers));
		int nbTopManagers= (int) Math.round(nbWorkers*(shareTopManagers/shareWorkers));
		int nbResearchers= (int) Math.round(nbWorkers*(shareResearchers/shareWorkers));
		int nbEmployees=nbManagers+nbTopManagers+nbResearchers+nbWorkers;
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
		if(nbResearchers>currentResearchers){
			this.researchersDemand=nbResearchers-currentResearchers;
		}else{
			this.setActive(false, StaticValues.MKT_LABORRESEARCHERS);
			this.researchersDemand=0;
			emplPop = new AgentList();
			for(MacroAgent ag : this.researchers)
				emplPop.add(ag);
			emplPop.shuffle(prng);
			for(int i=0;i<currentResearchers-nbResearchers;i++){
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
		if(this.researchersDemand>0){
			this.setActive(true, StaticValues.MKT_LABORRESEARCHERS);
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
		ArrayList<MacroAgent> newResearchersList = new ArrayList<MacroAgent>();
		for(MacroAgent researcher:researchers){
			if(((LaborSupplier) researcher).getEmployer()!=null){
				newResearchersList.add(researcher);
			}
		}
		this.workers=newWorkersList;
		this.managers=newManagersList;
		this.topManagers=newTopManagersList;
		this.researchers=newResearchersList;
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
		else {
			this.researchersDemand-=1;
			this.researchers.add(worker);
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
		else if(marketID==StaticValues.MKT_LABORTOPMANAGERS){
			return this.topManagersDemand;
		}
		else{
			return this.researchersDemand;
		}
	}
	
	@Override
	protected void produce() {
		double outputQty=0;
		if(this.workers.size()>0){
			outputQty=Math.round(this.workers.size()*this.laborProductivity);
			CapitalGood inventories = (CapitalGood)this.getItemStockMatrix(true, this.getProductionStockId());
			inventories.setQuantity(inventories.getQuantity()+outputQty);
			if (outputQty>0){
			inventories.setUnitCost(this.getWageBill()/outputQty);
			}
		}
		this.addValue(StaticValues.LAG_PRODUCTION, outputQty);
		
	}

	/**
	 * Computes the amount of credit needed to finance production and R&D
	 */
	@Override
	protected void computeCreditDemand() {
		computeDebtPayments();
		FinanceStrategy strategy = (FinanceStrategy)this.getStrategy(StaticValues.STRATEGY_FINANCE);
		Expectation expectation = this.getExpectation(StaticValues.EXPECTATIONS_WAGES);
		Expectation expectation1 = this.getExpectation(StaticValues.EXPECTATIONS_NOMINALSALES);
		Expectation expectation2=this.getExpectation(StaticValues.EXPECTATIONS_REALSALES);
		double expRealSales=expectation2.getExpectation();
		CapitalGood inventories = (CapitalGood)this.getItemStockMatrix(true, StaticValues.SM_CAPGOOD); 
		double uc=inventories.getUnitCost();
		int inv = (int)inventories.getQuantity();
		int nbWorkers = this.getRequiredWorkers();
		//IF YOU CHANGE THE MATH.FLOOR OR MATH.ROUND OR MATH.CEIL REMEMBER TO DO THAT ALSO IN THE 
		// computeLaborDemand () method.
		int nbManagers= (int) Math.round(nbWorkers*((shareManagers-shareResearchers)/shareWorkers));
		int nbTopManagers= (int) Math.round(nbWorkers*(shareTopManagers/shareWorkers));
		int nbResearchers= (int) Math.round(nbWorkers*(shareResearchers/shareWorkers));
		int nbEmployees=nbManagers+nbTopManagers+nbResearchers+nbWorkers;
		double expWages = expectation.getExpectation();
		DividendsStrategy strategyDiv=(DividendsStrategy)this.getStrategy(StaticValues.STRATEGY_DIVIDENDS);
		TargetExpectedInventoriesOutputStrategy strategyProd= (TargetExpectedInventoriesOutputStrategy) this.getStrategy(StaticValues.STRATEGY_PRODUCTION);
		ProfitsWealthTaxStrategyWithDeficitManagement taxStrategy= (ProfitsWealthTaxStrategyWithDeficitManagement) this.getStrategy(StaticValues.STRATEGY_TAXES);
		double profitTaxRate=taxStrategy.getProfitTaxRate();
		double shareInvenstories=strategyProd.getInventoryShare();
		double profitShare=strategyDiv.getProfitShare();
		double expRevenues = expectation1.getExpectation();
		double expectedProfits=expRevenues-(nbEmployees*expWages)+this.interestReceived-this.debtInterests+(shareInvenstories*expRealSales-inv)*uc;
		double expectedTaxes=expectedProfits*profitTaxRate;
		double expectedDividends=expectedProfits*(1-profitTaxRate)*profitShare;
		double expectedFinancialRequirement=(nbEmployees*expWages)+(Math.floor(this.amountResearch/expWages))*expWages +
				this.debtBurden - this.interestReceived + expectedDividends+expectedTaxes-expRevenues+ this.shareOfExpIncomeAsDeposit*(nbEmployees*expWages);
		this.creditDemanded = strategy.computeCreditDemand(expectedFinancialRequirement);
		if(creditDemanded>0)
			this.setActive(true, StaticValues.MKT_CREDIT);
	}
	
	protected void researchDecision() {
	}
	
	protected void researchOutcome() {
		if(this.employees.size()>0){
			RandDOutcome strategy= (RandDOutcome) this.getStrategy(StaticValues.STRATEGY_RANDDEVELOPMENTOUTCOME);
			double productivityGain= strategy.computeRandDOutcome(researchers.size());
			this.setCapitalProductivity(this.getCapitalProductivity()*(1+productivityGain));
			updateInventoriesAfterInnovation(this.capitalProductivity);
		}
	}
	
	@Override
	public double getPriceLowerBound() {
		double overallEmployeesProductivity=this.getLaborProductivity()*shareWorkers;
		double expectedAverageVarCosts=this.getExpectation(StaticValues.EXPECTATIONS_WAGES).getExpectation()/overallEmployeesProductivity;
		expectedVariableCosts=expectedAverageVarCosts;
		return expectedAverageVarCosts;
	}
	
	
	@Override
	public void onAgentArrival(AgentArrivalEvent event) {
		MacroSimulation macroSim = (MacroSimulation)event.getSimulationController().getSimulation();
		int marketID=macroSim.getActiveMarket().getMarketId();
		switch(marketID){
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
			SelectWorkerStrategy strategyLaborManagers = (SelectWorkerStrategy) this.getStrategy(StaticValues.STRATEGY_LABOR);
			MacroAgent manager= (MacroAgent)strategyLaborManagers.selectWorker(event.getObjects());
			macroSim.getActiveMarket().commit(this, manager,marketID);
			break;
		case StaticValues.MKT_LABORTOPMANAGERS:
			SelectWorkerStrategy strategyLaborTopManagers = (SelectWorkerStrategy) this.getStrategy(StaticValues.STRATEGY_LABOR);
			MacroAgent topManager= (MacroAgent)strategyLaborTopManagers.selectWorker(event.getObjects());
			macroSim.getActiveMarket().commit(this, topManager,marketID);
			break;
		case StaticValues.MKT_LABORRESEARCHERS:
			SelectWorkerStrategy strategyLaborResearchers = (SelectWorkerStrategy) this.getStrategy(StaticValues.STRATEGY_LABOR);
			MacroAgent researcher= (MacroAgent)strategyLaborResearchers.selectWorker(event.getObjects());
			macroSim.getActiveMarket().commit(this, researcher,marketID);
			break;
		}
		
	}
	@Override
	protected void updateExpectations() {
		if (this.scalingExpectations==0.0){
			CapitalGood inventories = (CapitalGood)this.getItemStockMatrix(true, StaticValues.SM_CAPGOOD); 
			double nW=this.getNetWealth();
			this.addValue(StaticValues.LAG_NETWEALTH,nW);
			inventories.setAge(-1);
			this.cleanSM();
		}
		else{
			CapitalGood inventories = (CapitalGood)this.getItemStockMatrix(true, StaticValues.SM_CAPGOOD); 
			double nW=this.getNetWealth();
			this.addValue(StaticValues.LAG_NETWEALTH,nW);
			inventories.setAge(-1);
			this.cleanSM();
			//IF DEFAULTED SET EXPECTATIONS AND LAST PERIOD VALUES EQUAL TO THE SECTOR AVERAGES
			if (this.defaulted || this.employees.size()==0){
				double sectorExpRSales=0;
			    double sectorRSales=0;
			    double sectorExpNSales=0;
			    double sectorNSales=0;
			    double sectorExpWages=0;
			    double sectorWages=0;
			    double activeFirms=0;
			    double sectorOCF=0;
			    double averageCapitalProductivity=0;
			    SimulationController controller = (SimulationController)this.getScheduler();
				MacroPopulation macroPop = (MacroPopulation) controller.getPopulation();
				Population capitalFirms = macroPop.getPopulation(StaticValues.CAPITALFIRMS_ID);
				for (Agent f:capitalFirms.getAgents()){
	             	CapitalFirm firm= (CapitalFirm) f;
	             	if (!firm.isDefaulted()&&firm.getEmployees().size()>0){
	             	averageCapitalProductivity+=firm.getCapitalProductivity();
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
					averageCapitalProductivity=averageCapitalProductivity/activeFirms;
					if (this.getCapitalProductivity()<averageCapitalProductivity){
						this.setCapitalProductivity(averageCapitalProductivity);
					}
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
	 * @return the shareResearchers
	 */
	public double getShareResearchers() {
		return shareResearchers;
	}

	/**
	 * @param shareResearchers the shareResearchers to set
	 */
	public void setShareResearchers(double shareResearchers) {
		this.shareResearchers = shareResearchers;
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
	 * @return the researchersDemand
	 */
	public int getResearchersDemand() {
		return researchersDemand;
	}

	/**
	 * @param researchersDemand the researchersDemand to set
	 */
	public void setResearchersDemand(int researchersDemand) {
		this.researchersDemand = researchersDemand;
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
