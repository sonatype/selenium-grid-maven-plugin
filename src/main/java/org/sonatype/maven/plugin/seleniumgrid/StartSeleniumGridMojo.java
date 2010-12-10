package org.sonatype.maven.plugin.seleniumgrid;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.StreamPumper;

/**
 * @author velo
 * @goal start-grid
 * @phase pre-integration-test
 */
public class StartSeleniumGridMojo extends AbstractMojo {

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
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * @parameter default-value="true"
     */
    private boolean silent;

    private void noise(String message) {
        if (!this.silent) {
            getLog().info("selenium-grid-maven-plugin: " + message);
        }
    }

    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException, MojoFailureException {
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
        StringBuilder ports = new StringBuilder();

        Integer hubPort = getRandomFreePort();

        project.getProperties().put("selenium.hubPort", hubPort.toString());
        noise("setting project property selenium.hubport=" + hubPort.toString());
        ports.append(hubPort);

        try {
            copyGridConfigurationYml(hubPort);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        final Map<String, Artifact> pluginArtifactMap = ArtifactUtils
                .artifactMapByVersionlessId(pluginClasspath);
        Artifact hubArtifact = pluginArtifactMap
                .get("org.seleniumhq.selenium.grid:selenium-grid-hub");
        Artifact rcGridArtifact = pluginArtifactMap
                .get("org.seleniumhq.selenium.grid:selenium-grid-remote-control");
        // use WebDriver if it is on the classpath
        Artifact rcArtifact = pluginArtifactMap
                .get("org.seleniumhq.selenium:selenium-server");
        if (rcArtifact == null) {
            // fallback to Selenium 1.x
            rcArtifact = pluginArtifactMap
                    .get("org.seleniumhq.selenium.server:selenium-server");
        }
        if (rcArtifact == null) {
            throw new MojoFailureException(
                    "Missing selenium-server standalone dependency. "
                            + "Please add a selenium-server standalone implementation to this plugin's dependency list.");
        }

        noise("Using " + rcArtifact.getGroupId() + ":"
                + rcArtifact.getArtifactId()
                + " artifact to launch remote control servers.");

        // start a hub instance
        Commandline cmd = new Commandline();
        cmd.setExecutable("java");
        cmd.createArgument().setLine("-classpath");
        cmd.createArgument().setLine(
                testOutputDirectory.getAbsolutePath() + File.pathSeparator
                        + hubArtifact.getFile().getAbsolutePath());
        cmd.createArgument().setLine(
                "com.thoughtworks.selenium.grid.hub.HubServer");
        String hubUrl = "http://localhost:" + hubPort + "/console";
        noise("starting selenium hub server at " + hubUrl);
        execute(cmd);

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

        // start remote controls
        for (int i = 0; i < numberOfInstances; i++) {
            Integer port = getRandomFreePort();
            ports.append(',');
            ports.append(port);

            cmd = new Commandline();
            cmd.setExecutable("java");
            cmd.setWorkingDirectory(rcGridArtifact.getFile().getParentFile()
                    .getAbsolutePath());
            cmd.createArgument().setLine("-classpath");
            cmd.createArgument().setLine(
                    rcArtifact.getFile().getAbsolutePath() + File.pathSeparator
                            + rcGridArtifact.getFile().getAbsolutePath());
            cmd.createArgument()
                    .setLine(
                            "com.thoughtworks.selenium.grid.remotecontrol.SelfRegisteringRemoteControlLauncher");
            cmd.createArgument().setLine("-port");
            cmd.createArgument().setLine(port.toString());
            cmd.createArgument().setLine("-host");
            cmd.createArgument().setLine("localhost");
            cmd.createArgument().setLine("-hubURL");
            cmd.createArgument().setLine("http://localhost:" + hubPort);
            cmd.createArgument().setLine("-env");
            cmd.createArgument().setLine(environment);
            noise("selenium remote control server on port " + port.toString()
                    + " starting");
            execute(cmd);

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

        noise("setting maven session execution property selenium-ports to: "
                + ports.toString());
        session.getExecutionProperties()
                .put("selenium-ports", ports.toString());
        Runtime.getRuntime().addShutdownHook(
                new SeleniumShutdown(ports.toString()));

    }

    private void copyGridConfigurationYml(Integer hubPort) throws IOException {
        String cfg = IOUtil.toString(getClass().getResourceAsStream(
                "/cfg.template"));
        cfg = cfg.replace("${hubPort}", hubPort.toString());

        testOutputDirectory.mkdirs();
        FileUtils.fileWrite(new File(testOutputDirectory,
                "grid_configuration.yml").getAbsolutePath(), cfg);
    }

    private int getRandomFreePort() throws MojoExecutionException {
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

    private void execute(Commandline cmd) throws MojoExecutionException {
        try {
            Process p = cmd.execute();

            StreamPumper outputPumper = new StreamPumper(p.getInputStream(),
                    new StreamConsumer() {
                        public void consumeLine(String line) {
                            if (getLog().isDebugEnabled()) {
                                getLog().info(line);
                            }
                        }
                    });
            StreamPumper errorPumper = new StreamPumper(p.getErrorStream(),
                    new StreamConsumer() {
                        public void consumeLine(String line) {
                            if (getLog().isDebugEnabled()) {
                                getLog().error(line);
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
}
