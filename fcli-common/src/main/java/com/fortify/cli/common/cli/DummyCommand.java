package com.fortify.cli.common.cli;

import picocli.CommandLine;

@CommandLine.Command(
        name = "dummy",
        description = "This is a placeholder (dummy) command. More commands will be added later."
)
public class DummyCommand implements Runnable{

    /**
     * This is just a dummy placeholder command when implementing product entity commands
     * <p>
     * Eventually be removed once all entities have been implemented. Or maybe not, I don't know yet.
     */
    @Override
    public void run() {
        System.out.println("Work in progress. More functionality will be added later.");
    }
}
