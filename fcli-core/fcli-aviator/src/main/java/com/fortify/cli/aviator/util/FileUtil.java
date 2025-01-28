package com.fortify.cli.aviator.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public final class FileUtil {

    private FileUtil() {
    }

    public static boolean isZipFile(String fileName) {
        return isZipFile(new File(fileName));
    }

    public static boolean isZipFile(File file) {
        ZipInputStream zip = null;
        FileInputStream stream = null;

        boolean var4;
        try {
            stream = new FileInputStream(file);
            zip = new ZipInputStream(stream);
            ZipEntry zipEntry = zip.getNextEntry();
            var4 = zipEntry != null;
            return var4;
        } catch (IOException var8) {
            var4 = false;
        } finally {
            close((InputStream) zip);
            close((InputStream) stream);
        }

        return var4;
    }

    public static boolean deleteDirectoryStructure(File directory) {
        return directory.isDirectory() ? deleteDirStructHelper(directory, new HashSet(), (Pattern) null) : false;
    }

    public static boolean deleteDirectoryStructure(File directory, Pattern pattern) {
        return directory.isDirectory() ? deleteDirStructHelper(directory, new HashSet(), pattern) : false;
    }

    private static boolean deleteDirStructHelper(File dir, HashSet<String> dirList, Pattern pattern) {
        try {
            String dirName = dir.getCanonicalPath();
            if (dirList.contains(dirName)) {
                return false;
            }

            dirList.add(dirName);
        } catch (Exception var5) {
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return false;
        } else {
            for (int i = 0; i < files.length; ++i) {
                if (files[i].isDirectory()) {
                    deleteDirStructHelper(files[i], dirList, pattern);
                } else if (pattern == null || pattern.matcher(files[i].getName()).matches()) {
                    files[i].delete();
                }
            }

            if (dir.listFiles() != null && dir.listFiles().length == 0) {
                return dir.delete();
            } else {
                return false;
            }
        }
    }

    public static String getFileExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot > 0) {
            return fileName.substring(lastIndexOfDot + 1);
        }
        return "";
    }

    public static IOException close(InputStream in) {
        if (in == null) {
            return null;
        } else {
            try {
                in.close();
                return null;
            } catch (IOException var2) {
                IOException ex = var2;
                return ex;
            }
        }
    }

    public static IOException close(Reader in) {
        if (in == null) {
            return null;
        } else {
            try {
                in.close();
                return null;
            } catch (IOException var2) {
                IOException ex = var2;
                return ex;
            }
        }
    }

    public static IOException close(ZipFile zip) {
        if (zip == null) {
            return null;
        } else {
            try {
                zip.close();
                return null;
            } catch (IOException var2) {
                IOException ex = var2;
                return ex;
            }
        }
    }

    public static boolean isDirectory(String pathString) {
        Path path = Paths.get(pathString);
        return Files.isDirectory(path);
    }


}
