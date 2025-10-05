<script lang="ts">
    import {HelpCircle, ChevronDown, Loader2} from 'lucide-svelte';

    export let baseUrl: string;
    export let isServerOnline: boolean;
    export let serverVersion: string;
    export let isLoading: boolean;
    export let onStatusClick: () => void;
    export let onOpenGitHub: () => void;
    export let onOpenApiDocs: () => void;
    export let onShowInstructions: () => void;

    let showHelpMenu = false;

    function toggleHelpMenu() {
        showHelpMenu = !showHelpMenu;
    }

    function handleClickOutside(event: MouseEvent) {
        const target = event.target as HTMLElement;
        if (!target.closest('.help-menu-container')) {
            showHelpMenu = false;
        }
    }

    function handleGitHub() {
        onOpenGitHub();
        showHelpMenu = false;
    }

    function handleApiDocs() {
        onOpenApiDocs();
        showHelpMenu = false;
    }

    function handleInstructions() {
        onShowInstructions();
        showHelpMenu = false;
    }
</script>

<svelte:document on:click={handleClickOutside}/>

<header class="chat-header">
    <div class="header-content">
        <div class="ai-indicator">
            <img src="{baseUrl}logo.png" alt="Elven Assistant" class="header-avatar"/>
            <span>Elven Assistant</span>
        </div>
        <div class="header-right">
            <div class="status {!isServerOnline ? 'clickable' : ''}" on:click={onStatusClick}>
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
                        <button class="help-menu-item" on:click={handleGitHub}>
                            View Project on GitHub
                        </button>
                        <button class="help-menu-item" on:click={handleApiDocs}>
                            OpenAPI Specification
                        </button>
                        <button class="help-menu-item" on:click={handleInstructions}>
                            Setup Instructions
                        </button>
                    </div>
                {/if}
            </div>
        </div>
    </div>
</header>

<style>
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

    .help-button :global(.chevron) {
        transition: transform 0.2s;
    }

    .help-button :global(.chevron.open) {
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

    @keyframes pulse {
        0%, 100% {
            opacity: 1;
        }
        50% {
            opacity: 0.5;
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

    @media (prefers-color-scheme: dark) {
        .help-dropdown {
            background: #4c2e70;
        }

        .help-menu-item:hover {
            background: rgba(255, 255, 255, 0.15);
        }
    }

    @media (min-width: 1920px) {
        .chat-header {
            padding: 2rem 3rem;
        }

        .ai-indicator {
            font-size: 2.25rem;
        }
    }
</style>
