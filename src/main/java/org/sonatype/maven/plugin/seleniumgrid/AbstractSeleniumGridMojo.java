
package org.sonatype.maven.plugin.seleniumgrid;

import java.util.Arrays;
import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Selenium Grid Mojo Support
 * @author plynch
 */
public abstract class AbstractSeleniumGridMojo extends AbstractMojo {

    /**
     * Session execution property set by start-grid mojo that stores the list of ports on which
     * Selenium Remote Controls have been started on.
     */
    protected static final String SELENIUM_PORTS_PROP = "selenium-ports";

    /**
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;


    /**
     * @parameter default-value="true" expression="${selenium-grid.silent}"
     */
    private boolean silent = true;

    protected void noise(String message) {
        if (!this.silent) {
            getLog().info("selenium-grid-maven-plugin: " + message);
        }
    }

    protected void hasMissingPorts() throws MojoExecutionException {
        throw new MojoExecutionException("Please specify one or more ports using -D" + SELENIUM_PORTS_PROP + "=<port>,(port),... where a selenium grid hub proxy or selenium remote control server is listening for connections.");
    }

    protected List<String> verifyPortsList() throws MojoExecutionException {
        String ports = session.getExecutionProperties().getProperty(SELENIUM_PORTS_PROP);
        if (ports == null) {
            hasMissingPorts();
        }

        List<String> portList = Arrays.asList(ports.split(","));
        if (portList.isEmpty()) {
            hasMissingPorts();
        }

        for (String port : portList) {
            try {
                Integer.parseInt(port);
            } catch (NumberFormatException e) {
                throw new MojoExecutionException("You apparently specified an invalid port: " + port, e);
            }
        }
        return portList;
    }


}
