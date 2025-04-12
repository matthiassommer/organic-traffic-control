#include "AKIProxie.h"
#include "CIProxie.h"
#include "ANGConProxie.h"
#include "AAPI.h"
#include "JavaMethods.h"

extern "C"
{
#include "JavaLauncher.h"
#include <jni.h>
}

#include <windows.h>
#include <process.h>
#include <direct.h>
#include <stdio.h>
#include <sstream>
#include "JavaCalls.h"

//Anzahl der Fähigkeiten eines Detektors; Stand 28.4.2010
#define NUMDETECTORCAPABILITIES 8

int InitOTCManager(JavaOTCEnvironment);
int getDetectorTargetSections(int **);
int getStatisticalData(float);
void retrieveActivePolicies();
int lanesFunction(JavaOTCEnvironment);
void printJavaConsoleOutputStream();

void sendPublicTransportData();
int sendPublicTransportDetectorData();

JavaOTCEnvironment joe;
static bool isGatheringStatistics = false;
static int *otcControlledJunctionIds = NULL;
static int numOtcControlledJunctions = -1;
jobject javaController;

JavaMethods *javaMethods;

int AAPILoad() {
	return 0;
}

int getReturnValue() {
	if (joe.env->ExceptionOccurred()) {
		joe.env->ExceptionDescribe();
		return -1;
	}
	return 0;
}

void checkStatistics() {
	int status = AKIIsGatheringStatistics();
	if (status == 1) {
		isGatheringStatistics = true;
	}
	else {
		AKIPrintString("Statistische Daten werden nicht gesammelt.");
		isGatheringStatistics = false;
	}
}

void getOTCControlledJunctions(int replicationId) {
	const char *otcControlledJunctionIdsString;
	const unsigned short *otcControlledJunctionIdsShort;
	void *otcControlledJunctionsAttr = ANGConnGetAttribute(AKIConvertFromAsciiString("GKReplication::otcControlledJunctions"));

	if (otcControlledJunctionsAttr == NULL) {
		otcControlledJunctionsAttr = ANGConnCreateAttribute(AKIConvertFromAsciiString("GKReplication"), AKIConvertFromAsciiString("GKReplication::otcControlledJunctions"), AKIConvertFromAsciiString("Junctions to be controlled by OTC (by Id)"), INTEGER_TYPE, EXTERNAL);
	}
	otcControlledJunctionIdsShort = ANGConnGetAttributeValueString(otcControlledJunctionsAttr, replicationId);

	bool *problem = false;
	otcControlledJunctionIdsString = AKIConvertToAsciiString(otcControlledJunctionIdsShort, false, problem);

	if (otcControlledJunctionIdsString != NULL) {
		// Zerlegen des Strings in Tokens, ermitteln, wieviele Tokens enthalten sind
		// Trennzeichen werden von strtok durch '\0' ersetzt
		// Führende Trennzeichen werden vorab entfernt
		char *junctionSplitter = (char *)otcControlledJunctionIdsString + strspn(otcControlledJunctionIdsString, " ,;");
		junctionSplitter = strtok(junctionSplitter, " ,;");

		numOtcControlledJunctions = 0;
		while (junctionSplitter != NULL) {
			numOtcControlledJunctions++;
			junctionSplitter = strtok(NULL, " ,;");
		}

		if (numOtcControlledJunctions > 0) {
			otcControlledJunctionIds = (int *)malloc(sizeof(int)* numOtcControlledJunctions);
			junctionSplitter = (char *)otcControlledJunctionIdsString + strspn(otcControlledJunctionIdsString, " ,;");

			for (int j = 0; j < numOtcControlledJunctions; j++) {
				otcControlledJunctionIds[j] = atoi(junctionSplitter);
				junctionSplitter = junctionSplitter + strlen(junctionSplitter) + 1;
			}
		}
	}
}

/* called when Aimsun starts the simulation and can be used to initialise whatever the module needs */
int AAPIInit() {
	joe.env = NULL;
	joe.vm = NULL;
	joe.hJavaEvent = CreateEvent(NULL, FALSE, FALSE, "Init JVM");

	int replicationId = ANGConnGetReplicationId();

	getOTCControlledJunctions(replicationId);

	void *javaDebuggerAttach = ANGConnGetAttribute(AKIConvertFromAsciiString("GKReplication::javaDebugAttach"));
	if (javaDebuggerAttach == NULL) {
		javaDebuggerAttach = ANGConnCreateAttribute(AKIConvertFromAsciiString("GKReplication"), AKIConvertFromAsciiString("GKReplication::javaDebugAttach"), AKIConvertFromAsciiString("Attach Java Debugger (0: no, 1: yes, 2: yes + suspend VM)"), INTEGER_TYPE);
	}

	if (replicationId < 0) {
		AKIPrintString("Replication ID invalid!");
		return -1;
	}
	joe.debugLevel = ANGConnGetAttributeValueInt(javaDebuggerAttach, replicationId);

	MyStarter(&joe);

	if (WaitForJavaEvent(15, joe.hJavaEvent) < 0) {
		AKIPrintString("Event timeout");
		return -1;
	}

	jclass localRefCls = joe.env->FindClass("de/dfg/oc/otc/manager/OTCManager");
	if (localRefCls == NULL) {
		AKIPrintString("OTCManager class not found.");
		return -1;
	}

	joe.controllerClass = (jclass)joe.env->NewGlobalRef(localRefCls);

	localRefCls = joe.env->FindClass("de/dfg/oc/otc/manager/aimsun/AimsunNetwork");
	if (localRefCls == NULL) {
		AKIPrintString("AimsunNetwork class not found.");
		return -1;
	}
	joe.networkClass = (jclass)joe.env->NewGlobalRef(localRefCls);

	joe.env->DeleteLocalRef(localRefCls);

	javaMethods = new JavaMethods(joe.env, joe.controllerClass);
	javaController = joe.env->CallStaticObjectMethod(joe.controllerClass, javaMethods->getMethodID(JM_GET_INSTANCE));

	checkStatistics();

	int status = InitOTCManager(joe);
	if (status < 0) {
		getReturnValue();
	}

	return 0;
}

/**
Initialise Public Transport Lines.
*/
void sendPublicTransportData() {
	int numberlines = AKIPTGetNumberLines();

	jint *sectionsInLine = 0;
	jintArray jSectionsInLine;

	for (int i = 0; i < numberlines; i++) {
		int lineID = AKIPTGetIdLine(i);
		int numbersectionsinline = AKIPTGetNumberSectionsInLine(lineID);
		jSectionsInLine = joe.env->NewIntArray(numbersectionsinline);
		sectionsInLine = (jint *)malloc(numbersectionsinline * sizeof(jint));

		for (int j = 0; j < numbersectionsinline; j++) {
			sectionsInLine[j] = AKIPTGetIdSectionInLine(lineID, j);
		}

		joe.env->SetIntArrayRegion(jSectionsInLine, 0, numbersectionsinline, sectionsInLine);
		joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_SET_PUBLIC_TRANSPORT_DATA), jSectionsInLine, (jint)lineID);
	}

	if (sectionsInLine != 0) {
		free(sectionsInLine);
		joe.env->DeleteLocalRef(jSectionsInLine);
	}
}

/**
* Send detector data from public transport detectors (equipped vehicles).
*/
int sendPublicTransportDetectorData() {
	struct StaticInfVeh staticVehicleInfo;
	struct InfVeh vehicleInfo;
	int numDetectors = AKIDetGetNumberDetectors();
	int status = -1;

	for (int i = 0; i < numDetectors; i++) {
		int detectorId = AKIDetGetIdDetector(i);
		int equippedVehicles = AKIDetGetNbVehsEquippedInDetectionCyclebyId(detectorId, 0);

		for (int j = 0; j < equippedVehicles; j++) {
			staticVehicleInfo = AKIDetGetInfVehInDetectionStaticInfVehCyclebyId(detectorId, j, 0);
			vehicleInfo = AKIDetGetInfVehInDetectionInfVehCyclebyId(detectorId, j, 0);

			int lineID = staticVehicleInfo.idLine;
			//read speed of the vehicle, it is assmumed that vehicles read their speed from GPS-sensors, alternatively average speed of vehicle can be used to calculate arrival time
			float speed = (float)vehicleInfo.CurrentSpeed;

			status = (int)joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_SET_PUBLIC_TRANSPORT_DETECTORS), (jint)detectorId, (jint)lineID, (jfloat)speed);
		}
	}

	return status;
}

/* Abfragen der Zustände (=Phasen) aller Knotencontroller */
void updatePhases(double timeSta, double time, double acicle) {
	jobjectArray phases = (jobjectArray)joe.env->CallObjectMethod(javaController, javaMethods->getMethodID(JM_GET_PHASES));

	if (phases != NULL) {
		int arraySize = (int)joe.env->GetArrayLength(phases);

		jintArray phaseArray;
		jint *buffer = (jint *)malloc(2 * sizeof(jint));
		for (int i = 0; i < arraySize; i++) {
			phaseArray = (jintArray)joe.env->GetObjectArrayElement(phases, (jsize)i);
			joe.env->GetIntArrayRegion(phaseArray, 0, 2, buffer);
			ECIChangeDirectPhase(buffer[0], buffer[1], timeSta, time, acicle);
		}

		free(buffer);
		joe.env->DeleteLocalRef(phaseArray);
	}

	joe.env->DeleteLocalRef(phases);
}

/* Übertragen der aktuellen Zeit, gleichzeitig Signal an Java-Seite, dass alle Detektordaten übertragen sind */
void sendTime(double time) {
	joe.env->CallVoidMethod(javaController, javaMethods->getMethodID(JM_SET_TIME), (jfloat)time, (jint)ANGConnGetReplicationId());
}

void manageRoutingData() {
	jobjectArray routingData = (jobjectArray)joe.env->CallObjectMethod(javaController, javaMethods->getMethodID(JM_GET_ROUTING_DATA));
	if (routingData != NULL) {
		// buffer = { incoming section, outgoing section, centroid }
		jint *buffer = (jint *)malloc(3 * sizeof(jint));
		int length = (int)joe.env->GetArrayLength(routingData);

		AKIActionReset();
		int idVeh = ANGConnGetObjectId(AKIConvertFromAsciiString("taxi"), false);
		int id = AKIVehGetVehTypeInternalPosition(88);

		for (int i = 0; i < length; i++) {
			jintArray tempRoutingData = (jintArray)joe.env->GetObjectArrayElement(routingData, (jsize)i);
			joe.env->GetIntArrayRegion(tempRoutingData, 0, 3, buffer);

			// Compliance rate über Anteil Taxi zu anderer Autos simulieren!
			// in AIMSUN 2 Matrizen definieren und 30/70 z.B. einstellen bei Demand
			// vehTypePos muss auf Taxi gestellt sein (88), diese folgen alle den Routenempfehlungen
			// Sontige Autos folgen fest kürzestem Pfad
			AKIActionAddNextTurningODAction(buffer[0], buffer[1], -1, buffer[2], id, -1, 1, 200.0);

			joe.env->DeleteLocalRef(tempRoutingData);
		}

		free(buffer);
	}
	joe.env->DeleteLocalRef(routingData);
}

void sendSectionRawFlow() {
	int numberSections = AKIInfNetNbSectionsANG();
	for (int i = 0; i < numberSections; i++) {
		int sectionID = AKIInfNetGetSectionANGId(i);
		int flow = AKIEstGetParcialStatisticsSection(sectionID, 0, 0).Flow;

		if (flow >= 0) {
			joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_ADD_SECTION_RAW_FLOW), sectionID, flow);
		}
	}
}

void sendDetectorData() {
	jfloatArray jDetectorValueArray = joe.env->NewFloatArray(NUMDETECTORCAPABILITIES);
	jfloat *detectorValueArray = (jfloat *)malloc(NUMDETECTORCAPABILITIES * sizeof(jfloat));
	int status = 0;

	/* Übertragen der Messwerte der Detektoren */
	int numDetectors = AKIDetGetNumberDetectors();
	for (int i = 0; i < numDetectors; i++) {
		int detectorId = AKIDetGetIdDetector(i);

		// 1: Count, 2: Presence, 3: Speed, 4: Occupied Time Percentage, 5: Headway, 6: Density, 7: EquippedVehicle
		detectorValueArray[0] = (float)AKIDetGetCounterCyclebyId(detectorId, 0);
		detectorValueArray[1] = (float)AKIDetGetPresenceCyclebyId(detectorId, 0);
		detectorValueArray[2] = AKIDetGetSpeedCyclebyId(detectorId, 0);
		detectorValueArray[3] = AKIDetGetTimeOccupedCyclebyId(detectorId, 0);
		detectorValueArray[4] = AKIDetGetHeadwayCyclebyId(detectorId, 0);
		detectorValueArray[5] = AKIDetGetDensityCyclebyId(detectorId, 0);
		detectorValueArray[6] = (float)AKIDetGetNbVehsEquippedInDetectionCyclebyId(detectorId, 0);
		detectorValueArray[7] = (float)AKIDetGetNbintervalsOccupedCyclebyId(detectorId, 0);

		joe.env->SetFloatArrayRegion(jDetectorValueArray, 0, NUMDETECTORCAPABILITIES, detectorValueArray);
		status = (int)joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_SET_DETECTOR_VALUE), (jint)detectorId, jDetectorValueArray);

		if (status < 0) {
			AKIPrintString("Fehler bei der Übertragung der Detektoren.");
		}
	}

	free(detectorValueArray);
	joe.env->DeleteLocalRef(jDetectorValueArray);
}

/* is called in every simulation step at the beginning of the cycle.
Can be used to request detector measures, vehicle information and interact with junctions, meterings and VMS */
int AAPIManage(double time, double timeSta, double timTrans, double acicle) {
	retrieveActivePolicies();
	sendDetectorData();
	sendSectionRawFlow();
	manageRoutingData();
	sendTime(time);
	updatePhases(timeSta, time, acicle);
	sendPublicTransportDetectorData();

	return 0;
}

/* called in every simulation step at the end of the cycle */
int AAPIPostManage(double time, double timeSta, double timTrans, double acicle) {
	if (isGatheringStatistics) {
		int status = getStatisticalData(time);
		if (status != 0) {
			AKIPrintString("Übertragen der statistischen Daten fehlgeschlagen");
		}
	}
	printJavaConsoleOutputStream();
	return 0;
}

int AAPIFinish() {
	free(otcControlledJunctionIds);

	int status = joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_READY_FOR_FINISH));

	if (status == 0) {
		AKIPrintString("Waiting for Java to finish");
		ResetEvent(joe.hJavaEvent);

		if (WaitForJavaEvent(INFINITE, joe.hJavaEvent) < 0) {
			AKIPrintString("Event nicht eingetreten");
			return -1;
		}
	}

	return 0;
}

int AAPIUnLoad() {
	return 0;
}

void sendCentroids() {
	jint *toSecIdArray;
	jint *fromSecIdArray;
	jintArray jToSecIdArray;
	jintArray jFromSecIdArray;

	int nbCentroids = AKIInfNetNbCentroids();
	for (int i = 0; i < nbCentroids; i++) {
		int centId = AKIInfNetGetCentroidId(i);
		A2KCentroidInf centInf = AKIInfNetGetCentroidInf(centId);

		toSecIdArray = (jint *)malloc(centInf.NumConnecTo * sizeof(jint));
		for (int j = 0; j < centInf.NumConnecTo; j++) {
			int secId = AKIInfNetGetIdSectionofOriginCentroidConnector(centId, j);
			toSecIdArray[j] = secId;
		}

		fromSecIdArray = (jint *)malloc(centInf.NumConnecFrom * sizeof(jint));
		for (int j = 0; j < centInf.NumConnecFrom; j++) {
			int secId = AKIInfNetGetIdSectionofDestinationCentroidConnector(centId, j);
			fromSecIdArray[j] = secId;
		}

		jToSecIdArray = joe.env->NewIntArray(centInf.NumConnecTo);
		joe.env->SetIntArrayRegion(jToSecIdArray, 0, centInf.NumConnecTo, toSecIdArray);

		jFromSecIdArray = joe.env->NewIntArray(centInf.NumConnecFrom);
		joe.env->SetIntArrayRegion(jFromSecIdArray, 0, centInf.NumConnecFrom, fromSecIdArray);

		joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_ADD_CENTROID), (jint)centId, jFromSecIdArray, jToSecIdArray);

		free(toSecIdArray);
		free(fromSecIdArray);
	}
}

void sendSimulationStepSize() {
	joe.env->CallVoidMethod(javaController, javaMethods->getMethodID(JM_SET_SIMULATION_STEP_SIZE), (jfloat)AKIGetSimulationStepTime());
}

void sendNetworkObject() {
	bool *problem = false;
	char *networkPath = (char *)AKIConvertToAsciiString(AKIInfNetGetNetworkName(), false, problem);
	jstring jmessage = joe.env->NewStringUTF(networkPath);
	joe.env->CallObjectMethod(javaController, javaMethods->getMethodID(JM_CREATE_NETWORK), jmessage);

	joe.env->DeleteLocalRef(jmessage);
}

int sendSectionsAndTurnings() {
	void *roadTypeAttribute = ANGConnGetAttribute(AKIConvertFromAsciiString("GKSection::roadTypeAtt"));

	int counter = AKIInfNetNbSectionsANG();
	for (int i = 0; i < counter; i++) {
		int sectionANGId = AKIInfNetGetSectionANGId(i);

		A2KSectionInf sectionInfo = AKIInfNetGetSectionANGInf(sectionANGId);
		if (sectionInfo.report == 0) {
			jint *turningDestIdArray = (jint *)malloc(sectionInfo.nbTurnings * sizeof(jint));
			for (int j = 0; j < sectionInfo.nbTurnings; j++) {
				int turningDestId = AKIInfNetGetIdSectionANGDestinationofTurning(sectionANGId, j);
				if (turningDestId > 0) {
					turningDestIdArray[j] = turningDestId;
				}
				else {
					return -1;
				}
			}

			jintArray jTurningDestIdArray = joe.env->NewIntArray(sectionInfo.nbTurnings);
			joe.env->SetIntArrayRegion(jTurningDestIdArray, 0, sectionInfo.nbTurnings, turningDestIdArray);
			jint sectionID = (jint)sectionANGId;
			jint numberTurnings = (jint)sectionInfo.nbTurnings;
			jfloat sectionLength = (jfloat)sectionInfo.length;
			jfloat speedLimit = (jfloat)sectionInfo.speedLimit;
			jfloat capacity = (jfloat)sectionInfo.capacity;
			jint roadType = (jint)ANGConnGetAttributeValueInt(roadTypeAttribute, sectionID);

			int status = (int)joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_ADD_SECTION), sectionID, roadType, numberTurnings, sectionLength, jTurningDestIdArray, speedLimit, capacity);

			if (status < 0) {
				AKIPrintString("Fehler bei Übertragung der Sections.");
				return getReturnValue();
			}

			free(turningDestIdArray);
			joe.env->DeleteLocalRef(jTurningDestIdArray);
		}
		else {
			return -1;
		}
	}

	return 0;
}

void sendExperimentID() {
	joe.env->CallVoidMethod(javaController, javaMethods->getMethodID(JM_SET_EXPERIMENT), (jint)ANGConnGetExperimentId());
}

void sendReplicationID() {
	joe.env->CallVoidMethod(javaController, javaMethods->getMethodID(JM_SET_REPLICATION), (jint)ANGConnGetReplicationId());
}

int sendDetectors() {
	jint *detectorDestinationSections;
	jintArray jDetectorDestinationSections;
	int *targetSectionIds = NULL;
	int counter = AKIDetGetNumberDetectors();

	for (int i = 0; i < counter; i++) {
		int detectorId = AKIDetGetIdDetector(i);
		structA2KDetector detectorInfo = AKIDetGetPropertiesDetector(i);
		int firstLane = detectorInfo.IdFirstLane;
		int lastLane = detectorInfo.IdLastLane;
		jstring jmessage = joe.env->NewStringUTF("");

		int status = joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_ADD_DETECTOR), (jint)detectorId, (jint)detectorInfo.IdSection, (jfloat)detectorInfo.InitialPosition, (jfloat)detectorInfo.FinalPosition, (jint)firstLane, (jint)lastLane, jmessage);

		if (status < 0) {
			AKIPrintString("Fehler bei Übertragung der Detektoren.");
			return -1;
		}

		status = joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_SET_DETECTOR_CAPABILITIES), (jint)detectorId,
			(jboolean)AKIDetIsCountGather(detectorInfo.Capabilities), (jboolean)AKIDetIsPresenceGather(detectorInfo.Capabilities),
			(jboolean)AKIDetIsSpeedGather(detectorInfo.Capabilities), (jboolean)AKIDetIsOccupancyGather(detectorInfo.Capabilities),
			(jboolean)AKIDetIsHeadwayGather(detectorInfo.Capabilities), (jboolean)AKIDetIsDensityGather(detectorInfo.Capabilities),
			(jboolean)AKIDetIsInfEquippedVehGather(detectorInfo.Capabilities));

		if (status < 0) {
			AKIPrintString("Fehler bei Übertragung der Detektoren (Features).");
			return -1;
		}

		int numTurnings = getDetectorTargetSections(&targetSectionIds);
		jDetectorDestinationSections = joe.env->NewIntArray(numTurnings);
		detectorDestinationSections = (jint *)malloc(numTurnings * sizeof(jint));
		for (int j = 0; j < numTurnings; j++) {
			detectorDestinationSections[j] = targetSectionIds[j];
		}

		joe.env->SetIntArrayRegion(jDetectorDestinationSections, 0, numTurnings, detectorDestinationSections);

		status = joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_SET_DETECTOR_DESTINATIONS), (jint)detectorId, jDetectorDestinationSections);

		free(detectorDestinationSections);
		joe.env->DeleteLocalRef(jmessage);

		if (status < 0) {
			AKIPrintString("Fehler bei Übertragung der Ziel-Sections der Detektoren.");
			return -1;
		}
	}

	return 0;
}

int sendJunctions() {
	int sectionInId, sectionOutId;
	double phaseDuration, phaseMaxDuration, phaseMinDuration;

	int counter = ECIGetNumberJunctions();
	for (int i = 0; i < counter; i++) {
		int junctionId = ECIGetJunctionId(i);
		int numSignalGrps = ECIGetNumberSignalGroups(junctionId);
		bool *problem = false;
		char *junctionName = (char *)AKIConvertToAsciiString(ECIGetJunctionName(junctionId), false, problem);
		jstring jmessage2 = joe.env->NewStringUTF(junctionName);
		int controlType;

		if (numSignalGrps < 1) {
			controlType = 0;
			int status = joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_ADD_JUNCTION), (jint)junctionId, (jint)controlType, jmessage2);

			if (status < 0) {
				AKIPrintString("Fehler bei Übertragung der Junctions.");
				return -1;
			}
			continue;
		}

		// Wenn Junctions angegeben sind, sollen nur diese von OTC gesteuert werden.
		if (otcControlledJunctionIds != NULL) {
			controlType = 1;
			for (int j = 0; j < numOtcControlledJunctions; j++) {
				if (junctionId == otcControlledJunctionIds[j]) {
					controlType = ECIGetControlType(junctionId);
					break;
				}
			}
		}
		else {
			controlType = ECIGetControlType(junctionId);
		}

		// Bei Junctions mit Control Type "Extern" (2) wird die Steurung umgestellt, um Zugriff von außen zu haben.
		if (controlType == 2 && ECIDisableEvents(junctionId) < 0) {
			AKIPrintString("Fehler bei Übergabe der Kontrolle an externe Steuerung.");
		}

		// Bei Junctions mit Control Type "Actuated" (3) wird die Steurung umgestellt, um Zugriff von außen zu haben.
		if (controlType == 3 && ECIDisableEvents(junctionId) < 0) {
			AKIPrintString("Fehler bei Übergabe der Kontrolle an actuated Steuerung.");
		}

		int status = joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_ADD_JUNCTION), (jint)junctionId, (jint)controlType, jmessage2);
		if (status < 0) {
			AKIPrintString("Fehler bei Übertragung der Junctions (1).");
			return -1;
		}

		for (int j = 1; j <= numSignalGrps; j++) {
			status = joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_ADD_SIGNAL_GRP), (jint)j, (jint)junctionId);
			if (status < 0) {
				AKIPrintString("Fehler bei Übertragung der Junctions (2).");
				return -1;
			}

			int numTurnings = ECIGetNumberTurningsofSignalGroup(junctionId, j);
			for (int k = 0; k < numTurnings; k++) {
				status = ECIGetFromToofTurningofSignalGroup(junctionId, j, k, &sectionInId, &sectionOutId);
				status = joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_ADD_TURNING), (jint)junctionId, (jint)j, (jint)sectionInId, (jint)sectionOutId);
				if (status < 0) {
					AKIPrintString("Fehler bei Übertragung der Junctions (3).");
					return -1;
				}
			}
		}

		int numPhases = ECIGetNumberPhases(junctionId);
		for (int j = 1; j <= numPhases; j++) {
			int isInterphase = 0;
			if (ECIIsAnInterPhase(junctionId, j, 0) == 1) {
				isInterphase = 1;
			}

			status = ECIGetDurationsPhase(junctionId, j, 0, &phaseDuration, &phaseMaxDuration, &phaseMinDuration);
			if (status < 0) {
				AKIPrintString("Fehler bei Ermittlung der Phasendauern.");
				return -1;
			}

			status = joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_ADD_PHASE), (jint)j, (jint)isInterphase, (jfloat)phaseDuration, (jfloat)phaseMaxDuration, (jfloat)phaseMinDuration, (jint)junctionId);
			if (status < 0) {
				AKIPrintString("Fehler bei Übertragung der Phasen.");
				return -1;
			}

			int numSigGrpsPhase = ECIGetNbSignalGroupsPhaseofJunction(junctionId, j, 0);
			for (int k = 1; k <= numSigGrpsPhase; k++) {
				// TODO: Check, ob ECIGetSignalGroupPhaseofJunction auch zukünftig beim Parameter indexSG von 0 zählt
				int sigGrpId = ECIGetSignalGroupPhaseofJunction(junctionId, j, k - 1, 0);

				status = joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_ADD_SIGNAL_GRP_PHASE), (jint)sigGrpId, (jint)j, (jint)junctionId);
				if (status < 0) {
					AKIPrintString("Fehler bei Übertragung der Signalgruppen der Phasen.");
					return -1;
				}
			}
		}
		joe.env->DeleteLocalRef(jmessage2);
	}

	return 0;
}

int InitOTCManager(JavaOTCEnvironment joe) {
	int status = (int)joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_RESTART));
	if (status > 0) {
		AKIPrintString("Netzwerk ist bereits übertragen");
		return 0;
	}

	sendSimulationStepSize();
	sendNetworkObject();

	status = sendSectionsAndTurnings();
	if (status < 0) {
		return status;
	}

	status = sendDetectors();
	if (status < 0) {
		return status;
	}

	joe.env->CallVoidMethod(javaController, javaMethods->getMethodID(JM_INIT_SUBDETECTORS));

	status = sendJunctions();
	if (status < 0) {
		return status;
	}

	sendCentroids();
	sendPublicTransportData();

	joe.env->CallVoidMethod(javaController, javaMethods->getMethodID(JM_INIT_JUNCTIONS));

	lanesFunction(joe);

	sendExperimentID();
	sendReplicationID();

	joe.env->CallVoidMethod(javaController, javaMethods->getMethodID(JM_FINALIZE_INIT));

	if (isGatheringStatistics) {
		double statisticsInterval = AKIEstGetIntervalStatistics();
		joe.env->CallVoidMethod(javaController, javaMethods->getMethodID(JM_SET_STATISTICAL_INTERVAL), (jfloat)statisticsInterval);
	}

	return 0;
}

int getDetectorTargetSections(int **targetSectionIds) {
	char *inputString, *subString;
	int counter = 1, *localTargetSectionIds, ANGTargetSectionId;

	inputString = (char *)malloc((strlen("") + 1) * sizeof(char));
	inputString = strcpy(inputString, "");
	subString = inputString;
	while (true) {
		subString = strchr(subString, '-');
		if (subString == NULL)
		{
			break;
		}
		subString++;
		counter++;
	}

	if (*(targetSectionIds) != NULL) {
		free(*(targetSectionIds));
	}

	subString = inputString;
	localTargetSectionIds = (int *)malloc(counter * sizeof(int));
	for (int i = 0; i < counter; i++) {
		ANGTargetSectionId = (int)strtol(subString, &subString, 0);
		localTargetSectionIds[i] = ANGTargetSectionId;
		subString++;
	}

	*targetSectionIds = localTargetSectionIds;

	free(inputString);

	return counter;
}

int lanesFunction(JavaOTCEnvironment joe) {
	int numSection = AKIInfNetNbSectionsANG();

	for (int i = 0; i < numSection; i++) {
		int sectionANGId = AKIInfNetGetSectionANGId(i);
		A2KSectionInf sectionInfo = AKIInfNetGetSectionANGInf(sectionANGId);
		int numberOfLanes = sectionInfo.nbCentralLanes + sectionInfo.nbSideLanes;

		joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_SET_SECTION_NB_LANES), (jint)sectionANGId, (jint)numberOfLanes);

		int numberOfTurnings = sectionInfo.nbTurnings;
		for (int j = 0; j < numberOfTurnings; j++) {
			int destinationTurning = AKIInfNetGetIdSectionANGDestinationofTurning(sectionANGId, j);
			int firstLaneOriginTurning = AKIInfNetGetOriginFromLaneofTurning(sectionANGId, j);
			int lastLaneOriginTurning = AKIInfNetGetOriginToLaneofTurning(sectionANGId, j);
			int firstLaneDestinationTurning = AKIInfNetGetDestinationFromLaneofTurning(sectionANGId, j);
			int lastLaneDestinationTurning = AKIInfNetGetDestinationToLaneofTurning(sectionANGId, j);

			joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_SET_TURNING_LANES), (jint)sectionANGId, (jint)destinationTurning,
				(jint)firstLaneOriginTurning, (jint)lastLaneOriginTurning, (jint)firstLaneDestinationTurning, (jint)lastLaneDestinationTurning);
		}
	}

	return 0;
}

int getStatisticalData(float time) {
	int sectionInId, sectionOutId;
	char message[200];
	void* losSIAtt = ANGConnGetAttribute(AKIConvertFromAsciiString("GGetramModule::SI_GKNode_node los_"));
	void* losSRCAtt = ANGConnGetAttribute(AKIConvertFromAsciiString("GGetramModule::SRC_GKNode_node los_"));

	if (AKIEstIsNewStatisticsAvailable()) {
		joe.env->CallVoidMethod(javaController, javaMethods->getMethodID(JM_RESET_STATISTICS_TIME), (jfloat)time);
	}

	// get statistical data for turnings
	int numJunctions = ECIGetNumberJunctions();
	for (int junctionIter = 0; junctionIter < numJunctions; junctionIter++) {
		int junctionId = ECIGetJunctionId(junctionIter);
		int numSignalGrps = ECIGetNumberSignalGroups(junctionId);

		// Signalgruppen sind beginnend mit 1 (nicht 0) durchnummeriert!
		for (int sigGrpIter = 1; sigGrpIter <= numSignalGrps; sigGrpIter++) {
			int numTurnings = ECIGetNumberTurningsofSignalGroup(junctionId, sigGrpIter);
			for (int turningIter = 0; turningIter < numTurnings; turningIter++) {
				ECIGetFromToofTurningofSignalGroup(junctionId, sigGrpIter, turningIter, &sectionInId, &sectionOutId);

				StructAkiEstadTurning turningData = AKIEstGetParcialStatisticsTurning(sectionInId, sectionOutId, 0, 0);
				if (turningData.report == 0) {
					int status = joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_ADD_TURNING_RAW_STATISTICAL_DATA), (jint)turningData.IdSectionFrom, (jint)turningData.IdSectionTo,
						(jfloat)time, (jint)turningData.Flow, (jfloat)turningData.TTa, (jfloat)turningData.DTa, (jfloat)turningData.STa, (jfloat)turningData.LongQueueAvg,
						(jfloat)turningData.NumStops, (jfloat)turningData.Sa, (jfloat)turningData.Sd, (jfloat)turningData.LongQueueMax);

					if (status != 0) {
						sprintf_s(message, "Fehler (%d): Statistische Daten für Junction %d, Signal Group %d, Turning %d\n", status, junctionId, sigGrpIter, turningIter);
						AKIPrintString(message);
					}
				}
			}
		}
	}

	return getReturnValue();
}

void retrieveActivePolicies()
{
	int policyToQuery = -1;
	do
	{
		policyToQuery = joe.env->CallIntMethod(javaController, javaMethods->getMethodID(JM_GET_POLICY_TO_QUERY));
		if (policyToQuery > 0)
		{
			bool act = ANGConnIsPolicyActive(policyToQuery);
			joe.env->CallVoidMethod(javaController, javaMethods->getMethodID(JM_SET_POLICY_STATUS), policyToQuery, act);
		}
	} while (policyToQuery > 0);


}

void printJavaConsoleOutputStream() 
{
	// get the System.out.println() stream content to print it here
	jstring s = (jstring)joe.env->CallObjectMethod(javaController, javaMethods->getMethodID(JM_GET_PRINTSTREAMOUTPUT));

	char* nativeString;
	const char* _nativeString = joe.env->GetStringUTFChars(s, 0);
	nativeString = strdup(_nativeString);
	joe.env->ReleaseStringUTFChars(s, _nativeString);

	// As the aimsun console does not allow multi-line output, split it here and print it line by line
	std::stringstream ss;
	ss.str(nativeString);
	std::string item;
	while (std::getline(ss, item, '\n')) 
	{
		char* cstrn;
		cstrn = strdup(item.c_str());
		AKIPrintString(cstrn);
		free(cstrn);
	}
	free(nativeString);
}