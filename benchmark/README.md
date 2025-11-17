# Java Macro Agent-Based (JMAB) toolkit - "Benchmark" model Version 2.0 

##Overview

This project is a complete overhaul of the <a href="https://github.com/S120/benchmark">JMAB benchmark model</a> by Caiani et al. (2016, Journal of Economic Dynamics and Control (69), pp. 375–408).

The original model shows fundamental problems in every agent behavior:
- Agents use exponential smoothing to calculate their expectations despite not being suitable for the model.
- Firms ignore the prices of competitors in their own price calculation.
- The investment of firms create feedback-loops which prevents the model from reaching a steady state.
- Loan interest setting of banks make them constantly go bankrupt.
- Loan approval decision of banks ignore their funding costs.
- Probability of default is set too high and does not consider capital expenditure of consumption firms.
- Deposit interest setting of banks is counter empirical and reduces their profits.
- Several errors in the calibration.

As result the model produces unplausible outputs, reaches no steady-state, and always crashes after 1000 periods.

The improved version JMAB 2.0 model in this repository fixes all these shortcomings and produces only plausible results. Among the improvements are: 

- Expectations: Firms use a simpler and more suitable method (rule based expectation) to calculate expectations. Other agents use double exponential smoothing.
- Pricing: Firms consider the prices of their competitors, which makes their marketshares more homogenous.
- Investment: The investment of firms is rougly fixed and is only slowly adjusted, which significantly reduces volatility.
- Loan and deposit mechanisms: Banks set their loans interest rates based on funding costs and demand. The deposit interest setting was overhauled and is in line with empirical data.
- Loan approval: Banks now consider funding costs and calculate the probability of default of firms using the Debt Service Coverage Ratio.
- Execution time: JMAB 2.0 runs about 5 times faster then the original one.

##Prerequisites

JMAB requires Java version 6 or later. It has been tested against version 1.6.0_35 and 1.7.0_75.

Note that on Mac OS, you will need to use the Oracle version of Java instead of the default one shipped with the OS.

##Installation

This repository can be directly imported in Eclipse with the URI: https://github.com/simonhess/benchmark2.git and the "Clone Submodules" option being on.

Alternatively, clone this repository recursively and import it in Eclipse as an local Git repository:

```
git clone --recursive https://github.com/simonhess/benchmark2.git
```

##Use the model

Use Eclipse and run the Main.java class in the /src/benchmark2 folder with the VM arguments "-Djabm.config=Model/mainBenchmark.xml -Xmx4G -Xms4G" . In order to do so open the "Run configurations..." menu in Eclipse in the dropdown menu of the Run-Button, go to the arguments tab and add the arguments in the VM arguments section.

##Replicate the JMAB 2.0 paper results

To replicate the results of the JMAB 2.0 paper perform the following steps:

1. Change the number of simulations in the /Model/mainBenchmark.xml file to 100:

```
<property name="numSimulations" value="100"/>	
```

2. Run the simulation (Needs about 13 hrs) with the VM arguments "-Djabm.config=Model/mainBenchmark.xml -Xmx4G -Xms4G".

3. Clone the JMAB 1.0 data and unzip all files in the benchmark2_supp_data/JMAB1_data to retrieve the JMAB 1.0 data R workspace.

```
git clone https://github.com/simonhess/benchmark2_supp_data.git
```
4. Remove the comment symbol "#" in the following lines in the /data/BaselineAnalysisWithComparison.R file (Lines 11-13):

```
#source("MergeMonteCarloSim.R")
#generateMergedCSV(folder)
#generateSums(folder)
```

5. Run the BaselineAnalysisWithComparison.R file to create the plots.

The plots will be created in the /data folder.

##Custom calibration

To change the calibration of the model edit the .ipynb file in the "/Calibration" folder and execute the file with the SageMath Software (<a href="https://www.sagemath.org/">). The calibration should print a calibration xml block into the .ipynb file which can be copied into the /Model/modelBenchmark.xml file.

##Documentation

The folder documentation in the <a href="https://github.com/S120/jmab">JMAB project</a> contains a user guide.
