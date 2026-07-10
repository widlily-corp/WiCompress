#include <jni.h>
#include <vector>
#include <numeric>
#include <android/log.h>

#define LOG_TAG "WiCompressNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jint JNICALL
Java_com_widlily_wicompress_util_ImageHashUtil_computeHammingDistance(
        JNIEnv *env,
        jobject thiz,
        jlong hash1,
        jlong hash2) {
    // Exclusive OR gives 1s where bits differ
    uint64_t diff = static_cast<uint64_t>(hash1 ^ hash2);
    
    // GCC/Clang built-in to count set bits (popcount)
    #if defined(__clang__) || defined(__GNUC__)
        return static_cast<jint>(__builtin_popcountll(diff));
    #else
        // Fallback popcount if not compiled on gcc/clang
        jint count = 0;
        while (diff) {
            count += (diff & 1);
            diff >>= 1;
        }
        return count;
    #endif
}

JNIEXPORT jlong JNICALL
Java_com_widlily_wicompress_util_ImageHashUtil_computeAverageHash(
        JNIEnv *env,
        jobject thiz,
        jintArray pixels,
        jint width,
        jint height) {
    if (width <= 0 || height <= 0) {
        return 0;
    }

    jsize len = env->GetArrayLength(pixels);
    if (len < width * height) {
        return 0;
    }

    jint *pixel_data = env->GetIntArrayElements(pixels, nullptr);
    if (!pixel_data) {
        return 0;
    }

    // 8x8 average values
    double grid[8][8] = {0.0};
    int counts[8][8] = {0};

    // Rescale input image pixels into 8x8 grid cells using box filter
    for (int y = 0; y < height; ++y) {
        int grid_y = (y * 8) / height;
        if (grid_y > 7) grid_y = 7;

        for (int x = 0; x < width; ++x) {
            int grid_x = (x * 8) / width;
            if (grid_x > 7) grid_x = 7;

            // Extract ARGB colors
            jint color = pixel_data[y * width + x];
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            // Convert to grayscale luminance (rec601 standard)
            double gray = 0.299 * r + 0.587 * g + 0.114 * b;
            grid[grid_y][grid_x] += gray;
            counts[grid_y][grid_x]++;
        }
    }

    // Release array resource
    env->ReleaseIntArrayElements(pixels, pixel_data, JNI_ABORT);

    // Compute average value per grid cell and sum up overall total
    double total_sum = 0.0;
    double cell_values[64];
    int idx = 0;

    for (int gy = 0; gy < 8; ++gy) {
        for (int gx = 0; gx < 8; ++gx) {
            double avg_cell = 0.0;
            if (counts[gy][gx] > 0) {
                avg_cell = grid[gy][gx] / counts[gy][gx];
            }
            cell_values[idx++] = avg_cell;
            total_sum += avg_cell;
        }
    }

    double average_luminance = total_sum / 64.0;

    // Generate 64-bit hash: 1 if cell luminance is >= average luminance, 0 otherwise
    uint64_t hash = 0;
    for (int i = 0; i < 64; ++i) {
        if (cell_values[i] >= average_luminance) {
            hash |= (static_cast<uint64_t>(1) << i);
        }
    }

    return static_cast<jlong>(hash);
}

}
