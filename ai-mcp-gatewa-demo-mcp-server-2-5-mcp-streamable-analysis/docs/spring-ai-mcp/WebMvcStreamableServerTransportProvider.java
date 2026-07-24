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
import io.modelcontextprotocol.spec.McpStreamableServerSession;
import io.modelcontextprotocol.spec.McpStreamableServerTransport;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
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
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class WebMvcStreamableServerTransportProvider implements McpStreamableServerTransportProvider {
    private static final Logger logger = LoggerFactory.getLogger(WebMvcStreamableServerTransportProvider.class);
    public static final String MESSAGE_EVENT_TYPE = "message";
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";
    public static final String DEFAULT_BASE_URL = "";
    private final String mcpEndpoint;
    private final boolean disallowDelete;
    private final McpJsonMapper jsonMapper;
    private final RouterFunction<ServerResponse> routerFunction;
    private McpStreamableServerSession.Factory sessionFactory;
    private final ConcurrentHashMap<String, McpStreamableServerSession> sessions = new ConcurrentHashMap();
    private McpTransportContextExtractor<ServerRequest> contextExtractor;
    private volatile boolean isClosing = false;
    private KeepAliveScheduler keepAliveScheduler;

    private WebMvcStreamableServerTransportProvider(McpJsonMapper jsonMapper, String mcpEndpoint, boolean disallowDelete, McpTransportContextExtractor<ServerRequest> contextExtractor, Duration keepAliveInterval) {
        Assert.notNull(jsonMapper, "McpJsonMapper must not be null");
        Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
        Assert.notNull(contextExtractor, "McpTransportContextExtractor must not be null");
        this.jsonMapper = jsonMapper;
        this.mcpEndpoint = mcpEndpoint;
        this.disallowDelete = disallowDelete;
        this.contextExtractor = contextExtractor;
        this.routerFunction = RouterFunctions.route().GET(this.mcpEndpoint, this::handleGet).POST(this.mcpEndpoint, this::handlePost).DELETE(this.mcpEndpoint, this::handleDelete).build();
        if (keepAliveInterval != null) {
            this.keepAliveScheduler = KeepAliveScheduler.builder(() -> this.isClosing ? Flux.empty() : Flux.fromIterable(this.sessions.values())).initialDelay(keepAliveInterval).interval(keepAliveInterval).build();
            this.keepAliveScheduler.start();
        }

    }

    public List<String> protocolVersions() {
        return List.of("2024-11-05", "2025-03-26", "2025-06-18");
    }

    public void setSessionFactory(McpStreamableServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Mono<Void> notifyClients(String method, Object params) {
        if (this.sessions.isEmpty()) {
            logger.debug("No active sessions to broadcast message to");
            return Mono.empty();
        } else {
            logger.debug("Attempting to broadcast message to {} active sessions", this.sessions.size());
            return Mono.fromRunnable(() -> this.sessions.values().parallelStream().forEach((session) -> {
                try {
                    session.sendNotification(method, params).block();
                } catch (Exception e) {
                    logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                }

            }));
        }
    }

    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            this.isClosing = true;
            logger.debug("Initiating graceful shutdown with {} active sessions", this.sessions.size());
            this.sessions.values().parallelStream().forEach((session) -> {
                try {
                    session.closeGracefully().block();
                } catch (Exception e) {
                    logger.error("Failed to close session {}: {}", session.getId(), e.getMessage());
                }

            });
            this.sessions.clear();
            logger.debug("Graceful shutdown completed");
        }).then().doOnSuccess((v) -> {
            if (this.keepAliveScheduler != null) {
                this.keepAliveScheduler.shutdown();
            }

        });
    }

    public RouterFunction<ServerResponse> getRouterFunction() {
        return this.routerFunction;
    }

    private ServerResponse handleGet(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        } else {
            List<MediaType> acceptHeaders = request.headers().asHttpHeaders().getAccept();
            if (!acceptHeaders.contains(MediaType.TEXT_EVENT_STREAM)) {
                return ServerResponse.badRequest().body("Invalid Accept header. Expected TEXT_EVENT_STREAM");
            } else {
                McpTransportContext transportContext = this.contextExtractor.extract(request);
                if (request.headers().header("Mcp-Session-Id").isEmpty()) {
                    return ServerResponse.badRequest().body("Session ID required in mcp-session-id header");
                } else {
                    String sessionId = request.headers().asHttpHeaders().getFirst("Mcp-Session-Id");
                    McpStreamableServerSession session = (McpStreamableServerSession)this.sessions.get(sessionId);
                    if (session == null) {
                        return ServerResponse.notFound().build();
                    } else {
                        logger.debug("Handling GET request for session: {}", sessionId);

                        try {
                            return ServerResponse.sse((sseBuilder) -> {
                                sseBuilder.onTimeout(() -> logger.debug("SSE connection timed out for session: {}", sessionId));
                                WebMvcStreamableMcpSessionTransport sessionTransport = new WebMvcStreamableMcpSessionTransport(sessionId, sseBuilder);
                                if (!request.headers().header("Last-Event-ID").isEmpty()) {
                                    String lastId = request.headers().asHttpHeaders().getFirst("Last-Event-ID");

                                    try {
                                        session.replay(lastId).contextWrite((ctx) -> ctx.put("MCP_TRANSPORT_CONTEXT", transportContext)).toIterable().forEach((message) -> {
                                            try {
                                                sessionTransport.sendMessage(message).contextWrite((ctx) -> ctx.put("MCP_TRANSPORT_CONTEXT", transportContext)).block();
                                            } catch (Exception e) {
                                                logger.error("Failed to replay message: {}", e.getMessage());
                                                sseBuilder.error(e);
                                            }

                                        });
                                    } catch (Exception e) {
                                        logger.error("Failed to replay messages: {}", e.getMessage());
                                        sseBuilder.error(e);
                                    }
                                } else {
                                    McpStreamableServerSession.McpStreamableServerSessionStream listeningStream = session.listeningStream(sessionTransport);
                                    sseBuilder.onComplete(() -> {
                                        logger.debug("SSE connection completed for session: {}", sessionId);
                                        listeningStream.close();
                                    });
                                }

                            }, Duration.ZERO);
                        } catch (Exception e) {
                            logger.error("Failed to handle GET request for session {}: {}", sessionId, e.getMessage());
                            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        }
                    }
                }
            }
        }
    }

    private ServerResponse handlePost(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        } else {
            List<MediaType> acceptHeaders = request.headers().asHttpHeaders().getAccept();
            if (acceptHeaders.contains(MediaType.TEXT_EVENT_STREAM) && acceptHeaders.contains(MediaType.APPLICATION_JSON)) {
                McpTransportContext transportContext = this.contextExtractor.extract(request);

                try {
                    String body = (String)request.body(String.class);
                    McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(this.jsonMapper, body);
                    if (message instanceof McpSchema.JSONRPCRequest) {
                        McpSchema.JSONRPCRequest jsonrpcRequest = (McpSchema.JSONRPCRequest)message;
                        if (jsonrpcRequest.method().equals("initialize")) {
                            McpSchema.InitializeRequest initializeRequest = (McpSchema.InitializeRequest)this.jsonMapper.convertValue(jsonrpcRequest.params(), new TypeRef<McpSchema.InitializeRequest>() {
                            });
                            McpStreamableServerSession.McpStreamableServerSessionInit init = this.sessionFactory.startSession(initializeRequest);
                            this.sessions.put(init.session().getId(), init.session());

                            try {
                                McpSchema.InitializeResult initResult = (McpSchema.InitializeResult)init.initResult().block();
                                return ((ServerResponse.BodyBuilder)ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).header("Mcp-Session-Id", new String[]{init.session().getId()})).body(new McpSchema.JSONRPCResponse("2.0", jsonrpcRequest.id(), initResult, (McpSchema.JSONRPCResponse.JSONRPCError)null));
                            } catch (Exception e) {
                                logger.error("Failed to initialize session: {}", e.getMessage());
                                return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new McpError(e.getMessage()));
                            }
                        }
                    }

                    if (request.headers().header("Mcp-Session-Id").isEmpty()) {
                        return ServerResponse.badRequest().body(new McpError("Session ID missing"));
                    } else {
                        String sessionId = request.headers().asHttpHeaders().getFirst("Mcp-Session-Id");
                        McpStreamableServerSession session = (McpStreamableServerSession)this.sessions.get(sessionId);
                        if (session == null) {
                            return ServerResponse.status(HttpStatus.NOT_FOUND).body(new McpError("Session not found: " + sessionId));
                        } else if (message instanceof McpSchema.JSONRPCResponse) {
                            McpSchema.JSONRPCResponse jsonrpcResponse = (McpSchema.JSONRPCResponse)message;
                            session.accept(jsonrpcResponse).contextWrite((ctx) -> ctx.put("MCP_TRANSPORT_CONTEXT", transportContext)).block();
                            return ServerResponse.accepted().build();
                        } else if (message instanceof McpSchema.JSONRPCNotification) {
                            McpSchema.JSONRPCNotification jsonrpcNotification = (McpSchema.JSONRPCNotification)message;
                            session.accept(jsonrpcNotification).contextWrite((ctx) -> ctx.put("MCP_TRANSPORT_CONTEXT", transportContext)).block();
                            return ServerResponse.accepted().build();
                        } else if (message instanceof McpSchema.JSONRPCRequest) {
                            McpSchema.JSONRPCRequest jsonrpcRequest = (McpSchema.JSONRPCRequest)message;
                            return ServerResponse.sse((sseBuilder) -> {
                                sseBuilder.onComplete(() -> logger.debug("Request response stream completed for session: {}", sessionId));
                                sseBuilder.onTimeout(() -> logger.debug("Request response stream timed out for session: {}", sessionId));
                                WebMvcStreamableMcpSessionTransport sessionTransport = new WebMvcStreamableMcpSessionTransport(sessionId, sseBuilder);

                                try {
                                    session.responseStream(jsonrpcRequest, sessionTransport).contextWrite((ctx) -> ctx.put("MCP_TRANSPORT_CONTEXT", transportContext)).block();
                                } catch (Exception e) {
                                    logger.error("Failed to handle request stream: {}", e.getMessage());
                                    sseBuilder.error(e);
                                }

                            }, Duration.ZERO);
                        } else {
                            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new McpError("Unknown message type"));
                        }
                    }
                } catch (IOException | IllegalArgumentException e) {
                    logger.error("Failed to deserialize message: {}", ((Exception)e).getMessage());
                    return ServerResponse.badRequest().body(new McpError("Invalid message format"));
                } catch (Exception e) {
                    logger.error("Error handling message: {}", e.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new McpError(e.getMessage()));
                }
            } else {
                return ServerResponse.badRequest().body(new McpError("Invalid Accept headers. Expected TEXT_EVENT_STREAM and APPLICATION_JSON"));
            }
        }
    }

    private ServerResponse handleDelete(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        } else if (this.disallowDelete) {
            return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        } else {
            McpTransportContext transportContext = this.contextExtractor.extract(request);
            if (request.headers().header("Mcp-Session-Id").isEmpty()) {
                return ServerResponse.badRequest().body("Session ID required in mcp-session-id header");
            } else {
                String sessionId = request.headers().asHttpHeaders().getFirst("Mcp-Session-Id");
                McpStreamableServerSession session = (McpStreamableServerSession)this.sessions.get(sessionId);
                if (session == null) {
                    return ServerResponse.notFound().build();
                } else {
                    try {
                        session.delete().contextWrite((ctx) -> ctx.put("MCP_TRANSPORT_CONTEXT", transportContext)).block();
                        this.sessions.remove(sessionId);
                        return ServerResponse.ok().build();
                    } catch (Exception e) {
                        logger.error("Failed to delete session {}: {}", sessionId, e.getMessage());
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new McpError(e.getMessage()));
                    }
                }
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private class WebMvcStreamableMcpSessionTransport implements McpStreamableServerTransport {
        private final String sessionId;
        private final ServerResponse.SseBuilder sseBuilder;
        private final ReentrantLock lock = new ReentrantLock();
        private volatile boolean closed = false;

        WebMvcStreamableMcpSessionTransport(String sessionId, ServerResponse.SseBuilder sseBuilder) {
            this.sessionId = sessionId;
            this.sseBuilder = sseBuilder;
            WebMvcStreamableServerTransportProvider.logger.debug("Streamable session transport {} initialized with SSE builder", sessionId);
        }

        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return this.sendMessage(message, (String)null);
        }

        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId) {
            return Mono.fromRunnable(() -> {
                if (this.closed) {
                    WebMvcStreamableServerTransportProvider.logger.debug("Attempted to send message to closed session: {}", this.sessionId);
                } else {
                    this.lock.lock();

                    try {
                        if (!this.closed) {
                            String jsonText = WebMvcStreamableServerTransportProvider.this.jsonMapper.writeValueAsString(message);
                            this.sseBuilder.id(messageId != null ? messageId : this.sessionId).event("message").data(jsonText);
                            WebMvcStreamableServerTransportProvider.logger.debug("Message sent to session {} with ID {}", this.sessionId, messageId);
                            return;
                        }

                        WebMvcStreamableServerTransportProvider.logger.debug("Session {} was closed during message send attempt", this.sessionId);
                    } catch (Exception var10) {
                        Exception e = var10;
                        WebMvcStreamableServerTransportProvider.logger.error("Failed to send message to session {}: {}", this.sessionId, var10.getMessage());

                        try {
                            this.sseBuilder.error(e);
                        } catch (Exception errorException) {
                            WebMvcStreamableServerTransportProvider.logger.error("Failed to send error to SSE builder for session {}: {}", this.sessionId, errorException.getMessage());
                        }

                        return;
                    } finally {
                        this.lock.unlock();
                    }

                }
            });
        }

        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            return (T)WebMvcStreamableServerTransportProvider.this.jsonMapper.convertValue(data, typeRef);
        }

        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(() -> this.close());
        }

        public void close() {
            this.lock.lock();

            try {
                if (!this.closed) {
                    this.closed = true;
                    this.sseBuilder.complete();
                    WebMvcStreamableServerTransportProvider.logger.debug("Successfully completed SSE builder for session {}", this.sessionId);
                    return;
                }

                WebMvcStreamableServerTransportProvider.logger.debug("Session transport {} already closed", this.sessionId);
            } catch (Exception e) {
                WebMvcStreamableServerTransportProvider.logger.warn("Failed to complete SSE builder for session {}: {}", this.sessionId, e.getMessage());
                return;
            } finally {
                this.lock.unlock();
            }

        }
    }

    public static class Builder {
        private McpJsonMapper jsonMapper;
        private String mcpEndpoint = "/mcp";
        private boolean disallowDelete = false;
        private McpTransportContextExtractor<ServerRequest> contextExtractor = (serverRequest) -> McpTransportContext.EMPTY;
        private Duration keepAliveInterval;

        public Builder jsonMapper(McpJsonMapper jsonMapper) {
            Assert.notNull(jsonMapper, "McpJsonMapper must not be null");
            this.jsonMapper = jsonMapper;
            return this;
        }

        public Builder mcpEndpoint(String mcpEndpoint) {
            Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
            this.mcpEndpoint = mcpEndpoint;
            return this;
        }

        public Builder disallowDelete(boolean disallowDelete) {
            this.disallowDelete = disallowDelete;
            return this;
        }

        public Builder contextExtractor(McpTransportContextExtractor<ServerRequest> contextExtractor) {
            Assert.notNull(contextExtractor, "contextExtractor must not be null");
            this.contextExtractor = contextExtractor;
            return this;
        }

        public Builder keepAliveInterval(Duration keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        public WebMvcStreamableServerTransportProvider build() {
            Assert.notNull(this.mcpEndpoint, "MCP endpoint must be set");
            return new WebMvcStreamableServerTransportProvider(this.jsonMapper == null ? McpJsonMapper.getDefault() : this.jsonMapper, this.mcpEndpoint, this.disallowDelete, this.contextExtractor, this.keepAliveInterval);
        }
    }
}
