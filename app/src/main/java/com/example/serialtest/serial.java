package com.example.serialtest;

public class serial {

	public native int open(int Port, int Rate);

	public native int close();

	public native byte[] read();

	public native int write(byte[] buffer);

}
