/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.Client.net;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;



/**
 *
 * @author Александр
 */
class ListRenderer extends JCheckBox implements ListCellRenderer {
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
        setEnabled(list.isEnabled());
        setSelected(((CheckListItem)value).isSelected());
        setFont(list.getFont());
        setBackground(list.getBackground());
        setForeground(list.getForeground());
        setText(value.toString());
        return this;
   }
}

class CheckListItem implements Comparable {
    private String  label;
    private boolean isSelected = false;

    public CheckListItem(String label) {
        this.label = label;
    }

    public CheckListItem(String label, boolean isSelected) {
        this.label = label;
        this.isSelected = isSelected;
    }
    
    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
   }

   public String toString() {
      return label;
   }

    @Override
    public int compareTo(Object o) {
        CheckListItem item = (CheckListItem) o;
        return label.compareTo(item.label);
    }
}