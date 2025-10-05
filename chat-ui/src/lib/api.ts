// API service for connecting to Spring Boot backend at 127.0.0.1:8080

const API_BASE_URL = 'http://127.0.0.1:8080';
const WS_BASE_URL = 'ws://127.0.0.1:8080';

export interface ChatRequest {
    message: string;
    chatSessionId: string | undefined;
}

export interface Answer {
    message: string;
    chatSessionId: string;
}

export class ApiError extends Error {
    constructor(message: string, public status?: number) {
        super(message);
        this.name = 'ApiError';
    }
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
    private messageHandlers: ((answer: Answer) => void)[] = [];
    private globalMessageHandlers: ((answer: Answer) => void)[] = [];
    private sessionId: string | null = null;
    private disconnectHandlers: (() => void)[] = [];
    private sessionIdReceivedHandlers: ((sessionId: string) => void)[] = [];
    private chatSessionId: string | undefined = undefined;

    constructor(sessionId?: string | null) {
        this.sessionId = sessionId || null;
    }

    addMessageHandler(handler: (answer: Answer) => void) {
        this.globalMessageHandlers.push(handler);
    }

    removeMessageHandler(handler: (answer: Answer) => void) {
        const index = this.globalMessageHandlers.indexOf(handler);
        if (index > -1) {
            this.globalMessageHandlers.splice(index, 1);
        }
    }

    addDisconnectHandler(handler: () => void) {
        this.disconnectHandlers.push(handler);
    }

    removeDisconnectHandler(handler: () => void) {
        const index = this.disconnectHandlers.indexOf(handler);
        if (index > -1) {
            this.disconnectHandlers.splice(index, 1);
        }
    }

    addSessionIdReceivedHandler(handler: (sessionId: string) => void) {
        this.sessionIdReceivedHandlers.push(handler);
    }

    removeSessionIdReceivedHandler(handler: (sessionId: string) => void) {
        const index = this.sessionIdReceivedHandlers.indexOf(handler);
        if (index > -1) {
            this.sessionIdReceivedHandlers.splice(index, 1);
        }
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
                        const startMessage: ChatRequest = {
                            message: message,
                            chatSessionId: this.sessionId
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
                        // Ignore messages with empty content
                        if (!answer.message || answer.message.trim() === '') {
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
                        // Call temporary handlers (for sendMessage responses)
                        this.messageHandlers.forEach(handler => handler(answer));
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

    sendMessage(message: string): Promise<Answer> {
        return new Promise((resolve, reject) => {
            if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
                reject(new ApiError('WebSocket is not connected'));
                return;
            }

            const handler = (answer: Answer) => {
                // Remove handler after receiving response
                const index = this.messageHandlers.indexOf(handler);
                if (index > -1) {
                    this.messageHandlers.splice(index, 1);
                }
                resolve(answer);
            };

            this.messageHandlers.push(handler);

            const request: ChatRequest = {
                message: message,
                chatSessionId: this.chatSessionId
            };
            this.ws.send(JSON.stringify(request));
        });
    }

    close() {
        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }
        this.messageHandlers = [];
    }

    isConnected(): boolean {
        return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
    }
}