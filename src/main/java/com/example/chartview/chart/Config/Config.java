package com.example.chartview.chart.Config;

import com.example.chartview.chart.Bean.IPConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

@Configuration
public class Config {

    @Value("${ip1}")
    private String ip1;
    @Value("${ip2}")
    private String ip2;
    //http://192.168.10.124:9999/hello

    //将方法的返回值添加到容器中，容器中这个组件默认的ID是方法名
    @Bean("IPConfig")
    public IPConfig IPConfig() {
        IPConfig config = new IPConfig();
        String IPs=getIpAddress();
        if(!"".equals(IPs)){
            ip1=ip1.replaceAll("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+",IPs);
            ip2=ip2.replaceAll("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+",IPs);
        }else{
            System.out.println("it is not online. use the default");
        }
        config.setIp1(ip1);
        config.setIp2(ip2);
        System.out.println("IP Config sucess: "+IPs);
        return config;
    }

    public static String getIpAddress() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
                    continue;
                } else {
                    Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        ip = addresses.nextElement();
                        if (ip != null && ip instanceof Inet4Address) {
                            return ip.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("IP地址获取失败" + e.toString());
        }
        return "";
    }
}
