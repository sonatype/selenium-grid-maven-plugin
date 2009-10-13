package org.sonatype.maven.plugin.seleniumgrid.util;

import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class SeleniumUtil
{
    public static void stopHub()
        throws Exception
    {
        String data = URLEncoder.encode( "action", "UTF-8" ) + "=" + URLEncoder.encode( "shutdown", "UTF-8" );

        // Send data
        URL url = new URL( "http://localhost:4444/lifecycle-manager" );
        URLConnection conn = url.openConnection();
        conn.setDoOutput( true );
        OutputStreamWriter wr = new OutputStreamWriter( conn.getOutputStream() );
        wr.write( data );
        wr.flush();
        try
        {
            conn.getInputStream().close();
        }
        catch ( Exception e )
        {
            // will throw exception because the service will die
        }
        wr.close();
    }

    public static void stopRC( int port )
        throws Exception
    {
        URL url = new URL( "http://localhost:" + port + "/selenium-server/driver/?cmd=shutDownSeleniumServer" );
        URLConnection conn = url.openConnection();
        try
        {
            conn.getInputStream().close();
        }
        catch ( Exception e )
        {
            // will throw exception because the service will die
        }
    }
}
