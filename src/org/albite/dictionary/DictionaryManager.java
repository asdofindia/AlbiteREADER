/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.albite.dictionary;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import org.albite.book.model.Languages;

/**
 *
 * @author albus
 */
public class DictionaryManager {
    private String folder = null;

    private short language = Languages.LANG_UNKNOWN;

    private LocalDictionary[]   localDictionaries = null;
//    private WebDictionary[]     webDictionaries = null;

    private LocalDictionary     currentBookDictionary = null;
    private LocalDictionary[]   currentLocalDictionaries = null;
//    private WebDictionary[]     currentWebDictionaries = null;

    public final void reloadDictionaries(final String folder) {
        this.folder = folder;
        reloadDictionaries();
    }

    public final void reloadDictionaries() {

        if (folder != null) {
            final Vector dicts = new Vector();

            try {
                FileConnection f = (FileConnection) Connector.open(folder);
                System.out.println("Trying " + folder);
                if (f.exists() && f.isDirectory()) {
                    System.out.println("Worked!");

                    final Enumeration filesList =
                            f.list("*" + LocalDictionary.FILE_EXTENSION, true);

                    while(filesList.hasMoreElements()) {
                        try {
                            final String s = (String) filesList.nextElement();
                            System.out.println("Opening " + s);
                            final FileConnection file =
                                    (FileConnection)Connector.open(folder + s);

                            final Dictionary dict = new LocalDictionary(file);

                            dicts.addElement(dict);
                        }

                        /*
                         * If an exception is thrown, just skip the dict
                         */
                        catch (IOException e) {}
                        catch (DictionaryException e) {}
                    }
                }
            } catch (IOException e) {

            } catch (IllegalArgumentException e) {}

            final int size = dicts.size();

            if (size > 0) {
                localDictionaries = new LocalDictionary[size];

                for (int i = 0; i < size; i++) {
                    localDictionaries[i] = (LocalDictionary) dicts.elementAt(i);
                }
            } else {
                localDictionaries = null;
            }
        }
    }

    public final void setLanguage(short language) {

        /*
         * Do work, only if language has changed.
         */
        if (this.language != language && localDictionaries != null) {
            this.language = language;
            updateCurrentDictionaries();
        }
        this.language = language;
    }

    public final void updateCurrentDictionaries() {
        /*
         * Unload current dicts
         */
        if (localDictionaries != null) {
            for (int i = 0; i < localDictionaries.length; i++) {
                localDictionaries[i].unload();
            }
        }

        if (localDictionaries == null) {
            currentLocalDictionaries = null;
            return;
        }

        /*
         * Make new list
         */
        Vector v = new Vector();
        for (int i = 0; i < localDictionaries.length; i++) {
            final LocalDictionary d = localDictionaries[i];

            System.out.println("??" + d.getTitle() + ", "
                    + d.getLanguage() + " <->" + language);

            if (d.getLanguage() == language) {
                System.out.println("Adding dictionary: " + d.getTitle());
                v.addElement(d);
            }
        }

        final int size = v.size();

        if (size > 0) {
            currentLocalDictionaries = new LocalDictionary[size];

            for (int i = 0; i < size; i++) {
                currentLocalDictionaries[i] = (LocalDictionary) v.elementAt(i);
            }
        } else {
            currentLocalDictionaries = null;
        }
    }

    public final void setCurrentBookDictionary(final LocalDictionary d) {
        currentBookDictionary = d;
    }

    public final Dictionary getCurrentBookDictionary() {
        return currentBookDictionary;
    }

    public final LocalDictionary[] getCurrentLocalDictionaries() {
        return currentLocalDictionaries;
    }

    public final void unloadDictionaries() {
        if (currentBookDictionary != null) {
            currentBookDictionary.unload();
        }

        if (currentLocalDictionaries != null) {
            for (int i = 0; i < currentLocalDictionaries.length; i++) {
                currentLocalDictionaries[i].unload();
            }
        }
    }
}