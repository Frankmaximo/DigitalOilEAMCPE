package cpe.webclient.servlet;


import com.alibaba.fastjson.JSONObject;
import com.ibm.tivoli.maximo.report.birt.session.WebAppSessionProvider;
import psdi.util.MXSession;
import psdi.webclient.system.runtime.WebClientRuntime;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Logger;


/**
 * zhangmeng 20200229
 * 中油瑞飞平台与Maximo单点登陆接口
 */
public class LoginServlet extends HttpServlet {


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
            Logger logger  =  Logger.getLogger(LoginServlet.class.getName());

            psdi.server.MXServer mxServer = psdi.server.MXServer.getMXServer();
            Properties properties = mxServer.getConfig();
            String hostname = properties.getProperty("mxe.hostname").trim();

            String token = request.getParameter("token");

            /* 中油瑞飞提供的token验证接口*/

            URL url = new URL("http://www.cpe-dev.cnpcrd.rdtp.cloud/sys/user/currentuser");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + token);

            logger.info("token" + "Bearer " + token);
            connection.connect();

            // 定义BufferedReader输入流来读取URL的响应
            BufferedReader currentuser = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            String line;
            StringBuilder result = null;
            while ((line = currentuser.readLine()) != null) {

                result.append(line);
            }
            //解析json对象
            assert result != null;

            /* 阿里巴巴的fastjson解析 字符串转换为对象 */
            JSONObject currentsJson = (JSONObject) JSONObject.parse(result.toString());
            /* 修改为SLF4j */

            logger.info("登陆用户名称：" + currentsJson.get("loginName"));

            String username = WebClientRuntime.decodeSafevalue((String) currentsJson.get("loginName"));
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


}
