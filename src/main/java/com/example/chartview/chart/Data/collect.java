package com.example.chartview.chart.Data;
import com.pi4j.io.serial.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class collect {


    //首先 实例化serial对象，用于初始化串口
    public static Serial serial=SerialFactory.createInstance();
    public static String serial_lastStr = "";//用于存放从串口获取的字符串信息
    public static void read()  {
        System.out.println("===================start=================");
        SerialConfig config = new SerialConfig();//初始化config配置类
        //默认获取第一个串口
        String rs232port = RaspberryPiSerial.DEFAULT_COM_PORT;
        //config设定
        config.device(rs232port)
                .baud(Baud._9600)//波特率
                .dataBits(DataBits._8)//数据位
                .parity(Parity.EVEN)//偶校验
                .stopBits(StopBits._1);//停止位
        try {
            serial.open(config);
        } catch (IOException e) {
            System.out.println("=================open error======================");
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
        System.out.println("============================================");
        System.out.println("============================================");
        System.out.println("== == == == == == ==是否打开  "+serial.isOpen());
        System.out.println("============================================");
        System.out.println("============================================");

        serial.addListener(new SerialDataEventListener() {
            @Override
            public void dataReceived(SerialDataEvent event) {
                //从单片机读取
                try {
                    serial_lastStr = event.getAsciiString();
                    System.out.println("received: "+serial_lastStr);

                } catch (IOException e) {
                    System.out.println("Get RS232 Error:"+e.getMessage());
                }
            }
        });
        System.out.println("===================end=================");


    }

}
