// API service for connecting to Spring Boot backend at 127.0.0.1:8080

const API_BASE_URL = 'http://127.0.0.1:8080';
const WS_BASE_URL = 'ws://127.0.0.1:8080';

export interface ChatRequest {
    message: string;
    chatSessionId: string | undefined;
    chatRequestId: string;
}

export interface Answer {
    message: string;
    chatSessionId: string;
    chatRequestId?: string;
    completed: boolean;
}

export class ApiError extends Error {
    constructor(message: string, public status?: number) {
        super(message);
        this.name = 'ApiError';
    }
}

function generateRequestId(): string {
    return 'REQ_' + crypto.randomUUID().replace(/-/g, '');
}

export async function getApiVersion(): Promise<string> {
    try {
        const response = await fetch(`${API_BASE_URL}/api/version`);

        if (!response.ok) {
            throw new ApiError(`Failed to get API version: ${response.statusText}`, response.status);
        }

        return await response.text();
    } catch (error) {
        if (error instanceof ApiError) {
            throw error;
        }
        throw new ApiError(`Network error: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
}

export async function getStrategyGraph(): Promise<string> {
    try {
        const response = await fetch(`${API_BASE_URL}/api/koog/strategy/graph`, {
            headers: {
                Accept: 'text/plain',
            },
        });
        if (!response.ok) {
            throw new ApiError(`Failed to get strategy graph: ${response.statusText}`, response.status);
        }

        return await response.text();
    } catch (error) {
        if (error instanceof ApiError) {
            throw error;
        }
        throw new ApiError(`Network error: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
}

export class WebSocketChatClient {
    private ws: WebSocket | null = null;
    private messageHandlers: Map<string, (answer: Answer) => void> = new Map();
    private globalMessageHandlers: ((answer: Answer) => void)[] = [];
    private sessionId: string | null = null;
    private disconnectHandlers: (() => void)[] = [];
    private sessionIdReceivedHandlers: ((sessionId: string) => void)[] = [];
    private chatSessionId: string | undefined = undefined;
    private pendingRequestIds: Set<string> = new Set();

    constructor(sessionId?: string | null) {
        this.sessionId = sessionId || null;
    }

    private addHandler<T>(handlers: T[], handler: T): void {
        handlers.push(handler);
    }

    private removeHandler<T>(handlers: T[], handler: T): void {
        const index = handlers.indexOf(handler);
        if (index > -1) {
            handlers.splice(index, 1);
        }
    }

    addMessageHandler(handler: (answer: Answer) => void) {
        this.addHandler(this.globalMessageHandlers, handler);
    }

    removeMessageHandler(handler: (answer: Answer) => void) {
        this.removeHandler(this.globalMessageHandlers, handler);
    }

    addDisconnectHandler(handler: () => void) {
        this.addHandler(this.disconnectHandlers, handler);
    }

    removeDisconnectHandler(handler: () => void) {
        this.removeHandler(this.disconnectHandlers, handler);
    }

    addSessionIdReceivedHandler(handler: (sessionId: string) => void) {
        this.addHandler(this.sessionIdReceivedHandlers, handler);
    }

    removeSessionIdReceivedHandler(handler: (sessionId: string) => void) {
        this.removeHandler(this.sessionIdReceivedHandlers, handler);
    }

    connect(hasHistory: boolean = false): Promise<void> {
        return new Promise((resolve, reject) => {
            try {
                this.ws = new WebSocket(`${WS_BASE_URL}/ws/chat`);

                this.ws.onopen = () => {
                    console.log('WebSocket connected');

                    // Send appropriate message based on connection state
                    if (this.sessionId && this.ws) {
                        const message = hasHistory ? '[CONTINUE]' : '[START]';
                        const requestId = generateRequestId();
                        this.pendingRequestIds.add(requestId);
                        const startMessage: ChatRequest = {
                            message: message,
                            chatSessionId: this.sessionId,
                            chatRequestId: requestId
                        };
                        this.ws.send(JSON.stringify(startMessage));
                        console.log(`Sent ${message} message to server with session:`, this.sessionId);
                    }

                    resolve();
                };

                this.ws.onerror = (error) => {
                    console.error('WebSocket error:', error);
                    reject(new ApiError('WebSocket connection error'));
                };

                this.ws.onclose = () => {
                    console.log('WebSocket disconnected');
                    // Notify all disconnect handlers
                    this.disconnectHandlers.forEach(handler => handler());
                };

                this.ws.onmessage = (event) => {
                    try {
                        const answer: Answer = JSON.parse(event.data);
                        // Ignore messages with empty content unless completed=true
                        if ((!answer.message || answer.message.trim() === '') && answer.completed !== true) {
                            console.log('Ignoring empty message from server');
                            return;
                        }
                        // Capture the server-assigned chatSessionId for subsequent requests
                        if (answer.chatSessionId) {
                            const wasNewSession = !this.chatSessionId;
                            this.chatSessionId = answer.chatSessionId;
                            // Notify handlers when session ID is received from server
                            if (wasNewSession) {
                                this.sessionIdReceivedHandlers.forEach(handler => handler(answer.chatSessionId));
                            }
                        }

                        // Verify request ID if present
                        if (answer.chatRequestId) {
                            if (!this.pendingRequestIds.has(answer.chatRequestId)) {
                                // Only warn if there's also no message handler (indicating it's truly unknown)
                                // Initialization messages like [START]/[CONTINUE] have pending IDs but no handlers
                                if (!this.messageHandlers.has(answer.chatRequestId)) {
                                    console.warn('Received answer with unknown request ID:', answer.chatRequestId);
                                }
                            } else {
                                this.pendingRequestIds.delete(answer.chatRequestId);
                            }

                            // Call specific handler for this request
                            const handler = this.messageHandlers.get(answer.chatRequestId);
                            if (handler) {
                                handler(answer);
                                // Only delete handler when message is completed
                                if (answer.completed !== false) {
                                    this.messageHandlers.delete(answer.chatRequestId);
                                }
                            }
                        }

                        // Call global handlers (for greeting and other messages)
                        this.globalMessageHandlers.forEach(handler => handler(answer));
                    } catch (error) {
                        console.error('Failed to parse message:', error);
                    }
                };
            } catch (error) {
                reject(new ApiError(`Failed to connect: ${error instanceof Error ? error.message : 'Unknown error'}`));
            }
        });
    }

    sendMessage(message: string, onUpdate?: (answer: Answer) => void): Promise<Answer> {
        return new Promise((resolve, reject) => {
            if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
                reject(new ApiError('WebSocket is not connected'));
                return;
            }

            const requestId = generateRequestId();
            this.pendingRequestIds.add(requestId);
            let accumulatedText = '';

            const handler = (answer: Answer) => {
                // Accumulate text from streaming responses
                accumulatedText += answer.message;

                // Call update callback if provided
                if (onUpdate) {
                    onUpdate({
                        ...answer,
                        message: accumulatedText,
                        completed: answer.completed
                    });
                }

                // Only resolve and cleanup when completed
                if (answer.completed !== false) {
                    this.messageHandlers.delete(requestId);
                    resolve({
                        ...answer,
                        message: accumulatedText
                    });
                }
            };

            this.messageHandlers.set(requestId, handler);

            const request: ChatRequest = {
                message: message,
                chatSessionId: this.chatSessionId,
                chatRequestId: requestId
            };
            this.ws.send(JSON.stringify(request));
        });
    }

    close() {
        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }
        this.messageHandlers.clear();
        this.pendingRequestIds.clear();
    }

    isConnected(): boolean {
        return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
    }
}