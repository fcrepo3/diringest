@echo off
if "%SIP2FOX_HOME%" == "" set SIP2FOX_HOME="dist\sip2fox"
if not exist "%SIP2FOX_HOME%\lib\sip2fox.jar" goto appNotFound
java -Xms64m -Xmx96m "-Dsip2fox.home=%SIP2FOX_HOME%" "-Djava.endorsed.dirs=%SIP2FOX_HOME%\lib" %1 %2 %3 %4 %5 %6 %7 %8 %9
goto end
:appNotFound
echo ERROR: %SIP2FOX_HOME%\lib\sip2fox.jar not found
:end
