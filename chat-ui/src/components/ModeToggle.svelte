<script lang="ts">
    import { onMount } from 'svelte';
    import { Presentation, Type } from 'lucide-svelte';

    type Mode = 'normal' | 'presentation';
    let currentMode: Mode = 'normal';

    function toggleMode() {
        const newMode: Mode = currentMode === 'normal' ? 'presentation' : 'normal';
        currentMode = newMode;
        localStorage.setItem('display-mode', newMode);

        const root = document.documentElement;
        if (newMode === 'presentation') {
            root.setAttribute('data-mode', 'presentation');
        } else {
            root.removeAttribute('data-mode');
        }
    }

    onMount(() => {
        const savedMode = localStorage.getItem('display-mode') as Mode;
        if (savedMode) {
            currentMode = savedMode;
            if (savedMode === 'presentation') {
                document.documentElement.setAttribute('data-mode', 'presentation');
            }
        }
    });
</script>

<button
    class="mode-toggle"
    on:click={toggleMode}
    title="{currentMode === 'presentation' ? 'Presentation mode' : 'Normal mode'}"
>
    {#if currentMode === 'presentation'}
        <Presentation size={20} />
    {:else}
        <Type size={20} />
    {/if}
    <span class="mode-label">{currentMode === 'presentation' ? 'Present' : 'Normal'}</span>
</button>

<style>
    .mode-toggle {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        background: transparent;
        border: none;
        color: white;
        padding: 0.5rem 1rem;
        border-radius: 0.5rem;
        cursor: pointer;
        transition: all 0.2s;
        font-size: 1rem;
        opacity: 0.7;
    }

    .mode-toggle:hover {
        background: rgba(255, 255, 255, 0.1);
        opacity: 1;
    }

    .mode-label {
        min-width: 4rem;
        text-align: left;
    }

    /* Hide labels on smaller screens */
    @media (max-width: 1400px) {
        .mode-label {
            display: none;
        }

        .mode-toggle {
            padding: 0.5rem;
        }
    }
</style>
