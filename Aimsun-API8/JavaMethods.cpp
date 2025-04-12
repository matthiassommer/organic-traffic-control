#include "JavaMethods.h"

JavaMethods::JavaMethods(JNIEnv *env, jclass controller) {
	_jniEnv = env;
	_controllerClass = controller;
	initAllMethodIDs();
}

void JavaMethods::initAllMethodIDs() {
	this->_availableMethods[JM_GET_INSTANCE] = new JMethodObj(JM_GET_INSTANCE, JM_GET_INSTANCE_SIG, static_method);
	this->_availableMethods[JM_GET_PHASES] = new JMethodObj(JM_GET_PHASES, JM_GET_PHASES_SIG, instance_method);
	this->_availableMethods[JM_SET_TIME] = new JMethodObj(JM_SET_TIME, JM_SET_TIME_SIG, instance_method);
	this->_availableMethods[JM_GET_ROUTING_DATA] = new JMethodObj(JM_GET_ROUTING_DATA, JM_GET_ROUTING_DATA_SIG, instance_method);
	this->_availableMethods[JM_SET_DETECTOR_VALUE] = new JMethodObj(JM_SET_DETECTOR_VALUE, JM_SET_DETECTOR_VALUE_SIG, instance_method);
	this->_availableMethods[JM_READY_FOR_FINISH] = new JMethodObj(JM_READY_FOR_FINISH, JM_READY_FOR_FINISH_SIG, instance_method);
	this->_availableMethods[JM_ADD_CENTROID] = new JMethodObj(JM_ADD_CENTROID, JM_ADD_CENTROID_SIG, instance_method);
	this->_availableMethods[JM_SET_SIMULATION_STEP_SIZE] = new JMethodObj(JM_SET_SIMULATION_STEP_SIZE, JM_SET_SIMULATION_STEP_SIZE_SIG, static_method);
	this->_availableMethods[JM_CREATE_NETWORK] = new JMethodObj(JM_CREATE_NETWORK, JM_CREATE_NETWORK_SIG, instance_method);
	this->_availableMethods[JM_ADD_SECTION] = new JMethodObj(JM_ADD_SECTION, JM_ADD_SECTION_SIG, instance_method);
	this->_availableMethods[JM_SET_EXPERIMENT] = new JMethodObj(JM_SET_EXPERIMENT, JM_SET_EXPERIMENT_SIG, instance_method);
	this->_availableMethods[JM_SET_REPLICATION] = new JMethodObj(JM_SET_REPLICATION, JM_SET_REPLICATION_SIG, instance_method);
	this->_availableMethods[JM_ADD_DETECTOR] = new JMethodObj(JM_ADD_DETECTOR, JM_ADD_DETECTOR_SIG, instance_method);
	this->_availableMethods[JM_SET_DETECTOR_CAPABILITIES] = new JMethodObj(JM_SET_DETECTOR_CAPABILITIES, JM_SET_DETECTOR_CAPABILITIES_SIG, instance_method);
	this->_availableMethods[JM_SET_DETECTOR_DESTINATIONS] = new JMethodObj(JM_SET_DETECTOR_DESTINATIONS, JM_SET_DETECTOR_DESTINATIONS_SIG, instance_method);
	this->_availableMethods[JM_ADD_JUNCTION] = new JMethodObj(JM_ADD_JUNCTION, JM_ADD_JUNCTION_SIG, instance_method);
	this->_availableMethods[JM_ADD_SIGNAL_GRP] = new JMethodObj(JM_ADD_SIGNAL_GRP, JM_ADD_SIGNAL_GRP_SIG, instance_method);
	this->_availableMethods[JM_ADD_TURNING] = new JMethodObj(JM_ADD_TURNING, JM_ADD_TURNING_SIG, instance_method);
	this->_availableMethods[JM_ADD_PHASE] = new JMethodObj(JM_ADD_PHASE, JM_ADD_PHASE_SIG, instance_method);
	this->_availableMethods[JM_ADD_SIGNAL_GRP_PHASE] = new JMethodObj(JM_ADD_SIGNAL_GRP_PHASE, JM_ADD_SIGNAL_GRP_PHASE_SIG, instance_method);
	this->_availableMethods[JM_NEW_INFO] = new JMethodObj(JM_NEW_INFO, JM_NEW_INFO_SIG, instance_method);
	this->_availableMethods[JM_NEW_WARNING] = new JMethodObj(JM_NEW_WARNING, JM_NEW_WARNING_SIG, instance_method);
	this->_availableMethods[JM_RESTART] = new JMethodObj(JM_RESTART, JM_RESTART_SIG, instance_method);
	this->_availableMethods[JM_INIT_SUBDETECTORS] = new JMethodObj(JM_INIT_SUBDETECTORS, JM_INIT_SUBDETECTORS_SIG, instance_method);
	this->_availableMethods[JM_INIT_JUNCTIONS] = new JMethodObj(JM_INIT_JUNCTIONS, JM_INIT_JUNCTIONS_SIG, instance_method);
	this->_availableMethods[JM_SET_STATISTICAL_INTERVAL] = new JMethodObj(JM_SET_STATISTICAL_INTERVAL, JM_SET_STATISTICAL_INTERVAL_SIG, instance_method);
	this->_availableMethods[JM_SET_TURNING_LANES] = new JMethodObj(JM_SET_TURNING_LANES, JM_SET_TURNING_LANES_SIG, instance_method);
	this->_availableMethods[JM_SET_SECTION_NB_LANES] = new JMethodObj(JM_SET_SECTION_NB_LANES, JM_SET_SECTION_NB_LANES_SIG, instance_method);
	this->_availableMethods[JM_FINALIZE_INIT] = new JMethodObj(JM_FINALIZE_INIT, JM_FINALIZE_INIT_SIG, instance_method);
	this->_availableMethods[JM_RESET_STATISTICS_TIME] = new JMethodObj(JM_RESET_STATISTICS_TIME, JM_RESET_STATISTICS_TIME_SIG, instance_method);
	this->_availableMethods[JM_ADD_TURNING_RAW_STATISTICAL_DATA] = new JMethodObj(JM_ADD_TURNING_RAW_STATISTICAL_DATA, JM_ADD_TURNING_RAW_STATISTICAL_DATA_SIG, instance_method);
	this->_availableMethods[JM_ADD_SECTION_RAW_FLOW] = new JMethodObj(JM_ADD_SECTION_RAW_FLOW, JM_ADD_SECTION_RAW_FLOW_SIG, instance_method);
	this->_availableMethods[JM_SET_PUBLIC_TRANSPORT_DATA] = new JMethodObj(JM_SET_PUBLIC_TRANSPORT_DATA, JM_SET_PUBLIC_TRANSPORT_DATA_SIG, instance_method);
	this->_availableMethods[JM_SET_PUBLIC_TRANSPORT_DETECTORS] = new JMethodObj(JM_SET_PUBLIC_TRANSPORT_DETECTORS, JM_SET_PUBLIC_TRANSPORT_DETECTORS_SIG, instance_method);
	this->_availableMethods[JM_GET_POLICY_TO_QUERY] = new JMethodObj(JM_GET_POLICY_TO_QUERY, JM_GET_POLICY_TO_QUERY_SIG, instance_method);
	this->_availableMethods[JM_SET_POLICY_STATUS] = new JMethodObj(JM_SET_POLICY_STATUS, JM_SET_POLICY_STATUS_SIG, instance_method);
	this->_availableMethods[JM_GET_PRINTSTREAMOUTPUT] = new JMethodObj(JM_GET_PRINTSTREAMOUTPUT, JM_GET_PRINTSTREAMOUTPUT_SIG, instance_method);


	for (std::map<const char*, JMethodObj*>::iterator iter = _availableMethods.begin(); iter != _availableMethods.end(); iter++) {
		jmethodID mid;
		JMethodObj *mobj = iter->second;
		switch (mobj->_mType) {
		case static_method:
			mid = this->_jniEnv->GetStaticMethodID(this->_controllerClass, mobj->_mName, mobj->_mSignature);
			break;
		case instance_method:
			mid = this->_jniEnv->GetMethodID(this->_controllerClass, mobj->_mName, mobj->_mSignature);
			break;
		default:
			mid = NULL;
		}

		if (mid != NULL) {
			this->_methodIDs[mobj->_mName] = mid;
		}
	}
}

bool JavaMethods::initMethod(JMethodObj *methodObj) {
	jmethodID mid;
	switch (methodObj->_mType) {
	case static_method:
		mid = this->_jniEnv->GetStaticMethodID(this->_controllerClass, methodObj->_mName, methodObj->_mSignature);
		break;
	case instance_method:
		mid = this->_jniEnv->GetMethodID(this->_controllerClass, methodObj->_mName, methodObj->_mSignature);
		break;
	default:
		mid = NULL;
	}

	if (mid == NULL) {
		return false;
	}
	else {
		this->_methodIDs[methodObj->_mName] = mid;
		return true;
	}
}

jmethodID JavaMethods::getMethodID(const char* methodName) {
	jmethodID mid = _methodIDs[methodName];

	if (mid == NULL) {
		// Try to reinitialize the unknown method
		JMethodObj * mob = this->_availableMethods[methodName];
		if (!initMethod(mob)) {
			exit(-1);
		}
		else {
			return this->_methodIDs[methodName];
		}
	}

	return mid;
}

JavaMethods::~JavaMethods()
{
}
