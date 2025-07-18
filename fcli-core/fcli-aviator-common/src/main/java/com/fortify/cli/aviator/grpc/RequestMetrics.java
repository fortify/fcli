package com.fortify.cli.aviator.grpc;

class RequestMetrics {
    private final long startTime;
    private volatile long endTime = 0;
    private volatile String status = "PENDING";

    public RequestMetrics() {
        this.startTime = System.currentTimeMillis();
    }

    public void complete(String status) {
        this.endTime = System.currentTimeMillis();
        this.status = status;
    }

    public long getDuration() {
        return endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime;
    }
}