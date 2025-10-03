import { writable } from 'svelte/store';

const SESSION_ID_KEY = 'chat_session_id';

function generateSessionId(): string {
  return `session_${Date.now()}_${Math.random().toString(36).substring(2, 15)}`;
}

function getStoredSessionId(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(SESSION_ID_KEY);
}

function storeSessionId(id: string): void {
  if (typeof window === 'undefined') return;
  localStorage.setItem(SESSION_ID_KEY, id);
}

function initializeSessionId(): string {
  const stored = getStoredSessionId();
  if (stored) {
    return stored;
  }

  const newId = generateSessionId();
  storeSessionId(newId);
  return newId;
}

// Create a writable store for session ID
function createSessionStore() {
  const { subscribe, set } = writable<string>(initializeSessionId());

  return {
    subscribe,
    update: (id: string) => {
      storeSessionId(id);
      set(id);
    },
    reset: () => {
      const newId = generateSessionId();
      storeSessionId(newId);
      set(newId);
    }
  };
}

export const sessionId = createSessionStore();
