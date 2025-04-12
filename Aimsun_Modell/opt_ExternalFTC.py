# Optimization of external (API programmed) fixed time controllers

import sys

from PyANGApp import *
from PyANGBasic import *
from PyANGKernel import *
from PyANGAimsun import *
from OTCScriptSupport6 import *

# Initialize the simulation environment
def initSim(networkFile):
    receivedFile = recv()
    if (receivedFile != "NONE"):
        networkFile = receivedFile
    
    if app.open(networkFile):
        print "Opened " + networkFile
        send("ANGFILE_OK\n")
        
        model = app.getModel()
        if (model != None):
            catalog = model.getCatalog();

            replicationID, nodeID, simStart, simulationDuration, warmDur, pointInTime = receiveOptTaskData(catalog)
    
            global replication
            replication = catalog.find(replicationID)    
            experiment = replication.getExperiment()
            scenario = experiment.getScenario()
            scenarioData = scenario.getInputData()
            
            # Add API-DLL
            scenarioData.removeExtensions()
            scenarioData.addExtension("AAPI_D.dll")
            
            # Set up database
            addResultDB(scenario, "Layer2.mdb", simulationDuration)
    
            # Start a simulation to call API-DLL.init()
            # TODO: Simulationsdauer abkuerzen!
            simulateReplication(replication, app)
               
            # Set warm up period for simulation
            setWarmUpTime(warmDur, experiment)
        
            # Set up traffic demands for simulation
            setupTrafficDemand(app, scenario, simStart + pointInTime, simulationDuration)
                
            # Adjust duration of MasterControlPlan to duration of new demand
            setupMCP(app, scenario, simStart + pointInTime, simulationDuration)
    
            send("INIT_DONE\n")
    else:
        send("ANGFILE_NOT_OK\n")
        print "Network could not be opened."
        sys.exit()

    
# Start optimization
def startOpt(networkFile):
    # Initialize simulation environment
    initSim(networkFile)
      
    while True:
        received = recv()
        # Check if further simulations are necessary
        if (received.startswith("DONE")) :
            print "Received stop signal from EA"
            app.close()
            # Restart optimization
            startOpt(networkFile)

        # new generation starts, the random seed must be changed
        elif (received.startswith("NEW_GEN")) :
            print "Evaluating new generation"
            send("NEW_GEN_RECV\n")
            randno = int(recv())
            replication.setRandomSeed(randno)
            print "Random seed set to %i." % replication.getRandomSeed()
            send("SEED_SET\n")

        # new individuum must be evaluated
        elif (received.startswith("NEW_IND")) :
            # Evaluate the next individuum
            # TLC provided by API
            simulateReplication(replication, app)
            send("READY\n")

        else:
            print "Socket protocol error: Received " + received + ", expected DONE, NEW_GEN or NEW_IND."         
        

# Obtain network file from command line argument
if (len(sys.argv) != 2):
    print "Usage: opt_ExternalFTC <filename.ang>"
    sys.exit(-1)
else:
    networkFile = sys.argv[1]
    if (networkFile.endswith(".ang") == False):
        networkFile = networkFile + ".ang"       

startJVM("de/dfg/oc/otc/layer2/ea/EAExternalFTC")

createSocket(1234)
    
# Start AIMSUN
global app
app = ANGApp()

# app.initGui()
app.initConsole()

startOpt(networkFile)