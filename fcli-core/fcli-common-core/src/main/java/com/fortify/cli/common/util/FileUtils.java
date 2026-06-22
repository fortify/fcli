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
package com.fortify.cli.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import com.fortify.cli.common.exception.FcliSimpleException;

import lombok.SneakyThrows;

// TODO For now, methods provided in this class are only used by the tools module,
//      but potentially some methods or the full class could be moved to the common module.
public final class FileUtils {
    public static final Set<PosixFilePermission> execPermissions = PosixFilePermissions.fromString("rwxr-xr-x");
    private FileUtils() {}
    
    @SneakyThrows
    public static final InputStream openInputStream(Path path) {
        return !Files.exists(path) ? null : Files.newInputStream(path);
    }
    
    public static final InputStream openResourceInputStream(String resourcePath) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
    }
    
    @SneakyThrows
    public static final String readResourceAsString(String resourcePath, Charset charset) {
        return new String(checkCharset(readResourceAsBytes(resourcePath), charset), charset);
    }
    
    @SneakyThrows
    public static final String readInputStreamAsString(InputStream is, Charset charset) {
        return new String(checkCharset(is.readAllBytes(), charset), charset);
    }
    
    public static final byte[] checkCharset(byte[] bytes, Charset charset) {
        try {
            var decoder = charset.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            decoder.decode(ByteBuffer.wrap(bytes));
            return bytes;
        } catch ( Exception e ) {
            throw new FcliSimpleException("Input must be in "+charset.displayName()+" encoding");
        }
    }
    
    @SneakyThrows
    public static final byte[] readResourceAsBytes(String resourcePath) {
        try ( InputStream in = openResourceInputStream(resourcePath) ) {
            return in.readAllBytes();
        }
    }

    public static void writeStringWithOwnerOnlyPermissions(Path filePath, String contents) throws IOException {
        var parent = filePath.getParent();
        if ( parent!=null && !Files.exists(parent) ) {
            Files.createDirectories(parent);
        }
        Files.writeString(filePath, contents==null?"":contents, StandardCharsets.UTF_8);
        if ( FileSystems.getDefault().supportedFileAttributeViews().contains("posix") ) {
            Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rw-------"));
        } else {
            File file = filePath.toFile();
            file.setExecutable(false, false);
            file.setReadable(true, true);
            file.setWritable(true, true);
        }
    }

    public static String readString(Path filePath) throws IOException {
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }
    
    public static final void copyResource(String resourcePath, Path destinationFilePath, CopyOption... options) {
        var parent = destinationFilePath.getParent();
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new FcliSimpleException(String.format("Error creating directory %s", parent), e);
        }
        try ( InputStream in = openResourceInputStream(resourcePath) ) {
            Files.copy( in, destinationFilePath, options);
        } catch ( IOException e ) {
            throw new FcliSimpleException(String.format("Error copying resource %s to %s", resourcePath, destinationFilePath), e);
        }
    }
    
    public static final void copyResourceToDir(String resourcePath, Path destinationPath, CopyOption... options) {
        String fileName = Paths.get(resourcePath).getFileName().toString();
        copyResource(resourcePath, destinationPath.resolve(fileName), options);
    }
    
    @SneakyThrows
    public static final void moveFiles(Path sourcePath, Path targetPath, String regex) {
        Files.createDirectories(targetPath);
        try ( var ls = Files.list(sourcePath) ) {
            ls.map(Path::toFile)
                .map(File::getName)
                .filter(name->name.matches(regex))
                .forEach(name->move(sourcePath.resolve(name), targetPath.resolve(name)));
        }
    }

    public static final void move(Path source, Path target) {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FcliSimpleException(String.format("Error moving %s to %s", source, target), e);
        }
    }

    @SneakyThrows
    public static final void setAllFilePermissions(Path path, Set<PosixFilePermission> permissions, boolean recursive) {
        if ( path!=null && Files.exists(path) ) {
            if ( Files.isDirectory(path) ) {
                try (Stream<Path> walk = Files.walk(path)) {
                    walk.forEach(p->{
                        var isDir = Files.isDirectory(p);
                        if ( isDir && recursive ) {
                            setAllFilePermissions(p, permissions, recursive);
                        } else if ( !isDir ) {
                            setSinglePathPermissions(p, permissions);
                        }
                    });
                }
            }
        }
    }

    @SneakyThrows
    public static final void setSinglePathPermissions(Path p, Set<PosixFilePermission> permissions) {
        try {
            Files.setPosixFilePermissions(p, permissions);
        } catch ( UnsupportedOperationException e ) {
            // Log warning?
            // Ignore on filesystems that don't support POSIX permissions
        }
    }

    public static final Function<Path,Path> defaultExtractPathResolver(Path targetPath, Function<Path,Path> sourcePathRewriter) {
        return sourcePath->{
            var newSourcePath = sourcePathRewriter==null ? sourcePath : sourcePathRewriter.apply(sourcePath);
            var resolvedPath = targetPath.resolve(newSourcePath);
            if (!resolvedPath.startsWith(targetPath.normalize())) {
                // see: https://snyk.io/research/zip-slip-vulnerability
                throw new FcliSimpleException("Entry with an illegal path: " + sourcePath);
            }
            return resolvedPath;
        };
    }
    
    @SneakyThrows
    public static final void extractZip(File zipFile, Path targetDir) {
        extractZip(zipFile, defaultExtractPathResolver(targetDir, null));
    }
    
    @SneakyThrows
    public static final void extractZip(File zipFile, Function<Path, Path> extractPathResolver) {
        try (FileInputStream fis = new FileInputStream(zipFile); ZipInputStream zipIn = new ZipInputStream(fis)) {
            for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null; ) {
                Path resolvedPath = extractPathResolver.apply(Path.of(ze.getName())).normalize();
                if (ze.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    Files.createDirectories(resolvedPath.getParent());
                    Files.copy(zipIn, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
    
    @SneakyThrows
    public static final void extractTarGZ(File tgzFile, Path targetDir) {
        extractTarGZ(tgzFile, defaultExtractPathResolver(targetDir, null));
    }
    
    @SneakyThrows
    public static final void extractTarGZ(File tgzFile, Function<Path,Path> extractPathResolver) {
        try (InputStream source = Files.newInputStream(tgzFile.toPath());
                GZIPInputStream gzip = new GZIPInputStream(source);
                TarArchiveInputStream tar = new TarArchiveInputStream(gzip)) {

            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                Path extractTo = extractPathResolver.apply(Path.of(entry.getName()));
                if(entry.isDirectory()) {
                    Files.createDirectories(extractTo);
                } else {
                    Files.copy(tar, extractTo, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
    
    /**
     * Recursively delete the given path. As a best practice, this method should
     * only be invoked if {@link #isDirPathInUse(Path)} returns false. The
     * deleteRecursive() method itself doesn't invoke {@link #isDirPathInUse(Path)}
     * for performance reasons, as callers may wish to explicitly check whether
     * any files are in use in order to perform some alternative action.
     * @param path
     */
    @SneakyThrows
    public static final void deleteRecursive(Path path) {
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
    
    @SneakyThrows
    public static final boolean isDirPathInUse(Path path) {
        if ( isDirPathInUseByCurrentExecutable(path) ) { return true; }
        try (Stream<Path> walk = Files.walk(path)) {
            return walk.anyMatch(FileUtils::isFilePathInUse);
        }
    }
    
    @SneakyThrows
    public static final boolean isDirPathInUseByCurrentExecutable(Path path) {
        var currentExecutablePath = Path.of(FileUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return currentExecutablePath.normalize().startsWith(path.normalize());
    }
    
    @SneakyThrows
    public static final boolean isFilePathInUse(Path path) {
        if ( path.toFile().isFile() ) {
            try ( var fc = FileChannel.open(path, StandardOpenOption.APPEND) ) {
                if ( fc.tryLock()==null ) {
                    return true;
                }
            } catch ( FileSystemException e ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convert the given path to a string, using the given separator character.
     * If given path is null, null will be returned.
     */
    public static final String pathToString(Path path, char separatorChar) {
        if ( path==null ) { return null; }
        StringBuilder result = new StringBuilder();
        path.iterator().forEachRemaining(part -> result.append(separatorChar).append(part));
        if ( !path.isAbsolute() ) { result.deleteCharAt(0); } // Remove leading separator character if path is not absolute
        // TODO If original path contains a drive letter, should we include this?
        return result.toString();
    }
    
    /**
     * Process files matching a glob pattern using a stream processor function.
     * Uses Java NIO {@link java.nio.file.PathMatcher} glob syntax:
     * {@code *} matches within a single path segment, {@code **} matches across directories.
     * 
     * @param <R> Return type of the processor function
     * @param baseDir Base directory to search from
     * @param globPattern Glob pattern relative to baseDir
     * @param maxDepth Maximum directory depth to search
     * @param streamProcessor Function to process the stream of matching paths
     * @return Result from the stream processor function
     */
    @SneakyThrows
    public static final <R> R processMatchingFileStream(Path baseDir, String globPattern, int maxDepth, 
            Function<Stream<Path>, R> streamProcessor) {
        return processMatchingStream(baseDir, globPattern, maxDepth, Files::isRegularFile, streamProcessor);
    }
    
    /**
     * Process directories matching a glob pattern using a stream processor function.
     * Uses Java NIO {@link java.nio.file.PathMatcher} glob syntax.
     * 
     * @param <R> Return type of the processor function
     * @param baseDir Base directory to search from
     * @param globPattern Glob pattern relative to baseDir
     * @param maxDepth Maximum directory depth to search
     * @param streamProcessor Function to process the stream of matching paths
     * @return Result from the stream processor function
     */
    @SneakyThrows
    public static final <R> R processMatchingDirStream(Path baseDir, String globPattern, int maxDepth,
            Function<Stream<Path>, R> streamProcessor) {
        return processMatchingStream(baseDir, globPattern, maxDepth, Files::isDirectory, streamProcessor);
    }
    
    /**
     * Process paths matching a glob pattern using a stream processor function.
     * Uses Java NIO {@link java.nio.file.PathMatcher} glob syntax:
     * {@code *} matches within a single path segment, {@code **} matches across directories,
     * {@code ?} matches a single character, {@code [...]} matches character classes,
     * {@code {...}} matches alternatives.
     * 
     * @param <R> Return type of the processor function
     * @param baseDir Base directory to search from
     * @param globPattern Glob pattern relative to baseDir
     * @param maxDepth Maximum directory depth to search
     * @param pathFilter Predicate to filter paths (e.g., Files::isRegularFile, Files::isDirectory)
     * @param streamProcessor Function to process the stream of matching paths
     * @return Result from the stream processor function
     */
    @SneakyThrows
    public static final <R> R processMatchingStream(Path baseDir, String globPattern, int maxDepth,
            Predicate<Path> pathFilter, Function<Stream<Path>, R> streamProcessor) {
        if (baseDir == null || !Files.isDirectory(baseDir)) {
            throw new FcliSimpleException("Base directory must be a valid directory");
        }
        if (pathFilter == null) {
            throw new FcliSimpleException("Path filter must not be null");
        }
        var pathMatcher = baseDir.getFileSystem().getPathMatcher("glob:" + normalizeGlobForRootMatch(globPattern));
        try (Stream<Path> paths = Files.walk(baseDir, maxDepth)) {
            Stream<Path> filtered = paths
                .filter(p -> !p.equals(baseDir))
                .filter(pathFilter)
                .map(baseDir::relativize)
                .filter(pathMatcher::matches)
                .map(baseDir::resolve);
            
            return streamProcessor.apply(filtered);
        }
    }
    
    /**
     * Process paths matching a glob path that may contain glob characters at any position.
     * Automatically splits the path into a base directory (the longest prefix without
     * glob characters) and a glob pattern, then walks the base directory matching against
     * the glob pattern.
     * <p>
     * If the path contains no glob characters, checks if the exact path exists and matches
     * the filter. Returns an empty stream (not an exception) if the base directory does
     * not exist.
     * <p>
     * Glob characters are: {@code *}, {@code ?}, {@code [}, <code>&#123;</code>.
     * If the glob contains {@code **}, traversal depth is unlimited; otherwise it
     * is limited to the number of path segments in the glob tail.
     * 
     * @param <R> Return type of the processor function
     * @param globPath Absolute or relative path that may contain glob characters
     * @param pathFilter Predicate to filter paths (e.g., Files::isRegularFile, Files::isDirectory)
     * @param streamProcessor Function to process the stream of matching paths
     * @return Result from the stream processor function
     */
    @SneakyThrows
    public static final <R> R processGlobPathStream(String globPath,
            Predicate<Path> pathFilter, Function<Stream<Path>, R> streamProcessor) {
        if (globPath == null || globPath.isBlank()) {
            throw new FcliSimpleException("Glob path cannot be null or empty");
        }
        var normalized = globPath.replace('\\', '/');
        int firstGlob = indexOfFirstGlobChar(normalized);
        if (firstGlob < 0) {
            // No glob characters — check exact path
            var path = Path.of(globPath);
            return streamProcessor.apply(
                Files.exists(path) && pathFilter.test(path)
                    ? Stream.of(path) : Stream.empty());
        }
        int lastSep = normalized.lastIndexOf('/', firstGlob);
        var baseDirStr = lastSep > 0 ? normalized.substring(0, lastSep)
            : lastSep == 0 ? "/" : ".";
        var baseDir = Path.of(baseDirStr);
        if (!Files.isDirectory(baseDir)) {
            return streamProcessor.apply(Stream.empty());
        }
        var globTail = normalized.substring(lastSep + 1);
        int maxDepth = globTail.contains("**")
            ? Integer.MAX_VALUE
            : (int) globTail.chars().filter(c -> c == '/').count() + 1;
        return processMatchingStream(baseDir, globTail, maxDepth, pathFilter, streamProcessor);
    }
    
    private static int indexOfFirstGlobChar(String path) {
        for (int i = 0; i < path.length(); i++) {
            if ("*?[{".indexOf(path.charAt(i)) >= 0) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * NIO PathMatcher's {@code **}{@code /X} requires at least one directory level
     * before X, unlike common glob conventions where {@code **}{@code /} can match zero
     * levels. This wraps such patterns with an alternation so root-level paths also match.
     * For example, {@code **}{@code /*.jar} becomes <code>{*.jar,**&#47;*.jar}</code>.
     */
    private static String normalizeGlobForRootMatch(String globPattern) {
        if (globPattern.startsWith("**/")) {
            return "{" + globPattern.substring(3) + "," + globPattern + "}";
        }
        return globPattern;
    }
}
