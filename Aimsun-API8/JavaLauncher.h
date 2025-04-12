#ifndef _JAVALAUNCHER_H_
#define _JAVALAUNCHER_H_

#include "java.h"

int startJava(int, char **, JavaVM **, JNIEnv **);
int loadJavaLibrary(int, char **);

#endif