package com.fortify.cli.tools.model;

import io.micronaut.core.annotation.ReflectiveAccess;

@ReflectiveAccess
public enum ChecksumType {
    MD5,
    SHA256,
    SHA512,
    SHA3,
    URL,
    UNKNOWN
}
