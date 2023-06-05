package com.example.serialtest;

public class rs485ctl {
    public native int open();
    public native int close();
    public native int ioctl(int cmd, int arg);
}
