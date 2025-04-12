/*
 * $Id: MyLauncher.c,v 1.1 2010/02/25 10:04:33 hpr Exp $
 *
 * $Author: hpr $
 */
extern "C"
{
#include "JavaLauncher.h"
#include <jni.h>
}

#include <windows.h>
#include <process.h>
#include <direct.h>
#include <stdio.h>
#include "JavaCalls.h"

#if 0
void SimThread(PVOID);
int CallJavaMethod(JavaVM *);

typedef struct _ThreadParams{
	int kill;
	char status[200];
	JavaVM *vm;
}ThreadParams;

namespace
{
	int counter;
}
#endif

int main(void)
{
	int i = 0, j;
	char input;
	JavaOTCEnvironment joe;

	joe.hJavaEvent = CreateEvent(NULL, FALSE, FALSE, "Init JVM");
	for (j = 0; j < 2; j++)
	{
		joe.env = NULL;
		joe.vm = NULL;
		MyStarter(&joe);
		printf("Warte auf Event, Durchlauf %i\n", j + 1);

		if (WaitForJavaEvent(15, joe.hJavaEvent) < 0) {
			printf("Event nicht eingetreten");
			return -1;
		}

		joe.controllerClass = joe.env->FindClass("de/dfg/oc/otc/manager/OTCManager");
		if (joe.controllerClass == NULL) {
			printf("Class not found.");
			return -1;
		}

		printf("Adresse der VM (Main2): %i\n", joe.vm);

		CallJavaMethod(23 * (j + 1), joe);

		printf("Close GUI: Warte auf Tastendruck\n");
		input = getchar();

		printf("Detach, Destroy: Warte auf Tastendruck\n");
		input = getchar();
		joe.vm->DetachCurrentThread();
	}
	printf("CloseHandle: Warte auf Tastendruck\n");
	input = getchar();

	CloseHandle(joe.hJavaEvent);
	printf("Final: Warte auf Tastendruck\n");
	input = getchar();
	return 0;
}