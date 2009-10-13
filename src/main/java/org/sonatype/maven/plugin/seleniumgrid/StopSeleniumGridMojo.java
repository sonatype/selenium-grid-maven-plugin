package org.sonatype.maven.plugin.seleniumgrid;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.sonatype.maven.plugin.seleniumgrid.util.SeleniumUtil;

/**
 * @author velo
 * @goal stop-grid
 * @phase post-integration-test
 */
public class StopSeleniumGridMojo
    extends AbstractMojo
{

    /**
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        String[] ports = session.getExecutionProperties().getProperty( "selenium-ports" ).split( "," );
        try
        {
            for ( String port : ports )
            {
                SeleniumUtil.stopRC( Integer.parseInt( port ) );
            }

            SeleniumUtil.stopHub();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

}
