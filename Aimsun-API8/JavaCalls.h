#ifndef _JAVACALLS_H_
#define _JAVACALLS_H_

#include <windows.h>
#include <direct.h>
#include <process.h>
#include <jni.h>
#include "java.h"

#ifdef AIMSUN_API
#include "AKIProxie.h"
#endif

extern "C"
{
#include "JavaLauncher.h"
}

typedef struct _ThreadParams{
	int kill;
	char status[200];
	JavaVM *vm;
}ThreadParams;

typedef struct _JavaOTCEnvironment{
	jclass controllerClass;
	jclass networkClass;
	JNIEnv *env;
	HANDLE hJavaEvent;
	JavaVM *vm;
	int debugLevel;
}JavaOTCEnvironment;
 
int WaitForJavaEvent(int, HANDLE);
void MyStarter(JavaOTCEnvironment *);

#endif