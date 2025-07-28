package com.fortify.cli.aviator.grpc;

import com.fortify.cli.aviator.audit.model.UserPrompt;

class RequestWrapper {
    final UserPrompt userPrompt;
    int attemptCount = 0;

    RequestWrapper(UserPrompt userPrompt) {
        this.userPrompt = userPrompt;
    }
}