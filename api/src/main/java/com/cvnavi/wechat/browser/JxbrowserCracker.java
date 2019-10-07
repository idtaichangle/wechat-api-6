package com.cvnavi.wechat.browser;

import java.lang.reflect.Field;
import java.util.Date;

public class JxbrowserCracker{
    /**
     * <p>
     * ay.b()方法是检查License的方法。
     * 该方法中，首先判断上次检查license是什么时候。如果上次检查是在一天之内（86400000L），则不用再检查。
     * 记录上次检查时间的变量为ay类中的 private long e=0L; 所以，只需要将e设置成当前时间(e=new
     * Date().getTi)，b()就直接return了。
     * </p>
     * <p>
     * 还有一个可以临时破解的方法。从官方网站下载的jxbrowser包中，有一个jxbrowserdemo.jar。里面有一个META-INF\teamdev.licenses。
     * BrowserContext类在com.teamdev.jxbrowser.chromium.demo.JxBrowserDemo
     * 类中初始化时，它会使用该demo license。 所以可以使用如下代码初始化BrowserContext。 <br/>
     *
     * <PRE>
     * package com.teamdev.jxbrowser.chromium.demo;
     *
     * import com.teamdev.jxbrowser.chromium.BrowserContext;
     *
     * public class JxBrowserDemo {
     * 	private static BrowserContext context;
     * 	static {
     * 		context = BrowserContext.defaultContext();
     * 	}
     *
     * 	public static BrowserContext getContext() {
     * 		return context;
     * 	}
     *
     * }
     * </PRE>
     *
     * <br/>
     * 使用Browser对象时，这样初始化：Browser browser=new
     * Browser(JxBrowserDemo.getContext());
     * </p>
     */
    public static void crack() {
        try {
            Class<?> c = Class.forName("com.teamdev.jxbrowser.chromium.internal.ipc.j");
            Field f = c.getDeclaredField("d");
            f.setAccessible(true);
            Object obj = f.get(null);

            Field f2 = c.getSuperclass().getDeclaredField("e");
            f2.setAccessible(true);
            f2.setLong(obj, new Date().getTime());//
        } catch (Exception ex) {
        }
    }
}