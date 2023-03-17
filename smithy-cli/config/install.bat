@echo off

:: Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
:: SPDX-License-Identifier: Apache-2.0

set exe_name=smithy
set installer_path=%~dp0
set installer_drive=%~d0
set installer_exe=%installer_path%bin\%exe_name%
set install_path=%installer_drive%\Program Files\Smithy


:: Set the installation path
set choice_path=%install_path%
set /p choice_path=Install path [%install_path%]: 
echo Installing Smithy to %choice_path%...


echo Checking for existing installation...
if exist "%choice_path%" goto upgrade


:start
:: Create a wrapper bat
(
  echo @echo off
  echo "%choice_path%\bin\smithy" %%*
) > "%installer_path%\%exe_name%.bat" 

:: Copy the installation
xcopy /i /h /e /k "%installer_path%*" "%choice_path%\"
setx path "%path%;%choice_path%\;"
call %exe_name% warmup
echo You may now run '%exe_name% --version'
goto done


:: Check if upgrade is desired
:upgrade
echo Existing Smithy installation found...
set choice_upgrade=''
set /p choice_upgrade=Upgrade Smithy? [y/n]:
if '%choice_upgrade%'=='y' goto yes
if '%choice_upgrade%'=='n' goto no
echo "%choice_upgrade%" is not valid
goto upgrade


:no
echo Not upgrading Smithy...
goto done

:yes
echo Upgrading Smithy...
rmdir "%choice_path%" /s /q
goto start

:done
pause
exit
