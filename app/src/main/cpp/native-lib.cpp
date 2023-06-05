#include <jni.h>
#include <string>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <errno.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <string.h>
#include <stdint.h>
#include <termios.h>
#include <android/log.h>
#include <sys/ioctl.h>

#undef  TCSAFLUSH
#define TCSAFLUSH  TCSETSF
#ifndef _TERMIOS_H_
#define _TERMIOS_H_
#endif

int fd1 = 0;
int fd2 = 0;

#define LOG_TAG "[native]"
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG,__VA_ARGS__ ))


extern "C"
JNIEXPORT jint JNICALL
Java_com_example_serialtest_serial_open(JNIEnv *env, jobject thiz, jint port, jint rate) {

    struct termios options;
    fd1 = open("/dev/ttyS7", O_RDWR | O_NOCTTY | O_NONBLOCK);
    if (fd1 == -1) {
        return -1;
    }
    tcgetattr(fd1, &options);
    cfsetispeed(&options, B115200);
    cfsetospeed(&options, B115200);
    options.c_cflag |= (CLOCAL | CREAD);
    options.c_cflag &= ~PARENB;
    options.c_cflag &= ~CSTOPB;
    options.c_cflag &= ~CSIZE;
    options.c_cflag |= CS8;
    options.c_cflag &= ~CRTSCTS;
    options.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);
    options.c_iflag &= ~(IXON | IXOFF | IXANY);
    options.c_oflag &= ~OPOST;
    tcsetattr(fd1, TCSANOW, &options);

    LOGE("open fd: %d\n", fd1);
    return fd1;
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_example_serialtest_serial_open2(JNIEnv *env, jobject thiz, jint port, jint rate) {

    if (fd1 <= 0) {

        if (0 == port) {
            __android_log_print(ANDROID_LOG_INFO, "serial", "open fd1 /dev/ttyS4");
            fd1 = open("/dev/ttyS4", O_RDWR | O_NOCTTY | O_NONBLOCK);
        } else if (1 == port) {
            __android_log_print(ANDROID_LOG_INFO, "serial", "open fd1 /dev/ttyS9");
            fd1 = open("/dev/ttyS9", O_RDWR | O_NOCTTY | O_NONBLOCK);
        }
       else if (2 == port) {
            __android_log_print(ANDROID_LOG_INFO, "serial", "open fd1 /dev/ttyS7");
            fd1 = open("/dev/ttyS7", O_RDWR | O_NOCTTY | O_NONBLOCK);
        }
        else {
            __android_log_print(ANDROID_LOG_INFO, "serial", "Parameter Error serial not found");
            fd1 = 0;
            return -1;
        }
#if 1
        if (fd1 > 0) {
            __android_log_print(ANDROID_LOG_INFO, "serial", "serial open ok fd1=%d", fd1);
            // disable echo on serial ports
            struct termios ios;
            tcgetattr(fd1, &ios);
            ios.c_oflag &= ~(INLCR | IGNCR | ICRNL);
            ios.c_oflag &= ~(ONLCR | OCRNL);
            ios.c_iflag &= ~(ICRNL | IXON);
            ios.c_iflag &= ~(INLCR | IGNCR | ICRNL);
            ios.c_iflag &= ~(ONLCR | OCRNL);
            tcflush(fd1, TCIFLUSH);

            if (rate == 0) {
                cfsetospeed(&ios, B2400);
                cfsetispeed(&ios, B2400);
            }
            if (rate == 1) {
                cfsetospeed(&ios, B4800);
                cfsetispeed(&ios, B4800);
            }
            if (rate == 2) {
                cfsetospeed(&ios, B9600);
                cfsetispeed(&ios, B9600);
            }
            if (rate == 3) {
                cfsetospeed(&ios, B19200);
                cfsetispeed(&ios, B19200);
            }
            if (rate == 4) {
                cfsetospeed(&ios, B38400);
                cfsetispeed(&ios, B38400);
            }
            if (rate == 5) {
                cfsetospeed(&ios, B57600);
                cfsetispeed(&ios, B57600);
            }
            if (rate == 6) {
                cfsetospeed(&ios, B115200);
                cfsetispeed(&ios, B115200);
            }

            ios.c_cflag |= (CLOCAL | CREAD);
            ios.c_cflag &= ~PARENB;
            ios.c_cflag &= ~CSTOPB;
            ios.c_cflag &= ~CSIZE;
            ios.c_cflag |= CS8;
            ios.c_lflag = 0;
            tcsetattr(fd1, TCSANOW, &ios);
        } else {
            __android_log_print(ANDROID_LOG_INFO, "serial", "serial open failed fd1=%d", fd1);
        }
#endif
    }

    return fd1;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_serialtest_serial_close(JNIEnv *env, jobject thiz) {
    // TODO: implement close()
    if (fd1 > 0)close(fd1);
    fd1 = -1;
    return 1;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_serialtest_serial_read(JNIEnv *env, jobject thiz) {
    // TODO: implement read()
    unsigned char buffer[512];
    // int BufToJava[512];
    int len = 0, i = 0;
    memset(buffer, 0, sizeof(buffer));

    while (fd1 > 0) {
        len = read(fd1, buffer, 512);

        //if (len < 0)return nullptr;
        if (len <= 0) {
            usleep(500);
            continue;
        }
        //  __android_log_print(ANDROID_LOG_INFO, "serial", "fd1=%d", fd1);

        jbyteArray array = (*env).NewByteArray(len);
        (*env).SetByteArrayRegion(array, 0, len, reinterpret_cast<const jbyte *>(buffer));
        env->ReleaseByteArrayElements(array, env->GetByteArrayElements(array, JNI_FALSE), 0);
        return array;
    }
    return nullptr;


}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_serialtest_serial_write(JNIEnv *env, jobject thiz, jbyteArray buf) {
    jsize len = env->GetArrayLength(buf);
    jbyte *buffer = new jbyte[len];
    env->GetByteArrayRegion(buf, 0, len, buffer);

    char data_buf[len * 3 + 1];
    for (int i = 0; i < len; i++) {
        sprintf(data_buf + i * 3, "%02x ", buffer[i]);
    }
    LOGE("buffer: %s", data_buf);

    int ret = write(fd1, buffer, len);
    //delete[] buffer;

    return ret;
}

////////////////////////////////////////////////////////////////////////////////////////////////////
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_serialtest_rs485ctl_open(JNIEnv *env, jobject thiz, jint port, jint rate) {
    if ((fd2 = open("/dev/rs485_ctl", O_RDWR | O_NDELAY | O_NONBLOCK)) == -1) {
        __android_log_print(ANDROID_LOG_INFO, "serial", "open /dev/rs485_ctl Error");
    } else {
        __android_log_print(ANDROID_LOG_INFO, "serial", "open /dev/rs485_ctl Sucess fd2=%d", fd2);
    }
    return fd2;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_serialtest_rs485ctl_close(JNIEnv *env, jobject thiz) {
    if (fd2 > 0)close(fd2);
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_serialtest_rs485ctl_ioctl(JNIEnv *env, jobject thiz, jint cmd, jint arg) {
    ioctl(fd2, cmd, arg);
    return 0;
}

