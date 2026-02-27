/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.action.helper.fs;

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.util;

import java.nio.file.Files;
import java.nio.file.Path;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;

/**
 * SpEL functions for file system operations in action workflows.
 * These functions allow actions to validate file and directory existence,
 * permissions, and types before attempting operations that might fail.
 * 
 * Available via the {@code #fs} SpEL variable in action YAML files.
 * 
 * @author rsenden
 */
@Reflectable
@SpelFunctionPrefix("fs.")
public class ActionFileSystemSpelFunctions {
    
    @SpelFunction(cat=util, desc="Checks whether a file or directory exists at the given path.",
            returns="`true` if the file or directory exists, `false` otherwise")
    public boolean exists(
            @SpelFunctionParam(name="path", desc="the file or directory path to check") String path)
    {
        return Files.exists(Path.of(path));
    }
    
    @SpelFunction(cat=util, desc="Checks whether the given path points to an existing directory.",
            returns="`true` if path exists and is a directory, `false` otherwise")
    public boolean isDirectory(
            @SpelFunctionParam(name="path", desc="the path to check") String path)
    {
        Path p = Path.of(path);
        return Files.exists(p) && Files.isDirectory(p);
    }
    
    @SpelFunction(cat=util, desc="Checks whether the given path points to an existing, readable regular file.",
            returns="`true` if path exists, is a regular file, and is readable, `false` otherwise")
    public boolean isReadableFile(
            @SpelFunctionParam(name="path", desc="the file path to check") String path)
    {
        Path p = Path.of(path);
        return Files.exists(p) && Files.isRegularFile(p) && Files.isReadable(p);
    }
    
    @SpelFunction(cat=util, desc="Checks whether the given path points to an existing, writable directory.",
            returns="`true` if path exists, is a directory, and is writable, `false` otherwise")
    public boolean isWritableDir(
            @SpelFunctionParam(name="path", desc="the directory path to check") String path)
    {
        Path p = Path.of(path);
        return Files.exists(p) && Files.isDirectory(p) && Files.isWritable(p);
    }
    
    @SpelFunction(cat=util, desc="Checks whether the given path points to a regular file (may or may not exist).",
            returns="`true` if path is a regular file, `false` otherwise")
    public boolean isFile(
            @SpelFunctionParam(name="path", desc="the path to check") String path)
    {
        return Files.isRegularFile(Path.of(path));
    }
    
    @SpelFunction(cat=util, desc="Checks whether the given file or directory is readable.",
            returns="`true` if path exists and is readable, `false` otherwise")
    public boolean isReadable(
            @SpelFunctionParam(name="path", desc="the path to check") String path)
    {
        return Files.isReadable(Path.of(path));
    }
    
    @SpelFunction(cat=util, desc="Checks whether the given file or directory is writable.",
            returns="`true` if path exists and is writable, `false` otherwise")
    public boolean isWritable(
            @SpelFunctionParam(name="path", desc="the path to check") String path)
    {
        return Files.isWritable(Path.of(path));
    }
}
