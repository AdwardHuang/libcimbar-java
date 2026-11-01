package com.hrz.libcimbar.encoder;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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


    /**
     * Loads the four unmodified upstream marker PNGs from libcimbar's bitmap directory.
     * Expected names are anchor-{dark|light}.png, anchor-secondary-{dark|light}.png,
     * guide-horizontal-{dark|light}.png and guide-vertical-{dark|light}.png.
     */
    public static CimbFrameMarkers loadOriginal(Path bitmapDirectory, boolean dark) throws IOException {
        if (bitmapDirectory == null) { throw new IllegalArgumentException("bitmapDirectory cannot be null"); }
        String suffix = dark ? "dark" : "light";
        return new CimbFrameMarkers(
            read(Files.newInputStream(bitmapDirectory.resolve("anchor-" + suffix + ".png")), "anchor-" + suffix + ".png"),
            read(Files.newInputStream(bitmapDirectory.resolve("anchor-secondary-" + suffix + ".png")), "anchor-secondary-" + suffix + ".png"),
            read(Files.newInputStream(bitmapDirectory.resolve("guide-horizontal-" + suffix + ".png")), "guide-horizontal-" + suffix + ".png"),
            read(Files.newInputStream(bitmapDirectory.resolve("guide-vertical-" + suffix + ".png")), "guide-vertical-" + suffix + ".png"));
    }

    /** Loads upstream marker PNGs from a classpath resource directory, e.g. "cimbar/bitmap". */
    public static CimbFrameMarkers loadOriginal(ClassLoader loader, String resourceDirectory, boolean dark) throws IOException {
        if (loader == null || resourceDirectory == null || resourceDirectory.isBlank()) {
            throw new IllegalArgumentException("loader and resourceDirectory are required");
        }
        String suffix = dark ? "dark" : "light";
        String root = resourceDirectory.replace('\\', '/').replaceAll("^/+|/+$", "");
        return new CimbFrameMarkers(
            readResource(loader, root + "/anchor-" + suffix + ".png"),
            readResource(loader, root + "/anchor-secondary-" + suffix + ".png"),
            readResource(loader, root + "/guide-horizontal-" + suffix + ".png"),
            readResource(loader, root + "/guide-vertical-" + suffix + ".png"));
    }

    /** Convenience classpath lookup at /cimbar/bitmap; the four PNGs must be packaged by the application. */
    public static CimbFrameMarkers loadOriginal(boolean dark) throws IOException {
        return loadOriginal(CimbFrameMarkers.class.getClassLoader(), "cimbar/bitmap", dark);
    }

    private static BufferedImage readResource(ClassLoader loader, String name) throws IOException {
        InputStream input = loader.getResourceAsStream(name);
        if (input == null) { throw new IOException("Missing original libcimbar marker resource: " + name); }
        return read(input, name);
    }

    private static BufferedImage read(InputStream input, String name) throws IOException {
        try (InputStream in = input) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) { throw new IOException("Unsupported or invalid marker image: " + name); }
            return image;
        }
    }

    public BufferedImage anchor() { return copy(anchor); }
    public BufferedImage secondaryAnchor() { return copy(secondaryAnchor); }
    public BufferedImage horizontalGuide() { return copy(horizontalGuide); }
    public BufferedImage verticalGuide() { return copy(verticalGuide); }
}
