//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.modelcontextprotocol.server.transport;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.util.Assert;
import io.modelcontextprotocol.util.KeepAliveScheduler;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class WebMvcSseServerTransportProvider implements McpServerTransportProvider {
    private static final Logger logger = LoggerFactory.getLogger(WebMvcSseServerTransportProvider.class);
    public static final String MESSAGE_EVENT_TYPE = "message";
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";
    public static final String SESSION_ID = "sessionId";
    public static final String DEFAULT_SSE_ENDPOINT = "/sse";
    private final McpJsonMapper jsonMapper;
    private final String messageEndpoint;
    private final String sseEndpoint;
    private final String baseUrl;
    private final RouterFunction<ServerResponse> routerFunction;
    private McpServerSession.Factory sessionFactory;
    private final ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap();
    private McpTransportContextExtractor<ServerRequest> contextExtractor;
    private volatile boolean isClosing = false;
    private KeepAliveScheduler keepAliveScheduler;

    private WebMvcSseServerTransportProvider(McpJsonMapper jsonMapper, String baseUrl, String messageEndpoint, String sseEndpoint, Duration keepAliveInterval, McpTransportContextExtractor<ServerRequest> contextExtractor) {
        Assert.notNull(jsonMapper, "McpJsonMapper must not be null");
        Assert.notNull(baseUrl, "Message base URL must not be null");
        Assert.notNull(messageEndpoint, "Message endpoint must not be null");
        Assert.notNull(sseEndpoint, "SSE endpoint must not be null");
        Assert.notNull(contextExtractor, "Context extractor must not be null");
        this.jsonMapper = jsonMapper;
        this.baseUrl = baseUrl;
        this.messageEndpoint = messageEndpoint;
        this.sseEndpoint = sseEndpoint;
        this.contextExtractor = contextExtractor;
        this.routerFunction = RouterFunctions.route().GET(this.sseEndpoint, this::handleSseConnection).POST(this.messageEndpoint, this::handleMessage).build();
        if (keepAliveInterval != null) {
            this.keepAliveScheduler = KeepAliveScheduler.builder(() -> this.isClosing ? Flux.empty() : Flux.fromIterable(this.sessions.values())).initialDelay(keepAliveInterval).interval(keepAliveInterval).build();
            this.keepAliveScheduler.start();
        }

    }

    public List<String> protocolVersions() {
        return List.of("2024-11-05");
    }

    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Mono<Void> notifyClients(String method, Object params) {
        if (this.sessions.isEmpty()) {
            logger.debug("No active sessions to broadcast message to");
            return Mono.empty();
        } else {
            logger.debug("Attempting to broadcast message to {} active sessions", this.sessions.size());
            return Flux.fromIterable(this.sessions.values()).flatMap((session) -> session.sendNotification(method, params).doOnError((e) -> logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage())).onErrorComplete()).then();
        }
    }

    public Mono<Void> closeGracefully() {
        return Flux.fromIterable(this.sessions.values()).doFirst(() -> {
            this.isClosing = true;
            logger.debug("Initiating graceful shutdown with {} active sessions", this.sessions.size());
        }).flatMap(McpServerSession::closeGracefully).then().doOnSuccess((v) -> {
            logger.debug("Graceful shutdown completed");
            this.sessions.clear();
            if (this.keepAliveScheduler != null) {
                this.keepAliveScheduler.shutdown();
            }

        });
    }

    public RouterFunction<ServerResponse> getRouterFunction() {
        return this.routerFunction;
    }

    private ServerResponse handleSseConnection(ServerRequest request) {
        return this.isClosing ? ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down") : ServerResponse.sse((sseBuilder) -> {
            WebMvcMcpSessionTransport sessionTransport = new WebMvcMcpSessionTransport(sseBuilder);
            McpServerSession session = this.sessionFactory.create(sessionTransport);
            String sessionId = session.getId();
            logger.debug("Creating new SSE connection for session: {}", sessionId);
            sseBuilder.onComplete(() -> {
                logger.debug("SSE connection completed for session: {}", sessionId);
                this.sessions.remove(sessionId);
            });
            sseBuilder.onTimeout(() -> {
                logger.debug("SSE connection timed out for session: {}", sessionId);
                this.sessions.remove(sessionId);
            });
            this.sessions.put(sessionId, session);

            try {
                sseBuilder.event("endpoint").data(this.buildEndpointUrl(sessionId));
            } catch (Exception e) {
                logger.error("Failed to send initial endpoint event: {}", e.getMessage());
                this.sessions.remove(sessionId);
                sseBuilder.error(e);
            }

        }, Duration.ZERO);
    }

    private String buildEndpointUrl(String sessionId) {
        return UriComponentsBuilder.fromUriString(this.baseUrl).path(this.messageEndpoint).queryParam("sessionId", new Object[]{sessionId}).build().toUriString();
    }

    private ServerResponse handleMessage(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        } else if (request.param("sessionId").isEmpty()) {
            return ServerResponse.badRequest().body(new McpError("Session ID missing in message endpoint"));
        } else {
            String sessionId = (String)request.param("sessionId").get();
            McpServerSession session = (McpServerSession)this.sessions.get(sessionId);
            if (session == null) {
                return ServerResponse.status(HttpStatus.NOT_FOUND).body(new McpError("Session not found: " + sessionId));
            } else {
                try {
                    McpTransportContext transportContext = this.contextExtractor.extract(request);
                    String body = (String)request.body(String.class);
                    McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(this.jsonMapper, body);
                    session.handle(message).contextWrite((ctx) -> ctx.put("MCP_TRANSPORT_CONTEXT", transportContext)).block();
                    return ServerResponse.ok().build();
                } catch (IOException | IllegalArgumentException e) {
                    logger.error("Failed to deserialize message: {}", ((Exception)e).getMessage());
                    return ServerResponse.badRequest().body(new McpError("Invalid message format"));
                } catch (Exception e) {
                    logger.error("Error handling message: {}", e.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new McpError(e.getMessage()));
                }
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private class WebMvcMcpSessionTransport implements McpServerTransport {
        private final ServerResponse.SseBuilder sseBuilder;
        private final ReentrantLock sseBuilderLock = new ReentrantLock();

        WebMvcMcpSessionTransport(ServerResponse.SseBuilder sseBuilder) {
            this.sseBuilder = sseBuilder;
        }

        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return Mono.fromRunnable(() -> {
                this.sseBuilderLock.lock();

                try {
                    String jsonText = WebMvcSseServerTransportProvider.this.jsonMapper.writeValueAsString(message);
                    this.sseBuilder.event("message").data(jsonText);
                } catch (Exception e) {
                    WebMvcSseServerTransportProvider.logger.error("Failed to send message: {}", e.getMessage());
                    this.sseBuilder.error(e);
                } finally {
                    this.sseBuilderLock.unlock();
                }

            });
        }

        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            return (T)WebMvcSseServerTransportProvider.this.jsonMapper.convertValue(data, typeRef);
        }

        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(() -> {
                this.sseBuilderLock.lock();

                try {
                    this.sseBuilder.complete();
                } catch (Exception e) {
                    WebMvcSseServerTransportProvider.logger.warn("Failed to complete SSE builder: {}", e.getMessage());
                } finally {
                    this.sseBuilderLock.unlock();
                }

            });
        }

        public void close() {
            this.sseBuilderLock.lock();

            try {
                this.sseBuilder.complete();
            } catch (Exception e) {
                WebMvcSseServerTransportProvider.logger.warn("Failed to complete SSE builder: {}", e.getMessage());
            } finally {
                this.sseBuilderLock.unlock();
            }

        }
    }

    public static class Builder {
        private McpJsonMapper jsonMapper;
        private String baseUrl = "";
        private String messageEndpoint;
        private String sseEndpoint = "/sse";
        private Duration keepAliveInterval;
        private McpTransportContextExtractor<ServerRequest> contextExtractor = (serverRequest) -> McpTransportContext.EMPTY;

        public Builder jsonMapper(McpJsonMapper jsonMapper) {
            Assert.notNull(jsonMapper, "McpJsonMapper must not be null");
            this.jsonMapper = jsonMapper;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            Assert.notNull(baseUrl, "Base URL must not be null");
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder messageEndpoint(String messageEndpoint) {
            Assert.hasText(messageEndpoint, "Message endpoint must not be empty");
            this.messageEndpoint = messageEndpoint;
            return this;
        }

        public Builder sseEndpoint(String sseEndpoint) {
            Assert.hasText(sseEndpoint, "SSE endpoint must not be empty");
            this.sseEndpoint = sseEndpoint;
            return this;
        }

        public Builder keepAliveInterval(Duration keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        public Builder contextExtractor(McpTransportContextExtractor<ServerRequest> contextExtractor) {
            Assert.notNull(contextExtractor, "contextExtractor must not be null");
            this.contextExtractor = contextExtractor;
            return this;
        }

        public WebMvcSseServerTransportProvider build() {
            if (this.messageEndpoint == null) {
                throw new IllegalStateException("MessageEndpoint must be set");
            } else {
                return new WebMvcSseServerTransportProvider(this.jsonMapper == null ? McpJsonMapper.getDefault() : this.jsonMapper, this.baseUrl, this.messageEndpoint, this.sseEndpoint, this.keepAliveInterval, this.contextExtractor);
            }
        }
    }
}
