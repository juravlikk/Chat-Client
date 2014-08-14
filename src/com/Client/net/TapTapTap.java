/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.Client.net;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 *
 * @author Александр
 */
public class TapTapTap {
    
    private Rectangle location;
    
    public TapTapTap(Rectangle location) {
        this.location = location;
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createUI();
            }
        });
    }
    
    public void createUI() {
        final JDialog f = new JDialog();
        f.setUndecorated(true);
        final WaitLayerUI layerUI = new WaitLayerUI();
        JPanel panel = createPanel();
        JLayer<JPanel> jlayer = new JLayer<JPanel>(panel, layerUI);
    
        final Timer stopper = new Timer(2500, new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                layerUI.stop();
                f.setVisible(false);
            }
        });
        stopper.setRepeats(false);
        layerUI.start();
        if (!stopper.isRunning()) {
            stopper.start();
        }
        f.add (jlayer);
        f.setSize(location.width, location.height);
        f.setLocation(location.getLocation());
        f.setOpacity(0.5f);
        f.setVisible (true);
    }

    private JPanel createPanel() {
        JPanel p = new JPanel();
        return p;
    }
}

class WaitLayerUI extends javax.swing.plaf.LayerUI<JPanel> implements ActionListener {
    private boolean mIsRunning;
    private boolean mIsFadingOut;
    private Timer mTimer;

    private int mAngle;
    private int mFadeCount;
    private int mFadeLimit = 15;

    @Override
    public void paint (Graphics g, JComponent c) {
        int w = c.getWidth();
        int h = c.getHeight();

        // Paint the view.
        super.paint (g, c);

        if (!mIsRunning) {
            return;
        }

        Graphics2D g2 = (Graphics2D)g.create();

        float fade = (float)mFadeCount / (float)mFadeLimit;
        // Gray it out.
        Composite urComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f * fade));
        g2.fillRect(0, 0, w, h);
        g2.setComposite(urComposite);

        // Paint the wait indicator.
        int s = Math.min(w, h) / 5;
        int cx = w / 2;
        int cy = h / 2;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(s / 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setPaint(Color.white);
        g2.rotate(Math.PI * mAngle / 180, cx, cy);
        for (int i = 0; i < 12; i++) {
            float scale = (11.0f - (float)i) / 11.0f;
            g2.drawLine(cx + s, cy, cx + s * 2, cy);
            g2.rotate(-Math.PI / 6, cx, cy);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, scale * fade));
        }
        
        g2.dispose();
    }

    public void actionPerformed(ActionEvent e) {
        if (mIsRunning) {
            firePropertyChange("tick", 0, 1);
            mAngle += 3;
            if (mAngle >= 360) {
                mAngle = 0;
            }
            if (mIsFadingOut) {
                if (--mFadeCount == 0) {
                    mIsRunning = false;
                    mTimer.stop();
                }
            } else if (mFadeCount < mFadeLimit) {
                mFadeCount++;
            }
        }
    }

    public void start() {
        if (mIsRunning) {
            return;
        }
    
        // Run a thread for animation.
        mIsRunning = true;
        mIsFadingOut = false;
        mFadeCount = 0;
        int fps = 24;
        int tick = 1000 / fps;
        mTimer = new Timer(tick, this);
        mTimer.start();
    }

    public void stop() {
        mIsFadingOut = true;
    }

    @Override
    public void applyPropertyChange(PropertyChangeEvent pce, JLayer l) {
        if ("tick".equals(pce.getPropertyName())) {
            l.repaint();
        }
    }
}
