<script context="module" lang="ts">
    export interface Message {
        id: string;
        text: string;
        isUser: boolean;
        timestamp: Date;
        requestId?: string;
        completed?: boolean;
    }
</script>

<script lang="ts">
    import {User} from 'lucide-svelte';
    import {afterUpdate} from 'svelte';
    import ChatMessageView from './ChatMessageView.svelte';

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
        <ChatMessageView {message} {baseUrl} {renderMarkdown} />
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

    @media (prefers-color-scheme: dark) {
        .chat-container {
            background: #111827;
        }
    }
</style>