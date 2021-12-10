package com.automaton.billingsystem.core;

import com.automaton.http.HttpParser;
import com.automaton.http.HttpParsingException;
import com.automaton.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class HttpConnectionWorkerThread extends Thread
{
    private final static Logger LOGGER = LoggerFactory.getLogger(HttpConnectionWorkerThread.class);
    private Socket socket;

    public HttpConnectionWorkerThread(Socket socket)
    {
        this.socket = socket;
    }

    @Override
    public void run()
    {
        OutputStream outputStream = null;
        InputStream inputStream = null;

        try
        {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            HttpParser parser = new HttpParser();
            HttpRequest request = parser.parseHttpRequest(inputStream);

            String html = "<html><head><title>Billing System</title></head><body><h1>" + request.getBody() + "</h1></body></html>";

            final String CRLF = "\r\n"; // 13, 10

            String response = "HTTP/1.1 200 OK" + CRLF +
                    "Content-Length: " + html.getBytes().length + CRLF +
                    CRLF +
                    html +
                    CRLF + CRLF;
            
            outputStream.write(response.getBytes());


            LOGGER.info(" * Connection Processing Finished");
        } catch (IOException e)
        {
            LOGGER.error(" * Problem with communication", e);
        } catch (HttpParsingException e)
        {
            LOGGER.error(" * Error parsing the request", e);
        } finally
        {
            if (inputStream != null)
            {
                try
                {
                    inputStream.close();
                } catch (IOException ignored)
                {
                }

            }
            if (outputStream != null)
            {
                try
                {
                    outputStream.close();
                } catch (IOException ignored)
                {
                }
            }
            if (socket != null)
            {
                try
                {
                    socket.close();
                } catch (IOException ignored)
                {
                }
            }

        }
    }
}
