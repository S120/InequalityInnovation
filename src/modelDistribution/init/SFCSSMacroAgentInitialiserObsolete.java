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
package modelDistribution.init;

import jmab.agents.CreditSupplier;
import jmab.agents.DepositSupplier;
import jmab.agents.GoodSupplier;
import jmab.agents.MacroAgent;
import jmab.agents.SimpleAbstractAgent;
import jmab.expectations.Expectation;
import jmab.init.AbstractMacroAgentInitialiser;
import jmab.init.MacroAgentInitialiser;
import jmab.population.MacroPopulation;
import jmab.simulations.MacroSimulation;
import jmab.stockmatrix.Bond;
import jmab.stockmatrix.CapitalGood;
import jmab.stockmatrix.Cash;
import jmab.stockmatrix.ConsumptionGood;
import jmab.stockmatrix.Deposit;
import jmab.stockmatrix.Item;
import jmab.stockmatrix.Loan;
import jmab.strategies.BestQualityPriceCapitalSupplierWithSwitching;
import jmab.strategies.CheapestGoodSupplierWithSwitching;
import jmab.strategies.CheapestLenderWithSwitching;
import jmab.strategies.MostPayingDepositWithSwitching;
import modelDistribution.StaticValues;
import modelDistribution.agents.Bank;
import modelDistribution.agents.CapitalFirm;
import modelDistribution.agents.CapitalFirmWithHierarchy;
import modelDistribution.agents.CentralBank;
import modelDistribution.agents.ConsumptionFirm;
import modelDistribution.agents.ConsumptionFirmWithHierarchy;
import modelDistribution.agents.GovernmentDifferentiatedWorkers;
import modelDistribution.agents.HouseholdsDifferentiated;
import modelDistribution.strategies.InvestmentCapacityOperatingCashFlowExpected;
import net.sourceforge.jabm.Population;
import cern.jet.random.Uniform;
import cern.jet.random.engine.RandomEngine;

/**
 * @author Alessandro Caiani and Antoine Godin
 *
 */
public class SFCSSMacroAgentInitialiserObsolete extends AbstractMacroAgentInitialiser implements MacroAgentInitialiser{

	//Stocks
	//Households
	

	//Cap Firms
	private double ksDep;
	private int ksInv;
	private double ksLoans;
	private double ksLoans0;
	private double capitalLaborRatio;
	//Cons Firms
	private double csDep;
	private int csInv;
	private double csLoans;
	private double csLoans0;
	
	private int csKap;
	//Banks
	private double bsBonds;
	private double bsRes;
	private double bsAdv;
	private double bsCash;

	//Flows
	//Households
	private double dividendsReceived;
	//private double hhsInc;
	private double workersShareIncome;
	private double managerShareIncome;
	private double topManagerShareIncome;
	private double workersShareWealth;
	private double managerResearchersShareWealth;
	private double topManagerShareWealth;
	private double averageWage;
	private double householdsCash;
	private double householdsDeposits;

	//CapFirms
	private int ksEmpl;
	private double ksSales;
	private double ksProfits;
	private double kPrice;
	private double kUnitCost;
	private double ksOCF;

	//ConsFirms
	private int csEmpl;
	private double csSales;
	private double csProfits;
	private double cPrice;
	private double cUnitCost;
	private double csOCF;
	//Banks
	private double iLoans;
	private double iDep;
	private double bsProfits;
	//Government
	private int gEmpl;
	private double iBonds;
	//Central Bank
	private double iAdv;
	private double cbBonds;
	//RandomEngine
	private RandomEngine prng;
	private double uniformDistr;
	private double gr;
	private double shareWorkers;
	private double shareManagers;
	private double shareTopManagers;
	private double shareResearchers;
	

	/* (non-Javadoc)
	 * @see jmab.init.MacroAgentInitialiser#initialise(jmab.population.MacroPopulation)
	 */
	@Override
	public void initialise(MacroPopulation population, MacroSimulation sim) {	
		
		Population workers = population.getPopulation(StaticValues.WORKERS_ID);
		Population managers= population.getPopulation(StaticValues.MANAGERS_ID);
		Population researchers= population.getPopulation(StaticValues.RESEARCHERS_ID);
		Population topManagers= population.getPopulation(StaticValues.TOPMANAGERS_ID);
		Population banks = population.getPopulation(StaticValues.BANKS_ID);
		Population kFirms = population.getPopulation(StaticValues.CAPITALFIRMS_ID);
		Population cFirms = population.getPopulation(StaticValues.CONSUMPTIONFIRMS_ID);
		GovernmentDifferentiatedWorkers govt = (GovernmentDifferentiatedWorkers)population.getPopulation(StaticValues.GOVERNMENT_ID).getAgentList().get(0);
		CentralBank cb = (CentralBank)population.getPopulation(StaticValues.CB_ID).getAgentList().get(0);

		int workerSize=workers.getSize();
		int researcherSize=researchers.getSize();
		int managerSize=managers.getSize();
		int topManagerSize=topManagers.getSize();
		int bSize=banks.getSize();
		int kSize=kFirms.getSize();
		int cSize=cFirms.getSize();

		int workerPerBank = (int) Math.ceil((double)workerSize/(double)bSize);
		int researcherPerBank=(int) Math.ceil((double)researcherSize/(double)bSize);
		int managerPerBank=(int) Math.ceil((double)managerSize/(double)bSize);
		int topManagerPerBank= (int) Math.ceil((double)topManagerSize/(double)bSize);
		int cFirmPerBank = (int) Math.ceil((double)cSize/(double)bSize);
		int kFirmPerBank = (int) Math.ceil((double)kSize/(double)bSize);
		int cFirmPerkFirm = (int) Math.ceil((double)cSize/(double)kSize);
		int workerPercFirm = (int) Math.ceil((double)workerSize/(double)cSize);
		int managerPercFirm=(int) Math.ceil((double)managerSize/(double)cSize);
		int topManagerPercFirm=(int) Math.ceil((double)topManagerSize/(double)cSize);
		int researcherPercFirm=(int) Math.ceil((double)researcherSize/(double)cSize);
		double workersWage=(averageWage*workersShareIncome)/shareWorkers;
		double managerWage=(averageWage*managerShareIncome)/shareManagers;
		double topManagerWage=(averageWage*topManagerShareIncome)/shareTopManagers;
		double researcherWage=managerWage;
		
		double workersDep=(householdsDeposits*workersShareWealth);
		double managerDep=(householdsDeposits*managerResearchersShareWealth)*managerSize/(managerSize+researcherSize);
		double researcherDep=(householdsDeposits*managerResearchersShareWealth)*researcherSize/(managerSize+researcherSize);
        double topManagerDep=(householdsDeposits*topManagerShareWealth);
        double workersCash=(householdsCash*workersShareWealth);
		double managerCash=(householdsCash*managerResearchersShareWealth)*managerSize/(managerSize+researcherSize);
		double researcherCash=(householdsCash*managerResearchersShareWealth)*researcherSize/(managerSize+researcherSize);
        double topManagerCash=(householdsCash*topManagerShareWealth);
		
        Uniform distr = new Uniform(-uniformDistr,uniformDistr,prng);
        
		//Households
		
		double workerCons = this.csSales/(workerSize*cPrice);
		double managerCons = this.csSales/(managerSize*cPrice);
		double topManagerCons = this.csSales/(topManagerSize*cPrice);
		double researcherCons=this.csSales/(researcherSize*cPrice);

		for(int i = 0; i<workerSize; i++){
			HouseholdsDifferentiated worker = (HouseholdsDifferentiated) workers.getAgentList().get(i);
			worker.setDividendsReceived(0);

			//Cash Holdings
			Cash cash = new Cash(workersCash,(SimpleAbstractAgent)worker,(SimpleAbstractAgent)cb);
			worker.addItemStockMatrix(cash, true, StaticValues.SM_CASH);
			cb.addItemStockMatrix(cash, false, StaticValues.SM_CASH);

			//Deposit Holdings
			int bankId = (int) i/workerPerBank;
			MacroAgent bank = (MacroAgent) banks.getAgentList().get(bankId);
			Deposit dep = new Deposit(workersDep/workerSize, worker, bank, this.iDep);
			worker.addItemStockMatrix(dep, true, StaticValues.SM_DEP);
			bank.addItemStockMatrix(dep, false, StaticValues.SM_DEP);

			//Make sure there are no employer
			worker.setEmployer(null);
			
			//Set previous seller
			int sellerId= (int) i/workerPercFirm;
			GoodSupplier previousSeller= (GoodSupplier) cFirms.getAgentList().get(sellerId);
			CheapestGoodSupplierWithSwitching buyingStrategy= (CheapestGoodSupplierWithSwitching) worker.getStrategy(StaticValues.STRATEGY_BUYING);
			buyingStrategy.setPreviousSeller(previousSeller);
			
			//Set Previous Deposit Supplier
			MostPayingDepositWithSwitching depositStrategy= (MostPayingDepositWithSwitching) worker.getStrategy(StaticValues.STRATEGY_DEPOSIT);
			DepositSupplier previousBankDeposit= (DepositSupplier) banks.getAgentList().get(bankId);
			depositStrategy.setPreviousDepositSupplier(previousBankDeposit);
			
			//Expectations and Lagged values
			worker.setWage(workersWage);
			worker.addValue(StaticValues.LAG_NETWEALTH, worker.getNetWealth());
			Expectation cPriceExp = worker.getExpectation(StaticValues.EXPECTATIONS_CONSPRICE);
			int nbObs = cPriceExp.getNumberPeriod();
			double[][] passedcPrices = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedcPrices[j][0]=this.cPrice;
				passedcPrices[j][1]=this.cPrice;
			}
			cPriceExp.setPassedValues(passedcPrices);
			worker.addValue(StaticValues.LAG_EMPLOYED,0);
			worker.addValue(StaticValues.LAG_CONSUMPTION,workerCons*(1+distr.nextDouble()));
			worker.computeExpectations();
		}
		
		for(int i = 0; i<researcherSize; i++){
			HouseholdsDifferentiated researcher = (HouseholdsDifferentiated) researchers.getAgentList().get(i);
			researcher.setDividendsReceived(0);

			//Cash Holdings
			Cash cash = new Cash(researcherCash,(SimpleAbstractAgent)researcher,(SimpleAbstractAgent)cb);
			researcher.addItemStockMatrix(cash, true, StaticValues.SM_CASH);
			cb.addItemStockMatrix(cash, false, StaticValues.SM_CASH);

			//Deposit Holdings
			int bankId = (int) i/researcherPerBank;
			MacroAgent bank = (MacroAgent) banks.getAgentList().get(bankId);
			Deposit dep = new Deposit(researcherDep/researcherSize, researcher, bank, this.iDep);
			researcher.addItemStockMatrix(dep, true, StaticValues.SM_DEP);
			bank.addItemStockMatrix(dep, false, StaticValues.SM_DEP);

			//Make sure there are no employer
			researcher.setEmployer(null);
			
			//Set previous seller
			int sellerId= (int) i/researcherPercFirm;
			GoodSupplier previousSeller= (GoodSupplier) cFirms.getAgentList().get(sellerId);
			CheapestGoodSupplierWithSwitching buyingStrategy= (CheapestGoodSupplierWithSwitching) researcher.getStrategy(StaticValues.STRATEGY_BUYING);
			buyingStrategy.setPreviousSeller(previousSeller);
			
			//Set Previous Deposit Supplier
			MostPayingDepositWithSwitching depositStrategy= (MostPayingDepositWithSwitching) researcher.getStrategy(StaticValues.STRATEGY_DEPOSIT);
			DepositSupplier previousBankDeposit= (DepositSupplier) banks.getAgentList().get(bankId);
			depositStrategy.setPreviousDepositSupplier(previousBankDeposit);
			
			//Expectations and Lagged values
			researcher.setWage(researcherWage);
			researcher.addValue(StaticValues.LAG_NETWEALTH, researcher.getNetWealth());
			Expectation cPriceExp = researcher.getExpectation(StaticValues.EXPECTATIONS_CONSPRICE);
			int nbObs = cPriceExp.getNumberPeriod();
			double[][] passedcPrices = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedcPrices[j][0]=this.cPrice;
				passedcPrices[j][1]=this.cPrice;
			}
			cPriceExp.setPassedValues(passedcPrices);
			researcher.addValue(StaticValues.LAG_EMPLOYED,0);
			researcher.addValue(StaticValues.LAG_CONSUMPTION,researcherCons*(1+distr.nextDouble()));
			researcher.computeExpectations();
		}
		
		for(int i = 0; i<managerSize; i++){
			HouseholdsDifferentiated manager = (HouseholdsDifferentiated) managers.getAgentList().get(i);
			//manager.setDividendsReceived(this.dividendsReceived/managerSize);
			manager.setDividendsReceived(0);
			
			//Cash Holdings
			Cash cash = new Cash(managerCash,(SimpleAbstractAgent)manager,(SimpleAbstractAgent)cb);
			manager.addItemStockMatrix(cash, true, StaticValues.SM_CASH);
			cb.addItemStockMatrix(cash, false, StaticValues.SM_CASH);

			//Deposit Holdings
			int bankId = (int) i/managerPerBank;
			MacroAgent bank = (MacroAgent) banks.getAgentList().get(bankId);
			Deposit dep = new Deposit(managerDep/managerSize, manager, bank, this.iDep);
			manager.addItemStockMatrix(dep, true, StaticValues.SM_DEP);
			bank.addItemStockMatrix(dep, false, StaticValues.SM_DEP);

			//Make sure there are no employer
			manager.setEmployer(null);
			
			//Set previous seller
			int sellerId= (int) i/managerPercFirm;
			GoodSupplier previousSeller= (GoodSupplier) cFirms.getAgentList().get(sellerId);
			CheapestGoodSupplierWithSwitching buyingStrategy= (CheapestGoodSupplierWithSwitching) manager.getStrategy(StaticValues.STRATEGY_BUYING);
			buyingStrategy.setPreviousSeller(previousSeller);
			
			//Set Previous Deposit Supplier
			MostPayingDepositWithSwitching depositStrategy= (MostPayingDepositWithSwitching) manager.getStrategy(StaticValues.STRATEGY_DEPOSIT);
			DepositSupplier previousBankDeposit= (DepositSupplier) banks.getAgentList().get(bankId);
			depositStrategy.setPreviousDepositSupplier(previousBankDeposit);
			
			//Expectations and Lagged values
			manager.setWage(managerWage);
			manager.addValue(StaticValues.LAG_NETWEALTH, manager.getNetWealth());
			Expectation cPriceExp = manager.getExpectation(StaticValues.EXPECTATIONS_CONSPRICE);
			int nbObs = cPriceExp.getNumberPeriod();
			double[][] passedcPrices = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedcPrices[j][0]=this.cPrice;
				passedcPrices[j][1]=this.cPrice;
			}
			cPriceExp.setPassedValues(passedcPrices);
			manager.addValue(StaticValues.LAG_EMPLOYED,0);
			manager.addValue(StaticValues.LAG_CONSUMPTION,managerCons*(1+distr.nextDouble()));
			manager.computeExpectations();
		}
		
		for(int i = 0; i<topManagerSize; i++){
			HouseholdsDifferentiated topManager = (HouseholdsDifferentiated) topManagers.getAgentList().get(i);
			topManager.setDividendsReceived(this.dividendsReceived/topManagerSize);

			//Cash Holdings
			Cash cash = new Cash(topManagerCash,(SimpleAbstractAgent)topManager,(SimpleAbstractAgent)cb);
			topManager.addItemStockMatrix(cash, true, StaticValues.SM_CASH);
			cb.addItemStockMatrix(cash, false, StaticValues.SM_CASH);

			//Deposit Holdings
			int bankId = (int) i/topManagerPerBank;
			MacroAgent bank = (MacroAgent) banks.getAgentList().get(bankId);
			Deposit dep = new Deposit(topManagerDep/topManagerSize, topManager, bank, this.iDep);
			topManager.addItemStockMatrix(dep, true, StaticValues.SM_DEP);
			bank.addItemStockMatrix(dep, false, StaticValues.SM_DEP);

			//Make sure there are no employer
			topManager.setEmployer(null);
			
			//Set previous seller
			int sellerId= (int) i/topManagerPercFirm;
			GoodSupplier previousSeller= (GoodSupplier) cFirms.getAgentList().get(sellerId);
			CheapestGoodSupplierWithSwitching buyingStrategy= (CheapestGoodSupplierWithSwitching) topManager.getStrategy(StaticValues.STRATEGY_BUYING);
			buyingStrategy.setPreviousSeller(previousSeller);
			
			//Set Previous Deposit Supplier
			MostPayingDepositWithSwitching depositStrategy= (MostPayingDepositWithSwitching) topManager.getStrategy(StaticValues.STRATEGY_DEPOSIT);
			DepositSupplier previousBankDeposit= (DepositSupplier) banks.getAgentList().get(bankId);
			depositStrategy.setPreviousDepositSupplier(previousBankDeposit);
			
			//Expectations and Lagged values
			topManager.setWage(topManagerWage);
			topManager.addValue(StaticValues.LAG_NETWEALTH, topManager.getNetWealth());
			Expectation cPriceExp = topManager.getExpectation(StaticValues.EXPECTATIONS_CONSPRICE);
			int nbObs = cPriceExp.getNumberPeriod();
			double[][] passedcPrices = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedcPrices[j][0]=this.cPrice;
				passedcPrices[j][1]=this.cPrice;
			}
			cPriceExp.setPassedValues(passedcPrices);
			topManager.addValue(StaticValues.LAG_EMPLOYED,0);
			topManager.addValue(StaticValues.LAG_CONSUMPTION,topManagerCons*(1+distr.nextDouble()));
			topManager.computeExpectations();
		}
		
		workers.getAgentList().shuffle(prng);
		managers.getAgentList().shuffle(prng);
		topManagers.getAgentList().shuffle(prng);
		researchers.getAgentList().shuffle(prng);

		//Capital Firms
		int kInv = ksInv/kSize;
		double kDep = ksDep/kSize;
		double kLoan=ksLoans0/kSize;
		int workerCounter = 0;
		int managerCounter=0;
		int topManagerCounter=0;
		int researcherCounter=0;
		int bankLoanIterator=0;
		double kProfit=this.ksProfits/kSize;
		double kSales=this.ksSales/kSize;
		double kOutput=kSales/kPrice;
		double kOCF=ksOCF/kSize;
		double lMat=0;
		int kEmpl = ksEmpl/kSize;
		int kManagers=(int) Math.floor(kEmpl*(this.shareManagers-this.shareResearchers));
		int kTopManagers=(int) Math.floor(kEmpl*this.shareTopManagers);
		int kResearchers=(int) Math.floor(kEmpl*this.shareResearchers);
		int kWorkers=kEmpl-kManagers-kTopManagers-kResearchers;
		int laborProductivity=(int) Math.round(kOutput/kWorkers);
		//C FIRMS EMPLOYEES
		int cEmpl=csEmpl/cSize;
		int cManagers=(int) Math.floor(cEmpl*this.shareManagers);
		int cTopManagers=(int) Math.floor(cEmpl*this.shareTopManagers);
		int cWorkers=cEmpl-cManagers-cTopManagers;
		ConsumptionFirm cf= (ConsumptionFirm) cFirms.getAgentList().get(1);
		InvestmentCapacityOperatingCashFlowExpected invStrategy=(InvestmentCapacityOperatingCashFlowExpected)cf.getStrategy(StaticValues.STRATEGY_INVESTMENT);
		double targetCapacityUtilization=invStrategy.getTargetCapacityUtlization();
		double capitalLaborRatio= csKap/cSize*targetCapacityUtilization/cWorkers;

		for(int i = 0 ; i < kSize ; i++){
			CapitalFirmWithHierarchy k = (CapitalFirmWithHierarchy) kFirms.getAgentList().get(i);
            k.setLaborProductivity(laborProductivity);
            k.setCapitalLaborRatio(capitalLaborRatio);
			//Inventories
			CapitalGood kGood = new CapitalGood(kInv*this.kPrice, kInv, k, k, this.kPrice, k.getCapitalProductivity(), 
					k.getCapitalDuration(), k.getCapitalAmortization(), capitalLaborRatio);
			kGood.setUnitCost(kUnitCost);
			k.addItemStockMatrix(kGood, true, StaticValues.SM_CAPGOOD);
            k.setCapitalLaborRatio(capitalLaborRatio);
			//Workers
			for(int j=0;j<kWorkers;j++){
				HouseholdsDifferentiated worker = (HouseholdsDifferentiated) workers.getAgentList().get(workerCounter);
				worker.setEmployer(k);
				worker.addValue(StaticValues.LAG_EMPLOYED,1);
				k.addEmployee(worker, StaticValues.MKT_LABOR);
				workerCounter++;
			}
			for(int j=0;j<kResearchers;j++){
				HouseholdsDifferentiated researcher = (HouseholdsDifferentiated) researchers.getAgentList().get(researcherCounter);
				researcher.setEmployer(k);
			    researcher.addValue(StaticValues.LAG_EMPLOYED,1);
				k.addEmployee(researcher, StaticValues.MKT_LABORRESEARCHERS);
				researcherCounter++;
			}
			for(int j=0;j<kManagers;j++){
				HouseholdsDifferentiated manager = (HouseholdsDifferentiated) managers.getAgentList().get(managerCounter);
				manager.setEmployer(k);
			    manager.addValue(StaticValues.LAG_EMPLOYED,1);
				k.addEmployee(manager,StaticValues.MKT_LABORMANAGERS);
				managerCounter++;
			}
			for(int j=0;j<kTopManagers;j++){
				HouseholdsDifferentiated topManager = (HouseholdsDifferentiated) topManagers.getAgentList().get(topManagerCounter);
				topManager.setEmployer(k);
			    topManager.addValue(StaticValues.LAG_EMPLOYED,1);
				k.addEmployee(topManager, StaticValues.MKT_LABORTOPMANAGERS);
				topManagerCounter++;
			}
			

			//Deposit Holdings
			int bankId = (int) i/kFirmPerBank;
			MacroAgent bank = (MacroAgent) banks.getAgentList().get(bankId);
			Deposit dep = new Deposit(kDep, k, bank, this.iDep);
			k.addItemStockMatrix(dep, true, StaticValues.SM_DEP);
			bank.addItemStockMatrix(dep, false, StaticValues.SM_DEP);
			//Set Previous Deposit Supplier
			MostPayingDepositWithSwitching depositStrategy= (MostPayingDepositWithSwitching) k.getStrategy(StaticValues.STRATEGY_DEPOSIT);
			DepositSupplier previousBankDeposit= (DepositSupplier) banks.getAgentList().get(bankId);
			depositStrategy.setPreviousDepositSupplier(previousBankDeposit);
			

			//Cash
			Cash cash = new Cash(0,(SimpleAbstractAgent)k,(SimpleAbstractAgent)cb);
			k.addItemStockMatrix(cash, true, StaticValues.SM_CASH);
			cb.addItemStockMatrix(cash, false, StaticValues.SM_CASH);

			
			//Loans
			bankLoanIterator=bankId;
			lMat = k.getLoanLength();
			for(int j = 0 ; j <= lMat -1 ; j++){
				Bank loanBank = (Bank) banks.getAgentList().get(bankLoanIterator);
				bankLoanIterator++;
				if(bankLoanIterator>=bSize)bankLoanIterator=0;
				Loan loan = new Loan(kLoan*(1/(Math.pow((1+gr),j)))*((lMat-j)/lMat), loanBank, k, this.iLoans, j+1, k.getLoanAmortizationType(), (int)lMat);
				loan.setInitialAmount(kLoan/Math.pow((1+gr),j));
				k.addItemStockMatrix(loan, false, StaticValues.SM_LOAN);
				loanBank.addItemStockMatrix(loan, true, StaticValues.SM_LOAN);
				//Set last period lender as previous lender in the firm's borrowing strategy
				if (j==1){
					CheapestLenderWithSwitching borrowingStrategy = (CheapestLenderWithSwitching) k.getStrategy(StaticValues.STRATEGY_BORROWING);
					CreditSupplier previousCreditor= (CreditSupplier) bank;
					borrowingStrategy.setPreviousLender(previousCreditor);
				}
			}
			
			//Expectations and Lagged Values
			k.addValue(StaticValues.LAG_PROFITPRETAX, kProfit*(1+distr.nextDouble()));
			k.addValue(StaticValues.LAG_PROFITAFTERTAX, kProfit*(1+distr.nextDouble()));
			//double lagKInv=kInv*(1+distr.nextDouble());
			double lagKInv=kInv;
			k.addValue(StaticValues.LAG_INVENTORIES, lagKInv);
			k.addValue(StaticValues.LAG_PRODUCTION, kOutput*(1+distr.nextDouble()));
			k.addValue(StaticValues.LAG_REALSALES, kOutput*(1+distr.nextDouble()));
			k.addValue(StaticValues.LAG_PRICE, kPrice);
			k.addValue(StaticValues.LAG_NOMINALSALES, kSales*(1+distr.nextDouble()));
			k.addValue(StaticValues.LAG_NETWEALTH, k.getNetWealth()*(1+distr.nextDouble()));
			k.addValue(StaticValues.LAG_NOMINALINVENTORIES, lagKInv*kUnitCost);
			k.addValue(StaticValues.LAG_OPERATINGCASHFLOW,kOCF);
			Expectation kWageExp = k.getExpectation(StaticValues.EXPECTATIONS_WAGES);
			int nbObs = kWageExp.getNumberPeriod();
			double[][] passedWage = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedWage[j][0]=this.averageWage*(1+distr.nextDouble());
				passedWage[j][1]=this.averageWage*(1+distr.nextDouble());
			}
			kWageExp.setPassedValues(passedWage);
			Expectation kSalesExp = k.getExpectation(StaticValues.EXPECTATIONS_NOMINALSALES);
			nbObs = kSalesExp .getNumberPeriod();
			double[][] passedSales = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedSales[j][0]=kSales*(1+distr.nextDouble());
				passedSales[j][1]=kSales*(1+distr.nextDouble());
			}
			kSalesExp.setPassedValues(passedSales);
			Expectation kRSalesExp = k.getExpectation(StaticValues.EXPECTATIONS_REALSALES);
			nbObs = kRSalesExp .getNumberPeriod();
			double[][] passedRSales = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedRSales[j][0]=kOutput*(1+distr.nextDouble());
				passedRSales[j][1]=kOutput*(1+distr.nextDouble());
			}
			kRSalesExp.setPassedValues(passedRSales);
			k.computeExpectations();
		}

		//Consumption Firms
		int cInv = csInv/cSize;
		double cDep = csDep/cSize;
		double cLoan = csLoans0/cSize;
		double cProfit=this.csProfits/cSize;
		double cSales=this.csSales/cSize;
		double cOutput=cSales/cPrice;
		double cOCF=csOCF/cSize;
		for(int i = 0 ; i < cSize ; i++){
			ConsumptionFirmWithHierarchy c = (ConsumptionFirmWithHierarchy) cFirms.getAgentList().get(i);
			//Inventories
			ConsumptionGood cGood = new ConsumptionGood(cInv*this.cPrice, cInv, c, c, this.cPrice, 0);
			cGood.setUnitCost(cUnitCost);
			cGood.setAge(-1);
			c.addItemStockMatrix(cGood, true, StaticValues.SM_CONSGOOD);
			
			//Capital Stock
			int kFirmId = (int) i/cFirmPerkFirm;
			CapitalFirm kFirm = (CapitalFirm) kFirms.getAgentList().get(kFirmId);
			int kMat = kFirm.getCapitalDuration();
			double kAm = kFirm.getCapitalAmortization();
			double cCap = this.csKap/cSize;
			double cCapPerPeriod=cCap/kMat;
			double capitalValue=0;//Changed this, because we assume the capital stock to work fine until it becomes obsolete
			for(int j = 0 ; j < kMat ; j++){
				CapitalGood kGood = new CapitalGood(this.kPrice*cCapPerPeriod*(1-j/kAm)/Math.pow((1+gr),j), cCapPerPeriod, c, kFirm, 
						this.kPrice/Math.pow((1+gr),j),kFirm.getCapitalProductivity(),kMat,(int)kAm,kFirm.getCapitalLaborRatio());
				kGood.setAge(j);
				kGood.setUnitCost(kUnitCost);
				capitalValue+=kGood.getValue();
				c.addItemStockMatrix(kGood, true, StaticValues.SM_CAPGOOD);
			}
			
			//Previous cpaital supplier
			BestQualityPriceCapitalSupplierWithSwitching buyingStrategy= (BestQualityPriceCapitalSupplierWithSwitching) c.getStrategy(StaticValues.STRATEGY_BUYING);
			buyingStrategy.setPreviousSupplier(kFirm);
			
			
			//Workers
			for(int j=0;j<cWorkers;j++){
				HouseholdsDifferentiated worker = (HouseholdsDifferentiated) workers.getAgentList().get(workerCounter);
				worker.setEmployer(c);
				worker.addValue(StaticValues.LAG_EMPLOYED,1);
				c.addEmployee(worker, StaticValues.MKT_LABOR);
				workerCounter++;
			}
			for(int j=0;j<cManagers;j++){
				HouseholdsDifferentiated manager = (HouseholdsDifferentiated) managers.getAgentList().get(managerCounter);
				manager.setEmployer(c);
			    manager.addValue(StaticValues.LAG_EMPLOYED,1);
				c.addEmployee(manager, StaticValues.MKT_LABORMANAGERS);
				managerCounter++;
			}
			for(int j=0;j<cTopManagers;j++){
				HouseholdsDifferentiated topManager = (HouseholdsDifferentiated) topManagers.getAgentList().get(topManagerCounter);
				topManager.setEmployer(c);
			    topManager.addValue(StaticValues.LAG_EMPLOYED,1);
				c.addEmployee(topManager, StaticValues.MKT_LABORTOPMANAGERS);
				topManagerCounter++;
			}

			//Deposit Holdings
			int bankId = (int)i/cFirmPerBank;
			MacroAgent bank = (MacroAgent) banks.getAgentList().get(bankId);
			Deposit dep = new Deposit(cDep, c, bank, this.iDep);
			c.addItemStockMatrix(dep, true, StaticValues.SM_DEP);
			bank.addItemStockMatrix(dep, false, StaticValues.SM_DEP);
			//Set Previous DepositSupplier
			MostPayingDepositWithSwitching depositStrategy= (MostPayingDepositWithSwitching) c.getStrategy(StaticValues.STRATEGY_DEPOSIT);
			DepositSupplier previousDepositSupplier= (DepositSupplier) banks.getAgentList().get(bankId);
			depositStrategy.setPreviousDepositSupplier(previousDepositSupplier);

			//Cash
			Cash cash = new Cash(0,(SimpleAbstractAgent)c,(SimpleAbstractAgent)cb);
			c.addItemStockMatrix(cash, true, StaticValues.SM_CASH);
			cb.addItemStockMatrix(cash, false, StaticValues.SM_CASH);
			
			//Loans
			bankLoanIterator=bankId;
			lMat = c.getLoanLength();
			
			for(int j = 0 ; j <= lMat-1 ; j++){
				Bank loanBank = (Bank) banks.getAgentList().get(bankLoanIterator);
				bankLoanIterator++;
				if(bankLoanIterator>=bSize)bankLoanIterator=0;
				Loan loan = new Loan(cLoan*(1/(Math.pow((1+gr),j)))*((lMat-j)/lMat), loanBank, c, this.iLoans, j+1, c.getLoanAmortizationType(), (int)lMat);
				loan.setInitialAmount(cLoan/Math.pow((1+gr),j));
				c.addItemStockMatrix(loan, false, StaticValues.SM_LOAN);
				loanBank.addItemStockMatrix(loan, true, StaticValues.SM_LOAN);
				//Set last period lender as previous lender in the firm's borrowing strategy
				if (j==1){
					CheapestLenderWithSwitching borrowingStrategy = (CheapestLenderWithSwitching) c.getStrategy(StaticValues.STRATEGY_BORROWING);
					CreditSupplier previousCreditor= (CreditSupplier) bank;
					borrowingStrategy.setPreviousLender(previousCreditor);
				}
				
			}
			
			//Expectations and Lagged Values
			c.addValue(StaticValues.LAG_PROFITPRETAX, cProfit*(1+distr.nextDouble()));
			c.addValue(StaticValues.LAG_PROFITAFTERTAX, cProfit*(1+distr.nextDouble()));
			//double lagCInv=cInv*(1+distr.nextDouble());
			double lagCInv=cInv;
			c.addValue(StaticValues.LAG_INVENTORIES, lagCInv);
			c.addValue(StaticValues.LAG_PRODUCTION, cOutput*(1+distr.nextDouble()));
			c.addValue(StaticValues.LAG_REALSALES, cOutput*(1+distr.nextDouble()));
			c.addValue(StaticValues.LAG_PRICE, cPrice);
			c.addValue(StaticValues.LAG_CAPACITY,cCap*kFirm.getCapitalProductivity()*(1+distr.nextDouble()));
			c.addValue(StaticValues.LAG_CAPITALFINANCIALVALUE,capitalValue);
			c.addValue(StaticValues.LAG_NOMINALSALES, cOutput*cPrice*(1+distr.nextDouble()));
			c.addValue(StaticValues.LAG_NETWEALTH, c.getNetWealth()*(1+distr.nextDouble()));
			c.addValue(StaticValues.LAG_NOMINALINVENTORIES, lagCInv*cUnitCost);
			c.addValue(StaticValues.LAG_OPERATINGCASHFLOW, cOCF);
			Expectation cWageExp = c.getExpectation(StaticValues.EXPECTATIONS_WAGES);
			int nbObs = cWageExp.getNumberPeriod();
			double[][] passedWage = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedWage[j][0]=this.averageWage*(1+distr.nextDouble());
				passedWage[j][1]=this.averageWage*(1+distr.nextDouble());
			}
			cWageExp.setPassedValues(passedWage);
			Expectation cSalesExp = c.getExpectation(StaticValues.EXPECTATIONS_NOMINALSALES);
			nbObs = cSalesExp .getNumberPeriod();
			double[][] passedSales = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedSales[j][0]=cSales*(1+distr.nextDouble());
				passedSales[j][1]=cSales*(1+distr.nextDouble());
			}
			cSalesExp.setPassedValues(passedSales);
			Expectation cRSalesExp = c.getExpectation(StaticValues.EXPECTATIONS_REALSALES);
			nbObs = cRSalesExp .getNumberPeriod();
			double[][] passedRSales = new double[nbObs][2];
			for(int j = 0; j<nbObs; j++){
				passedRSales[j][0]=cOutput*(1+distr.nextDouble());
				passedRSales[j][1]=cOutput*(1+distr.nextDouble());
			}
			cRSalesExp.setPassedValues(passedRSales);
			
			c.computeExpectations();
		}

		//Banks
		double bCash = this.bsCash/bSize;
		double bRes = this.bsRes/bSize;
		int bondMat = govt.getBondMaturity();
		double bBond=this.bsBonds/bSize;
		int bondPrice = (int)govt.getBondPrice();
		int nbBondsPerPeriod = (int) bBond/(bondMat*bondPrice);
		double bProfit = this.bsProfits/bSize;
		double bAdv = this.bsAdv/bSize;
		for(int i = 0; i<bSize; i++){
			Bank b = (Bank) banks.getAgentList().get(i);
			//b.setRiskAversion(3);
			//b.setRiskAversionC(distr1.nextDouble());
			//b.setRiskAversionK(distr1.nextDouble());
			//Cash Holdings
			Cash cash = new Cash(bCash,(SimpleAbstractAgent)b,(SimpleAbstractAgent)cb);
			b.addItemStockMatrix(cash, true, StaticValues.SM_CASH);
			cb.addItemStockMatrix(cash, false, StaticValues.SM_CASH);

			//Reserve Holdings
			Deposit res = new Deposit(bRes,(SimpleAbstractAgent)b,(SimpleAbstractAgent)cb,0);
			b.addItemStockMatrix(res, true, StaticValues.SM_RESERVES);
			cb.addItemStockMatrix(res, false, StaticValues.SM_RESERVES);

			//Bonds Holdings
			for(int j = 1 ; j<=bondMat; j++){
				Bond bond = new Bond(nbBondsPerPeriod*bondPrice, nbBondsPerPeriod, b, govt, govt.getBondMaturity(), this.iBonds, bondPrice);
				bond.setAge(j);
				b.addItemStockMatrix(bond, true, StaticValues.SM_BONDS);
				govt.addItemStockMatrix(bond, false, StaticValues.SM_BONDS);
			}
			
			//Advances			
			int aMat = b.getAdvancesLength();
			double aValue = bAdv/aMat;
			for(int j = 1 ; j <= aMat ; j++){
				Loan loan = new Loan(aValue, cb, b, this.iAdv, j, b.getAdvancesAmortizationType(), aMat);
				loan.setInitialAmount(aValue);
				b.addItemStockMatrix(loan, false, StaticValues.SM_ADVANCES);
				cb.addItemStockMatrix(loan, true, StaticValues.SM_ADVANCES);
			}

			//Expectations and Lagged Values
			
			b.addValue(StaticValues.LAG_PROFITPRETAX, bProfit*(1+distr.nextDouble()));
			b.addValue(StaticValues.LAG_PROFITAFTERTAX, bProfit*(1+distr.nextDouble()));
			b.addValue(StaticValues.LAG_NONPERFORMINGLOANS, 0*(1+distr.nextDouble()));
			b.addValue(StaticValues.LAG_REMAININGCREDIT, 0*(1+distr.nextDouble()));
			b.addValue(StaticValues.LAG_NETWEALTH, b.getNetWealth()*(1+distr.nextDouble()));
			b.addValue(StaticValues.LAG_BANKTOTLOANSUPPLY, (((csLoans+ksLoans)/bSize))*2/(lMat+1)*(1+distr.nextDouble()));
			b.addValue(StaticValues.LAG_DEPOSITINTEREST,iDep);
			b.addValue(StaticValues.LAG_LOANINTEREST,iLoans);
			double[][] bs = b.getNumericBalanceSheet();
			Expectation bDepExp = b.getExpectation(StaticValues.EXPECTATIONS_DEPOSITS);
			int nbObs = bDepExp.getNumberPeriod();
			double[][] passedbDep = new double[nbObs][2];
			double passedDebValue = bs[1][StaticValues.SM_DEP]*(1+0.05*distr.nextDouble());
			for(int j = 0; j<nbObs; j++){
				passedbDep[j][0]=passedDebValue;
				passedbDep[j][1]=passedDebValue;
			}
			bDepExp.setPassedValues(passedbDep);
			
			b.computeExpectations();
		}
		
		//Government
		//Employment
		int gManagers=(int) Math.round(gEmpl*shareManagers);
		int gTopManagers=(int) Math.round(gEmpl*shareTopManagers);
		int gWorkers=gEmpl-gManagers-gTopManagers;

		for(int i = 0 ; i < gWorkers ; i++){
			HouseholdsDifferentiated worker = (HouseholdsDifferentiated) workers.getAgentList().get(workerCounter);
			worker.setEmployer(govt);
			worker.addValue(StaticValues.LAG_EMPLOYED,1);
			govt.addEmployee(worker, StaticValues.MKT_LABOR);
			workerCounter++;
		}
		for(int i = 0 ; i < gManagers ; i++){
			HouseholdsDifferentiated manager = (HouseholdsDifferentiated) managers.getAgentList().get(managerCounter);
			manager.setEmployer(govt);
			manager.addValue(StaticValues.LAG_EMPLOYED,1);
			govt.addEmployee(manager, StaticValues.MKT_LABORMANAGERS);
			managerCounter++;
		}
		for(int i = 0 ; i < gTopManagers ; i++){
			HouseholdsDifferentiated topManager = (HouseholdsDifferentiated) topManagers.getAgentList().get(topManagerCounter);
			topManager.setEmployer(govt);
			topManager.addValue(StaticValues.LAG_EMPLOYED,1);
			govt.addEmployee(topManager, StaticValues.MKT_LABORTOPMANAGERS);
			topManagerCounter++;
		}
		
		
		//Central Bank Deposit
		Deposit govtRes = new Deposit(0,(SimpleAbstractAgent)govt,(SimpleAbstractAgent)cb,0);
		govt.addItemStockMatrix(govtRes, true, StaticValues.SM_RESERVES);
		cb.addItemStockMatrix(govtRes, false, StaticValues.SM_RESERVES);
		
		//Central Bank
		int nbBondsPerPeriod1 = (int) this.cbBonds/(bondMat*bondPrice);
		for(int j = 1 ; j<=bondMat; j++){
			Bond bond = new Bond(nbBondsPerPeriod1*bondPrice, nbBondsPerPeriod1, cb, govt, govt.getBondMaturity(), this.iBonds, bondPrice);
			bond.setAge(j);
			cb.addItemStockMatrix(bond, true, StaticValues.SM_BONDS);
			govt.addItemStockMatrix(bond, false, StaticValues.SM_BONDS);
		}
		govt.setAggregateValue(StaticValues.LAG_AGGUNEMPLOYMENT, 0.08*(1+distr.nextDouble()));//TODO
	    double initialPublicDebt=0;
	    for (Item bond:govt.getItemsStockMatrix(false, StaticValues.SM_BONDS)){
	    	initialPublicDebt+=bond.getValue();
	    }
	    //govt.getPassedValues().get(StaticValues.LAG_PUBLICDEBT).addObservation(initialPublicDebt, 0);
	    govt.addValue(StaticValues.LAG_PUBLICDEBT, initialPublicDebt);
	    govt.addValue(StaticValues.LAG_PUBLICDEBT, initialPublicDebt);
	    govt.setTaxRatesMultiplyingFactor(1);

	}

	
	
	/**
	 * @return the workersShareIncome
	 */
	public double getWorkersShareIncome() {
		return workersShareIncome;
	}



	/**
	 * @param workersShareIncome the workersShareIncome to set
	 */
	public void setWorkersShareIncome(double workersShareIncome) {
		this.workersShareIncome = workersShareIncome;
	}




	/**
	 * @return the managerShareIncome
	 */
	public double getManagerShareIncome() {
		return managerShareIncome;
	}






	/**
	 * @param managerShareIncome the managerShareIncome to set
	 */
	public void setManagerShareIncome(double managerShareIncome) {
		this.managerShareIncome = managerShareIncome;
	}






	/**
	 * @return the topManagerShareIncome
	 */
	public double getTopManagerShareIncome() {
		return topManagerShareIncome;
	}






	/**
	 * @param topManagerShareIncome the topManagerShareIncome to set
	 */
	public void setTopManagerShareIncome(double topManagerShareIncome) {
		this.topManagerShareIncome = topManagerShareIncome;
	}






	/**
	 * @return the workersShareWealth
	 */
	public double getWorkersShareWealth() {
		return workersShareWealth;
	}






	/**
	 * @param workersShareWealth the workersShareWealth to set
	 */
	public void setWorkersShareWealth(double workersShareWealth) {
		this.workersShareWealth = workersShareWealth;
	}






	/**
	 * @return the managerResearchersShareWealth
	 */
	public double getManagerResearchersShareWealth() {
		return managerResearchersShareWealth;
	}






	/**
	 * @param managerResearchersShareWealth the managerResearchersShareWealth to set
	 */
	public void setManagerResearchersShareWealth(
			double managerResearchersShareWealth) {
		this.managerResearchersShareWealth = managerResearchersShareWealth;
	}






	/**
	 * @return the topManagerShareWealth
	 */
	public double getTopManagerShareWealth() {
		return topManagerShareWealth;
	}






	/**
	 * @param topManagerShareWealth the topManagerShareWealth to set
	 */
	public void setTopManagerShareWealth(double topManagerShareWealth) {
		this.topManagerShareWealth = topManagerShareWealth;
	}






	/**
	 * @return the averageWage
	 */
	public double getAverageWage() {
		return averageWage;
	}






	/**
	 * @param averageWage the averageWage to set
	 */
	public void setAverageWage(double averageWage) {
		this.averageWage = averageWage;
	}






	/**
	 * @return the householdsCash
	 */
	public double getHouseholdsCash() {
		return householdsCash;
	}






	/**
	 * @param householdsCash the householdsCash to set
	 */
	public void setHouseholdsCash(double householdsCash) {
		this.householdsCash = householdsCash;
	}






	/**
	 * @return the householdsDeposits
	 */
	public double getHouseholdsDeposits() {
		return householdsDeposits;
	}






	/**
	 * @param householdsDeposits the householdsDeposits to set
	 */
	public void setHouseholdsDeposits(double householdsDeposits) {
		this.householdsDeposits = householdsDeposits;
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
	 * @return the ksDep
	 */
	public double getKsDep() {
		return ksDep;
	}

	/**
	 * @param ksDep the ksDep to set
	 */
	public void setKsDep(double ksDep) {
		this.ksDep = ksDep;
	}

	/**
	 * @return the ksInv
	 */
	public int getKsInv() {
		return ksInv;
	}

	/**
	 * @param ksInv the ksInv to set
	 */
	public void setKsInv(int ksInv) {
		this.ksInv = ksInv;
	}

	/**
	 * @return the ksLoans
	 */
	public double getKsLoans() {
		return ksLoans;
	}

	/**
	 * @param ksLoans the ksLoans to set
	 */
	public void setKsLoans(double ksLoans) {
		this.ksLoans = ksLoans;
	}

	/**
	 * @return the csDep
	 */
	public double getCsDep() {
		return csDep;
	}

	/**
	 * @param csDep the csDep to set
	 */
	public void setCsDep(double csDep) {
		this.csDep = csDep;
	}

	/**
	 * @return the csInv
	 */
	public int getCsInv() {
		return csInv;
	}

	/**
	 * @param csInv the csInv to set
	 */
	public void setCsInv(int csInv) {
		this.csInv = csInv;
	}

	/**
	 * @return the csLoans
	 */
	public double getCsLoans() {
		return csLoans;
	}

	/**
	 * @param csLoans the csLoans to set
	 */
	public void setCsLoans(double csLoans) {
		this.csLoans = csLoans;
	}

	/**
	 * @return the csKap
	 */
	public int getCsKap() {
		return csKap;
	}

	/**
	 * @param csKap the csKap to set
	 */
	public void setCsKap(int csKap) {
		this.csKap = csKap;
	}

	/**
	 * @return the bsBonds
	 */
	public double getBsBonds() {
		return bsBonds;
	}

	/**
	 * @param bsBonds the bsBonds to set
	 */
	public void setBsBonds(double bsBonds) {
		this.bsBonds = bsBonds;
	}

	/**
	 * @return the bsRes
	 */
	public double getBsRes() {
		return bsRes;
	}

	/**
	 * @param bsRes the bsRes to set
	 */
	public void setBsRes(double bsRes) {
		this.bsRes = bsRes;
	}

	/**
	 * @return the bsAdv
	 */
	public double getBsAdv() {
		return bsAdv;
	}

	/**
	 * @param bsAdv the bsAdv to set
	 */
	public void setBsAdv(double bsAdv) {
		this.bsAdv = bsAdv;
	}

	/**
	 * @return the bsCash
	 */
	public double getBsCash() {
		return bsCash;
	}

	/**
	 * @param bsCash the bsCash to set
	 */
	public void setBsCash(double bsCash) {
		this.bsCash = bsCash;
	}



	/**
	 * @return the ksEmpl
	 */
	public int getKsEmpl() {
		return ksEmpl;
	}

	/**
	 * @param ksEmpl the ksEmpl to set
	 */
	public void setKsEmpl(int ksEmpl) {
		this.ksEmpl = ksEmpl;
	}

	/**
	 * @return the ksSales
	 */
	public double getKsSales() {
		return ksSales;
	}

	/**
	 * @param ksSales the ksSales to set
	 */
	public void setKsSales(double ksSales) {
		this.ksSales = ksSales;
	}

	/**
	 * @return the ksProfits
	 */
	public double getKsProfits() {
		return ksProfits;
	}

	/**
	 * @param ksProfits the ksProfits to set
	 */
	public void setKsProfits(double ksProfits) {
		this.ksProfits = ksProfits;
	}

	/**
	 * @return the kPrice
	 */
	public double getkPrice() {
		return kPrice;
	}

	/**
	 * @param kPrice the kPrice to set
	 */
	public void setkPrice(double kPrice) {
		this.kPrice = kPrice;
	}

	/**
	 * @return the csEmpl
	 */
	public int getCsEmpl() {
		return csEmpl;
	}

	/**
	 * @param csEmpl the csEmpl to set
	 */
	public void setCsEmpl(int csEmpl) {
		this.csEmpl = csEmpl;
	}

	/**
	 * @return the csSales
	 */
	public double getCsSales() {
		return csSales;
	}

	/**
	 * @param csSales the csSales to set
	 */
	public void setCsSales(double csSales) {
		this.csSales = csSales;
	}

	/**
	 * @return the csProfits
	 */
	public double getCsProfits() {
		return csProfits;
	}

	/**
	 * @param csProfits the csProfits to set
	 */
	public void setCsProfits(double csProfits) {
		this.csProfits = csProfits;
	}

	/**
	 * @return the cPrice
	 */
	public double getcPrice() {
		return cPrice;
	}

	/**
	 * @param cPrice the cPrice to set
	 */
	public void setcPrice(double cPrice) {
		this.cPrice = cPrice;
	}

	/**
	 * @return the iLoans
	 */
	public double getiLoans() {
		return iLoans;
	}

	/**
	 * @param iLoans the iLoans to set
	 */
	public void setiLoans(double iLoans) {
		this.iLoans = iLoans;
	}

	/**
	 * @return the iDep
	 */
	public double getiDep() {
		return iDep;
	}

	/**
	 * @param iDep the iDep to set
	 */
	public void setiDep(double iDep) {
		this.iDep = iDep;
	}

	/**
	 * @return the bsProfits
	 */
	public double getBsProfits() {
		return bsProfits;
	}

	/**
	 * @param bsProfits the bsProfits to set
	 */
	public void setBsProfits(double bsProfits) {
		this.bsProfits = bsProfits;
	}

	/**
	 * @return the iBonds
	 */
	public double getiBonds() {
		return iBonds;
	}

	/**
	 * @param iBonds the iBonds to set
	 */
	public void setiBonds(double iBonds) {
		this.iBonds = iBonds;
	}

	/**
	 * @return the iAdv
	 */
	public double getiAdv() {
		return iAdv;
	}

	/**
	 * @param iAdv the iAdv to set
	 */
	public void setiAdv(double iAdv) {
		this.iAdv = iAdv;
	}

	/**
	 * @return the gEmpl
	 */
	public int getgEmpl() {
		return gEmpl;
	}

	/**
	 * @param gEmpl the gEmpl to set
	 */
	public void setgEmpl(int gEmpl) {
		this.gEmpl = gEmpl;
	}
	
	/**
	 * @return the kUnitCost
	 */
	public double getkUnitCost() {
		return kUnitCost;
	}

	/**
	 * @param kUnitCost the kUnitCost to set
	 */
	public void setkUnitCost(double kUnitCost) {
		this.kUnitCost = kUnitCost;
	}

	/**
	 * @return the cUnitCost
	 */
	public double getcUnitCost() {
		return cUnitCost;
	}

	/**
	 * @param cUnitCost the cUnitCost to set
	 */
	public void setcUnitCost(double cUnitCost) {
		this.cUnitCost = cUnitCost;
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
	 * @see net.sourceforge.jabm.init.AgentInitialiser#initialise(net.sourceforge.jabm.Population)
	 */
	@Override
	public void initialise(Population population) {
		MacroPopulation macroPop = (MacroPopulation) population;
		this.initialise(macroPop);
	}

	/**
	 * @return the prng
	 */
	public RandomEngine getPrng() {
		return prng;
	}

	/**
	 * @param prng the prng to set
	 */
	public void setPrng(RandomEngine prng) {
		this.prng = prng;
	}

	/**
	 * @return the uniformDistr
	 */
	public double getUniformDistr() {
		return uniformDistr;
	}

	/**
	 * @param uniformDistr the uniformDistr to set
	 */
	public void setUniformDistr(double uniformDistr) {
		this.uniformDistr = uniformDistr;
	}
	/**
	 * @return the kOCF
	 */
	public double getKsOCF() {
		return ksOCF;
	}

	/**
	 * @param ksOCF the kOCF to set
	 */
	public void setKsOCF(double ksOCF) {
		this.ksOCF = ksOCF;
	}

	/**
	 * @return the cOCF
	 */
	public double getCsOCF() {
		return csOCF;
	}

	/**
	 * @param csOCF the cOCF to set
	 */
	public void setCsOCF(double csOCF) {
		this.csOCF = csOCF;
	}

	/**
	 * @return the gr
	 */
	public double getGr() {
		return gr;
	}

	/**
	 * @param gr the gr to set
	 */
	public void setGr(double gr) {
		this.gr = gr;
	}

	/**
	 * @return the cbBonds
	 */
	public double getCbBonds() {
		return cbBonds;
	}

	/**
	 * @param cbBonds the cbBonds to set
	 */
	public void setCbBonds(double cbBonds) {
		this.cbBonds = cbBonds;
	}

	/**
	 * @return the ksLoans0
	 */
	public double getKsLoans0() {
		return ksLoans0;
	}

	/**
	 * @param ksLoans0 the ksLoans0 to set
	 */
	public void setKsLoans0(double ksLoans0) {
		this.ksLoans0 = ksLoans0;
	}

	/**
	 * @return the csLoans0
	 */
	public double getCsLoans0() {
		return csLoans0;
	}

	/**
	 * @param csLoans0 the csLoans0 to set
	 */
	public void setCsLoans0(double csLoans0) {
		this.csLoans0 = csLoans0;
	}


	
	/**
	 * @return the capitalLaborRatio
	 */
	public double getCapitalLaborRatio() {
		return capitalLaborRatio;
	}

	/**
	 * @param capitalLaborRatio the capitalLaborRatio to set
	 */
	public void setCapitalLaborRatio(double capitalLaborRatio) {
		this.capitalLaborRatio = capitalLaborRatio;
	}
	
	
	

	
}
