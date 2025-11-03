import axios from "axios"
import Cookies from "js-cookie"

const BASE_URL = "http://localhost:23456"

// --- INTERFACES ---
export interface PaginatedResponse<T> {
  content: T[]
  totalPages: number
  totalElements: number
  number: number
  size: number
}

export interface User {
  id: string
  name: string
  email: string
  active?: boolean
  createdAt?: string
  role?: string
}

export interface LoginPayload {
  email: string
  password: string
}

export interface RegisterPayload {
  name: string
  email: string
  password: string
}

export interface AuthResponse {
  token?: string
  user?: User
  message?: string
}

// --- CONFIGURAÇÃO DO AXIOS ---
export const api = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
})

// --- SCHEDULE COMMAND API ---
export interface ScheduleCommandPayload {
  scheduledFor: string // ISO string, ex: "2025-11-03T15:41:00"
  allHostsCommand?: { command: string } | null
  groupCommand?: { group: string; command: string } | null
  hostCommand?: { hostname: string; command: string } | null
}

/**
 * Agenda um comando para host(s) específicos, grupo ou todos.
 */
export async function scheduleCommand(payload: ScheduleCommandPayload) {
  try {
    const { data } = await api.post("/api/manager/admin/schedule_command", payload)
    return data
  } catch (error) {
    throw handleError(error, "Erro ao agendar comando")
  }
}


// Adiciona o token automaticamente às requisições
api.interceptors.request.use(
  (config) => {
    const token = Cookies.get("auth_token")
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// --- FUNÇÃO DE ERRO PADRONIZADA ---
function handleError(error: unknown, defaultMessage: string): Error {
  if (axios.isAxiosError(error) && error.response?.data?.message) {
    return new Error(error.response.data.message)
  }
  return new Error(defaultMessage)
}

// --- AUTH API ---
export async function login(payload: LoginPayload): Promise<AuthResponse> {
  try {
    const { data } = await api.post<AuthResponse>("/auth/login", payload)
    return data
  } catch (error) {
    throw handleError(error, "Erro ao fazer login")
  }
}

export async function register(payload: RegisterPayload): Promise<AuthResponse> {
  // Limpa qualquer token antigo antes de registrar
  Cookies.remove("auth_token")

  try {
    const { data } = await api.post<AuthResponse>("/users", payload)
    return data
  } catch (error) {
    throw handleError(error, "Erro ao registrar usuário")
  }
}

export async function getMe(): Promise<User> {
  const token = Cookies.get("auth_token")
  if (!token) throw new Error("Usuário não autenticado")

  try {
    const { data } = await api.get<User>("/auth/me")
    return data
  } catch (error) {
    throw handleError(error, "Sessão inválida ou expirada")
  }
}

// --- USERS API ---
export async function getPendingUsers(page = 0, size = 20) {
  try {
    const { data } = await api.get<PaginatedResponse<User>>("/users/pending", {
      params: { page, size },
    })
    return data
  } catch (error) {
    throw handleError(error, "Erro ao buscar usuários pendentes")
  }
}

export async function getActiveUsers(page = 0, size = 20) {
  try {
    const { data } = await api.get<PaginatedResponse<User>>("/users", {
      params: { page, size },
    })
    return data
  } catch (error) {
    throw handleError(error, "Erro ao buscar usuários ativos")
  }
}

export async function activateUser(id: string) {
  try {
    await api.post(`/users/${id}/activate`)
  } catch (error) {
    throw handleError(error, "Erro ao ativar/desativar usuário")
  }
}

export async function updateUser(id: string, payload: Partial<RegisterPayload>) {
  try {
    const { data } = await api.patch<User>(`/users/${id}`, payload)
    return data
  } catch (error) {
    throw handleError(error, "Erro ao atualizar usuário")
  }
}

export async function deleteUser(id: string) {
  try {
    await api.delete(`/users/${id}`)
  } catch (error) {
    throw handleError(error, "Erro ao deletar usuário")
  }
}
