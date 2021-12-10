package com.automaton.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class HttpParser
{
    private final static Logger LOGGER = LoggerFactory.getLogger(HttpParser.class);

    private static final int SP = 0x20; // 32
    private static final int CR = 0x0D; // 13
    private static final int LF = 0x0A; // 10

    public HttpRequest parseHttpRequest(InputStream inputStream) throws HttpParsingException
    {
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.US_ASCII);

        HttpRequest request = new HttpRequest();

        try
        {
            LOGGER.debug(inputStream.toString());
            parseRequestLine(reader, request);
            parseHeaders(reader, request);
            parseBody(reader, request);
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return request;
    }

    private void parseRequestLine(InputStreamReader reader, HttpRequest request) throws IOException, HttpParsingException
    {
        StringBuilder processingDataBuffer = new StringBuilder();

        boolean methodParsed = false;
        boolean requestTargetParsed = false;

        int _byte;
        while ((_byte = reader.read()) >= 0)
        {
            if (_byte == CR)
            {
                _byte = reader.read();
                if (_byte == LF)
                {
                    LOGGER.debug("Request Line VERSION to process: {}", processingDataBuffer.toString());
                    if (!methodParsed || !requestTargetParsed)
                    {
                        throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                    }

                    try
                    {
                        request.setHttpVersion(processingDataBuffer.toString());
                    } catch (BadHttpVersionException e)
                    {
                        throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                    }
                    return;
                } else
                {
                    throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                }
            }

            if (_byte == SP)
            {
                if (!methodParsed)
                {
                    LOGGER.debug("Request Line METHOD to process: {}", processingDataBuffer.toString());
                    request.setMethod(processingDataBuffer.toString());
                    methodParsed = true;
                } else if (!requestTargetParsed)
                {
                    LOGGER.debug("Request Line REQUEST TARGET to process: {}", processingDataBuffer.toString());
                    request.setRequestTarget(processingDataBuffer.toString());
                    requestTargetParsed = true;
                } else
                {
                    throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                }
                processingDataBuffer.delete(0, processingDataBuffer.length());
            } else
            {
                processingDataBuffer.append((char) _byte);
                if (!methodParsed)
                {
                    if (processingDataBuffer.length() > HttpMethod.MAX_LENGTH)
                    {
                        throw new HttpParsingException(HttpStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED);
                    }
                }
            }
        }

    }

    private void parseHeaders(InputStreamReader reader, HttpRequest request) throws IOException, HttpParsingException
    {
        StringBuilder processingDataBuffer = new StringBuilder();

        int _byte;
        String header = "";
        while ((_byte = reader.read()) >= 0)
        {
            if (_byte == CR)
            {
                _byte = reader.read();
                if (_byte == LF)
                {
                    if (processingDataBuffer.length() > 0)
                    {
                        header = processingDataBuffer.toString();
                        int colonIndex = header.indexOf(":");
                        request.setHeader(header.substring(0, colonIndex),
                                header.substring(colonIndex + 1, header.length()));

                        processingDataBuffer.delete(0, processingDataBuffer.length());
                        header = "";

                    } else
                    {
                        throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                    }

                    _byte = reader.read();
                    if (_byte == CR)
                    {
                        _byte = reader.read();
                        if (_byte == LF)
                        {
                            return;
                        } else
                        {
                            throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                        }

                    } else
                    {
                        processingDataBuffer.append((char) _byte);
                    }
                } else
                {
                    throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                }
            } else
            {
                processingDataBuffer.append((char) _byte);
            }
        }
    }

    private void parseBody(InputStreamReader reader, HttpRequest request) throws IOException, HttpParsingException
    {
        if (request.getMethod() != null)
        {
            if (request.getMethod().equals(HttpMethod.POST))
            {
                int maxLength = 0;
                try
                {
                    if (request.getHeader("Content-Length") != null)
                    {
                        maxLength = Integer.parseInt(request.getHeader("Content-Length"));
                    }
                } catch (NumberFormatException e)
                {
                    throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
                }

                if (maxLength == 0)
                {
                    return;
                }

                StringBuilder processingDataBuffer = new StringBuilder();
                int _byte;
                while ((_byte = reader.read()) >= 0)
                {
                    if (_byte == CR)
                    {
                        _byte = reader.read();
                        if (_byte == LF)
                        {
                            _byte = reader.read();
                            if (_byte == CR)
                            {
                                _byte = reader.read();
                                if (_byte == LF)
                                {
                                    break;
                                } else
                                {
                                    processingDataBuffer.append(CR);
                                    processingDataBuffer.append(LF);
                                    processingDataBuffer.append(CR);
                                    maxLength -= 3;
                                }
                            } else
                            {
                                processingDataBuffer.append(CR);
                                processingDataBuffer.append(LF);
                                maxLength -= 2;
                            }
                        } else
                        {
                            processingDataBuffer.append(CR);
                            maxLength--;
                        }
                    }

                    processingDataBuffer.append((char) _byte);
                    maxLength--;
                    if (maxLength <= 0)
                    {
                        break;
                    }
                }

                request.setBody(processingDataBuffer.toString());
            }
        }
    }
}
