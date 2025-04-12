/*
 * $Author: hpr $
 * Der Schalter AIMSUN_API muss explizit in den Projekteigenschaften gesetzt werden!
 */
#include "JavaCalls.h"
#include "JavaMethods.h"

int WaitForJavaEvent(int sec, HANDLE hJavaEvent)
{
	int returnwert;
	DWORD test;
	char message[200];

	if (sec == INFINITE) {
		test = WaitForSingleObject(hJavaEvent, INFINITE);
	}
	else {
		test = WaitForSingleObject(hJavaEvent, sec * 1000);
	}
	switch (test) {
	case WAIT_ABANDONED:
		sprintf(message, "Cancelled\n");
		returnwert = -1;
		break;
	case WAIT_OBJECT_0:
		sprintf(message, "Event getriggert.\n");
		returnwert = 0;
		break;
	case WAIT_TIMEOUT:
		sprintf(message, "Timeout\n");
		returnwert = -1;
		break;
	case WAIT_FAILED:
		sprintf(message, "Failed\n");
		returnwert = -1;
		break;
	default:
		sprintf(message, "Unknown error.\n");
		returnwert = -1;
	}
#ifdef AIMSUN_API
	AKIPrintString(message);
#else
	printf("%s\n", message);
#endif
	return returnwert;
}

void MyStarter(JavaOTCEnvironment *joe)
{
	char **javaParameter;
	int cJavaParameter;
	int returnwert;
	char pfad[MAX_PATH];
	char *pfad2;
	getcwd(pfad, MAX_PATH);
	bool problem;

#ifdef AIMSUN_API
	pfad2 = (char *)AKIConvertToAsciiString(AKIInfNetGetNetworkPath(), false, &problem);
#endif

	int tempwert = strlen(pfad2);
	if (tempwert > MAX_PATH) {
		exit(1);
	}

	// Für Java-Remote-Debugger folgende Bedingung auf 0 setzen, sonst 1
	if (joe->debugLevel == 0) {
		cJavaParameter = 3;
		javaParameter = (char **)malloc(sizeof(char *)*cJavaParameter);
		javaParameter[0] = (char *)malloc(sizeof(char)*MAX_PATH);
		javaParameter[1] = (char *)malloc(sizeof(char)* 16);
		javaParameter[2] = (char *)malloc(sizeof(char)*MAX_PATH);
		sprintf(javaParameter[0], "%s/java", pfad2);
		sprintf(javaParameter[1], "-Xmx1536m");
		sprintf(javaParameter[2], "de/dfg/oc/otc/manager/gui/Main");
	}
	else {
		cJavaParameter = 6;
		javaParameter = (char **)malloc(sizeof(char *)*cJavaParameter);
		javaParameter[0] = (char *)malloc(sizeof(char)*MAX_PATH);
		javaParameter[1] = (char *)malloc(sizeof(char)*MAX_PATH);
		javaParameter[2] = (char *)malloc(sizeof(char)*MAX_PATH);
		javaParameter[3] = (char *)malloc(sizeof(char)*MAX_PATH);
		javaParameter[4] = (char *)malloc(sizeof(char)* 16);
		javaParameter[5] = (char *)malloc(sizeof(char)*MAX_PATH);
		javaParameter[6] = (char *)malloc(sizeof(char)*MAX_PATH);
		sprintf(javaParameter[0], "%s/java", pfad2);
		sprintf(javaParameter[1], "-Xdebug");
		sprintf(javaParameter[2], "-Xnoagent");
		//Erste Zeile: JVM pausiert, bis sich der Debugger angehängt hat; zweite Zeile: JVM wartet nicht auf Debugger.
		if (joe->debugLevel > 1) {
			sprintf(javaParameter[3], "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=localhost:8000");
		}
		else {
			sprintf(javaParameter[3], "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=localhost:8000");
		}
		sprintf(javaParameter[4], "-Xmx1536m");
		sprintf(javaParameter[5], "de/dfg/oc/otc/manager/gui/Main");
	}

	returnwert = startJava(cJavaParameter, javaParameter, &(joe->vm), &(joe->env));
}
