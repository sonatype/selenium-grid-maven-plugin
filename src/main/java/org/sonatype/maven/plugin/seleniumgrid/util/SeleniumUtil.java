package org.sonatype.maven.plugin.seleniumgrid.util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.StreamPumper;

public class SeleniumUtil {


    public static int getRandomFreePort() throws MojoExecutionException {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(0);
            return ss.getLocalPort();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    // just closing
                }
            }
        }
    }

    public static void execute(final Commandline cmd, final Log log) throws MojoExecutionException {
        try {
            Process p = cmd.execute();

            StreamPumper outputPumper = new StreamPumper(p.getInputStream(),
                    new StreamConsumer()  {

                        public void consumeLine(String line) {
                            if (log.isDebugEnabled()) {
                                log.info(line);
                            }
                        }
                    });
            StreamPumper errorPumper = new StreamPumper(p.getErrorStream(),
                    new StreamConsumer()  {

                        public void consumeLine(String line) {
                            if (log.isDebugEnabled()) {
                                log.error(line);
                            }
                        }
                    });

            outputPumper.start();
            errorPumper.start();

            Thread.yield();
        } catch (CommandLineException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    public static void stopHub(int port)
            throws Exception {
        String data = URLEncoder.encode("action", "UTF-8") + "=" + URLEncoder.encode("shutdown", "UTF-8");

        // Send data
        URL url = new URL("http://localhost:" + port + "/lifecycle-manager");
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        try {
            conn.getInputStream().close();
        } catch (Exception e) {
            // will throw exception because the service will die
        }
        wr.close();
    }

    public static void stopRC(int port)
            throws Exception {
        URL url = new URL("http://localhost:" + port + "/selenium-server/driver/?cmd=shutDownSeleniumServer");
        URLConnection conn = url.openConnection();
        try {
            conn.getInputStream().close();
        } catch (Exception e) {
            // will throw exception because the service will die
        }
    }

    public static String startBrowser(int port, String browser)
            throws Exception {
        // http://localhost:5555/selenium-server/driver/?cmd=getNewBrowserSession&1=*opera&2=http://localhost:8084/nexus
        URL url =
                new URL("http://localhost:" + port + "/selenium-server/driver/?cmd=getNewBrowserSession&1=" + browser
                + "&2=http://www.google.com");
        URLConnection conn = url.openConnection();
        String result = IOUtil.toString(conn.getInputStream());
        if (!result.startsWith("OK")) {
            throw new IllegalStateException("Server ran into an invalid state: " + result);
        }

        return result.substring(3);
    }

    public static void open(int port, String sessionId)
            throws Exception {
        // http://localhost:5555/selenium-server/driver/?cmd=open&1=http://localhost:8084/nexus&sessionId=a6c173f6e7054ab489a5b5a88c0da1a5
        URL url =
                new URL("http://localhost:" + port
                + "/selenium-server/driver/?cmd=open&1=http://www.google.com&sessionId=" + sessionId);
        URLConnection conn = url.openConnection();
        String result = IOUtil.toString(conn.getInputStream());
        if (!"OK".equals(result)) {
            throw new IllegalStateException("Server ran into an invalid state: " + result);
        }
    }

    public static void closeBrowser(int port, String sessionId)
            throws Exception {
        // http://localhost:5555/selenium-server/driver/?cmd=testComplete&sessionId=a6c173f6e7054ab489a5b5a88c0da1a5
        URL url =
                new URL("http://localhost:" + port + "/selenium-server/driver/?cmd=testComplete&sessionId=" + sessionId);
        URLConnection conn = url.openConnection();
        String result = IOUtil.toString(conn.getInputStream());
        if (!"OK".equals(result)) {
            throw new IllegalStateException("Server ran into an invalid state: " + result);
        }
    }
}
