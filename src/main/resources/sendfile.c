#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <jni.h>

// JNI wrapper for the sendfile system call
JNIEXPORT jlong JNICALL Java_csw_korea_festival_main_common_util_SendfileUtil_nativeSendfile(
    JNIEnv *env, jobject obj, jint out_fd, jstring file_path) {
    const char *file_path_str = (*env)->GetStringUTFChars(env, file_path, 0);

    // Open the file
    int in_fd = open(file_path_str, O_RDONLY);
    if (in_fd < 0) {
        (*env)->ReleaseStringUTFChars(env, file_path, file_path_str);
        return -1;
    }

    // Get file size
    struct stat file_stat;
    fstat(in_fd, &file_stat);

    // Use sendfile to transfer the file
    off_t offset = 0;
    ssize_t sent_bytes = sendfile(out_fd, in_fd, &offset, file_stat.st_size);

    close(in_fd);
    (*env)->ReleaseStringUTFChars(env, file_path, file_path_str);
    return sent_bytes; // Return the number of bytes sent
}

// gcc -shared -fPIC -o libsendfile.so -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux sendfile.c

