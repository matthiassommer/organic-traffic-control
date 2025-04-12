#include <Python.h>
#include "JavaCalls.h"

static PyMethodDef JavaLauncherMethods[] = {
    {"startJvm",  javaLauncher_startJvm, METH_VARARGS,
     "Start JVM."},
    {NULL, NULL, 0, NULL}        /* Sentinel */
};

static PyObject *
javaLauncher_startJvm(PyObject *self, PyObject *args)
{
    const char *command;
    int sts;
	
	char **javaParameter;
	int cJavaParameter;
	int returnwert;
	char pfad[MAX_PATH];
	char *pfad2;
	int tempwert;
	JNIEnv *env = NULL;
	JavaVM *vm = NULL;

    if (!PyArg_ParseTuple(args, "s", &command))
        return NULL;
    /* sts = system(command);
    return Py_BuildValue("i", sts); */

	getcwd(pfad, MAX_PATH);
	
	pfad2 = (char *)calloc(200, sizeof(char));
	sprintf(pfad2, "D:/Eigene Dateien/Eclipse/OTC-Manager/bin");

	tempwert = strlen(pfad2);
	if (tempwert > MAX_PATH)
	{
		exit(1);
	}
// Für Java-Remote-Debugger folgende Bedingung auf 0 setzen, sonst 1
#if 1
	cJavaParameter = 2;
	javaParameter = (char **)malloc(sizeof(char *)*cJavaParameter);
	javaParameter[0] = (char *)malloc(sizeof(char)*MAX_PATH);
	javaParameter[1] = (char *)malloc(sizeof(char)*MAX_PATH);
	sprintf(javaParameter[0], "%s/java",pfad2);
	sprintf(javaParameter[1], "de/unihannover/sra/otc/manager/gui/Main");
#else
	cJavaParameter = 5;
	javaParameter = (char **)malloc(sizeof(char *)*cJavaParameter);
	javaParameter[0] = (char *)malloc(sizeof(char)*MAX_PATH);
	javaParameter[1] = (char *)malloc(sizeof(char)*MAX_PATH);
	javaParameter[2] = (char *)malloc(sizeof(char)*MAX_PATH);
	javaParameter[3] = (char *)malloc(sizeof(char)*MAX_PATH);
	javaParameter[4] = (char *)malloc(sizeof(char)*MAX_PATH);
	sprintf(javaParameter[0], "%s/java",pfad2);
	sprintf(javaParameter[1], "-Xdebug");
	sprintf(javaParameter[2], "-Xnoagent");
	//Erste Zeile: JVM pausiert, bis sich der Debugger angehängt hat; zweite Zeile: JVM wartet nicht auf Debugger.
	sprintf(javaParameter[3], "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=localhost:8000");
	//sprintf(javaParameter[3], "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=localhost:8000");
	sprintf(javaParameter[4], "de/unihannover/sra/otc/manager/gui/Main");
#endif
	returnwert = startJava(cJavaParameter, javaParameter, vm, env);
	return Py_BuildValue("i", returnwert);
}

PyMODINIT_FUNC
initjavaLauncher(void)
{
    (void) Py_InitModule("javaLauncher", JavaLauncherMethods);
}