package com.fortify.cli.sc_sast.scan.cli.cmd;

import com.fortify.cli.sc_sast.rest.cli.cmd.AbstractSCSastUnirestRunnerCommand;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Command;

@Command(name = "start")
public class SCSastScanStartCommand extends AbstractSCSastUnirestRunnerCommand {

    @Override
    protected Void run(UnirestInstance unirest) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
