package com.example.androidserver;

import java.io.File;

public class DisplayFile {
    private static String path;

    void cunstructor(String path) {
        this.path = path;
    }
    public void printFileNames(File[] a, int i, int lvl) {
        // base case of the recursion
        // i == a.length means the directory has
        // no more files. Hence, the recursion has to stop
        if (i == a.length) {
            return;
        }
        // checking if the encountered object is a file or not
        if (a[i].isFile()) {
            System.out.println(a[i].getName());
        }
        // recursively printing files from the directory
        // i + 1 means look for the next file
        printFileNames(a, i + 1, lvl);
    }
}
