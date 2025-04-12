# This module contains all relevant methods for AIMSUN scripts used in OTC.

from PyANGBasic import *
from PyANGKernel import *
from PyANGGui import *
from PyANGConsole import *
from PyANGAimsun import *
from socket import *

import time
import os

# Create socket and wait for connection
def createSocket(port):
    global s, conn, addr
    s=socket(AF_INET, SOCK_STREAM)
    s.bind(("", port))
    print "Waiting for incoming connection on port %i" % port
    s.listen(1)
    conn, addr = s.accept()
    conn.setblocking(1)
    
def send(message):
    conn.send(message)
    
def recv():
    return conn.recv(1024)

def closeSocket():
    conn.close()
    s.close() 


# Set warm up time.
# Parameters: warmDur - warm up time (in secs)
#             experiment - the experiment where the time will be set
def setWarmUpTime(warmDur, experiment):
    warmDurHour = warmDur / 3600
    warmDurMin = (warmDur % 3600) / 60
    warmDurSec = warmDur - warmDurHour * 3600 - warmDurMin * 60
    experiment.setWarmupTime(GKTimeDuration(warmDurHour, warmDurMin, warmDurSec))

# Set an result database for the given scenario.
# A filename of an existing access-database (.mdb).
# The .mdb-file will /not/ be created here.
def addResultDB(scenario, dbType, dbFile, statIntervall):
    info = GKDataBaseInfo()
    info.setDatabaseName(dbFile)
    if dbType == "Access":
        info.setDriverName("Access");
    else:
        info.setDriverName("ODBC");
        info.setUserName("otc")
        info.setPassword("layer2")
    
    scenarioData = scenario.getInputData()
    scenario.setDB(info)
            
    # Which  stastistical information should be stored to db-file
    scenarioData.enableStoreStatistics (1)
    scenarioData.enableStoreDetection (0)

    # do collect statistical information for sections (used by Layer 1)
    scenarioData.setSectionsStatistics (1)
    scenarioData.setTurnsStatistics (1)

    # do not collect statistical information for O/D-matrices
    scenarioData.setMatrixStatistics (0)

    # do not collect statistical information for public transport
    scenarioData.setPTStatistics (0)

    # and set statistics interval to simulation duration
    intervallDuration = GKTimeDuration(secondsToQTime(statIntervall))
    scenarioData.setStatisticalInterval(intervallDuration)  

# Converts a QTime object to seconds
def convertTime(qtime):
    return qtime.hour() * 3600 + qtime.minute() * 60 + qtime.second()

# Converts seconds into a QTime object
def secondsToQTime(dur):
    hour = dur / 3600
    minute = (dur % 3600) / 60
    second = dur - hour *3600 - minute * 60
    qtime = QTime()
    qtime.setHMS(hour, minute, second, 0)
    return qtime
   
# Simulate the given replication
def simulateReplication(replication, gui):
    if replication != None:
        plugin = GKSystem.getSystem().getPlugin("GGetram")
        simulator = plugin.getCreateSimulator(gui.getModel())
        
        if simulator.isBusy() == False: 
            simulator.setModel(gui.getModel())
            simulator.addSimulationTask(replication, GKReplication.eBatch)
            simulator.simulate()
    else:
        print "Replication not found"

# Opens a network and returns the active gui / console 
# and a boolean value that tells if aimsun_ng or angconsole is running
# Parameters: filename - name of the network that should be loaded
# Return: the active gui / console 
#         and 
#         True if aimsun_ng is running or False if angconsole is running
def loadNetwork(filename):
    # AIMSUN_NG?
    gui = GKGUISystem.getGUISystem().getActiveGui()
    if gui != None:
        isGuiAvailable = True
        if gui.loadNetwork(filename) == False:
            print "[AIMSUN_NG] Could not open network file."

    # ANGCONSOLE?
    else:
        gui = ANGConsole()
        isGuiAvailable = False
        if gui.open(filename) == False:
            print "[ANGCONSOLE] Could not open network file."
    return gui, isGuiAvailable

# Receives data contained in an OptTask, i.e. a replicationID, a pointInTime, and a nodeID.
# The received data is checked and returned.
# Parameters: catalog - AIMSUNs catalog
# Return: the replicationID
#         the pointInTime
#         the nodeID
def receiveOptTaskData(catalog):
    replicationID = int(recv())
    replication = catalog.find(replicationID)
    if (replication == None):
        print "ERROR: Received invalid replication ID. ID %i is not present in the network." % replicationID
        send("REPLICATION_ID_INVALID\n")
    elif (replication.isA("GKReplication") == 0):
        print "ERROR: Received invalid replication ID. ID %i is no GKReplication." % replicationID
        send("REPLICATION_ID_INVALID\n")
    else :
        send("REPLICATION_ID_OK\n")
         
    experiment = replication.getExperiment()
    scenario = experiment.getScenario()
        
    # Receive amount of time passed since start of simulation
    pointInTime = float(recv())
    demand = scenario.getDemand()
    if (pointInTime <= convertTime(demand.duration())):
        send("TIME_OK\n")
    else:
        print "ERROR: Received wrong pointInTime (%i)" % (pointInTime)
        send("TIME_INVALID\n")
        
    # Determine start time of simulation
    demandStart = convertTime(demand.initialTime())
        
    # Receive nodeID
    nodeID = int(recv())
    node = catalog.find(nodeID)
    if (replication == None):
        print "ERROR: Received invalid node ID. ID %i is not present in the network." % nodeID
        send("NODE_ID_INVALID\n")
    elif (node.isA("GKNode") == 0):
        print "ERROR: Received invalid node ID. ID %i is no GKNode." % nodeID
        send("NODE_ID_INVALID\n")
    else :
        send("NODE_ID_OK\n")
        
    # Receive simulation duration
    try:
        recvString = recv()
        simDur = int(recvString)
        send("SIMDUR_OK\n")
    except:
        print "ERROR: Simulation duration invalid." + recvString
        send("SIMDUR_INVALID\n")
        
    # Receive warm up duration
    try:
        recvString = recv()
        warmDur = int(recvString)
        send("WARMDUR_OK\n")
    except:
        print "ERROR: Warmup duration invalid." + recvString
        send("WARMDUR_INVALID\n")
        
    # Receive situation
    situation = []
    try:
        cont = True
        while(cont):
            recvString = recv()
            if (recvString != "SITUATION_DONE"):
                situation.append(float(recvString))
                send("SITUATION_ENTRY_OK\n")
            else:
                cont = False
                send("SITUATION_DONE_OK\n")
        print situation
    except:
        print "ERROR: Situation invalid."
        send("SITUATION_INVALID\n")

    # Receive situation
    sectionIds = []
    try:
        cont = True
        while(cont):
            recvString = recv()
            if (recvString != "SECTION_IDS_DONE"):
                sectionIds.append(int(recvString))
                send("SECTION_ID_OK\n")
            else:
                cont = False
                send("SECTION_IDS_DONE_OK\n")
        print sectionIds
    except:
        print "ERROR: Section Ids invalid."
        send("SECTION_IDS_INVALID\n")
        
    print "Replication %i, node %i, pointInTime %.2f." % (replicationID, nodeID, pointInTime)
    print "Simulating %.2f sec., warmUpTime %.2f sec." %(simDur, warmDur) 
    return replicationID, nodeID, demandStart, simDur, warmDur, pointInTime, situation, sectionIds

##########################################################
# Turnings

# Determine all signalized turnings for the node object given as parameter.
# Each turning is represented as an id tuple (fromSectionID, toSectionID).    
def determineAllTurningsForJunction(node):
    turnings = node.getTurnings()
    idString = ""
    for turning in turnings:
        # Is turning signalized?
        if node.getNumberSignals(turning) > 0:
            fromSectionId = turning.getOrigin().getId()
            toSectionId = turning.getDestination().getId()
            idString += str(fromSectionId) + " "
            idString += str(toSectionId) + " "
    return idString

# Determine signalized turnings the node object given as parameter. Only turnings 
# for motorized traffic (inbound section != footpath) will be returned. Each turning 
# is represented as an id tuple (fromSectionID, toSectionID).    
def determineMotorizedTurningsForJunction(node, catalog):
    turnings = node.getTurnings()
    idString = ""

    for turning in turnings:
        # Is turning signalized?
        if node.getNumberSignals(turning) > 0:
            fromSectionId = turning.getOrigin().getId()
            toSectionId = turning.getDestination().getId()

            # Determine type of start section
            fromSection = catalog.find(fromSectionId)
            attr = fromSection.getType().getColumn("GKSection::roadTypeAtt", GKType.eSearchOnlyThisType)
            roadtype = fromSection.getDataValueString(attr)

            # and add only non footpaths.
            if (roadtype != "Footpath"):
                idString += str(fromSectionId) + " "
                idString += str(toSectionId) + " "
    return idString

# Determine signalized turnings the node object given as parameter. Only turnings 
# for pedestrians (inbound section == footpath) will be returned. Each turning 
# is represented as an id tuple (fromSectionID, toSectionID).    
def determinePedestrianTurningsForJunction(node, catalog):
    turnings = node.getTurnings()
    idString = ""

    for turning in turnings:
        # Is turning signalized?
        if node.getNumberSignals(turning) > 0:
            fromSectionId = turning.getOrigin().getId()
            toSectionId = turning.getDestination().getId()

            # Determine type of start section
            fromSection = catalog.find(fromSectionId)
            attr = fromSection.getType().getColumn("GKSection::roadTypeAtt")
            roadtype = fromSection.getDataValue(attr).toString()

            # and add only non footpaths.
            if (roadtype == "Footpath"):
                idString += str(fromSectionId) + " "
                idString += str(toSectionId) + " "
    return idString


# Send the turningIds determined by "determineTurningsForJunction(nodeId)" via socket to JAVA. 
def sendTurningsForJunction(idString):
    received = recv()
    if (received.startswith("WAITING_FOR_TURNINGS")):
        send(idString + "\n") 
    else:
        print "Socket protocol error: Received " + received + ", expected WAITING_FOR_TURNINGS." 
     
##########################################################
# Control plans

# Creates a new MasterControlPlan and adds it to the appropriate folder
# Parameters: gui - the active gui / console
# Returns: the newly created master control plan
def createNewMCP(gui):
    model = gui.getModel()

    # Create new MasterControlPlan
    newMCP = GKSystem.getSystem().newObject("GKMasterControlPlan", model)
    newMCP.setName("Layer2MasterControlPlan")

    externalFolderName = "Master Control Plans" 
    folderName = "GKModel::masterControlPlans" 
    type = model.getType("GKMasterControlPlan") 
    folder = model.getCreateRootFolder().findFolder(folderName) 
    if folder == None: 
        folder = model.getCreateRootFolder().createFolder(externalFolderName, folderName, type)
    folder.append(newMCP)

    return newMCP
    
# Returns the GKScheduleMasterControlPlanItem that is active at the given absolute(!) point in time (in seconds)
def getActiveControlPlanItem(scenario, time):
    activeMCP = scenario.getMasterControlPlan()
    mcpScheduleItems = activeMCP.getSchedule()

    for item in mcpScheduleItems:
        itemStart =  item.getFrom()
        itemEnd   =  itemStart + item.getDuration()
        if ((itemStart <= time) & (itemEnd >= time)):
            print "Control plan for time %.2f obtained (plan starts %.2f and ends %.2f)." % (time, itemStart, itemEnd)
            # There is exactly one GKScheduleMasterControlPlanItem for each point in time.
            return item

    print "[getActiveControlPlanItem] GKScheduleMasterControlPlanItem for time %i could not be obtained." % time
    return None

# Set up a new MasterControlPlan
# Parameters: gui - the active gui (aimsun_ng) or console (angconsole) resp.
#             scenario - the scenario
#             time - the absolute(!) point in time for which the control plan is copied to the new master control plan
#             duration - the duration of the new master control plan
# Comment: The master control plan is changed to the newly created 
#          master control plan for the given scenario!
def setupMCP(gui, scenario, time, duration):
    # Determine active control plan
    cpItem = getActiveControlPlanItem(scenario, time)
       
    newMCP = createNewMCP(gui)
    # create new schedule item and store it in the new MCP
    newCpItem = GKScheduleMasterControlPlanItem()
    newCpItem.setFrom(convertTime(newMCP.initialTime()))
    newCpItem.setDuration(duration)
    newCpItem.setControlPlan(cpItem.getControlPlan())
    newMCP.addToSchedule(newCpItem)
    
    scenario.setMasterControlPlan(newMCP)
    
#######################################################################
# Traffic demands -- by point in time

# Creates a new traffic demand and adds it to the appropriate folder
# Parameters: gui - the active gui / console
# Returns: the newly created traffic demand
def createNewDemand(gui):
    model = gui.getModel()

    newDemand = GKSystem.getSystem().newObject("GKTrafficDemand", model)
    newDemand.setName("Layer2Demand")

    externalFolderName = "Traffic Demand" 
    folderName = "GKModel::trafficDemand" 
    type = model.getType("GKTrafficDemand") 
    folder = model.getCreateRootFolder().findFolder(folderName) 
    if folder == None: 
        folder = model.getCreateRootFolder().createFolder(externalFolderName, folderName, type)
    folder.append(newDemand)
    return newDemand

# Returns a list of GKScheduleDemandItems that are active at the given point in time (in seconds)
def getDemandItemList(scenario, time):
    demand = scenario.getDemand()
    schedule = demand.getSchedule()

    demandItemList = []
    for item in schedule:
        itemStart =  item.getFrom() # - demandStart
        itemEnd   =  itemStart + item.getDuration()
        if ((itemStart <= time) & (itemEnd > time)):
            print "Demand for time %.2f obtained (demand starts %.2f and ends %.2f)." % (time, itemStart, itemEnd)
            demandItemList.append(item)
    return demandItemList
    
# Set up a new traffic demand
# Parameters: gui - the active gui (aimsun_ng) or console (angconsole) resp.
#             scenario - the scenario
#             time - the absolute(!) point in time for which the demand items are is copied to the new demand
#             duration - the duration of the new demand
# Comment: The traffic demand is changed to the newly created 
#          demand for the given scenario!
def setupTrafficDemand(gui, scenario, time, duration):  
    # Determine all relevant demand items
    demandList = getDemandItemList(scenario, time)

    newDemand = createNewDemand(gui)
    # copy relevant demand items and store copies in the new demand
    for item in demandList:
        newItem = GKScheduleDemandItem()
        initialDuration = float(item.getDuration())
        newItem.setFrom(convertTime(newDemand.initialTime()))
        newItem.setDuration(duration)

        # Set scale factor if demand uses OD matrices
        if (item.getTrafficDemand().isA("GKODMatrix")):
            newItem.setFactor(str(duration / initialDuration * 100))
        else:
            newItem.setFactor(item.getFactor())
        newItem.setTrafficDemand(item.getTrafficDemand())
        newDemand.addToSchedule(newItem)  # item is copied! (see ScriptingDocs)

    # Check newly created demand
    if ((newDemand.overlappedItems()) or (not newDemand.isValid())):
        print "[setupTrafficDemand] Trafficdemand created by setupTrafficDemand is not valid."  
  
    # Set new demand in scenario
    scenario.setDemand(newDemand)

#######################################################################
# Traffic demands -- by situation

# Creates an O/D-matrix with the traffic demand defined by the flow values given in the 'situation' array.
# The relevant centroids for the flows are determined by the section ids contained in the resp. array.
def createMatrix(scenario, model, situation, sectionIds):
    # Create O/D-matrix
    matrix = GKSystem.getSystem().newObject("GKODMatrix", model)
    matrix.setName("OD-Matrix for Layer 2")
    
    # Add to active centroid configuration
    catalog = model.getCatalog()
    centroidConfType = model.getType("GKCentroidConfiguration") 
    centroidConfIds = catalog.getObjectsByType(centroidConfType)

    for centroidConfId in centroidConfIds:
        centroidConf = catalog.find(centroidConfId)
        if (centroidConf.isActive()):
            activeConf = centroidConf
    print "Active centroid configuration: %i" % activeConf.getId()
    centroidConf.addODMatrix(matrix)
    
    # Configure matrix properties
    vehicles = model.getType("GKVehicle") 
    vehicle = catalog.findByName("car", vehicles)
    matrix.setVehicle(vehicle)
    matrix.setDuration(GKTimeDuration(1, 0, 0))

    # Configure matrix demands
    # TODO Verzicht auf external IDs!
    for index in range(len(situation)):             
        inSection = catalog.find(sectionIds[2 * index])
        inSectionExternalId = inSection.getExternalId()
        roadType = inSection.getRoadType()
        internalAtt = roadType.getType().getColumn("GKRoadType::internalAtt", GKType.eSearchOnlyThisType) 
        inSectionIsInternal = roadType.getDataValueBool(internalAtt) # Internal section?
       
        outSection = catalog.find(sectionIds[2 * index + 1])
        outSectionExternalId = outSection.getExternalId()
        roadType = outSection.getRoadType()
        internalAtt = roadType.getType().getColumn("GKRoadType::internalAtt", GKType.eSearchOnlyThisType)
        outSectionIsInternal = roadType.getDataValueBool(internalAtt) # Internal section?
           
        fromCentroidId = inSectionExternalId.toInt()
        fromCentroid = catalog.find(fromCentroidId)

        if ((outSectionIsInternal or inSectionIsInternal) == False):
            # "Normal" sections            
            toCentroidId = outSectionExternalId.toInt()
            toCentroid = catalog.find(toCentroidId)     
            matrix.setTrips(fromCentroid, toCentroid, situation[index])
            print "Sect. %i (Centr. %i) -> Sect. %i (Centr. %i) = %f trips" % (inSection.getId(), fromCentroidId, outSection.getId(), toCentroidId, situation[index]) 
            
        elif (outSectionIsInternal):
            # Internal sections (o unambiguous centroid definition in external id possible)
            turnings = outSection.getDestination().getFromTurnings(outSection)
            # Assumption: Only one turning starts at internal section!  
            for turning in turnings: 
                toCentroidId = turning.getDestination().getExternalId().toInt()
                toCentroid = catalog.find(toCentroidId)
                matrix.setTrips(fromCentroid, toCentroid, situation[index])
                print "Internal section: Sect. %i (Centr. %i) -> Sect. %i (Centr. %i) = %f trips" % (inSection.getId(), fromCentroidId, outSection.getId(), toCentroidId, situation[index])

    return matrix

# Adds the matrix given as parameter to the traffic demand given as parameter. 
# The duration for the demand is given as parameter, too.
def addMatrixToDemand(matrix, demand, duration):
    newItem = GKScheduleDemandItem()
    newItem.setFrom(convertTime(demand.initialTime()))
    newItem.setDuration(duration)

    # Set scale factor if demand uses OD matrices
    initialDuration = float(convertTime(matrix.getDuration()))
    newItem.setFactor(str(duration / initialDuration * 100))
    newItem.setTrafficDemandItem(matrix)
    demand.addToSchedule(newItem)  # item is copied! (see ScriptingDocs)

# Check for active policies (that contain incidents) 
# and modify them so they are included in the simulations performed by Layer 2.
# 
# Parameters: simTime - the absolute(!) point in time for which the policies are checked
#             exp - the experiment running on Layer 0/1
def modifyActivePoliciesForLayer2(simTime, exp):   
    policies = exp.getPolicies()

    for policy in policies:
        # Check the activation type of the policy (time-dependent, always, ...)
        type = policy.getActivationType()

        if (type == GKPolicy.eTime):
            # Check if the policy is currently active
            fromTime = convertTime(policy.getFromTime())
            duration = convertTime(policy.getDurationTime())
            endTime = fromTime + duration
            print "%i %i %i" % (fromTime, duration, endTime)

            # Check if policy is currently active
            if (fromTime <= simTime and simTime <= endTime):
                policy.setActivationType(GKPolicy.eAlways)
            else:
                # remove inactive policies from experiment
                exp.removePolicy(policy)
        elif (type == GKPolicy.eTrigger or type == GKPolicy.eExternal):
            print "WARNING: Some policies are not activated always or time-dependent."