#ifndef _INVOKE_H_
#define _INVOKE_H_

#include <windows.h>
#include "jni.h"

int startJava(char **, JNIEnv *, CRITICAL_SECTION);

#endif