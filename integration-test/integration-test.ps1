function Set-LocalEnvironmentVariable {
<#
.SYNOPSIS
	Set enviornment variable for this script scope
.DESCRIPTION
	This PowerShell function set the eviornment variable for this script.
#>
    param (
        [Parameter()]
        [System.String]
        $Name,

        [Parameter()]
        [System.String]
        $Value,

        [Parameter()]
        [Switch]
        $Append
    )
    if($Append.IsPresent)
    {
        if(Test-Path "env:$Name")
        {
            $Value = (Get-Item "env:$Name").Value + $Value
        }
    }
    Set-Item env:$Name -Value "$value" | Out-Null
}

function Get-LocalEnviornmentVariable {
<#
.SYNOPSIS
	Get enviornment variable for this script scope
.DESCRIPTION
	This PowerShell function get the eviornment variable for this script.
#>
    param (
        [Parameter()]
        [System.String]
        $Name
    )
    $ValVar = $null
    if(Test-Path "env:$Name") {
        $ValVar = (Get-Item "env:$Name").Value
    }
    return $ValVar
}

Function Validate_Variables {
<#
.SYNOPSIS
	Check required enviornment variable for this script execution scope
.DESCRIPTION
	This PowerShell function validates required variables and stop the execution of the script.
#>
    

    #$global:FCLI_MODULE = Read-Host -Prompt $MODULE_INPUT_STR
    #$FCLI_URL = Read-Host -Prompt "Enter FOD url, [Default] https://api.sandbox.fortify.com:"
    #$FCLI_FOD_TENANT = Read-Host -Prompt "Enter FOD Tenant:"
    #$FCLI_FOD_USER = Read-Host -Prompt "Enter FOD Username:"
    #$FCLI_FOD_PWD = Read-Host -Prompt "Enter FOD Password:" -AsSecureString

    $global:FCLI_MODULE = Get-LocalEnviornmentVariable "FCLI_MODULE"
    if($global:FCLI_MODULE -eq $null -or $global:FCLI_MODULE -eq "") {
        if ($global:blnLinux) {
            $global:FCLI_MODULE = "fcli.jar"
        } else {
            $global:FCLI_MODULE = "fcli.exe"
        }
    }

    if (-not(Test-Path -Path "$global:FCLI_MODULE" -PathType Leaf)) {
        Write-Host "The file [$global:FCLI_MODULE] has not been found.set variable 'FCLI_MODULE' with exact path" -ForegroundColor Red
        Break
    }

    $FCLI_URL = Get-LocalEnviornmentVariable "FCLI_URL"
    if($FCLI_URL) {
        Set-LocalEnvironmentVariable "FCLI_DEFAULT_URL" $FCLI_URL
    }else{
        Set-LocalEnvironmentVariable "FCLI_DEFAULT_URL" "https://api.ams.fortify.com"
        Write-Host "FCLI_URL variable empty, setting FoD URL, the Default is set as 'ams.fortify.com'" -ForegroundColor Red
    }

    $FCLI_FOD_TENANT = Get-LocalEnviornmentVariable "FCLI_FOD_TENANT"
	if ($FCLI_FOD_TENANT) {
        Set-LocalEnvironmentVariable "FCLI_DEFAULT_TENANT" $FCLI_FOD_TENANT
	}else {
		Set-LocalEnvironmentVariable "FCLI_DEFAULT_TENANT" $null
        Write-Host "Environment variable not found: FCLI_FOD_TENANT" -ForegroundColor Red
        break
    }

    $FCLI_FOD_USER = Get-LocalEnviornmentVariable "FCLI_FOD_USER"
	if ($FCLI_FOD_USER) {
        Set-LocalEnvironmentVariable "FCLI_DEFAULT_USER" $FCLI_FOD_USER
    } else {
        Set-LocalEnvironmentVariable "FCLI_DEFAULT_USER" $null
        Write-Host "Environment variable not found: FCLI_FOD_USER" -ForegroundColor Red
        break
    }

    $FCLI_FOD_PWD = Get-LocalEnviornmentVariable "FCLI_FOD_PWD"
	if ($FCLI_FOD_PWD) {
        $FCLI_FOD_PWD_UNSECURE = [System.Net.NetworkCredential]::new("", $FCLI_FOD_PWD).Password
        Set-LocalEnvironmentVariable "FCLI_DEFAULT_PASSWORD" $FCLI_FOD_PWD_UNSECURE
    } else {
        Set-LocalEnvironmentVariable "FCLI_DEFAULT_PASSWORD" $null
        Write-Host "Environment variable not found: FCLI_FOD_PWD" -ForegroundColor Red
        break
    }
}

Function Clear_Variables {
<#
.SYNOPSIS
	Clear enviornment variable used in this script
.DESCRIPTION
	This PowerShell function clear required variables and stop the execution of the script.
#>
    Set-Location Env:
    #Remove-Item FCLI_MODULE -ErrorAction Ignore
    Remove-Item FCLI_DEFAULT_URL -ErrorAction Ignore
    Remove-Item FCLI_DEFAULT_TENANT -ErrorAction Ignore
    Remove-Item FCLI_DEFAULT_USER -ErrorAction Ignore
    Remove-Item FCLI_DEFAULT_PASSWORD -ErrorAction Ignore
    Remove-Item FCLI_TEST_MODULES -ErrorAction Ignore

    if ($global:blnLinux) {
    Set-Location -
    } else {
    Set-Location -Path C:
    }
}

function Join-String {
<#
.SYNOPSIS
	Util to join strings from array
.DESCRIPTION
	This PowerShell function util to join strings from array.
#>
    [CmdletBinding()]
    Param(
        [Parameter(Mandatory=$true,ValueFromPipeline=$true)] [string[]]$StringArray, 
        $Separator=",",
        [switch]$DoubleQuote=$false
    )
    BEGIN{
        $joinArray = [System.Collections.ArrayList]@()
    }
    PROCESS {
        foreach ($astring in $StringArray) {
            $joinArray.Add($astring) | Out-Null
        }
    }
    END {
        $Object = [PSCustomObject]@{}
        $count = 0;
        foreach ($aString in $joinArray) {

            $aString = $aString.Trim() + "*"
            $name = "ieo_$($count)"
            $Object | Add-Member -MemberType NoteProperty -Name $name -Value $aString;
            $count = $count + 1;
        }
        $ObjectCsv = $Object | ConvertTo-Csv -NoTypeInformation -Delimiter $separator
        $result = $ObjectCsv[1]
        if (-not $DoubleQuote) {
            $result = $result.Replace('","',",").TrimStart('"').TrimEnd('"')
        }
        return $result
    }
}

Function Run_Modules($sModuleNames) {
<#
.SYNOPSIS
	Run the modules given in enviornment variables.
.DESCRIPTION
	This PowerShell function locate selected JSON file to triggere demo data with testing.
#>
    $arrModules = $sModuleNames -split ','

    $strFinal = Join-String -StringArray $arrModules

    $strToExecute = "Get-Childitem -Path . -Recurse -Filter *.json -Include " + $strFinal
    $oFiles = Invoke-Expression $strToExecute

    $oFiles | Foreach-Object {
        Run_Test -sJsonLocation $_.FullName
    }
}

Function Run_Test($sJsonLocation) {
<#
.SYNOPSIS
	Run the tests given in json file.
.DESCRIPTION
	This PowerShell function take json path as parameter to run the test script with given json data.
#>
   Write-Host $sJsonLocation
   $FCLI_FOD_USER = Get-LocalEnviornmentVariable "FCLI_FOD_USER"
   $FCLI_DEFAULT_FOD_APP = Get-LocalEnviornmentVariable "FCLI_DEFAULT_FOD_APP"
   Write-Host $FCLI_FOD_USER
   Write-Host $Global:ENTITLE_ID
   Write-Host $FCLI_DEFAULT_FOD_APP

   [PSCustomObject]$json = [PSCustomObject]((Get-Content -Raw $sJsonLocation) -replace "{PATH}",$PSScriptRoot.Replace("\","\\") `
                            -replace "{FCLI_FOD_USER}",$FCLI_FOD_USER `
                            -replace "{FCLI_DEFAULT_FOD_APP}",$FCLI_DEFAULT_FOD_APP `
                            -replace "{ENTITLE_ID}",$Global:ENTITLE_ID `
                            -replace '(?m)(?<=^([^"]|"[^"]*")*)//.*' `
                            -replace '(?ms)/\*.*?\*/' | Out-String | ConvertFrom-Json)

   foreach ($jsCmd in $json) {
    $validateOutput = $null

    if($prop = $jsCmd.PSObject.Properties.Item('expectOutput')) {
        write-host ("Check output for :" + $prop.Value)
        $validateOutput = $jsCmd.expectOutput
    }
    else {
        write-host "No output found"
    }

    #Write-Host $jsCmd.commands + $jsCmd.arguments + $validateOutput -ForegroundColor Cyan
    if ($prop = $jsCmd.PSObject.Properties.Item('waitSeconds')) {
        Write-Host "Waiting " $jsCmd.waitSeconds " seconds to run this command." -ForegroundColor DarkYellow
        Start-Sleep -Seconds $jsCmd.waitSeconds
    }
    Run_Command -sCmd $jsCmd.commands -argument $jsCmd.arguments -valOutput $jsCmd.expectOutput

   }

}

Function Run_Command($sCmd, $argument, $valOutput) {
<#
.SYNOPSIS
	Run each command as defined.
.DESCRIPTION
	This PowerShell function execute command as defined in json file and output to the console.
#>
 Write-Host ("command: " + $sCmd)
 Write-Host ("arg: " + $argument)
 Write-Host ("Output: " + $valOutput)

 #$global:blnLinux = ($env:OS -eq "" -or $env:OS -eq $null)
 if ($global:blnLinux) {
    $sCmd = "java -jar " + "$global:FCLI_MODULE" + " " + $sCmd + " " + $argument
 }else{
    #$sCmd = "./" + $global:FCLI_MODULE + " " + $sCmd + " " + $argument
    $sCmd =  $global:FCLI_MODULE.Replace(' ','` ') + " " + $sCmd + " " + $argument
 }

 Write-Host ("Running Command: $($sCmd)") -ForegroundColor Cyan

 $retVal = Invoke-Expression $sCmd -ErrorAction Continue | Out-String
 Write-Host "Testing results returned:`n" $retVal

 if ($valOutput -ne $null -and $retVal -ne $null)
 {

    $found = $retVal -match $valOutput

    if ($found)
    {
        Write-Host "Test Result matched with Output parameter, PASSED" -ForegroundColor Green
    } else {
        Write-Host "Test Result did not matched with Output parameter, FAILS..." -ForegroundColor Red
    }
 }
 if ($valOutput -eq $null -and $retVal -ne $null)
 {
    Write-Host "TEST PASSED..." -ForegroundColor Green
 } 
 if ($valOutput -eq $null -and $retVal -eq $null)
 {
    Write-Host "TEST FAILED..." -ForegroundColor Red
 } 
}

#
# Main 
#
$Global:ENTITLE_ID = 393
Set-LocalEnvironmentVariable "FCLI_TEST_MODULES" "fod, ssc, scsast"
#$global:FCLI_MODULE="fcli-beta.exe"
$global:blnLinux = ($env:OS -eq "" -or $env:OS -eq $null)
$MODULE_INPUT_STR="Enter name and path of the command line tool along with extension[fcli.exe]"
if ($global:blnLinux) {
    $MODULE_INPUT_STR="Enter name and path of the command line tool along with extension[fcli.jar]"
    $global:FCLI_MODULE="fcli-beta.jar"
}

$ErrorActionPreference="SilentlyContinue"
Stop-Transcript | out-null
$ErrorActionPreference = "Continue"
Start-Transcript -path .\fcli-$(get-date -f yyyy-MM-dd).log -append
Validate_Variables
Run_Modules(Get-LocalEnviornmentVariable "FCLI_TEST_MODULES")
Clear_Variables
Stop-Transcript
