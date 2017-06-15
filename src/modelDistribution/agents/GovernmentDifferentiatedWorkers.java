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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jmab.agents.BondSupplier;
import jmab.agents.LaborSupplier;
import jmab.agents.LiabilitySupplier;
import jmab.agents.MacroAgent;
import jmab.events.MacroTicEvent;
import jmab.population.MacroPopulation;
import jmab.simulations.MacroSimulation;
import jmab.stockmatrix.Deposit;
import jmab.stockmatrix.Item;
import jmab.strategies.SelectWorkerStrategy;
import modelDistribution.StaticValues;
import modelDistribution.strategies.DeficitManagementStrategy;
import modelDistribution.strategies.ProgressiveTaxRatesComputerStrategyWithDeficitManagement;
import net.sourceforge.jabm.Population;
import net.sourceforge.jabm.SimulationController;
import net.sourceforge.jabm.agent.Agent;
import net.sourceforge.jabm.agent.AgentList;
import net.sourceforge.jabm.event.AgentArrivalEvent;

/**
 * @author Alessandro Caiani and Antoine Godin
 * Note that the government uses a reserve account in the central bank rather than a deposit account due to
 * the bond market.
 */
/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
@SuppressWarnings("serial")
public class GovernmentDifferentiatedWorkers extends Government implements LaborDemanderDifferentiatedWorkers, BondSupplier{
	
	double unemploymentBenefit;
	protected double doleExpenditure;
	protected double profitsFromCB;
	protected ArrayList<MacroAgent> workers;
	protected ArrayList<MacroAgent> managers;
	protected ArrayList<MacroAgent> topManagers;
	protected ArrayList<MacroAgent> researchers;
	private double shareManagers;
	private double shareTopManagers;
	private int workersDemand;
	private int managersDemand;
	private int topManagersDemand;
	protected double workersTotalIncome;
	protected double managersResearchersTotalIncome;
	protected double topManagersTotalIncome;
	protected double workersTotalDeposits;
	protected double topManagersTotalDeposits;
	protected double managersResearchersTotalDeposits;
	
	private double workersIncomeTaxRate;
	private double workersWealthTaxRate;
	private double managersIncomeTaxRate;
	private double managersWealthTaxRate;
	private double topManagersIncomeTaxRate;
	private double topManagersWealthTaxRate;


	public GovernmentDifferentiatedWorkers() {
		super();
		this.workers=new ArrayList <MacroAgent>();
		this.managers=new ArrayList <MacroAgent>();
		this.researchers=new ArrayList <MacroAgent>();
		this.topManagers=new ArrayList <MacroAgent>();
	}

	/**
	 * @return the unemploymentBenefit
	 */
	public double getUnemploymentBenefit() {
		return unemploymentBenefit;
	}

	/**
	 * @param unemploymentBenefit the unemploymentBenefit to set
	 */
	public void setUnemploymentBenefit(double unemploymentBenefit) {
		this.unemploymentBenefit = unemploymentBenefit;
	}

	/* (non-Javadoc)
	 * @see jmab.agents.SimpleAbstractAgent#onTicArrived(AgentTicEvent)
	 */
	@Override
	protected void onTicArrived(MacroTicEvent event) {
		switch(event.getTic()){
		case StaticValues.TIC_GOVERNMENTLABOR:
			computeLaborDemand();
			break;
		case StaticValues.TIC_TAXES:
			double totalPreTaxIncome=workersTotalIncome+managersResearchersTotalIncome+topManagersTotalIncome;
			double totalPreTaxDeposits=workersTotalDeposits+managersResearchersTotalDeposits+topManagersTotalDeposits;
			this.setAggregateValue(StaticValues.LAG_WORKERSHAREINCOME, workersTotalIncome/totalPreTaxIncome);
			this.setAggregateValue(StaticValues.LAG_RESEARCHERSMANAGERSHAREINCOME, managersResearchersTotalIncome/totalPreTaxIncome);
			this.setAggregateValue(StaticValues.LAG_TOPMANAGERSHAREINCOME, topManagersTotalIncome/totalPreTaxIncome);
			this.setAggregateValue(StaticValues.LAG_WORKERSHAREWEALTH, workersTotalDeposits/totalPreTaxDeposits);
			this.setAggregateValue(StaticValues.LAG_RESEARCHERSMANAGERSHAREWEALTH, managersResearchersTotalDeposits/totalPreTaxDeposits);
			this.setAggregateValue(StaticValues.LAG_TOPMANAGERSHAREWEALTH, topManagersTotalDeposits/totalPreTaxDeposits);
			DeficitManagementStrategy defManagementStrategy= (DeficitManagementStrategy) this.getStrategy(StaticValues.STRATEGY_DEFICITMANAGEMENT);
			this.taxRatesMultiplyingFactor=defManagementStrategy.computeTaxRatesMultiplyingFactor();
			ProgressiveTaxRatesComputerStrategyWithDeficitManagement taxRateComputerStrategy= (ProgressiveTaxRatesComputerStrategyWithDeficitManagement) this.getStrategy(StaticValues.STRATEGY_TAXRATECOMPUTER);
			taxRateComputerStrategy.computeTaxRates();
			collectTaxes(event.getSimulationController());
			this.addValue(StaticValues.LAG_WORKERSTAXRATEINCOME, workersIncomeTaxRate);
        	this.addValue(StaticValues.LAG_WORKERSTAXRATEWEALTH, workersWealthTaxRate);
        	this.addValue(StaticValues.LAG_MANAGERSRESEARCHERSTAXRATEINCOME, managersIncomeTaxRate);
        	this.addValue(StaticValues.LAG_MANAGERSRESEARCHERSTAXRATEWEALTH, managersWealthTaxRate);
        	this.addValue(StaticValues.LAG_TOPMANAGERSTAXRATEINCOME, topManagersIncomeTaxRate);
        	this.addValue(StaticValues.LAG_TOPMANAGERSTAXRATEWEALTH, topManagersWealthTaxRate);
			break;
		case StaticValues.TIC_BONDINTERESTS:
			payInterests();
			break;
		case StaticValues.TIC_BONDSUPPLY:
			receiveCBProfits();
			determineBondsInterestRate();
			emitBonds();
			break;
		case StaticValues.TIC_WAGEPAYMENT:
			payWages();
			payUnemploymentBenefits(event.getSimulationController());
			break;
		case StaticValues.TIC_UPDATEEXPECTATIONS:
			this.updateAggregateVariables();
			break;
		}
	}

	/**
	 * 
	 */
	private void receiveCBProfits() {
		Item deposit=this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
		CentralBank cb=(CentralBank) deposit.getLiabilityHolder();
		deposit.setValue(deposit.getValue()+cb.getCBProfits());
		profitsFromCB=cb.getCBProfits();
	}

	/**
	 * 
	 */
	private void payUnemploymentBenefits(SimulationController simulationController) {
		MacroPopulation macroPop = (MacroPopulation) simulationController.getPopulation();
		Population workersPop= (Population) macroPop.getPopulation(StaticValues.WORKERS_ID);
		MacroPopulation macroPop1 = (MacroPopulation) simulationController.getPopulation();
		Population topManagersPop= (Population) macroPop1.getPopulation(StaticValues.TOPMANAGERS_ID);
		MacroPopulation macroPop2 = (MacroPopulation) simulationController.getPopulation();
		Population managersPop= (Population) macroPop2.getPopulation(StaticValues.MANAGERS_ID);
		MacroPopulation macroPop3 = (MacroPopulation) simulationController.getPopulation();
		Population researchersPop= (Population) macroPop3.getPopulation(StaticValues.RESEARCHERS_ID);
		
		double averageWorkersWage=0;
		double employed=0;
		for(Agent agent:workersPop.getAgents()){
			HouseholdsDifferentiated worker= (HouseholdsDifferentiated) agent;
			if (worker.getEmployer()!=null){
				averageWorkersWage+=worker.getWage();
				employed+=1;
			}
		}
		averageWorkersWage=averageWorkersWage/employed;
		double unemploymentBenefit=averageWorkersWage*this.unemploymentBenefit;
		double doleAmount=0;
		for(Agent agent:workersPop.getAgents()){
			HouseholdsDifferentiated worker= (HouseholdsDifferentiated) agent;
			if (worker.getEmployer()==null){
				LaborSupplier unemployed = (LaborSupplier) worker;
				Deposit depositGov = (Deposit) this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
				Item payableStock = unemployed.getPayableStock(StaticValues.MKT_LABOR);
				LiabilitySupplier payingSupplier = (LiabilitySupplier) depositGov.getLiabilityHolder();
				payingSupplier.transfer(depositGov, payableStock, unemploymentBenefit);
				doleAmount+=unemploymentBenefit;
			}
		}
		for(Agent agent:managersPop.getAgents()){
			HouseholdsDifferentiated manager= (HouseholdsDifferentiated) agent;
			if (manager.getEmployer()==null){
				LaborSupplier unemployed = (LaborSupplier) manager;
				Deposit depositGov = (Deposit) this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
				Item payableStock = unemployed.getPayableStock(StaticValues.MKT_LABOR);
				LiabilitySupplier payingSupplier = (LiabilitySupplier) depositGov.getLiabilityHolder();
				payingSupplier.transfer(depositGov, payableStock, unemploymentBenefit);
				doleAmount+=unemploymentBenefit;
			}
		}
		for(Agent agent:topManagersPop.getAgents()){
			HouseholdsDifferentiated topManager= (HouseholdsDifferentiated) agent;
			if (topManager.getEmployer()==null){
				LaborSupplier unemployed = (LaborSupplier) topManager;
				Deposit depositGov = (Deposit) this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
				Item payableStock = unemployed.getPayableStock(StaticValues.MKT_LABOR);
				LiabilitySupplier payingSupplier = (LiabilitySupplier) depositGov.getLiabilityHolder();
				payingSupplier.transfer(depositGov, payableStock, unemploymentBenefit);
				doleAmount+=unemploymentBenefit;
			}
		}
		for(Agent agent:researchersPop.getAgents()){
			HouseholdsDifferentiated researcher= (HouseholdsDifferentiated) agent;
			if (researcher.getEmployer()==null){
				LaborSupplier unemployed = (LaborSupplier) researcher;
				Deposit depositGov = (Deposit) this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
				Item payableStock = unemployed.getPayableStock(StaticValues.MKT_LABOR);
				LiabilitySupplier payingSupplier = (LiabilitySupplier) depositGov.getLiabilityHolder();
				payingSupplier.transfer(depositGov, payableStock, unemploymentBenefit);
				doleAmount+=unemploymentBenefit;
			}
		}
		this.doleExpenditure=doleAmount;
	}

	/**
	 * Sets the labor demand equal to the fixed labor demand
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
		int nbEmployees = this.fixedLaborDemand;
		int nbManagers= (int) Math.round(nbEmployees*shareManagers);
		int nbTopManagers= (int) Math.round(nbEmployees*shareTopManagers);
		int nbWorkers=nbEmployees-(nbManagers+nbTopManagers);
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
			for(int i=0;i<currentWorkers-nbWorkers;i++){
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
			for(int i=0;i<currentWorkers-nbWorkers;i++){
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
	protected void updateAggregateVariables() {
		this.setAggregateValue(StaticValues.LAG_AGGUNEMPLOYMENT, 
				uComputer.computeVariable((MacroSimulation)((SimulationController)this.scheduler).getSimulation()));
		this.cleanSM();
		double publicDebt=0;
		for (Item bond:this.getItemsStockMatrix(false, StaticValues.SM_BONDS)){
			publicDebt+=bond.getValue();
		}
		this.addValue(StaticValues.LAG_PUBLICDEBT, publicDebt);
		MacroPopulation macroPop = (MacroPopulation) ((SimulationController)this.scheduler).getPopulation();
		Population households= (Population) macroPop.getPopulation(StaticValues.WORKERS_ID);
		double averageWage=0;
		double employed=0;
		for(Agent agent:households.getAgents()){
			HouseholdsDifferentiated worker= (HouseholdsDifferentiated) agent;
			if (worker.getEmployer()!=null){
				averageWage+=worker.getWage();
				employed+=1;
			}
		}
		averageWage=averageWage/employed;
		this.setAggregateValue (StaticValues.LAG_AVERAGEWORKERSWAGE,averageWage);
		this.workersTotalIncome=0;
	    this.managersResearchersTotalIncome=0;
	    this.topManagersTotalIncome=0;
	    this.workersTotalDeposits=0;
	    this.managersResearchersTotalDeposits=0;
	    this.topManagersTotalDeposits=0;
	    
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

	protected void payWages(){
		if(employees.size()>0){
			Deposit deposit = (Deposit) this.getItemStockMatrix(true, StaticValues.SM_RESERVES);
			payWages(deposit,StaticValues.MKT_LABOR);
		}
	}
	
	@Override
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
			HouseholdsDifferentiated worker= (HouseholdsDifferentiated) employee;
			wages+=wage;
			double income = worker.getGrossIncome();
			//double wealth=worker.getNetWealth();
			double wealth=worker.getNumericBalanceSheet()[0][StaticValues.SM_DEP];
		    if (worker.getPopulationId()==StaticValues.WORKERS_ID){
		    	this.setWorkersTotalIncome(this.getWorkersTotalIncome()+income);
	        	this.setWorkersTotalDeposits(this.getWorkersTotalDeposits()+wealth);

		    }
			if (worker.getPopulationId()==StaticValues.MANAGERS_ID||worker.getPopulationId()==StaticValues.RESEARCHERS_ID){
				this.setManagersResearchersTotalIncome(this.getManagersResearchersTotalIncome()+income);
				this.setManagersResearchersTotalDeposits(this.getManagersResearchersTotalDeposits()+wealth);
			}
			if (worker.getPopulationId()==StaticValues.TOPMANAGERS_ID){
				this.setTopManagersTotalIncome(this.getTopManagersTotalIncome()+income);
				this.setTopManagersTotalDeposits(this.getTopManagersTotalDeposits()+wealth);			}
		}
		wageBill=wages;
		
	}

	/**
	 * @return the doleExpenditure
	 */
	public double getDoleExpenditure() {
		return doleExpenditure;
	}

	/**
	 * @param doleExpenditure the doleExpenditure to set
	 */
	public void setDoleExpenditure(double doleExpenditure) {
		this.doleExpenditure = doleExpenditure;
	}


	/**
	 * @return the profitsFromCB
	 */
	public double getProfitsFromCB() {
		return profitsFromCB;
	}

	/**
	 * @param profitsFromCB the profitsFromCB to set
	 */
	public void setProfitsFromCB(double profitsFromCB) {
		this.profitsFromCB = profitsFromCB;
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
	 * @return the workersTotalIncome
	 */
	public double getWorkersTotalIncome() {
		return workersTotalIncome;
	}

	/**
	 * @param workersTotalIncome the workersTotalIncome to set
	 */
	public void setWorkersTotalIncome(double workersTotalIncome) {
		this.workersTotalIncome = workersTotalIncome;
	}

	/**
	 * @return the managersResearchersTotalIncome
	 */
	public double getManagersResearchersTotalIncome() {
		return managersResearchersTotalIncome;
	}

	/**
	 * @param managersResearchersTotalIncome the managersResearchersTotalIncome to set
	 */
	public void setManagersResearchersTotalIncome(
			double managersResearchersTotalIncome) {
		this.managersResearchersTotalIncome = managersResearchersTotalIncome;
	}

	/**
	 * @return the topManagersTotalIncome
	 */
	public double getTopManagersTotalIncome() {
		return topManagersTotalIncome;
	}

	/**
	 * @param topManagersTotalIncome the topManagersTotalIncome to set
	 */
	public void setTopManagersTotalIncome(double topManagersTotalIncome) {
		this.topManagersTotalIncome = topManagersTotalIncome;
	}

	/**
	 * @return the workersTotalDeposits
	 */
	public double getWorkersTotalDeposits() {
		return workersTotalDeposits;
	}

	/**
	 * @param workersTotalDeposits the workersTotalDeposits to set
	 */
	public void setWorkersTotalDeposits(double workersTotalDeposits) {
		this.workersTotalDeposits = workersTotalDeposits;
	}

	/**
	 * @return the topManagersTotalDeposits
	 */
	public double getTopManagersTotalDeposits() {
		return topManagersTotalDeposits;
	}
	
	

	/**
	 * @param workersIncomeTaxRate the workersIncomeTaxRate to set
	 */
	public void setWorkersIncomeTaxRate(double workersIncomeTaxRate) {
		this.workersIncomeTaxRate = workersIncomeTaxRate;
	}

	/**
	 * @param workersWealthTaxRate the workersWealthTaxRate to set
	 */
	public void setWorkersWealthTaxRate(double workersWealthTaxRate) {
		this.workersWealthTaxRate = workersWealthTaxRate;
	}

	/**
	 * @param managersIncomeTaxRate the managersIncomeTaxRate to set
	 */
	public void setManagersIncomeTaxRate(double managersIncomeTaxRate) {
		this.managersIncomeTaxRate = managersIncomeTaxRate;
	}

	/**
	 * @param managersWealthTaxRate the managersWealthTaxRate to set
	 */
	public void setManagersWealthTaxRate(double managersWealthTaxRate) {
		this.managersWealthTaxRate = managersWealthTaxRate;
	}

	/**
	 * @param topManagersIncomeTaxRate the topManagersIncomeTaxRate to set
	 */
	public void setTopManagersIncomeTaxRate(double topManagersIncomeTaxRate) {
		this.topManagersIncomeTaxRate = topManagersIncomeTaxRate;
	}

	/**
	 * @param topManagersWealthTaxRate the topManagersWealthTaxRate to set
	 */
	public void setTopManagersWealthTaxRate(double topManagersWealthTaxRate) {
		this.topManagersWealthTaxRate = topManagersWealthTaxRate;
	}

	
	/**
	 * @return the workersIncomeTaxRate
	 */
	public double getWorkersIncomeTaxRate() {
		return workersIncomeTaxRate;
	}

	/**
	 * @return the workersWealthTaxRate
	 */
	public double getWorkersWealthTaxRate() {
		return workersWealthTaxRate;
	}

	/**
	 * @return the managersIncomeTaxRate
	 */
	public double getManagersIncomeTaxRate() {
		return managersIncomeTaxRate;
	}

	/**
	 * @return the managersWealthTaxRate
	 */
	public double getManagersWealthTaxRate() {
		return managersWealthTaxRate;
	}

	/**
	 * @return the topManagersIncomeTaxRate
	 */
	public double getTopManagersIncomeTaxRate() {
		return topManagersIncomeTaxRate;
	}

	/**
	 * @return the topManagersWealthTaxRate
	 */
	public double getTopManagersWealthTaxRate() {
		return topManagersWealthTaxRate;
	}

	/**
	 * @param topManagersTotalDeposits the topManagersTotalDeposits to set
	 */
	public void setTopManagersTotalDeposits(double topManagersTotalDeposits) {
		this.topManagersTotalDeposits = topManagersTotalDeposits;
	}

	/**
	 * @return the managersResearchersTotalDeposits
	 */
	public double getManagersResearchersTotalDeposits() {
		return managersResearchersTotalDeposits;
	}

	/**
	 * @param managersResearchersTotalDeposits the managersResearchersTotalDeposits to set
	 */
	public void setManagersResearchersTotalDeposits(
			double managersResearchersTotalDeposits) {
		this.managersResearchersTotalDeposits = managersResearchersTotalDeposits;
	}

	/**
	 * @param shareTopManagers the shareTopManagers to set
	 */
	public void setShareTopManagers(double shareTopManagers) {
		this.shareTopManagers = shareTopManagers;
	}
	
	public void onAgentArrival(AgentArrivalEvent event) {
		MacroSimulation macroSim = (MacroSimulation)event.getSimulationController().getSimulation();
		int marketID=macroSim.getActiveMarket().getMarketId();
		switch(marketID){
		case StaticValues.MKT_LABOR: //se should use random robin mixer in the case of government labor market
			SelectWorkerStrategy strategyLabor = (SelectWorkerStrategy) this.getStrategy(StaticValues.STRATEGY_LABOR);
			List<MacroAgent> workers= (List<MacroAgent>)strategyLabor.selectWorkers(event.getObjects(),this.workersDemand);
			for(MacroAgent worker:workers)
				macroSim.getActiveMarket().commit(this, worker,marketID);
			break;	
	case StaticValues.MKT_LABORMANAGERS: //se should use random robin mixer in the case of government labor market
		SelectWorkerStrategy strategyLaborManagers = (SelectWorkerStrategy) this.getStrategy(StaticValues.STRATEGY_LABOR);
		List<MacroAgent> managers= (List<MacroAgent>)strategyLaborManagers.selectWorkers(event.getObjects(),this.managersDemand);
		for(MacroAgent manager:managers)
			macroSim.getActiveMarket().commit(this, manager,marketID);
		break;
	case StaticValues.MKT_LABORTOPMANAGERS: //se should use random robin mixer in the case of government labor market
		SelectWorkerStrategy strategyLaborTopManagers = (SelectWorkerStrategy) this.getStrategy(StaticValues.STRATEGY_LABOR);
		List<MacroAgent> topManagers= (List<MacroAgent>)strategyLaborTopManagers.selectWorkers(event.getObjects(),this.topManagersDemand);
		for(MacroAgent topManager:topManagers)
			macroSim.getActiveMarket().commit(this, topManager,marketID);
		break;
	}
	}

	/**
	 * Populates the agent characteristics using the byte array content. The structure is as follows:
	 * [sizeMacroAgentStructure][MacroAgentStructure][bondPrice][bondInterestRate][turnoverLabor][unemploymentBenefit][laborDemand]
	 * [fixedLaborDemand][bondMaturity][sizeTaxedPop][taxedPopulations][matrixSize][stockMatrixStructure][expSize][ExpectationStructure]
	 * [passedValSize][PassedValStructure][stratsSize][StrategiesStructure]
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
		unemploymentBenefit = buf.getDouble();
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
	 * [sizeMacroAgentStructure][MacroAgentStructure][bondPrice][bondInterestRate][turnoverLabor][unemploymentBenefit][laborDemand]
	 * [fixedLaborDemand][bondMaturity][sizeTaxedPop][taxedPopulations][matrixSize][stockMatrixStructure][expSize][ExpectationStructure]
	 * [passedValSize][PassedValStructure][stratsSize][StrategiesStructure]
	 */
	@Override
	public byte[] getBytes() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			byte[] charBytes = super.getAgentCharacteristicsBytes();
			out.write(ByteBuffer.allocate(4).putInt(charBytes.length).array());
			out.write(charBytes);
			ByteBuffer buf = ByteBuffer.allocate(48+4*taxedPopulations.length);
			buf.putDouble(bondPrice);
			buf.putDouble(bondInterestRate);
			buf.putDouble(turnoverLabor);
			buf.putDouble(unemploymentBenefit);
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
}
