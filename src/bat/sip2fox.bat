@echo off
if "%SIP2FOX_HOME%" == "" set SIP2FOX_HOME="."
if not exist "%SIP2FOX_HOME%\lib\sip2fox.jar" goto appNotFound
java -Xms64m -Xmx96m "-Dsip2fox.home=%SIP2FOX_HOME%" "-Djava.endorsed.dirs=%SIP2FOX_HOME%\lib" -jar "%SIP2FOX_HOME%\lib\sip2fox.jar" %1 %2 %3 %4 %5 %6 %7 %8 %9
goto end
:appNotFound
echo ERROR: When running sip2fox from a directory other than where it was 
echo        installed, you must define the SIP2FOX_HOME environment variable
echo        to be the directory where it was installed.
:end
