/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.Client.net;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author Александр
 */
class JPlayer {
    
    private String filename;
    private AudioInputStream ais;

    public JPlayer(String filename) {
        this.filename = filename;
        this.initialize();
    }

    public void initialize()
    {
        try {
            ais = AudioSystem.getAudioInputStream(new File(filename));
        } catch (Exception e) {
            System.out.println("Exception in initializing. "+e);
        }
        AudioFormat format = ais.getFormat();
        SourceDataLine line = null;
        DataLine.Info    info = new DataLine.Info(SourceDataLine.class,format);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            int    nBytesRead = 0;
            byte[]    abData = new byte[128];
            while (nBytesRead != -1) {
                try {
                    nBytesRead = ais.read(abData, 0, abData.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (nBytesRead >= 0) {
                    int nBytesWritten = line.write(abData, 0, nBytesRead);
                }
           }
       } catch (Exception e) {
           e.printStackTrace();
           System.out.println("Exception in inintializing. getting line. " + e);
       }
       line.drain();
       line.close();
    }
}
