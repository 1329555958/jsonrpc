package com.netfinworks.cloud.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.IJsonRpcClient;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.netfinworks.cloud.rpc.sleuth.RpcHttpClientSpanInjector;
import org.apache.commons.collections.MapUtils;
import org.kopitubruk.util.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.JSONRPC_CONTENT_TYPE;

/**
 * A JSON-RPC client that uses the HTTP protocol.
 */
@SuppressWarnings("unused")
public class RpcHttpClient extends JsonRpcClient implements IJsonRpcClient {

    private static final Logger logger = LoggerFactory.getLogger(RpcHttpClient.class);

    private final Map<String, String> headers = new HashMap<>();
    private URL serviceUrl;
    private Proxy connectionProxy = Proxy.NO_PROXY;
    private int connectionTimeoutMillis = 60 * 1000;
    private int readTimeoutMillis = 60 * 1000 * 2;
    private SSLContext sslContext = null;
    private HostnameVerifier hostNameVerifier = null;
    private String contentType = JSONRPC_CONTENT_TYPE;

    private ApplicationContext applicationContext;

    private LoadBalancerClient loadBalancerClient;
    //服务名称
    private String serviceId;
    //rpc 路径
    private String servicePath;
    // sleuth 追踪器
    private Tracer tracer;


    /**
     * Creates the {@link RpcHttpClient} bound to the given {@code serviceUrl}.
     * The headers provided in the {@code headers} map are added to every request
     * made to the {@code serviceUrl}.
     *
     * @param serviceUrl the service end-point URL
     * @param headers    the headers
     */
    public RpcHttpClient(URL serviceUrl, Map<String, String> headers) {
        this(new ObjectMapper(), serviceUrl, headers);
    }

    /**
     * Creates the {@link RpcHttpClient} bound to the given {@code serviceUrl}.
     * The headers provided in the {@code headers} map are added to every request
     * made to the {@code serviceUrl}.
     *
     * @param mapper     the {@link ObjectMapper} to use for json&lt;-&gt;java conversion
     * @param serviceUrl the service end-point URL
     * @param headers    the headers
     */
    public RpcHttpClient(ObjectMapper mapper, URL serviceUrl, Map<String, String> headers) {
        super(mapper);
        this.serviceUrl = serviceUrl;
        this.headers.putAll(headers);
    }

    /**
     * Creates the {@link RpcHttpClient} bound to the given {@code serviceUrl}.
     * The headers provided in the {@code headers} map are added to every request
     * made to the {@code serviceUrl}.
     *
     * @param mapper  the {@link ObjectMapper} to use for json&lt;-&gt;java conversion
     * @param headers the headers
     */
    public RpcHttpClient(ObjectMapper mapper, Map<String, String> headers) {
        super(mapper);
        this.headers.putAll(headers);
    }

    /**
     * Creates the {@link RpcHttpClient} bound to the given {@code serviceUrl}.
     * The headers provided in the {@code headers} map are added to every request
     * made to the {@code serviceUrl}.
     *
     * @param serviceUrl the service end-point URL
     */
    public RpcHttpClient(URL serviceUrl) {
        this(new ObjectMapper(), serviceUrl, new HashMap<String, String>());
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * {@inheritDoc}
     */
    @Override

    public void invoke(String methodName, Object argument) throws Throwable {
        invoke(methodName, argument, null, new HashMap<String, String>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(String methodName, Object argument, Type returnType) throws Throwable {
        return invoke(methodName, argument, returnType, new HashMap<String, String>());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(String methodName, Object argument, Type returnType, Map<String, String> extraHeaders) throws Throwable {
        Object response = null;
        Span span = null;
        if (tracer != null) {
            span = tracer.createSpan(serviceId);
            span.tag("params", JSONUtil.toJSON(argument));
            span.logEvent(Span.CLIENT_SEND);
        }

        Map<String, String> spanHeaders = RpcHttpClientSpanInjector.sleuthHeaders(span);
        spanHeaders.putAll(extraHeaders);
        logger.debug("connection with extraHeaders:{}", spanHeaders);
        HttpURLConnection connection = prepareConnection(spanHeaders);
        try {
            connection.connect();
        } catch (Exception e) {
            //连接异常，重试一次
            if (e instanceof ConnectException) {
                logger.warn("{},连接异常,进行一次重试:{}", connection.getURL().toString(), e.getMessage());
                connection = prepareConnection(spanHeaders);
                connection.connect();
            }
        }

        try {
            try (OutputStream send = connection.getOutputStream()) {
                super.invoke(methodName, argument, send);
            }
            final boolean useGzip = useGzip(connection);
            // read and return value
            try {
                try (InputStream answer = getStream(connection.getInputStream(), useGzip)) {
                    return response = super.readResponse(returnType, answer);
                }
            } catch (IOException e) {
                try (InputStream answer = getStream(connection.getErrorStream(), useGzip)) {
                    return response = super.readResponse(returnType, answer);
                } catch (IOException ef) {
                    throw new HttpException(readErrorString(connection), ef);
                }
            }
        } finally {
            if (tracer != null && tracer.isTracing()) {
                span.tag("result", JSONUtil.toJSON(response));
                span.logEvent(Span.CLIENT_RECV);
                tracer.close(span);
            }
            connection.disconnect();
        }

    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T invoke(String methodName, Object argument, Class<T> clazz) throws Throwable {
        return (T) invoke(methodName, argument, Type.class.cast(clazz));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T invoke(String methodName, Object argument, Class<T> clazz, Map<String, String> extraHeaders) throws Throwable {
        return (T) invoke(methodName, argument, Type.class.cast(clazz), extraHeaders);
    }

    /**
     * Prepares a connection to the server.
     *
     * @param extraHeaders extra headers to add to the request
     * @return the unopened connection
     * @throws IOException
     */
    private HttpURLConnection prepareConnection(Map<String, String> extraHeaders) throws IOException {

        // create URLConnection
        HttpURLConnection connection = (HttpURLConnection) getServiceUrl().openConnection(connectionProxy);
        connection.setConnectTimeout(connectionTimeoutMillis);
        connection.setReadTimeout(readTimeoutMillis);
        connection.setAllowUserInteraction(false);
        connection.setDefaultUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod("POST");

        setupSsl(connection);
        addHeaders(extraHeaders, connection);

        return connection;
    }

    private InputStream getStream(final InputStream inputStream, final boolean useGzip) throws IOException {
        return useGzip ? new GZIPInputStream(inputStream) : inputStream;
    }

    private boolean useGzip(final HttpURLConnection connection) {
        String contentEncoding = connection.getHeaderField("Content-Encoding");
        return contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip");
    }

    private static String readErrorString(final HttpURLConnection connection) {
        try (InputStream stream = connection.getErrorStream()) {
            StringBuilder buffer = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
                for (int ch = reader.read(); ch >= 0; ch = reader.read()) {
                    buffer.append((char) ch);
                }
            }
            return buffer.toString();
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    private void setupSsl(HttpURLConnection connection) {
        if (HttpsURLConnection.class.isInstance(connection)) {
            HttpsURLConnection https = HttpsURLConnection.class.cast(connection);
            if (hostNameVerifier != null) {
                https.setHostnameVerifier(hostNameVerifier);
            }
            if (sslContext != null) {
                https.setSSLSocketFactory(sslContext.getSocketFactory());
            }
        }
    }

    private void addHeaders(Map<String, String> extraHeaders, HttpURLConnection connection) {
        connection.setRequestProperty("Content-Type", contentType);
        for (Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        for (Entry<String, String> entry : extraHeaders.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }

    /**
     * @return the serviceUrl
     */
    public URL getServiceUrl() throws MalformedURLException {
        if (!StringUtils.isEmpty(serviceId)) {
            Assert.notNull(getLoadBalancerClient(), "loadBalancerClient is null,need ribbon in the classpath!");
            long start = System.currentTimeMillis();
            ServiceInstance serviceInstance = loadBalancerClient.choose(serviceId);
            long take = System.currentTimeMillis() - start;
            Assert.notNull(serviceInstance, "can't find service of [" + serviceId + "],eureka is on the right way ?");
            URL url = new URL(serviceInstance.getUri().toURL(), MapUtils.getString(serviceInstance.getMetadata(), "context-path", "") + com.netfinworks.cloud.rpc.Util.addPrefixAndDistinct(servicePath));
            logger.debug("got lb url={},take {}ms", url.toString(), take);
            return url;
        }
        return serviceUrl;
    }

    public LoadBalancerClient getLoadBalancerClient() {
        if (loadBalancerClient == null && applicationContext != null) {
            loadBalancerClient = applicationContext.getBean(LoadBalancerClient.class);
            logger.warn("############loadBalancerClient is null,get bean from applicationContext,{}", loadBalancerClient);
        }
        return loadBalancerClient;
    }

    public void setLoadBalancerClient(LoadBalancerClient loadBalancerClient) {
        this.loadBalancerClient = loadBalancerClient;
    }

    /**
     * @param serviceUrl the serviceUrl to set
     */

    public void setServiceUrl(URL serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    /**
     * @return the connectionProxy
     */
    public Proxy getConnectionProxy() {
        return connectionProxy;
    }

    /**
     * @param connectionProxy the connectionProxy to set
     */
    public void setConnectionProxy(Proxy connectionProxy) {
        this.connectionProxy = connectionProxy;
    }

    /**
     * @return the connectionTimeoutMillis
     */
    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    /**
     * @param connectionTimeoutMillis the connectionTimeoutMillis to set
     */
    public void setConnectionTimeoutMillis(int connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }

    /**
     * @return the readTimeoutMillis
     */
    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    /**
     * @param readTimeoutMillis the readTimeoutMillis to set
     */
    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    /**
     * @return the headers
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * @param headers the headers to set
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers.clear();
        this.headers.putAll(headers);
    }

    /**
     * @param sslContext the sslContext to set
     */
    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    /**
     * @param hostNameVerifier the hostNameVerifier to set
     */
    public void setHostNameVerifier(HostnameVerifier hostNameVerifier) {
        this.hostNameVerifier = hostNameVerifier;
    }

    /**
     * @param contentType the contentType to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getServicePath() {
        return servicePath;
    }

    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }

    public Tracer getTracer() {
        return tracer;
    }

    public void setTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
