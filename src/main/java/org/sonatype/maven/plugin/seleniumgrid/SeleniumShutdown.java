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
        for ( int i = ports.length - 1; i >= 0; i-- )
        {
            int port = Integer.parseInt( ports[i] );
            if ( i == 0 )
            {
                try
                {
                    SeleniumUtil.stopHub( port );
                }
                catch ( Exception e )
                {
                    // nothing I can do at this point!
                }
            }
            else
            {
                try
                {
                    SeleniumUtil.stopRC( port );
                }
                catch ( Exception e )
                {
                    // nothing I can do at this point!
                }
            }
        }

    }

}
