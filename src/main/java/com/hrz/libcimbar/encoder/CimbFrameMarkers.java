package com.hrz.libcimbar.encoder;

import java.awt.image.BufferedImage;

/** Marker artwork used by the original CimbWriter layout. Images are copied on input and output. */
public final class CimbFrameMarkers {
    private final BufferedImage anchor;
    private final BufferedImage secondaryAnchor;
    private final BufferedImage horizontalGuide;
    private final BufferedImage verticalGuide;

    public CimbFrameMarkers(BufferedImage anchor, BufferedImage secondaryAnchor, BufferedImage horizontalGuide, BufferedImage verticalGuide) {
        this.anchor = copy(require(anchor, "anchor"));
        this.secondaryAnchor = copy(require(secondaryAnchor, "secondaryAnchor"));
        this.horizontalGuide = copy(require(horizontalGuide, "horizontalGuide"));
        this.verticalGuide = copy(require(verticalGuide, "verticalGuide"));
    }

    private static BufferedImage require(BufferedImage image, String name) {
        if (image == null) { throw new IllegalArgumentException(name + " cannot be null"); }
        return image;
    }

    static BufferedImage copy(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        result.setRGB(0, 0, source.getWidth(), source.getHeight(), source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth()), 0, source.getWidth());
        return result;
    }

    public BufferedImage anchor() { return copy(anchor); }
    public BufferedImage secondaryAnchor() { return copy(secondaryAnchor); }
    public BufferedImage horizontalGuide() { return copy(horizontalGuide); }
    public BufferedImage verticalGuide() { return copy(verticalGuide); }
}
