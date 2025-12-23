package com.example.smartbinapp.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class ImageCompressor {

    private static final String TAG = "ImageCompressor";

    /**
     * Nén ảnh xuống dưới kích thước tối đa với chất lượng tốt
     * @param imagePath đường dẫn ảnh gốc
     * @param maxSizeKB kích thước tối đa (KB), khuyến nghị 800-1000KB
     * @return File ảnh đã nén
     */
    public static File compressImage(String imagePath, int maxSizeKB) {
        try {
            File originalFile = new File(imagePath);

            if (!originalFile.exists()) {
                Log.e(TAG, "File không tồn tại: " + imagePath);
                return originalFile;
            }

            // Decode ảnh với kích thước giảm
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            // Tính toán inSampleSize để giảm kích thước
            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;
            int maxDimension = 1920; // Max width/height (Full HD)

            int inSampleSize = 1;
            if (originalWidth > maxDimension || originalHeight > maxDimension) {
                int halfWidth = originalWidth / 2;
                int halfHeight = originalHeight / 2;

                while ((halfWidth / inSampleSize) >= maxDimension
                        && (halfHeight / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2;
                }
            }

            // Decode ảnh với sample size
            options.inSampleSize = inSampleSize;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Giảm bộ nhớ

            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

            if (bitmap == null) {
                Log.e(TAG, "Không thể decode bitmap từ: " + imagePath);
                return originalFile;
            }

            // Nén ảnh với quality giảm dần cho đến khi đạt kích thước mong muốn
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int quality = 90;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);

            // Giảm quality cho đến khi đạt kích thước mong muốn
            while (baos.toByteArray().length / 1024 > maxSizeKB && quality > 10) {
                baos.reset();
                quality -= 10;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            }

            // Lưu file đã nén
            File compressedFile = new File(originalFile.getParent(),
                    "compressed_" + originalFile.getName());

            FileOutputStream fos = new FileOutputStream(compressedFile);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();

            // Log kết quả
            long originalSize = originalFile.length() / 1024;
            long compressedSize = compressedFile.length() / 1024;

            Log.d(TAG, "✅ Nén thành công:");
            Log.d(TAG, "   Original: " + originalSize + " KB");
            Log.d(TAG, "   Compressed: " + compressedSize + " KB");
            Log.d(TAG, "   Quality: " + quality + "%");
            Log.d(TAG, "   Saved: " + (originalSize - compressedSize) + " KB (" +
                    String.format("%.1f", (1 - (float)compressedSize/originalSize) * 100) + "%)");

            bitmap.recycle();

            return compressedFile;

        } catch (Exception e) {
            Log.e(TAG, "❌ Lỗi nén ảnh: " + e.getMessage(), e);
            return new File(imagePath);
        }
    }

    /**
     * Nén ảnh với quality cố định
     */
    public static File compressImageWithQuality(String imagePath, int quality) {
        try {
            File originalFile = new File(imagePath);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;

            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
            if (bitmap == null) return originalFile;

            File compressedFile = new File(originalFile.getParent(),
                    "compressed_" + originalFile.getName());

            FileOutputStream fos = new FileOutputStream(compressedFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            fos.flush();
            fos.close();

            bitmap.recycle();

            Log.d(TAG, "Nén với quality " + quality + "%: " +
                    (originalFile.length() / 1024) + " KB → " +
                    (compressedFile.length() / 1024) + " KB");

            return compressedFile;

        } catch (Exception e) {
            Log.e(TAG, "Lỗi nén với quality: " + e.getMessage());
            return new File(imagePath);
        }
    }
}