package com.hrz.libcimbar.demo;

import com.hrz.libcimbar.image.AverageHash;
import com.hrz.libcimbar.image.BitMatrix;
import com.hrz.libcimbar.image.BitMatrixFactory;
import com.hrz.libcimbar.image.SymbolGridPreprocessor;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public final class Main {
    private Main() { }
    public static void main(String[] args) throws Exception {
        BufferedImage image;
        if (args.length > 0) {
            image = ImageIO.read(new File(args[0]));
            if (image == null) { throw new IllegalArgumentException("Unsupported image: " + args[0]); }
            System.out.println("Input: " + args[0]);
        } else {
            image = createDemoImage();
            System.out.println("Input: generated demo image");
        }
        byte[] binary = SymbolGridPreprocessor.preprocess(image, false);
        BitMatrix matrix = BitMatrixFactory.fromBinaryImage(binary, image.getWidth(), image.getHeight());
        System.out.println("Image: " + image.getWidth() + "x" + image.getHeight());
        System.out.println("First row bits (up to 32): " + Integer.toBinaryString(matrix.get(0, 0, Math.min(32, image.getWidth()))));
        System.out.println("Binary hash: " + Long.toUnsignedString(AverageHash.calculate(image), 16));
        System.out.println("Preprocessing + packing completed.");
        System.out.println("Note: this demo is not a complete libcimbar frame decoder.");
    }

    private static BufferedImage createDemoImage() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                boolean white = ((x / 8) + (y / 8)) % 2 == 0;
                image.setRGB(x, y, white ? 0xffffff : 0x000000);
            }
        }
        return image;
    }
}
