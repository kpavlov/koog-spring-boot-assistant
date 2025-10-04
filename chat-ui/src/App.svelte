<script lang="ts">
    import {afterUpdate, onDestroy, onMount} from 'svelte';
    import {Loader2, Send, User} from 'lucide-svelte';
    import {ApiError, getApiVersion, WebSocketChatClient} from './lib/api';
    import {sessionId} from './lib/session';
    import {marked} from 'marked';

    interface Message {
        id: string;
        text: string;
        isUser: boolean;
        timestamp: Date;
    }

    let messages: Message[] = [];


    let currentMessage = '';
    let isLoading = false;
    let chatContainer: HTMLElement;
    let messageInput: HTMLTextAreaElement;
    let currentSessionId: string;
    let isServerOnline = true;
    let serverVersion = '';
    let wsClient: WebSocketChatClient | null = null;
    let reconnectTimeout: number | null = null;
    let reconnectAttempts = 0;
    const MAX_RECONNECT_ATTEMPTS = 10;
    const INITIAL_RECONNECT_DELAY = 1000; // 1 second

    // Configure marked options
    marked.setOptions({
        breaks: true,
        gfm: true
    });

    // Render markdown to HTML
    function renderMarkdown(text: string): string {
        return marked.parse(text) as string;
    }

    // Check server health and reconnect WebSocket if needed
    async function checkServerHealth() {
        const wasOffline = !isServerOnline;

        // Check if WebSocket is connected
        if (wsClient && wsClient.isConnected()) {
            // Connection is live, get version and update status
            try {
                const version = await getApiVersion();
                isServerOnline = true;
                serverVersion = version;
            } catch (error) {
                // WebSocket is connected but version call failed
                // Still consider online since WebSocket works
                isServerOnline = true;
                console.warn('Version endpoint failed but WebSocket is connected:', error);
            }
        } else {
            // WebSocket is not connected, try to reconnect
            isServerOnline = false;
            serverVersion = '';

            try {
                await connectWebSocket();
                // After successful connection, get version
                if (wsClient && wsClient.isConnected()) {
                    try {
                        const version = await getApiVersion();
                        isServerOnline = true;
                        serverVersion = version;
                    } catch (error) {
                        // WebSocket connected but version failed
                        isServerOnline = true;
                        console.warn('Version endpoint failed but WebSocket is connected:', error);
                    }
                }
            } catch (error) {
                console.error('WebSocket connection failed:', error);
                // Trigger reconnection if connection fails
                scheduleReconnect();
            }
        }
    }

    // Automatic reconnection with exponential backoff
    function scheduleReconnect() {
        if (reconnectTimeout) {
            clearTimeout(reconnectTimeout);
        }

        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            console.log('Max reconnection attempts reached');
            isServerOnline = false;
            serverVersion = '';
            return;
        }

        const delay = 5000; // Fixed 5-second interval for reconnection attempts
        console.log(`Scheduling reconnection attempt ${reconnectAttempts + 1} in ${delay}ms`);

        reconnectTimeout = setTimeout(async () => {
            reconnectAttempts++;
            try {
                await connectWebSocket();
                if (wsClient && wsClient.isConnected()) {
                    console.log('Reconnection successful');
                }
            } catch (error) {
                console.error('Reconnection failed:', error);
                scheduleReconnect(); // Schedule next attempt
            }
        }, delay);
    }

    // Handle WebSocket disconnection
    function handleDisconnect() {
        console.log('WebSocket disconnected, updating status and scheduling reconnection');
        isServerOnline = false;
        serverVersion = '';
        scheduleReconnect();
    }

    // Connect to WebSocket
    async function connectWebSocket() {
        // Check if already connected to prevent duplicate connections
        if (wsClient && wsClient.isConnected()) {
            console.log('WebSocket already connected, skipping connection attempt');
            return;
        }

        try {
            if (wsClient) {
                wsClient.close();
            }
            wsClient = new WebSocketChatClient(currentSessionId);

            // Add handler for greeting and other messages that are not handled by sendMessage
            wsClient.addMessageHandler((answer) => {
                // Only add messages when not loading (to avoid duplicating sendMessage responses)
                // Also ignore messages with empty content
                if (!isLoading && answer.message && answer.message.trim() !== '') {
                    const newMessage: Message = {
                        id: Date.now().toString(),
                        text: answer.message,
                        isUser: false,
                        timestamp: new Date()
                    };
                    messages = [...messages, newMessage];
                }
            });

            // Add disconnect handler for automatic reconnection
            wsClient.addDisconnectHandler(handleDisconnect);

            // Check if we have message history to determine connection type
            const hasHistory = messages.length > 0;
            await wsClient.connect(hasHistory);
            console.log('WebSocket connected');
            
            // Reset reconnect attempts on successful connection
            reconnectAttempts = 0;
        } catch (error) {
            console.error('Failed to connect WebSocket:', error);
            wsClient = null;
        }
    }

    // Auto-scroll functionality
    function scrollToBottom() {
        if (chatContainer) {
            chatContainer.scrollTop = chatContainer.scrollHeight;
        }
    }

    // Auto-resize textarea
    function resizeTextarea() {
        if (messageInput) {
            messageInput.style.height = 'auto';
            messageInput.style.height = messageInput.scrollHeight + 'px';
        }
    }

    $: if (currentMessage !== undefined) {
        resizeTextarea();
    }

    afterUpdate(() => {
        scrollToBottom();
    });

    // Send message to AI backend via WebSocket
    async function sendMessage() {
        if (!currentMessage.trim() || isLoading || !isServerOnline) return;

        // Ensure WebSocket is connected
        if (!wsClient || !wsClient.isConnected()) {
            await connectWebSocket();
            if (!wsClient || !wsClient.isConnected()) {
                console.error('WebSocket not connected');
                return;
            }
        }

        const userMessage: Message = {
            id: Date.now().toString(),
            text: currentMessage.trim(),
            isUser: true,
            timestamp: new Date()
        };

        // Create immediate loading response bubble
        const loadingMessageId = (Date.now() + 1).toString();
        const loadingMessage: Message = {
            id: loadingMessageId,
            text: '', // Empty text - will be shown as loading dots
            isUser: false,
            timestamp: new Date()
        };

        messages = [...messages, userMessage, loadingMessage];
        const messageText = currentMessage.trim();
        currentMessage = '';
        isLoading = true;

        // Block UI for 30 seconds or until response
        const timeout = setTimeout(() => {
            isLoading = false;
        }, 30000);

        try {
            const response = await wsClient.sendMessage(messageText);

            clearTimeout(timeout);

            // Update the loading message with the actual response
            messages = messages.map(msg =>
                msg.id === loadingMessageId
                    ? {...msg, text: response.message, timestamp: new Date()}
                    : msg
            );

            isLoading = false;

            // Focus input field after receiving response
            setTimeout(() => {
                messageInput?.focus();
            }, 50);
        } catch (error) {
            clearTimeout(timeout);
            isLoading = false;
            console.error('Failed to get AI response:', error);

            // Update the loading message with error
            const errorText = error instanceof ApiError
                ? `Error: ${error.message}`
                : 'Sorry, I encountered an error while processing your request. Please make sure the server is running on http://127.0.0.1:8080';

            messages = messages.map(msg =>
                msg.id === loadingMessageId
                    ? {...msg, text: errorText, timestamp: new Date()}
                    : msg
            );

            // Focus input field after error message
            setTimeout(() => {
                messageInput?.focus();
            }, 50);
        }
    }

    function handleKeyPress(event: KeyboardEvent) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            sendMessage();
        }
    }

    onMount(() => {
        // Subscribe to session ID store
        const unsubscribe = sessionId.subscribe(value => {
            currentSessionId = value;
        });

        // Focus with a slight delay to ensure DOM is ready
        setTimeout(() => {
            messageInput?.focus();
            // Initialize textarea height
            resizeTextarea();
        }, 100);

        // Initial health check and WebSocket connection (async operations)
        (async () => {
            await checkServerHealth();
            await connectWebSocket();
        })();

        return () => {
            unsubscribe();
            if (reconnectTimeout) {
                clearTimeout(reconnectTimeout);
            }
            if (wsClient) {
                wsClient.close();
            }
        };
    });

    onDestroy(() => {
        if (reconnectTimeout) {
            clearTimeout(reconnectTimeout);
        }
        if (wsClient) {
            wsClient.close();
        }
    });
</script>

<main class="chat-app">
    <!-- Header -->
    <header class="chat-header">
        <div class="header-content">
            <div class="ai-indicator">
                <img src="/logo.png" alt="Elven Assistant" class="header-avatar"/>
                <span>Elven Assistant</span>
            </div>
            <div class="status">
                {#if isLoading && false}
                    <Loader2 size={20} class="animate-spin"/>
                    <span>AI is thinking...</span>
                {:else}
                    <div class="status-dot {isServerOnline ? 'online' : 'offline'}"></div>
                    <span>
            {isServerOnline ? 'Online' : 'Offline'}
                        {#if serverVersion}
              <sup>{serverVersion}</sup>
            {/if}
          </span>
                {/if}
            </div>
        </div>
    </header>

    <!-- Chat Messages -->
    <div class="chat-container" bind:this={chatContainer}>
        {#each messages as message (message.id)}
            <div class="message-wrapper {message.isUser ? 'user-message' : 'ai-message'}">
                <div class="avatar">
                    {#if message.isUser}
                        <User size={24}/>
                    {:else}
                        <img src="/elf.png" alt="Elven Assistant" class="avatar-image"/>
                    {/if}
                </div>
                <div class="message-content">
                    <div class="message-bubble">
                        {#if message.text}
                            <div class="markdown-content">
                                {@html renderMarkdown(message.text)}
                            </div>
                        {:else if !message.isUser}
                            <div class="loading-dots">
                                <span></span>
                                <span></span>
                                <span></span>
                            </div>
                        {:else}
                            <div class="markdown-content">
                                {@html renderMarkdown(message.text)}
                            </div>
                        {/if}
                    </div>
                    <div class="message-time">
                        {message.timestamp.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})}
                    </div>
                </div>
            </div>
        {/each}
    </div>

    <!-- Message Input -->
    <div class="input-container {isLoading ? 'disabled' : ''}">
        <div class="input-wrapper">
      <textarea
          bind:this={messageInput}
          bind:value={currentMessage}
          on:keypress={handleKeyPress}
          placeholder="Type your message here..."
          rows="1"
          disabled={isLoading}
          class="message-input"
          tabindex="1"
      ></textarea>
            <button
                on:click={sendMessage}
                disabled={isLoading || !currentMessage.trim() || !isServerOnline}
                class="send-button"
            >
                {#if isLoading}
                    <Loader2 size={24} class="animate-spin"/>
                {:else}
                    <Send size={24}/>
                {/if}
            </button>
        </div>
    </div>
</main>

<style>
    .chat-app {
        display: flex;
        flex-direction: column;
        height: 100vh;
        background: white;
        width: 80%;
        max-width: 1400px;
        margin: 0 auto;
        border-radius: 0;
        overflow: hidden;
        box-shadow: 0 0 100px rgba(0, 0, 0, 0.3);
    }

    /* Header */
    .chat-header {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        padding: 1.5rem 2rem;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
    }

    .header-content {
        display: flex;
        justify-content: space-between;
        align-items: center;
        max-width: 1400px;
        margin: 0 auto;
    }

    .ai-indicator {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        font-size: 3rem;
        font-weight: 600;
        font-family: fantasy, 'Times New Roman', serif;
        letter-spacing: 0.05em;
        text-shadow: 0 0 20px rgba(255, 255, 255, 0.8), 0 0 40px rgba(255, 255, 255, 0.6);
        animation: gentle-glow 3s ease-in-out infinite alternate;
    }

    .header-avatar {
        width: 5rem;
        height: 5rem;
        border-radius: 50%;
        object-fit: cover;
        border: 6px solid rgba(255, 255, 255, 0.5);
    }

    .status {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-size: 1.5rem;
        font-weight: bold;
        opacity: 0.9;
    }

    .status sup {
        font-size: 0.8rem;

    }

    .status-dot {
        width: 16px;
        height: 16px;
        border-radius: 50%;
        animation: pulse 2s infinite;
    }

    .status-dot.online {
        background: #10b981;
    }

    .status-dot.offline {
        background: #ef4444;
    }

    /* Chat Container */
    .chat-container {
        flex: 1;
        overflow-y: auto;
        padding: 2rem;
        background: #f8fafc;
        scroll-behavior: smooth;
        max-width: 1400px;
        margin: 0 auto;
        width: 100%;
        min-height: 0;
        height: 100%;
    }

    /* Messages */
    .message-wrapper {
        display: flex;
        margin-bottom: 2rem;
        max-width: 80%;
    }

    .message-wrapper.user-message {
        margin-left: auto;
        flex-direction: row-reverse;
    }

    .message-wrapper.ai-message {
        margin-right: auto;
    }

    .avatar {
        width: 6rem;
        height: 6rem;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 1rem;
        flex-shrink: 0;
    }

    .user-message .avatar {
        background: #2563eb;
        color: white;
    }

    .ai-message .avatar {
        background: #8b5cf6;
        color: white;
        overflow: hidden;
    }

    .avatar-image {
        width: 100%;
        height: 100%;
        object-fit: cover;
    }

    .message-content {
        flex: 1;
        display: flex;
        flex-direction: column;
    }

    .message-bubble {
        padding: 1.25rem 1.75rem;
        border-radius: 1.5rem;
        font-size: 3rem;
        line-height: 1.5;
        word-wrap: break-word;
        word-break: break-word;
        overflow-wrap: break-word;
        hyphens: auto;
        max-width: 100%;
    }

    .user-message .message-bubble {
        background: #2563eb;
        color: white;
        border-bottom-right-radius: 0.5rem;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    .ai-message .message-bubble {
        background: white;
        color: #1f2937;
        border: 1px solid #e5e7eb;
        border-bottom-left-radius: 0.5rem;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    .message-bubble p {
        margin: 0;
    }

    .markdown-content {
        width: 100%;
    }

    .markdown-content p {
        margin: 0 0 1rem 0;
    }

    .markdown-content p:last-child {
        margin-bottom: 0;
    }

    .markdown-content h1,
    .markdown-content h2,
    .markdown-content h3,
    .markdown-content h4,
    .markdown-content h5,
    .markdown-content h6 {
        margin: 1.5rem 0 1rem 0;
        font-weight: 600;
    }

    .markdown-content h1:first-child,
    .markdown-content h2:first-child,
    .markdown-content h3:first-child {
        margin-top: 0;
    }

    .markdown-content code {
        background: rgba(0, 0, 0, 0.05);
        padding: 0.2em 0.4em;
        border-radius: 0.3rem;
        font-family: 'Courier New', monospace;
        font-size: 0.9em;
    }

    .ai-message .markdown-content code {
        background: rgba(0, 0, 0, 0.05);
    }

    .user-message .markdown-content code {
        background: rgba(255, 255, 255, 0.2);
    }

    .markdown-content pre {
        background: rgba(0, 0, 0, 0.05);
        padding: 1rem;
        border-radius: 0.5rem;
        overflow-x: auto;
        margin: 1rem 0;
    }

    .markdown-content pre code {
        background: none;
        padding: 0;
    }

    .markdown-content ul,
    .markdown-content ol {
        margin: 1rem 0;
        padding-left: 2rem;
    }

    .markdown-content li {
        margin: 0.5rem 0;
    }

    .markdown-content blockquote {
        border-left: 4px solid rgba(0, 0, 0, 0.2);
        padding-left: 1rem;
        margin: 1rem 0;
        font-style: italic;
    }

    .markdown-content a {
        color: inherit;
        text-decoration: underline;
    }

    .markdown-content strong {
        font-weight: 700;
    }

    .markdown-content em {
        font-style: italic;
    }

    .loading-dots {
        display: flex;
        align-items: center;
        gap: 4px;
        height: 1.6em;
    }

    .loading-dots span {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background-color: #6b7280;
        animation: dot-bounce 1.4s infinite ease-in-out both;
    }

    .loading-dots span:nth-child(1) {
        animation-delay: -0.32s;
    }

    .loading-dots span:nth-child(2) {
        animation-delay: -0.16s;
    }

    @keyframes dot-bounce {
        0%, 80%, 100% {
            transform: scale(0);
        }
        40% {
            transform: scale(1);
        }
    }

    .message-time {
        display: none; /* hide it temporary */
        font-size: 1.5rem;
        color: #6b7280;
        margin-top: 0.5rem;
        padding: 0 0.5rem;
    }

    .user-message .message-time {
        text-align: right;
    }

    /* Input Container */
    .input-container {
        background: white;
        border-top: 2px solid #e5e7eb;
        padding: 1.5rem 2rem 2rem;
    }

    .input-container.disabled {
        opacity: 0.7;
        pointer-events: none;
    }

    .input-wrapper {
        display: flex;
        gap: 1rem;
        max-width: 1400px;
        margin: 0 auto;
        align-items: end;
    }

    .message-input {
        flex: 1;
        border: 4px solid #e5e7eb;
        border-radius: 1.5rem;
        padding: 1.25rem 1.75rem;
        font-size: 3rem;
        font-family: inherit;
        resize: none;
        outline: none;
        transition: border-color 0.2s;
        min-height: 3.5rem;
        max-height: 30vh;
        overflow-y: auto;
        caret-color: #2563eb;
        caret-width: 4px;
    }

    .message-input:focus {
        border-color: #3b82f6;
    }

    .message-input:disabled {
        background: #f9fafb;
        cursor: not-allowed;
    }

    .send-button {
        width: 3.5rem;
        height: 3.5rem;
        border: none;
        border-radius: 50%;
        background: #3b82f6;
        color: white;
        cursor: pointer;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
    }

    .send-button:hover:not(:disabled) {
        background: #2563eb;
        transform: scale(1.05);
    }

    .send-button:disabled {
        background: #9ca3af;
        cursor: not-allowed;
        transform: none;
    }

    /* Animations */
    @keyframes pulse {
        0%, 100% {
            opacity: 1;
        }
        50% {
            opacity: 0.5;
        }
    }

    :global(.animate-spin) {
        animation: spin 1s linear infinite;
    }

    @keyframes spin {
        from {
            transform: rotate(0deg);
        }
        to {
            transform: rotate(360deg);
        }
    }

    @keyframes gentle-glow {
        0% {
            text-shadow: 0 0 20px rgba(255, 255, 255, 0.8), 0 0 40px rgba(255, 255, 255, 0.6);
        }
        100% {
            text-shadow: 0 0 30px rgba(255, 255, 255, 1), 0 0 60px rgba(255, 255, 255, 0.8);
        }
    }

    /* Dark mode */
    @media (prefers-color-scheme: dark) {
        .chat-app {
            background: #1f2937;
        }

        .chat-container {
            background: #111827;
        }

        .ai-message .message-bubble {
            background: #374151;
            color: #f9fafb;
            border-color: #4b5563;
        }

        .input-container {
            background: #1f2937;
            border-color: #374151;
        }

        .message-input {
            background: #374151;
            color: #f9fafb;
            border-color: #4b5563;
        }

        .message-input:focus {
            border-color: #3b82f6;
        }

        .message-time {
            color: #9ca3af;
        }
    }

    /* Large screen optimizations for presentations */
    @media (min-width: 1920px) {
        .chat-header {
            padding: 2rem 3rem;
        }

        .ai-indicator {
            font-size: 2.25rem;
        }

        .chat-container {
            padding: 3rem;
        }

        .message-bubble {
            padding: 1.5rem 2rem;
            font-size: 1.3rem;
        }

        .avatar {
            width: 4rem;
            height: 4rem;
            margin: 0 1.5rem;
        }

        .input-container {
            padding: 2rem 3rem 2.5rem;
        }

        .message-input {
            padding: 1.5rem 2rem;
            font-size: 1.3rem;
            min-height: 4rem;
        }

        .send-button {
            width: 4rem;
            height: 4rem;
        }
    }
</style>
