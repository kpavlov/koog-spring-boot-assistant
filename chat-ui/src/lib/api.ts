// API service for connecting to Spring Boot backend at 127.0.0.1:8080

const API_BASE_URL = 'http://127.0.0.1:8080';

export interface ChatRequest {
  message: string;
  sessionId?: string | null;
}

export interface Answer {
  message: string;
  sessionId: string;
}

export class ApiError extends Error {
  constructor(message: string, public status?: number) {
    super(message);
    this.name = 'ApiError';
  }
}

export async function getApiVersion(): Promise<string> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/version`);
    
    if (!response.ok) {
      throw new ApiError(`Failed to get API version: ${response.statusText}`, response.status);
    }
    
    return await response.text();
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(`Network error: ${error instanceof Error ? error.message : 'Unknown error'}`);
  }
}

export async function sendChatMessage(request: ChatRequest): Promise<Answer> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    
    if (!response.ok) {
      throw new ApiError(`Chat request failed: ${response.statusText}`, response.status);
    }
    
    const answer: Answer = await response.json();
    return answer;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(`Network error: ${error instanceof Error ? error.message : 'Unknown error'}`);
  }
}