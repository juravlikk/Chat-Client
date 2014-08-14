/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.Client.net;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 *
 * @author Александр
 */
class CellRenderer extends DefaultListCellRenderer {
    
    private Hashtable iconTable = new Hashtable();
    private Map<Object, List> messageTable = new HashMap<Object, List>();
    
    @Override
    public Component getListCellRendererComponent(JList jlist, Object value, int cellIndex, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(jlist, value, cellIndex, isSelected, cellHasFocus);
        ImageIcon image = (ImageIcon) iconTable.get(value);
        label.setIcon(image);
        return label;
    }
    
    public void setImageIcon(ImageIcon image, Object value) {
        iconTable.put(value, image);
    }

    public void removeImageIcon(Object value) {
        iconTable.remove(value);
    }
    
    public Object getIcon(Object value) {
        return iconTable.get(value);
    }
    
    public void addMessage(Object value, Object message) {
        List l = messageTable.get(value);
        if (l == null) {
            l = new ArrayList();
        }
        l.add(message);
        messageTable.put(value, l);
    }
    
    public void removeFirstMessage(Object value) {
        List l = messageTable.get(value);
        l.remove(0);
        messageTable.put(value, l);
    }

    public Object getFirstMessage(Object value) {
        List l = messageTable.get(value);
        return l.get(0);
    }
    
    public List getMessages(Object value) {
        return messageTable.get(value);
    }
}