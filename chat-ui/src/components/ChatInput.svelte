<script lang="ts">

    export let currentMessage: string;
    export let isLoading: boolean;
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
        <div class="textarea-container">
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
        </div>
    </div>
</div>

<style>
    .input-container {
        background: white;
        border-top: 2px solid #e5e7eb;
        padding: var(--spacing-input);
    }

    .input-container.disabled {
        opacity: 0.7;
        pointer-events: none;
    }

    .input-wrapper {
        display: flex;
        max-width: 1400px;
        margin: 0 auto;
        align-items: end;
    }

    .textarea-container {
        position: relative;
        flex: 1;
        width: 100%;
    }

    .message-input {
        width: 100%;
        border: 4px solid #e5e7eb;
        border-radius: 1.5rem;
        padding: var(--spacing-input-padding);
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
            padding: var(--spacing-input);
        }

        .message-input {
            padding: var(--spacing-input-padding);
            font-size: var(--fs-input);
            min-height: var(--size-input-min-height);
        }
    }
</style>
