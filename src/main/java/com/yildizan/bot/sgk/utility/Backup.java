package com.yildizan.bot.sgk.utility;

import com.yildizan.bot.sgk.model.Watch;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class Backup {

    private Backup() {}

    public static List<Watch> read() throws Exception {
        List<Watch> watches = new ArrayList<>();
        if(Files.notExists(Paths.get(Constants.BACKUP_FILE))) {
            return watches;
        }

        File file = null;
        FileInputStream fileStream = null;
        ObjectInputStream objectStream = null;
        try {
            file = new File(Constants.BACKUP_FILE);
            fileStream = new FileInputStream(file);
            objectStream = new ObjectInputStream(fileStream);
            Watch watch = (Watch) objectStream.readObject();
            do {
                watches.add(watch);
                watch = (Watch) objectStream.readObject();
            }
            while (watch != null);
        }
        catch (Exception ignored) {}
        finally {
            if(objectStream != null) {
                objectStream.close();
            }
            if(fileStream != null) {
                fileStream.close();
            }
            if(file != null) {
                Files.delete(Paths.get(Constants.BACKUP_FILE));
            }
        }

        return watches;
    }

    public static void write(List<Watch> watches) throws Exception {
        File file = null;
        FileOutputStream fileStream = null;
        ObjectOutputStream objectStream = null;
        try {
            file = new File(Constants.BACKUP_FILE);
            fileStream = new FileOutputStream(file);
            objectStream = new ObjectOutputStream(fileStream);
            for(Watch watch : watches) {
                objectStream.writeObject(watch);
            }
        }
        catch (Exception e) {
            if(file != null) {
                Files.delete(Paths.get(Constants.BACKUP_FILE));
            }
        }
        finally {
            if(objectStream != null) {
                objectStream.close();
            }
            if(fileStream != null) {
                fileStream.close();
            }
        }
    }

}
