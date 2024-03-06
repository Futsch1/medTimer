package com.futsch1.medtimer.exporters;

import java.io.File;
import java.io.IOException;

public interface Exporter {
    void export(File file) throws IOException;

    String getExtension();
}
