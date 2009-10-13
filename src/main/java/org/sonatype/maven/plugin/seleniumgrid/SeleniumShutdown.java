package org.sonatype.maven.plugin.seleniumgrid;

import org.sonatype.maven.plugin.seleniumgrid.util.SeleniumUtil;

public class SeleniumShutdown
    extends Thread
{

    private String ports;

    public SeleniumShutdown( String ports )
    {
        this.ports = ports;
    }

    @Override
    public void run()
    {
        String[] ports = this.ports.split( "," );
        for ( String port : ports )
        {
            try
            {
                SeleniumUtil.stopRC( new Integer( port ) );
            }
            catch ( Exception e )
            {
                // ignore and next!
            }
        }

        try
        {
            SeleniumUtil.stopHub();
        }
        catch ( Exception e )
        {
            // nothing I can do at this point!
        }
    }

}
