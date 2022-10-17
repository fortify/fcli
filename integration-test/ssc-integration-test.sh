#!/bin/bash -x

SSC_SESSION_NAME=integration-test

checkVars() {
    [[ -z "${FCLI_CMD}" ]] && echo "FCLI_CMD must be set to either 'java -jar path/to/fcli.jar' or path/to/fcli native binary" && exit 1
    [[ -z "${FCLI_SSC_URL}" ]] && echo "FCLI_SSC_URL must be set to SSC demo container URL" && exit 1
    [[ -z "${FCLI_SSC_USER}" ]] && echo "FCLI_SSC_USER must be set to SSC demo container user" && exit 1
    [[ -z "${FCLI_SSC_PASSWORD}" ]] && echo "FCLI_SSC_PASSWORD must be set to SSC demo container user password" && exit 1
}

sscSessionCmd() {
    sscCmd "$@" --session ${SSC_SESSION_NAME}
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
    checkOutput=(fgrep ${SSC_SESSION_NAME}); sscCmd session login ${SSC_SESSION_NAME}
    runTestCommands
    sscCmd session logout ${SSC_SESSION_NAME}
}

runTestCommands() {
    sscSessionCmd activity-feed list
    sscSessionCmd alert-definition list
    sscSessionCmd alert list
    sscSessionCmd app list
    sscSessionCmd appversion list
    checkOutput=(fgrep DevPhase); sscSessionCmd attribute-definition list
    sscSessionCmd user list
    sscSessionCmd event list
    sscSessionCmd issue-template list
    sscSessionCmd job list
    sscSessionCmd plugin list
    checkOutput=(fgrep "OWASP Top 10"); sscSessionCmd report-template list
    checkOutput=(fgrep Administrator); sscSessionCmd role list
    checkOutput=(fgrep projectversion_add); sscSessionCmd role-permission list
    checkOutput=(fgrep CIToken); sscSessionCmd token-definition list
    
    checkOutput=(fgrep CIToken); sscSessionCmd token create CIToken --expire-in 5m --store ciToken=restToken
    sscSessionCmd token revoke {?ciToken:restToken}
    runCmd ${FCLI_CMD} config var def delete ciToken

    appName="fcli-test $(date +%s)" 
    sscSessionCmd appversion create "${appName}:v1" -d "Test fcli appversion create" --issue-template "Prioritized High Risk Issue Template" --auto-required-attrs --store currentAppVersion=id
    # TODO Current commands don't properly produce singular output; once this is fixed, we can simply use {?currentAppVersion:id} 
    newAppVersionId="{?currentAppVersion:id}"
    checkOutput=(fgrep "No data"); sscSessionCmd appversion-artifact list --appversion ${newAppVersionId}
    sscSessionCmd appversion-attribute set "DevPhase=Active Development" --appversion ${newAppVersionId}
    sscSessionCmd appversion-attribute list --appversion ${newAppVersionId}
    sscSessionCmd appversion create "${appName}:v2" -d "Test fcli appversion create" --issue-template "Prioritized High Risk Issue Template" --auto-required-attrs --store ?
    checkOutput=(fgrep "v2"); sscSessionCmd appversion get ?    
    sscSessionCmd appversion create "${appName}:v3" -d "Test fcli appversion create" --issue-template "Prioritized High Risk Issue Template" --auto-required-attrs
    sscSessionCmd app delete "${appName}" --delete-versions
    checkOutput=(fgrep -v "${appName}"); sscSessionCmd appversion list
}

run() {
    checkVars
    runTestCommandsInSession
}

run