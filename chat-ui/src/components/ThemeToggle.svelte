<script lang="ts">
    import { onMount } from 'svelte';
    import { Sun, Moon, SunMoon } from 'lucide-svelte';

    type Theme = 'light' | 'dark' | 'auto';
    let currentTheme: Theme = 'auto';

    function setTheme(theme: Theme) {
        currentTheme = theme;
        localStorage.setItem('theme', theme);

        const root = document.documentElement;
        if (theme === 'auto') {
            root.removeAttribute('data-theme');
        } else {
            root.setAttribute('data-theme', theme);
        }
    }

    onMount(() => {
        const savedTheme = localStorage.getItem('theme') as Theme;
        if (savedTheme) {
            currentTheme = savedTheme;
            if (savedTheme !== 'auto') {
                document.documentElement.setAttribute('data-theme', savedTheme);
            }
        }
    });

    function cycleTheme() {
        const themes: Theme[] = ['light', 'dark', 'auto'];
        const currentIndex = themes.indexOf(currentTheme);
        const nextTheme = themes[(currentIndex + 1) % themes.length];
        setTheme(nextTheme);
    }
</script>

<button
    class="theme-toggle"
    on:click={cycleTheme}
    title="{currentTheme === 'light' ? 'Light theme' : currentTheme === 'dark' ? 'Dark theme' : 'Auto theme'}"
>
    {#if currentTheme === 'light'}
        <Sun size={20} />
    {:else if currentTheme === 'dark'}
        <Moon size={20} />
    {:else}
        <SunMoon size={20} />
    {/if}
    <span class="theme-label">{currentTheme}</span>
</button>

<style>
    .theme-toggle {
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

    .theme-toggle:hover {
        background: rgba(255, 255, 255, 0.1);
        opacity: 1;
    }

    .theme-label {
        text-transform: capitalize;
        min-width: 4rem;
        text-align: left;
    }

    /* Hide labels on smaller screens */
    @media (max-width: 1400px) {
        .theme-label {
            display: none;
        }

        .theme-toggle {
            padding: 0.5rem;
        }
    }
</style>
