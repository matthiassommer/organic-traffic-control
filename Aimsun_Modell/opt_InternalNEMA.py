# Optimization of AIMSUN's internal NEMA controllers

import sys

from PyANGApp import *
from PyANGBasic import *
from PyANGKernel import *
from PyANGAimsun import *
from OTCScriptSupport6 import *

def initSim(networkFile):    
    receivedFile = recv()
    if (receivedFile != "NONE"):
        networkFile = receivedFile
    
    if app.open(networkFile):        
        send("ANGFILE_OK\n")
        
        model = app.getModel()
        if (model != None):
            catalog = model.getCatalog();
      
            replicationID, nodeID, simStart, simulationDuration, warmDur, pointInTime = receiveOptTaskData(catalog)
    
            # Get replication 
            global replication
            replication = catalog.find(replicationID)
            experiment = replication.getExperiment()
            scenario = experiment.getScenario()
            scenarioData = scenario.getInputData()
    
            # Remove API-DLLs from scenario 
            scenarioData.removeExtensions()
    
            addResultDB(scenario, "Layer2.mdb", simulationDuration)
    
            setWarmUpTime(warmDur, experiment)            
            setupTrafficDemand(app, scenario, simStart + pointInTime, simulationDuration)
          
            # Adjust duration of MasterControlPlan to duration of new demand 
            setupMCP(app, scenario, simStart + pointInTime, simulationDuration)

            # Determine turning ids for junction 
            node = catalog.find(nodeID)
            idString = determineMotorizedTurningsForJunction(node, catalog)
            
            # Send turning ids to JAVA 
            sendTurningsForJunction(idString)
    
            # Retrieve newly created controlplan 
            controlPlan = getActiveControlPlanItem(scenarioData, simulationDuration).getControlPlan()
    
            # Get control program for node 
            global controlJunction
            controlJunction = controlPlan.getControlJunction( nodeID )
    
            # Check if node is set to fixedtime control 
            # 0 = eUnspecified / 1 = eUncontrolled / 2 eFixedControl / 3 eExternal / 4 eActuated
            if (controlJunction.getControlJunctionType() != 4):
                print "WARNING: ControlType of junction %i is not eActuated." % nodeID
    
            # Determine number of (inter)phases 
            global phases
            phases = controlJunction.getPhases()

            global nonInterphases
            nonInterphases = []
            for item in phases:
                if (item.getInterphase() == 0):
                    nonInterphases.append(item)
    
            # send to EA
            received = recv()
            if (received.startswith("WAITING_FOR_PHASES")):
                # Send number of phases
                send(str(numberOfPhases) + "\n")
                
                # Send duration of interphases 
                # or "-1" if phase is not an interphase 
                # and should be considered for optimization 
                for item in phases:
                    received = recv()
                    if (received.startswith("WAITING_NEXT_PHASE")):
                        if (item.getInterphase()):
                            send(str(item.getDuration()) + " IP\n")
                        else:
                            send(str(item.getDuration()) + " nonIP\n")
                    else:
                        print "Socket protocol error: Received " + received + ", expected WAITING_NEXT_PHASE." 
            else:
                print "Socket protocol error: Received " + received + ", expected WAITING_FOR_PHASES." 
    else:
        send("ANGFILE_NOT_OK\n")
        print "Network could not be opened."
        sys.exit()

def startOpt(networkFile):    
    initSim(networkFile)
    
    while True:
        received = recv()

        ## Check if further simulations are necessary
        if (received.startswith("DONE")) :
            print "Received stop signal from EA "
            # closeSocket()   
            app.close()
            # Restart optimization 
            startOpt(networkFile)

        ## or a new generation starts, so the random seed must be changed
        elif (received.startswith("NEW_GEN")) :
            send("NEW_GEN_RECV\n")
            randno = int(recv())
            replication.setRandomSeed(randno)
            print "Random seed set to %i." % replication.getRandomSeed()
            send("SEED_SET\n")

        elif (received.startswith("NEW_IND")) :
            # Evaluate the next individuum
            # Receive phase durations and calculate cycle time
            durations = []
            # For each noninterphase 
            for i in range(0, len(nonInterphases)):
                #  receive mingreen, maxinitgreen, maxout, secs/actuation, passage time
                for j in range(0, 5):
                    send("NEXT_ALLELE\n")
                    dur = int(recv())
                    durations.append(dur)
            
            # Update phase durations
            j = 0
            for i in range(0, len(phases)):
                phase = controlJunction.getPhaseByPos(i)                    
                # NonInterphase? -> Set values 
                if (phase.getInterphase() == 0):
                    phase.setMinDuration(durations[j])
                    j = j + 1
                    phase.setMaximumInitial(phase.getMinDuration() + durations[j])
                    j = j + 1
                    phase.setMaxDuration(phase.getMaximumInitial() + durations[j])
                    j = j + 1
                    phase.setSecondsActuation(durations[j])
                    j = j + 1
                    phase.setPassageTime(durations[j])
                    j = j + 1
                       
            # Replication to simulate
            simulateReplication(replication, app)
            send("SIM_DONE\n")
        else:
            print "Socket protocol error: Received " + received + ", expected DONE, NEW_GEN or NEW_IND." 

# Obtain network file from command line argument 
if (len(sys.argv) != 2):
    print "Usage: opt_InternalNEMA <filename.ang>"
    sys.exit(-1)
else:
    networkFile = sys.argv[1]
    if (networkFile.endswith(".ang") == False):
        networkFile = networkFile + ".ang"

startJVM("de/dfg/oc/otc/layer2/ea/EAInternalNEMA")

createSocket(1234)
    
# Start AIMSUN 
global app
app = ANGApp()

# app.initGui()
app.initConsole()

startOpt(networkFile)
