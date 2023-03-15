#!/bin/bash -x

checkVars() {
    [[ -z "${FCLI_CMD}" ]] && echo "FCLI_CMD must be set to either 'java -jar path/to/fcli.jar' or path/to/fcli native binary" && exit 1
    [[ -z "${FCLI_DEFAULT_SSC_URL}" ]] && echo "FCLI_DEFAULT_SSC_URL must be set to SSC demo container URL" && exit 1
    [[ -z "${FCLI_DEFAULT_SSC_USER}" ]] && echo "FCLI_DEFAULT_SSC_USER must be set to SSC demo container user" && exit 1
    [[ -z "${FCLI_DEFAULT_SSC_PASSWORD}" ]] && echo "FCLI_DEFAULT_SSC_PASSWORD must be set to SSC demo container user password" && exit 1
}

sscCmd() {
    runCmd ${FCLI_CMD} ssc "$@"
}

runCmd() {
    echo "$@"
    lastOutput=$("$@") || exit 1 
    if [[ -v checkOutput[@] ]]; then
        echo "$lastOutput" | "${checkOutput[@]}" || exit 1
        unset checkOutput
    fi
}

runTestCommandsInSession() {
    checkOutput=(fgrep ${FCLI_DEFAULT_SSC_SESSION}); sscCmd session login
    runTestCommands
    sscCmd session logout
}

runTestCommands() {
    sscCmd activity-feed list
    sscCmd alert-definition list
    sscCmd alert list
    sscCmd app list
    sscCmd appversion list
    checkOutput=(fgrep DevPhase); sscCmd attribute-definition list
    sscCmd user list
    sscCmd event list
    sscCmd issue-template list
    sscCmd job list
    sscCmd plugin list
    checkOutput=(fgrep "OWASP Top 10"); sscCmd report-template list
    checkOutput=(fgrep Administrator); sscCmd role list
    checkOutput=(fgrep projectversion_add); sscCmd role-permission list
    checkOutput=(fgrep CIToken); sscCmd token-definition list
    
    checkOutput=(fgrep CIToken); sscCmd token create CIToken --expire-in 5m --store ciToken:restToken
    sscCmd token revoke {?ciToken:restToken}
    runCmd ${FCLI_CMD} state var delete ciToken

    appName="fcli-test $(date +%s)" 
    sscCmd appversion create "${appName}:v1" -d "Test fcli appversion create" --issue-template "Prioritized High Risk Issue Template" --auto-required-attrs --store currentAppVersion:id
    newAppVersionId="::currentAppVersion::id"
    checkOutput=(fgrep "No data"); sscCmd appversion-artifact list --appversion ${newAppVersionId}
    sscCmd appversion-attribute set "DevPhase=Active Development" --appversion ${newAppVersionId}
    sscCmd appversion-attribute list --appversion ${newAppVersionId}
    checkOutput=(fgrep "SKIPPED_EXISTING"); sscCmd appversion create "${appName}:v1" -d "Test fcli appversion create" --issue-template "Prioritized High Risk Issue Template" --auto-required-attrs --skip-if-exists
    sscCmd appversion create "${appName}:v2" -d "Test fcli appversion create" --issue-template "Prioritized High Risk Issue Template" --auto-required-attrs --store myAppVersion
    checkOutput=(fgrep "v2"); sscCmd appversion get ::myAppVersion::    
    sscCmd appversion create "${appName}:v3" -d "Test fcli appversion create" --issue-template "Prioritized High Risk Issue Template" --auto-required-attrs
    sscCmd app delete "${appName}" --delete-versions
    checkOutput=(fgrep -v "${appName}"); sscCmd appversion list
}

run() {
    checkVars
    FCLI_DEFAULT_SSC_SESSION=integration-test runTestCommandsInSession
}

run