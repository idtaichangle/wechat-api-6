package com.cvnavi.wechat;

import com.cvnavi.wechat.browser.BrowserNetworkDelegate;
import com.cvnavi.wechat.browser.JxbrowserCracker;
import com.cvnavi.wechat.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamdev.jxbrowser.chromium.*;
import com.teamdev.jxbrowser.chromium.dom.By;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringEscapeUtils;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static com.teamdev.jxbrowser.chromium.BrowserKeyEvent.KeyCode.VK_BACK;
import static com.teamdev.jxbrowser.chromium.BrowserKeyEvent.KeyCode.VK_END;
import static com.teamdev.jxbrowser.chromium.BrowserKeyEvent.KeyCode.VK_SPACE;
import static com.teamdev.jxbrowser.chromium.BrowserKeyEvent.KeyEventType.PRESSED;
import static com.teamdev.jxbrowser.chromium.BrowserKeyEvent.KeyEventType.RELEASED;
import static com.teamdev.jxbrowser.chromium.BrowserKeyEvent.KeyEventType.TYPED;

/**
 * 微信客户端
 */
@Log4j2
public class WechatClient implements AutoCloseable{

    public static WechatClient INSTANCE=new WechatClient();

    @Getter
    private Browser browser;
    @Getter
    private WechatClientView view;

    @Getter
    private List<WechatListener> listeners=new ArrayList<>();
    private BrowserNetworkDelegate delegate;
    @Getter @Setter
    private boolean isLogin=false;
    @Getter @Setter
    private BufferedImage qrImage;
    @Getter @Setter
    private Member self;
    @Getter @Setter
    private MemerList memberList=new MemerList();
    @Getter @Setter
    private GroupList groupList=new GroupList();

    private WechatClient(){
        JxbrowserCracker.crack();
        BrowserContextParams params = new BrowserContextParams(BrowserPreferences.getDefaultDataDir());
        params.setStorageType(StorageType.MEMORY);

        delegate=new BrowserNetworkDelegate(this);
        BrowserContext browserContext = new BrowserContext(params);
        browserContext.getNetworkService().setNetworkDelegate(delegate);
        browser=new Browser(browserContext);

        view=new WechatClientView(browser);
    }


    /**
     * 发送消息
     * @param msg
     * @return
     */
    public boolean sendMessage(final Message msg){
        if(!isLogin){
            return  false;
        }
        SwingUtilities.invokeLater(()->{
            try{
                if(openChatWindow(msg.getTo().getUserName())){
                    Thread.sleep(10);
                    DOMElement de=browser.getDocument().findElement(By.id("editArea"));
                    de.setInnerText(msg.getContent());
                    browser.executeJavaScript("document.getElementById(\"editArea\").focus();");
                    fireTextChangeEvent();

                    Thread.sleep(5);

                    browser.getDocument().findElement(By.className("btn_send")).click();
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
        return true;
    }
    /**
     * 发送消息
     * @param msg
     * @return
     */
    public boolean sendGroupMessage(final GroupMessage msg){
        if(!isLogin){
            return  false;
        }
        SwingUtilities.invokeLater(()->{
            try{
                DOMElement de=findDomByUserName(msg.getTo().getUserName());
                if(de!=null){
                    de.click();
                    Thread.sleep(20);
                    de=browser.getDocument().findElement(By.id("editArea"));
                    de.setInnerText(msg.getContent());
                    browser.executeJavaScript("document.getElementById(\"editArea\").focus();");
                    fireTextChangeEvent();

                    Thread.sleep(10);

                    browser.getDocument().findElement(By.className("btn_send")).click();
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        });
        return true;
    }

    /**
     * 退出登录
     */
    public void logout(){
        DOMElement de= browser.getDocument().findElement(By.cssSelector("a.opt"));
        if(de!=null){
            de.click();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            de= browser.getDocument().findElement(By.cssSelector("i.menuicon_quit"));
            if(de!=null){
                de.click();
            }
        }
        setLogin(false);
    }

    /**
     * 获取登录页面的二维码图片
     * @return
     */
    public BufferedImage refreshLoginQrImage(){
        if(!isLogin){
            qrImage=null;
            browser.loadURL(Config.loginUrl);
            try {
                for(int i=0;i<100;i++){
                    Thread.sleep(200);
                    if(qrImage!=null){
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return qrImage;
        }
        return null;
    }

    /**
     * 从最近聊天记录里点击某个人
     * @param userName
     */
    private DOMElement findDomByUserName(String userName) throws InterruptedException {
        browser.getDocument().findElement(By.cssSelector(".web_wechat_tab_chat")).click();
        Thread.sleep(50);
        List<DOMElement> list=browser.getDocument().findElements(By.className("chat_item"));
        for(DOMElement de :list){
            if(userName.equals(de.getAttribute("data-username"))){
             return  de;
            }
        }
        return  null;
    }

    /**
     * 从联系人列表打开指定人的聊天窗口
     * @param userName
     */
    private boolean openChatWindow(String userName) throws InterruptedException {
        DOMElement tab=browser.getDocument().findElement(By.cssSelector(".web_wechat_tab_friends"));
        tab.click();
        Thread.sleep(50);
        List<DOMElement> list=browser.getDocument().findElements(By.cssSelector(".contact_item img"));
        for(DOMElement de:list){
            String src=de.getAttribute("mm-src");
            if(src.contains("username="+userName)){
                de.click();
                Thread.sleep(50);
                de=browser.getDocument().findElement(By.cssSelector(".action_area a"));
                de.click();
                return true;
            }
        }
        return  false;
    }

    /**
     * 触发文本框的值变化事件
     */
    private void fireTextChangeEvent() throws InterruptedException {
        Thread.sleep(50);
        forwardKeyEvent(VK_END);
        Thread.sleep(50);
        forwardKeyEvent(VK_SPACE);
        Thread.sleep(50);
        forwardKeyEvent(VK_BACK);
    }


    private void forwardKeyEvent(BrowserKeyEvent.KeyCode code) {
        browser.forwardKeyEvent(new BrowserKeyEvent(PRESSED, code));
        browser.forwardKeyEvent(new BrowserKeyEvent(TYPED, code));
        browser.forwardKeyEvent(new BrowserKeyEvent(RELEASED, code));
    }


    public void addListener(WechatListener listener) {
        listeners.add(listener);
    }

    public void removeListener(WechatListener listener){
        listeners.remove(listener);
    }


    @Override
    public void close() throws Exception {
        browser.dispose();
    }

    public void onLogout() {
        if(isLogin){
            setLogin(false);
            setSelf(null);
            memberList.clear();
            groupList.clear();
            for(WechatListener listener :listeners){
                listener.onLogout();
            }
        }
    }
}
