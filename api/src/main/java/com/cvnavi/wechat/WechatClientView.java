package com.cvnavi.wechat;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public class WechatClientView extends BrowserView implements AncestorListener {

    public WechatClientView(Browser browser) {
        super((browser));
        addAncestorListener(this);
        SwingUtilities.invokeLater(()->{
            getBrowser().loadURL(Config.loginUrl);
        });
    }


    @Override
    public void ancestorAdded(AncestorEvent event) {
//        getBrowser().loadURL("http://wx.qq.com");
    }

    @Override
    public void ancestorRemoved(AncestorEvent event) {

    }

    @Override
    public void ancestorMoved(AncestorEvent event) {

    }
}
