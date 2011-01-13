package org.sonatype.maven.plugin.seleniumgrid;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.Commandline;
import org.sonatype.maven.plugin.seleniumgrid.util.SeleniumUtil;

/**
 * Starts a Selenium Grid Hub Server, then one or more Selenium remote Control Servers at random ports.
 *
 * More info: http://selenium-grid.seleniumhq.org/how_it_works.html
 *
 * @author velo
 * @goal start-grid
 * @phase pre-integration-test
 */
public class StartSeleniumGridMojo extends AbstractSeleniumGridMojo {

    protected static final String SELENIUM_HUBPORT_PROP = "selenium.hubPort";

    protected static final String SELENIUM_GRID_HUB_GA = "org.seleniumhq.selenium.grid:selenium-grid-hub";

    protected static final String SELENIUM_GRID_RC_GA = "org.seleniumhq.selenium.grid:selenium-grid-remote-control";

    protected static final String SELENIUM_1_GA = "org.seleniumhq.selenium.server:selenium-server";

    protected static final String WEBDRIVER_GA = "org.seleniumhq.selenium:selenium-server";

    protected static final String HUB_SERVER_MAIN = "com.thoughtworks.selenium.grid.hub.HubServer";

    protected static final String RC_LAUNCHER_MAIN = "com.thoughtworks.selenium.grid.remotecontrol.SelfRegisteringRemoteControlLauncher";

    /**
     * Plugin classpath.
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    protected List<Artifact> pluginClasspath;

    /**
     * @parameter expression="${selenium.numberOfInstances}"
     */
    private Integer numberOfInstances;

    /**
     * @parameter default-value="*firefox" expression="${selenium.environment}"
     */
    private String environment;

    /**
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    /**
     * @parameter expression="${project.build.testOutputDirectory}"
     * @required
     */
    private File testOutputDirectory;

    /**
     * @parameter default-value="false" expression="${selenium-grid.block}"
     */
    private boolean block;

    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    protected void configureNumberOfRemoteControlServers() {
        if (numberOfInstances == null) {
            String processors = System.getenv("NUMBER_OF_PROCESSORS");
            try {
                numberOfInstances = Integer.parseInt(processors);
            } catch (NumberFormatException e) {
                numberOfInstances = 2;
            }
        }
        noise("Number of remote control instances configured: "
                + numberOfInstances);
    }

    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException, MojoFailureException {
        configureNumberOfRemoteControlServers();

        StringBuilder ports = new StringBuilder();
        Integer hubPort = SeleniumUtil.getRandomFreePort();

        project.getProperties().put(SELENIUM_HUBPORT_PROP, hubPort.toString());
        noise("setting project property " + SELENIUM_HUBPORT_PROP + "=" + hubPort.toString());
        ports.append(hubPort);

        try {
            copyGridConfigurationYml(hubPort);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        // get artifacts used in the classpath when starting the grid servers
        final Map<String, Artifact> pluginArtifactMap = ArtifactUtils.artifactMapByVersionlessId(pluginClasspath);
        Artifact hubArtifact = pluginArtifactMap.get(SELENIUM_GRID_HUB_GA);
        if (hubArtifact == null) {
            throw new MojoExecutionException("Missing required depdendency " + SELENIUM_GRID_HUB_GA);
        }

        Artifact rcGridArtifact = pluginArtifactMap.get(SELENIUM_GRID_RC_GA);
        if (rcGridArtifact == null) {
            throw new MojoExecutionException("Missing required depdendency " + SELENIUM_GRID_RC_GA);
        }

        // use WebDriver/Selenium 2 if it is on the classpath
        Artifact rcArtifact = pluginArtifactMap.get(WEBDRIVER_GA);
        if (rcArtifact == null) {
            // fallback to Selenium 1.x
            rcArtifact = pluginArtifactMap.get(SELENIUM_1_GA);
        }
        if (rcArtifact == null) {
            String ls = System.getProperty("line.separator");
            StringBuilder str = new StringBuilder();
            str.append(ls).append("Missing selenium-server standalone dependency. ").append(ls);
            str.append("Please add a selenium-server standalone implementation to this plugin's dependency list. One of").append(ls);
            str.append("Webdriver/Selenium 2: ").append(WEBDRIVER_GA).append(ls);
            str.append("Selenium 1.x: ").append(SELENIUM_1_GA).append(ls);
            str.append("If both are present, WebDriver will be selected.");

            throw new MojoFailureException(str.toString());
        }
        noise("Using " + rcArtifact.getGroupId() + ":"
                + rcArtifact.getArtifactId()
                + " artifact to launch remote control servers.");

        // start the grid hub instance
        Commandline cmd = new Commandline();
        cmd.setExecutable("java");
        cmd.createArg().setLine("-classpath");
        cmd.createArg().setLine(
                testOutputDirectory.getAbsolutePath() + File.pathSeparator
                + hubArtifact.getFile().getAbsolutePath());
        cmd.createArg().setLine(HUB_SERVER_MAIN);
        String hubUrl = "http://localhost:" + hubPort + "/console";
        noise("starting selenium hub server at " + hubUrl);
        SeleniumUtil.execute(cmd, getLog());

        // wait for hub to start before proceeding
        int c = 0;
        while (true) {
            c++;
            try {
                URL url = new URL(hubUrl);
                URLConnection conn = url.openConnection();
                conn.getInputStream().close();
                break;
            } catch (Exception e) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    // ignore
                }
                if (c > 50) {
                    throw new MojoExecutionException(
                            "Failed launch grid hub console: " + hubUrl);
                }
            }
        }
        noise("selenium hub server start complete.");

        // start remote controls that the hub will talk to
        for (int i = 0; i < numberOfInstances; i++) {
            Integer port = SeleniumUtil.getRandomFreePort();
            ports.append(',');
            ports.append(port);

            cmd = new Commandline();
            cmd.setExecutable("java");
            cmd.setWorkingDirectory(rcGridArtifact.getFile().getParentFile().getAbsolutePath());
            cmd.createArg().setLine("-classpath");
            cmd.createArg().setLine(
                    rcArtifact.getFile().getAbsolutePath() + File.pathSeparator
                    + rcGridArtifact.getFile().getAbsolutePath());
            cmd.createArg().setLine(RC_LAUNCHER_MAIN);
            cmd.createArg().setLine("-port");
            cmd.createArg().setLine(port.toString());
            cmd.createArg().setLine("-host");
            cmd.createArg().setLine("localhost");
            cmd.createArg().setLine("-hubURL");
            cmd.createArg().setLine("http://localhost:" + hubPort);
            cmd.createArg().setLine("-env");
            cmd.createArg().setLine(environment);
            noise("selenium remote control server on port " + port.toString()
                    + " starting with environment " + environment);
            SeleniumUtil.execute(cmd, getLog());

            // wait until remote control is launched
            c = 0;
            while (true) {
                try {
                    c++;

                    URL url = new URL("http://localhost:" + port
                            + "/selenium-server/driver/?cmd=status");
                    noise("testing selenium remote control server at "
                            + url.toString());
                    URLConnection conn = url.openConnection();
                    conn.getInputStream().close();
                    noise("selenium remote control server on port "
                            + port.toString() + " started");
                    break;
                } catch (Exception e) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e1) {
                        // ignore
                    }
                    if (c > 50) {
                        throw new MojoExecutionException(
                                "Failed launch grid hub console: " + hubUrl);
                    }
                }
            }
        }

        noise("Storing ports as " + SELENIUM_PORTS_PROP + "="
                + ports.toString());
        session.getExecutionProperties().put(SELENIUM_PORTS_PROP, ports.toString());
        Runtime.getRuntime().addShutdownHook(
                new SeleniumShutdown(ports.toString()));

        // optionally pause and wait - useful for manually testing with a separate test execution
        if (block) {
            getLog().info("Blocking to accept connections...");
            try {
                Thread.currentThread().join();
            } catch (InterruptedException ex) {
                // user killed the process, ignore
            }
        }

    }

    private void copyGridConfigurationYml(Integer hubPort) throws IOException {
        String cfg = IOUtil.toString(getClass().getResourceAsStream(
                "/cfg.template"));
        cfg = cfg.replace("${hubPort}", hubPort.toString());

        testOutputDirectory.mkdirs();
        FileUtils.fileWrite(new File(testOutputDirectory,
                "grid_configuration.yml").getAbsolutePath(), cfg);
    }
}
