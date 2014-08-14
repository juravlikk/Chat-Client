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

public class Message implements Serializable {

    public byte type;
    public String text;
    public String from;
    public Vector clients;

    public Message(byte type, String text) {
        this.type = type;
        this.text = text;
    }

    public Message(byte type, Vector clients, String from, String text) {
        this.type = type;
        this.clients = clients;
        this.from = from;
        this.text = text;
    }

    public Message(byte type, Vector clients, String text) {
        this.type = type;
        this.clients = clients;
        this.text = text;
    }
}