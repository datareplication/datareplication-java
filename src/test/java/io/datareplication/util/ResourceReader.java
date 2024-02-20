package io.datareplication.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public final class ResourceReader {

    private ResourceReader() {
    }

    public static  String readFromInputStream(String pathToFile) throws IOException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResource(pathToFile).openStream();
        String result;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            result = br.lines().map(line -> line + "\n").collect(Collectors.joining());
        }
        return result;
    }

}
