<script lang="ts">
    import {afterUpdate, onDestroy, onMount} from 'svelte';
    import {Loader2, Send, User, HelpCircle, ChevronDown} from 'lucide-svelte';
    import {ApiError, getApiVersion, WebSocketChatClient} from './lib/api';
    import {sessionId} from './lib/session';
    import {marked} from 'marked';

    // Get base URL for asset paths
    const baseUrl = import.meta.env.BASE_URL || '/';

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
    const MAX_RECONNECT_INTERVAL = 7000; // 5 seconds - max time between reconnect attempts
    let showHelpMenu = false;
    let showInstructionsModal = false;
    let instructionsContent = '';
    let showOfflineToast = false;
    let offlineToastTimer: number | null = null;
    let toastDismissCount = 0;
    const INITIAL_TOAST_DELAY = 10000; // 10 seconds
    const MAX_TOAST_INTERVAL = 300000; // 5 minutes

    // Configure marked options with custom renderer for links
    const renderer = new marked.Renderer();
    const originalLinkRenderer = renderer.link.bind(renderer);
    renderer.link = (token) => {
        const html = originalLinkRenderer(token);
        return html.replace('<a', '<a target="_blank" rel="noopener noreferrer"');
    };

    marked.setOptions({
        breaks: true,
        gfm: true,
        renderer: renderer
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
                // Dismiss toast and reset counter when server comes online
                if (wasOffline) {
                    showOfflineToast = false;
                    toastDismissCount = 0;
                    if (offlineToastTimer) {
                        clearTimeout(offlineToastTimer);
                        offlineToastTimer = null;
                    }
                }
            } catch (error) {
                // WebSocket is connected but version call failed
                // Still consider online since WebSocket works
                isServerOnline = true;
                console.warn('Version endpoint failed but WebSocket is connected:', error);
                // Dismiss toast and reset counter when server comes online
                if (wasOffline) {
                    showOfflineToast = false;
                    toastDismissCount = 0;
                    if (offlineToastTimer) {
                        clearTimeout(offlineToastTimer);
                        offlineToastTimer = null;
                    }
                }
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
                        // Dismiss toast and reset counter when server comes online
                        if (wasOffline) {
                            showOfflineToast = false;
                            toastDismissCount = 0;
                            if (offlineToastTimer) {
                                clearTimeout(offlineToastTimer);
                                offlineToastTimer = null;
                            }
                        }
                    } catch (error) {
                        // WebSocket connected but version failed
                        isServerOnline = true;
                        console.warn('Version endpoint failed but WebSocket is connected:', error);
                        // Dismiss toast and reset counter when server comes online
                        if (wasOffline) {
                            showOfflineToast = false;
                            toastDismissCount = 0;
                            if (offlineToastTimer) {
                                clearTimeout(offlineToastTimer);
                                offlineToastTimer = null;
                            }
                        }
                    }
                }
            } catch (error) {
                console.error('WebSocket connection failed:', error);
                // Trigger reconnection if connection fails
                scheduleReconnect();
            }
        }
    }

    // Automatic reconnection with exponential backoff (max 5 seconds)
    function scheduleReconnect() {
        if (reconnectTimeout) {
            clearTimeout(reconnectTimeout);
        }

        // Calculate delay with exponential backoff, capped at MAX_RECONNECT_INTERVAL
        const baseDelay = 1000; // 1 second
        const exponentialDelay = baseDelay * Math.pow(2, Math.min(reconnectAttempts, 3));
        const delay = Math.min(exponentialDelay, MAX_RECONNECT_INTERVAL);

        console.log(`Scheduling reconnection attempt ${reconnectAttempts + 1} in ${delay}ms`);

        reconnectTimeout = setTimeout(async () => {
            reconnectAttempts++;
            try {
                await connectWebSocket();
                if (wsClient && wsClient.isConnected()) {
                    console.log('Reconnection successful after ${reconnectAttempts} attempts');
                }
            } catch (error) {
                console.error('Reconnection failed:', error);
                scheduleReconnect(); // Continue reconnecting forever
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

            // Add handler for session ID received from server
            wsClient.addSessionIdReceivedHandler((serverSessionId) => {
                console.log('Received session ID from server:', serverSessionId);
                // Store the session ID in localStorage only when confirmed by server
                sessionId.update(serverSessionId);
            });

            // Add disconnect handler for automatic reconnection
            wsClient.addDisconnectHandler(handleDisconnect);

            // Check if we have message history to determine connection type
            const hasHistory = messages.length > 0;
            await wsClient.connect(hasHistory);
            console.log('WebSocket connected');

            // Update UI state on successful connection
            isServerOnline = true;

            // Dismiss toast and reset counter when server comes online
            showOfflineToast = false;
            toastDismissCount = 0;
            if (offlineToastTimer) {
                clearTimeout(offlineToastTimer);
                offlineToastTimer = null;
            }

            // Refresh server version
            try {
                serverVersion = await getApiVersion();
            } catch (error) {
                console.warn('Failed to get server version after connect:', error);
                // Keep isServerOnline true since WebSocket is connected
            }

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

    function toggleHelpMenu() {
        showHelpMenu = !showHelpMenu;
    }

    function openGitHub() {
        window.open('https://github.com/kpavlov/koog-spring-boot-assistant', '_blank');
        showHelpMenu = false;
    }

    function openApiDocs() {
        window.open('https://petstore.swagger.io/?url=https://kpavlov.github.io/koog-spring-boot-assistant/docs/openapi.yaml', 'elven-openapi');
        showHelpMenu = false;
    }

    async function showInstructions() {
        showHelpMenu = false;
        if (!instructionsContent) {
            try {
                const response = await fetch(`${baseUrl}setup-instructions.md`);
                instructionsContent = await response.text();
            } catch (error) {
                console.error('Failed to load instructions:', error);
                instructionsContent = '# Error\nFailed to load setup instructions.';
            }
        }
        showInstructionsModal = true;
    }

    function closeInstructionsModal() {
        showInstructionsModal = false;
    }

    function openInstructionsFromToast() {
        showOfflineToast = false;
        showInstructions();
    }

    function dismissToast() {
        showOfflineToast = false;
        if (offlineToastTimer) {
            clearTimeout(offlineToastTimer);
            offlineToastTimer = null;
        }

        // Schedule toast to reappear if server is still offline and was never connected
        // But stop after 3 dismissals
        const hadPreviousSession = localStorage.getItem('chat_session_id') !== null;
        if (!hadPreviousSession && !isServerOnline) {
            toastDismissCount++;
            if (toastDismissCount <= 2) {
                scheduleToastReappearance();
            } else {
                console.log('Toast dismissed 3 times - will not show again this session');
            }
        }
    }

    function scheduleToastReappearance() {
        if (offlineToastTimer) {
            clearTimeout(offlineToastTimer);
        }

        // Don't schedule if dismissed too many times
        if (toastDismissCount > 2) {
            return;
        }

        // Calculate delay with exponential backoff: 30s, 60s
        const baseDelay = 30000; // 30 seconds
        const exponentialDelay = baseDelay * Math.pow(2, toastDismissCount - 1);
        const delay = Math.min(exponentialDelay, MAX_TOAST_INTERVAL);

        console.log(`Toast will reappear in ${delay / 1000} seconds (dismiss count: ${toastDismissCount})`);

        offlineToastTimer = setTimeout(() => {
            // Only show toast if still offline and no session was ever created
            const hadPreviousSession = localStorage.getItem('chat_session_id') !== null;
            if (!hadPreviousSession && !isServerOnline && toastDismissCount <= 2) {
                showOfflineToast = true;
            }
        }, delay);
    }

    function handleClickOutside(event: MouseEvent) {
        const target = event.target as HTMLElement;
        if (!target.closest('.help-menu-container')) {
            showHelpMenu = false;
        }
    }

    async function handleOfflineClick() {
        if (!isServerOnline) {
            console.log('Manual reconnection triggered');
            await checkServerHealth();
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

        // Check if server was never online - show toast after initial delay
        const hadPreviousSession = localStorage.getItem('chat_session_id') !== null;
        if (!hadPreviousSession) {
            offlineToastTimer = setTimeout(() => {
                // Only show toast if still offline after initial delay
                if (!isServerOnline) {
                    showOfflineToast = true;
                }
            }, INITIAL_TOAST_DELAY);
        }

        // Add click outside handler for help menu
        document.addEventListener('click', handleClickOutside);

        return () => {
            unsubscribe();
            if (reconnectTimeout) {
                clearTimeout(reconnectTimeout);
            }
                if (offlineToastTimer) {
                clearTimeout(offlineToastTimer);
            }
            if (wsClient) {
                wsClient.close();
            }
            document.removeEventListener('click', handleClickOutside);
        };
    });

    onDestroy(() => {
        if (reconnectTimeout) {
            clearTimeout(reconnectTimeout);
        }
        if (offlineToastTimer) {
            clearTimeout(offlineToastTimer);
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
                <img src="{baseUrl}logo.png" alt="Elven Assistant" class="header-avatar"/>
                <span>Elven Assistant</span>
            </div>
            <div class="header-right">
                <div class="status {!isServerOnline ? 'clickable' : ''}" on:click={handleOfflineClick}>
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
                <div class="help-menu-container">
                    <button class="help-button" on:click={toggleHelpMenu} title="Help">
                        <HelpCircle size={24}/>
                        <ChevronDown size={16} class="chevron {showHelpMenu ? 'open' : ''}"/>
                    </button>
                    {#if showHelpMenu}
                        <div class="help-dropdown">
                            <button class="help-menu-item" on:click={openGitHub}>
                                View Project on GitHub
                            </button>
                            <button class="help-menu-item" on:click={openApiDocs}>
                                OpenAPI Specification
                            </button>
                            <button class="help-menu-item" on:click={showInstructions}>
                                Setup Instructions
                            </button>
                        </div>
                    {/if}
                </div>
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
                        <img src="{baseUrl}elf.png" alt="Elven Assistant" class="avatar-image"/>
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

    <!-- Instructions Modal -->
    {#if showInstructionsModal}
        <div class="modal-overlay" on:click={closeInstructionsModal}>
            <div class="modal-content" on:click={(e) => e.stopPropagation()}>
                <div class="modal-header">
                    <h2>Setup Instructions</h2>
                    <button class="modal-close" on:click={closeInstructionsModal}>✕</button>
                </div>
                <div class="modal-body">
                    <div class="markdown-content">
                        {@html renderMarkdown(instructionsContent)}
                    </div>
                </div>
            </div>
        </div>
    {/if}

    <!-- Offline Toast Notification -->
    {#if showOfflineToast}
        <div class="toast-container">
            <div class="toast" on:click={openInstructionsFromToast}>
                <div class="toast-content">
                    <div class="toast-icon">🧙‍♂️</div>
                    <div class="toast-message">
                        <div class="toast-title">The ancient servers slumber...</div>
                        <div class="toast-subtitle">It seems the Spring-Boot realm is not yet awakened. Shall I guide thee through the ritual of summoning?</div>
                    </div>
                </div>
                <button class="toast-dismiss" on:click={(e) => { e.stopPropagation(); dismissToast(); }} title="Dismiss">✕</button>
            </div>
        </div>
    {/if}
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

    .header-right {
        display: flex;
        align-items: center;
        gap: 1.5rem;
    }

    .status {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-size: 1.5rem;
        font-weight: bold;
        opacity: 0.9;
    }

    .status.clickable {
        cursor: pointer;
        transition: all 0.2s;
        padding: 0.5rem 1rem;
        border-radius: 0.5rem;
    }

    .status.clickable:hover {
        background: rgba(255, 255, 255, 0.2);
        opacity: 1;
    }

    /* Help Menu */
    .help-menu-container {
        position: relative;
    }

    .help-button {
        display: flex;
        align-items: center;
        gap: 0.25rem;
        background: rgba(255, 255, 255, 0.2);
        border: 2px solid rgba(255, 255, 255, 0.3);
        color: white;
        padding: 0.5rem 1rem;
        border-radius: 0.5rem;
        cursor: pointer;
        transition: all 0.2s;
        font-size: 1rem;
    }

    .help-button:hover {
        background: rgba(255, 255, 255, 0.3);
        border-color: rgba(255, 255, 255, 0.5);
    }

    .help-button .chevron {
        transition: transform 0.2s;
    }

    .help-button .chevron.open {
        transform: rotate(180deg);
    }

    .help-dropdown {
        position: absolute;
        top: calc(100% + 0.5rem);
        right: 0;
        background: #764ba2;
        border-radius: 0.5rem;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
        width: max-content;
        z-index: 1000;
        overflow: hidden;
    }

    .help-menu-item {
        display: block;
        width: 100%;
        padding: 1rem 1.5rem;
        background: transparent;
        border: none;
        color: white;
        text-align: left;
        cursor: pointer;
        transition: background 0.2s;
        font-size: 1.5rem;
        white-space: nowrap;
    }

    .help-menu-item:hover {
        background: rgba(255, 255, 255, 0.2);
    }

    .help-menu-item:not(:last-child) {
        border-bottom: 1px solid rgba(255, 255, 255, 0.2);
    }

    /* Modal */
    .modal-overlay {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 2000;
    }

    .modal-content {
        background: white;
        border-radius: 1rem;
        max-width: 900px;
        width: 90%;
        max-height: 80vh;
        display: flex;
        flex-direction: column;
        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
    }

    .modal-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 2rem 3rem;
        border-bottom: 2px solid #e5e7eb;
    }

    .modal-header h2 {
        margin: 0;
        font-size: 2rem;
        color: #1f2937;
    }

    .modal-close {
        background: none;
        border: none;
        font-size: 2.5rem;
        color: #6b7280;
        cursor: pointer;
        padding: 0;
        width: 2.5rem;
        height: 2.5rem;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 0.25rem;
        transition: all 0.2s;
    }

    .modal-close:hover {
        background: #f3f4f6;
        color: #1f2937;
    }

    .modal-body {
        padding: 3rem;
        overflow-y: auto;
        font-size: 1.2rem;
    }

    .modal-body .markdown-content {
        color: #1f2937;
    }

    .modal-body .markdown-content h1 {
        font-size: 2.25rem;
        margin-top: 0;
    }

    .modal-body .markdown-content h2 {
        font-size: 1.875rem;
    }

    .modal-body .markdown-content h3 {
        font-size: 1.5rem;
    }

    .modal-body .markdown-content code {
        background: #f3f4f6;
        color: #dc2626;
        padding: 0.2em 0.4em;
        border-radius: 0.25rem;
        font-size: 0.9em;
    }

    .modal-body .markdown-content pre {
        background: #1f2937;
        color: #f9fafb;
        padding: 1.5rem;
        border-radius: 0.5rem;
        overflow-x: auto;
    }

    .modal-body .markdown-content pre code {
        background: none;
        color: inherit;
        padding: 0;
    }

    /* Toast Notification */
    .toast-container {
        position: fixed;
        bottom: 2rem;
        right: 2rem;
        z-index: 3000;
        animation: slideIn 0.3s ease-out;
    }

    @keyframes slideIn {
        from {
            transform: translateY(100%);
            opacity: 0;
        }
        to {
            transform: translateY(0);
            opacity: 1;
        }
    }

    .toast {
        background: white;
        border-radius: 1.5rem;
        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
        padding: 3rem 3.5rem;
        min-width: 600px;
        max-width: 750px;
        display: flex;
        align-items: flex-start;
        gap: 2rem;
        cursor: pointer;
        transition: all 0.2s;
        border: 4px solid #8b5cf6;
    }

    .toast:hover {
        transform: translateY(-3px);
        box-shadow: 0 15px 50px rgba(139, 92, 246, 0.3);
    }

    .toast-content {
        display: flex;
        gap: 2rem;
        flex: 1;
    }

    .toast-icon {
        font-size: 8rem;
        line-height: 1;
        flex-shrink: 0;
    }

    .toast-message {
        flex: 1;
    }

    .toast-title {
        font-weight: 700;
        font-size: 2rem;
        color: #1f2937;
        margin-bottom: 0.75rem;
        font-family: fantasy, 'Times New Roman', serif;
    }

    .toast-subtitle {
        font-size: 1.8rem;
        color: #6b7280;
        line-height: 1.6;
    }

    .toast-dismiss {
        background: none;
        border: none;
        font-size: 2.5rem;
        color: #9ca3af;
        cursor: pointer;
        padding: 0;
        width: 2.5rem;
        height: 2.5rem;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 0.25rem;
        transition: all 0.2s;
        flex-shrink: 0;
    }

    .toast-dismiss:hover {
        background: #f3f4f6;
        color: #1f2937;
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
        line-height: 1.2;
        word-wrap: break-word;
        word-break: break-word;
        overflow-wrap: break-word;
        hyphens: auto;
        max-width: 100%;
        font-family: Georgia, 'Times New Roman', serif;
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
        font-family: 'Courier New', 'Consolas', monospace;
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

        .help-dropdown {
            background: #4c2e70;
        }

        .help-menu-item:hover {
            background: rgba(255, 255, 255, 0.15);
        }

        .modal-content {
            background: #1f2937;
        }

        .modal-header {
            border-bottom-color: #4b5563;
        }

        .modal-header h2 {
            color: #f9fafb;
        }

        .modal-close {
            color: #9ca3af;
        }

        .modal-close:hover {
            background: #374151;
            color: #f9fafb;
        }

        .modal-body .markdown-content {
            color: #f9fafb;
        }

        .modal-body .markdown-content code {
            background: #374151;
            color: #fbbf24;
        }

        .modal-body .markdown-content pre {
            background: #111827;
            border: 1px solid #4b5563;
        }

        .toast {
            background: #1f2937;
            border-color: #8b5cf6;
        }

        .toast:hover {
            box-shadow: 0 15px 50px rgba(139, 92, 246, 0.4);
        }

        .toast-title {
            color: #f9fafb;
        }

        .toast-subtitle {
            color: #d1d5db;
        }

        .toast-dismiss {
            color: #6b7280;
        }

        .toast-dismiss:hover {
            background: #374151;
            color: #f9fafb;
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
