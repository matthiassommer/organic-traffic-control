#include <windows.h>
#include <process.h>
#include "EventManager.h"

JNIEXPORT void JNICALL Java_de_dfg_oc_otc_manager_EventManager_setEvent(JNIEnv *env, jclass thisclass)
{
	HANDLE hEvent = OpenEvent(EVENT_ALL_ACCESS,FALSE,"Init JVM");
	SetEvent(hEvent);
}