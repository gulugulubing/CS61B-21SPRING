package gitlet;

import java.io.Serializable;

/** Represents a Bulb object which stores a committed file's infos
 *  Every Bulb maps a file.
 *  So every Bulb should store the fileId and file name.
 *  FileId is unique and also is shared for serialized Bulb's name.
 *  Files and  serialized Bulb will be saved in the same dir.
 *
 */

public class Blob implements Serializable {


    private String fileName;
    private String fileContent;

    Blob (String fileName, String fileContent) {
        this.fileContent = fileContent;
        this.fileName =fileName;
    }

    public String getFileContent() {
        return fileContent;
    }

    public String getFileName() {
        return fileName;
    }
}
