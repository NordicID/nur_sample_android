@REM Copies the the nurapiandroid-release.aar to all the projects
@REM as Android Studio likely is not aware of the cahnges made to the module.

@SET SOURCE_AAR=NurApiAndroid\nurapiandroid\build\outputs\aar\nurapiandroid-release.aar

@REM Accessory demo
COPY %SOURCE_AAR% NURAccessoryDemo\nurapiandroid\ /y

@REM RFID demo
COPY %SOURCE_AAR% RFIDDemo\nurapiandroid\ /y

@REM FW updater
COPY %SOURCE_AAR% NurFWUpdate\nurapiandroid\ /y

@PAUSE

