REM ** Remove .#
FOR /F "tokens=*" %%G IN ('DIR /B /A- /S *.#*') DO DEL /A- /Q "%%G"

REM ** Remove obj dirs
FOR /F "tokens=*" %%G IN ('DIR /B /AD /S *obj*') DO RMDIR /S /Q "%%G"

REM ** Remove PDB files
FOR /F "tokens=*" %%G IN ('DIR /B /A- /S *.pdb*') DO DEL /A- /Q "%%G"

REM ** Remove vshost.exe
FOR /F "tokens=*" %%G IN ('DIR /B /A- /S *.vshost.exe*') DO DEL /A- /Q "%%G"

REM ** Remove .user
FOR /F "tokens=*" %%G IN ('DIR /B /A- /S *.user*') DO DEL /A- /Q "%%G"

REM ** Remove .suo
FOR /F "tokens=*" %%G IN ('DIR /B /A- /S *.suo*') DO DEL /A- /Q "%%G"

REM ** Remove .bak
FOR /F "tokens=*" %%G IN ('DIR /B /A- /S *.bak*') DO DEL /A- /Q "%%G"

REM ** Remove .ncb
FOR /F "tokens=*" %%G IN ('DIR /B /A- /S *.ncb*') DO DEL /A- /Q "%%G"


REM ** Remove CVS dirs
FOR /F "tokens=*" %%G IN ('DIR /B /AD /S *CVS*') DO RMDIR /S /Q "%%G"

PAUSE
