//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.modelcontextprotocol.client.transport;

import io.modelcontextprotocol.client.transport.customizer.McpAsyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.ClosedMcpTransportSession;
import io.modelcontextprotocol.spec.DefaultMcpTransportSession;
import io.modelcontextprotocol.spec.DefaultMcpTransportStream;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpTransportException;
import io.modelcontextprotocol.spec.McpTransportSession;
import io.modelcontextprotocol.spec.McpTransportSessionNotFoundException;
import io.modelcontextprotocol.spec.McpTransportStream;
import io.modelcontextprotocol.util.Assert;
import io.modelcontextprotocol.util.Utils;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class HttpClientStreamableHttpTransport implements McpClientTransport {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientStreamableHttpTransport.class);
    private static final String DEFAULT_ENDPOINT = "/mcp";
    private final HttpClient httpClient;
    private final HttpRequest.Builder requestBuilder;
    private static final String MESSAGE_EVENT_TYPE = "message";
    private static final String APPLICATION_JSON = "application/json";
    private static final String TEXT_EVENT_STREAM = "text/event-stream";
    public static int NOT_FOUND = 404;
    public static int METHOD_NOT_ALLOWED = 405;
    public static int BAD_REQUEST = 400;
    private final McpJsonMapper jsonMapper;
    private final URI baseUri;
    private final String endpoint;
    private final boolean openConnectionOnStartup;
    private final boolean resumableStreams;
    private final McpAsyncHttpClientRequestCustomizer httpRequestCustomizer;
    private final AtomicReference<McpTransportSession<Disposable>> activeSession = new AtomicReference();
    private final AtomicReference<Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>>> handler = new AtomicReference();
    private final AtomicReference<Consumer<Throwable>> exceptionHandler = new AtomicReference();
    private final List<String> supportedProtocolVersions;
    private final String latestSupportedProtocolVersion;

    private HttpClientStreamableHttpTransport(McpJsonMapper jsonMapper, HttpClient httpClient, HttpRequest.Builder requestBuilder, String baseUri, String endpoint, boolean resumableStreams, boolean openConnectionOnStartup, McpAsyncHttpClientRequestCustomizer httpRequestCustomizer, List<String> supportedProtocolVersions) {
        this.jsonMapper = jsonMapper;
        this.httpClient = httpClient;
        this.requestBuilder = requestBuilder;
        this.baseUri = URI.create(baseUri);
        this.endpoint = endpoint;
        this.resumableStreams = resumableStreams;
        this.openConnectionOnStartup = openConnectionOnStartup;
        this.activeSession.set(this.createTransportSession());
        this.httpRequestCustomizer = httpRequestCustomizer;
        this.supportedProtocolVersions = Collections.unmodifiableList(supportedProtocolVersions);
        this.latestSupportedProtocolVersion = (String)this.supportedProtocolVersions.stream().sorted(Comparator.reverseOrder()).findFirst().get();
    }

    public List<String> protocolVersions() {
        return this.supportedProtocolVersions;
    }

    public static Builder builder(String baseUri) {
        return new Builder(baseUri);
    }

    public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
        return Mono.deferContextual((ctx) -> {
            this.handler.set(handler);
            if (this.openConnectionOnStartup) {
                logger.debug("Eagerly opening connection on startup");
                return this.reconnect((McpTransportStream)null).onErrorComplete((t) -> {
                    logger.warn("Eager connect failed ", t);
                    return true;
                }).then();
            } else {
                return Mono.empty();
            }
        });
    }

    private McpTransportSession<Disposable> createTransportSession() {
        Function<String, Publisher<Void>> onClose = (sessionId) -> (Publisher)(sessionId == null ? Mono.empty() : this.createDelete(sessionId));
        return new DefaultMcpTransportSession(onClose);
    }

    private McpTransportSession<Disposable> createClosedSession(McpTransportSession<Disposable> existingSession) {
        String existingSessionId = (String)Optional.ofNullable(existingSession).filter((session) -> !(session instanceof ClosedMcpTransportSession)).flatMap(McpTransportSession::sessionId).orElse((Object)null);
        return new ClosedMcpTransportSession(existingSessionId);
    }

    private Publisher<Void> createDelete(String sessionId) {
        URI uri = Utils.resolveUri(this.baseUri, this.endpoint);
        return Mono.deferContextual((ctx) -> {
            HttpRequest.Builder builder = this.requestBuilder.copy().uri(uri).header("Cache-Control", "no-cache").header("Mcp-Session-Id", sessionId).header("MCP-Protocol-Version", (String)ctx.getOrDefault("io.modelcontextprotocol.client.negotiated-protocol-version", this.latestSupportedProtocolVersion)).DELETE();
            McpTransportContext transportContext = (McpTransportContext)ctx.getOrDefault("MCP_TRANSPORT_CONTEXT", McpTransportContext.EMPTY);
            return Mono.from(this.httpRequestCustomizer.customize(builder, "DELETE", uri, (String)null, transportContext));
        }).flatMap((requestBuilder) -> {
            HttpRequest request = requestBuilder.build();
            return Mono.fromFuture(() -> this.httpClient.sendAsync(request, BodyHandlers.ofString()));
        }).then();
    }

    public void setExceptionHandler(Consumer<Throwable> handler) {
        logger.debug("Exception handler registered");
        this.exceptionHandler.set(handler);
    }

    private void handleException(Throwable t) {
        logger.debug("Handling exception for session {}", sessionIdOrPlaceholder((McpTransportSession)this.activeSession.get()), t);
        if (t instanceof McpTransportSessionNotFoundException) {
            McpTransportSession<?> invalidSession = (McpTransportSession)this.activeSession.getAndSet(this.createTransportSession());
            logger.warn("Server does not recognize session {}. Invalidating.", invalidSession.sessionId());
            invalidSession.close();
        }

        Consumer<Throwable> handler = (Consumer)this.exceptionHandler.get();
        if (handler != null) {
            handler.accept(t);
        }

    }

    public Mono<Void> closeGracefully() {
        return Mono.defer(() -> {
            logger.debug("Graceful close triggered");
            McpTransportSession<Disposable> currentSession = (McpTransportSession)this.activeSession.getAndUpdate(this::createClosedSession);
            return currentSession != null ? Mono.from(currentSession.closeGracefully()) : Mono.empty();
        });
    }

    private Mono<Disposable> reconnect(McpTransportStream<Disposable> stream) {
        return Mono.deferContextual((ctx) -> {
            if (stream != null) {
                logger.debug("Reconnecting stream {} with lastId {}", stream.streamId(), stream.lastId());
            } else {
                logger.debug("Reconnecting with no prior stream");
            }

            AtomicReference<Disposable> disposableRef = new AtomicReference();
            McpTransportSession<Disposable> transportSession = (McpTransportSession)this.activeSession.get();
            URI uri = Utils.resolveUri(this.baseUri, this.endpoint);
            Disposable connection = Mono.deferContextual((connectionCtx) -> {
                HttpRequest.Builder requestBuilder = this.requestBuilder.copy();
                if (transportSession != null && transportSession.sessionId().isPresent()) {
                    requestBuilder = requestBuilder.header("Mcp-Session-Id", (String)transportSession.sessionId().get());
                }

                if (stream != null && stream.lastId().isPresent()) {
                    requestBuilder = requestBuilder.header("Last-Event-ID", (String)stream.lastId().get());
                }

                HttpRequest.Builder builder = requestBuilder.uri(uri).header("Accept", "text/event-stream").header("Cache-Control", "no-cache").header("MCP-Protocol-Version", (String)connectionCtx.getOrDefault("io.modelcontextprotocol.client.negotiated-protocol-version", this.latestSupportedProtocolVersion)).GET();
                McpTransportContext transportContext = (McpTransportContext)connectionCtx.getOrDefault("MCP_TRANSPORT_CONTEXT", McpTransportContext.EMPTY);
                return Mono.from(this.httpRequestCustomizer.customize(builder, "GET", uri, (String)null, transportContext));
            }).flatMapMany((requestBuilder) -> Flux.create((sseSink) -> this.httpClient.sendAsync(requestBuilder.build(), (responseInfo) -> ResponseSubscribers.sseToBodySubscriber(responseInfo, sseSink)).whenComplete((response, throwable) -> {
                if (throwable != null) {
                    sseSink.error(throwable);
                } else {
                    logger.debug("SSE connection established successfully");
                }

            })).map((responseEvent) -> (ResponseSubscribers.SseResponseEvent)responseEvent).flatMap((responseEvent) -> {
                int statusCode = responseEvent.responseInfo().statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    if ("message".equals(responseEvent.sseEvent().event())) {
                        try {
                            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(this.jsonMapper, responseEvent.sseEvent().data());
                            Tuple2<Optional<String>, Iterable<McpSchema.JSONRPCMessage>> idWithMessages = Tuples.of(Optional.ofNullable(responseEvent.sseEvent().id()), List.of(message));
                            McpTransportStream<Disposable> sessionStream = (McpTransportStream<Disposable>)(stream != null ? stream : new DefaultMcpTransportStream(this.resumableStreams, this::reconnect));
                            logger.debug("Connected stream {}", sessionStream.streamId());
                            return Flux.from(sessionStream.consumeSseStream(Flux.just(idWithMessages)));
                        } catch (IOException ioException) {
                            return Flux.error(new McpTransportException("Error parsing JSON-RPC message: " + String.valueOf(responseEvent), ioException));
                        }
                    } else {
                        logger.debug("Received SSE event with type: {}", responseEvent.sseEvent());
                        return Flux.empty();
                    }
                } else if (statusCode == METHOD_NOT_ALLOWED) {
                    logger.debug("The server does not support SSE streams, using request-response mode.");
                    return Flux.empty();
                } else if (statusCode == NOT_FOUND) {
                    if (transportSession != null && transportSession.sessionId().isPresent()) {
                        logger.debug("Session not found for session ID: {}", transportSession.sessionId().get());
                        String sessionIdRepresentation = sessionIdOrPlaceholder(transportSession);
                        McpTransportSessionNotFoundException exception = new McpTransportSessionNotFoundException("Session not found for session ID: " + sessionIdRepresentation);
                        return Flux.error(exception);
                    } else {
                        return Flux.error(new McpTransportException("Server Not Found. Status code:" + statusCode + ", response-event:" + String.valueOf(responseEvent)));
                    }
                } else if (statusCode == BAD_REQUEST) {
                    if (transportSession != null && transportSession.sessionId().isPresent()) {
                        String sessionIdRepresentation = sessionIdOrPlaceholder(transportSession);
                        McpTransportSessionNotFoundException exception = new McpTransportSessionNotFoundException("Session not found for session ID: " + sessionIdRepresentation);
                        return Flux.error(exception);
                    } else {
                        return Flux.error(new McpTransportException("Bad Request. Status code:" + statusCode + ", response-event:" + String.valueOf(responseEvent)));
                    }
                } else {
                    return Flux.error(new McpTransportException("Received unrecognized SSE event type: " + responseEvent.sseEvent().event()));
                }
            }).flatMap((jsonrpcMessage) -> (Publisher)((Function)this.handler.get()).apply(Mono.just(jsonrpcMessage))).onErrorMap(CompletionException.class, (t) -> t.getCause()).onErrorComplete((t) -> {
                this.handleException(t);
                return true;
            }).doFinally((s) -> {
                Disposable ref = (Disposable)disposableRef.getAndSet((Object)null);
                if (ref != null) {
                    transportSession.removeConnection(ref);
                }

            })).contextWrite(ctx).subscribe();
            disposableRef.set(connection);
            transportSession.addConnection(connection);
            return Mono.just(connection);
        });
    }

    private HttpResponse.BodyHandler<Void> toSendMessageBodySubscriber(FluxSink<ResponseSubscribers.ResponseEvent> sink) {
        HttpResponse.BodyHandler<Void> responseBodyHandler = (responseInfo) -> {
            String contentType = ((String)responseInfo.headers().firstValue("Content-Type").orElse("")).toLowerCase();
            if (contentType.contains("text/event-stream")) {
                logger.debug("Received SSE stream response, using line subscriber");
                return ResponseSubscribers.sseToBodySubscriber(responseInfo, sink);
            } else if (contentType.contains("application/json")) {
                logger.debug("Received response, using string subscriber");
                return ResponseSubscribers.aggregateBodySubscriber(responseInfo, sink);
            } else {
                logger.debug("Received Bodyless response, using discarding subscriber");
                return ResponseSubscribers.bodilessBodySubscriber(responseInfo, sink);
            }
        };
        return responseBodyHandler;
    }

    public String toString(McpSchema.JSONRPCMessage message) {
        try {
            return this.jsonMapper.writeValueAsString(message);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize JSON-RPC message", e);
        }
    }

    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage sentMessage) {
        return Mono.create((deliveredSink) -> {
            logger.debug("Sending message {}", sentMessage);
            AtomicReference<Disposable> disposableRef = new AtomicReference();
            McpTransportSession<Disposable> transportSession = (McpTransportSession)this.activeSession.get();
            URI uri = Utils.resolveUri(this.baseUri, this.endpoint);
            String jsonBody = this.toString(sentMessage);
            Disposable connection = Mono.deferContextual((ctx) -> {
                HttpRequest.Builder requestBuilder = this.requestBuilder.copy();
                if (transportSession != null && transportSession.sessionId().isPresent()) {
                    requestBuilder = requestBuilder.header("Mcp-Session-Id", (String)transportSession.sessionId().get());
                }

                HttpRequest.Builder builder = requestBuilder.uri(uri).header("Accept", "application/json, text/event-stream").header("Content-Type", "application/json").header("Cache-Control", "no-cache").header("MCP-Protocol-Version", (String)ctx.getOrDefault("io.modelcontextprotocol.client.negotiated-protocol-version", this.latestSupportedProtocolVersion)).POST(BodyPublishers.ofString(jsonBody));
                McpTransportContext transportContext = (McpTransportContext)ctx.getOrDefault("MCP_TRANSPORT_CONTEXT", McpTransportContext.EMPTY);
                return Mono.from(this.httpRequestCustomizer.customize(builder, "POST", uri, jsonBody, transportContext));
            }).flatMapMany((requestBuilder) -> Flux.create((responseEventSink) -> Mono.fromFuture(this.httpClient.sendAsync(requestBuilder.build(), this.toSendMessageBodySubscriber(responseEventSink)).whenComplete((response, throwable) -> {
                if (throwable != null) {
                    responseEventSink.error(throwable);
                } else {
                    logger.debug("SSE connection established successfully");
                }

            })).onErrorMap(CompletionException.class, (t) -> t.getCause()).onErrorComplete().subscribe())).flatMap((responseEvent) -> {
                if (transportSession.markInitialized((String)responseEvent.responseInfo().headers().firstValue("mcp-session-id").orElseGet(() -> null))) {
                    this.reconnect((McpTransportStream)null).contextWrite(deliveredSink.contextView()).subscribe();
                }

                String sessionRepresentation = sessionIdOrPlaceholder(transportSession);
                int statusCode = responseEvent.responseInfo().statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    String contentType = ((String)responseEvent.responseInfo().headers().firstValue("Content-Type").orElse("")).toLowerCase();
                    String contentLength = (String)responseEvent.responseInfo().headers().firstValue("Content-Length").orElse((Object)null);
                    if (!contentType.isBlank() && !"0".equals(contentLength)) {
                        if (contentType.contains("text/event-stream")) {
                            return Flux.just(((ResponseSubscribers.SseResponseEvent)responseEvent).sseEvent()).flatMap((sseEvent) -> {
                                try {
                                    McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(this.jsonMapper, sseEvent.data());
                                    Tuple2<Optional<String>, Iterable<McpSchema.JSONRPCMessage>> idWithMessages = Tuples.of(Optional.ofNullable(sseEvent.id()), List.of(message));
                                    McpTransportStream<Disposable> sessionStream = new DefaultMcpTransportStream(this.resumableStreams, this::reconnect);
                                    logger.debug("Connected stream {}", sessionStream.streamId());
                                    deliveredSink.success();
                                    return Flux.from(sessionStream.consumeSseStream(Flux.just(idWithMessages)));
                                } catch (IOException ioException) {
                                    return Flux.error(new McpTransportException("Error parsing JSON-RPC message: " + String.valueOf(responseEvent), ioException));
                                }
                            });
                        } else if (contentType.contains("application/json")) {
                            deliveredSink.success();
                            String data = ((ResponseSubscribers.AggregateResponseEvent)responseEvent).data();
                            if (sentMessage instanceof McpSchema.JSONRPCNotification) {
                                logger.warn("Notification: {} received non-compliant response: {}", sentMessage, Utils.hasText(data) ? data : "[empty]");
                                return Mono.empty();
                            } else {
                                try {
                                    return Mono.just(McpSchema.deserializeJsonRpcMessage(this.jsonMapper, data));
                                } catch (IOException e) {
                                    return Mono.error(new McpTransportException("Error deserializing JSON-RPC message: " + String.valueOf(responseEvent), e));
                                }
                            }
                        } else {
                            logger.warn("Unknown media type {} returned for POST in session {}", contentType, sessionRepresentation);
                            return Flux.error(new RuntimeException("Unknown media type returned: " + contentType));
                        }
                    } else {
                        logger.debug("No body returned for POST in session {}", sessionRepresentation);
                        deliveredSink.success();
                        return Flux.empty();
                    }
                } else if (statusCode == NOT_FOUND) {
                    if (transportSession != null && transportSession.sessionId().isPresent()) {
                        logger.debug("Session not found for session ID: {}", transportSession.sessionId().get());
                        McpTransportSessionNotFoundException exception = new McpTransportSessionNotFoundException("Session not found for session ID: " + sessionRepresentation);
                        return Flux.error(exception);
                    } else {
                        return Flux.error(new McpTransportException("Server Not Found. Status code:" + statusCode + ", response-event:" + String.valueOf(responseEvent)));
                    }
                } else if (statusCode == BAD_REQUEST) {
                    if (transportSession != null && transportSession.sessionId().isPresent()) {
                        McpTransportSessionNotFoundException exception = new McpTransportSessionNotFoundException("Session not found for session ID: " + sessionRepresentation);
                        return Flux.error(exception);
                    } else {
                        return Flux.error(new McpTransportException("Bad Request. Status code:" + statusCode + ", response-event:" + String.valueOf(responseEvent)));
                    }
                } else {
                    return Flux.error(new RuntimeException("Failed to send message: " + String.valueOf(responseEvent)));
                }
            }).flatMap((jsonRpcMessage) -> (Publisher)((Function)this.handler.get()).apply(Mono.just(jsonRpcMessage))).onErrorMap(CompletionException.class, (t) -> t.getCause()).onErrorComplete((t) -> {
                this.handleException(t);
                deliveredSink.error(t);
                return true;
            }).doFinally((s) -> {
                logger.debug("SendMessage finally: {}", s);
                Disposable ref = (Disposable)disposableRef.getAndSet((Object)null);
                if (ref != null) {
                    transportSession.removeConnection(ref);
                }

            }).contextWrite(deliveredSink.contextView()).subscribe();
            disposableRef.set(connection);
            transportSession.addConnection(connection);
        });
    }

    private static String sessionIdOrPlaceholder(McpTransportSession<?> transportSession) {
        return (String)transportSession.sessionId().orElse("[missing_session_id]");
    }

    public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
        return (T)this.jsonMapper.convertValue(data, typeRef);
    }

    public static class Builder {
        private final String baseUri;
        private McpJsonMapper jsonMapper;
        private HttpClient.Builder clientBuilder;
        private String endpoint;
        private boolean resumableStreams;
        private boolean openConnectionOnStartup;
        private HttpRequest.Builder requestBuilder;
        private McpAsyncHttpClientRequestCustomizer httpRequestCustomizer;
        private Duration connectTimeout;
        private List<String> supportedProtocolVersions;

        private Builder(String baseUri) {
            this.clientBuilder = HttpClient.newBuilder().version(Version.HTTP_1_1);
            this.endpoint = "/mcp";
            this.resumableStreams = true;
            this.openConnectionOnStartup = false;
            this.requestBuilder = HttpRequest.newBuilder();
            this.httpRequestCustomizer = McpAsyncHttpClientRequestCustomizer.NOOP;
            this.connectTimeout = Duration.ofSeconds(10L);
            this.supportedProtocolVersions = List.of("2024-11-05", "2025-03-26", "2025-06-18");
            Assert.hasText(baseUri, "baseUri must not be empty");
            this.baseUri = baseUri;
        }

        public Builder clientBuilder(HttpClient.Builder clientBuilder) {
            Assert.notNull(clientBuilder, "clientBuilder must not be null");
            this.clientBuilder = clientBuilder;
            return this;
        }

        public Builder customizeClient(final Consumer<HttpClient.Builder> clientCustomizer) {
            Assert.notNull(clientCustomizer, "clientCustomizer must not be null");
            clientCustomizer.accept(this.clientBuilder);
            return this;
        }

        public Builder requestBuilder(HttpRequest.Builder requestBuilder) {
            Assert.notNull(requestBuilder, "requestBuilder must not be null");
            this.requestBuilder = requestBuilder;
            return this;
        }

        public Builder customizeRequest(final Consumer<HttpRequest.Builder> requestCustomizer) {
            Assert.notNull(requestCustomizer, "requestCustomizer must not be null");
            requestCustomizer.accept(this.requestBuilder);
            return this;
        }

        public Builder jsonMapper(McpJsonMapper jsonMapper) {
            Assert.notNull(jsonMapper, "jsonMapper must not be null");
            this.jsonMapper = jsonMapper;
            return this;
        }

        public Builder endpoint(String endpoint) {
            Assert.hasText(endpoint, "endpoint must be a non-empty String");
            this.endpoint = endpoint;
            return this;
        }

        public Builder resumableStreams(boolean resumableStreams) {
            this.resumableStreams = resumableStreams;
            return this;
        }

        public Builder openConnectionOnStartup(boolean openConnectionOnStartup) {
            this.openConnectionOnStartup = openConnectionOnStartup;
            return this;
        }

        public Builder httpRequestCustomizer(McpSyncHttpClientRequestCustomizer syncHttpRequestCustomizer) {
            this.httpRequestCustomizer = McpAsyncHttpClientRequestCustomizer.fromSync(syncHttpRequestCustomizer);
            return this;
        }

        public Builder asyncHttpRequestCustomizer(McpAsyncHttpClientRequestCustomizer asyncHttpRequestCustomizer) {
            this.httpRequestCustomizer = asyncHttpRequestCustomizer;
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            Assert.notNull(connectTimeout, "connectTimeout must not be null");
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder supportedProtocolVersions(List<String> supportedProtocolVersions) {
            Assert.notEmpty(supportedProtocolVersions, "supportedProtocolVersions must not be empty");
            this.supportedProtocolVersions = Collections.unmodifiableList(supportedProtocolVersions);
            return this;
        }

        public HttpClientStreamableHttpTransport build() {
            HttpClient httpClient = this.clientBuilder.connectTimeout(this.connectTimeout).build();
            return new HttpClientStreamableHttpTransport(this.jsonMapper == null ? McpJsonMapper.getDefault() : this.jsonMapper, httpClient, this.requestBuilder, this.baseUri, this.endpoint, this.resumableStreams, this.openConnectionOnStartup, this.httpRequestCustomizer, this.supportedProtocolVersions);
        }
    }
}
