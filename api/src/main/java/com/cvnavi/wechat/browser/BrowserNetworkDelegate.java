package com.cvnavi.wechat.browser;

import com.cvnavi.wechat.WechatClient;
import com.cvnavi.wechat.WechatParser;
import com.teamdev.jxbrowser.chromium.BeforeURLRequestParams;
import com.teamdev.jxbrowser.chromium.DataReceivedParams;
import com.teamdev.jxbrowser.chromium.RequestCompletedParams;
import com.teamdev.jxbrowser.chromium.swing.DefaultNetworkDelegate;
import lombok.extern.log4j.Log4j2;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class BrowserNetworkDelegate extends DefaultNetworkDelegate {

    private WechatClient client;

    public BrowserNetworkDelegate(WechatClient client) {
        this.client = client;
    }

    /**
     * 某些请求返回的数据量比较大，一次请求可能要多次调用 onDataReceived。
     * 需要将多次请求返回的结果拼接起来。
     */
    ConcurrentHashMap<Long,byte[]> respMap=new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String,BufferedImage> imageMap=new ConcurrentHashMap<>();


    @Override
    public void onDataReceived(DataReceivedParams params) {

        String url=params.getURL();
        if(url.contains("webwxgetcontact") ||
                url.contains("webwxbatchgetcontact") ||
                url.contains("webwxsync") ||
                url.contains("webwxinit") ||
                (url.contains("/qrcode/") && params.getMimeType().contains("image")) ||
                (url.contains("/webwxgetmsgimg") && params.getMimeType().contains("image"))){
            synchronized (respMap){
                byte[] b=respMap.get(params.getRequestId());
                if(b==null){
                    b=params.getData();
                }else{
                    byte[] temp=new byte[b.length+params.getData().length];
                    System.arraycopy(b,0,temp,0,b.length);
                    System.arraycopy(params.getData(),0,temp,b.length,params.getData().length);
                    b=temp;
                }
                respMap.put(params.getRequestId(),b);
            }
        }
    }

    @Override
    public void onBeforeURLRequest(BeforeURLRequestParams params) {
        log.debug("onBeforeURLRequest:"+params.getURL());
        if(params.getURL().contains("webwxlogout")){
            client.onLogout();
        }
    }

    @Override
    public void onCompleted(RequestCompletedParams params) {
        log.debug("onCompleted:"+params.getURL());
        if(params.getURL().contains("webwxgetcontact")){
            new Thread(()->{
                byte b[]=respMap.get(params.getRequestId());
                respMap.remove(params.getRequestId());
                if(b!=null){
                    WechatParser.parseMemberList(new String(b));
                }
            }).start();
        }else if(params.getURL().contains("webwxbatchgetcontact")){
            new Thread(()->{
                byte b[]=respMap.get(params.getRequestId());
                respMap.remove(params.getRequestId());
                if(b!=null){
                    WechatParser.parseGroupList(new String(b));
                }
            }).start();
        }else if(params.getURL().contains("webwxsync")){
            new Thread(()->{
                byte b[]=respMap.get(params.getRequestId());
                respMap.remove(params.getRequestId());
                if(b!=null){
                    WechatParser.parseMessage(new String(b));
                }

            }).start();
        }else if(params.getURL().contains("webwxinit")){
            if(!client.isLogin()){
                client.setLogin(true);
                byte b[]=respMap.get(params.getRequestId());
                respMap.remove(params.getRequestId());
                if(b!=null){
                    WechatParser.parseLogin(new String(b));
                }
            }
        }else if(params.getURL().contains("/qrcode/")){
            new Thread(()->{
                byte b[]=respMap.get(params.getRequestId());
                respMap.remove(params.getRequestId());
                if(b!=null){
                    try {
                        BufferedImage image=ImageIO.read(new ByteArrayInputStream(b));
                        client.setQrImage(image);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }else if(params.getURL().contains("/webwxgetmsgimg")){
            new Thread(()->{
                byte b[]=respMap.get(params.getRequestId());
                respMap.remove(params.getRequestId());
                if(b!=null){
                    Matcher m=Pattern.compile("(?<=MsgID=)[0-9]+").matcher(params.getURL());
                    if(m.find()){
                        String msgID=m.group(0);
                        try {
                            BufferedImage image=ImageIO.read(new ByteArrayInputStream(b));
                            imageMap.put(msgID,image);
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    imageMap.remove(msgID);
                                }
                            },3000);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }
}
