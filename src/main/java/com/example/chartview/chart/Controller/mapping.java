package com.example.chartview.chart.Controller;


import com.example.chartview.chart.Bean.IPConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi4j.io.serial.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.pi4j.io.serial.*;

import javax.annotation.Resource;

@EnableConfigurationProperties
@Controller
public class mapping {

    static ArrayList<res> out;
    static ArrayList<Double>infom;
    static Lock lock;
    static Serial serial;

    static {
        out=new ArrayList<>();
        infom=new ArrayList<>();
        infom.add(77.77);
        infom.add(88.88);
        lock=new ReentrantLock();
        serial= SerialFactory.createInstance();
        new Thread(new Runnable() {
            @Override
            public void run() {
                testData();
            }
        }).start();
    }
    ObjectMapper mapper = new ObjectMapper();

    @Resource(name = "IPConfig")
    private IPConfig IPConfig;

    @ResponseBody
    @GetMapping("/hello")
    public ResponseEntity<String> dataFormat() throws JsonProcessingException {
        String json="null";
        try{
            lock.lock();
            json = mapper.writeValueAsString(out);
        }catch (Exception e){
            System.out.println("error hello");
        }finally {
            lock.unlock();
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Access-Control-Allow-Origin", "*");

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(json);
    }

    @ResponseBody
    @GetMapping("/information")
    public ResponseEntity<String> infoFormat() throws JsonProcessingException {
        String time=new Date().toString();
        double THDx=66.6;
        double fre=77.77;
        try{
            lock.lock();
            THDx=infom.get(1);
            fre=infom.get(0);

        }catch (Exception e){
            System.out.println("error information");
        }finally {
            lock.unlock();
        }

        info in = new info(time,THDx,fre);
        String res = mapper.writeValueAsString(in);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        return ResponseEntity.ok()
                .headers(headers)
                .body(res);
    }

    @GetMapping("/chart")
    public String chart(Model model){
        model.addAttribute("ip1",IPConfig.getIp1());
        model.addAttribute("ip2",IPConfig.getIp2());

        return "helloer";
    }

    @ResponseBody
    @GetMapping("/out")
    public String out(){
        System.out.println("outer");
        return new Date().toString();
    }

    static class res{
        public double xx = 0.0;
        public double yy=0.0;
        public res(){}
        public res(double xx,double yy){
            this.xx=xx;
            this.yy=yy;
        }
    }

    static class info{
        public String time="test";
        public double THDx=66.0;
        public double fre=0.0;
        public info(){}
        public info (String time,double THDx,double fre){
            this.time=time;
            this.THDx=THDx;
            this.fre=fre;
        }
    }

    public static void testData(){
        try {
            System.out.println("start");

            SerialConfig config = new SerialConfig();
            config.device(RaspberryPiSerial.DEFAULT_COM_PORT)
                    .baud(Baud._115200)
                    .dataBits(DataBits._8)
                    .parity(Parity.NONE)
                    .stopBits(StopBits._1)
                    .flowControl(FlowControl.NONE);

            System.out.println("innit over");


            serial.open(config);
            System.out.println("open over");


            new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("listener....");
                    serial.addListener(new SerialDataEventListener() {
                        @Override
                        public void dataReceived(SerialDataEvent event) {
                            System.out.println("data-RECEIVED");


                            try {
                                System.out.println("[HEX DATA]   " + event.getHexByteString());
                                System.out.println("[ASCII DATA] " + event.getAsciiString());
//                                String accis= event.getAsciiString();
//                                StringBuilder sb = new StringBuilder();
//                                int i = 0;
//                                while (i < accis.length()){
//                                    int parseInt = Integer.parseInt(accis.substring(i, i + 2), 16);
//                                    i+=2;
//                                    sb.append((char) (parseInt));
//                                }
//                                String rev=sb.toString();
                                String rev= event.getAsciiString();
                                if(rev.length()==0){
                                    System.out.println("rev null");
                                    return ;
                                }
                                if(rev.charAt(0)!='*'||rev.charAt(rev.length()-1)!='@'){
                                    System.out.println("数据截止异常");
                                    return;
                                }
                                double fre=0.0;
                                double thd=0.0;
                                ArrayList<Double> u = new ArrayList<>();
                                int index=1;
                                fre=Double.parseDouble(rev.substring(1,8));
                                if(rev.charAt(8)!='#'){
                                    System.out.println("index 8 error");
                                }
                                thd=Double.parseDouble(rev.substring(9,15));
                                if(rev.charAt(15)!='#'){
                                    System.out.println("index 15 error");
                                }

                                index=16;
                                while(Math.abs(rev.length()-index)>=6){
                                    double ele=Double.parseDouble(rev.substring(index,index+7));
                                    u.add(ele);
                                    index+=8;
                                }
                                double y=0.0;
                                double time=1.0/(fre*1000.0)*(1.2);
                                double delta=0.0;
                                double dd=time/120.0;
                                double temp=fre*1000.0*Math.PI;

                                try {
                                    lock.lock();
                                    out.clear();
                                    while (Math.abs(time - delta) > 0.0000001) {
                                        y = u.get(0) * Math.cos(2 * temp * delta) +
                                                u.get(1) * Math.cos(4 * temp * delta) +
                                                u.get(2) * Math.cos(6 * temp * delta) +
                                                u.get(3) * Math.cos(8 * temp * delta) +
                                                u.get(4) * Math.cos(10 * temp * delta);
                                        res ele = new res(delta, y);
                                        out.add(ele);
                                        delta += dd;
                                    }
                                    infom.clear();
                                    infom.add(fre);
                                    infom.add(thd);
                                }catch (Exception e){
                                    System.out.println("lock test Data error");
                                    System.out.println(Arrays.toString(e.getStackTrace()));
                                }finally {
                                    lock.unlock();
                                }




                            } catch (IOException e) {
                                System.out.println("error"+e.getMessage().toString());
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }).start();

            // continuous loop to keep the program running until the user terminates the program
            while(true) {
                System.out.println("Proved it is running .....");
//                try {
//                    String order = "*123.456#06.333#023.456#013.456#180.456#323.456#103.446#001.456#@";
//                    char[] sbuf = order.toCharArray();
//                    StringBuilder stringBuilder = new StringBuilder();
//                    for (char c : sbuf) {
//                        String charToHex = Integer.toHexString(c);
//                        stringBuilder.append(charToHex);
//                    }
//                    serial.write(stringBuilder.toString().toCharArray());
//
//                    System.out.println("*123.456#06.333#023.456#013.456#180.456#323.456#103.446#001.456#@" );
//                }
//                catch(IllegalStateException ex){
//                    ex.printStackTrace();
//                }

                Thread.sleep(10000 );
            }

        }
        catch(IOException | InterruptedException ex) {
            System.out.println(" ==>> SERIAL SETUP FAILED : " + ex.getMessage());
        }

    }
}


