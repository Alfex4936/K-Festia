package csw.korea.festival.main.common.service;

import com.groupdocs.metadata.core.*;
import com.groupdocs.metadata.Metadata;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.lang.Math;

@Service
public class BlurHashService {

    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#$%*+,-.:;=?@[]^_{|}~";
    private static final int[] DECODE_CHAR_MAP = new int[128];
    private static final float[] SRGB_TO_LINEAR = new float[256];
    private static final int[] LINEAR_TO_SRGB = new int[4096];

    // ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @PostConstruct
    void initTables() {
        // Initialize sRGB -> Linear
        for (int i = 0; i < 256; i++) {
            float v = i / 255f;
            if (v <= 0.04045) {
                SRGB_TO_LINEAR[i] = v / 12.92f;
            } else {
                SRGB_TO_LINEAR[i] = (float) Math.pow((v + 0.055) / 1.055, 2.4);
            }
        }

        // Initialize Linear -> sRGB
        for (int i = 0; i < 4096; i++) {
            double v = i / 4095.0;
            double val;
            if (v <= 0.0031308) {
                val = v * 12.92 * 255 + 0.5;
            } else {
                val = (1.055 * Math.pow(v, 1 / 2.4) - 0.055) * 255 + 0.5;
            }
            LINEAR_TO_SRGB[i] = (int) val;
        }

        Arrays.fill(DECODE_CHAR_MAP, -1);
        for (int i = 0; i < CHARACTERS.length(); i++) {
            DECODE_CHAR_MAP[CHARACTERS.charAt(i)] = i;
        }
    }

    /**
     * Example method that encodes a BlurHash from a BufferedImage.
     */
    public String encodeBlurHash(BufferedImage img, int xComponents, int yComponents) {
        // Convert to RGB array
        int width = img.getWidth();
        int height = img.getHeight();
        byte[] pixels = new byte[width * height * 3];
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                // Extract RGB
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                pixels[idx] = (byte) r;
                pixels[idx+1] = (byte) g;
                pixels[idx+2] = (byte) b;
                idx += 3;
            }
        }

        return encodeBlurHash(xComponents, yComponents, width, height, pixels, width * 3);
    }

    /**
     * Encode BlurHash logic, similar to Go code.
     */
    public String encodeBlurHash(int xComp, int yComp, int width, int height, byte[] pixels, int bytesPerRow) {
        if (xComp < 1 || xComp > 9 || yComp < 1 || yComp > 9) {
            return "";
        }

        float[][] cosX = precomputeCosinesFloat(width, xComp);
        float[][] cosY = precomputeCosinesFloat(height, yComp);

        int factorsCount = xComp * yComp;
        float[] factorsR = new float[factorsCount];
        float[] factorsG = new float[factorsCount];
        float[] factorsB = new float[factorsCount];

        // Convert sRGB -> Linear
        float[] linearPixels = new float[width * height * 3];
        for (int i = 0; i < linearPixels.length; i++) {
            // & 0xFF to ensure signed byte conversion
            linearPixels[i] = SRGB_TO_LINEAR[pixels[i] & 0xFF];
        }

        // Compute factors
        for (int y = 0; y < height; y++) {
            float[] cyArr = cosY[y];
            int rowStart = y * bytesPerRow;
            for (int x = 0; x < width; x++) {
                int pIndex = rowStart + x * 3;
                float r = linearPixels[pIndex];
                float g = linearPixels[pIndex + 1];
                float b = linearPixels[pIndex + 2];
                float[] cxArr = cosX[x];

                for (int j = 0; j < yComp; j++) {
                    float cy = cyArr[j];
                    int rowOff = j * xComp;
                    for (int i = 0; i < xComp; i++) {
                        float c = cxArr[i] * cy;
                        int off = rowOff + i;
                        factorsR[off] += c * r;
                        factorsG[off] += c * g;
                        factorsB[off] += c * b;
                    }
                }
            }
        }

        float invCount = 1f / (width * height);
        for (int i = 0; i < factorsCount; i++) {
            factorsR[i] *= invCount;
            factorsG[i] *= invCount;
            factorsB[i] *= invCount;
        }

        // Find max AC component
        float maxVal = 0f;
        for (int i = 1; i < factorsCount; i++) {
            maxVal = Math.max(maxVal, Math.abs(factorsR[i]));
            maxVal = Math.max(maxVal, Math.abs(factorsG[i]));
            maxVal = Math.max(maxVal, Math.abs(factorsB[i]));
        }

        int quantMax = 0;
        if (maxVal > 0f) {
            double quant = (maxVal * 166.0) - 1.0;
            quant = Math.max(0.0, Math.min(82.0, quant));
            quantMax = (int) quant;
        }
        double maxAc = (quantMax + 1) / 166.0;

        int dcValue = encodeDC(factorsR[0], factorsG[0], factorsB[0]);
        int[] acValues = new int[factorsCount - 1];
        for (int i = 1; i < factorsCount; i++) {
            acValues[i - 1] = encodeAC(factorsR[i] / (float) maxAc, factorsG[i] / (float) maxAc, factorsB[i] / (float) maxAc);
        }

        int sizeFlag = (yComp - 1) * 9 + (xComp - 1);
        StringBuilder sb = new StringBuilder();
        sb.append(encode83(sizeFlag, 1));
        sb.append(encode83(quantMax, 1));
        sb.append(encode83(dcValue, 4));
        for (int val : acValues) {
            sb.append(encode83(val, 2));
        }

        return sb.toString();
    }

    /**
     * Decoding logic would be similar: parse the hash, extract components, reconstruct pixels.
     * For brevity, only the encoding logic is shown here. Decoding is similar to the Go code.
     */
    public byte[] decodeBlurHash(String hash, int width, int height, int punch) throws IOException {
        // Similar to the Go code, validate hash, decode components, compute pixels.
        // Due to length, not fully implemented here.
        throw new UnsupportedOperationException("Decode logic similar to Go code not shown for brevity.");
    }

    /**
     * Orientation and rotation methods
     */
    public BufferedImage fixOrientation(BufferedImage img, int orientation) {
        // pattern matching switch from newer Java versions:
        return switch (orientation) {
            case 3 -> rotate180(img);
            case 6 -> rotate90(img);
            case 8 -> rotate270(img);
            default -> img;
        };
    }

    public BufferedImage rotate90(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage rotated = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                rotated.setRGB(h - y - 1, x, img.getRGB(x, y));
            }
        }
        return rotated;
    }

    public BufferedImage rotate180(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage rotated = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                rotated.setRGB(w - x - 1, h - y - 1, img.getRGB(x, y));
            }
        }
        return rotated;
    }

    public BufferedImage rotate270(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage rotated = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                rotated.setRGB(y, w - x - 1, img.getRGB(x, y));
            }
        }
        return rotated;
    }

    /**
     * Reads orientation from EXIF using GroupDocs.Metadata.
     *
     * @param inputStream the image input stream
     * @return orientation value (1 is default if not found)
     * @throws IOException if reading the stream fails
     */
    public int getOrientation(InputStream inputStream) throws IOException {
        // Create Metadata instance from InputStream
        // Note: GroupDocs.Metadata typically loads from a file.
        // Check their docs to see if InputStream constructor is supported or
        // write the InputStream to a temp file and read from it.

        // If InputStream cannot be directly used, you might need to:
        // Path tempFile = Files.createTempFile("image", ".tmp");
        // Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        // try (Metadata metadata = new Metadata(tempFile.toString())) { ... }

        // If GroupDocs.Metadata supports InputStream directly (check docs):
        try (Metadata metadata = new Metadata(inputStream)) {
            // Get the EXIF root package
            IExif root = (IExif) metadata.getRootPackage();
            if (root == null || root.getExifPackage() == null) {
                return 1; // Default orientation
            }

            ExifPackage exifPackage = root.getExifPackage();
            ExifIfdPackage exifIfd = exifPackage.getExifIfdPackage();
            if (exifIfd == null) {
                return 1; // Default if no IFD
            }

            // Orientation often stored as a TiffTag named "Orientation"
            IReadOnlyList<TiffTag> tags = exifIfd.toList();
            for (TiffTag tag : tags) {
                if ("Orientation".equalsIgnoreCase(tag.getName())) {
                    PropertyValue val = tag.getValue();

                    if (val == null || val.getRawValue() == null) {
                        continue; // No value present
                    }

                    // Check the type and handle accordingly
                    switch (val.getType()) {
                        case Integer:
                            // Orientation is stored as an Integer
                            return (Integer) val.getRawValue(); // directly return as int

                        case Long:
                            // If it's a Long, convert to int
                            Long longVal = (Long) val.getRawValue();
                            return longVal.intValue();

                        case Double:
                            // If it's a Double for some reason, cast down
                            Double doubleVal = (Double) val.getRawValue();
                            return doubleVal.intValue();

                        // e.g., Short,,,
                        default:
                            // Not a numeric type we recognize for orientation
                            break;
                    }
                }
            }

// If not found or not a suitable numeric type, return default
            return 1;
        }
    }

    /**
     * Precompute cosine (float) similar to Go code
     */
    private float[][] precomputeCosinesFloat(int size, int components) {
        float[][] cosines = new float[size][components];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < components; j++) {
                double angle = (Math.PI * i * j) / size;
                cosines[i][j] = (float) Math.cos(angle);
            }
        }
        return cosines;
    }

    private String encode83(int value, int length) {
        char[] result = new char[length];
        for (int i = length - 1; i >= 0; i--) {
            int digit = value % 83;
            result[i] = CHARACTERS.charAt(digit);
            value /= 83;
        }
        return new String(result);
    }

    private int encodeDC(float r, float g, float b) {
        int ri = linearToSRGB(r);
        int gi = linearToSRGB(g);
        int bi = linearToSRGB(b);
        return (ri << 16) + (gi << 8) + bi;
    }

    private int encodeAC(float r, float g, float b) {
        int quantR = (int) Math.max(0, Math.min(18, Math.floor(signPow(r, 0.5) * 9 + 9.5)));
        int quantG = (int) Math.max(0, Math.min(18, Math.floor(signPow(g, 0.5) * 9 + 9.5)));
        int quantB = (int) Math.max(0, Math.min(18, Math.floor(signPow(b, 0.5) * 9 + 9.5)));
        return quantR * 19 * 19 + quantG * 19 + quantB;
    }

    private int linearToSRGB(float value) {
        if (value <= 0) return LINEAR_TO_SRGB[0];
        if (value >= 1) return LINEAR_TO_SRGB[4095];
        int idx = (int) (value * 4095);
        return LINEAR_TO_SRGB[idx];
    }

    private double signPow(double val, double exp) {
        return val < 0 ? -Math.pow(-val, exp) : Math.pow(val, exp);
    }

    /**
     * Example of using Virtual Threads for concurrency:
     * You could submit tasks like encoding/decoding multiple images simultaneously.
     */
    public Future<String> encodeBlurHashAsync(BufferedImage img, int xComp, int yComp) {
        return executor.submit(() -> encodeBlurHash(img, xComp, yComp));
    }
}
