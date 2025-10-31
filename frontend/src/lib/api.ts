import axios from "axios"
import Cookies from "js-cookie"

const BASE_URL = "http://localhost:23456"

// --- INTERFACES ---

// Interface de paginação que estava faltando
export interface PaginatedResponse<T> {
  content: T[]
  totalPages: number
  totalElements: number
  number: number
  size: number
  // Adicione outras propriedades de paginação se necessário
}

export type ResponsePadrao<T> = {
  content?: T
  message?: string
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
  token: string
  user?: User // O usuário é opcional no login, mas idealmente é retornado
  message?: string
}

export interface ApiError {
  message: string
}

// --- CONFIGURAÇÃO DO AXIOS ---

export const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
})

// Interceptor para adicionar o token
api.interceptors.request.use(
  (config) => {
    const token = Cookies.get("auth_token")
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// --- FUNÇÕES DA API ---

/**
 * Trata erros do Axios e retorna uma mensagem de erro padronizada.
 * CORRIGIDO: Agora propaga o erro com a mensagem da API.
 */
function handleError(error: unknown, defaultMessage: string): Error {
  if (axios.isAxiosError(error) && error.response?.data?.message) {
    return new Error(error.response.data.message)
  }
  return new Error(defaultMessage)
}

// Auth API
export async function login(payload: LoginPayload): Promise<AuthResponse> {
  try {
    const { data } = await api.post<AuthResponse>("/auth/login", payload)
    return data
  } catch (error) {
    throw handleError(error, "Erro ao fazer login")
  }
}

export async function register(payload: RegisterPayload): Promise<AuthResponse> {
  try {
    const { data } = await api.post<AuthResponse>("/users", payload)
    return data
  } catch (error) {
    throw handleError(error, "Erro ao registrar usuário")
  }
}

/**
 * NOVO: Função para buscar dados do usuário logado (validar o token)
 * Presume que existe um endpoint /auth/me ou /users/me
 */
export async function getMe(): Promise<User> {
  try {
    // Ajuste o endpoint se necessário (ex: /users/me)
    const { data } = await api.get<User>("/auth/me")
    return data
  } catch (error) {
    throw handleError(error, "Sessão inválida ou expirada")
  }
}

// Users API
export async function getPendingUsers(
  page = 0,
  size = 20,
): Promise<PaginatedResponse<User>> {
  try {
    const { data } = await api.get<PaginatedResponse<User>>("/users/pending", {
      params: { page, size },
    })
    return data
  } catch (error) {
    throw handleError(error, "Erro ao buscar usuários pendentes")
  }
}

export async function getActiveUsers(
  page = 0,
  size = 20,
): Promise<PaginatedResponse<User>> {
  try {
    const { data } = await api.get<PaginatedResponse<User>>("/users", {
      params: { page, size },
    })
    return data
  } catch (error) {
    throw handleError(error, "Erro ao buscar usuários ativos")
  }
}

export async function activateUser(id: string): Promise<void> {
  try {
    await api.post(`/users/${id}/activate`)
  } catch (error) {
    throw handleError(error, "Erro ao ativar/desativar usuário")
  }
}

export async function updateUser(
  id: string,
  payload: Partial<RegisterPayload>,
): Promise<User> {
  try {
    const { data } = await api.patch<User>(`/users/${id}`, payload)
    return data
  } catch (error) {
    throw handleError(error, "Erro ao atualizar usuário")
  }
}

export async function deleteUser(id: string): Promise<void> {
  try {
    await api.delete(`/users/${id}`)
  } catch (error) {
    throw handleError(error, "Erro ao deletar usuário")
  }
}
