package com.example.serialtest;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    String rxIdCode = "";
    String tag = "serial test";
    private EditText ET1;
    private Button RECV;
    private Button SEND;
    private Button OPENSERIAL;
    private Button CLOSESERIAL;
    private Button CLEAR;
    private TextView msglist;
    private ScrollView sv;
    private serial mSerial = new serial();
    private rs485ctl mRs485ctl = new rs485ctl();

    private Spinner spinner;
    private List<String> data_list;
    private ArrayAdapter<String> arr_adapter;
    private int serial;
    private int Baudrate;

    private Spinner spinner2;
    private List<String> data_list2;
    private ArrayAdapter<String> arr_adapter2;

    static {
        System.loadLibrary("serialtest");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Log.e("T", "日志正常");
        ET1 = findViewById(R.id.edit1);
        SEND = findViewById(R.id.send1);
        OPENSERIAL = findViewById(R.id.open_serial);
        CLOSESERIAL = findViewById(R.id.close_serial);
        msglist = findViewById(R.id.msglist);
        sv = findViewById(R.id.list);
        CLEAR = findViewById(R.id.clear);

        SEND.setOnClickListener(this);
        OPENSERIAL.setOnClickListener(this);
        CLOSESERIAL.setOnClickListener(this);
        CLEAR.setOnClickListener(this);


        spinner = (Spinner) findViewById(R.id.spinner);
        spinner2 = (Spinner) findViewById(R.id.spinner2);

        data_list = new ArrayList<String>();
        data_list.add("/dev/ttyS4");
        data_list.add("/dev/ttyS7");
        data_list.add("/dev/ttyS9");

        data_list2 = new ArrayList<String>();
        data_list2.add("2400");
        data_list2.add("4800");
        data_list2.add("9600");
        data_list2.add("19200");
        data_list2.add("38400");
        data_list2.add("57600");
        data_list2.add("115200");

        arr_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, data_list);
        arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arr_adapter);

        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                serial = arg2;

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        arr_adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, data_list2);
        arr_adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(arr_adapter2);

        spinner2.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Baudrate = arg2;

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner.setSelection(1, true);
        spinner2.setSelection(6, true);

    }

    MyThread myThread = null;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send1:
                mRs485ctl.ioctl(0, 1);
                //Log.d(tag, "send start ...");
                String m = ET1.getText().toString() + "\n";

                byte[] test = {0x02, 0x30, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
                mSerial.write(test);
//                mSerial.write(m.getBytes());

                msglist.append("[发送]" + m);
                ET1.setText("");

                sv.post(() -> {
                    sv.fullScroll(ScrollView.FOCUS_DOWN);
                });

                mRs485ctl.ioctl(0, 0);
                break;
            case R.id.open_serial:
                int ret1 = mSerial.open(serial, Baudrate);
                int ret2 = mRs485ctl.open();
                if ((ret1 > 0) && (ret2 > 0)) {
                    Toast.makeText(MainActivity.this, "串口打开成功", Toast.LENGTH_LONG).show();
                    OPENSERIAL.setEnabled(false);
                    CLOSESERIAL.setEnabled(true);
                    SEND.setEnabled(true);
                   // mRs485ctl.ioctl(0, 0);
                    /*
                     * 因为费时的操作不能在主线程中进行
                     * 因此我们需要开启一个子线程
                     * 实例化一个myThread线程类，
                     * 并调用start方法开始运行这个子线程
                     * */
                    myThread = new MyThread();
                    myThread.start();
                }

                if (ret1 <= 0) {
                    mRs485ctl.close();
                    Toast.makeText(MainActivity.this, "串口打开失败，错误码：" + ret1, Toast.LENGTH_LONG).show();
                }
                if (ret2 <= 0) {
                    mSerial.close();
                    Toast.makeText(MainActivity.this, "串口控制引脚打开失败，错误码：" + ret2, Toast.LENGTH_LONG).show();
                }

               // mRs485ctl.ioctl(0, 0);
                break;

            case R.id.close_serial:
                /*
                 * 务必关掉子线程
                 * */
                if (myThread != null) {
                    mSerial.close();
                    mRs485ctl.close();
                    myThread = null;
                }
                SEND.setEnabled(false);
                break;
            case R.id.clear:

                msglist.setText("");
                break;

        }
    }

    /**
     * 定义一个MyThread类并继承Thread线程类
     * 复写run方法，run中的代码就是在子线程中执行的代码
     * 需要强调的是，必须调用start方法来执行子线程
     * 而不是直接运行run
     */
    class MyThread extends Thread {

        // 步骤2：复写run（），内容 = 定义线程行为
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {

            while (true) {

                byte[] bytes = mSerial.read();
                Log.d("xxxx", "收到数据");
                if (bytes == null) {
                    break;
                }
                String string = null;
                string = new String(bytes);
                String finalString = print(bytes) + "\n";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        msglist.append("[接收]" + finalString);
                        sv.post(() -> {
                            // TODO Auto-generated method stub
                            sv.fullScroll(ScrollView.FOCUS_DOWN);
                        });
                    }

                });

            }
            /*
             * runOnUiThread 意为将任务发送到主线程执行
             * 因为不能在子线程中修改UI
             * 因此这个函数是异步的
             */
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    OPENSERIAL.setEnabled(true);
                    CLOSESERIAL.setEnabled(false);
                }
            });
        }
    }

    public String print(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.main, menu);
        return false;
    }



}
