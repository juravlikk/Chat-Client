/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.net;

/**
 *
 * @author Александр
 */
import java.io.Serializable;
import java.util.Vector;

public class FileMessage implements Serializable {

    public byte type;
    public String from;
    public Vector clients;
    public String file;
    public boolean end;

    public FileMessage(byte type, String from, Vector clients, String file, boolean end) {
        this.type = type;
        this.from = from;
        this.clients = clients;
        this.file = file;
        this.end = end;
    }
    
    public FileMessage(byte type, String from) {
        this.type = type;
        this.from = from;
    }
}