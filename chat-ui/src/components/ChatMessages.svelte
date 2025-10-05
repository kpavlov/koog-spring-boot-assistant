<script context="module" lang="ts">
    export interface Message {
        id: string;
        text: string;
        isUser: boolean;
        timestamp: Date;
    }
</script>

<script lang="ts">
    import {User} from 'lucide-svelte';
    import {afterUpdate} from 'svelte';

    export let messages: Message[];
    export let baseUrl: string;
    export let renderMarkdown: (text: string) => string;

    let chatContainer: HTMLElement;

    function scrollToBottom() {
        if (chatContainer) {
            chatContainer.scrollTop = chatContainer.scrollHeight;
        }
    }

    afterUpdate(() => {
        scrollToBottom();
    });

    export function getContainer() {
        return chatContainer;
    }
</script>

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

<style>
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

    .markdown-content :global(p) {
        margin: 0 0 1rem 0;
    }

    .markdown-content :global(p:last-child) {
        margin-bottom: 0;
    }

    .markdown-content :global(h1),
    .markdown-content :global(h2),
    .markdown-content :global(h3),
    .markdown-content :global(h4),
    .markdown-content :global(h5),
    .markdown-content :global(h6) {
        margin: 1.5rem 0 1rem 0;
        font-weight: 600;
    }

    .markdown-content :global(h1:first-child),
    .markdown-content :global(h2:first-child),
    .markdown-content :global(h3:first-child) {
        margin-top: 0;
    }

    .markdown-content :global(code) {
        background: rgba(0, 0, 0, 0.05);
        padding: 0.2em 0.4em;
        border-radius: 0.3rem;
        font-family: 'Courier New', 'Consolas', monospace;
        font-size: 0.9em;
    }

    .ai-message .markdown-content :global(code) {
        background: rgba(0, 0, 0, 0.05);
    }

    .user-message .markdown-content :global(code) {
        background: rgba(255, 255, 255, 0.2);
    }

    .markdown-content :global(pre) {
        background: rgba(0, 0, 0, 0.05);
        padding: 1rem;
        border-radius: 0.5rem;
        overflow-x: auto;
        margin: 1rem 0;
    }

    .markdown-content :global(pre code) {
        background: none;
        padding: 0;
    }

    .markdown-content :global(ul),
    .markdown-content :global(ol) {
        margin: 1rem 0;
        padding-left: 2rem;
    }

    .markdown-content :global(li) {
        margin: 0.5rem 0;
    }

    .markdown-content :global(blockquote) {
        border-left: 4px solid rgba(0, 0, 0, 0.2);
        padding-left: 1rem;
        margin: 1rem 0;
        font-style: italic;
    }

    .markdown-content :global(a) {
        color: inherit;
        text-decoration: underline;
    }

    .markdown-content :global(strong) {
        font-weight: 700;
    }

    .markdown-content :global(em) {
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
        display: none;
        font-size: 1.5rem;
        color: #6b7280;
        margin-top: 0.5rem;
        padding: 0 0.5rem;
    }

    .user-message .message-time {
        text-align: right;
    }

    @media (prefers-color-scheme: dark) {
        .chat-container {
            background: #111827;
        }

        .ai-message .message-bubble {
            background: #374151;
            color: #f9fafb;
            border-color: #4b5563;
        }

        .message-time {
            color: #9ca3af;
        }
    }

    @media (min-width: 1920px) {
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
    }
</style>
