@ECHO OFF
ECHO BATCH-DATEI ZUM STARTEN PARALLELER EAs
ECHO opt_InternalFTC8_Debug.bat #EAs ANGFILENAME

SET /a M = %1
for /L %%N IN (1, 1, %M%) DO (
	ECHO Starting opt_InternalFTC8.py %2 %%N
	start aconsole -script opt_InternalFTC8.py %2 %%N

	ECHO Starting EAServer
	start /B java -cp java -Xdebug -Xrunjdwp:transport=dt_socket,address="127.0.0.1:8000",server=y,suspend=y de.dfg.oc.otc.layer2.ea.EAServer
)