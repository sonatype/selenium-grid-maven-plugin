package org.sonatype.maven.plugin.seleniumgrid;

import java.util.Arrays;
import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.sonatype.maven.plugin.seleniumgrid.util.SeleniumUtil;

/**
 * Tests connections to selenium grid hub by specifying the port to connect to and optionally the environment (browser, platform).
 *
 * In order for this to be useful, start a server first.
 *
 * @goal test-grid
 * @phase integration-test
 */
public class TestSeleniumGridMojo
        extends AbstractSeleniumGridMojo {


    /**
     * The environment to run the tests in.
     *
     * @parameter default-value="*firefox" expression="${selenium.environment}"
     */
    private String environment;



    public void execute()
            throws MojoExecutionException, MojoFailureException {

        List<String> portList = verifyPortsList();

        try {
            for (String port : portList) {
                int parseInt = Integer.parseInt(port);

                getLog().info("Trying port " + parseInt + " using environment " + environment);

                String sessionId = SeleniumUtil.startBrowser(parseInt, environment);

                SeleniumUtil.open(parseInt, sessionId);

                SeleniumUtil.closeBrowser(parseInt, sessionId);

            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
