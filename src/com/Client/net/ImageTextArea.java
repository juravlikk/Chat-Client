/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.Client.net;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;

/**
 *
 * @author Александр
 */
public class ImageTextArea extends JTextArea {

    private Image img;
    private JScrollPane scroll;
    public ImageTextArea() {
    }

    public ImageTextArea(String text) {
        super(text);
    }
    
    public ImageTextArea (Image img) {
        this.img = img;
    }

    @Override
    public void paint(Graphics g) {
        Rectangle r = ((JViewport) scroll.getViewport()).getViewRect();
        g.drawImage(img, 0, r.y, r.width, r.height, this);
        this.setOpaque(false);
        super.paint(g);
        this.setOpaque(true);
    }

    @Override
    public String getText() {
        return super.getText();
    }

    @Override
    public void setText(String t) {
        super.setText(t);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
    
    public void setImage(Image img) {
        this.img = img;
    }
    
    public void setScrollPane(JScrollPane scroll) {
        this.scroll = scroll;
    }
}
