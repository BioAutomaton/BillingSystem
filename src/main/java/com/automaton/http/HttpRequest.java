package com.automaton.http;

import java.util.HashMap;

public class HttpRequest extends HttpMessage
{
    private HttpMethod method;
    private String requestTarget;
    private String originalHttpVersion;  // literal from the request
    private HttpVersion bestCompatibleHttpVersion;
    private HashMap<String, String> headers;
    private String body;

    HttpRequest()
    {
        headers = new HashMap<String, String>();
    }

    public HttpMethod getMethod()
    {
        return method;
    }

    void setMethod(String methodName) throws HttpParsingException
    {
        for (HttpMethod method : HttpMethod.values())
        {
            if (methodName.equals(method.name()))
            {
                this.method = method;
                return;
            }
        }
        throw new HttpParsingException(
                HttpStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED
        );
    }

    public String getRequestTarget()
    {
        return requestTarget;
    }

    void setRequestTarget(String requestTarget) throws HttpParsingException
    {
        if (requestTarget == null || requestTarget.length() == 0)
        {
            throw new HttpParsingException(HttpStatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
        }
        this.requestTarget = requestTarget;
    }

    public void setHeader(String key, String value) throws HttpParsingException
    {
        if (headers.get(key) == null)
        {
            this.headers.put(key, value.strip());
        } else
        {
            throw new HttpParsingException(HttpStatusCode.CLIENT_ERROR_400_BAD_REQUEST);
        }
    }

    public String getHeader(String key)
    {
        return headers.get(key);
    }

    public HttpVersion getBestCompatibleHttpVersion()
    {
        return bestCompatibleHttpVersion;
    }

    public String getOriginalHttpVersion()
    {
        return originalHttpVersion;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public void setHttpVersion(String originalHttpVersion) throws BadHttpVersionException, HttpParsingException
    {
        this.originalHttpVersion = originalHttpVersion;
        this.bestCompatibleHttpVersion = HttpVersion.getBestCompatibleVersion(originalHttpVersion);
        if (this.bestCompatibleHttpVersion == null)
        {
            throw new HttpParsingException(
                    HttpStatusCode.SERVER_ERROR_505_HTTP_VERSION_NOT_SUPPORTED
            );
        }
    }
}
