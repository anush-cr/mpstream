package com.mpstream.datastructures;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 *
 * @author anush_cr
 */
public class ImageComponent extends JComponent {
    private static final long serialVersionUID = 5584422798735147930L;
    private Image mImage;
    private Dimension mSize;
    public void setImage(Image image) {
      SwingUtilities.invokeLater(new ImageRunnable(image));
    }
    private class ImageRunnable implements Runnable {
        private final Image newImage;
        public ImageRunnable(Image newImage) {
            super();
            this.newImage = newImage;
        }
        @Override
        public void run() {
            ImageComponent.this.mImage = newImage;
            repaint();
        }
    }
    public ImageComponent(int w, int h) {
      mSize = new Dimension(w, h);
      this.setSize(mSize);
    }
    public ImageComponent() {
      mSize = new Dimension(0, 0);
      this.setSize(mSize);
    }
    @Override
    public synchronized void paint(Graphics g) {
        if (mImage != null)
            g.drawImage(mImage, 0, 0, this);
    }
}
