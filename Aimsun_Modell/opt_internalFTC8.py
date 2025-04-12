# Optimization of internal fixed time controllers
import sys
import shutil

from PyANGBasic import *
from PyANGConsole import *
from PyANGKernel import *
from PyANGAimsun import *
from OTCScriptSupport8 import *
  
def initSim(networkFile):
    receivedFile = recv()
    if (receivedFile != "NONE"):
        networkFile = receivedFile
    	
    if app.open(networkFile):       
        print "Opened " + networkFile
        send("ANGFILE_OK\n")
        
        model = app.getModel()
        if (model != None):
            global catalog
            catalog = model.getCatalog();
            
            # Receive OptTaskData from JAVA
            replicationID, nodeID, simStart, simulationDuration, warmDur, pointInTime, situation, sectionIds = receiveOptTaskData(catalog)

            global replication
            replication = catalog.find(replicationID)
            experiment = replication.getExperiment()
            scenario = experiment.getScenario()
            scenarioData = scenario.getInputData()
            
            # Remove API-DLLs from scenario
            scenarioData.removeExtensions()
            
            # Set result database
            if dbType == "Access":            
                dbFileName = "Layer2_" + str(id) + ".mdb"
                shutil.copyfile("Layer2.mdb", dbFileName)
            else:
                dbFileName = "DB_L2_" + str(id)
            addResultDB(scenario, dbType, dbFileName, simulationDuration)
            
            setWarmUpTime(warmDur, experiment)
            
            # Set up traffic demands for simulation
            # use point in time if situation data not available
            if (len(situation) == 0 or len(sectionIds) == 0 or 2 * len(situation) != len(sectionIds)):
                setupTrafficDemand(app, scenario, simStart + pointInTime, simulationDuration)
            # otherwise use situation data
            else:
                layer2Demand = createNewDemand(app)
                odMarix = createMatrix(scenario, model, situation, sectionIds)
                addMatrixToDemand(odMarix, layer2Demand, simulationDuration)
                scenario.setDemand(layer2Demand)
          
            # Adjust duration of MasterControlPlan to duration of new demand
            setupMCP(app, scenario, simStart + pointInTime, simulationDuration)
    
            # Retrieve newly created controlplan
            global controlPlan
            controlPlan = getActiveControlPlanItem(scenario, simulationDuration).getControlPlan()
            
            # Modify policies so that incidents are simulated on Layer 2
            modifyActivePoliciesForLayer2(simStart + pointInTime, experiment)
                      
            # Determine turning ids for junction
            node = catalog.find(nodeID)
            idString = determineMotorizedTurningsForJunction(node, catalog)
            
            sendTurningsForJunction(idString)        
            determineJunctionData(nodeID)
            sendJunctionData()
    else:
        send("ANGFILE_NOT_OK\n")
        print "Network could not be opened: %s" % networkFile
        sys.exit()
            
# Determine junction data (length of interphases etc.)
def determineJunctionData(nodeID):
    # Get control program for node
    global controlJunction
    controlJunction = controlPlan.getControlJunction(nodeID)
    if (controlJunction == None):
        print "ERROR: Could not find a controlled node with ID %i in the model." %nodeID
        exit()
    
    # Check if node is set to fixedtime or external control
    # 0 = eUnspecified / 1 = eUncontrolled / 2 eFixedControl / 3 eExternal / 4 eActuated
    ctrlType = controlJunction.getControlJunctionType() 
    if (ctrlType == 0 or ctrlType == 1 or ctrlType == 4):
        print "WARNING: ControlType of junction %i is not FixedTime or External." % nodeID
        
    global phases
    phases = controlJunction.getPhases()
    numberOfPhases = len(phases)
            
    global numberOfNonInterphases
    numberOfNonInterphases = 0

    # Determine (sum of) interphase durations
    global interphaseDurationSum
    interphaseDurationSum = 0
    interphaseDurations = []
           
    for item in phases:
        if (item.getInterphase()):
            duration = item.getDuration()
            interphaseDurationSum = interphaseDurationSum + item.getDuration()
            interphaseDurations.append(duration)
        else:
            numberOfNonInterphases = numberOfNonInterphases + 1        
        
# Send junction data determined by "determineJunctionData(nodeID)" via socket to JAVA
def sendJunctionData():
    numberOfPhases = len(phases)
                       
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

def startOpt(networkFile):  
    initSim(networkFile)
    
    while True:
        received = recv()
        # Check if further simulations are necessary
        if (received.startswith("DONE")) :           
            app.close()
            # restart
            startOpt(networkFile)
    
        # or a new generation starts, so the random seed must be changed
        elif (received.startswith("NEW_GEN")) :
            send("NEW_GEN_RECV\n")
            randno = int(recv())
            replication.setRandomSeed(randno)
            print "Random seed set to %i." % replication.getRandomSeed()
            send("SEED_SET\n")
            
        # or a new solution needs to be evaluated
        elif (received.startswith("NEW_IND")) :
            # Evaluate the next individual
            # Receive phase durations and calculate cycle time
            durations = []
            
            # Sum of non interphase durations (for calculating the cycle time)
            nonInterphaseDurationSum = 0
            for i in range(0, numberOfNonInterphases):
                send("NEXT_ALLELE\n")
                dur = int(recv())
                nonInterphaseDurationSum = nonInterphaseDurationSum + dur
                durations.append(dur)
                           
            # Update cycle time
            cycleTime = nonInterphaseDurationSum + interphaseDurationSum
            controlJunction.setCycle(cycleTime)
                       
            # Update phase durations
            startTime = 0
            j = 0
            for i in range(0, len(phases)):
                phase = controlJunction.getPhaseByPos(i)                    
                phase.setFrom(startTime)
                # Interphase? -> Keep duration unchanged
                if (phase.getInterphase() == 0):
                    phase.setDuration(durations[j])
                    startTime = startTime + durations[j]  
                    j = j + 1
                else:
                    startTime = startTime + phase.getDuration()
                                           
            simulateReplication(replication, app)
            send("SIM_DONE\n")
        else:
            print "Socket protocol error: Received " + received + ", expected DONE, NEW_GEN or NEW_IND." 


# Obtain network file from command line argument
global id

if (len(sys.argv) == 1):
    networkFile = "NOT SPECIFIED"
    id = 1
elif (len(sys.argv) == 2):
    networkFile = sys.argv[1]
    if (networkFile.endswith(".ang") == False):
        networkFile = networkFile + ".ang"
    id = 1
elif (len(sys.argv) == 3):
    networkFile = sys.argv[1]
    if (networkFile.endswith(".ang") == False):
        networkFile = networkFile + ".ang"
    id = int(sys.argv[2])
else:
    print "Usage: opt_InternalFTC8 <filename.ang> <id>"
    sys.exit(-1)


# Database type (SQL server or Access file)
dbType = "SQL"
#dbType = "Access"

# Copy access database files
if dbType == "Access":
    dbFileName = "Layer2_" + str(id) + ".mdb"
    shutil.copyfile("Layer2.mdb", dbFileName)

port = 1234 + id
createSocket(port)
   
# Start AIMSUN
global app
app = ANGConsole()

startOpt(networkFile)
