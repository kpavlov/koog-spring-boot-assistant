<script lang="ts">
    import {Send, Loader2} from 'lucide-svelte';

    export let currentMessage: string;
    export let isLoading: boolean;
    export let isServerOnline: boolean;
    export let onSend: () => void;
    export let onMessageChange: (message: string) => void;

    let messageInput: HTMLTextAreaElement;

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

    function handleKeyPress(event: KeyboardEvent) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            onSend();
        }
    }

    function handleInput(event: Event) {
        const target = event.target as HTMLTextAreaElement;
        onMessageChange(target.value);
    }

    export function focus() {
        messageInput?.focus();
    }

    export function getInputElement() {
        return messageInput;
    }
</script>

<div class="input-container {isLoading ? 'disabled' : ''}">
    <div class="input-wrapper">
        <textarea
            bind:this={messageInput}
            value={currentMessage}
            on:input={handleInput}
            on:keypress={handleKeyPress}
            placeholder="Type your message here..."
            rows="1"
            disabled={isLoading}
            class="message-input"
            tabindex="1"
        ></textarea>
        <button
            on:click={onSend}
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

<style>
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

    @media (prefers-color-scheme: dark) {
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
    }

    @media (min-width: 1920px) {
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
