package org.sonatype.maven.plugin.seleniumgrid;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.sonatype.maven.plugin.seleniumgrid.util.SeleniumUtil;

/**
 * @goal test-grid
 * @phase integration-test
 */
public class TestSeleniumGridMojo
    extends AbstractMojo
{

    /**
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    /**
     * @parameter default-value="*firefox" expression="${selenium.environment}"
     */
    private String environment;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        String[] ports = session.getExecutionProperties().getProperty( "selenium-ports" ).split( "," );
        try
        {
            for ( String port : ports )
            {

                System.out.println( port );

                int parseInt = Integer.parseInt( port );

                System.out.println( parseInt );

                String sessionId = SeleniumUtil.startBrowser( parseInt, environment );

                SeleniumUtil.open( parseInt, sessionId );

                SeleniumUtil.closeBrowser( parseInt, sessionId );

            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

}
