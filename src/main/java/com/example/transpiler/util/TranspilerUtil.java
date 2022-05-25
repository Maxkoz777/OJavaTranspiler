package com.example.transpiler.util;

import com.example.transpiler.typeChecker.TypeChecker;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TranspilerUtil {

    /**
     * incrementing number of future trees for typeChecker by number of files
     * @return List of source files
     */
    public List<File> retrieveSourceLanguageFiles() {
        List<File> files = getResourceFolderFiles("sourceCode");
        TypeChecker.treesCount += files.size();
        return files;
    }

    /**
     * incrementing number of future trees for typeChecker by number of files
     * @return List of library files
     */
    public List<File> retrieveSourceLanguageLibraryFiles() {
        List<File> files = getResourceFolderFiles("lib");
        TypeChecker.treesCount += files.size();
        return files;
    }

    /**
     *
     * @param folder - name of folder from resources
     * @return List of files from that folder
     */
    private List<File> getResourceFolderFiles (String folder) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource(folder);
        assert url != null;
        return Arrays.asList(Objects.requireNonNull(new File(url.getPath()).listFiles()));
    }



}
