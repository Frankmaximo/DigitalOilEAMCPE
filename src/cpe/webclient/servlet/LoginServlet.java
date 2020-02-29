package cpe.webclient.servlet


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

/**
 * zhangmeng 20200229
 * 中油瑞飞平台与Maximo系
 *
 *
 */
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = -1668394618015299239L;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /**
         *  如果子类重写父类的doPost或者doGet且在这些方法里面使用了跳转(服务端跳转或者客户端跳转), 则不能调用super.doPost(req, resp)这样类似的方法。
         *  super.doPost(req, resp);
         */
        MXSession mxsession = getMXSession(req);
//		mxsession.setLocale(Locale.CHINA);
//		mxsession.setLangCode("ZH");
        boolean isLogin = false;
        boolean isConnected = mxsession.isConnected();
        if (isConnected) { // 如果之前已经连接则直接跳转到启动中心
            isLogin = true;
        } else {
            // 否则尝试连接
            connect(mxsession, req);
            if (mxsession.isConnected()) {
                // 如果登录成功
                isLogin = true;
            }
        }

        if (isLogin) {
            // 登录成功 跳转到启动中心
            String url = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath() + "/";
            String startcntrUrl = url + "ui/login";
            // 登陆成功,则转到启动中心
            resp.sendRedirect(startcntrUrl);

            /**
             * 下面的这段代码无效
             *   WebClientSession wcs = WebClientSessionFactory.getWebClientSessionFactory().createSession(request, resp);
             *   WebClientEvent event= new WebClientEvent("loadapp", "startcntr", "startcntr", wcs);
             *   WebClientRuntime.sendEvent(event);
             */
        }
    }

    /**
     * 获取会话
     */
    public static MXSession getMXSession(HttpServletRequest request) {
        HttpSession httpsession = request.getSession(true);
        MXSession mxsession = null;
        if (httpsession != null) {
            mxsession = (MXSession) httpsession.getAttribute("MXSession");
            if (mxsession == null) {
                mxsession = WebAppSessionProvider.getNewWebAppSession();
                if (mxsession != null) {
                    mxsession.setInteractive(true);
                    httpsession.setAttribute("MXSession", mxsession);
                }
            }
        }

        return mxsession;
    }

    /**
     * 连接系统(登陆系统)
     */
    public static void connect(MXSession mxsession, HttpServletRequest request) {
        try {
            MXServer mxServer = MXServer.getMXServer();
            Properties properties = mxServer.getConfig();
            String hostname = properties.getProperty("mxe.hostname").trim();

            String token = request.getParameter("token");

            /** 中油瑞飞提供的token验证接口*/

            URL url = new URL("http://www.cpe-dev.cnpcrd.rdtp.cloud/sys/user/currentuser");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.setRequestProperty("Authorization", "Bearer " + token);
            connection.connect();

            // 定义BufferedReader输入流来读取URL的响应
            BufferedReader currentuser = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String line;
            while ((line = currentuser.readLine()) != null) {
                result += line;
            }
            //解析json对象
            JSONObject currentuserJson = JSONObject.fromObject(result);
            System.out.println(currentuserJson.get("loginName"));

            String username = WebClientRuntime.decodeSafevalue(currentuserJson.get("loginName"));
            //密码直接使用数据库中存储的密码
            String password = WebClientRuntime.decodeSafevalue("111111");


            mxsession.setHost(hostname);
            mxsession.setUserName(username);
            mxsession.setPassword(password);
            mxsession.connect();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * 断开连接(退出系统)
     */
    public static void disconnect(MXSession mxsession) {
        try {
            mxsession.disconnect();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

}
