<script lang="ts">
  import { onMount, afterUpdate } from 'svelte';
  import { Send, Bot, User, Loader2 } from 'lucide-svelte';
  import { sendChatMessage, getApiVersion, ApiError } from './lib/api';

  interface Message {
    id: string;
    text: string;
    isUser: boolean;
    timestamp: Date;
  }

  let messages: Message[] = [
    {
      id: '1',
      text: 'Hello! I\'m your AI assistant. How can I help you today?',
      isUser: false,
      timestamp: new Date()
    }
  ];

  let currentMessage = '';
  let isLoading = false;
  let chatContainer: HTMLElement;
  let messageInput: HTMLInputElement;
  let sessionId: string | null = null;

  // Auto-scroll functionality
  function scrollToBottom() {
    if (chatContainer) {
      chatContainer.scrollTop = chatContainer.scrollHeight;
    }
  }

  afterUpdate(() => {
    scrollToBottom();
  });

  // Send message to AI backend
  async function sendMessage() {
    if (!currentMessage.trim() || isLoading) return;

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

    // Block UI for 5 seconds or until response
    const timeout = setTimeout(() => {
      isLoading = false;
    }, 5000);

    try {
      const response = await sendChatMessage({
        message: messageText,
        sessionId: sessionId
      });
      
      clearTimeout(timeout);
      
      // Update sessionId from response
      sessionId = response.sessionId;
      
      // Update the loading message with the actual response
      messages = messages.map(msg => 
        msg.id === loadingMessageId 
          ? { ...msg, text: response.message, timestamp: new Date() }
          : msg
      );
      
      isLoading = false;
      
      // Focus input field after receiving response
      messageInput?.focus();
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
          ? { ...msg, text: errorText, timestamp: new Date() }
          : msg
      );
      
      // Focus input field after error message
      messageInput?.focus();
    }
  }

  function handleKeyPress(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      sendMessage();
    }
  }

  onMount(() => {
    messageInput?.focus();
  });
</script>

<main class="chat-app">
  <!-- Header -->
  <header class="chat-header">
    <div class="header-content">
      <div class="ai-indicator">
        <Bot size={32} />
        <span>AI Assistant</span>
      </div>
      <div class="status">
        {#if isLoading}
          <Loader2 size={20} class="animate-spin" />
          <span>AI is thinking...</span>
        {:else}
          <div class="status-dot"></div>
          <span>Online</span>
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
            <User size={24} />
          {:else}
            <Bot size={24} />
          {/if}
        </div>
        <div class="message-content">
          <div class="message-bubble">
            {#if message.text}
              <p>{message.text}</p>
            {:else if !message.isUser}
              <div class="loading-dots">
                <span></span>
                <span></span>
                <span></span>
              </div>
            {:else}
              <p>{message.text}</p>
            {/if}
          </div>
          <div class="message-time">
            {message.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
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
        disabled={isLoading || !currentMessage.trim()}
        class="send-button"
      >
        {#if isLoading}
          <Loader2 size={24} class="animate-spin" />
        {:else}
          <Send size={24} />
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
    max-width: 100vw;
    margin: 0 auto;
    border-radius: 0;
    overflow: hidden;
    box-shadow: 0 0 50px rgba(0, 0, 0, 0.1);
  }

  /* Header */
  .chat-header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 1.5rem 2rem;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
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
    font-size: 1.25rem;
    font-weight: 600;
  }

  .status {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.9rem;
    opacity: 0.9;
  }

  .status-dot {
    width: 8px;
    height: 8px;
    background: #10b981;
    border-radius: 50%;
    animation: pulse 2s infinite;
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
    width: 3rem;
    height: 3rem;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 1rem;
    flex-shrink: 0;
  }

  .user-message .avatar {
    background: #3b82f6;
    color: white;
  }

  .ai-message .avatar {
    background: #8b5cf6;
    color: white;
  }

  .message-content {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  .message-bubble {
    padding: 1.25rem 1.75rem;
    border-radius: 1.5rem;
    font-size: 1.1rem;
    line-height: 1.6;
    word-wrap: break-word;
    max-width: 100%;
  }

  .user-message .message-bubble {
    background: #3b82f6;
    color: white;
    border-bottom-right-radius: 0.5rem;
  }

  .ai-message .message-bubble {
    background: white;
    color: #1f2937;
    border: 1px solid #e5e7eb;
    border-bottom-left-radius: 0.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .message-bubble p {
    margin: 0;
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
    font-size: 0.8rem;
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
    border-top: 1px solid #e5e7eb;
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
    border: 2px solid #e5e7eb;
    border-radius: 1.5rem;
    padding: 1.25rem 1.75rem;
    font-size: 1.1rem;
    font-family: inherit;
    resize: none;
    outline: none;
    transition: border-color 0.2s;
    min-height: 3.5rem;
    max-height: 8rem;
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
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }

  :global(.animate-spin) {
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
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
      font-size: 1.5rem;
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
