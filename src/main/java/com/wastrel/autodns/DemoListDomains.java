package com.wastrel.autodns;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.*;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.sun.mail.util.MailSSLSocketFactory;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.util.List;
import java.util.Properties;

public class DemoListDomains {
    private static Config config;
    private static IAcsClient client = null;
    private static String domain = "wastrel.top";

    public static void initClient() {
        String regionId = "cn-hangzhou"; //必填固定值，必须为“cn-hanghou”
        IClientProfile profile = DefaultProfile.getProfile(regionId, config.accessKeyId, config.accessKeySec);
        // 若报Can not find endpoint to access异常，请添加以下此行代码
        // \DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", "Alidns", "alidns.aliyuncs.com");
        client = new DefaultAcsClient(profile);
    }


    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("请传入配置文件！");
            return;
        }
        config = parseConfig(args[0]);
        String useIp = null;
        try {
            initClient();
            useIp = NetWorkIPFactory.getCanUseIp(config.ipServers);
            DescribeDomainRecordsRequest request = new DescribeDomainRecordsRequest();
            DescribeDomainRecordsResponse response;
            request.setDomainName(config.domain);

            response = client.getAcsResponse(request);
            List<DescribeDomainRecordsResponse.Record> list = response.getDomainRecords();
            for (DescribeDomainRecordsResponse.Record domain : list) {
                System.out.println(domain.getRR());

                if (domain.getRR().equals(config.rr))
                    if (!useIp.equals(domain.getValue())) {
                        updateDNS(domain, useIp);
                    } else {
                        System.out.println("本次监听外网IP没有变更！");
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
            String msg = "获取到外网IP：" + useIp + "\n" + "更新过程中出现异常！\n";
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            msg += w.toString();
            msg = msg.replace("\n", "<br/>");
            try {
                sendErrorMail(msg);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    public static Config parseConfig(String configFile) throws IOException {
        File file = new File(configFile);
        if (file.isFile() && file.exists()) {
            try (InputStream in = new FileInputStream(file);) {
                return JSON.parseObject(in, Config.class);
            }
        } else throw new RuntimeException("解析配置文件出错！");
    }

    public static void updateDNS(DescribeDomainRecordsResponse.Record domain, String ip) throws ClientException {
        UpdateDomainRecordRequest request = new UpdateDomainRecordRequest();
        request.setRecordId(domain.getRecordId());
        request.setValue(ip);
        request.setType(domain.getType());
        request.setRR(domain.getRR());
        request.setPriority(domain.getPriority());
        request.setTTL(domain.getTTL());
        request.setLine(domain.getLine());
        UpdateDomainRecordResponse recordResponse = client.getAcsResponse(request);
        System.out.println("更新DNS服务成功！");
    }

    public static void sendErrorMail(String msg) throws Exception {
        Properties prop = new Properties();
        // 开启debug调试，以便在控制台查看
        prop.setProperty("mail.debug", "true");
        // 设置邮件服务器主机名
        prop.setProperty("mail.host", "smtp.qq.com");
        // 发送服务器需要身份验证
        prop.setProperty("mail.smtp.auth", "true");
        // 发送邮件协议名称
        prop.setProperty("mail.transport.protocol", "smtp");

        // 开启SSL加密，否则会失败
        MailSSLSocketFactory sf = new MailSSLSocketFactory();
        sf.setTrustAllHosts(true);
        prop.put("mail.smtp.ssl.enable", "true");
        prop.put("mail.smtp.ssl.socketFactory", sf);

        // 创建session
        Session session = Session.getInstance(prop);
        // 通过session得到transport对象
        Transport ts = session.getTransport();
        // 连接邮件服务器：邮箱类型，帐号，授权码代替密码（更安全）
        ts.connect("smtp.qq.com", config.sendMailQQ, config.qqMailPwd);//后面的字符是授权码，用qq密码反正我是失败了（用自己的，别用我的，这个号是我瞎编的，为了。。。。）
        MimeMessage message = new MimeMessage(session);
// 指明邮件的发件人
        message.setFrom(new InternetAddress(config.sendMailQQ + "@qq.com"));
// 指明邮件的收件人，现在发件人和收件人是一样的，那就是自己给自己发
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(config.receiveMail));
// 邮件的标题
        message.setSubject("自动更新DNS解析失败！");
// 邮件的文本内容
        message.setContent(msg, "text/html;charset=UTF-8");
        // 发送邮件
        ts.sendMessage(message, message.getAllRecipients());
        ts.close();
    }
}