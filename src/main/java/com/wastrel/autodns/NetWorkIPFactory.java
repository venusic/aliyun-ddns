package com.wastrel.autodns;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.List;

public class NetWorkIPFactory {

    private static OkHttpClient client;

    static {
        client = new OkHttpClient();
    }


    static String getCanUseIp(List<String> ipServers) {
        for (String url : ipServers) {
            System.out.println("正在从" + url + "获取外网IP！");
            String ip = doGet(url).trim();
            if (ipCheck(ip)) {
                System.out.println("获取到外网IP：" + ip);
                return ip;
            }
        }
        throw new RuntimeException("没有找到合适的外网IP获取服务器！");
    }

    private static boolean ipCheck(String text) {
        if (text != null && !text.isEmpty()) {
            String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                    + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                    + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                    + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
            return text.matches(regex);
        }
        return false;
    }

    private static String doGet(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body != null) {
                return body.string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
