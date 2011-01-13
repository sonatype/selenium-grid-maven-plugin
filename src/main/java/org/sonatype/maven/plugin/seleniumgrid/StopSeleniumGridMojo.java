package org.sonatype.maven.plugin.seleniumgrid;

import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.sonatype.maven.plugin.seleniumgrid.util.SeleniumUtil;

/**
 * @author velo
 * @goal stop-grid
 * @phase post-integration-test
 */
public class StopSeleniumGridMojo
    extends AbstractSeleniumGridMojo
{

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        List<String> portList = verifyPortsList();
        String[] ports = portList.toArray(new String[]{});
        try
        {
            // process in reverse so that hub is stopped last
            for ( int i = ports.length - 1; i >= 0; i-- )
            {
                int port = Integer.parseInt( ports[i] );
                if ( i == 0 )
                {
                    noise("stopping hub on port " + port);
                    SeleniumUtil.stopHub( port );
                }
                else
                {
                    noise("stopping remote control server on port " + port);
                    SeleniumUtil.stopRC( port );
                }
            }

        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

}
