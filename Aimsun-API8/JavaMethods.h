#include "jni.h"
#include <vector>
#include <map>

#define JM_GET_INSTANCE "getInstance"
#define JM_GET_INSTANCE_SIG "()Lde/dfg/oc/otc/manager/OTCManager;"

#define JM_GET_PHASES "getPhases"
#define JM_GET_PHASES_SIG "()[[I"

#define JM_SET_TIME "setTime"
#define JM_SET_TIME_SIG "(F)V"

#define JM_GET_ROUTING_DATA "getRoutingData"
#define JM_GET_ROUTING_DATA_SIG "()[[I"

#define JM_SET_DETECTOR_VALUE "setDetectorValue"
#define JM_SET_DETECTOR_VALUE_SIG "(I[F)I"

#define JM_GENERATE_FTC_FOR_JUNCTION "generateFTCforJunction"
#define JM_GENERATE_FTC_FOR_JUNCTION_SIG "(I[I[F)V"

#define JM_GENERATE_NEMA_FOR_JUNCTION "generateNEMAforJunction"
#define JM_GENERATE_NEMA_FOR_JUNCTION_SIG "(I[I[F[F[I[F[F[F[F)V"

#define JM_READY_FOR_FINISH "readyForFinish"
#define JM_READY_FOR_FINISH_SIG "()I"

#define JM_ADD_CENTROID "addCentroid"
#define JM_ADD_CENTROID_SIG "(I[I[I)V"

#define JM_SET_SIMULATION_STEP_SIZE "setSimulationStepSize"
#define JM_SET_SIMULATION_STEP_SIZE_SIG "(F)V"

#define JM_CREATE_NETWORK "createNetwork"
#define JM_CREATE_NETWORK_SIG "(Ljava/lang/String;)Lde/dfg/oc/otc/manager/aimsun/AimsunNetwork;"

#define JM_ADD_SECTION "addSection"
#define JM_ADD_SECTION_SIG "(IIIF[IFF)I"

#define JM_SET_EXPERIMENT "setExperiment"
#define JM_SET_EXPERIMENT_SIG "(I)V"

#define JM_SET_REPLICATION "setReplicationID"
#define JM_SET_REPLICATION_SIG "(I)V"

#define JM_ADD_DETECTOR "addDetector"
#define JM_ADD_DETECTOR_SIG "(IIFFIILjava/lang/String;)I"

#define JM_SET_DETECTOR_CAPABILITIES "setDetectorCapabilities"
#define JM_SET_DETECTOR_CAPABILITIES_SIG "(IZZZZZZZ)I"

#define JM_SET_DETECTOR_DESTINATIONS "setDetectorDestinations"
#define JM_SET_DETECTOR_DESTINATIONS_SIG "(I[I)I"

#define JM_ADD_JUNCTION "addJunction"
#define JM_ADD_JUNCTION_SIG "(IILjava/lang/String;)I"

#define JM_ADD_SIGNAL_GRP "addSignalGrp"
#define JM_ADD_SIGNAL_GRP_SIG "(II)I"

#define JM_ADD_TURNING "addTurning"
#define JM_ADD_TURNING_SIG "(IIII)I"

#define JM_ADD_PHASE "addPhase"
#define JM_ADD_PHASE_SIG "(IIFFFI)I"

#define JM_ADD_SIGNAL_GRP_PHASE "addSignalGrpPhase"
#define JM_ADD_SIGNAL_GRP_PHASE_SIG "(III)I"

#define JM_NEW_INFO "newInfo"
#define JM_NEW_INFO_SIG "(Ljava/lang/String;)V"

#define JM_NEW_WARNING "newWarning"
#define JM_NEW_WARNING_SIG "(Ljava/lang/String;)V"

#define JM_RESTART "restart"
#define JM_RESTART_SIG "()I"

#define JM_INIT_SUBDETECTORS "initSubDetectors"
#define JM_INIT_SUBDETECTORS_SIG "()V"

#define JM_INIT_JUNCTIONS "initJunctions"
#define JM_INIT_JUNCTIONS_SIG "()V"

#define JM_SET_STATISTICAL_INTERVAL "setStatisticsInterval"
#define JM_SET_STATISTICAL_INTERVAL_SIG "(F)V"

#define JM_SET_TURNING_LANES "setTurningLanes"
#define JM_SET_TURNING_LANES_SIG "(IIIIII)I"

#define JM_SET_SECTION_NB_LANES "setSectionNbLanes"
#define JM_SET_SECTION_NB_LANES_SIG "(II)I"

#define JM_FINALIZE_INIT "finalizeInit"
#define JM_FINALIZE_INIT_SIG "()V"

#define JM_RESET_STATISTICS_TIME "resetStatisticsTime"
#define JM_RESET_STATISTICS_TIME_SIG "(F)V"

#define JM_ADD_TURNING_RAW_STATISTICAL_DATA "addTurningRawStatisticalData"
#define JM_ADD_TURNING_RAW_STATISTICAL_DATA_SIG "(IIFIFFFFFFFF)I"

#define JM_ADD_SECTION_RAW_FLOW "addSectionRawFlow"
#define JM_ADD_SECTION_RAW_FLOW_SIG "(II)I"

#define JM_SET_PUBLIC_TRANSPORT_DATA "setPublicTransportData"
#define JM_SET_PUBLIC_TRANSPORT_DATA_SIG "([II)V"

#define JM_SET_PUBLIC_TRANSPORT_DETECTORS "setPublicTransportDetectors"
#define JM_SET_PUBLIC_TRANSPORT_DETECTORS_SIG "(IIF)I"

#define JM_GET_POLICY_TO_QUERY "getPolicyToQuery"
#define JM_GET_POLICY_TO_QUERY_SIG "()I"

#define JM_SET_POLICY_STATUS "setPolicyStatus"
#define JM_SET_POLICY_STATUS_SIG "(IZ)V"

#define JM_GET_PRINTSTREAMOUTPUT "getPrintStreamOutput"
#define JM_GET_PRINTSTREAMOUTPUT_SIG "()Ljava/lang/String;"

typedef enum MethodType {
	static_method,
	instance_method
} MethodType;

class JMethodObj {

public:
	JMethodObj(const char * name, const char * signature, MethodType type) {
		_mName = name;
		_mSignature = signature;
		_mType = type;
	}

	const char * _mName; // The identifer
	const char * _mSignature; // Return Parameter and Formal Parameters
	MethodType _mType; // Static or Instace Method
};

class JavaMethods
{
public:
	std::map<const char*, JMethodObj*> _availableMethods;
	std::map<const char*, jmethodID> _methodIDs;

	JNIEnv * _jniEnv;
	jclass _controllerClass;

	JavaMethods(JNIEnv * env, jclass controller);
	~JavaMethods();

	void initAllMethodIDs();
	bool initMethod(JMethodObj * methodObj);

	jmethodID getMethodID(const char* methodName);
};
