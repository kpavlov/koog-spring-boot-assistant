<script lang="ts">
    import {User} from 'lucide-svelte';
    import type {Message} from './ChatMessages.svelte';

    export let message: Message;
    export let baseUrl: string;
    export let renderMarkdown: (text: string) => string;
</script>

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
            {/if}
            {#if !message.text || message.completed === false }
                <div class="updating-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
            {/if}
        </div>
        <div class="message-time">
            {message.timestamp.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})}
        </div>
    </div>
</div>

<style>
    .message-wrapper {
        display: flex;
        margin-bottom: 2rem;
        max-width: 80%;
        align-items: flex-end;
    }

    .message-wrapper.user-message {
        margin-left: auto;
        flex-direction: row-reverse;
    }

    .message-wrapper.ai-message {
        margin-right: auto;
    }

    .avatar {
        width: var(--size-avatar, 6rem);
        height: var(--size-avatar, 6rem);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 1rem 0 0;
        flex-shrink: 0;
    }

    .user-message .avatar {
        background: var(--color-user-bubble, #3b82f6);
        color: white;
        margin: 0 0 0 1rem;
    }

    .ai-message .avatar {
        background: var(--color-accent, #8b5cf6);
        color: white;
        overflow: hidden;
        margin: 0 1rem 0 0;
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
        padding: var(--spacing-message-bubble, 2rem);
        border-radius: 1.5rem;
        font-size: var(--fs-message-bubble, 1.8rem);
        line-height: 1.2;
        word-wrap: break-word;
        word-break: break-word;
        overflow-wrap: break-word;
        hyphens: auto;
        max-width: 100%;
        font-family: var(--font-serif, Georgia, 'Times New Roman', serif);
    }

    .user-message .message-bubble {
        background: var(--color-user-bubble, #3b82f6);
        color: white;
        border-bottom-right-radius: 0.5rem;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    .ai-message .message-bubble {
        background: var(--color-ai-bubble-bg, white);
        color: var(--color-ai-bubble-text, #1f2937);
        border: 1px solid var(--color-border, #e5e7eb);
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
        background: var(--color-code-bg, #f3f4f6);
        padding: 0.2em 0.4em;
        border-radius: 0.3rem;
        font-family: var(--font-mono, 'Monaco', 'Menlo', 'Ubuntu Mono', monospace);
        font-size: 0.9em;
    }

    .ai-message .markdown-content code {
        background: var(--color-code-bg, #f3f4f6);
        color: var(--color-code-text, #1f2937);
    }

    .user-message .markdown-content code {
        background: rgba(255, 255, 255, 0.2);
        color: white;
    }

    .markdown-content pre {
        background: var(--color-code-bg, #f3f4f6);
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
        background-color: var(--color-text-secondary, #6b7280);
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

    .updating-indicator {
        display: flex;
        align-items: center;
        gap: 4px;
        margin-top: 0.8rem;
        margin-bottom: 1rem;
        padding-top: 0.8rem;
    }

    .updating-indicator span {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background-color: var(--color-text-secondary, #6b7280);
        animation: dot-bounce 1.4s infinite ease-in-out both;
    }

    .updating-indicator span:nth-child(1) {
        animation-delay: -0.32s;
    }

    .updating-indicator span:nth-child(2) {
        animation-delay: -0.16s;
    }

    .message-time {
        display: none; /* hide it temporary */
        font-size: 1.5rem;
        color: var(--color-text-secondary, #6b7280);
        margin-top: 0.5rem;
        padding: 0 0.5rem;
    }

    .user-message .message-time {
        text-align: right;
    }
</style>