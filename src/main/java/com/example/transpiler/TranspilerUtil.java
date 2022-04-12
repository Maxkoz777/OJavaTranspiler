package com.example.transpiler;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TranspilerUtil {

    List<File> retrieveSourceLanguageFiles() {
        return getResourceFolderFiles("sourceCode");
    }

    List<File> retrieveSourceLanguageLibraryFiles() {
        return getResourceFolderFiles("lib");
    }

    private List<File> getResourceFolderFiles (String folder) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource(folder);
        assert url != null;
        return Arrays.asList(Objects.requireNonNull(new File(url.getPath()).listFiles()));
    }



}
