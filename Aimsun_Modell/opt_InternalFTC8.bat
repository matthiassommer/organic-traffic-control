@ECHO OFF
ECHO BATCH-DATEI ZUM STARTEN PARALLELER EAs
ECHO opt_InternalFTC8.bat #EAs ANGFILENAME

SET /a M = %1
for /L %%N IN (1, 1, %M%) DO (
	ECHO Starting opt_InternalFTC8.py %2 %%N
	start aconsole -script opt_InternalFTC8.py %2 %%N
	start /B java -cp java de.dfg.oc.otc.layer2.ea.EAServer
)