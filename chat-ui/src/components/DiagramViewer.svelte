<script lang="ts">
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import mermaid from 'mermaid';
    import panzoom, { type PanZoom } from 'panzoom';

    export let title: string = 'Diagram';
    export let diagramFetcher: () => Promise<string>;
    export let isOpen: boolean = false;
    export let modalId: string = 'diagram-viewer';

    const dispatch = createEventDispatcher();

    let diagramContent = '';
    let panzoomInstance: PanZoom | null = null;
    let isLoading = false;
    let error = '';

    async function loadDiagram() {
        if (diagramContent) return; // Already loaded

        isLoading = true;
        error = '';
        
        try {
            const rawContent = await diagramFetcher();
            // Render the Mermaid diagram
            const { svg } = await mermaid.render(`${modalId}-graph`, rawContent);
            diagramContent = svg;
        } catch (loadError) {
            console.error('Failed to load diagram:', loadError);
            try {
                const { svg } = await mermaid.render(`${modalId}-error`, 'graph TD\n    Error["Failed to load diagram"]');
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

    // Handle modal open/close
    $: if (isOpen) {
        loadDiagram().then(() => {
            if (diagramContent && !error) {
                initializePanzoom();
            }
        });
    } else {
        cleanup();
    }

    onMount(() => {
        // Initialize Mermaid if not already done
        mermaid.initialize({
            startOnLoad: false,
            theme: 'default',
            securityLevel: 'loose',
        });
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
        background: white;
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
        border-bottom: 2px solid #e5e7eb;
        flex-shrink: 0;
    }

    .diagram-modal-header h2 {
        margin: 0;
        font-size: 1.8rem;
        color: #1f2937;
    }

    .diagram-controls {
        display: flex;
        align-items: center;
        gap: 1rem;
    }

    .zoom-info {
        font-size: 0.9rem;
        color: #6b7280;
        font-style: italic;
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
        border: 1px solid #e5e7eb;
        border-radius: 0.5rem;
        background: #f9fafb;
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
        color: #6b7280;
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

    /* Dark mode */
    @media (prefers-color-scheme: dark) {
        .diagram-modal-content {
            background: #1f2937;
        }

        .diagram-modal-header {
            border-bottom-color: #4b5563;
        }

        .diagram-modal-header h2 {
            color: #f9fafb;
        }

        .zoom-info {
            color: #9ca3af;
        }

        .modal-close {
            color: #9ca3af;
        }

        .modal-close:hover {
            background: #374151;
            color: #f9fafb;
        }

        .diagram-container {
            background: #111827;
            border-color: #4b5563;
        }

        .loading-message {
            color: #9ca3af;
        }

        .error-message {
            background: #7f1d1d;
            color: #fca5a5;
            border-color: #991b1b;
        }
    }
</style>