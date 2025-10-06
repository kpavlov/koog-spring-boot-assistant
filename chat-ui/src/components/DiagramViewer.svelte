<script lang="ts">
    import {createEventDispatcher, onDestroy, onMount} from 'svelte';
    import mermaid from 'mermaid';
    import panzoom, {type PanZoom} from 'panzoom';

    export let title: string = 'Diagram';
    export let diagramFetcher: () => Promise<string>;
    export let isOpen: boolean = false;
    export let modalId: string = 'diagram-viewer';

    const dispatch = createEventDispatcher();

    let diagramContent = '';
    let panzoomInstance: PanZoom | null = null;
    let isLoading = false;
    let error = '';
    let currentTheme = '';

    // Function to detect the current effective theme
    function getCurrentTheme(): 'light' | 'dark' {
        const dataTheme = document.documentElement.getAttribute('data-theme');
        if (dataTheme === 'light' || dataTheme === 'dark') {
            return dataTheme;
        }
        // For 'auto' theme or no theme set, check system preference
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    // Function to get Mermaid theme based on current theme
    function getMermaidTheme(theme: 'light' | 'dark'): "dark" | "default" {
        return theme === 'dark' ? 'dark' : 'default';
    }

    async function loadDiagram() {
        const theme = getCurrentTheme();
        const mermaidTheme = getMermaidTheme(theme);

        // Check if we need to re-render due to theme change
        if (diagramContent && currentTheme === mermaidTheme) {
            return; // Already loaded with correct theme
        }

        isLoading = true;
        error = '';
        currentTheme = mermaidTheme;

        // Reinitialize Mermaid with the current theme
        mermaid.initialize({
            startOnLoad: false,
            theme: mermaidTheme,
            securityLevel: 'loose',
        });

        try {
            const rawContent = await diagramFetcher();
            // Render the Mermaid diagram
            const {svg} = await mermaid.render(`${modalId}-graph`, rawContent);
            diagramContent = svg;
        } catch (loadError) {
            console.error('Failed to load diagram:', loadError);
            try {
                const {svg} = await mermaid.render(`${modalId}-error`, 'graph TD\n    Error["Failed to load diagram"]');
                diagramContent = svg;
            } catch (renderError) {
                error = 'Failed to load diagram';
            }
        } finally {
            isLoading = false;
        }
    }

    function initializePanzoom() {
        // Initialize panzoom after the modal is rendered
        setTimeout(() => {
            const diagramElement = document.querySelector(`#${modalId} .diagram-content svg`);
            if (diagramElement && !panzoomInstance) {
                panzoomInstance = panzoom(diagramElement as SVGElement, {
                    maxZoom: 5,
                    minZoom: 0.1,
                    initialZoom: 1,
                    smoothScroll: false,
                    bounds: true,
                    boundsPadding: 0.1
                });
            }
        }, 100);
    }

    function cleanup() {
        if (panzoomInstance) {
            panzoomInstance.dispose();
            panzoomInstance = null;
        }
    }

    function handleClose() {
        cleanup();
        dispatch('close');
    }

    // Handle modal open/close and theme changes
    $: if (isOpen) {
        loadDiagram().then(() => {
            if (diagramContent && !error) {
                initializePanzoom();
            }
        });
    } else {
        cleanup();
    }

    // Reactive logic to detect theme changes and force re-render
    $: if (typeof window !== 'undefined') {
        const theme = getCurrentTheme();
        const mermaidTheme = getMermaidTheme(theme);
        if (isOpen && currentTheme && currentTheme !== mermaidTheme) {
            // Theme changed, clear content to force re-render
            diagramContent = '';
            loadDiagram().then(() => {
                if (diagramContent && !error) {
                    initializePanzoom();
                }
            });
        }
    }

    onMount(() => {
        // Listen for system theme changes when in auto mode
        const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
        const handleThemeChange = () => {
            if (isOpen && document.documentElement.getAttribute('data-theme') === null) {
                // Only react to system theme changes when in auto mode
                diagramContent = '';
                loadDiagram().then(() => {
                    if (diagramContent && !error) {
                        initializePanzoom();
                    }
                });
            }
        };

        mediaQuery.addEventListener('change', handleThemeChange);

        return () => {
            mediaQuery.removeEventListener('change', handleThemeChange);
        };
    });

    onDestroy(() => {
        cleanup();
    });
</script>

{#if isOpen}
    <div class="modal-overlay" on:click={handleClose}>
        <div class="diagram-modal-content" on:click={(e) => e.stopPropagation()}>
            <div class="diagram-modal-header">
                <h2>{title}</h2>
                <div class="diagram-controls">
                    <div class="zoom-info">Mouse wheel to zoom, drag to pan</div>
                    <button class="modal-close" on:click={handleClose}>✕</button>
                </div>
            </div>
            <div class="diagram-modal-body">
                <div class="diagram-container" id={modalId}>
                    {#if isLoading}
                        <div class="loading-message">Loading diagram...</div>
                    {:else if error}
                        <div class="error-message">{error}</div>
                    {:else if diagramContent}
                        <div class="diagram-content">
                            {@html diagramContent}
                        </div>
                    {:else}
                        <div class="loading-message">Preparing diagram...</div>
                    {/if}
                </div>
            </div>
        </div>
    </div>
{/if}

<style>
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

    /* Diagram Modal - Large version for diagrams */
    .diagram-modal-content {
        background: var(--color-modal-bg);
        border-radius: 1rem;
        width: 95%;
        height: 95vh;
        display: flex;
        flex-direction: column;
        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
        max-width: none;
        max-height: none;
    }

    .diagram-modal-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1.5rem 2rem;
        border-bottom: 2px solid var(--color-modal-border);
        flex-shrink: 0;
    }

    .diagram-modal-header h2 {
        margin: 0;
        font-size: 1.8rem;
        color: var(--color-modal-text);
    }

    .diagram-controls {
        display: flex;
        align-items: center;
        gap: 1rem;
    }

    .zoom-info {
        font-size: 0.9rem;
        color: var(--color-text-secondary);
        font-style: italic;
    }

    .modal-close {
        background: none;
        border: none;
        font-size: var(--fs-modal-close);
        color: var(--color-text-secondary);
        cursor: pointer;
        padding: 0;
        width: var(--size-modal-close);
        height: var(--size-modal-close);
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 0.25rem;
        transition: all 0.2s;
    }

    .modal-close:hover {
        background: var(--color-code-bg);
        color: var(--color-modal-text);
    }

    .diagram-modal-body {
        flex: 1;
        padding: 1rem;
        overflow: hidden;
        display: flex;
        flex-direction: column;
    }

    .diagram-container {
        flex: 1;
        display: flex;
        justify-content: center;
        align-items: center;
        width: 100%;
        height: 100%;
        overflow: hidden;
        border: 1px solid var(--color-modal-border);
        border-radius: 0.5rem;
        background: var(--color-chat-bg);
    }

    .diagram-content {
        width: 100%;
        height: 100%;
        display: flex;
        justify-content: center;
        align-items: center;
        cursor: grab;
    }

    .diagram-content:active {
        cursor: grabbing;
    }

    .diagram-content :global(svg) {
        max-width: none;
        max-height: none;
        width: auto;
        height: auto;
    }

    .loading-message {
        color: var(--color-text-secondary);
        font-size: 1.2rem;
        text-align: center;
    }

    .error-message {
        color: #dc2626;
        font-size: 1.2rem;
        text-align: center;
        padding: 2rem;
        background: #fef2f2;
        border-radius: 0.5rem;
        border: 1px solid #fecaca;
    }

</style>